package com.example.calculator.feedback

import android.content.Context
import android.database.ContentObserver
import android.media.AudioAttributes
import android.os.*
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.View

enum class HapticIntensity { Off, Light, Medium, Strong, Maximum }
enum class FeedbackKind { Number, Operator, Equals, Clear, Navigation }

/** Touch usage never requests bypass flags or ignores the user's system settings. */
class HapticFeedbackManager(context: Context) : AutoCloseable {
    private val resolver = context.contentResolver
    @Suppress("DEPRECATION")
    private val vibrator = if (Build.VERSION.SDK_INT >= 31)
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    else context.getSystemService(Vibrator::class.java)
    private val available = vibrator?.hasVibrator() == true
    private val amplitudeControl = Build.VERSION.SDK_INT >= 26 && vibrator?.hasAmplitudeControl() == true
    private var systemEnabled = readSystemEnabled()
    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) { systemEnabled = readSystemEnabled() }
    }
    private val audio = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
    private val attributes = if (Build.VERSION.SDK_INT >= 33)
        VibrationAttributes.Builder().setUsage(VibrationAttributes.USAGE_TOUCH).build() else null
    private val effects: Map<Pair<HapticIntensity, FeedbackKind>, VibrationEffect> =
        if (Build.VERSION.SDK_INT >= 26) buildMap {
            HapticIntensity.entries.filter { it != HapticIntensity.Off }.forEach { level ->
                FeedbackKind.entries.forEach { kind ->
                    val amplitude = if (amplitudeControl) when(level) {
                        HapticIntensity.Light -> 65; HapticIntensity.Medium -> 125
                        HapticIntensity.Strong -> 200; else -> 255
                    } else VibrationEffect.DEFAULT_AMPLITUDE
                    val duration = when(kind) {
                        FeedbackKind.Number -> 14L; FeedbackKind.Operator -> 19L
                        FeedbackKind.Equals -> 28L; FeedbackKind.Clear -> 12L
                        FeedbackKind.Navigation -> 9L
                    }
                    put(level to kind, if(kind == FeedbackKind.Clear)
                        VibrationEffect.createWaveform(longArrayOf(0, duration, 24, duration), intArrayOf(0, amplitude, 0, amplitude), -1)
                    else VibrationEffect.createOneShot(duration, amplitude))
                }
            }
        } else emptyMap()
    init { resolver.registerContentObserver(Settings.System.getUriFor(Settings.System.HAPTIC_FEEDBACK_ENABLED), false, observer) }
    private fun readSystemEnabled() = Settings.System.getInt(resolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) != 0
    val capabilityNote: String get() = when {
        !available -> "This device has no vibration motor."
        !amplitudeControl -> "This device uses system feedback strength; intensity levels may feel identical."
        else -> "Strength is subject to Android touch-feedback settings and device limits."
    }
    fun perform(view: View, level: HapticIntensity, kind: FeedbackKind) {
        if (level == HapticIntensity.Off || !available || !systemEnabled || !view.isHapticFeedbackEnabled || !view.hasWindowFocus()) return
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                val effect = effects.getValue(level to kind)
                if (Build.VERSION.SDK_INT >= 33) vibrator?.vibrate(effect, attributes!!)
                else vibrator?.vibrate(effect, audio)
            } else {
                view.performHapticFeedback(when(kind) {
                    FeedbackKind.Equals, FeedbackKind.Clear -> HapticFeedbackConstants.LONG_PRESS
                    FeedbackKind.Navigation -> HapticFeedbackConstants.CLOCK_TICK
                    else -> HapticFeedbackConstants.KEYBOARD_TAP
                })
            }
        } catch (_: SecurityException) { /* Device policy can suppress vibration. */ }
    }
    override fun close() { resolver.unregisterContentObserver(observer) }
}
