package ch.gypaete.follow.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri

object SoundManager {

    private var mediaPlayer: MediaPlayer? = null

    val SOUNDS = linkedMapOf(
        "aucun"        to "Aucun son",
        "default"      to "Son systeme",
        "notification" to "Notification",
        "ding"         to "Sonnerie"
    )

    fun playForAction(context: Context, action: String) {
        val prefs = context.getSharedPreferences("follow", Context.MODE_PRIVATE)
        val soundKey = prefs.getString("sound_$action", "default") ?: "default"
        play(context, soundKey)
    }

    fun play(context: Context, soundKey: String) {
        if (soundKey == "aucun") return
        try {
            mediaPlayer?.release()
            mediaPlayer = null
            val uri: Uri = when (soundKey) {
                "ding" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                else   -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .build()
                )
                setDataSource(context, uri)
                prepare()
                start()
                setOnCompletionListener { release() }
            }
        } catch (e: Exception) { /* silencieux */ }
    }

    fun saveSoundForAction(context: Context, action: String, soundKey: String) {
        context.getSharedPreferences("follow", Context.MODE_PRIVATE)
            .edit().putString("sound_$action", soundKey).apply()
    }

    fun getSoundForAction(context: Context, action: String): String =
        context.getSharedPreferences("follow", Context.MODE_PRIVATE)
            .getString("sound_$action", "default") ?: "default"
}
