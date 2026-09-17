package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BgmTrack(
    val id: String,
    val nameHindi: String,
    val nameEnglish: String,
    val categoryHindi: String,
    val descriptionHindi: String,
    val streamUrl: String
)

object DevotionalBgmManager {
    private var bgmPlayer: MediaPlayer? = null
    private var isPlayingBgm = false
    private var currentTrackId: String = "peaceful_morning"
    private var currentCustomUri: String = ""
    private var currentVolume: Float = 0.4f

    private val _previewingTrackId = MutableStateFlow<String?>(null)
    val previewingTrackId: StateFlow<String?> = _previewingTrackId.asStateFlow()

    val availableTracks = listOf(
        // Category 1: Piano & Strings
        BgmTrack(
            id = "peaceful_morning",
            nameHindi = "शांत प्रातःकाल (Peaceful Piano)",
            nameEnglish = "Peaceful Morning Piano",
            categoryHindi = "पियानो एवं तार (Piano & Strings)",
            descriptionHindi = "सुबह की प्रार्थना एवं शांत अध्ययन हेतु सुमधुर पियानो",
            streamUrl = "https://cdn.pixabay.com/download/audio/2022/05/27/audio_1808fbf07a.mp3"
        ),
        BgmTrack(
            id = "gentle_worship",
            nameHindi = "मधुर आराधना (Gentle Worship Strings)",
            nameEnglish = "Gentle Worship Strings",
            categoryHindi = "पियानो एवं तार (Piano & Strings)",
            descriptionHindi = "हृदयस्पर्शी वॉयलिन एवं स्ट्रिंग्स की आराधना धुन",
            streamUrl = "https://cdn.pixabay.com/download/audio/2022/01/18/audio_d0a13f69d2.mp3"
        ),
        BgmTrack(
            id = "holy_spirit_peace",
            nameHindi = "पवित्र आत्मा की शांति (Acoustic Meditation)",
            nameEnglish = "Holy Spirit Peace Guitar",
            categoryHindi = "पियानो एवं तार (Piano & Strings)",
            descriptionHindi = "शांत और सरल ध्वनिक गिटार की मनमोहक शांत धुन",
            streamUrl = "https://cdn.pixabay.com/download/audio/2022/11/06/audio_c07223b2c2.mp3"
        ),
        BgmTrack(
            id = "serene_cello",
            nameHindi = "गंभीर आत्मिक स्तुति (Serene Cello & Harmony)",
            nameEnglish = "Serene Cello & Harmony",
            categoryHindi = "पियानो एवं तार (Piano & Strings)",
            descriptionHindi = "गहरी भक्ति में प्रभु के निकट आने हेतु सेलो धुन",
            streamUrl = "https://cdn.pixabay.com/download/audio/2022/01/26/audio_d0a13f69d2.mp3"
        ),

        // Category 2: Sanctuary & Prayer
        BgmTrack(
            id = "prayer_ambient",
            nameHindi = "प्रार्थना वातावरण (Deep Prayer Sanctuary)",
            nameEnglish = "Deep Prayer Sanctuary",
            categoryHindi = "प्रार्थना एवं आराधना (Sanctuary & Prayer)",
            descriptionHindi = "एकाग्र मन से प्रभु की उपस्थिति में लीन होने हेतु",
            streamUrl = "https://cdn.pixabay.com/download/audio/2022/10/14/audio_9939f77cb7.mp3"
        ),
        BgmTrack(
            id = "cathedral_glory",
            nameHindi = "स्तुति और महिमा (Cathedral Choirs & Strings)",
            nameEnglish = "Cathedral Worship Choirs",
            categoryHindi = "प्रार्थना एवं आराधना (Sanctuary & Prayer)",
            descriptionHindi = "पवित्र स्थान की भव्यता और आत्मिक आराधना",
            streamUrl = "https://cdn.pixabay.com/download/audio/2022/03/15/audio_c8c8a73467.mp3"
        ),
        BgmTrack(
            id = "night_rest",
            nameHindi = "रात्रि विश्राम व अनुग्रह (Night Rest & Grace)",
            nameEnglish = "Lullaby of Grace",
            categoryHindi = "प्रार्थना एवं आराधना (Sanctuary & Prayer)",
            descriptionHindi = "रात्रि अध्ययन व विश्राम के लिए शांत लोरी",
            streamUrl = "https://cdn.pixabay.com/download/audio/2022/05/16/audio_db607386d1.mp3"
        ),

        // Category 3: Harp, Flute & Nature
        BgmTrack(
            id = "heavenly_harp",
            nameHindi = "स्वर्गीय वीणा एवं बांसुरी (Heavenly Harp & Flute)",
            nameEnglish = "Heavenly Harp & Flute",
            categoryHindi = "वीणा, बांसुरी व प्रकृति (Harp, Flute & Nature)",
            descriptionHindi = "दाऊद की वीणा के समान स्वर्गीय व शांतिदायक स्वर",
            streamUrl = "https://cdn.pixabay.com/download/audio/2021/08/04/audio_12b0c7443c.mp3"
        ),
        BgmTrack(
            id = "morning_star",
            nameHindi = "भोर का तारा (Morning Star Flute & Pads)",
            nameEnglish = "Morning Star Flute",
            categoryHindi = "वीणा, बांसुरी व प्रकृति (Harp, Flute & Nature)",
            descriptionHindi = "दिन की शुरुआत में आत्मिक ऊर्जा व ताज़गी",
            streamUrl = "https://cdn.pixabay.com/download/audio/2022/08/02/audio_884fe92c21.mp3"
        ),
        BgmTrack(
            id = "graceful_ocean",
            nameHindi = "प्रभु का अनुग्रह (Ocean Waves & Piano)",
            nameEnglish = "Graceful Ocean & Piano",
            categoryHindi = "वीणा, बांसुरी व प्रकृति (Harp, Flute & Nature)",
            descriptionHindi = "शांत सागर की लहरों के साथ ध्यानमयी पियानो",
            streamUrl = "https://cdn.pixabay.com/download/audio/2021/09/06/audio_73e738ff8e.mp3"
        ),
        BgmTrack(
            id = "calm_stream",
            nameHindi = "शांत झरना और बांसुरी (Calm Stream & Flute)",
            nameEnglish = "Calm Stream & Flute",
            categoryHindi = "वीणा, बांसुरी व प्रकृति (Harp, Flute & Nature)",
            descriptionHindi = "प्रकृति की कलकल ध्वनि के साथ बांसुरी का मधुर स्वर",
            streamUrl = "https://cdn.pixabay.com/download/audio/2021/11/24/audio_9b6574fbc0.mp3"
        ),
        BgmTrack(
            id = "temple_bells",
            nameHindi = "आराध्य वीणा व घंटियाँ (Peace Chimes & Strings)",
            nameEnglish = "Peace Chimes & Strings",
            categoryHindi = "वीणा, बांसुरी व प्रकृति (Harp, Flute & Nature)",
            descriptionHindi = "मन को एकाग्र और शांत करने वाली सुखद घंटियाँ",
            streamUrl = "https://cdn.pixabay.com/download/audio/2022/02/07/audio_d3f3f509b5.mp3"
        )
    )

    fun startOrUpdateBgm(
        context: Context,
        enabled: Boolean,
        trackId: String,
        volume: Float,
        customUri: String = ""
    ) {
        currentVolume = volume.coerceIn(0f, 1f)
        _previewingTrackId.value = null

        if (!enabled) {
            stopBgm()
            return
        }

        // Check if same track/URI is already playing
        if (bgmPlayer != null && isPlayingBgm && currentTrackId == trackId && (trackId != "custom_file" && trackId != "custom_url" || currentCustomUri == customUri)) {
            // Just update volume
            bgmPlayer?.setVolume(currentVolume, currentVolume)
            return
        }

        currentTrackId = trackId
        currentCustomUri = customUri

        if ((trackId == "custom_file" || trackId == "custom_url") && customUri.isNotBlank()) {
            playCustomSource(context, customUri)
        } else {
            val track = availableTracks.firstOrNull { it.id == trackId } ?: availableTracks.first()
            playTrack(context, track)
        }
    }

    fun togglePreview(context: Context, trackId: String, customUri: String = "", volume: Float = 0.5f) {
        if (_previewingTrackId.value == trackId && isPlayingBgm) {
            stopPreview()
        } else {
            _previewingTrackId.value = trackId
            currentVolume = volume.coerceIn(0f, 1f)
            currentTrackId = trackId
            currentCustomUri = customUri

            if ((trackId == "custom_file" || trackId == "custom_url") && customUri.isNotBlank()) {
                playCustomSource(context, customUri)
            } else {
                val track = availableTracks.firstOrNull { it.id == trackId } ?: availableTracks.first()
                playTrack(context, track)
            }
        }
    }

    fun stopPreview() {
        _previewingTrackId.value = null
        stopBgm()
    }

    private fun playCustomSource(context: Context, uriString: String) {
        stopBgmInternal()
        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                if (uriString.startsWith("http://", ignoreCase = true) || uriString.startsWith("https://", ignoreCase = true)) {
                    setDataSource(uriString)
                } else {
                    val uri = Uri.parse(uriString)
                    setDataSource(context, uri)
                }
                isLooping = true
                setVolume(currentVolume, currentVolume)
                setOnPreparedListener { mp ->
                    mp.start()
                    isPlayingBgm = true
                }
                setOnErrorListener { _, what, extra ->
                    Log.w("DevotionalBgmManager", "Custom BGM Player error: $what, $extra")
                    isPlayingBgm = false
                    _previewingTrackId.value = null
                    true
                }
            }
            bgmPlayer = player
            player.prepareAsync()
        } catch (e: Exception) {
            Log.e("DevotionalBgmManager", "Error playing custom BGM source $uriString: ${e.localizedMessage}")
            isPlayingBgm = false
            _previewingTrackId.value = null
        }
    }

    private fun playTrack(context: Context, track: BgmTrack) {
        stopBgmInternal()

        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(track.streamUrl)
                isLooping = true
                setVolume(currentVolume, currentVolume)
                setOnPreparedListener { mp ->
                    mp.start()
                    isPlayingBgm = true
                }
                setOnErrorListener { _, what, extra ->
                    Log.w("DevotionalBgmManager", "BGM Player error: $what, $extra")
                    isPlayingBgm = false
                    _previewingTrackId.value = null
                    true
                }
            }
            bgmPlayer = player
            player.prepareAsync()
        } catch (e: Exception) {
            Log.e("DevotionalBgmManager", "Error preparing BGM: ${e.localizedMessage}")
            isPlayingBgm = false
            _previewingTrackId.value = null
        }
    }

    fun pauseBgm() {
        try {
            if (bgmPlayer?.isPlaying == true) {
                bgmPlayer?.pause()
            }
        } catch (e: Exception) {
            Log.w("DevotionalBgmManager", "Error pausing BGM", e)
        }
    }

    fun resumeBgm() {
        try {
            if (bgmPlayer != null && !isPlayingBgm) {
                bgmPlayer?.start()
                isPlayingBgm = true
            }
        } catch (e: Exception) {
            Log.w("DevotionalBgmManager", "Error resuming BGM", e)
        }
    }

    fun setVolume(volume: Float) {
        currentVolume = volume.coerceIn(0f, 1f)
        try {
            bgmPlayer?.setVolume(currentVolume, currentVolume)
        } catch (e: Exception) {
            Log.w("DevotionalBgmManager", "Error setting BGM volume", e)
        }
    }

    private fun stopBgmInternal() {
        try {
            bgmPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            Log.w("DevotionalBgmManager", "Error stopping internal BGM", e)
        }
        bgmPlayer = null
        isPlayingBgm = false
    }

    fun stopBgm() {
        _previewingTrackId.value = null
        stopBgmInternal()
    }
}

