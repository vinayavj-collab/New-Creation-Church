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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

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
    val dedicatedNotesRepository: com.example.data.bible.repository.DedicatedNotesRepository,
    val lyricsRepository: com.example.data.bible.repository.LyricsRepository,
    val backupRepository: com.example.data.bible.repository.BackupRepository,
    val syncCenterRepository: com.example.data.repository.SyncCenterRepository,
    val bibleRepository: com.example.data.bible.repository.BibleRepository
) : AndroidViewModel(application) {

    val settings: StateFlow<UserSettings> = preferencesManager.settings

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _isOffline = MutableStateFlow(!isOnline())
    val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Posts stream responding immediately to showPersonalVlog toggle
    val allPosts: StateFlow<List<BlogPost>> = settings
        .map { it.showPersonalVlog }
        .flatMapLatest { showPersonal ->
            bloggerRepository.getPostsFlow(showPersonal)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val fellowshipPosts: StateFlow<List<BlogPost>> = bloggerRepository
        .getPostsBySourceFlow(BlogSourceType.FELLOWSHIP_EVENTS)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val personalVlogPosts: StateFlow<List<BlogPost>> = bloggerRepository
        .getPostsBySourceFlow(BlogSourceType.PERSONAL_VLOG)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Photos derived from visible posts
    val galleryPhotos: StateFlow<List<GalleryPhoto>> = allPosts
        .map { posts -> bloggerRepository.extractPhotos(posts) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dynamic categories extracted from current posts
    val fellowshipCategories: StateFlow<List<String>> = fellowshipPosts
        .map { posts ->
            posts.flatMap { it.labels }.distinct().sorted()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val personalVlogCategories: StateFlow<List<String>> = personalVlogPosts
        .map { posts ->
            posts.flatMap { it.labels }.distinct().sorted()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // YouTube channel videos
    val youtubeVideos: StateFlow<List<YouTubeVideo>> = youtubeRepository
        .getAllVideosFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    // Upcoming Events: Extracted only from Fellowship Events with label "Upcoming"
    val upcomingEvents: StateFlow<List<UpcomingEvent>> = fellowshipPosts
        .map { posts ->
            posts.mapNotNull { EventExtractor.extractUpcomingEvent(it) }
                .sortedBy { it.startTimestamp }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Latest 2 YouTube videos across channels (sorted newest first, no duplicates)
    val latestYouTubeVideos: StateFlow<List<YouTubeVideo>> = youtubeVideos
        .map { videos ->
            videos.sortedByDescending { it.publishedTimestamp }.take(2)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Saved Items
    val savedItems: StateFlow<List<SavedItemEntity>> = savedItemRepository
        .getAllSavedItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
        searchQuery,
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
    }

    fun isOnline(): Boolean {
        val cm = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun refreshAll() {
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshing.value = true
            _isOffline.value = !isOnline()
            _errorMessage.value = null

            val bloggerRes = bloggerRepository.refreshPosts(settings.value.showPersonalVlog)
            val ytRes = youtubeRepository.refreshChannelVideos()

            if (bloggerRes.isFailure && ytRes.isFailure && !isOnline()) {
                _isOffline.value = true
                _errorMessage.value = "You're offline. Showing saved content."
            } else if (bloggerRes.isFailure) {
                _errorMessage.value = "Fellowship Events feed is temporarily unavailable."
            }

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
            val dedicatedNotesRepo = com.example.data.bible.repository.DedicatedNotesRepository(bibleDao)
            val lyricsRepo = com.example.data.bible.repository.LyricsRepository(bibleDao)
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
                dedicatedNotesRepo,
                lyricsRepo,
                backupRepo,
                syncCenterRepo,
                bibleRepo
            ) as T
        }
    }
}
