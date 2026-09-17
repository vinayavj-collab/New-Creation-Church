package com.example.ui.components

import android.content.Context
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.data.model.YouTubeVideo
import com.example.ui.adapters.YouTubeVideoAdapter
import com.example.ui.util.EndlessRecyclerViewScrollListener
import com.example.ui.viewmodel.MainViewModel
import com.example.util.VideoPlatform

/**
 * Custom Endless Video RecyclerView Composable wrapper.
 * Combines:
 * 1. EndlessRecyclerViewScrollListener for continuous pagination
 * 2. Background fetching for YouTube (pageToken) and Dailymotion (page)
 * 3. Seamless appending using adapter.appendVideos(...) & notifyItemRangeInserted
 * 4. Subtle loading spinner footer while loading next batch
 */
@Composable
fun VideoRecyclerView(
    videos: List<YouTubeVideo>,
    viewModel: MainViewModel,
    onVideoClick: (YouTubeVideo) -> Unit,
    modifier: Modifier = Modifier
) {
    val isLoadingMore by viewModel.isLoadingMoreVideos.collectAsStateWithLifecycle()
    var adapterRef by remember { mutableStateOf<YouTubeVideoAdapter?>(null) }
    var scrollListenerRef by remember { mutableStateOf<EndlessRecyclerViewScrollListener?>(null) }

    // Sync subtle loading footer when state changes
    LaunchedEffect(isLoadingMore) {
        adapterRef?.setLoadingFooter(isLoadingMore)
    }

    // Sync initial or updated list seamlessly
    LaunchedEffect(videos) {
        adapterRef?.let { adapter ->
            if (adapter.getItems().isEmpty() && videos.isNotEmpty()) {
                adapter.setVideos(videos)
            } else if (videos.size > adapter.getItems().size) {
                val currentIds = adapter.getItems().map { it.id }.toSet()
                val newItems = videos.filterNot { currentIds.contains(it.id) }
                if (newItems.isNotEmpty()) {
                    adapter.appendVideos(newItems)
                } else {
                    adapter.submitList(videos)
                }
            } else {
                adapter.submitList(videos)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context: Context ->
                val recyclerView = RecyclerView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    clipToPadding = false
                    setPadding(0, 0, 0, 80)
                }

                val layoutManager = LinearLayoutManager(context)
                recyclerView.layoutManager = layoutManager

                val adapter = YouTubeVideoAdapter(
                    onItemClick = { video, platform ->
                        onVideoClick(video)
                    }
                )
                adapterRef = adapter
                adapter.setVideos(videos)
                recyclerView.adapter = adapter

                // 1. Endless Scroll Listener: Attach custom EndlessRecyclerViewScrollListener
                val scrollListener = object : EndlessRecyclerViewScrollListener(layoutManager) {
                    override fun onLoadMore(page: Int, totalItemsCount: Int, view: RecyclerView?) {
                        // 2. Pagination Logic: Detect near bottom scroll and fetch next set of videos
                        viewModel.loadMoreVideos { newVideos ->
                            // 4. Seamless Appending: Append without resetting scroll position
                            adapter.appendVideos(newVideos)
                        }
                    }
                }
                scrollListenerRef = scrollListener
                recyclerView.addOnScrollListener(scrollListener)

                recyclerView
            },
            update = { recyclerView ->
                // Ensure layout manager and items are consistent
            }
        )
    }
}
