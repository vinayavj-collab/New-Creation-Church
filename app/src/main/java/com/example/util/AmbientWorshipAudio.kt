package com.example.util

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sin

/**
 * Lightweight, 100% offline peaceful ambient worship music synthesizer.
 * Generates warm, gentle meditative pad chords in D Major / G Major (Root, 5th, 9th)
 * using real-time PCM synthesis via AudioTrack.
 */
class AmbientWorshipAudio private constructor() {

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private var audioTrack: AudioTrack? = null
    private var synthJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun toggle() {
        if (_isPlaying.value) {
            stop()
        } else {
            start()
        }
    }

    fun start() {
        if (_isPlaying.value) return
        _isPlaying.value = true

        synthJob = scope.launch {
            try {
                val sampleRate = 44100
                val minBufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                val bufferSize = (minBufferSize * 2).coerceAtLeast(8192)

                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                audioTrack = track
                track.play()

                // Meditative Warm Chords (Dmaj9 / Gmaj7 meditative drone frequencies in Hz)
                // Chord 1: D (146.83 Hz), A (220.0 Hz), E (329.63 Hz), F# (369.99 Hz)
                // Chord 2: G (196.00 Hz), D (293.66 Hz), B (493.88 Hz), F# (369.99 Hz)
                val chords = listOf(
                    listOf(146.83, 220.00, 329.63, 369.99, 440.00),
                    listOf(196.00, 293.66, 369.99, 493.88, 587.33)
                )

                val chunkSamples = 2048
                val shortBuffer = ShortArray(chunkSamples * 2) // Stereo: L, R
                var sampleIndex = 0L
                val chordDurationSamples = sampleRate * 12 // Change chord every 12 seconds
                var currentChordIdx = 0

                while (isActive && _isPlaying.value) {
                    val chord = chords[currentChordIdx]
                    val nextChord = chords[(currentChordIdx + 1) % chords.size]

                    val chordPhase = (sampleIndex % chordDurationSamples).toDouble() / chordDurationSamples
                    // Smooth crossfade between chords in the last 2 seconds
                    val crossfadeRatio = if (chordPhase > 0.85) {
                        ((chordPhase - 0.85) / 0.15).coerceIn(0.0, 1.0)
                    } else {
                        0.0
                    }

                    for (i in 0 until chunkSamples) {
                        val t = (sampleIndex + i).toDouble() / sampleRate
                        
                        // Gentle slow breathing LFO (0.1 Hz) for warmth
                        val breathLfo = 0.75 + 0.25 * sin(2.0 * Math.PI * 0.08 * t)

                        // Base chord synthesis
                        var mixedSignal = 0.0
                        for (freq in chord) {
                            val wave = sin(2.0 * Math.PI * freq * t)
                            val harmonic = 0.3 * sin(2.0 * Math.PI * (freq * 2.0) * t)
                            mixedSignal += (wave + harmonic)
                        }
                        mixedSignal = (mixedSignal / chord.size) * (1.0 - crossfadeRatio)

                        // Next chord synthesis for seamless crossfade
                        if (crossfadeRatio > 0.0) {
                            var nextMixedSignal = 0.0
                            for (freq in nextChord) {
                                val wave = sin(2.0 * Math.PI * freq * t)
                                val harmonic = 0.3 * sin(2.0 * Math.PI * (freq * 2.0) * t)
                                nextMixedSignal += (wave + harmonic)
                            }
                            nextMixedSignal = (nextMixedSignal / nextChord.size) * crossfadeRatio
                            mixedSignal += nextMixedSignal
                        }

                        val sampleValue = (mixedSignal * breathLfo * 7000.0).toInt().coerceIn(-32767, 32767)
                        
                        // Stereo output (slight chorus stereo spread)
                        val rightValue = ((mixedSignal * (0.8 + 0.2 * sin(2.0 * Math.PI * 0.11 * t))) * 7000.0).toInt().coerceIn(-32767, 32767)

                        shortBuffer[i * 2] = sampleValue.toShort()
                        shortBuffer[i * 2 + 1] = rightValue.toShort()
                    }

                    sampleIndex += chunkSamples
                    if (sampleIndex % chordDurationSamples < chunkSamples) {
                        currentChordIdx = (currentChordIdx + 1) % chords.size
                    }

                    val written = track.write(shortBuffer, 0, shortBuffer.size)
                    if (written < 0) {
                        break
                    }
                }
            } catch (e: CancellationException) {
                // Expected when stopped
            } catch (e: Exception) {
                Log.w("AmbientWorshipAudio", "Synth error: ${e.message}")
            } finally {
                cleanUpTrack()
            }
        }
    }

    fun stop() {
        _isPlaying.value = false
        synthJob?.cancel()
        cleanUpTrack()
    }

    private fun cleanUpTrack() {
        try {
            audioTrack?.apply {
                if (playState == AudioTrack.PLAYSTATE_PLAYING) {
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            // Ignore
        } finally {
            audioTrack = null
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: AmbientWorshipAudio? = null

        fun getInstance(): AmbientWorshipAudio {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AmbientWorshipAudio().also { INSTANCE = it }
            }
        }
    }
}
