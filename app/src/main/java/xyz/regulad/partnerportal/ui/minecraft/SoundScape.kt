package xyz.regulad.partnerportal.ui.minecraft

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Looper
import xyz.regulad.partnerportal.R
import java.util.*

object ClickSoundManager {
    private val mediaPlayerWeakMap: MutableMap<Context, MediaPlayer> = Collections.synchronizedMap(WeakHashMap())
    private val looper = Looper.getMainLooper()

    fun Context.doClickSound() = looper.run {
        val mediaPlayer = mediaPlayerWeakMap.getOrPut(this@doClickSound) {
            MediaPlayer.create(this@doClickSound, R.raw.click).apply {
                // don't release it because we constantly replay this
                isLooping = false
            }
        }

        if (!mediaPlayer.isPlaying && !getIsSilent()) {
            mediaPlayer.start()
        }
    }

    fun Context.getIsSilent(): Boolean {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return when (audioManager.ringerMode) {
            AudioManager.RINGER_MODE_SILENT -> true
            AudioManager.RINGER_MODE_VIBRATE -> true
            else -> false
        }
    }
}
