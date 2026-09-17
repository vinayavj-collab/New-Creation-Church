package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.data.bible.model.BibleVerse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class AudioSourceType(val displayNameHindi: String, val displayNameEnglish: String) {
    PRE_RECORDED("पूर्व-रिकॉर्डेड MP3", "Pre-recorded MP3"),
    TTS_NARRATION("वॉइस वाचन (TTS)", "Speech Narration (TTS)")
}

class BibleAudioManager private constructor(private val context: Context) : TextToSpeech.OnInitListener {

    companion object {
        @Volatile
        private var instance: BibleAudioManager? = null

        fun getInstance(context: Context): BibleAudioManager {
            return instance ?: synchronized(this) {
                instance ?: BibleAudioManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private var mediaPlayer: MediaPlayer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _hasActiveSession = MutableStateFlow(false)
    val hasActiveSession: StateFlow<Boolean> = _hasActiveSession.asStateFlow()

    private val _currentBookIdFlow = MutableStateFlow(43) // Default John
    val currentBookIdFlow: StateFlow<Int> = _currentBookIdFlow.asStateFlow()

    private val _currentChapterFlow = MutableStateFlow(1)
    val currentChapterFlow: StateFlow<Int> = _currentChapterFlow.asStateFlow()

    private val _currentBookName = MutableStateFlow("पवित्र बाइबिल")
    val currentBookName: StateFlow<String> = _currentBookName.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _currentVerseNumber = MutableStateFlow<Int?>(null)
    val currentVerseNumber: StateFlow<Int?> = _currentVerseNumber.asStateFlow()

    private val _audioLanguage = MutableStateFlow("hi") // "hi" or "en"
    val audioLanguage: StateFlow<String> = _audioLanguage.asStateFlow()

    private val _audioSourceType = MutableStateFlow(AudioSourceType.TTS_NARRATION)
    val audioSourceType: StateFlow<AudioSourceType> = _audioSourceType.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    // Settings Configuration State
    private var smartStartEnabled: Boolean = true
    private var backgroundPlayEnabled: Boolean = true
    private var sleepTimerMinutes: Int = 0
    private var sleepChapterCount: Int = 0
    private var devotionalBgmEnabled: Boolean = false
    private var ttsVolume: Float = 1.0f
    private var bgmVolume: Float = 0.5f
    private var selectedBgmTrackId: String = "peaceful_morning"
    private var customBgmUri: String = ""

    // Sleep tracking counters
    private var chaptersPlayedInSession: Int = 0
    private var currentBookId: Int = 43
    private var currentChapterNumber: Int = 1

    private val handler = Handler(Looper.getMainLooper())
    private var progressRunnable: Runnable? = null
    private var sleepTimerRunnable: Runnable? = null

    private var currentVerseList: List<BibleVerse> = emptyList()
    private var currentTtsVerseIndex = 0

    // Callback for auto-advancing to next chapter when a chapter finishes
    var onChapterCompletedListener: ((nextChapterNumber: Int) -> Unit)? = null

    init {
        textToSpeech = TextToSpeech(context.applicationContext, this)
        setupProgressTracker()
    }

    fun setBookDetails(bookId: Int, chapter: Int, bookName: String) {
        currentBookId = bookId
        currentChapterNumber = chapter
        _currentBookIdFlow.value = bookId
        _currentChapterFlow.value = chapter
        if (bookName.isNotBlank()) {
            _currentBookName.value = bookName
        }
    }

    fun configureAudioSettings(
        smartStart: Boolean,
        backgroundPlay: Boolean,
        sleepMinutes: Int,
        sleepChapters: Int,
        enableBgm: Boolean,
        ttsVol: Float,
        bgmVol: Float,
        bgmTrackId: String,
        customUri: String = ""
    ) {
        val oldSleepMinutes = sleepTimerMinutes
        smartStartEnabled = smartStart
        backgroundPlayEnabled = backgroundPlay
        sleepTimerMinutes = sleepMinutes
        sleepChapterCount = sleepChapters
        devotionalBgmEnabled = enableBgm
        ttsVolume = ttsVol
        bgmVolume = bgmVol
        selectedBgmTrackId = bgmTrackId
        customBgmUri = customUri

        // If sleep timer minutes changed while playing, restart timer
        if (_isPlaying.value && oldSleepMinutes != sleepMinutes) {
            scheduleSleepTimer()
        }

        // If background play setting was turned off while playing, stop foreground service
        if (!backgroundPlay && _isPlaying.value) {
            BibleAudioForegroundService.stopService(context)
        } else if (backgroundPlay && _isPlaying.value) {
            updateForegroundService()
        }

        // Synchronize BGM state
        if (_isPlaying.value) {
            DevotionalBgmManager.startOrUpdateBgm(
                context = context,
                enabled = devotionalBgmEnabled,
                trackId = selectedBgmTrackId,
                volume = bgmVolume,
                customUri = customBgmUri
            )
        } else {
            DevotionalBgmManager.stopBgm()
        }
    }

    private fun scheduleSleepTimer() {
        sleepTimerRunnable?.let { handler.removeCallbacks(it) }
        sleepTimerRunnable = null

        if (sleepTimerMinutes > 0) {
            sleepTimerRunnable = Runnable {
                Log.d("BibleAudioManager", "Sleep timer fired after $sleepTimerMinutes minutes. Stopping audio.")
                stop()
            }
            handler.postDelayed(sleepTimerRunnable!!, sleepTimerMinutes * 60 * 1000L)
        }
    }

    private fun cancelSleepTimer() {
        sleepTimerRunnable?.let { handler.removeCallbacks(it) }
        sleepTimerRunnable = null
    }

    private fun updateForegroundService() {
        if (!backgroundPlayEnabled) return

        val bName = _currentBookName.value.ifBlank {
            if (_audioLanguage.value == "en") "Book $currentBookId" else "पवित्र बाइबिल"
        }
        val bookTitle = "$bName - अध्याय $currentChapterNumber"
        val vNum = _currentVerseNumber.value
        val subtitle = if (vNum != null) "वचन $vNum वाचन..." else "ऑडियो वाचन जारी है..."

        BibleAudioForegroundService.updateNotification(
            context = context,
            title = bookTitle,
            subtitle = subtitle,
            isPlaying = _isPlaying.value
        )
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsReady = true
            setTtsLanguage(_audioLanguage.value)
            textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isPlaying.value = true
                    _isBuffering.value = false
                    utteranceId?.toIntOrNull()?.let { vNum ->
                        _currentVerseNumber.value = vNum
                        if (backgroundPlayEnabled) {
                            handler.post { updateForegroundService() }
                        }
                    }
                }

                override fun onDone(utteranceId: String?) {
                    handler.post {
                        playNextTtsVerse()
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    handler.post {
                        _isPlaying.value = false
                        _isBuffering.value = false
                        if (backgroundPlayEnabled) {
                            BibleAudioForegroundService.stopService(context)
                        }
                    }
                }
            })
        } else {
            Log.e("BibleAudioManager", "TTS Initialization failed")
        }
    }

    private fun setTtsLanguage(langCode: String) {
        if (!isTtsReady) return
        val locale = if (langCode == "en") Locale.US else Locale("hi", "IN")
        val result = textToSpeech?.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w("BibleAudioManager", "TTS Language $langCode not supported directly, using default")
        }
    }

    fun setAudioLanguage(langCode: String) {
        if (_audioLanguage.value != langCode) {
            _audioLanguage.value = langCode
            setTtsLanguage(langCode)
            if (_isPlaying.value) {
                stop()
            }
        }
    }

    fun setAudioSourceType(type: AudioSourceType) {
        if (_audioSourceType.value != type) {
            _audioSourceType.value = type
            if (_isPlaying.value) {
                stop()
            }
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                try {
                    player.playbackParams = player.playbackParams.setSpeed(speed)
                } catch (e: Exception) {
                    Log.e("BibleAudioManager", "Failed to set playback speed on MediaPlayer", e)
                }
            }
        }
        textToSpeech?.setSpeechRate(speed)
    }

    fun playChapterAudio(bookId: Int, chapter: Int, verses: List<BibleVerse>, explicitTargetVerse: Int? = null) {
        currentBookId = bookId
        currentChapterNumber = chapter
        _currentBookIdFlow.value = bookId
        _currentChapterFlow.value = chapter
        _hasActiveSession.value = true
        currentVerseList = verses.sortedBy { it.verseNumber }

        // Start Sleep Timer if configured
        scheduleSleepTimer()

        // Smart Start evaluation: if Smart Start is ON, use explicitTargetVerse or active verse if available
        val activeV = explicitTargetVerse ?: _currentVerseNumber.value
        val startIndex = if (smartStartEnabled && activeV != null) {
            val idx = currentVerseList.indexOfFirst { it.verseNumber == activeV }
            if (idx >= 0) idx else 0
        } else {
            0
        }

        if (_audioSourceType.value == AudioSourceType.PRE_RECORDED) {
            val totalVerses = currentVerseList.size.coerceAtLeast(1)
            val estimatedFraction = (startIndex.toFloat() / totalVerses.toFloat()).coerceIn(0f, 0.95f)
            playPreRecordedChapter(bookId, chapter, initialOffsetFraction = estimatedFraction)
        } else {
            startTtsNarration(startIndex)
        }

        if (backgroundPlayEnabled) {
            updateForegroundService()
        }

        if (devotionalBgmEnabled) {
            DevotionalBgmManager.startOrUpdateBgm(
                context = context,
                enabled = true,
                trackId = selectedBgmTrackId,
                volume = bgmVolume,
                customUri = customBgmUri
            )
        }
    }

    fun playFromVerse(targetVerseNum: Int, bookId: Int, chapter: Int, verses: List<BibleVerse>) {
        currentBookId = bookId
        currentChapterNumber = chapter
        _currentBookIdFlow.value = bookId
        _currentChapterFlow.value = chapter
        _hasActiveSession.value = true
        currentVerseList = verses.sortedBy { it.verseNumber }
        val verseIndex = currentVerseList.indexOfFirst { it.verseNumber == targetVerseNum }
        _currentVerseNumber.value = targetVerseNum

        // Start Sleep Timer if configured
        scheduleSleepTimer()

        if (_audioSourceType.value == AudioSourceType.TTS_NARRATION) {
            startTtsNarration(if (verseIndex >= 0) verseIndex else 0)
        } else {
            val totalVerses = currentVerseList.size.coerceAtLeast(1)
            val vIdx = if (verseIndex >= 0) verseIndex else (targetVerseNum - 1).coerceAtLeast(0)
            val estimatedFraction = (vIdx.toFloat() / totalVerses.toFloat()).coerceIn(0f, 0.95f)

            if (mediaPlayer == null || !_isPlaying.value) {
                playPreRecordedChapter(bookId, chapter, initialOffsetFraction = estimatedFraction)
            } else {
                val dur = _durationMs.value
                if (dur > 0) {
                    seekTo((dur * estimatedFraction).toLong())
                }
            }
        }

        if (backgroundPlayEnabled) {
            updateForegroundService()
        }

        if (devotionalBgmEnabled) {
            DevotionalBgmManager.startOrUpdateBgm(
                context = context,
                enabled = true,
                trackId = selectedBgmTrackId,
                volume = bgmVolume,
                customUri = customBgmUri
            )
        }
    }

    private fun playPreRecordedChapter(bookId: Int, chapter: Int, initialOffsetFraction: Float = 0f) {
        stopMediaPlayer()
        _isBuffering.value = true

        val langFolder = if (_audioLanguage.value == "en") "ENGKJV" else "HIOV"
        val mp3Url = "https://bolls.life/static/audio/$langFolder/$bookId/$chapter.mp3"

        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(mp3Url)
                setOnPreparedListener { mp ->
                    _isBuffering.value = false
                    _durationMs.value = mp.duration.toLong()
                    try {
                        mp.playbackParams = mp.playbackParams.setSpeed(_playbackSpeed.value)
                    } catch (e: Exception) {
                        Log.w("BibleAudioManager", "Could not set speed params", e)
                    }
                    if (initialOffsetFraction > 0f) {
                        val seekPos = (mp.duration * initialOffsetFraction).toInt()
                        mp.seekTo(seekPos)
                        _currentPositionMs.value = seekPos.toLong()
                    }
                    mp.start()
                    _isPlaying.value = true
                    if (backgroundPlayEnabled) {
                        updateForegroundService()
                    }
                }
                setOnCompletionListener {
                    _isPlaying.value = false
                    _currentPositionMs.value = _durationMs.value
                    handleChapterCompleted()
                }
                setOnErrorListener { _, _, _ ->
                    _isBuffering.value = false
                    _isPlaying.value = false
                    Log.w("BibleAudioManager", "MP3 stream error, falling back to TTS narration")
                    startTtsNarration(0)
                    true
                }
            }
            mediaPlayer = player
            player.prepareAsync()
        } catch (e: Exception) {
            Log.e("BibleAudioManager", "Error setting up MediaPlayer", e)
            _isBuffering.value = false
            startTtsNarration(0)
        }
    }

    private fun startTtsNarration(startIndex: Int) {
        if (currentVerseList.isEmpty()) return
        stopMediaPlayer()

        currentTtsVerseIndex = startIndex.coerceIn(0, currentVerseList.size - 1)
        speakCurrentTtsVerse()
    }

    private fun speakCurrentTtsVerse() {
        if (currentTtsVerseIndex >= currentVerseList.size) {
            _isPlaying.value = false
            _currentVerseNumber.value = null
            handleChapterCompleted()
            return
        }

        val verse = currentVerseList[currentTtsVerseIndex]
        _currentVerseNumber.value = verse.verseNumber

        val rawTextToSpeak = if (_audioLanguage.value == "en" && !verse.secondaryText.isNullOrBlank()) {
            "Verse ${verse.verseNumber}. ${verse.secondaryText}"
        } else {
            "वचन ${verse.verseNumber}. ${verse.text}"
        }
        val textToSpeak = ScriptureSpeechUtils.formatScriptureTextForSpeech(rawTextToSpeak)

        textToSpeech?.setSpeechRate(_playbackSpeed.value)
        setTtsLanguage(_audioLanguage.value)

        _isPlaying.value = true
        _isBuffering.value = false

        textToSpeech?.speak(
            textToSpeak,
            TextToSpeech.QUEUE_FLUSH,
            null,
            verse.verseNumber.toString()
        )
    }

    private fun playNextTtsVerse() {
        currentTtsVerseIndex++
        if (currentTtsVerseIndex < currentVerseList.size) {
            speakCurrentTtsVerse()
        } else {
            _isPlaying.value = false
            _currentVerseNumber.value = null
            handleChapterCompleted()
        }
    }

    private fun handleChapterCompleted() {
        chaptersPlayedInSession++

        // Check if sleep timer based on chapters has been reached
        if (sleepChapterCount > 0 && chaptersPlayedInSession >= sleepChapterCount) {
            Log.d("BibleAudioManager", "Sleep timer reached chapter limit of $sleepChapterCount chapters. Stopping audio.")
            stop()
            return
        }

        // Notify listener if configured to auto-advance
        onChapterCompletedListener?.invoke(currentChapterNumber + 1)
    }

    fun togglePlayPause() {
        playPause()
    }

    fun playPause() {
        if (_audioSourceType.value == AudioSourceType.PRE_RECORDED) {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.pause()
                    _isPlaying.value = false
                    DevotionalBgmManager.pauseBgm()
                    if (backgroundPlayEnabled) updateForegroundService()
                } else {
                    player.start()
                    _isPlaying.value = true
                    if (devotionalBgmEnabled) DevotionalBgmManager.resumeBgm()
                    if (backgroundPlayEnabled) updateForegroundService()
                }
            } ?: run {
                if (currentVerseList.isNotEmpty()) {
                    playPreRecordedChapter(currentVerseList.first().bookId, currentVerseList.first().chapter)
                }
            }
        } else {
            if (_isPlaying.value) {
                textToSpeech?.stop()
                _isPlaying.value = false
                DevotionalBgmManager.pauseBgm()
                if (backgroundPlayEnabled) updateForegroundService()
            } else {
                speakCurrentTtsVerse()
                if (devotionalBgmEnabled) DevotionalBgmManager.resumeBgm()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.let { player ->
            try {
                player.seekTo(positionMs.toInt())
                _currentPositionMs.value = positionMs
            } catch (e: Exception) {
                Log.e("BibleAudioManager", "Seek failed", e)
            }
        }
    }

    fun skipNextVerse() {
        if (_audioSourceType.value == AudioSourceType.TTS_NARRATION) {
            currentTtsVerseIndex = (currentTtsVerseIndex + 1).coerceAtMost(currentVerseList.size - 1)
            speakCurrentTtsVerse()
        } else {
            mediaPlayer?.let { player ->
                val newPos = (player.currentPosition + 15000).coerceAtMost(player.duration)
                seekTo(newPos.toLong())
            }
        }
    }

    fun skipPreviousVerse() {
        if (_audioSourceType.value == AudioSourceType.TTS_NARRATION) {
            currentTtsVerseIndex = (currentTtsVerseIndex - 1).coerceAtLeast(0)
            speakCurrentTtsVerse()
        } else {
            mediaPlayer?.let { player ->
                val newPos = (player.currentPosition - 15000).coerceAtLeast(0)
                seekTo(newPos.toLong())
            }
        }
    }

    fun setCurrentVerseNumber(vNum: Int?) {
        _currentVerseNumber.value = vNum
        if (vNum != null && currentVerseList.isNotEmpty()) {
            val idx = currentVerseList.indexOfFirst { it.verseNumber == vNum }
            if (idx >= 0) {
                currentTtsVerseIndex = idx
            }
        }
    }

    fun stop() {
        stopMediaPlayer()
        if (isTtsReady) {
            textToSpeech?.stop()
        }
        _isPlaying.value = false
        _hasActiveSession.value = false
        _isBuffering.value = false
        _currentPositionMs.value = 0L
        cancelSleepTimer()

        // Stop Devotional BGM
        DevotionalBgmManager.stopBgm()

        // Stop foreground service
        BibleAudioForegroundService.stopService(context)
    }

    private fun stopMediaPlayer() {
        mediaPlayer?.let { player ->
            try {
                if (player.isPlaying) {
                    player.stop()
                }
                player.release()
            } catch (e: Exception) {
                Log.e("BibleAudioManager", "Error stopping MediaPlayer", e)
            }
        }
        mediaPlayer = null
    }

    private fun setupProgressTracker() {
        progressRunnable = object : Runnable {
            override fun run() {
                mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        _currentPositionMs.value = player.currentPosition.toLong()
                        _durationMs.value = player.duration.toLong()
                    }
                }
                handler.postDelayed(this, 500)
            }
        }
        handler.post(progressRunnable!!)
    }

    fun release() {
        stop()
        progressRunnable?.let { handler.removeCallbacks(it) }
        textToSpeech?.shutdown()
        textToSpeech = null
    }
}
