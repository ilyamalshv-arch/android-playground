package com.ilyamalshv.vnutri.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.staticCompositionLocalOf
import com.ilyamalshv.vnutri.R
import com.ilyamalshv.vnutri.data.Settings

/** Quiet UI sounds plus light haptics, each switchable in settings. */
class Feedback(context: Context, private val settings: Settings) {
    private val pool = SoundPool.Builder()
        .setMaxStreams(3)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()
    private val tapId = pool.load(context, R.raw.tap, 1)
    private val selectId = pool.load(context, R.raw.select, 1)
    private val confirmId = pool.load(context, R.raw.confirm, 1)

    var view: View? = null

    /** Light touch: expanding a card, choosing intensity. */
    fun tap() = play(tapId, 0.5f, HapticFeedbackConstants.CLOCK_TICK)

    /** Choosing a feeling, saving a thought. */
    fun select() = play(selectId, 0.55f, HapticFeedbackConstants.CLOCK_TICK)

    /** Completing something: "Осмыслить", entering from the intro. */
    fun confirm() = play(
        confirmId,
        0.6f,
        if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.VIRTUAL_KEY,
    )

    private fun play(id: Int, volume: Float, haptic: Int) {
        if (settings.sounds) pool.play(id, volume, volume, 1, 0, 1f)
        if (settings.haptics) view?.performHapticFeedback(haptic)
    }

    fun release() = pool.release()
}

val LocalFeedback = staticCompositionLocalOf<Feedback?> { null }
