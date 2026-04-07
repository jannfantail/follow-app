package ch.gypaete.follow.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import ch.gypaete.follow.api.ApiClient
import ch.gypaete.follow.ui.MainActivity
import ch.gypaete.follow.util.NotificationHelper
import ch.gypaete.follow.util.SoundManager
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class FollowForegroundService : Service() {

    companion object {
        const val CHANNEL_ID    = "follow_service"
        const val NOTIF_ID      = 1
        const val ACTION_START  = "START"
        const val ACTION_STOP   = "STOP"
        const val EXTRA_URL     = "url"
        const val EXTRA_COOKIE  = "cookie"
        const val EXTRA_ROOM    = "room"
        private const val TAG   = "FollowService"
        private const val POLL_MS = 5_000L

        fun start(context: Context, url: String, cookie: String, room: Int) {
            val intent = Intent(context, FollowForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_URL, url)
                putExtra(EXTRA_COOKIE, cookie)
                putExtra(EXTRA_ROOM, room)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.startService(Intent(context, FollowForegroundService::class.java).apply {
                action = ACTION_STOP
            })
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var pollJob: Job? = null
    private var sinceId = 0
    private var baseUrl = ""
    private var cookie  = ""
    private var room    = 1

    // Etat precedent des vols pour detecter les changements
    private val lastStatus = mutableMapOf<Int, String>()

    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                baseUrl = intent.getStringExtra(EXTRA_URL) ?: ""
                cookie  = intent.getStringExtra(EXTRA_COOKIE) ?: ""
                room    = intent.getIntExtra(EXTRA_ROOM, 1)
                startForeground(NOTIF_ID, buildForegroundNotif())
                startPolling()
            }
            ACTION_STOP -> {
                stopPolling()
                stopForeground(true)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startPolling() {
        if (pollJob?.isActive == true) return
        pollJob = scope.launch {
            while (isActive) {
                try { doPoll() } catch (e: Exception) { Log.e(TAG, "Poll error", e) }
                delay(POLL_MS)
            }
        }
    }

    private fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    private suspend fun doPoll() {
        val url = "${baseUrl}api.php?action=list&room=$room"
        val req = Request.Builder()
            .url(url)
            .header("Cookie", cookie)
            .build()

        val response = http.newCall(req).execute()
        val body = response.body?.string() ?: return
        if (!body.trimStart().startsWith("{")) return

        val json = JSONObject(body)
        if (!json.optBoolean("ok", false)) return

        sinceId = json.optInt("since_id", sinceId)
        val vols = json.optJSONArray("vols") ?: return

        for (i in 0 until vols.length()) {
            val vol = vols.getJSONObject(i)
            val volId  = vol.optInt("vol_id")
            val nom    = vol.optString("nom", "Eleve")
            val status = vol.optString("status", "wait")
            val prev   = lastStatus[volId]

            if (prev != null && prev != status) {
                // Changement detecte - notifier
                notifyChange(nom, prev, status)
            }
            lastStatus[volId] = status
        }
    }

    private fun notifyChange(nom: String, prev: String, current: String) {
        val message = when (current) {
            "air"  -> "$nom a decollé"
            "down" -> "$nom est posé"
            "xfer" -> "$nom est transféré"
            "wait" -> "$nom est arrivé au déco"
            else   -> "$nom : $current"
        }
        val title = when (current) {
            "air"  -> "Decollage"
            "down" -> "Posé"
            "xfer" -> "Transféré"
            "wait" -> "Arrivée déco"
            else   -> "Follow"
        }

        // Utiliser le son configure pour l'action correspondante
        val actionKey = when (current) {
            "air"  -> "decolle"
            "down" -> "atterri"
            "xfer" -> "transfere"
            "wait" -> "arrive_deco"
            else   -> "notification"
        }
        // Son + notification avec le son configure pour cette action
        SoundManager.playForAction(this, actionKey)
        NotificationHelper.send(this, title, message, actionKey)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Follow Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setSound(null, null)
                enableVibration(false)
            }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotif(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pending = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Follow actif")
            .setContentText("Surveillance des vols en cours")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pending)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
