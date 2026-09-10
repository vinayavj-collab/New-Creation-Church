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

class BibleAudioManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var mediaPlayer: MediaPlayer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _currentVerseNumber = MutableStateFlow<Int?>(null)
    val currentVerseNumber: StateFlow<Int?> = _currentVerseNumber.asStateFlow()

    private val _audioLanguage = MutableStateFlow("hi") // "hi" or "en"
    val audioLanguage: StateFlow<String> = _audioLanguage.asStateFlow()

    private val _audioSourceType = MutableStateFlow(AudioSourceType.PRE_RECORDED)
    val audioSourceType: StateFlow<AudioSourceType> = _audioSourceType.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val handler = Handler(Looper.getMainLooper())
    private var progressRunnable: Runnable? = null

    private var currentVerseList: List<BibleVerse> = emptyList()
    private var currentTtsVerseIndex = 0

    init {
        textToSpeech = TextToSpeech(context.applicationContext, this)
        setupProgressTracker()
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

    fun playChapterAudio(bookId: Int, chapter: Int, verses: List<BibleVerse>) {
        currentVerseList = verses.sortedBy { it.verseNumber }
        if (_audioSourceType.value == AudioSourceType.PRE_RECORDED) {
            playPreRecordedChapter(bookId, chapter)
        } else {
            startTtsNarration(0)
        }
    }

    fun playFromVerse(targetVerseNum: Int, bookId: Int, chapter: Int, verses: List<BibleVerse>) {
        currentVerseList = verses.sortedBy { it.verseNumber }
        val verseIndex = currentVerseList.indexOfFirst { it.verseNumber == targetVerseNum }
        _currentVerseNumber.value = targetVerseNum

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
    }

    private fun playPreRecordedChapter(bookId: Int, chapter: Int, initialOffsetFraction: Float = 0f) {
        stop()
        _isBuffering.value = true

        val langFolder = if (_audioLanguage.value == "en") "ENGKJV" else "HIOV"
        // Public domain MP3 audio source for Bible chapters
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
                }
                setOnCompletionListener {
                    _isPlaying.value = false
                    _currentPositionMs.value = _durationMs.value
                }
                setOnErrorListener { _, _, _ ->
                    _isBuffering.value = false
                    _isPlaying.value = false
                    // Fallback to TTS narration if MP3 stream is unavailable or network error
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
            return
        }

        val verse = currentVerseList[currentTtsVerseIndex]
        _currentVerseNumber.value = verse.verseNumber

        val textToSpeak = if (_audioLanguage.value == "en" && !verse.secondaryText.isNullOrBlank()) {
            "Verse ${verse.verseNumber}. ${verse.secondaryText}"
        } else {
            "वचन ${verse.verseNumber}. ${verse.text}"
        }

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
        }
    }

    fun playPause() {
        if (_audioSourceType.value == AudioSourceType.PRE_RECORDED) {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.pause()
                    _isPlaying.value = false
                } else {
                    player.start()
                    _isPlaying.value = true
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
            } else {
                speakCurrentTtsVerse()
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

    fun stop() {
        stopMediaPlayer()
        if (isTtsReady) {
            textToSpeech?.stop()
        }
        _isPlaying.value = false
        _isBuffering.value = false
        _currentPositionMs.value = 0L
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
