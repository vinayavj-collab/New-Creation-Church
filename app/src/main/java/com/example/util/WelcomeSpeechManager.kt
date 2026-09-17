package com.example.util

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WelcomeSpeechManager private constructor(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false
    private var hasSpokenInCurrentSession = false
    private var isAppInForeground = false
    private val prefs = context.getSharedPreferences("welcome_speech_prefs", Context.MODE_PRIVATE)

    init {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e("WelcomeSpeechManager", "Failed to create TextToSpeech engine", e)
        }
    }

    companion object {
        @Volatile
        private var instance: WelcomeSpeechManager? = null

        fun getInstance(context: Context): WelcomeSpeechManager {
            return instance ?: synchronized(this) {
                instance ?: WelcomeSpeechManager(context.applicationContext).also { instance = it }
            }
        }
    }

    fun setAppInForeground(inForeground: Boolean) {
        isAppInForeground = inForeground
        if (!inForeground) {
            stop()
        }
    }

    fun isAppInForeground(): Boolean = isAppInForeground

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val hindiResult = tts?.setLanguage(Locale("hi", "IN"))
            if (hindiResult == TextToSpeech.LANG_MISSING_DATA || hindiResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w("WelcomeSpeechManager", "Hindi locale not fully supported, falling back to default locale")
                tts?.setLanguage(Locale.getDefault())
            }
            isTtsInitialized = true
        } else {
            Log.e("WelcomeSpeechManager", "TextToSpeech init failed with status $status")
            isTtsInitialized = false
        }
    }

    fun speakOnAppOpen(
        userName: String,
        enableWelcomeSpeech: Boolean,
        enableVerseSpeech: Boolean,
        welcomeOncePerDay: Boolean,
        verseOncePerDay: Boolean,
        todaysVerseText: String?,
        isUpdateAvailable: Boolean = false,
        speechPitch: Float = 1.0f,
        speechSpeed: Float = 1.0f,
        speechVolume: Float = 1.0f
    ) {
        // STRICT FOREGROUND REQUIREMENT: Never speak if app is not active in foreground
        if (!isAppInForeground) {
            return
        }

        if (hasSpokenInCurrentSession) {
            return
        }

        if (!isTtsInitialized || tts == null) {
            return
        }

        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val lastWelcomeDate = prefs.getString("last_welcome_date", "") ?: ""
        val lastVerseDate = prefs.getString("last_verse_date", "") ?: ""
        val lastUpdateDate = prefs.getString("last_update_speech_date", "") ?: ""

        val shouldSpeakWelcome = enableWelcomeSpeech && userName.isNotBlank() && (!welcomeOncePerDay || lastWelcomeDate != currentDate)
        val shouldSpeakVerse = enableVerseSpeech && !todaysVerseText.isNullOrBlank() && (!verseOncePerDay || lastVerseDate != currentDate)
        val shouldSpeakUpdate = isUpdateAvailable && (lastUpdateDate != currentDate)

        if (!shouldSpeakWelcome && !shouldSpeakVerse && !shouldSpeakUpdate) {
            return
        }

        hasSpokenInCurrentSession = true
        val speechQueue = mutableListOf<String>()

        if (shouldSpeakWelcome) {
            val cleanName = userName.trim()
            val greeting = RemoteConfigHelper.dailyGreetingText.value.ifBlank { "जय मसीह की" }
            val welcomeText = "$cleanName जी, $greeting"
            speechQueue.add(welcomeText)
            prefs.edit().putString("last_welcome_date", currentDate).apply()
        }

        if (shouldSpeakVerse) {
            val formattedVerse = ScriptureSpeechUtils.formatScriptureTextForSpeech(todaysVerseText!!.trim())
            val verseText = "आज का वचन है। $formattedVerse"
            speechQueue.add(verseText)
            prefs.edit().putString("last_verse_date", currentDate).apply()
        }

        if (shouldSpeakUpdate) {
            val cleanName = userName.trim()
            val namePrefix = if (cleanName.isNotBlank()) "$cleanName जी, " else ""
            val updateText = "${namePrefix}इस ऐप का नया अपडेट उपलब्ध है, कृपया डाउनलोड करके इंस्टॉल करें।"
            speechQueue.add(updateText)
            prefs.edit().putString("last_update_speech_date", currentDate).apply()
        }

        executeSpeechQueue(speechQueue, speechPitch, speechSpeed, speechVolume, requireForeground = true)
    }

    fun speakUpdateAnnouncement(userName: String) {
        if (!isAppInForeground || !isTtsInitialized || tts == null) return
        val cleanName = userName.trim()
        val namePrefix = if (cleanName.isNotBlank()) "$cleanName जी, " else ""
        val updateText = "${namePrefix}इस ऐप का नया अपडेट उपलब्ध है, कृपया डाउनलोड करके इंस्टॉल करें।"
        executeSpeechQueue(listOf(updateText), 1.0f, 1.0f, 1.0f, requireForeground = true)
    }

    fun testSpeech(
        userName: String,
        todaysVerseText: String?,
        speechPitch: Float = 1.0f,
        speechSpeed: Float = 1.0f,
        speechVolume: Float = 1.0f
    ) {
        if (!isTtsInitialized || tts == null) return

        val speechQueue = mutableListOf<String>()
        if (userName.isNotBlank()) {
            speechQueue.add("${userName.trim()} जी, जय मसीह की")
        } else {
            speechQueue.add("जय मसीह की")
        }

        val rawVerse = if (!todaysVerseText.isNullOrBlank()) todaysVerseText.trim() else "परमेश्वर का वचन ही जीवन का मार्ग है।"
        val formattedVerse = ScriptureSpeechUtils.formatScriptureTextForSpeech(rawVerse)
        speechQueue.add("आज का वचन है। $formattedVerse")

        executeSpeechQueue(speechQueue, speechPitch, speechSpeed, speechVolume, requireForeground = false)
    }

    fun speakDirect(
        messages: List<String>,
        speechPitch: Float = 1.0f,
        speechSpeed: Float = 1.0f,
        speechVolume: Float = 1.0f,
        requireForeground: Boolean = false
    ) {
        executeSpeechQueue(messages, speechPitch, speechSpeed, speechVolume, requireForeground = requireForeground)
    }

    private fun executeSpeechQueue(
        messages: List<String>,
        speechPitch: Float = 1.0f,
        speechSpeed: Float = 1.0f,
        speechVolume: Float = 1.0f,
        requireForeground: Boolean = false
    ) {
        if (messages.isEmpty() || tts == null) return
        if (requireForeground && !isAppInForeground) return

        try {
            tts?.stop()
            tts?.setPitch(speechPitch.coerceIn(0.5f, 2.0f))
            tts?.setSpeechRate(speechSpeed.coerceIn(0.5f, 2.0f))

            val params = Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, speechVolume.coerceIn(0.1f, 1.0f))
            }

            messages.forEachIndexed { index, message ->
                if (requireForeground && !isAppInForeground) {
                    tts?.stop()
                    return
                }
                val queueMode = if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
                tts?.speak(message, queueMode, params, "welcome_speech_$index")
                tts?.playSilentUtterance(1200L, TextToSpeech.QUEUE_ADD, "silence_$index")
            }
        } catch (e: Exception) {
            Log.e("WelcomeSpeechManager", "Error executing speech queue", e)
        }
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            // ignore
        }
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
            isTtsInitialized = false
        } catch (e: Exception) {
            // ignore
        }
    }
}
