package com.example.util

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Model representing a background music or prayer ambient audio track/source.
 */
data class BackgroundMusicTrack(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val url: String, // HTTP/HTTPS audio stream URL or builtin identifier
    val category: String = "प्रार्थना (Prayer)", // "प्रार्थना (Prayer)", "आराधना (Worship)", "शांत ध्यान (Meditation)", "सामान्य (General)"
    val isPrayerSpecific: Boolean = true,
    val addedBy: String = "Master Admin",
    val addedAt: Long = System.currentTimeMillis(),
    val isDefault: Boolean = false
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("title", title)
            put("url", url)
            put("category", category)
            put("isPrayerSpecific", isPrayerSpecific)
            put("addedBy", addedBy)
            put("addedAt", addedAt)
            put("isDefault", isDefault)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): BackgroundMusicTrack {
            return BackgroundMusicTrack(
                id = json.optString("id", UUID.randomUUID().toString()),
                title = json.optString("title", "Untitled Track"),
                url = json.optString("url", ""),
                category = json.optString("category", "प्रार्थना (Prayer)"),
                isPrayerSpecific = json.optBoolean("isPrayerSpecific", true),
                addedBy = json.optString("addedBy", "Master Admin"),
                addedAt = json.optLong("addedAt", System.currentTimeMillis()),
                isDefault = json.optBoolean("isDefault", false)
            )
        }
    }
}

/**
 * Singleton manager for Background Music and Prayer Ambient Audio library.
 * Allows Master Admin to add, update, delete links and playlist sources,
 * and handles seamless looped playback across the application.
 */
object BackgroundMusicManager {
    private const val TAG = "BackgroundMusicManager"
    private const val PREFS_NAME = "bg_music_library_prefs"
    private const val KEY_TRACKS = "saved_music_tracks"
    private const val KEY_SELECTED_PRAYER = "selected_prayer_track_id"
    private const val KEY_SELECTED_GENERAL = "selected_general_track_id"

    const val BUILTIN_SYNTH_URL = "builtin://ambient_synth"
    const val DEFAULT_SYNTH_ID = "default_synth_drone"

    private val defaultTracks = listOf(
        BackgroundMusicTrack(
            id = DEFAULT_SYNTH_ID,
            title = "शांत सिम्फनी सिंथेसाइज़र (Default Ambient Synth)",
            url = BUILTIN_SYNTH_URL,
            category = "प्रार्थना (Prayer)",
            isPrayerSpecific = true,
            addedBy = "System",
            isDefault = true
        ),
        BackgroundMusicTrack(
            id = "default_piano_meditation",
            title = "प्रार्थना एवं आराधना पियानो (Peaceful Worship Piano)",
            url = "https://cdn.pixabay.com/download/audio/2022/05/27/audio_1808fbf07a.mp3",
            category = "प्रार्थना (Prayer)",
            isPrayerSpecific = true,
            addedBy = "Master Admin",
            isDefault = false
        ),
        BackgroundMusicTrack(
            id = "default_worship_strings",
            title = "शांतिदायक स्ट्रिंग्स व पैड (Deep Meditative Worship)",
            url = "https://cdn.pixabay.com/download/audio/2022/03/15/audio_c8c8a73467.mp3",
            category = "आराधना (Worship)",
            isPrayerSpecific = false,
            addedBy = "Master Admin",
            isDefault = false
        )
    )

    private val _tracks = MutableStateFlow<List<BackgroundMusicTrack>>(emptyList())
    val tracks: StateFlow<List<BackgroundMusicTrack>> = _tracks.asStateFlow()

    private val _selectedPrayerTrackId = MutableStateFlow(DEFAULT_SYNTH_ID)
    val selectedPrayerTrackId: StateFlow<String> = _selectedPrayerTrackId.asStateFlow()

    private val _selectedGeneralTrackId = MutableStateFlow(DEFAULT_SYNTH_ID)
    val selectedGeneralTrackId: StateFlow<String> = _selectedGeneralTrackId.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentlyPlayingTrack = MutableStateFlow<BackgroundMusicTrack?>(null)
    val currentlyPlayingTrack: StateFlow<BackgroundMusicTrack?> = _currentlyPlayingTrack.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true

        val prefs = getPrefs(context)
        val jsonStr = prefs.getString(KEY_TRACKS, null)

        val loaded = if (!jsonStr.isNullOrBlank()) {
            try {
                val array = JSONArray(jsonStr)
                val list = mutableListOf<BackgroundMusicTrack>()
                for (i in 0 until array.length()) {
                    list.add(BackgroundMusicTrack.fromJson(array.getJSONObject(i)))
                }
                if (list.none { it.id == DEFAULT_SYNTH_ID }) {
                    list.add(0, defaultTracks.first())
                }
                list
            } catch (e: Exception) {
                Log.e(TAG, "Error loading saved tracks", e)
                defaultTracks
            }
        } else {
            defaultTracks
        }

        _tracks.value = loaded
        _selectedPrayerTrackId.value = prefs.getString(KEY_SELECTED_PRAYER, DEFAULT_SYNTH_ID) ?: DEFAULT_SYNTH_ID
        _selectedGeneralTrackId.value = prefs.getString(KEY_SELECTED_GENERAL, DEFAULT_SYNTH_ID) ?: DEFAULT_SYNTH_ID
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun persist(context: Context) {
        try {
            val array = JSONArray()
            _tracks.value.forEach { array.put(it.toJson()) }
            getPrefs(context).edit()
                .putString(KEY_TRACKS, array.toString())
                .putString(KEY_SELECTED_PRAYER, _selectedPrayerTrackId.value)
                .putString(KEY_SELECTED_GENERAL, _selectedGeneralTrackId.value)
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error persisting tracks", e)
        }
    }

    fun addOrUpdateTrack(context: Context, track: BackgroundMusicTrack) {
        val currentList = _tracks.value.toMutableList()
        val existingIndex = currentList.indexOfFirst { it.id == track.id }
        if (existingIndex >= 0) {
            currentList[existingIndex] = track
        } else {
            currentList.add(track)
        }
        _tracks.value = currentList
        persist(context)
    }

    fun deleteTrack(context: Context, trackId: String): Boolean {
        if (trackId == DEFAULT_SYNTH_ID) return false // Built-in cannot be deleted
        val currentList = _tracks.value.toMutableList()
        val removed = currentList.removeAll { it.id == trackId }
        if (removed) {
            _tracks.value = currentList
            if (_selectedPrayerTrackId.value == trackId) {
                _selectedPrayerTrackId.value = DEFAULT_SYNTH_ID
            }
            if (_selectedGeneralTrackId.value == trackId) {
                _selectedGeneralTrackId.value = DEFAULT_SYNTH_ID
            }
            if (_currentlyPlayingTrack.value?.id == trackId) {
                stopMusic()
            }
            persist(context)
        }
        return removed
    }

    fun setSelectedPrayerTrack(context: Context, trackId: String) {
        _selectedPrayerTrackId.value = trackId
        getPrefs(context).edit().putString(KEY_SELECTED_PRAYER, trackId).apply()
    }

    fun setSelectedGeneralTrack(context: Context, trackId: String) {
        _selectedGeneralTrackId.value = trackId
        getPrefs(context).edit().putString(KEY_SELECTED_GENERAL, trackId).apply()
    }

    fun getSelectedPrayerTrack(): BackgroundMusicTrack {
        return _tracks.value.find { it.id == _selectedPrayerTrackId.value }
            ?: defaultTracks.first()
    }

    fun togglePrayerMusic(context: Context) {
        init(context)
        val selected = getSelectedPrayerTrack()
        if (_isPlaying.value) {
            stopMusic()
        } else {
            playTrack(context, selected)
        }
    }

    fun playTrack(context: Context, track: BackgroundMusicTrack) {
        init(context)
        stopMusic()

        _currentlyPlayingTrack.value = track
        _isPlaying.value = true

        if (track.url == BUILTIN_SYNTH_URL || track.url.isBlank()) {
            AmbientWorshipAudio.getInstance().start()
            return
        }

        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(context.applicationContext, Uri.parse(track.url))
                isLooping = true
                setOnPreparedListener { mp ->
                    try {
                        if (_isPlaying.value) {
                            mp.start()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error starting playback", e)
                    }
                }
                setOnErrorListener { _, what, extra ->
                    Log.w(TAG, "MediaPlayer error: what=$what, extra=$extra. Falling back to ambient synth.")
                    stopMusic()
                    AmbientWorshipAudio.getInstance().start()
                    _isPlaying.value = true
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MediaPlayer for ${track.url}", e)
            AmbientWorshipAudio.getInstance().start()
        }
    }

    fun stopMusic() {
        _isPlaying.value = false
        _currentlyPlayingTrack.value = null

        AmbientWorshipAudio.getInstance().stop()

        try {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) {
                    mp.stop()
                }
                mp.reset()
                mp.release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing media player", e)
        } finally {
            mediaPlayer = null
        }
    }
}
