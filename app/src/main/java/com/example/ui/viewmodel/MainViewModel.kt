package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.bible.local.BibleDatabase
import com.example.data.bible.model.BibleBook
import com.example.data.local.AppDatabase
import com.example.data.local.PreferencesManager
import com.example.data.local.RecentlyViewedEntity
import com.example.data.local.SavedItemEntity
import com.example.data.model.*
import com.example.data.repository.BloggerRepository
import com.example.data.repository.RecentlyViewedRepository
import com.example.data.repository.SavedItemRepository
import com.example.data.repository.YouTubeRepository
import com.example.util.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.data.bible.model.ActiveReadingPlanItem
import com.example.data.bible.model.parsePlanColor
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(
    application: Application,
    val bloggerRepository: BloggerRepository,
    val youtubeRepository: YouTubeRepository,
    val savedItemRepository: SavedItemRepository,
    val recentlyViewedRepository: RecentlyViewedRepository,
    val preferencesManager: PreferencesManager,
    val bibleDatabase: BibleDatabase,
    val readingPlanRepository: com.example.data.bible.repository.ReadingPlanRepository,
    val studyNotesRepository: com.example.data.bible.repository.StudyNotesRepository,
    val lyricsRepository: com.example.data.bible.repository.LyricsRepository,
    val backupRepository: com.example.data.bible.repository.BackupRepository,
    val syncCenterRepository: com.example.data.repository.SyncCenterRepository,
    val bibleRepository: com.example.data.bible.repository.BibleRepository
) : AndroidViewModel(application) {

    val settings: StateFlow<UserSettings> = preferencesManager.settings

    val appUpdateManager = com.example.util.AppUpdateManager.getInstance(application)
    val updateState = appUpdateManager.updateState

    val welcomeSpeechManager = com.example.util.WelcomeSpeechManager.getInstance(application)

    val notificationRepository = com.example.data.repository.NotificationRepository(application)
    val adminNoticeRepository = com.example.data.repository.AdminNoticeRepository(application)

    val allNotifications: StateFlow<List<com.example.data.local.NotificationEntity>> = notificationRepository.allNotifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadNotificationCount: StateFlow<Int> = notificationRepository.unreadCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val activeAdminNotice: StateFlow<com.example.data.model.AdminNotice?> = adminNoticeRepository.activeNotice

    // Remote Config dynamic parameters & Vlog Master Override
    val isVlogServerEnabled: StateFlow<Boolean> = com.example.util.RemoteConfigHelper.isVlogServerEnabled
    val isSearchEnabled: StateFlow<Boolean> = com.example.util.RemoteConfigManager.isSearchEnabled
    val appNoticeHeading: StateFlow<String> = com.example.util.RemoteConfigManager.appNoticeHeading
    val dailyGreetingText: StateFlow<String> = com.example.util.RemoteConfigHelper.dailyGreetingText
    val verseOfTheDayText: StateFlow<String> = com.example.util.RemoteConfigHelper.verseOfTheDayText
    val specialAnnouncementText: StateFlow<String> = com.example.util.RemoteConfigHelper.specialAnnouncementText

    // Dual-Layer Vlog evaluation: Condition A (Local) && Condition B (Server)
    val isPersonalVlogAllowed: StateFlow<Boolean> = combine(
        settings,
        com.example.util.RemoteConfigHelper.isVlogServerEnabled
    ) { currentSettings, serverVlogEnabled ->
        val conditionA = currentSettings.showPersonalVlog || currentSettings.personalVlogMode != com.example.data.model.PersonalVlogMode.HIDDEN
        com.example.util.RemoteConfigHelper.isPersonalVlogAllowed(conditionA)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Firebase Realtime Database: Single String Values (Single Link Exemption: Remote overrides local fallback)
    val firebaseDataRepository = com.example.data.repository.FirebaseDataRepository.getInstance()
    val songSpreadsheetUrl: StateFlow<String> = firebaseDataRepository.songSpreadsheetUrl
    val todayScripture: StateFlow<String> = firebaseDataRepository.todayScripture

    fun isSearchFeatureEnabled(): Boolean = com.example.util.RemoteConfigManager.isSearchEnabled()
    fun getRemoteNoticeHeading(): String = com.example.util.RemoteConfigManager.getAppNoticeHeading()
    fun getActiveTodayScripture(): String = com.example.data.repository.FirebaseDataRepository.getInstance().getTodayScripture()
    fun getActiveSongSpreadsheetUrl(): String = com.example.data.repository.FirebaseDataRepository.getInstance().getSongSpreadsheetUrl()

    init {
        viewModelScope.launch {
            appUpdateManager.checkForUpdates(force = false)
        }
        viewModelScope.launch(Dispatchers.IO) {
            lyricsRepository.initializePreloadedLyrics()
            com.example.service.AppFirebaseMessagingService.initialize(getApplication())
        }
    }

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _isOffline = MutableStateFlow(!isOnline())
    val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Posts stream responding immediately to dual-layer Personal Vlog status
    val allPosts: StateFlow<List<BlogPost>> = isPersonalVlogAllowed
        .flatMapLatest { isAllowed ->
            bloggerRepository.getPostsFlow(isAllowed)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val fellowshipPosts: StateFlow<List<BlogPost>> = bloggerRepository
        .getPostsBySourceFlow(BlogSourceType.FELLOWSHIP_EVENTS)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val personalVlogPosts: StateFlow<List<BlogPost>> = combine(
        bloggerRepository.getPostsBySourceFlow(BlogSourceType.PERSONAL_VLOG),
        isPersonalVlogAllowed
    ) { posts, isAllowed ->
        if (isAllowed) posts else emptyList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Photos derived from visible posts
    val galleryPhotos: StateFlow<List<GalleryPhoto>> = allPosts
        .map { posts -> bloggerRepository.extractPhotos(posts) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Asynchronously fetched and bound active reading plans with progress and custom tint colors
    val activeReadingPlans: StateFlow<List<ActiveReadingPlanItem>> = combine(
        settings,
        readingPlanRepository.getAllProgress()
    ) { currentSettings, progressList ->
        withContext(Dispatchers.IO) {
            val activeIds = currentSettings.activePlanIds
            if (activeIds.isEmpty()) {
                return@withContext emptyList()
            }

            val predefined = readingPlanRepository.getAllPlans()
            val manualJson = preferencesManager.getManualPlansJson()
            val manual = if (manualJson.isNotBlank()) {
                try {
                    com.example.data.bible.model.ManualPlanData.deserializeList(manualJson).map { it.toReadingPlanInfo() }
                } catch (_: Exception) {
                    emptyList()
                }
            } else emptyList()

            val allPlans = predefined + manual
            val plansMap = allPlans.associateBy { it.id }

            val progressByPlan = progressList
                .filter { it.isCompleted }
                .groupBy { it.planId }

            val behindColor = parsePlanColor(currentSettings.planBehindColorHex, Color(0xFFEF4444))
            val onTrackColor = parsePlanColor(currentSettings.planOnTrackColorHex, Color(0xFFEAB308))
            val completedColor = parsePlanColor(currentSettings.planCompletedColorHex, Color(0xFF10B981))

            activeIds.mapNotNull { planId ->
                val plan = plansMap[planId] ?: return@mapNotNull null
                val completedCount = progressByPlan[planId]?.size ?: 0
                val fraction = if (plan.totalDays > 0) {
                    (completedCount.toFloat() / plan.totalDays.toFloat()).coerceIn(0f, 1f)
                } else 0f
                val percent = (fraction * 100).toInt()

                val tint = when {
                    completedCount >= plan.totalDays && plan.totalDays > 0 -> completedColor
                    completedCount > 0 -> onTrackColor
                    else -> behindColor
                }

                val completedDaysSet = progressByPlan[planId]?.map { it.dayNumber }?.toSet() ?: emptySet()
                val nextDayNumber = if (plan.totalDays > 0) {
                    ((1..plan.totalDays) - completedDaysSet).minOrNull() ?: plan.totalDays
                } else 1
                val dayPortion = plan.days.find { it.dayNumber == nextDayNumber }?.portions?.firstOrNull()
                val targetBookId = dayPortion?.bookId ?: 43
                val targetChapter = dayPortion?.startChapter ?: 1
                val targetStartChapter = dayPortion?.startChapter ?: 1
                val targetEndChapter = dayPortion?.endChapter ?: targetStartChapter
                val targetStartVerse = dayPortion?.startVerse ?: 1
                val totalChapterVerses = com.example.data.bible.model.BibleVerseCounts.getVerseCount(targetBookId, targetEndChapter)
                val targetEndVerse = dayPortion?.endVerse ?: (if (totalChapterVerses > 0) totalChapterVerses else 30)

                ActiveReadingPlanItem(
                    planId = plan.id,
                    title = plan.titleHindi.ifBlank { plan.titleEnglish },
                    totalDays = plan.totalDays,
                    completedDays = completedCount,
                    progressPercent = percent,
                    progressFraction = fraction,
                    tintColor = tint,
                    currentDayNumber = nextDayNumber,
                    targetBookId = targetBookId,
                    targetChapter = targetChapter,
                    targetStartChapter = targetStartChapter,
                    targetEndChapter = targetEndChapter,
                    targetStartVerse = targetStartVerse,
                    targetEndVerse = targetEndVerse
                )
            }
        }
    }.flowOn(Dispatchers.IO)
    .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Real-time Reading Insights & Streaks calculated reactively from Room SQLite progress
    val readingInsights: StateFlow<com.example.data.bible.model.ReadingInsightsData> = combine(
        settings,
        readingPlanRepository.getAllProgress()
    ) { currentSettings, progressList ->
        withContext(Dispatchers.IO) {
            val predefined = readingPlanRepository.getAllPlans()
            val manualJson = preferencesManager.getManualPlansJson()
            val manual = if (manualJson.isNotBlank()) {
                try {
                    com.example.data.bible.model.ManualPlanData.deserializeList(manualJson).map { it.toReadingPlanInfo() }
                } catch (_: Exception) {
                    emptyList()
                }
            } else emptyList()

            val allPlans = predefined + manual
            val activeIds = currentSettings.activePlanIds
            com.example.data.bible.model.ReadingInsightsCalculator.computeInsights(
                progressList = progressList,
                allPlans = allPlans,
                activePlanIds = activeIds
            )
        }
    }.flowOn(Dispatchers.IO)
    .stateIn(viewModelScope, SharingStarted.Lazily, com.example.data.bible.model.ReadingInsightsData())

    // Dynamic categories extracted from current posts
    val fellowshipCategories: StateFlow<List<String>> = fellowshipPosts
        .map { posts ->
            posts.flatMap { it.labels }.distinct().sorted()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val personalVlogCategories: StateFlow<List<String>> = personalVlogPosts
        .map { posts ->
            posts.flatMap { it.labels }.distinct().sorted()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // YouTube channel videos (Raw)
    private val rawYoutubeVideos: StateFlow<List<YouTubeVideo>> = youtubeRepository
        .getAllVideosFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered YouTube and Dailymotion channel videos:
    // 1. Christian Dailymotion (x27lzjr) videos are ALWAYS shown everywhere.
    // 2. Personal Vlog (x4sr8o4) videos are shown everywhere ONLY when dual-layer check passes (Condition A && Condition B).
    val youtubeVideos: StateFlow<List<YouTubeVideo>> = combine(rawYoutubeVideos, isPersonalVlogAllowed) { videos, isVlogAllowed ->
        videos.filter { video ->
            val isVlogVideo = video.channelId.contains("x4sr8o4", ignoreCase = true) ||
                    video.channelTitle.contains("Personal Vlog", ignoreCase = true) ||
                    video.videoUrl.contains("x4sr8o4", ignoreCase = true)
            if (isVlogVideo) {
                isVlogAllowed
            } else {
                true // Christian videos (x27lzjr) and all other channels show everywhere
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dynamic YouTube Playlists
    val youtubePlaylists: StateFlow<List<YouTubePlaylist>> = youtubeRepository
        .playlistsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PredefinedPlaylists.items)

    // YouTube Channel Default: AVJ Worship (MANDATORY REQUIREMENT)
    private val _selectedChannel = MutableStateFlow(PredefinedPlaylists.channelWorship)
    val selectedChannel: StateFlow<YouTubeChannelInfo> = _selectedChannel.asStateFlow()

    // Active playlist videos state
    private val _currentPlaylistVideos = MutableStateFlow<List<YouTubeVideo>>(emptyList())
    val currentPlaylistVideos: StateFlow<List<YouTubeVideo>> = _currentPlaylistVideos.asStateFlow()

    private val _isLoadingPlaylist = MutableStateFlow(false)
    val isLoadingPlaylist: StateFlow<Boolean> = _isLoadingPlaylist.asStateFlow()

    // Endless scrolling / Pagination state for Video RecyclerView & Feeds
    private val _isLoadingMoreVideos = MutableStateFlow(false)
    val isLoadingMoreVideos: StateFlow<Boolean> = _isLoadingMoreVideos.asStateFlow()

    private var dailymotionPage = 1
    private var hasMoreDailymotion = true
    private var youtubePageToken: String? = null
    private var hasMoreYouTube = true

    // Upcoming Events: Extracted only from Fellowship Events with label "Upcoming"
    val upcomingEvents: StateFlow<List<UpcomingEvent>> = fellowshipPosts
        .map { posts ->
            posts.mapNotNull { EventExtractor.extractUpcomingEvent(it) }
                .sortedBy { it.startTimestamp }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Direct Firebase Realtime Database Fellowship Events (Calendar & Meetings)
    val fellowshipEvents: StateFlow<List<com.example.data.model.FellowshipEvent>> = firebaseDataRepository
        .fellowshipEvents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.example.data.local.PredefinedData.hardcodedFellowshipEvents)

    // User's active RSVPs stored locally in state
    private val _userRsvps = MutableStateFlow<Map<String, com.example.data.model.EventRsvp>>(emptyMap())
    val userRsvps: StateFlow<Map<String, com.example.data.model.EventRsvp>> = _userRsvps.asStateFlow()

    // Latest 2 YouTube videos across channels (sorted newest first, no duplicates)
    val latestYouTubeVideos: StateFlow<List<YouTubeVideo>> = youtubeVideos
        .map { videos ->
            videos.sortedByDescending { it.publishedTimestamp }.take(2)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Saved Items
    val savedItems: StateFlow<List<SavedItemEntity>> = savedItemRepository
        .getAllSavedItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Intentional Randomized Feed (Blogs + Videos)
    private val _mixedRandomFeed = MutableStateFlow<List<MixedFeedItem>>(emptyList())
    val mixedRandomFeed: StateFlow<List<MixedFeedItem>> = _mixedRandomFeed.asStateFlow()

    // Recently Viewed
    val recentlyViewed: StateFlow<List<RecentlyViewedEntity>> = recentlyViewedRepository
        .getRecentItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Global Search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedSearchFilter = MutableStateFlow("ALL") // ALL, BLOG, VIDEO, PLAYLIST, BIBLE
    val selectedSearchFilter: StateFlow<String> = _selectedSearchFilter.asStateFlow()

    val globalSearchResults: StateFlow<List<SearchResultItem>> = combine(
        searchQuery.debounce(500L),
        selectedSearchFilter,
        allPosts,
        youtubeVideos
    ) { query, filter, posts, videos ->
        if (query.isBlank()) {
            return@combine emptyList<SearchResultItem>()
        }
        val q = query.trim().lowercase()
        val results = mutableListOf<SearchResultItem>()

        // 1. Blogs (Fellowship + Personal Vlog if enabled)
        if (filter == "ALL" || filter == "BLOG") {
            posts.filter { post ->
                post.title.lowercase().contains(q) ||
                post.plainTextExcerpt.lowercase().contains(q) ||
                post.labels.any { it.lowercase().contains(q) }
            }.take(20).forEach { post ->
                results.add(
                    SearchResultItem(
                        id = "POST_${post.id}",
                        type = SearchResultType.BLOG,
                        title = post.title,
                        snippet = post.plainTextExcerpt.take(120),
                        imageUrl = post.featuredImageUrl,
                        post = post
                    )
                )
            }
        }

        // 2. YouTube Videos
        if (filter == "ALL" || filter == "VIDEO") {
            videos.filter { vid ->
                vid.title.lowercase().contains(q) ||
                vid.description.lowercase().contains(q) ||
                vid.channelTitle.lowercase().contains(q)
            }.take(15).forEach { vid ->
                results.add(
                    SearchResultItem(
                        id = "VID_${vid.id}",
                        type = SearchResultType.VIDEO,
                        title = vid.title,
                        snippet = "${vid.channelTitle} • ${vid.publishedAt}",
                        imageUrl = vid.thumbnailUrl,
                        video = vid
                    )
                )
            }
        }

        // 3. Playlists
        if (filter == "ALL" || filter == "PLAYLIST") {
            PredefinedPlaylists.items.filter { pl ->
                pl.title.lowercase().contains(q) ||
                pl.channelTitle.lowercase().contains(q)
            }.forEach { pl ->
                results.add(
                    SearchResultItem(
                        id = "PL_${pl.id}",
                        type = SearchResultType.PLAYLIST,
                        title = pl.title,
                        snippet = "Playlist • ${pl.channelTitle}",
                        playlist = pl
                    )
                )
            }
        }

        // 4. Bible Verses (Simple lookup from predefined books or match)
        if (filter == "ALL" || filter == "BIBLE") {
            com.example.data.bible.model.BibleBookDefinitions.books.filter { book ->
                book.nameEnglish.lowercase().contains(q) ||
                book.nameHindi.contains(q)
            }.take(5).forEach { book ->
                results.add(
                    SearchResultItem(
                        id = "BIBLE_${book.id}",
                        type = SearchResultType.BIBLE,
                        title = "${book.nameEnglish} (${book.nameHindi})",
                        snippet = "Holy Bible • ${book.testament.name} • ${book.chapterCount} Chapters",
                        bibleBookId = book.id.toString(),
                        bibleChapter = 1,
                        bibleVerse = 1
                    )
                )
            }
        }

        results
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refreshAll()
        // Synchronized feed generator from local Room database flow
        viewModelScope.launch {
            combine(allPosts, youtubeVideos) { posts, videos ->
                Pair(posts, videos)
            }.collect { (posts, videos) ->
                if (_mixedRandomFeed.value.isEmpty() && (posts.isNotEmpty() || videos.isNotEmpty())) {
                    buildMixedRandomFeed()
                }
            }
        }
        // Schedule alarms on launch if enabled
        val s = preferencesManager.settings.value
        if (s.verseAlarmEnabled) {
            com.example.util.VerseAlarmScheduler.scheduleNextAlarm(getApplication(), s)
        }
        if (s.readingPlanReminderEnabled) {
            com.example.util.ReadingPlanReminderScheduler.scheduleDailyReminder(
                getApplication(),
                s.readingPlanReminderHour,
                s.readingPlanReminderMinute,
                s.readingPlanReminderEnabled
            )
        }
        if (s.dailyPrayerReminderEnabled) {
            com.example.util.DailyPrayerReminderScheduler.scheduleDailyReminder(
                getApplication(),
                s.dailyPrayerReminderHour,
                s.dailyPrayerReminderMinute,
                s.dailyPrayerReminderEnabled
            )
        }
    }

    fun isOnline(): Boolean {
        val cm = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun buildMixedRandomFeed() {
        val posts = if (allPosts.value.isNotEmpty()) allPosts.value else fellowshipPosts.value
        val videos = if (youtubeVideos.value.isNotEmpty()) youtubeVideos.value else latestYouTubeVideos.value

        val merged = mutableListOf<MixedFeedItem>()
        posts.forEach { merged.add(MixedFeedItem.BlogPostItem(it)) }
        videos.forEach { merged.add(MixedFeedItem.VideoItem(it)) }

        // True random shuffle of the unified merged list
        _mixedRandomFeed.value = merged.shuffled(kotlin.random.Random(System.currentTimeMillis()))
    }

    fun shuffleMixedFeed() {
        buildMixedRandomFeed()
    }

    fun refreshAll() {
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshing.value = true
            _isOffline.value = !isOnline()
            _errorMessage.value = null

            // 1. Synchronized Fetching: Wait for BOTH Blogger and YouTube data to fetch completely in the background to avoid UI jumping and race conditions
            val isVlogAllowed = isPersonalVlogAllowed.value
            val bloggerDeferred = async { bloggerRepository.refreshPosts(isVlogAllowed) }
            val ytDeferred = async { youtubeRepository.refreshChannelVideos() }
            val (bloggerRes, ytRes) = awaitAll(bloggerDeferred, ytDeferred)

            if (bloggerRes.isFailure && ytRes.isFailure && !isOnline()) {
                _isOffline.value = true
                _errorMessage.value = "You're offline. Showing saved content."
            } else if (bloggerRes.isFailure) {
                _errorMessage.value = "Fellowship Events feed is temporarily unavailable."
            }

            // 2. Intentional Shuffling: Merge both result lists into a single unified list and apply true random shuffle
            buildMixedRandomFeed()

            _isRefreshing.value = false
        }
    }

    fun setSelectedChannel(channel: YouTubeChannelInfo) {
        _selectedChannel.value = channel
    }

    fun loadPlaylistVideos(playlist: YouTubePlaylist) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingPlaylist.value = true
            val list = youtubeRepository.getPlaylistVideos(playlist)
            _currentPlaylistVideos.value = list
            _isLoadingPlaylist.value = false
        }
    }

    /**
     * Endless Scroll / Pagination Logic:
     * Initiates a background fetch for the next set of videos (YouTube & Dailymotion).
     * Saves new items into Room Database / Merged Feed and invokes [onAppended].
     */
    fun loadMoreVideos(onAppended: ((List<YouTubeVideo>) -> Unit)? = null) {
        if (_isLoadingMoreVideos.value) return
        if (!hasMoreYouTube && !hasMoreDailymotion) return

        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingMoreVideos.value = true
            val newlyFetched = mutableListOf<YouTubeVideo>()

            try {
                // 1. Fetch Dailymotion Next Page using 'page' query parameter
                if (hasMoreDailymotion) {
                    val isVlogAllowed = isPersonalVlogAllowed.value
                    val dmResult = youtubeRepository.fetchDailymotionPaginated(
                        page = dailymotionPage + 1,
                        limit = 10,
                        includePersonalVlog = isVlogAllowed
                    )
                    if (dmResult.videos.isNotEmpty()) {
                        dailymotionPage++
                        hasMoreDailymotion = dmResult.hasMore
                        newlyFetched.addAll(dmResult.videos)
                    } else {
                        hasMoreDailymotion = false
                    }
                }

                // 2. Fetch YouTube Next Page using 'pageToken'
                if (hasMoreYouTube) {
                    val ytResult = youtubeRepository.fetchMoreChannelVideos(
                        channelId = _selectedChannel.value.id,
                        channelTitle = _selectedChannel.value.name,
                        pageToken = youtubePageToken
                    )
                    if (ytResult.videos.isNotEmpty()) {
                        youtubePageToken = ytResult.nextPageToken
                        hasMoreYouTube = ytResult.hasMore
                        newlyFetched.addAll(ytResult.videos)
                    } else {
                        hasMoreYouTube = false
                    }
                }

                if (newlyFetched.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        onAppended?.invoke(newlyFetched)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoadingMoreVideos.value = false
            }
        }
    }

    fun resetVideoPagination() {
        dailymotionPage = 1
        hasMoreDailymotion = true
        youtubePageToken = null
        hasMoreYouTube = true
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedSearchFilter(filter: String) {
        _selectedSearchFilter.value = filter
    }

    // Saved Items operations
    fun isSaved(id: String): Flow<Boolean> = savedItemRepository.isSavedFlow(id)

    fun toggleSaveItem(
        id: String,
        type: String,
        title: String,
        subtitle: String,
        imageUrl: String? = null,
        url: String? = null,
        extraDataJson: String? = null,
        isCurrentlySaved: Boolean
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            savedItemRepository.toggleSave(
                SavedItemEntity(
                    id = id,
                    type = type,
                    title = title,
                    subtitle = subtitle,
                    imageUrl = imageUrl,
                    url = url,
                    extraDataJson = extraDataJson
                ),
                isCurrentlySaved
            )
        }
    }

    fun removeSavedItem(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            savedItemRepository.remove(id)
            if (id.startsWith("SONG_")) {
                val songId = id.removePrefix("SONG_").toLongOrNull()
                if (songId != null) {
                    lyricsRepository.setFavorite(songId, false)
                }
            }
        }
    }

    // Recently Viewed operations
    fun recordRecentlyViewed(
        id: String,
        type: String,
        title: String,
        subtitle: String,
        imageUrl: String? = null,
        extraDataJson: String? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            recentlyViewedRepository.record(id, type, title, subtitle, imageUrl, extraDataJson)
        }
    }

    fun clearRecentlyViewed() {
        viewModelScope.launch(Dispatchers.IO) {
            recentlyViewedRepository.clearHistory()
        }
    }

    // Settings operations
    fun updateThemeMode(mode: ThemeMode) {
        preferencesManager.updateThemeMode(mode)
    }

    fun updateAppLanguage(lang: AppLanguage) {
        preferencesManager.updateAppLanguage(lang)
    }

    fun toggleFavoriteCategory(category: String) {
        val current = settings.value.favoriteCategories.toMutableSet()
        if (current.contains(category)) current.remove(category) else current.add(category)
        preferencesManager.updateFavoriteCategories(current)
    }

    fun toggleHomeSection(section: HomeSectionType, enabled: Boolean) {
        preferencesManager.toggleHomeSection(section, enabled)
    }

    fun updateHomeSectionsOrder(order: List<HomeSectionType>) {
        preferencesManager.updateHomeSectionsOrder(order)
    }

    fun updateShowFellowshipEvents(enabled: Boolean) {
        preferencesManager.updateShowFellowshipEvents(enabled)
    }

    fun updateShowPersonalVlog(enabled: Boolean) {
        preferencesManager.updateShowPersonalVlog(enabled)
        if (enabled) {
            viewModelScope.launch(Dispatchers.IO) {
                bloggerRepository.refreshPosts(showPersonalVlog = true)
            }
        }
    }

    fun updateShowYouTube(enabled: Boolean) {
        preferencesManager.updateShowYouTube(enabled)
    }

    fun updateShowShorts(enabled: Boolean) {
        preferencesManager.updateShowShorts(enabled)
    }

    fun updateNotifyFellowshipEvents(enabled: Boolean) {
        preferencesManager.updateNotifyFellowshipEvents(enabled)
    }

    fun updateNotifyYouTube(enabled: Boolean) {
        preferencesManager.updateNotifyYouTube(enabled)
    }

    fun updateNotifyPersonalVlog(enabled: Boolean) {
        preferencesManager.updateNotifyPersonalVlog(enabled)
    }

    fun updateNotifyUpcomingReminders(enabled: Boolean) {
        preferencesManager.updateNotifyUpcomingReminders(enabled)
    }

    fun updatePersonalVlogMode(mode: PersonalVlogMode) {
        preferencesManager.updatePersonalVlogMode(mode)
        if (mode != PersonalVlogMode.HIDDEN) {
            viewModelScope.launch(Dispatchers.IO) {
                bloggerRepository.refreshPosts(showPersonalVlog = true)
            }
        }
    }

    fun updateBibleReadingStyle(style: BibleReadingStyle) {
        preferencesManager.updateBibleReadingStyle(style)
    }

    fun updateYouTubeDefaultTab(tab: YouTubeDefaultTab) {
        preferencesManager.updateYouTubeDefaultTab(tab)
    }

    fun updateCustomFourthTab(tab: CustomFourthTab) {
        preferencesManager.updateCustomFourthTab(tab)
    }

    fun updateBloggerPhotoLayout(layout: BloggerPhotoLayout) {
        preferencesManager.updateBloggerPhotoLayout(layout)
    }

    fun updateIsDrawerEnabled(enabled: Boolean) {
        preferencesManager.updateIsDrawerEnabled(enabled)
    }

    fun updateDrawerPosition(position: String) {
        preferencesManager.updateDrawerPosition(position)
    }

    fun updateNavTabsOrder(order: List<String>) {
        preferencesManager.updateNavTabsOrder(order)
    }

    fun updateActivePlanIds(ids: Set<String>) {
        preferencesManager.updateActivePlanIds(ids)
    }

    fun updateReadingPlanColors(behindHex: String, onTrackHex: String, completedHex: String) {
        preferencesManager.updateReadingPlanColors(behindHex, onTrackHex, completedHex)
    }

    fun updateDataSaver(enabled: Boolean) {
        preferencesManager.updateDataSaverEnabled(enabled)
    }

    fun scheduleEventReminder(
        context: Context,
        event: UpcomingEvent,
        offset: ReminderScheduler.ReminderOffset
    ): Boolean {
        return ReminderScheduler.scheduleReminder(
            context = context,
            postId = event.post.id,
            eventTitle = event.title,
            eventDate = event.dateString,
            eventTimestamp = event.startTimestamp,
            offset = offset
        )
    }

    fun submitFellowshipRsvp(
        eventId: String,
        userName: String,
        userContact: String,
        status: String = "GOING",
        attendeesCount: Int = 1,
        notes: String = "",
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val rsvp = com.example.data.model.EventRsvp(
            eventId = eventId,
            userName = userName,
            userContact = userContact,
            status = status,
            attendeesCount = attendeesCount,
            timestamp = System.currentTimeMillis(),
            notes = notes
        )
        // Store locally
        val current = _userRsvps.value.toMutableMap()
        current[eventId] = rsvp
        _userRsvps.value = current

        // Push to Firebase Realtime Database
        firebaseDataRepository.submitRsvp(
            rsvp = rsvp,
            onSuccess = onSuccess,
            onError = onError
        )
    }

    fun cancelFellowshipRsvp(eventId: String) {
        val current = _userRsvps.value.toMutableMap()
        current.remove(eventId)
        _userRsvps.value = current
    }

    fun clearCache() {
        viewModelScope.launch(Dispatchers.IO) {
            bloggerRepository.clearCache()
            youtubeRepository.clearCache()
            refreshAll()
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun checkForAppUpdates(force: Boolean = true) {
        viewModelScope.launch {
            val result = appUpdateManager.checkForUpdates(force)
            if (result.isUpdateAvailable) {
                val s = settings.value
                welcomeSpeechManager.speakUpdateAnnouncement(s.userName)
            }
        }
    }

    fun downloadAndInstallAppUpdate() {
        viewModelScope.launch {
            appUpdateManager.downloadAndInstallApk()
        }
    }

    fun installDownloadedApk() {
        appUpdateManager.installDownloadedApk()
    }

    fun speakUpdateAnnouncement() {
        val s = settings.value
        welcomeSpeechManager.speakUpdateAnnouncement(s.userName)
    }

    fun updateGitHubRepoPath(newPath: String) {
        appUpdateManager.updateCustomRepoPath(newPath)
    }

    fun triggerWelcomeSpeechOnLaunch() {
        val s = settings.value
        val activeVerse = getActiveTodayScripture()
        val isUpdateAvail = updateState.value.isUpdateAvailable
        val effectiveSpeechVolume = if (s.syncGreetingVolumeWithAlarm) s.alarmVolume else s.greetingSpeechVolume
        welcomeSpeechManager.speakOnAppOpen(
            userName = s.userName,
            enableWelcomeSpeech = s.enableWelcomeSpeech,
            enableVerseSpeech = s.enableVerseSpeechOnLaunch,
            welcomeOncePerDay = s.welcomeSpeechOncePerDay,
            verseOncePerDay = s.verseSpeechOncePerDay,
            todaysVerseText = activeVerse,
            isUpdateAvailable = isUpdateAvail,
            speechPitch = s.greetingSpeechPitch,
            speechSpeed = s.greetingSpeechSpeed,
            speechVolume = effectiveSpeechVolume
        )
    }

    fun testWelcomeSpeech() {
        val s = settings.value
        val activeVerse = getActiveTodayScripture()
        val effectiveSpeechVolume = if (s.syncGreetingVolumeWithAlarm) s.alarmVolume else s.greetingSpeechVolume
        welcomeSpeechManager.testSpeech(
            userName = s.userName,
            todaysVerseText = activeVerse,
            speechPitch = s.greetingSpeechPitch,
            speechSpeed = s.greetingSpeechSpeed,
            speechVolume = effectiveSpeechVolume
        )
    }

    fun updateUserName(name: String) = preferencesManager.updateUserName(name)
    fun updateEnableWelcomeSpeech(enabled: Boolean) = preferencesManager.updateEnableWelcomeSpeech(enabled)
    fun updateEnableVerseSpeechOnLaunch(enabled: Boolean) = preferencesManager.updateEnableVerseSpeechOnLaunch(enabled)
    fun updateWelcomeSpeechOncePerDay(oncePerDay: Boolean) = preferencesManager.updateWelcomeSpeechOncePerDay(oncePerDay)
    fun updateVerseSpeechOncePerDay(oncePerDay: Boolean) = preferencesManager.updateVerseSpeechOncePerDay(oncePerDay)
    fun updateWelcomeDialogDismissed(dismissed: Boolean) = preferencesManager.updateWelcomeDialogDismissed(dismissed)

    fun updateVerseAlarmEnabled(enabled: Boolean) {
        preferencesManager.updateVerseAlarmEnabled(enabled)
        val s = settings.value.copy(verseAlarmEnabled = enabled)
        if (enabled) {
            com.example.util.VerseAlarmScheduler.scheduleNextAlarm(getApplication(), s)
        } else {
            com.example.util.VerseAlarmScheduler.cancelAlarm(getApplication())
        }
    }

    fun updateVerseAlarmTime(hour: Int, minute: Int) {
        preferencesManager.updateVerseAlarmTime(hour, minute)
        val s = settings.value.copy(verseAlarmHour = hour, verseAlarmMinute = minute)
        if (s.verseAlarmEnabled) {
            com.example.util.VerseAlarmScheduler.scheduleNextAlarm(getApplication(), s)
        }
    }

    fun updateVerseAlarmFrequency(frequency: com.example.data.model.VerseAlarmFrequency) {
        preferencesManager.updateVerseAlarmFrequency(frequency)
        val s = settings.value.copy(verseAlarmFrequency = frequency)
        if (s.verseAlarmEnabled) {
            com.example.util.VerseAlarmScheduler.scheduleNextAlarm(getApplication(), s)
        }
    }

    fun updateVerseAlarmIntervalHours(intervalHours: Int) {
        preferencesManager.updateVerseAlarmIntervalHours(intervalHours)
        val s = settings.value.copy(verseAlarmIntervalHours = intervalHours)
        if (s.verseAlarmEnabled) {
            com.example.util.VerseAlarmScheduler.scheduleNextAlarm(getApplication(), s)
        }
    }

    fun updateVerseAlarmMode(mode: com.example.data.model.VerseAlarmMode) {
        preferencesManager.updateVerseAlarmMode(mode)
    }

    fun updateVerseAlarmContent(content: com.example.data.model.VerseAlarmContent) {
        preferencesManager.updateVerseAlarmContent(content)
    }

    fun updateSyncGreetingVolumeWithAlarm(sync: Boolean) {
        preferencesManager.updateSyncGreetingVolumeWithAlarm(sync)
    }

    fun updateGreetingSpeechVolume(volume: Float) {
        preferencesManager.updateGreetingSpeechVolume(volume)
    }

    fun updateGreetingSpeechPitch(pitch: Float) {
        preferencesManager.updateGreetingSpeechPitch(pitch)
    }

    fun updateGreetingSpeechSpeed(speed: Float) {
        preferencesManager.updateGreetingSpeechSpeed(speed)
    }

    fun updateAlarmVolume(volume: Float) {
        preferencesManager.updateAlarmVolume(volume)
    }

    fun updateReadingPlanReminderEnabled(enabled: Boolean) {
        preferencesManager.updateReadingPlanReminderEnabled(enabled)
        val s = settings.value.copy(readingPlanReminderEnabled = enabled)
        com.example.util.ReadingPlanReminderScheduler.scheduleDailyReminder(
            getApplication(),
            s.readingPlanReminderHour,
            s.readingPlanReminderMinute,
            enabled
        )
    }

    fun updateReadingPlanReminderTime(hour: Int, minute: Int) {
        preferencesManager.updateReadingPlanReminderTime(hour, minute)
        val s = settings.value.copy(readingPlanReminderHour = hour, readingPlanReminderMinute = minute)
        if (s.readingPlanReminderEnabled) {
            com.example.util.ReadingPlanReminderScheduler.scheduleDailyReminder(
                getApplication(),
                hour,
                minute,
                true
            )
        }
    }

    fun testVerseAlarm() {
        com.example.util.VerseAlarmScheduler.triggerTestAlarm(getApplication())
    }

    fun testVerseNotification() {
        com.example.util.VerseAlarmScheduler.triggerTestNotification(getApplication())
    }

    fun testReadingPlanReminder() {
        com.example.util.ReadingPlanReminderScheduler.triggerTestNotification(getApplication())
    }

    fun updateDailyPrayerReminderEnabled(enabled: Boolean) {
        preferencesManager.updateDailyPrayerReminderEnabled(enabled)
        val s = settings.value.copy(dailyPrayerReminderEnabled = enabled)
        com.example.util.DailyPrayerReminderScheduler.scheduleDailyReminder(
            getApplication(),
            s.dailyPrayerReminderHour,
            s.dailyPrayerReminderMinute,
            enabled
        )
    }

    fun updateDailyPrayerReminderTime(hour: Int, minute: Int) {
        preferencesManager.updateDailyPrayerReminderTime(hour, minute)
        val s = settings.value.copy(dailyPrayerReminderHour = hour, dailyPrayerReminderMinute = minute)
        if (s.dailyPrayerReminderEnabled) {
            com.example.util.DailyPrayerReminderScheduler.scheduleDailyReminder(
                getApplication(),
                hour,
                minute,
                true
            )
        }
    }

    fun updateDailyPrayerReminderSlot(slot: DailyPrayerSlot) {
        preferencesManager.updateDailyPrayerReminderSlot(slot)
        val s = preferencesManager.settings.value
        if (s.dailyPrayerReminderEnabled) {
            com.example.util.DailyPrayerReminderScheduler.scheduleDailyReminder(
                getApplication(),
                s.dailyPrayerReminderHour,
                s.dailyPrayerReminderMinute,
                true
            )
        }
    }

    fun testDailyPrayerReminder() {
        com.example.util.DailyPrayerReminderScheduler.triggerTestNotification(getApplication())
    }

    fun markAllNotificationsAsRead() {
        viewModelScope.launch {
            notificationRepository.markAllAsRead()
        }
    }

    fun deleteNotification(id: Long) {
        viewModelScope.launch {
            notificationRepository.deleteNotification(id)
        }
    }

    fun clearAllNotifications() {
        viewModelScope.launch {
            notificationRepository.clearAll()
        }
    }

    fun dismissAdminNotice(id: String) {
        adminNoticeRepository.dismissNotice(id)
    }

    fun addSampleNotification(title: String, body: String, linkUrl: String? = null) {
        viewModelScope.launch {
            notificationRepository.insertNotification(
                com.example.data.local.NotificationEntity(
                    title = title,
                    body = body,
                    timestamp = System.currentTimeMillis(),
                    isRead = false,
                    type = "admin",
                    linkUrl = linkUrl
                )
            )
        }
    }

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val db = AppDatabase.getInstance(app)
            val bibleDb = BibleDatabase.getInstance(app)
            val bloggerRepo = BloggerRepository(db)
            val ytRepo = YouTubeRepository(db)
            val savedRepo = SavedItemRepository(db)
            val recentRepo = RecentlyViewedRepository(db)
            val prefs = PreferencesManager(app)
            val bibleDao = bibleDb.bibleDao()
            val readingPlanRepo = com.example.data.bible.repository.ReadingPlanRepository(bibleDao)
            val studyNotesRepo = com.example.data.bible.repository.StudyNotesRepository(bibleDao)
            val lyricsRepo = com.example.data.bible.repository.LyricsRepository(bibleDao, savedItemRepository = savedRepo)
            val backupRepo = com.example.data.bible.repository.BackupRepository(app, bibleDao)
            val localDataSource = com.example.data.bible.local.BibleLocalDataSource(app, bibleDao)
            val remoteDataSource = com.example.data.bible.remote.BibleRemoteDataSource()
            val bibleRepo = com.example.data.bible.repository.BibleRepository(app, localDataSource, remoteDataSource)
            val syncCenterRepo = com.example.data.repository.SyncCenterRepository(app, bloggerRepo, ytRepo, bibleRepo, lyricsRepo)

            return MainViewModel(
                app,
                bloggerRepo,
                ytRepo,
                savedRepo,
                recentRepo,
                prefs,
                bibleDb,
                readingPlanRepo,
                studyNotesRepo,
                lyricsRepo,
                backupRepo,
                syncCenterRepo,
                bibleRepo
            ) as T
        }
    }
}
