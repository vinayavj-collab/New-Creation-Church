package com.example.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.data.model.YouTubeVideo
import com.example.ui.viewmodel.MainViewModel

/**
 * YouTubePlayerScreen delegates to the universal VideoPlayerScreen,
 * supporting both YouTube and Dailymotion videos seamlessly.
 */
@Composable
fun YouTubePlayerScreen(
    video: YouTubeVideo,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onRelatedVideoClick: (YouTubeVideo) -> Unit,
    modifier: Modifier = Modifier
) {
    VideoPlayerScreen(
        video = video,
        viewModel = viewModel,
        onBack = onBack,
        onRelatedVideoClick = onRelatedVideoClick,
        modifier = modifier
    )
}
