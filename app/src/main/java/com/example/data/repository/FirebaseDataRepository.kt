package com.example.data.repository

import android.util.Log
import com.example.data.bible.model.VerseOfTheDay
import com.example.data.local.PredefinedData
import com.example.data.model.BlogPost
import com.example.data.model.BlogSourceType
import com.example.data.model.YouTubePlaylist
import com.example.data.model.YouTubeVideo
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
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

    // --- 2. Single String Values (Single Link Exemption: Remote overrides local fallback directly) ---
    private val _songSpreadsheetUrl = MutableStateFlow(PredefinedData.FALLBACK_SONG_SPREADSHEET_URL)
    val songSpreadsheetUrl: StateFlow<String> = _songSpreadsheetUrl.asStateFlow()

    private val _todayScripture = MutableStateFlow(getDefaultScriptureFallback())
    val todayScripture: StateFlow<String> = _todayScripture.asStateFlow()

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
                        _todayScripture.value = getDefaultScriptureFallback()
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

            // --- List 3: YouTube Videos ---
            database.getReference("youtube_videos").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val remoteVideos = parseVideoSnapshot(snapshot)
                    val merged = mergeAndDeduplicate(
                        remoteItems = remoteVideos,
                        localHardcodedItems = PredefinedData.hardcodedVideos,
                        keySelector = { it.id.ifBlank { it.videoUrl } },
                        timestampSelector = { it.publishedTimestamp }
                    )
                    _videos.value = merged
                    Log.i(TAG, "Merged YouTube videos: total ${merged.size} (remote: ${remoteVideos.size})")
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "youtube_videos cancelled: ${error.message}")
                }
            })

            // Fallback node check for "videos"
            database.getReference("videos").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val remoteVideos = parseVideoSnapshot(snapshot)
                        if (remoteVideos.isNotEmpty()) {
                            val merged = mergeAndDeduplicate(
                                remoteItems = remoteVideos,
                                localHardcodedItems = PredefinedData.hardcodedVideos,
                                keySelector = { it.id.ifBlank { it.videoUrl } },
                                timestampSelector = { it.publishedTimestamp }
                            )
                            _videos.value = merged
                        }
                    }
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
                    ?: child.key ?: continue
                if (id.startsWith("local_vid") || id.startsWith("dm_x27lzjr_") || id.startsWith("dm_x4sr8o4_")) continue
                val title = child.child("title").getValue(String::class.java).orEmpty()
                if (title.isBlank()) continue

                val channelId = child.child("channelId").getValue(String::class.java).orEmpty()
                val channelTitle = child.child("channelTitle").getValue(String::class.java).orEmpty()
                val thumb = child.child("thumbnailUrl").getValue(String::class.java)
                    ?: "https://i.ytimg.com/vi/$id/hqdefault.jpg"
                val publishedAt = child.child("publishedAt").getValue(String::class.java).orEmpty()
                val timestamp = child.child("publishedTimestamp").getValue(Long::class.java)
                    ?: child.child("timestamp").getValue(Long::class.java)
                    ?: System.currentTimeMillis()
                val description = child.child("description").getValue(String::class.java).orEmpty()
                val videoUrl = child.child("videoUrl").getValue(String::class.java)
                    ?: "https://www.youtube.com/watch?v=$id"

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
                        videoUrl = videoUrl
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
}
