package ch.gypaete.follow.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri

object SoundManager {

    private var mediaPlayer: MediaPlayer? = null

    // Sons fixes disponibles
    val SOUNDS = linkedMapOf(
        "aucun"          to "Aucun son",
        "notification"   to "Notification systeme",
        "alarme"         to "Alarme systeme",
        "sonnerie"       to "Sonnerie telephone",
        "custom"         to "Sonnerie personnalisee"
    )

    fun playForAction(context: Context, action: String) {
        val prefs = context.getSharedPreferences("follow", Context.MODE_PRIVATE)
        val soundKey = prefs.getString("sound_$action", "notification") ?: "notification"
        val customUri = prefs.getString("sound_custom_${action}", null)
        play(context, soundKey, customUri)
    }

    fun play(context: Context, soundKey: String, customUri: String? = null) {
        if (soundKey == "aucun") return
        try {
            mediaPlayer?.release()
            mediaPlayer = null
            val uri: Uri = when (soundKey) {
                "alarme"   -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                "sonnerie" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                "custom"   -> if (customUri != null) Uri.parse(customUri)
                              else RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                else       -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .build()
                )
                setDataSource(context, uri)
                prepare()
                start()
                setOnCompletionListener { release() }
            }
        } catch (e: Exception) { /* silencieux */ }
    }

    fun saveSoundForAction(context: Context, action: String, soundKey: String,
                           customUri: String? = null) {
        val editor = context.getSharedPreferences("follow", Context.MODE_PRIVATE).edit()
        editor.putString("sound_$action", soundKey)
        if (customUri != null) editor.putString("sound_custom_$action", customUri)
        editor.apply()
    }

    fun getSoundForAction(context: Context, action: String): String =
        context.getSharedPreferences("follow", Context.MODE_PRIVATE)
            .getString("sound_$action", "notification") ?: "notification"

    fun getCustomUriForAction(context: Context, action: String): String? =
        context.getSharedPreferences("follow", Context.MODE_PRIVATE)
            .getString("sound_custom_$action", null)
}
