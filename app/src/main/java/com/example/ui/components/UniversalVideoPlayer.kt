package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.util.VideoPlatform
import com.example.util.VideoUrlParser

/**
 * Universal Video Player embedded component.
 * Inspects the video URL/ID, automatically routes to:
 * - Official YouTube IFrame Player API (via YouTubePlayerView) for YouTube URLs
 * - Hardware-accelerated WebView with WebChromeClient for Dailymotion URLs
 */
@Composable
fun UniversalVideoPlayer(
    videoUrlOrId: String,
    modifier: Modifier = Modifier,
    autoplay: Boolean = true
) {
    val parsed = VideoUrlParser.parse(videoUrlOrId)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        when (parsed.platform) {
            VideoPlatform.DAILYMOTION -> {
                DailymotionPlayerView(
                    videoId = parsed.videoId,
                    modifier = Modifier.fillMaxSize(),
                    autoplay = autoplay
                )
            }
            VideoPlatform.YOUTUBE, VideoPlatform.UNKNOWN -> {
                YouTubePlayerView(
                    videoId = parsed.videoId,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
