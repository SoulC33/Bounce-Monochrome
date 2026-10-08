package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.model.SurfaceType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlin.math.PI
import kotlin.math.sin

data class ToneNote(
    val freqHz: Double,
    val durationMs: Int,
    val dutyCycle: Double = 0.5,
    val volume: Float = 0.24f
)

class AudioManager(context: Context) {
    private val appContext = context.applicationContext
    private val audioScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Prevent exhausting Android's per-process AudioFlinger track limit during rapid bounces
    private val sfxSemaphore = Semaphore(4)

    @Volatile
    var soundEnabled: Boolean = true

    @Volatile
    var musicEnabled: Boolean = true

    @Volatile
    var hapticsEnabled: Boolean = true

    private var musicJob: Job? = null

    private val vibrator: Vibrator? by lazy {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        }.getOrNull()
    }

    fun playBounce(surface: SurfaceType) {
        if (hapticsEnabled) {
            vibratePulse(if (surface == SurfaceType.SPRING) 18L else 8L)
        }
        if (!soundEnabled) return
        when (surface) {
            SurfaceType.SPRING -> playSequence(
                listOf(
                    ToneNote(330.0, 28, 0.5, 0.25f),
                    ToneNote(494.0, 32, 0.5, 0.25f),
                    ToneNote(659.25, 48, 0.5, 0.26f)
                )
            )
            SurfaceType.DAMPENED -> playSequence(
                listOf(
                    ToneNote(146.8, 35, 0.35, 0.18f)
                )
            )
            SurfaceType.SLOPE -> playSequence(
                listOf(
                    ToneNote(246.9, 25, 0.5, 0.22f),
                    ToneNote(311.1, 25, 0.5, 0.20f)
                )
            )
            else -> playSequence(
                listOf(
                    ToneNote(220.0, 22, 0.5, 0.21f),
                    ToneNote(293.66, 26, 0.5, 0.19f)
                )
            )
        }
    }

    fun playBoost() {
        if (hapticsEnabled) vibratePulse(12L)
        if (!soundEnabled) return
        playSequence(
            listOf(
                ToneNote(392.0, 24, 0.5, 0.22f),
                ToneNote(523.25, 30, 0.5, 0.24f),
                ToneNote(659.25, 36, 0.5, 0.22f)
            )
        )
    }

    fun playCollectRing() {
        if (hapticsEnabled) vibratePulse(14L)
        if (!soundEnabled) return
        playSequence(
            listOf(
                ToneNote(987.77, 45, 0.5, 0.25f),
                ToneNote(1318.51, 85, 0.5, 0.26f)
            )
        )
    }

    fun playCollectGem() {
        if (!soundEnabled) return
        playSequence(
            listOf(
                ToneNote(783.99, 32, 0.5, 0.22f),
                ToneNote(1046.50, 48, 0.5, 0.22f)
            )
        )
    }

    fun playExtraLife() {
        if (hapticsEnabled) vibratePulse(25L)
        if (!soundEnabled) return
        playSequence(
            listOf(
                ToneNote(523.25, 50, 0.5, 0.25f),
                ToneNote(659.25, 50, 0.5, 0.25f),
                ToneNote(783.99, 50, 0.5, 0.25f),
                ToneNote(1046.50, 110, 0.5, 0.28f)
            )
        )
    }

    fun playCheckpoint() {
        if (hapticsEnabled) vibratePulse(20L)
        if (!soundEnabled) return
        playSequence(
            listOf(
                ToneNote(440.0, 45, 0.5, 0.24f),
                ToneNote(554.37, 45, 0.5, 0.24f),
                ToneNote(659.25, 80, 0.5, 0.26f)
            )
        )
    }

    fun playExitUnlocked() {
        if (hapticsEnabled) vibratePulse(30L)
        if (!soundEnabled) return
        playSequence(
            listOf(
                ToneNote(587.33, 55, 0.5, 0.26f),
                ToneNote(739.99, 55, 0.5, 0.26f),
                ToneNote(880.00, 55, 0.5, 0.26f),
                ToneNote(1174.66, 130, 0.5, 0.28f)
            )
        )
    }

    fun playHazardHit() {
        if (hapticsEnabled) vibratePulse(45L)
        if (!soundEnabled) return
        playSequence(
            listOf(
                ToneNote(311.13, 55, 0.25, 0.28f),
                ToneNote(233.08, 65, 0.25, 0.28f),
                ToneNote(164.81, 90, 0.25, 0.28f),
                ToneNote(110.00, 120, 0.25, 0.28f)
            )
        )
    }

    fun playLevelComplete() {
        if (hapticsEnabled) vibratePulse(35L)
        if (!soundEnabled) return
        playSequence(
            listOf(
                ToneNote(523.25, 70, 0.5, 0.26f),
                ToneNote(659.25, 70, 0.5, 0.26f),
                ToneNote(783.99, 70, 0.5, 0.26f),
                ToneNote(1046.50, 140, 0.5, 0.28f),
                ToneNote(0.0, 35, 0.5, 0f),
                ToneNote(783.99, 70, 0.5, 0.26f),
                ToneNote(1046.50, 210, 0.5, 0.28f)
            )
        )
    }

    fun playGameOver() {
        if (hapticsEnabled) vibratePulse(55L)
        if (!soundEnabled) return
        playSequence(
            listOf(
                ToneNote(440.00, 95, 0.5, 0.26f),
                ToneNote(415.30, 95, 0.5, 0.26f),
                ToneNote(392.00, 95, 0.5, 0.26f),
                ToneNote(369.99, 220, 0.5, 0.26f)
            )
        )
    }

    fun playMenuSelect() {
        if (hapticsEnabled) vibratePulse(6L)
        if (!soundEnabled) return
        playSequence(
            listOf(
                ToneNote(880.0, 22, 0.5, 0.20f)
            )
        )
    }

    fun playButtonPress() {
        if (hapticsEnabled) vibratePulse(8L)
        if (!soundEnabled) return
        playSequence(
            listOf(
                ToneNote(659.25, 22, 0.5, 0.22f),
                ToneNote(987.77, 28, 0.5, 0.22f)
            )
        )
    }

    fun startGameplayMusic() {
        if (!musicEnabled) {
            stopGameplayMusic()
            return
        }
        if (musicJob?.isActive == true) return

        // Synthesize the entire bar phrase as one static buffer per cycle to avoid
        // rapid AudioTrack creation and guarantee clean cancellation
        val phraseNotes = listOf(
            ToneNote(220.00, 130, 0.25, 0.06f),
            ToneNote(0.0, 50, 0.25, 0f),
            ToneNote(261.63, 130, 0.25, 0.06f),
            ToneNote(0.0, 50, 0.25, 0f),
            ToneNote(329.63, 130, 0.25, 0.06f),
            ToneNote(0.0, 50, 0.25, 0f),
            ToneNote(293.66, 130, 0.25, 0.06f),
            ToneNote(0.0, 50, 0.25, 0f),
            ToneNote(196.00, 130, 0.25, 0.06f),
            ToneNote(0.0, 50, 0.25, 0f),
            ToneNote(246.94, 130, 0.25, 0.06f),
            ToneNote(0.0, 50, 0.25, 0f),
            ToneNote(220.00, 180, 0.25, 0.06f),
            ToneNote(0.0, 180, 0.25, 0f)
        )

        musicJob = audioScope.launch {
            while (isActive && musicEnabled) {
                synthesizeAndPlayBlocking(phraseNotes)
            }
        }
    }

    fun stopGameplayMusic() {
        musicJob?.cancel()
        musicJob = null
    }

    private fun vibratePulse(durationMs: Long) {
        runCatching {
            val v = vibrator ?: return
            if (!v.hasVibrator()) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(durationMs)
            }
        }
    }

    private fun playSequence(notes: List<ToneNote>) {
        if (!sfxSemaphore.tryAcquire()) return
        audioScope.launch {
            try {
                synthesizeAndPlayBlocking(notes)
            } finally {
                sfxSemaphore.release()
            }
        }
    }

    private suspend fun synthesizeAndPlayBlocking(notes: List<ToneNote>) {
        val sampleRate = 22050
        val totalSamples = notes.sumOf { (sampleRate * it.durationMs) / 1000 }
        if (totalSamples <= 0) return

        val pcm = ShortArray(totalSamples)
        var offset = 0

        for (note in notes) {
            val count = (sampleRate * note.durationMs) / 1000
            val freq = note.freqHz
            val amp = (Short.MAX_VALUE * note.volume).toInt()
            val fadeSamples = minOf(count / 6, 80)

            for (i in 0 until count) {
                if (offset + i >= totalSamples) break
                if (freq <= 1.0) {
                    pcm[offset + i] = 0
                } else {
                    val phase = (i.toDouble() * freq / sampleRate) % 1.0
                    val rawSquare = if (phase < note.dutyCycle) 1.0 else -1.0
                    val subHarmonic = sin(2.0 * PI * (freq * 0.5) * i / sampleRate) * 0.15
                    val env = when {
                        i < fadeSamples && fadeSamples > 0 -> i.toDouble() / fadeSamples
                        i > count - fadeSamples && fadeSamples > 0 -> (count - i).toDouble() / fadeSamples
                        else -> 1.0
                    }
                    val sampleVal = ((rawSquare * 0.85 + subHarmonic) * amp * env).toInt()
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    pcm[offset + i] = sampleVal.toShort()
                }
            }
            offset += count
        }

        var track: AudioTrack? = null
        try {
            val byteSize = pcm.size * 2
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = maxOf(byteSize, minBuf)

            track = runCatching {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
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
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
            }.getOrNull() ?: return

            track.write(pcm, 0, pcm.size)
            track.play()
            val totalDurationMs = notes.sumOf { it.durationMs }.toLong()
            delay(totalDurationMs + 15L)
        } finally {
            runCatching { track?.stop() }
            runCatching { track?.release() }
        }
    }
}
