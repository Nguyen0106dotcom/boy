package com.example.media

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import com.example.domain.model.ScareTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin

class ScareAlertPlayer(context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var alarmJob: Job? = null
    private var audioTrack: AudioTrack? = null

    init {
        tts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val viLocale = Locale.forLanguageTag("vi-VN")
                val result = tts?.setLanguage(viLocale)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.language = Locale.getDefault()
                }
                tts?.setPitch(1.0f)
                tts?.setSpeechRate(0.95f)
                isTtsReady = true
            }
        }
    }

    fun triggerScare(
        theme: ScareTheme,
        playSound: Boolean,
        speakVoice: Boolean,
        vibrate: Boolean
    ) {
        stopActiveAlarm()

        if (vibrate) {
            startVibrationPattern(isGentle = theme.isGentleMode)
        }

        if (speakVoice && isTtsReady) {
            tts?.setPitch(if (theme.isGentleMode) 1.05f else 0.85f)
            tts?.speak(theme.spokenWarning, TextToSpeech.QUEUE_FLUSH, null, "kidguard_voice_tts")
        }

        if (playSound) {
            alarmJob = scope.launch {
                if (theme.isGentleMode) {
                    // Gentle chime plays twice softly instead of aggressive looping alarm
                    playSynthesizedTone(theme)
                    delay(1800L)
                    if (isActive) playSynthesizedTone(theme)
                } else {
                    while (isActive) {
                        playSynthesizedTone(theme)
                        delay(1500L)
                    }
                }
            }
        }
    }

    fun stopActiveAlarm() {
        alarmJob?.cancel()
        alarmJob = null
        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.release()
        } catch (_: Throwable) {
        } finally {
            audioTrack = null
        }
        try {
            tts?.stop()
        } catch (_: Throwable) {
        }
        stopVibration()
    }

    fun release() {
        stopActiveAlarm()
        try {
            tts?.shutdown()
        } catch (_: Throwable) {
        }
        tts = null
        scope.cancel()
    }

    private fun startVibrationPattern(isGentle: Boolean) {
        val pattern = if (isGentle) {
            longArrayOf(0, 180, 220, 180)
        } else {
            longArrayOf(0, 350, 150, 500, 150, 350, 400)
        }
        val repeatIndex = if (isGentle) -1 else 0
        val vibrator = getVibrator() ?: return
        if (!vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, repeatIndex))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, repeatIndex)
        }
    }

    private fun stopVibration() {
        try {
            getVibrator()?.cancel()
        } catch (_: Throwable) {
        }
    }

    private fun getVibrator(): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private fun playSynthesizedTone(theme: ScareTheme) {
        val sampleRate = 22050
        val durationSeconds = 1.0f
        val numSamples = (sampleRate * durationSeconds).toInt()
        val samples = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val freq = when (theme) {
                ScareTheme.GENTLE_EYECARE -> {
                    // Warm ascending 3-note chime (C5 -> E5 -> G5)
                    when {
                        t < 0.33 -> 523.25
                        t < 0.66 -> 659.25
                        else -> 783.99
                    }
                }
                ScareTheme.NIGHT_OWL -> {
                    // Soft soothing two-note bedtime chime
                    if (t < 0.5) 587.33 else 440.0
                }
                ScareTheme.SPOOKY_MONSTER -> {
                    val base = 240.0 - 110.0 * (t / durationSeconds)
                    base + 18.0 * sin(2.0 * PI * 9.0 * t)
                }
                ScareTheme.RED_SIREN -> {
                    520.0 + 360.0 * sin(2.0 * PI * 3.0 * t)
                }
            }
            val amplitude = if (theme.isGentleMode) 0.32 else 0.55
            val envelope = when {
                t < 0.06 -> t / 0.06
                t > durationSeconds - 0.14 -> (durationSeconds - t) / 0.14
                else -> 1.0
            }.coerceIn(0.0, 1.0)
            val wave = sin(2.0 * PI * freq * t)
            samples[i] = (wave * amplitude * envelope * Short.MAX_VALUE).toInt().toShort()
        }

        try {
            val bufferBytes = samples.size * 2
            val usageType = if (theme.isGentleMode) {
                AudioAttributes.USAGE_NOTIFICATION
            } else {
                AudioAttributes.USAGE_ALARM
            }
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(usageType)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferBytes)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack = track
            track.write(samples, 0, samples.size)
            track.play()
        } catch (_: Throwable) {
        }
    }
}
