package com.example.data.repository

import android.util.Log
import com.example.data.bible.model.VerseOfTheDay
import com.example.data.local.PredefinedData
import com.example.data.model.BlogPost
import com.example.data.model.BlogSourceType
import com.example.data.model.YouTubePlaylist
import com.example.data.model.YouTubeVideo
import com.example.data.model.LiveStreamInfo
import com.example.data.model.PrayerRequestItem
import com.example.data.model.PrayerRequestsConfig
import com.example.data.model.FeaturedBannerItem
import com.example.data.model.DailyAudioDevotional
import com.example.data.model.PrayerDeletionAuditLog
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import android.os.Build
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Repository responsible for fetching real-time data from Firebase Realtime Database
 * and smartly merging lists (Blogs, Vlogs, YouTube Videos, Playlists) with local hardcoded datasets,
 * while applying the Single Link Exemption for single string configuration values (like song_spreadsheet_url and today_scripture).
 */
class FirebaseDataRepository private constructor() {

    companion object {
        private const val TAG = "FirebaseDataRepository"

        @Volatile
        private var instance: FirebaseDataRepository? = null

        fun getInstance(): FirebaseDataRepository {
            return instance ?: synchronized(this) {
                instance ?: FirebaseDataRepository().also { instance = it }
            }
        }

        /**
         * Generic list merger respecting:
         * 1. Smart merging of remote items with local hardcoded items.
         * 2. Deduplication by unique ID or URL.
         * 3. Logical sorting: newest remote Firebase items at the top, followed by hardcoded items.
         */
        fun <T> mergeAndDeduplicate(
            remoteItems: List<T>,
            localHardcodedItems: List<T>,
            keySelector: (T) -> String,
            timestampSelector: (T) -> Long = { 0L }
        ): List<T> {
            val seenKeys = mutableSetOf<String>()
            val result = mutableListOf<T>()

            // 1. Sort remote items by newest timestamp descending
            val sortedRemote = remoteItems.sortedByDescending { timestampSelector(it) }
            for (item in sortedRemote) {
                val key = keySelector(item).trim()
                if (key.isNotBlank() && seenKeys.add(key)) {
                    result.add(item)
                }
            }

            // 2. Sort local items by newest timestamp descending
            val sortedLocal = localHardcodedItems.sortedByDescending { timestampSelector(it) }
            for (item in sortedLocal) {
                val key = keySelector(item).trim()
                if (key.isNotBlank() && seenKeys.add(key)) {
                    result.add(item)
                }
            }

            return result
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO)

    // --- 1. Merged Lists (Smart Merged with Local Hardcoded Baseline) ---
    private val _fellowshipBlogs = MutableStateFlow<List<BlogPost>>(PredefinedData.hardcodedFellowshipBlogs)
    val fellowshipBlogs: StateFlow<List<BlogPost>> = _fellowshipBlogs.asStateFlow()

    private val _personalVlogs = MutableStateFlow<List<BlogPost>>(PredefinedData.hardcodedPersonalVlogs)
    val personalVlogs: StateFlow<List<BlogPost>> = _personalVlogs.asStateFlow()

    private val _allBlogs = MutableStateFlow<List<BlogPost>>(
        mergeAndDeduplicate(
            remoteItems = emptyList(),
            localHardcodedItems = PredefinedData.hardcodedFellowshipBlogs + PredefinedData.hardcodedPersonalVlogs,
            keySelector = { it.id.ifBlank { it.url } },
            timestampSelector = { it.publishedTimestamp }
        )
    )
    val allBlogs: StateFlow<List<BlogPost>> = _allBlogs.asStateFlow()

    private val _videos = MutableStateFlow<List<YouTubeVideo>>(PredefinedData.hardcodedVideos)
    val videos: StateFlow<List<YouTubeVideo>> = _videos.asStateFlow()

    private val _playlists = MutableStateFlow<List<YouTubePlaylist>>(PredefinedData.hardcodedPlaylists)
    val playlists: StateFlow<List<YouTubePlaylist>> = _playlists.asStateFlow()

    private val _fellowshipEvents = MutableStateFlow<List<com.example.data.model.FellowshipEvent>>(PredefinedData.hardcodedFellowshipEvents)
    val fellowshipEvents: StateFlow<List<com.example.data.model.FellowshipEvent>> = _fellowshipEvents.asStateFlow()

    private val _eventRsvps = MutableStateFlow<Map<String, List<com.example.data.model.EventRsvp>>>(emptyMap())
    val eventRsvps: StateFlow<Map<String, List<com.example.data.model.EventRsvp>>> = _eventRsvps.asStateFlow()

    // --- 2. Single String Values & Dynamic Remote Security (Remote overrides local fallback directly) ---
    private val _songSpreadsheetUrl = MutableStateFlow(PredefinedData.FALLBACK_SONG_SPREADSHEET_URL)
    val songSpreadsheetUrl: StateFlow<String> = _songSpreadsheetUrl.asStateFlow()

    private val _dailyGreetingConfig = MutableStateFlow(com.example.data.model.DailyGreetingConfig())
    val dailyGreetingConfig: StateFlow<com.example.data.model.DailyGreetingConfig> = _dailyGreetingConfig.asStateFlow()

    private val _todayScripture = MutableStateFlow("")
    val todayScripture: StateFlow<String> = _todayScripture.asStateFlow()

    private val _personalVlogPassword = MutableStateFlow("9479")
    val personalVlogPassword: StateFlow<String> = _personalVlogPassword.asStateFlow()

    private val _isPersonalVlogEnabled = MutableStateFlow(true)
    val isPersonalVlogEnabled: StateFlow<Boolean> = _isPersonalVlogEnabled.asStateFlow()

    private val _privateProfilePassword = MutableStateFlow("Vin@122333")
    val privateProfilePassword: StateFlow<String> = _privateProfilePassword.asStateFlow()

    private val _isPrivateProfileEnabled = MutableStateFlow(true)
    val isPrivateProfileEnabled: StateFlow<Boolean> = _isPrivateProfileEnabled.asStateFlow()

    private val _adminPin = MutableStateFlow("7777")
    val adminPin: StateFlow<String> = _adminPin.asStateFlow()

    private val _quickAccessConfig = MutableStateFlow(com.example.data.model.QuickAccessConfig())
    val quickAccessConfig: StateFlow<com.example.data.model.QuickAccessConfig> = _quickAccessConfig.asStateFlow()

    // --- 3. Live Stream, Prayer Requests, Banners & Daily Audio Devotional ---
    private val _liveStreamInfo = MutableStateFlow(LiveStreamInfo())
    val liveStreamInfo: StateFlow<LiveStreamInfo> = _liveStreamInfo.asStateFlow()

    private val _prayerRequests = MutableStateFlow<List<PrayerRequestItem>>(emptyList())
    val prayerRequests: StateFlow<List<PrayerRequestItem>> = _prayerRequests.asStateFlow()

    private val _prayerRequestsConfig = MutableStateFlow(PrayerRequestsConfig())
    val prayerRequestsConfig: StateFlow<PrayerRequestsConfig> = _prayerRequestsConfig.asStateFlow()

    private val _featuredBanners = MutableStateFlow<List<FeaturedBannerItem>>(emptyList())
    val featuredBanners: StateFlow<List<FeaturedBannerItem>> = _featuredBanners.asStateFlow()

    private val _dailyAudioDevotional = MutableStateFlow<DailyAudioDevotional?>(null)
    val dailyAudioDevotional: StateFlow<DailyAudioDevotional?> = _dailyAudioDevotional.asStateFlow()

    private val _pinnedVideoId = MutableStateFlow<String?>(null)
    val pinnedVideoId: StateFlow<String?> = _pinnedVideoId.asStateFlow()

    private val _videoQuickAccessConfig = MutableStateFlow(com.example.data.model.VideoQuickAccessConfig())
    val videoQuickAccessConfig: StateFlow<com.example.data.model.VideoQuickAccessConfig> = _videoQuickAccessConfig.asStateFlow()

    private val _homeSectionsConfig = MutableStateFlow(com.example.data.model.HomeSectionsConfig())
    val homeSectionsConfig: StateFlow<com.example.data.model.HomeSectionsConfig> = _homeSectionsConfig.asStateFlow()

    init {
        initFirebaseListeners()
    }

    private fun getDefaultScriptureFallback(): String {
        return try {
            val v = VerseOfTheDay.getTodayVerse()
            "${v.textHindi} - ${v.referenceHindi}"
        } catch (e: Exception) {
            "यहोवा मेरा चरवाहा है, मुझे कोई घटी न होगी। - भजन संहिता 23:1"
        }
    }

    private fun initFirebaseListeners() {
        try {
            val database = FirebaseDatabase.getInstance()

            // --- Dynamic Security & Overrides: Personal Vlog Password (Default: 9479) ---
            database.getReference("personal_vlog_password").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val remotePass = snapshot.getValue(String::class.java)?.trim().orEmpty()
                    if (remotePass.isNotBlank()) {
                        Log.i(TAG, "Firebase personal_vlog_password override received successfully")
                        _personalVlogPassword.value = remotePass
                    } else {
                        _personalVlogPassword.value = "9479"
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "personal_vlog_password cancelled: ${error.message}")
                }
            })

            database.getReference("vlog_password").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val remotePass = snapshot.getValue(String::class.java)?.trim().orEmpty()
                    if (remotePass.isNotBlank()) {
                        _personalVlogPassword.value = remotePass
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })

            // --- Dynamic Master Switch: is_vlog_server_enabled / personal_vlog_enabled ---
            database.getReference("is_vlog_server_enabled").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val value = snapshot.getValue(Boolean::class.java)
                        ?: (snapshot.getValue(String::class.java)?.toBoolean())
                    if (value != null) {
                        Log.i(TAG, "Firebase is_vlog_server_enabled override received: $value")
                        _isPersonalVlogEnabled.value = value
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })

            database.getReference("personal_vlog_enabled").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val value = snapshot.getValue(Boolean::class.java)
                        ?: (snapshot.getValue(String::class.java)?.toBoolean())
                    if (value != null) {
                        Log.i(TAG, "Firebase personal_vlog_enabled override received: $value")
                        _isPersonalVlogEnabled.value = value
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })

            // --- Dynamic Security: Private Profile Password (Default: Vin@122333) ---
            database.getReference("private_profile_password").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val remotePass = snapshot.getValue(String::class.java)?.trim().orEmpty()
                    if (remotePass.isNotBlank()) {
                        Log.i(TAG, "Firebase private_profile_password override received successfully")
                        _privateProfilePassword.value = remotePass
                    } else {
                        _privateProfilePassword.value = "Vin@122333"
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "private_profile_password cancelled: ${error.message}")
                }
            })

            database.getReference("profile_b_password").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val remotePass = snapshot.getValue(String::class.java)?.trim().orEmpty()
                    if (remotePass.isNotBlank()) {
                        _privateProfilePassword.value = remotePass
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })

            // --- Dynamic Master Switch: private_profile_enabled ---
            database.getReference("private_profile_enabled").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val value = snapshot.getValue(Boolean::class.java)
                        ?: (snapshot.getValue(String::class.java)?.toBoolean())
                    if (value != null) {
                        Log.i(TAG, "Firebase private_profile_enabled override received: $value")
                        _isPrivateProfileEnabled.value = value
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })

            // --- Dynamic Admin / Authority PIN for Prayer & Admin Actions (Default: 7777) ---
            database.getReference("admin_pin").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val pin = snapshot.getValue(String::class.java)?.trim()
                        ?: snapshot.getValue(Long::class.java)?.toString()
                        ?: ""
                    if (pin.isNotBlank()) {
                        Log.i(TAG, "Firebase admin_pin override received successfully")
                        _adminPin.value = pin
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })

            database.getReference("app_settings").child("admin_pin").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val pin = snapshot.getValue(String::class.java)?.trim()
                        ?: snapshot.getValue(Long::class.java)?.toString()
                        ?: ""
                    if (pin.isNotBlank()) {
                        Log.i(TAG, "Firebase app_settings/admin_pin override received successfully")
                        _adminPin.value = pin
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })

            // --- Dynamic Daily Greeting Text & Time-based Config (Default: "जय मसीह की") ---
            database.getReference("daily_greeting_text").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val greeting = snapshot.getValue(String::class.java)?.trim().orEmpty()
                    if (greeting.isNotBlank()) {
                        Log.i(TAG, "Firebase daily_greeting_text received: $greeting")
                        com.example.util.RemoteConfigHelper.updateDailyGreetingText(greeting)
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })

            database.getReference("daily_greeting_config").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val text = snapshot.child("greetingText").getValue(String::class.java) ?: "जय मसीह की"
                        val isPerm = snapshot.child("isPermanent").getValue(Boolean::class.java) ?: true
                        val durH = snapshot.child("durationHours").getValue(Int::class.java) ?: 0
                        val durD = snapshot.child("durationDays").getValue(Int::class.java) ?: 0
                        val expiresAt = snapshot.child("expiresAtTimestamp").getValue(Long::class.java) ?: 0L
                        val ttlVal = snapshot.child("ttl").getValue(Long::class.java) ?: (if (expiresAt > 0L) expiresAt / 1000L else 0L)
                        val updatedTs = snapshot.child("updatedTimestamp").getValue(Long::class.java) ?: System.currentTimeMillis()
                        val cfg = com.example.data.model.DailyGreetingConfig(
                            greetingText = text,
                            isPermanent = isPerm,
                            durationHours = durH,
                            durationDays = durD,
                            expiresAtTimestamp = expiresAt,
                            ttl = ttlVal,
                            updatedTimestamp = updatedTs
                        )
                        _dailyGreetingConfig.value = cfg
                        if (!cfg.isExpired()) {
                            com.example.util.RemoteConfigHelper.updateDailyGreetingText(cfg.greetingText)
                        } else {
                            com.example.util.RemoteConfigHelper.updateDailyGreetingText("जय मसीह की")
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })

            database.getReference("pinned_video_id").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val vidId = snapshot.getValue(String::class.java)?.trim()
                    _pinnedVideoId.value = if (vidId.isNullOrBlank()) null else vidId
                    Log.i(TAG, "Firebase pinned_video_id updated: ${_pinnedVideoId.value}")
                }
                override fun onCancelled(error: DatabaseError) {}
            })

            database.getReference("quick_access_config").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val config = snapshot.getValue(com.example.data.model.QuickAccessConfig::class.java) ?: com.example.data.model.QuickAccessConfig()
                        _quickAccessConfig.value = config
                        Log.i(TAG, "Firebase quick_access_config received: $config")
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })

            database.getReference("video_quick_access_config").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        try {
                            val isBarVisible = snapshot.child("isBarVisible").getValue(Boolean::class.java) ?: true
                            val itemsSnap = snapshot.child("items")
                            val itemList = mutableListOf<com.example.data.model.VideoQuickAccessItem>()
                            for (child in itemsSnap.children) {
                                val item = child.getValue(com.example.data.model.VideoQuickAccessItem::class.java)
                                if (item != null) {
                                    itemList.add(item)
                                }
                            }
                            val items = if (itemList.isNotEmpty()) {
                                itemList.sortedWith(
                                    compareByDescending<com.example.data.model.VideoQuickAccessItem> { it.isPinned }
                                        .thenBy { it.order }
                                )
                            } else {
                                com.example.data.model.defaultVideoQuickAccessItems()
                            }
                            _videoQuickAccessConfig.value = com.example.data.model.VideoQuickAccessConfig(
                                isBarVisible = isBarVisible,
                                items = items
                            )
                            Log.i(TAG, "Firebase video_quick_access_config received: ${_videoQuickAccessConfig.value}")
                        } catch (e: Exception) {
                            Log.w(TAG, "Error parsing video_quick_access_config: ${e.message}")
                        }
                    } else {
                        _videoQuickAccessConfig.value = com.example.data.model.VideoQuickAccessConfig()
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })

            database.getReference("home_sections_config").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        try {
                            val isCustomLayoutEnabled = snapshot.child("isCustomLayoutEnabled").getValue(Boolean::class.java) ?: true
                            val lastUpdated = snapshot.child("lastUpdated").getValue(Long::class.java) ?: System.currentTimeMillis()
                            val sectionsSnap = snapshot.child("sections")
                            val sectionList = mutableListOf<com.example.data.model.HomeSectionItem>()
                            for (child in sectionsSnap.children) {
                                val item = child.getValue(com.example.data.model.HomeSectionItem::class.java)
                                if (item != null) {
                                    sectionList.add(item)
                                }
                            }
                            val sections = if (sectionList.isNotEmpty()) sectionList else com.example.data.model.defaultHomeSections()
                            _homeSectionsConfig.value = com.example.data.model.HomeSectionsConfig(
                                isCustomLayoutEnabled = isCustomLayoutEnabled,
                                sections = sections,
                                lastUpdated = lastUpdated
                            )
                            Log.i(TAG, "Firebase home_sections_config received with ${sections.size} sections")
                        } catch (e: Exception) {
                            Log.w(TAG, "Error parsing home_sections_config: ${e.message}")
                        }
                    } else {
                        _homeSectionsConfig.value = com.example.data.model.HomeSectionsConfig()
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })

            database.getReference("prayer_admin_pin").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val pin = snapshot.getValue(String::class.java)?.trim()
                        ?: snapshot.getValue(Long::class.java)?.toString()
                        ?: ""
                    if (pin.isNotBlank()) {
                        _adminPin.value = pin
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })

            // --- Single String Value: song_spreadsheet_url (Exempt from list merging) ---
            database.getReference("song_spreadsheet_url").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val remoteUrl = snapshot.getValue(String::class.java)?.trim().orEmpty()
                    if (remoteUrl.isNotBlank()) {
                        Log.i(TAG, "Firebase song_spreadsheet_url override received: $remoteUrl")
                        _songSpreadsheetUrl.value = remoteUrl
                    } else {
                        _songSpreadsheetUrl.value = PredefinedData.FALLBACK_SONG_SPREADSHEET_URL
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "song_spreadsheet_url cancelled: ${error.message}")
                }
            })

            // --- Single String Value: today_scripture (Exempt from list merging) ---
            database.getReference("today_scripture").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val remoteScripture = when {
                        snapshot.hasChild("text") -> {
                            val text = snapshot.child("text").getValue(String::class.java).orEmpty()
                            val ref = snapshot.child("reference").getValue(String::class.java).orEmpty()
                            if (ref.isNotBlank()) "$text - $ref" else text
                        }
                        else -> snapshot.getValue(String::class.java)?.trim().orEmpty()
                    }

                    if (remoteScripture.isNotBlank()) {
                        Log.i(TAG, "Firebase today_scripture override received: $remoteScripture")
                        _todayScripture.value = remoteScripture
                    } else {
                        _todayScripture.value = ""
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "today_scripture cancelled: ${error.message}")
                }
            })

            // --- List 1: Fellowship Blogs ---
            database.getReference("blogs").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val remotePosts = parseBlogSnapshot(snapshot, BlogSourceType.FELLOWSHIP_EVENTS)
                    val merged = mergeAndDeduplicate(
                        remoteItems = remotePosts,
                        localHardcodedItems = PredefinedData.hardcodedFellowshipBlogs,
                        keySelector = { it.id.ifBlank { it.url } },
                        timestampSelector = { it.publishedTimestamp }
                    )
                    _fellowshipBlogs.value = merged
                    updateAllBlogs()
                    Log.i(TAG, "Merged fellowship blogs: total ${merged.size} (remote: ${remotePosts.size})")
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "blogs cancelled: ${error.message}")
                }
            })

            // --- List 2: Personal Vlogs ---
            database.getReference("vlogs").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val remoteVlogs = parseBlogSnapshot(snapshot, BlogSourceType.PERSONAL_VLOG)
                    val merged = mergeAndDeduplicate(
                        remoteItems = remoteVlogs,
                        localHardcodedItems = PredefinedData.hardcodedPersonalVlogs,
                        keySelector = { it.id.ifBlank { it.url } },
                        timestampSelector = { it.publishedTimestamp }
                    )
                    _personalVlogs.value = merged
                    updateAllBlogs()
                    Log.i(TAG, "Merged personal vlogs: total ${merged.size} (remote: ${remoteVlogs.size})")
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "vlogs cancelled: ${error.message}")
                }
            })

            // --- List 3: Firebase & YouTube Videos (Listening to firebase_videos, youtube_videos, videos, custom_videos) ---
            val onVideosSnapshotUpdated = { snapshot: DataSnapshot, sourceNode: String ->
                try {
                    val remoteVideos = parseVideoSnapshot(snapshot)
                    if (remoteVideos.isNotEmpty()) {
                        val currentList = _videos.value
                        val merged = mergeAndDeduplicate(
                            remoteItems = remoteVideos,
                            localHardcodedItems = currentList,
                            keySelector = { it.id.ifBlank { it.videoUrl } },
                            timestampSelector = { it.publishedTimestamp }
                        )
                        _videos.value = merged
                        Log.i(TAG, "[$sourceNode] Merged Firebase videos: total ${merged.size} (node items: ${remoteVideos.size})")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error updating videos from $sourceNode: ${e.message}")
                }
            }

            database.getReference("firebase_videos").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    onVideosSnapshotUpdated(snapshot, "firebase_videos")
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "firebase_videos cancelled: ${error.message}")
                }
            })

            database.getReference("youtube_videos").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    onVideosSnapshotUpdated(snapshot, "youtube_videos")
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "youtube_videos cancelled: ${error.message}")
                }
            })

            database.getReference("videos").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    onVideosSnapshotUpdated(snapshot, "videos")
                }
                override fun onCancelled(error: DatabaseError) {}
            })

            database.getReference("custom_videos").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    onVideosSnapshotUpdated(snapshot, "custom_videos")
                }
                override fun onCancelled(error: DatabaseError) {}
            })

            // --- List 4: Playlists ---
            database.getReference("playlists").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val remotePlaylists = parsePlaylistSnapshot(snapshot)
                    val merged = mergeAndDeduplicate(
                        remoteItems = remotePlaylists,
                        localHardcodedItems = PredefinedData.hardcodedPlaylists,
                        keySelector = { it.id.ifBlank { it.playlistUrl } }
                    )
                    _playlists.value = merged
                    Log.i(TAG, "Merged YouTube playlists: total ${merged.size} (remote: ${remotePlaylists.size})")
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "playlists cancelled: ${error.message}")
                }
            })

            // --- List 5: Fellowship Events (Calendar / Meetings) ---
            database.getReference("fellowship_events").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val remoteEvents = parseEventSnapshot(snapshot)
                    val merged = mergeAndDeduplicate(
                        remoteItems = remoteEvents,
                        localHardcodedItems = PredefinedData.hardcodedFellowshipEvents,
                        keySelector = { it.id },
                        timestampSelector = { it.startTimestamp }
                    )
                    _fellowshipEvents.value = merged
                    Log.i(TAG, "Merged Fellowship Events: total ${merged.size} (remote: ${remoteEvents.size})")
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "fellowship_events cancelled: ${error.message}")
                }
            })

            // Secondary node listener for "events"
            database.getReference("events").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val remoteEvents = parseEventSnapshot(snapshot)
                        if (remoteEvents.isNotEmpty()) {
                            val merged = mergeAndDeduplicate(
                                remoteItems = remoteEvents,
                                localHardcodedItems = PredefinedData.hardcodedFellowshipEvents,
                                keySelector = { it.id },
                                timestampSelector = { it.startTimestamp }
                            )
                            _fellowshipEvents.value = merged
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })

            // --- RSVPs listener: event_rsvps ---
            database.getReference("event_rsvps").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val rsvpMap = mutableMapOf<String, MutableList<com.example.data.model.EventRsvp>>()
                    for (eventSnap in snapshot.children) {
                        val eventId = eventSnap.key ?: continue
                        val list = mutableListOf<com.example.data.model.EventRsvp>()
                        for (userSnap in eventSnap.children) {
                            try {
                                val rsvp = com.example.data.model.EventRsvp(
                                    eventId = eventId,
                                    userName = userSnap.child("userName").getValue(String::class.java).orEmpty(),
                                    userContact = userSnap.child("userContact").getValue(String::class.java).orEmpty(),
                                    status = userSnap.child("status").getValue(String::class.java) ?: "GOING",
                                    attendeesCount = userSnap.child("attendeesCount").getValue(Int::class.java) ?: 1,
                                    timestamp = userSnap.child("timestamp").getValue(Long::class.java) ?: System.currentTimeMillis(),
                                    notes = userSnap.child("notes").getValue(String::class.java).orEmpty()
                                )
                                list.add(rsvp)
                            } catch (e: Exception) {
                                Log.w(TAG, "Error parsing RSVP: ${e.message}")
                            }
                        }
                        rsvpMap[eventId] = list
                    }
                    _eventRsvps.value = rsvpMap
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "event_rsvps cancelled: ${error.message}")
                }
            })

            // --- Live Stream listener: live_stream ---
            database.getReference("live_stream").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val isLive = snapshot.child("is_live").getValue(Boolean::class.java)
                            ?: snapshot.child("isLive").getValue(Boolean::class.java)
                            ?: false
                        val title = snapshot.child("title").getValue(String::class.java) ?: "लाइव आराधना सेवा (Live Worship)"
                        val subtitle = snapshot.child("subtitle").getValue(String::class.java) ?: "अभी जुड़ें और प्रभु की महिमा करें"
                        val url = snapshot.child("url").getValue(String::class.java)
                            ?: snapshot.child("link").getValue(String::class.java)
                            ?: ""
                        val platform = snapshot.child("platform").getValue(String::class.java) ?: "YOUTUBE"
                        val scheduledTime = snapshot.child("scheduled_time").getValue(String::class.java) ?: ""
                        _liveStreamInfo.value = LiveStreamInfo(
                            isLive = isLive && url.isNotBlank(),
                            title = title,
                            subtitle = subtitle,
                            url = url,
                            platform = platform,
                            scheduledTime = scheduledTime
                        )
                    } else {
                        _liveStreamInfo.value = LiveStreamInfo(isLive = false)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "live_stream cancelled: ${error.message}")
                }
            })

            // --- Prayer Requests listener: prayer_requests ---
            database.getReference("prayer_requests").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<PrayerRequestItem>()
                    var fallbackIndex = 1
                    for (child in snapshot.children) {
                        try {
                            val id = child.key ?: ""
                            val serialNumber = child.child("serialNumber").getValue(Int::class.java)
                                ?: child.child("serial").getValue(Int::class.java)
                                ?: fallbackIndex
                            fallbackIndex = maxOf(fallbackIndex, serialNumber) + 1
                            val name = child.child("name").getValue(String::class.java) ?: "विश्वासी"
                            val city = child.child("city").getValue(String::class.java) ?: ""
                            val pastorName = child.child("pastorName").getValue(String::class.java)
                                ?: child.child("pastor").getValue(String::class.java)
                                ?: ""
                            val userRole = child.child("userRole").getValue(String::class.java)
                                ?: child.child("role").getValue(String::class.java)
                                ?: if (pastorName.isNotBlank()) "पास्टर" else "विश्वासी"
                            val isUrgent = child.child("isUrgent").getValue(Boolean::class.java)
                                ?: child.child("urgent").getValue(Boolean::class.java)
                                ?: false
                            val requestText = child.child("requestText").getValue(String::class.java)
                                ?: child.child("request").getValue(String::class.java)
                                ?: child.child("message").getValue(String::class.java)
                                ?: ""
                            val isPrivate = child.child("isPrivate").getValue(Boolean::class.java) ?: false
                            val timestamp = child.child("timestamp").getValue(Long::class.java) ?: System.currentTimeMillis()
                            val prayingCount = child.child("prayingCount").getValue(Int::class.java) ?: 0
                            val senderDeviceId = child.child("senderDeviceId").getValue(String::class.java)
                                ?: child.child("deviceId").getValue(String::class.java)
                                ?: ""
                            val isAnswered = child.child("isAnswered").getValue(Boolean::class.java)
                                ?: child.child("answered").getValue(Boolean::class.java)
                                ?: false
                            val answeredTimestamp = child.child("answeredTimestamp").getValue(Long::class.java) ?: 0L
                            val testimonyText = child.child("testimonyText").getValue(String::class.java)
                                ?: child.child("testimony").getValue(String::class.java)
                                ?: ""
                            val isVerifiedAdmin = child.child("isVerifiedAdmin").getValue(Boolean::class.java) ?: false
                            val adminDesignation = child.child("adminDesignation").getValue(String::class.java) ?: ""
                            val adminName = child.child("adminName").getValue(String::class.java) ?: ""
                            val category = child.child("category").getValue(String::class.java)
                                ?: child.child("topic").getValue(String::class.java)
                                ?: "अन्य"
                            val adminReplyText = child.child("adminReplyText").getValue(String::class.java) ?: ""
                            val adminReplyAuthorName = child.child("adminReplyAuthorName").getValue(String::class.java) ?: ""
                            val adminReplyAuthorDesignation = child.child("adminReplyAuthorDesignation").getValue(String::class.java) ?: ""
                            val adminReplyTimestamp = child.child("adminReplyTimestamp").getValue(Long::class.java) ?: 0L
                            val tagsList = mutableListOf<String>()
                            val tagsChild = child.child("tags")
                            if (tagsChild.exists()) {
                                for (t in tagsChild.children) {
                                    val tagStr = t.getValue(String::class.java)
                                    if (!tagStr.isNullOrBlank()) tagsList.add(tagStr)
                                }
                            }
                            if (tagsList.isEmpty() && category.isNotBlank()) {
                                tagsList.add(category)
                            }

                            if (requestText.isNotBlank()) {
                                list.add(
                                    PrayerRequestItem(
                                        id = id,
                                        serialNumber = serialNumber,
                                        name = name,
                                        city = city,
                                        pastorName = pastorName,
                                        userRole = userRole,
                                        isUrgent = isUrgent,
                                        requestText = requestText,
                                        isPrivate = isPrivate,
                                        timestamp = timestamp,
                                        prayingCount = prayingCount,
                                        senderDeviceId = senderDeviceId,
                                        isAnswered = isAnswered,
                                        answeredTimestamp = answeredTimestamp,
                                        testimonyText = testimonyText,
                                        isVerifiedAdmin = isVerifiedAdmin,
                                        adminDesignation = adminDesignation,
                                        adminName = adminName,
                                        category = category,
                                        tags = tagsList,
                                        adminReplyText = adminReplyText,
                                        adminReplyAuthorName = adminReplyAuthorName,
                                        adminReplyAuthorDesignation = adminReplyAuthorDesignation,
                                        adminReplyTimestamp = adminReplyTimestamp
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Error parsing prayer request: ${e.message}")
                        }
                    }
                    _prayerRequests.value = list.sortedByDescending { it.timestamp }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "prayer_requests cancelled: ${error.message}")
                }
            })

            // --- Prayer Requests Config listener: app_settings/prayer_requests_config ---
            database.getReference("app_settings").child("prayer_requests_config").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val enabled = snapshot.child("enabled").getValue(Boolean::class.java) ?: true
                    val showPastor = snapshot.child("showPastorField").getValue(Boolean::class.java) ?: true
                    val allowTestimonies = snapshot.child("allowTestimonies").getValue(Boolean::class.java) ?: true
                    val allowPublicWall = snapshot.child("allowPublicWall").getValue(Boolean::class.java) ?: true
                    val maxDailyPrayTaps = snapshot.child("maxDailyPrayTapsPerUser").getValue(Int::class.java)
                        ?: snapshot.child("maxDailyTaps").getValue(Int::class.java)
                        ?: 1
                    _prayerRequestsConfig.value = PrayerRequestsConfig(
                        enabled = enabled,
                        showPastorField = showPastor,
                        allowTestimonies = allowTestimonies,
                        allowPublicWall = allowPublicWall,
                        maxDailyPrayTapsPerUser = maxDailyPrayTaps
                    )
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "prayer_requests_config cancelled: ${error.message}")
                }
            })

            // --- Featured Banners listener: featured_banners ---
            database.getReference("featured_banners").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<FeaturedBannerItem>()
                    for (child in snapshot.children) {
                        try {
                            val id = child.key ?: ""
                            val title = child.child("title").getValue(String::class.java) ?: ""
                            val subtitle = child.child("subtitle").getValue(String::class.java) ?: ""
                            val imageUrl = child.child("imageUrl").getValue(String::class.java)
                                ?: child.child("image").getValue(String::class.java)
                                ?: ""
                            val actionUrl = child.child("actionUrl").getValue(String::class.java)
                                ?: child.child("url").getValue(String::class.java)
                                ?: ""
                            val badgeText = child.child("badgeText").getValue(String::class.java) ?: ""

                            if (imageUrl.isNotBlank() || title.isNotBlank()) {
                                list.add(
                                    FeaturedBannerItem(
                                        id = id,
                                        title = title,
                                        subtitle = subtitle,
                                        imageUrl = imageUrl,
                                        actionUrl = actionUrl,
                                        badgeText = badgeText
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Error parsing featured banner: ${e.message}")
                        }
                    }
                    _featuredBanners.value = list
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "featured_banners cancelled: ${error.message}")
                }
            })

            // --- Daily Audio Devotional listener: daily_audio_devotional ---
            database.getReference("daily_audio_devotional").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val title = snapshot.child("title").getValue(String::class.java) ?: "आज का आत्मिक मनन"
                        val speaker = snapshot.child("speaker").getValue(String::class.java) ?: "दैनिक मसीही सीख"
                        val audioUrl = snapshot.child("audioUrl").getValue(String::class.java)
                            ?: snapshot.child("url").getValue(String::class.java)
                            ?: ""
                        val date = snapshot.child("date").getValue(String::class.java) ?: ""
                        val durationText = snapshot.child("durationText").getValue(String::class.java)
                            ?: snapshot.child("duration").getValue(String::class.java)
                            ?: ""

                        if (audioUrl.isNotBlank()) {
                            _dailyAudioDevotional.value = DailyAudioDevotional(
                                title = title,
                                speaker = speaker,
                                audioUrl = audioUrl,
                                date = date,
                                durationText = durationText
                            )
                        } else {
                            _dailyAudioDevotional.value = null
                        }
                    } else {
                        _dailyAudioDevotional.value = null
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "daily_audio_devotional cancelled: ${error.message}")
                }
            })

        } catch (e: Exception) {
            Log.w(TAG, "Firebase Realtime Database initialization/listener error: ${e.message}")
        }
    }

    private fun updateAllBlogs() {
        val fellowship = _fellowshipBlogs.value
        val personal = _personalVlogs.value
        _allBlogs.value = mergeAndDeduplicate(
            remoteItems = fellowship + personal,
            localHardcodedItems = PredefinedData.hardcodedFellowshipBlogs + PredefinedData.hardcodedPersonalVlogs,
            keySelector = { it.id.ifBlank { it.url } },
            timestampSelector = { it.publishedTimestamp }
        )
    }

    private fun parseBlogSnapshot(snapshot: DataSnapshot, defaultSource: BlogSourceType): List<BlogPost> {
        val list = mutableListOf<BlogPost>()
        for (child in snapshot.children) {
            try {
                val id = child.child("id").getValue(String::class.java)
                    ?: child.key ?: continue
                val title = child.child("title").getValue(String::class.java).orEmpty()
                if (title.isBlank()) continue

                val sourceStr = child.child("source").getValue(String::class.java)
                val source = if (sourceStr != null) BlogSourceType.fromId(sourceStr) else defaultSource

                val publishedDate = child.child("publishedDate").getValue(String::class.java) ?: ""
                val timestamp = child.child("publishedTimestamp").getValue(Long::class.java)
                    ?: child.child("timestamp").getValue(Long::class.java)
                    ?: System.currentTimeMillis()

                val labels = mutableListOf<String>()
                child.child("labels").children.forEach { lbl ->
                    lbl.getValue(String::class.java)?.let { labels.add(it) }
                }

                val featuredImg = child.child("featuredImageUrl").getValue(String::class.java)
                    ?: child.child("imageUrl").getValue(String::class.java)

                val allImages = mutableListOf<String>()
                featuredImg?.let { allImages.add(it) }
                child.child("allImages").children.forEach { img ->
                    img.getValue(String::class.java)?.let { if (!allImages.contains(it)) allImages.add(it) }
                }

                val excerpt = child.child("plainTextExcerpt").getValue(String::class.java)
                    ?: child.child("summary").getValue(String::class.java).orEmpty()
                val content = child.child("contentHtml").getValue(String::class.java)
                    ?: child.child("content").getValue(String::class.java).orEmpty()
                val url = child.child("url").getValue(String::class.java)
                    ?: child.child("link").getValue(String::class.java).orEmpty()

                val videoIds = mutableListOf<String>()
                child.child("embeddedVideoIds").children.forEach { vid ->
                    vid.getValue(String::class.java)?.let { videoIds.add(it) }
                }

                list.add(
                    BlogPost(
                        id = id,
                        source = source,
                        title = title,
                        publishedDate = publishedDate,
                        publishedTimestamp = timestamp,
                        labels = labels,
                        featuredImageUrl = featuredImg,
                        allImages = allImages,
                        plainTextExcerpt = excerpt,
                        contentHtml = content,
                        url = url,
                        embeddedVideoIds = videoIds
                    )
                )
            } catch (e: Exception) {
                Log.w(TAG, "Error parsing blog snapshot child: ${e.message}")
            }
        }
        return list
    }

    private fun parseVideoSnapshot(snapshot: DataSnapshot): List<YouTubeVideo> {
        val list = mutableListOf<YouTubeVideo>()
        for (child in snapshot.children) {
            try {
                val id = child.child("id").getValue(String::class.java)
                    ?: child.child("videoId").getValue(String::class.java)
                    ?: child.key ?: continue
                if (id.startsWith("local_vid")) continue

                val title = child.child("title").getValue(String::class.java)
                    ?: child.child("name").getValue(String::class.java)
                    ?: child.child("heading").getValue(String::class.java).orEmpty()
                if (title.isBlank()) continue

                val channelId = child.child("channelId").getValue(String::class.java) ?: "firebase_channel"
                val channelTitle = child.child("channelTitle").getValue(String::class.java)
                    ?: child.child("channel").getValue(String::class.java)
                    ?: child.child("author").getValue(String::class.java)
                    ?: "Firebase Media"

                val videoUrl = child.child("videoUrl").getValue(String::class.java)
                    ?: child.child("url").getValue(String::class.java)
                    ?: child.child("streamUrl").getValue(String::class.java)
                    ?: child.child("link").getValue(String::class.java)
                    ?: if (id.length == 11 && !id.contains(".")) "https://www.youtube.com/watch?v=$id" else id

                var thumb = child.child("thumbnailUrl").getValue(String::class.java)
                    ?: child.child("thumbnail").getValue(String::class.java)
                    ?: child.child("thumb").getValue(String::class.java)
                    ?: child.child("imageUrl").getValue(String::class.java)
                    ?: child.child("image").getValue(String::class.java)
                    ?: child.child("poster").getValue(String::class.java).orEmpty()

                if (thumb.isBlank()) {
                    thumb = if (id.length == 11 && !id.contains(".")) {
                        "https://i.ytimg.com/vi/$id/hqdefault.jpg"
                    } else {
                        "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80"
                    }
                }

                val publishedAt = child.child("publishedAt").getValue(String::class.java).orEmpty()
                val timestamp = child.child("publishedTimestamp").getValue(Long::class.java)
                    ?: child.child("timestamp").getValue(Long::class.java)
                    ?: System.currentTimeMillis()
                val description = child.child("description").getValue(String::class.java)
                    ?: child.child("desc").getValue(String::class.java).orEmpty()

                list.add(
                    YouTubeVideo(
                        id = id,
                        title = title,
                        channelId = channelId,
                        channelTitle = channelTitle,
                        thumbnailUrl = thumb,
                        publishedAt = publishedAt,
                        publishedTimestamp = timestamp,
                        description = description,
                        videoUrl = videoUrl,
                        isRemote = true
                    )
                )
            } catch (e: Exception) {
                Log.w(TAG, "Error parsing video snapshot child: ${e.message}")
            }
        }
        return list
    }

    private fun parsePlaylistSnapshot(snapshot: DataSnapshot): List<YouTubePlaylist> {
        val list = mutableListOf<YouTubePlaylist>()
        for (child in snapshot.children) {
            try {
                val id = child.child("id").getValue(String::class.java)
                    ?: child.key ?: continue
                val title = child.child("title").getValue(String::class.java).orEmpty()
                if (title.isBlank()) continue

                val channelTitle = child.child("channelTitle").getValue(String::class.java).orEmpty()
                val playlistUrl = child.child("playlistUrl").getValue(String::class.java)
                    ?: "https://youtube.com/playlist?list=$id"
                val count = child.child("videoCountEstimate").getValue(Int::class.java)
                val thumb = child.child("thumbnailUrl").getValue(String::class.java)

                list.add(
                    YouTubePlaylist(
                        id = id,
                        title = title,
                        channelTitle = channelTitle,
                        playlistUrl = playlistUrl,
                        videoCountEstimate = count,
                        thumbnailUrl = thumb
                    )
                )
            } catch (e: Exception) {
                Log.w(TAG, "Error parsing playlist snapshot child: ${e.message}")
            }
        }
        return list
    }

    /**
     * Helper to merge dynamic external list with hardcoded lists for blog posts.
     */
    fun mergeExternalBlogPosts(
        externalPosts: List<BlogPost>,
        source: BlogSourceType
    ): List<BlogPost> {
        val hardcoded = when (source) {
            BlogSourceType.FELLOWSHIP_EVENTS -> PredefinedData.hardcodedFellowshipBlogs
            BlogSourceType.PERSONAL_VLOG -> PredefinedData.hardcodedPersonalVlogs
        }
        val remoteFromFirebase = when (source) {
            BlogSourceType.FELLOWSHIP_EVENTS -> _fellowshipBlogs.value
            BlogSourceType.PERSONAL_VLOG -> _personalVlogs.value
        }
        return mergeAndDeduplicate(
            remoteItems = remoteFromFirebase + externalPosts,
            localHardcodedItems = hardcoded,
            keySelector = { it.id.ifBlank { it.url } },
            timestampSelector = { it.publishedTimestamp }
        )
    }

    /**
     * Helper to merge dynamic external list with hardcoded lists for YouTube videos.
     */
    fun mergeExternalVideos(externalVideos: List<YouTubeVideo>): List<YouTubeVideo> {
        return mergeAndDeduplicate(
            remoteItems = _videos.value + externalVideos,
            localHardcodedItems = PredefinedData.hardcodedVideos,
            keySelector = { it.id.ifBlank { it.videoUrl } },
            timestampSelector = { it.publishedTimestamp }
        )
    }

    /**
     * Get the active song spreadsheet URL, honoring single-link exemption (Firebase override if present, else fallback).
     */
    fun getSongSpreadsheetUrl(): String {
        val current = _songSpreadsheetUrl.value.trim()
        return if (current.isNotBlank()) current else PredefinedData.FALLBACK_SONG_SPREADSHEET_URL
    }

    /**
     * Get the active today scripture, honoring single-link exemption (Firebase override if present, else fallback).
     */
    fun getTodayScripture(): String {
        val current = _todayScripture.value.trim()
        return if (current.isNotBlank()) current else getDefaultScriptureFallback()
    }

    private fun parseEventSnapshot(snapshot: DataSnapshot): List<com.example.data.model.FellowshipEvent> {
        val list = mutableListOf<com.example.data.model.FellowshipEvent>()
        for (child in snapshot.children) {
            try {
                val id = child.child("id").getValue(String::class.java)
                    ?: child.key ?: continue
                val title = child.child("title").getValue(String::class.java).orEmpty()
                if (title.isBlank()) continue

                val description = child.child("description").getValue(String::class.java)
                    ?: child.child("details").getValue(String::class.java).orEmpty()
                val dateString = child.child("dateString").getValue(String::class.java)
                    ?: child.child("date").getValue(String::class.java).orEmpty()
                val timeString = child.child("timeString").getValue(String::class.java)
                    ?: child.child("time").getValue(String::class.java).orEmpty()
                val locationString = child.child("locationString").getValue(String::class.java)
                    ?: child.child("location").getValue(String::class.java)
                    ?: child.child("venue").getValue(String::class.java).orEmpty()
                val speaker = child.child("speaker").getValue(String::class.java).orEmpty()
                val category = child.child("category").getValue(String::class.java) ?: "General"
                val startTimestamp = child.child("startTimestamp").getValue(Long::class.java)
                    ?: child.child("timestamp").getValue(Long::class.java)
                    ?: System.currentTimeMillis()
                val rsvpCount = child.child("rsvpCount").getValue(Int::class.java) ?: 0
                val meetingUrl = child.child("meetingUrl").getValue(String::class.java).orEmpty()
                val isOnline = child.child("isOnline").getValue(Boolean::class.java)
                    ?: (meetingUrl.isNotBlank() || locationString.contains("online", ignoreCase = true) || locationString.contains("zoom", ignoreCase = true))
                val imageUrl = child.child("imageUrl").getValue(String::class.java).orEmpty()
                val postUrl = child.child("postUrl").getValue(String::class.java).orEmpty()

                list.add(
                    com.example.data.model.FellowshipEvent(
                        id = id,
                        title = title,
                        description = description,
                        dateString = dateString,
                        timeString = timeString,
                        locationString = locationString,
                        speaker = speaker,
                        category = category,
                        startTimestamp = startTimestamp,
                        rsvpCount = rsvpCount,
                        meetingUrl = meetingUrl,
                        isOnline = isOnline,
                        imageUrl = imageUrl,
                        postUrl = postUrl
                    )
                )
            } catch (e: Exception) {
                Log.w(TAG, "Error parsing fellowship event child: ${e.message}")
            }
        }
        return list
    }

    /**
     * Submit or update RSVP in Firebase Realtime Database
     */
    fun submitRsvp(
        rsvp: com.example.data.model.EventRsvp,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        try {
            val database = FirebaseDatabase.getInstance()
            val rsvpRef = database.getReference("event_rsvps").child(rsvp.eventId)
            val rsvpKey = rsvp.userContact.replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank {
                rsvp.userName.replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "guest_${System.currentTimeMillis()}" }
            }

            val dataMap = mapOf(
                "eventId" to rsvp.eventId,
                "userName" to rsvp.userName,
                "userContact" to rsvp.userContact,
                "status" to rsvp.status,
                "attendeesCount" to rsvp.attendeesCount,
                "timestamp" to rsvp.timestamp,
                "notes" to rsvp.notes
            )

            rsvpRef.child(rsvpKey).setValue(dataMap)
                .addOnSuccessListener {
                    // Update local count optimism
                    val currentList = _fellowshipEvents.value.toMutableList()
                    val idx = currentList.indexOfFirst { it.id == rsvp.eventId }
                    if (idx != null && idx >= 0) {
                        val current = currentList[idx]
                        val added = if (rsvp.status == "GOING") rsvp.attendeesCount else 0
                        currentList[idx] = current.copy(rsvpCount = current.rsvpCount + added)
                        _fellowshipEvents.value = currentList
                    }
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    onError(e.localizedMessage ?: "Failed to submit RSVP")
                }
        } catch (e: Exception) {
            onError(e.localizedMessage ?: "Database error")
        }
    }

    /**
     * Create or update a fellowship event in Firebase Realtime Database and Firestore
     */
    fun addOrUpdateFellowshipEvent(
        event: com.example.data.model.FellowshipEvent,
        onSuccess: (com.example.data.model.FellowshipEvent) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        try {
            val database = FirebaseDatabase.getInstance()
            val eventId = if (event.id.isBlank()) {
                database.getReference("fellowship_events").push().key ?: "event_${System.currentTimeMillis()}"
            } else {
                event.id
            }

            var startTimestamp = event.startTimestamp
            if (startTimestamp <= 0L && event.dateString.isNotBlank()) {
                val formats = listOf("dd MMM yyyy", "d MMM yyyy", "yyyy-MM-dd", "dd/MM/yyyy", "MMMM dd, yyyy")
                for (fmt in formats) {
                    try {
                        val sdf = SimpleDateFormat(fmt, Locale.ENGLISH)
                        val d = sdf.parse(event.dateString)
                        if (d != null) {
                            val cal = java.util.Calendar.getInstance()
                            cal.time = d
                            if (event.timeString.isNotBlank()) {
                                val timeLower = event.timeString.lowercase(Locale.ENGLISH)
                                val isPm = timeLower.contains("pm")
                                val digits = event.timeString.replace(Regex("[^0-9:]"), "").split(":")
                                var hour = digits.getOrNull(0)?.toIntOrNull() ?: 10
                                val min = digits.getOrNull(1)?.toIntOrNull() ?: 0
                                if (isPm && hour < 12) hour += 12
                                if (!isPm && hour == 12) hour = 0
                                cal.set(java.util.Calendar.HOUR_OF_DAY, hour)
                                cal.set(java.util.Calendar.MINUTE, min)
                            }
                            startTimestamp = cal.timeInMillis
                            break
                        }
                    } catch (_: Exception) {}
                }
            }
            if (startTimestamp <= 0L) {
                startTimestamp = System.currentTimeMillis()
            }

            val finalEvent = event.copy(id = eventId, startTimestamp = startTimestamp)
            val dataMap = mapOf<String, Any>(
                "id" to finalEvent.id,
                "title" to finalEvent.title,
                "description" to finalEvent.description,
                "dateString" to finalEvent.dateString,
                "timeString" to finalEvent.timeString,
                "locationString" to finalEvent.locationString,
                "speaker" to finalEvent.speaker,
                "category" to finalEvent.category,
                "startTimestamp" to finalEvent.startTimestamp,
                "rsvpCount" to finalEvent.rsvpCount,
                "meetingUrl" to finalEvent.meetingUrl,
                "isOnline" to finalEvent.isOnline,
                "imageUrl" to finalEvent.imageUrl,
                "postUrl" to finalEvent.postUrl
            )

            database.getReference("fellowship_events").child(eventId).setValue(dataMap)
                .addOnSuccessListener {
                    try {
                        database.getReference("events").child(eventId).setValue(dataMap)
                        FirebaseFirestore.getInstance().collection("fellowship_events").document(eventId).set(dataMap)

                        // Trigger real-time push notification alert for Master Admin
                        val notifId = "notif_event_" + System.currentTimeMillis()
                        val title = "📅 कलीसिया कार्यक्रम संशोधित/नया (Event Modified)"
                        val body = "कलीसिया कार्यक्रम '${finalEvent.title}' (${finalEvent.dateString}) को जोड़ा या संशोधित किया गया है।"
                        val notifMap = mapOf<String, Any>(
                            "id" to notifId,
                            "title" to title,
                            "body" to body,
                            "timestamp" to System.currentTimeMillis(),
                            "type" to "event_modified",
                            "targetId" to finalEvent.id
                        )
                        FirebaseFirestore.getInstance().collection("admin_notifications").document(notifId).set(notifMap)
                    } catch (_: Exception) {}

                    val currentList = _fellowshipEvents.value.toMutableList()
                    val idx = currentList.indexOfFirst { it.id == eventId }
                    if (idx >= 0) {
                        currentList[idx] = finalEvent
                    } else {
                        currentList.add(0, finalEvent)
                    }
                    _fellowshipEvents.value = currentList.sortedBy { it.startTimestamp }
                    onSuccess(finalEvent)
                }
                .addOnFailureListener { e ->
                    onError(e.localizedMessage ?: "Failed to save event")
                }
        } catch (e: Exception) {
            onError(e.localizedMessage ?: "Database error")
        }
    }

    /**
     * Delete a fellowship event from Firebase Realtime Database and Firestore
     */
    fun deleteFellowshipEvent(
        eventId: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        if (eventId.isBlank()) return
        try {
            val database = FirebaseDatabase.getInstance()
            database.getReference("fellowship_events").child(eventId).removeValue()
                .addOnSuccessListener {
                    try {
                        database.getReference("events").child(eventId).removeValue()
                        FirebaseFirestore.getInstance().collection("fellowship_events").document(eventId).delete()
                    } catch (_: Exception) {}

                    _fellowshipEvents.value = _fellowshipEvents.value.filter { it.id != eventId }
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    onError(e.localizedMessage ?: "Failed to delete event")
                }
        } catch (e: Exception) {
            onError(e.localizedMessage ?: "Database error")
        }
    }

    fun submitPrayerRequest(
        name: String,
        city: String,
        pastorName: String = "",
        userRole: String = "विश्वासी",
        isUrgent: Boolean = false,
        requestText: String,
        isPrivate: Boolean,
        senderDeviceId: String = "",
        isVerifiedAdmin: Boolean = false,
        adminDesignation: String = "",
        adminName: String = "",
        category: String = "अन्य",
        tags: List<String> = listOf("अन्य"),
        onSuccess: (requestId: String, serialNumber: Int) -> Unit = { _, _ -> },
        onError: (String) -> Unit = {}
    ) {
        try {
            val database = FirebaseDatabase.getInstance()
            val ref = database.getReference("prayer_requests").push()
            val key = ref.key ?: "req_${System.currentTimeMillis()}"

            // Automatically compute sequential serial number
            val currentList = _prayerRequests.value
            val maxSerial = currentList.maxOfOrNull { it.serialNumber } ?: 0
            val nextSerial = maxSerial + 1
            val effectiveCategory = if (category.isNotBlank()) category else if (tags.isNotEmpty()) tags.first() else "अन्य"
            val effectiveTags = if (tags.isNotEmpty()) tags else listOf(effectiveCategory)

            val dataMap = mapOf<String, Any>(
                "id" to key,
                "serialNumber" to nextSerial,
                "name" to name.ifBlank { "विश्वासी" },
                "city" to city,
                "pastorName" to pastorName,
                "userRole" to userRole,
                "isUrgent" to isUrgent,
                "requestText" to requestText,
                "isPrivate" to isPrivate,
                "timestamp" to System.currentTimeMillis(),
                "prayingCount" to 0,
                "senderDeviceId" to senderDeviceId,
                "isAnswered" to false,
                "answeredTimestamp" to 0L,
                "testimonyText" to "",
                "isVerifiedAdmin" to isVerifiedAdmin,
                "adminDesignation" to adminDesignation,
                "adminName" to adminName,
                "category" to effectiveCategory,
                "tags" to effectiveTags
            )

            ref.setValue(dataMap)
                .addOnSuccessListener {
                    // Sync to Firestore prayers collection
                    try {
                        FirebaseFirestore.getInstance().collection("prayers").document(key).set(dataMap)
                    } catch (_: Exception) {}
                    onSuccess(key, nextSerial)
                }
                .addOnFailureListener { e -> onError(e.localizedMessage ?: "प्रार्थना निवेदन भेजने में त्रुटि हुई") }
        } catch (e: Exception) {
            onError(e.localizedMessage ?: "Database error")
        }
    }

    // Overload for backward compatibility
    fun submitPrayerRequest(
        name: String,
        city: String,
        requestText: String,
        isPrivate: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        submitPrayerRequest(
            name = name,
            city = city,
            pastorName = "",
            requestText = requestText,
            isPrivate = isPrivate,
            senderDeviceId = "",
            onSuccess = { _, _ -> onSuccess() },
            onError = onError
        )
    }

    fun markPrayerAsAnswered(
        requestId: String,
        testimonyText: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        if (requestId.isBlank()) return
        try {
            val database = FirebaseDatabase.getInstance()
            val ref = database.getReference("prayer_requests").child(requestId)
            val updates = mapOf<String, Any>(
                "isAnswered" to true,
                "answeredTimestamp" to System.currentTimeMillis(),
                "testimonyText" to testimonyText
            )
            ref.updateChildren(updates)
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { e -> onError(e.localizedMessage ?: "त्रुटि हुई") }
        } catch (e: Exception) {
            onError(e.localizedMessage ?: "Database error")
        }
    }

    fun replyToPrayerRequest(
        requestId: String,
        replyText: String,
        authorName: String,
        authorDesignation: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        if (requestId.isBlank()) return
        try {
            val database = FirebaseDatabase.getInstance()
            val ref = database.getReference("prayer_requests").child(requestId)
            val now = System.currentTimeMillis()
            val updates = mapOf<String, Any>(
                "adminReplyText" to replyText,
                "adminReplyAuthorName" to authorName,
                "adminReplyAuthorDesignation" to authorDesignation,
                "adminReplyTimestamp" to (if (replyText.isBlank()) 0L else now)
            )
            ref.updateChildren(updates)
                .addOnSuccessListener {
                    try {
                        FirebaseFirestore.getInstance().collection("prayers").document(requestId).update(updates)
                    } catch (_: Exception) {}
                    onSuccess()
                }
                .addOnFailureListener { e -> onError(e.localizedMessage ?: "उत्तर भेजने में त्रुटि हुई") }
        } catch (e: Exception) {
            onError(e.localizedMessage ?: "Database error")
        }
    }

    fun deletePrayerRequest(
        requestId: String,
        deletedItem: PrayerRequestItem? = null,
        deletedByRole: String = "AUTHOR",
        deletedByDeviceId: String = "",
        adminPinUsed: String = "",
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        if (requestId.isBlank()) return
        try {
            val database = FirebaseDatabase.getInstance()
            val ref = database.getReference("prayer_requests").child(requestId)

            // 1. Write Audit Log to Cloud Firestore for transparency & accountability
            try {
                val firestore = FirebaseFirestore.getInstance()
                val logId = "del_${requestId}_${System.currentTimeMillis()}"
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val dateFormatted = sdf.format(Date())

                val auditData = hashMapOf<String, Any>(
                    "id" to logId,
                    "prayerRequestId" to requestId,
                    "serialNumber" to (deletedItem?.serialNumber ?: 0),
                    "prayerName" to (deletedItem?.name ?: ""),
                    "prayerCity" to (deletedItem?.city ?: ""),
                    "prayerPastor" to (deletedItem?.pastorName ?: ""),
                    "prayerRole" to (deletedItem?.userRole ?: ""),
                    "prayerText" to (deletedItem?.requestText ?: ""),
                    "isAnswered" to (deletedItem?.isAnswered ?: false),
                    "testimonyText" to (deletedItem?.testimonyText ?: ""),
                    "originalTimestamp" to (deletedItem?.timestamp ?: 0L),
                    "deletedTimestamp" to System.currentTimeMillis(),
                    "deletedDateFormatted" to dateFormatted,
                    "deletedByRole" to deletedByRole,
                    "deletedByDeviceId" to deletedByDeviceId,
                    "adminPinUsed" to (if (adminPinUsed.isNotBlank()) "PIN_VERIFIED" else "NONE"),
                    "deviceModel" to "${Build.MANUFACTURER} ${Build.MODEL}",
                    "androidVersion" to "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                    "action" to "DELETE_PRAYER_REQUEST"
                )

                firestore.collection("prayer_deletion_audit_logs")
                    .document(logId)
                    .set(auditData)
                    .addOnSuccessListener {
                        Log.i(TAG, "Deletion audit log recorded in Firestore: $logId")
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Failed to write audit log to Firestore: ${e.message}")
                    }

                // Dual redundancy: also record in Realtime Database audit logs
                database.getReference("audit_logs").child("prayer_deletions").child(logId).setValue(auditData)
            } catch (auditEx: Exception) {
                Log.w(TAG, "Error generating deletion audit log: ${auditEx.message}")
            }

            // 2. Remove prayer request from active list
            ref.removeValue()
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { e -> onError(e.localizedMessage ?: "हटाने में त्रुटि हुई") }
        } catch (e: Exception) {
            onError(e.localizedMessage ?: "Database error")
        }
    }

    // Overload for backward compatibility
    fun deletePrayerRequest(
        requestId: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val cached = _prayerRequests.value.find { it.id == requestId }
        deletePrayerRequest(
            requestId = requestId,
            deletedItem = cached,
            deletedByRole = "AUTHOR",
            deletedByDeviceId = "",
            adminPinUsed = "",
            onSuccess = onSuccess,
            onError = onError
        )
    }


    fun revertAnsweredToPrayer(
        requestId: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        if (requestId.isBlank()) return
        try {
            val database = FirebaseDatabase.getInstance()
            val ref = database.getReference("prayer_requests").child(requestId)
            val updates = mapOf<String, Any>(
                "isAnswered" to false,
                "answeredTimestamp" to 0L,
                "testimonyText" to ""
            )
            ref.updateChildren(updates)
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { e -> onError(e.localizedMessage ?: "त्रुटि हुई") }
        } catch (e: Exception) {
            onError(e.localizedMessage ?: "Database error")
        }
    }

    fun incrementPrayingCount(requestId: String) {
        if (requestId.isBlank()) return
        try {
            val database = FirebaseDatabase.getInstance()
            val ref = database.getReference("prayer_requests").child(requestId)
            ref.child("prayingCount").get().addOnSuccessListener { snap ->
                val current = snap.getValue(Int::class.java) ?: 0
                ref.child("prayingCount").setValue(current + 1)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error incrementing praying count: ${e.message}")
        }
    }

    fun updatePrayerRequestsConfig(config: PrayerRequestsConfig, onComplete: ((Boolean) -> Unit)? = null) {
        try {
            val database = FirebaseDatabase.getInstance()
            database.getReference("app_settings").child("prayer_requests_config").setValue(
                mapOf(
                    "enabled" to config.enabled,
                    "showPastorField" to config.showPastorField,
                    "allowTestimonies" to config.allowTestimonies,
                    "allowPublicWall" to config.allowPublicWall,
                    "maxDailyPrayTapsPerUser" to config.maxDailyPrayTapsPerUser
                )
            ).addOnSuccessListener {
                _prayerRequestsConfig.value = config
                onComplete?.invoke(true)
            }.addOnFailureListener {
                onComplete?.invoke(false)
            }
        } catch (e: Exception) {
            onComplete?.invoke(false)
        }
    }

    fun updatePrivateProfilePassword(newPassword: String, onComplete: (Boolean) -> Unit = {}) {
        val clean = newPassword.trim()
        if (clean.isBlank()) {
            onComplete(false)
            return
        }
        _privateProfilePassword.value = clean
        try {
            val database = FirebaseDatabase.getInstance()
            database.getReference("private_profile_password").setValue(clean)
                .addOnSuccessListener {
                    database.getReference("profile_b_password").setValue(clean)
                    onComplete(true)
                }
                .addOnFailureListener {
                    onComplete(false)
                }
        } catch (e: Exception) {
            onComplete(false)
        }
    }

    fun setGlobalPersonalVlogServerEnabled(enabled: Boolean, onComplete: ((Boolean) -> Unit)? = null) {
        _isPersonalVlogEnabled.value = enabled
        try {
            val database = FirebaseDatabase.getInstance()
            database.getReference("is_vlog_server_enabled").setValue(enabled)
            database.getReference("personal_vlog_enabled").setValue(enabled)
                .addOnSuccessListener { onComplete?.invoke(true) }
                .addOnFailureListener { onComplete?.invoke(false) }
        } catch (e: Exception) {
            onComplete?.invoke(false)
        }
    }

    fun updateDailyGreetingConfig(config: com.example.data.model.DailyGreetingConfig, onComplete: ((Boolean) -> Unit)? = null) {
        val ttlValue = if (config.ttl > 0L) config.ttl else if (config.expiresAtTimestamp > 0L) config.expiresAtTimestamp / 1000L else 0L
        val effectiveConfig = config.copy(ttl = ttlValue)
        _dailyGreetingConfig.value = effectiveConfig
        val clean = effectiveConfig.greetingText.trim().ifBlank { "जय मसीह की" }
        com.example.util.RemoteConfigHelper.updateDailyGreetingText(if (effectiveConfig.isExpired()) "जय मसीह की" else clean)
        try {
            val database = FirebaseDatabase.getInstance()
            database.getReference("daily_greeting_text").setValue(clean)
            database.getReference("daily_greeting_config").setValue(effectiveConfig)
                .addOnSuccessListener {
                    // Sync to Firestore daily_messages collection with TTL
                    try {
                        val firestore = FirebaseFirestore.getInstance()
                        val firestoreData = mapOf<String, Any>(
                            "greetingText" to effectiveConfig.greetingText,
                            "isPermanent" to effectiveConfig.isPermanent,
                            "durationHours" to effectiveConfig.durationHours,
                            "durationDays" to effectiveConfig.durationDays,
                            "expiresAtTimestamp" to effectiveConfig.expiresAtTimestamp,
                            "ttl" to ttlValue,
                            "expirationDate" to if (effectiveConfig.expiresAtTimestamp > 0L) com.google.firebase.Timestamp(effectiveConfig.expiresAtTimestamp / 1000L, 0) else com.google.firebase.Timestamp.now(),
                            "updatedTimestamp" to effectiveConfig.updatedTimestamp,
                            "isExpired" to effectiveConfig.isExpired()
                        )
                        firestore.collection("daily_messages").document("daily_greeting_config").set(firestoreData)
                    } catch (_: Exception) {}
                    onComplete?.invoke(true)
                }
                .addOnFailureListener { onComplete?.invoke(false) }
        } catch (e: Exception) {
            onComplete?.invoke(false)
        }
    }

    fun updateDailyGreetingText(greeting: String, onComplete: ((Boolean) -> Unit)? = null) {
        val cfg = _dailyGreetingConfig.value.copy(
            greetingText = greeting.trim().ifBlank { "जय मसीह की" },
            isPermanent = true,
            durationHours = 0,
            durationDays = 0,
            expiresAtTimestamp = 0L,
            updatedTimestamp = System.currentTimeMillis()
        )
        updateDailyGreetingConfig(cfg, onComplete)
    }

    fun updateQuickAccessConfig(config: com.example.data.model.QuickAccessConfig, onComplete: ((Boolean) -> Unit)? = null) {
        _quickAccessConfig.value = config
        try {
            val database = FirebaseDatabase.getInstance()
            database.getReference("quick_access_config").setValue(config)
                .addOnSuccessListener { onComplete?.invoke(true) }
                .addOnFailureListener { onComplete?.invoke(false) }
        } catch (e: Exception) {
            onComplete?.invoke(false)
        }
    }

    fun updateVideoQuickAccessConfig(config: com.example.data.model.VideoQuickAccessConfig, onComplete: ((Boolean) -> Unit)? = null) {
        _videoQuickAccessConfig.value = config
        try {
            val database = FirebaseDatabase.getInstance()
            val itemsMap = config.items.mapIndexed { index, item ->
                mapOf(
                    "id" to item.id.ifBlank { "item_${System.currentTimeMillis()}_$index" },
                    "label" to item.label,
                    "filterType" to item.filterType,
                    "filterValue" to item.filterValue,
                    "isVisible" to item.isVisible,
                    "isPinned" to item.isPinned,
                    "order" to index
                )
            }
            val dataMap = mapOf(
                "isBarVisible" to config.isBarVisible,
                "items" to itemsMap
            )
            database.getReference("video_quick_access_config").setValue(dataMap)
                .addOnSuccessListener { onComplete?.invoke(true) }
                .addOnFailureListener { onComplete?.invoke(false) }
        } catch (e: Exception) {
            Log.w(TAG, "Error updating video_quick_access_config: ${e.message}")
            onComplete?.invoke(true)
        }
    }

    fun addOrUpdateYouTubePlaylist(playlist: com.example.data.model.YouTubePlaylist, onComplete: ((Boolean) -> Unit)? = null) {
        try {
            val database = FirebaseDatabase.getInstance()
            // Extract playlist ID from URL if ID is not direct
            var playlistId = playlist.id.trim()
            if (playlistId.isBlank() || playlistId.startsWith("http")) {
                val uri = android.net.Uri.parse(playlist.playlistUrl)
                playlistId = uri.getQueryParameter("list") ?: System.currentTimeMillis().toString()
            }
            val finalPlaylist = playlist.copy(id = playlistId)
            
            database.getReference("playlists").child(playlistId).setValue(mapOf(
                "id" to finalPlaylist.id,
                "title" to finalPlaylist.title,
                "channelTitle" to finalPlaylist.channelTitle,
                "playlistUrl" to finalPlaylist.playlistUrl,
                "thumbnailUrl" to (finalPlaylist.thumbnailUrl ?: ""),
                "videoCountEstimate" to (finalPlaylist.videoCountEstimate ?: 0)
            )).addOnSuccessListener { onComplete?.invoke(true) }
              .addOnFailureListener { onComplete?.invoke(false) }
        } catch (e: Exception) {
            onComplete?.invoke(false)
        }
    }

    fun deleteYouTubePlaylist(playlistId: String, onComplete: ((Boolean) -> Unit)? = null) {
        try {
            val database = FirebaseDatabase.getInstance()
            database.getReference("playlists").child(playlistId).removeValue()
                .addOnSuccessListener { onComplete?.invoke(true) }
                .addOnFailureListener { onComplete?.invoke(false) }
        } catch (e: Exception) {
            onComplete?.invoke(false)
        }
    }

    fun addOrUpdateCustomVideo(video: com.example.data.model.YouTubeVideo, onComplete: ((Boolean) -> Unit)? = null) {
        try {
            val database = FirebaseDatabase.getInstance()
            var vidId = video.id.trim()
            if (vidId.isBlank() || vidId.startsWith("http")) {
                val parsed = com.example.util.VideoUrlParser.parse(video.videoUrl)
                vidId = if (parsed.videoId.isNotBlank()) parsed.videoId else "custom_" + System.currentTimeMillis()
            }
            val finalVideo = video.copy(id = vidId)

            database.getReference("youtube_videos").child(vidId).setValue(mapOf(
                "id" to finalVideo.id,
                "title" to finalVideo.title,
                "channelTitle" to finalVideo.channelTitle,
                "videoUrl" to finalVideo.videoUrl,
                "thumbnailUrl" to finalVideo.thumbnailUrl,
                "description" to finalVideo.description,
                "publishedTimestamp" to finalVideo.publishedTimestamp
            )).addOnSuccessListener { onComplete?.invoke(true) }
              .addOnFailureListener { onComplete?.invoke(false) }
        } catch (e: Exception) {
            onComplete?.invoke(false)
        }
    }

    fun deleteCustomVideo(videoId: String, onComplete: ((Boolean) -> Unit)? = null) {
        try {
            val database = FirebaseDatabase.getInstance()
            database.getReference("youtube_videos").child(videoId).removeValue()
                .addOnSuccessListener { onComplete?.invoke(true) }
                .addOnFailureListener { onComplete?.invoke(false) }
        } catch (e: Exception) {
            onComplete?.invoke(false)
        }
    }

    fun setPinnedVideoId(videoId: String?, onComplete: ((Boolean) -> Unit)? = null) {
        try {
            val cleanId = videoId?.trim()?.ifBlank { null }
            _pinnedVideoId.value = cleanId

            val database = FirebaseDatabase.getInstance()
            val ref = database.getReference("pinned_video_id")
            if (cleanId == null) {
                ref.removeValue().addOnCompleteListener { task ->
                    onComplete?.invoke(task.isSuccessful)
                }
            } else {
                ref.setValue(cleanId).addOnCompleteListener { task ->
                    onComplete?.invoke(task.isSuccessful)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error updating pinned_video_id: ${e.message}")
            onComplete?.invoke(true)
        }
    }

    fun updateHomeSectionsConfig(config: com.example.data.model.HomeSectionsConfig, onComplete: ((Boolean) -> Unit)? = null) {
        try {
            _homeSectionsConfig.value = config
            val database = FirebaseDatabase.getInstance()
            database.getReference("home_sections_config").setValue(config)
                .addOnSuccessListener {
                    Log.i(TAG, "HomeSectionsConfig updated successfully in Firebase")
                    onComplete?.invoke(true)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to update HomeSectionsConfig in Firebase: ${e.message}", e)
                    onComplete?.invoke(false)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error in updateHomeSectionsConfig: ${e.message}", e)
            onComplete?.invoke(false)
        }
    }
}
