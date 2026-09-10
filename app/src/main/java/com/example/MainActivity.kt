package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AppLanguage
import com.example.data.model.AppStrings
import com.example.data.model.BlogPost
import com.example.data.model.GalleryPhoto
import com.example.data.model.LocalAppLanguage
import com.example.data.model.LocalAppStrings
import com.example.data.model.ThemeMode
import com.example.data.model.YouTubePlaylist
import com.example.data.model.YouTubeVideo
import com.example.data.model.appStrings
import com.example.ui.bible.BibleHomeScreen
import com.example.ui.bible.BibleReaderScreen
import com.example.ui.bible.BibleSavedScreen
import com.example.ui.bible.BibleSearchScreen
import com.example.ui.bible.BibleViewModel
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MainViewModel

enum class MainDestination(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    HOME("Home", Icons.Default.Home),
    BLOGS("Blogs", Icons.Default.Article),
    YOUTUBE("YouTube", Icons.Default.PlayCircle),
    PHOTOS("Photos", Icons.Default.PhotoLibrary),
    MORE("More", Icons.Default.MoreHoriz)
}

sealed interface AppRoute {
    data object Main : AppRoute
    data class PostDetail(val post: BlogPost) : AppRoute
    data class YouTubePlayer(val video: YouTubeVideo) : AppRoute
    data class PlaylistDetail(val playlist: YouTubePlaylist) : AppRoute
    data class PhotoViewer(val photos: List<GalleryPhoto>, val initialIndex: Int) : AppRoute
    data object Search : AppRoute
    data object Categories : AppRoute
    data object Settings : AppRoute
    data object About : AppRoute
    data object BibleHome : AppRoute
    data class BibleReader(val bookId: Int, val chapter: Int, val targetVerse: Int? = null) : AppRoute
    data object BibleSearch : AppRoute
    data object BibleSaved : AppRoute
    data object BibleReadingPlan : AppRoute
    data object DedicatedNotes : AppRoute
    data object Lyrics : AppRoute
    data object SyncCenter : AppRoute
    data object BackupRestore : AppRoute
    data object UpcomingEvents : AppRoute
    data object EventCalendar : AppRoute
    data object SavedItems : AppRoute
    data object RecentlyViewed : AppRoute
    data object HomeScreenSettings : AppRoute
}

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels {
        MainViewModel.Factory(application)
    }
    private val bibleViewModel: BibleViewModel by viewModels {
        BibleViewModel.Factory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val settings by viewModel.settings.collectAsState()
            val isDarkTheme = when (settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            val appStrings = remember(settings.appLanguage) {
                AppStrings.forLanguage(settings.appLanguage)
            }

            CompositionLocalProvider(
                LocalAppStrings provides appStrings,
                LocalAppLanguage provides settings.appLanguage
            ) {
                MyApplicationTheme(
                    darkTheme = isDarkTheme,
                    dynamicColor = false // Enforce clean branded Navy & Gold color scheme
                ) {
                    var showSplash by remember { mutableStateOf(true) }

                    if (showSplash) {
                        SplashScreen(onFinished = { showSplash = false })
                    } else {
                        AppNavigationHost(viewModel = viewModel, bibleViewModel = bibleViewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun AppNavigationHost(viewModel: MainViewModel, bibleViewModel: BibleViewModel) {
    val strings = appStrings()
    var currentDestination by remember { mutableStateOf(MainDestination.HOME) }
    var currentRoute by remember { mutableStateOf<AppRoute>(AppRoute.Main) }
    val galleryPhotos by viewModel.galleryPhotos.collectAsState()

    // Handle back press
    BackHandler(enabled = currentRoute != AppRoute.Main || currentDestination != MainDestination.HOME) {
        when (currentRoute) {
            is AppRoute.BibleReader, AppRoute.BibleSearch, AppRoute.BibleSaved -> {
                currentRoute = AppRoute.BibleHome
            }
            is AppRoute.BibleHome -> {
                currentRoute = AppRoute.Main
            }
            is AppRoute.EventCalendar -> {
                currentRoute = AppRoute.UpcomingEvents
            }
            AppRoute.Main -> {
                if (currentDestination != MainDestination.HOME) {
                    currentDestination = MainDestination.HOME
                }
            }
            else -> {
                currentRoute = AppRoute.Main
            }
        }
    }

    when (val route = currentRoute) {
        is AppRoute.Main -> {
            Scaffold(
                bottomBar = {
                    NavigationBar(
                        tonalElevation = 6.dp,
                        modifier = Modifier.testTag("bottom_nav_bar")
                    ) {
                        MainDestination.entries.forEach { dest ->
                            val selected = currentDestination == dest
                            val labelText = when (dest) {
                                MainDestination.HOME -> strings.navHome
                                MainDestination.BLOGS -> strings.navBlogs
                                MainDestination.YOUTUBE -> strings.navYouTube
                                MainDestination.PHOTOS -> strings.navPhotos
                                MainDestination.MORE -> strings.navMore
                            }
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentDestination = dest },
                                icon = {
                                    Icon(
                                        imageVector = dest.icon,
                                        contentDescription = labelText
                                    )
                                },
                                label = {
                                    Text(
                                        text = labelText,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                modifier = Modifier.testTag("nav_${dest.name.lowercase()}")
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (currentDestination) {
                        MainDestination.HOME -> {
                            HomeScreen(
                                viewModel = viewModel,
                                onPostClick = { currentRoute = AppRoute.PostDetail(it) },
                                onVideoClick = { currentRoute = AppRoute.YouTubePlayer(it) },
                                onPlaylistClick = { currentRoute = AppRoute.PlaylistDetail(it) },
                                onUpcomingEventsClick = { currentRoute = AppRoute.UpcomingEvents },
                                onSavedClick = { currentRoute = AppRoute.SavedItems },
                                onRecentlyViewedClick = { currentRoute = AppRoute.RecentlyViewed },
                                onCustomizeHomeClick = { currentRoute = AppRoute.HomeScreenSettings },
                                onViewAllPosts = { currentDestination = MainDestination.BLOGS },
                                onViewAllVideos = { currentDestination = MainDestination.YOUTUBE },
                                onViewGallery = { currentDestination = MainDestination.PHOTOS },
                                onReadVerse = { bId, chap, verseNum ->
                                    currentRoute = AppRoute.BibleReader(bId, chap, verseNum)
                                }
                            )
                        }
                        MainDestination.BLOGS -> {
                            BlogsScreen(
                                viewModel = viewModel,
                                onPostClick = { currentRoute = AppRoute.PostDetail(it) },
                                onSearchClick = { currentRoute = AppRoute.Search }
                            )
                        }
                        MainDestination.YOUTUBE -> {
                            YouTubeScreen(
                                viewModel = viewModel,
                                onVideoClick = { currentRoute = AppRoute.YouTubePlayer(it) },
                                onPlaylistClick = { currentRoute = AppRoute.PlaylistDetail(it) }
                            )
                        }
                        MainDestination.PHOTOS -> {
                            GalleryScreen(
                                viewModel = viewModel,
                                onPhotoClick = { idx ->
                                    currentRoute = AppRoute.PhotoViewer(galleryPhotos, idx)
                                },
                                onAlbumClick = { albumPhotos, idx ->
                                    currentRoute = AppRoute.PhotoViewer(albumPhotos, idx)
                                }
                            )
                        }
                        MainDestination.MORE -> {
                            MoreScreen(
                                viewModel = viewModel,
                                onUpcomingEventsClick = { currentRoute = AppRoute.UpcomingEvents },
                                onEventCalendarClick = { currentRoute = AppRoute.EventCalendar },
                                onSavedClick = { currentRoute = AppRoute.SavedItems },
                                onRecentlyViewedClick = { currentRoute = AppRoute.RecentlyViewed },
                                onCustomizeHomeClick = { currentRoute = AppRoute.HomeScreenSettings },
                                onBibleClick = { currentRoute = AppRoute.BibleHome },
                                onReadingPlanClick = { currentRoute = AppRoute.BibleReadingPlan },
                                onNotesClick = { currentRoute = AppRoute.DedicatedNotes },
                                onLyricsClick = { currentRoute = AppRoute.Lyrics },
                                onSyncCenterClick = { currentRoute = AppRoute.SyncCenter },
                                onBackupRestoreClick = { currentRoute = AppRoute.BackupRestore },
                                onSearchClick = { currentRoute = AppRoute.Search },
                                onCategoriesClick = { currentRoute = AppRoute.Categories },
                                onSettingsClick = { currentRoute = AppRoute.Settings },
                                onAboutClick = { currentRoute = AppRoute.About }
                            )
                        }
                    }
                }
            }
        }

        is AppRoute.PostDetail -> {
            PostDetailScreen(
                post = route.post,
                viewModel = viewModel,
                onBack = { currentRoute = AppRoute.Main },
                onImageClick = { imgUrl ->
                    val postPhotos = route.post.allImages.map { url ->
                        GalleryPhoto(
                            imageUrl = url,
                            postTitle = route.post.title,
                            postId = route.post.id,
                            source = route.post.source,
                            publishedDate = route.post.publishedDate
                        )
                    }
                    val targetList = if (postPhotos.isNotEmpty()) postPhotos else galleryPhotos
                    val idx = targetList.indexOfFirst { it.imageUrl == imgUrl }.coerceAtLeast(0)
                    currentRoute = AppRoute.PhotoViewer(targetList, idx)
                },
                onVideoClick = { vidId ->
                    val video = YouTubeVideo(
                        id = vidId,
                        title = route.post.title,
                        channelId = "",
                        channelTitle = "Vinay Kumar AVJ",
                        thumbnailUrl = "https://i.ytimg.com/vi/$vidId/hqdefault.jpg",
                        publishedAt = route.post.publishedDate,
                        publishedTimestamp = route.post.publishedTimestamp,
                        description = route.post.plainTextExcerpt
                    )
                    currentRoute = AppRoute.YouTubePlayer(video)
                },
                onPostClick = { currentRoute = AppRoute.PostDetail(it) },
                onPlaylistClick = { currentRoute = AppRoute.PlaylistDetail(it) }
            )
        }

        is AppRoute.YouTubePlayer -> {
            YouTubePlayerScreen(
                video = route.video,
                viewModel = viewModel,
                onBack = { currentRoute = AppRoute.Main },
                onRelatedVideoClick = { currentRoute = AppRoute.YouTubePlayer(it) }
            )
        }

        is AppRoute.PlaylistDetail -> {
            PlaylistDetailScreen(
                playlist = route.playlist,
                viewModel = viewModel,
                onBack = { currentRoute = AppRoute.Main },
                onVideoClick = { currentRoute = AppRoute.YouTubePlayer(it) }
            )
        }

        is AppRoute.PhotoViewer -> {
            PhotoViewerScreen(
                photos = route.photos,
                initialIndex = route.initialIndex,
                onBack = { currentRoute = AppRoute.Main }
            )
        }

        is AppRoute.UpcomingEvents -> {
            UpcomingEventsScreen(
                viewModel = viewModel,
                onPostClick = { currentRoute = AppRoute.PostDetail(it) },
                onOpenCalendarView = { currentRoute = AppRoute.EventCalendar },
                onBack = { currentRoute = AppRoute.Main }
            )
        }

        is AppRoute.EventCalendar -> {
            EventCalendarScreen(
                viewModel = viewModel,
                onPostClick = { currentRoute = AppRoute.PostDetail(it) },
                onBack = { currentRoute = AppRoute.UpcomingEvents }
            )
        }

        is AppRoute.SavedItems -> {
            SavedScreen(
                viewModel = viewModel,
                onPostClick = { currentRoute = AppRoute.PostDetail(it) },
                onVideoClick = { currentRoute = AppRoute.YouTubePlayer(it) },
                onPlaylistClick = { currentRoute = AppRoute.PlaylistDetail(it) },
                onBibleClick = { bId, ch -> currentRoute = AppRoute.BibleReader(bId, ch) },
                onBack = { currentRoute = AppRoute.Main }
            )
        }

        is AppRoute.RecentlyViewed -> {
            RecentlyViewedScreen(
                viewModel = viewModel,
                onPostClick = { currentRoute = AppRoute.PostDetail(it) },
                onVideoClick = { currentRoute = AppRoute.YouTubePlayer(it) },
                onPlaylistClick = { currentRoute = AppRoute.PlaylistDetail(it) },
                onBibleClick = { bId, ch -> currentRoute = AppRoute.BibleReader(bId, ch) },
                onBack = { currentRoute = AppRoute.Main }
            )
        }

        is AppRoute.HomeScreenSettings -> {
            HomeScreenSettingsScreen(
                viewModel = viewModel,
                onBack = { currentRoute = AppRoute.Main }
            )
        }

        is AppRoute.Search -> {
            SearchScreen(
                viewModel = viewModel,
                onPostClick = { currentRoute = AppRoute.PostDetail(it) },
                onVideoClick = { currentRoute = AppRoute.YouTubePlayer(it) },
                onPlaylistClick = { currentRoute = AppRoute.PlaylistDetail(it) },
                onBibleClick = { bId, ch -> currentRoute = AppRoute.BibleReader(bId, ch) },
                onBack = { currentRoute = AppRoute.Main }
            )
        }

        is AppRoute.Categories -> {
            CategoriesScreen(
                viewModel = viewModel,
                onCategorySelected = { label ->
                    viewModel.setSearchQuery(label)
                    currentRoute = AppRoute.Search
                },
                onBack = { currentRoute = AppRoute.Main }
            )
        }

        is AppRoute.Settings -> {
            SettingsScreen(
                viewModel = viewModel,
                onAboutClick = { currentRoute = AppRoute.About },
                onCustomizeHomeClick = { currentRoute = AppRoute.HomeScreenSettings },
                onSyncCenterClick = { currentRoute = AppRoute.SyncCenter },
                onBackupRestoreClick = { currentRoute = AppRoute.BackupRestore }
            )
        }

        is AppRoute.About -> {
            AboutScreen(
                onBack = { currentRoute = AppRoute.Main }
            )
        }

        is AppRoute.BibleReadingPlan -> {
            com.example.ui.bible.BibleReadingPlanScreen(
                planRepository = viewModel.readingPlanRepository,
                onBackClick = { currentRoute = AppRoute.Main },
                onOpenBible = { bId, ch -> currentRoute = AppRoute.BibleReader(bId, ch) }
            )
        }

        is AppRoute.DedicatedNotes -> {
            DedicatedNotesScreen(
                notesRepository = viewModel.dedicatedNotesRepository,
                onBackClick = { currentRoute = AppRoute.Main },
                onOpenVerse = { bId, ch, v -> currentRoute = AppRoute.BibleReader(bId, ch, v) }
            )
        }

        is AppRoute.Lyrics -> {
            LyricsScreen(
                lyricsRepository = viewModel.lyricsRepository,
                onBackClick = { currentRoute = AppRoute.Main }
            )
        }

        is AppRoute.SyncCenter -> {
            SyncCenterScreen(
                syncRepository = viewModel.syncCenterRepository,
                onBackClick = { currentRoute = AppRoute.Main }
            )
        }

        is AppRoute.BackupRestore -> {
            BackupRestoreScreen(
                backupRepository = viewModel.backupRepository,
                onBackClick = { currentRoute = AppRoute.Main }
            )
        }

        is AppRoute.BibleHome -> {
            BibleHomeScreen(
                viewModel = bibleViewModel,
                onBackClick = { currentRoute = AppRoute.Main },
                onOpenReader = { bId, chap, targetV ->
                    currentRoute = AppRoute.BibleReader(bId, chap, targetV)
                },
                onSearchClick = { currentRoute = AppRoute.BibleSearch },
                onSavedClick = { currentRoute = AppRoute.BibleSaved }
            )
        }

        is AppRoute.BibleReader -> {
            BibleReaderScreen(
                viewModel = bibleViewModel,
                bookId = route.bookId,
                chapter = route.chapter,
                targetVerse = route.targetVerse,
                onBackClick = { currentRoute = AppRoute.BibleHome },
                onSearchClick = { currentRoute = AppRoute.BibleSearch },
                onSavedClick = { currentRoute = AppRoute.BibleSaved }
            )
        }

        is AppRoute.BibleSearch -> {
            BibleSearchScreen(
                viewModel = bibleViewModel,
                onBackClick = { currentRoute = AppRoute.BibleHome },
                onVerseClick = { bId, chap, verseNum ->
                    currentRoute = AppRoute.BibleReader(bId, chap, verseNum)
                }
            )
        }

        is AppRoute.BibleSaved -> {
            BibleSavedScreen(
                viewModel = bibleViewModel,
                onBackClick = { currentRoute = AppRoute.BibleHome },
                onVerseClick = { bId, chap, verseNum ->
                    currentRoute = AppRoute.BibleReader(bId, chap, verseNum)
                }
            )
        }
    }
}
