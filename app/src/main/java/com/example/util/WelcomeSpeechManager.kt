package com.example.util

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WelcomeSpeechManager private constructor(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false
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
        todaysVerseText: String?
    ) {
        if (!isTtsInitialized || tts == null) {
            // Re-check or try init again if needed
            return
        }

        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val lastWelcomeDate = prefs.getString("last_welcome_date", "") ?: ""
        val lastVerseDate = prefs.getString("last_verse_date", "") ?: ""

        val shouldSpeakWelcome = enableWelcomeSpeech && userName.isNotBlank() && (!welcomeOncePerDay || lastWelcomeDate != currentDate)
        val shouldSpeakVerse = enableVerseSpeech && !todaysVerseText.isNullOrBlank() && (!verseOncePerDay || lastVerseDate != currentDate)

        if (!shouldSpeakWelcome && !shouldSpeakVerse) {
            return
        }

        val speechQueue = mutableListOf<String>()

        if (shouldSpeakWelcome) {
            val cleanName = userName.trim()
            val welcomeText = "$cleanName जी, जय मसीह की"
            speechQueue.add(welcomeText)
            prefs.edit().putString("last_welcome_date", currentDate).apply()
        }

        if (shouldSpeakVerse) {
            val cleanVerse = todaysVerseText!!.trim()
            val verseText = "आज का वचन है। $cleanVerse"
            speechQueue.add(verseText)
            prefs.edit().putString("last_verse_date", currentDate).apply()
        }

        executeSpeechQueue(speechQueue)
    }

    fun testSpeech(userName: String, todaysVerseText: String?) {
        if (!isTtsInitialized || tts == null) return

        val speechQueue = mutableListOf<String>()
        if (userName.isNotBlank()) {
            speechQueue.add("${userName.trim()} जी, जय मसीह की")
        } else {
            speechQueue.add("जय मसीह की")
        }

        val verse = if (!todaysVerseText.isNullOrBlank()) todaysVerseText.trim() else "परमेश्वर का वचन ही जीवन का मार्ग है।"
        speechQueue.add("आज का वचन है। $verse")

        executeSpeechQueue(speechQueue)
    }

    private fun executeSpeechQueue(messages: List<String>) {
        if (messages.isEmpty() || tts == null) return

        tts?.stop()
        messages.forEachIndexed { index, message ->
            val queueMode = if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            tts?.speak(message, queueMode, null, "welcome_speech_$index")
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
