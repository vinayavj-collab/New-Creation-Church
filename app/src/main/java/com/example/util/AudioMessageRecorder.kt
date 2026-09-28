package com.example.util

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File

class AudioMessageRecorder(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var startTimeMillis: Long = 0L

    fun startRecording(quality: String): File? {
        stopRecording()
        try {
            val audioDir = File(context.cacheDir, "audio_messages").apply { if (!exists()) mkdirs() }
            val outputFile = File(audioDir, "devotion_${System.currentTimeMillis()}.m4a")
            currentOutputFile = outputFile

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)

                when (quality) {
                    "data_saver_32k" -> {
                        setAudioEncodingBitRate(32000)
                        setAudioSamplingRate(22050)
                        setAudioChannels(1) // Mono
                    }
                    "high_128k" -> {
                        setAudioEncodingBitRate(128000)
                        setAudioSamplingRate(44100)
                        setAudioChannels(1)
                    }
                    else -> { // "standard_64k" default
                        setAudioEncodingBitRate(64000)
                        setAudioSamplingRate(44100)
                        setAudioChannels(1)
                    }
                }

                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            startTimeMillis = System.currentTimeMillis()
            return outputFile
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Failed to start recording: ${e.message}", e)
            stopRecording()
            return null
        }
    }

    fun stopRecording(): RecordedAudioInfo? {
        val recorder = mediaRecorder ?: return null
        val file = currentOutputFile
        return try {
            recorder.stop()
            recorder.release()
            mediaRecorder = null
            val durationSeconds = if (startTimeMillis > 0) {
                ((System.currentTimeMillis() - startTimeMillis) / 1000).toInt()
            } else 0
            if (file != null && file.exists()) {
                RecordedAudioInfo(
                    file = file,
                    durationSeconds = durationSeconds,
                    fileSizeBytes = file.length()
                )
            } else null
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Failed to stop recording: ${e.message}", e)
            mediaRecorder = null
            null
        }
    }

    fun cancelRecording() {
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
        } catch (e: Exception) {
            // Ignore
        } finally {
            mediaRecorder = null
            currentOutputFile?.delete()
            currentOutputFile = null
        }
    }
}

data class RecordedAudioInfo(
    val file: File,
    val durationSeconds: Int,
    val fileSizeBytes: Long
)
