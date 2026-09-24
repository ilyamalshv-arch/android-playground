package com.ilyamalshv.vnutri.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import com.ilyamalshv.vnutri.R

/** Looping ambient pad with gentle volume fades. All calls must be on the main thread. */
class Ambient(private val context: Context) {
    private var player: MediaPlayer? = null
    private var volume = 0f
    private var target = 0f
    private var enabled = false
    private var foreground = true
    private val handler = Handler(Looper.getMainLooper())

    private val step = object : Runnable {
        override fun run() {
            val p = player ?: return
            val delta = target - volume
            volume = if (kotlin.math.abs(delta) < 0.01f) target else volume + delta * 0.06f
            p.setVolume(volume, volume)
            if (volume == 0f && target == 0f) {
                p.pause()
            } else if (volume != target) {
                handler.postDelayed(this, 50)
            }
        }
    }

    /** Level: 0..1. Intro plays louder, the rest of the app sits quietly underneath. */
    fun setLevel(level: Float) {
        target = if (enabled && foreground) level else 0f
        levelWanted = level
        apply()
    }

    private var levelWanted = 0.25f

    fun setEnabled(on: Boolean) {
        enabled = on
        setLevel(levelWanted)
    }

    fun onForeground(isForeground: Boolean) {
        foreground = isForeground
        setLevel(levelWanted)
    }

    private fun apply() {
        if (target > 0f) {
            val p = player ?: MediaPlayer.create(context, R.raw.ambient)?.also {
                it.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                )
                it.isLooping = true
                it.setVolume(0f, 0f)
                player = it
            } ?: return
            if (!p.isPlaying) p.start()
        }
        handler.removeCallbacks(step)
        handler.post(step)
    }

    fun release() {
        handler.removeCallbacks(step)
        player?.release()
        player = null
        volume = 0f
    }
}
