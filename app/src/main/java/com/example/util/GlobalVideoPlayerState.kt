package com.example.util

import com.example.data.model.YouTubeVideo
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Global Video Player state manager supporting continuous Mini Player and Fullscreen controls
 * across the entire app hierarchy.
 */
object GlobalVideoPlayerState {
    private val _currentVideo = MutableStateFlow<YouTubeVideo?>(null)
    val currentVideo = _currentVideo.asStateFlow()

    private val _isMiniPlayerActive = MutableStateFlow(false)
    val isMiniPlayerActive = _isMiniPlayerActive.asStateFlow()

    private val _isPlaying = MutableStateFlow(true)
    val isPlaying = _isPlaying.asStateFlow()

    // Dynamic On-Screen Rect for Picture-in-Picture Source Rect Hint
    private val _activePipBounds = MutableStateFlow<android.graphics.Rect?>(null)
    val activePipBounds = _activePipBounds.asStateFlow()

    fun updatePipBounds(bounds: android.graphics.Rect?) {
        _activePipBounds.value = bounds
    }

    private val _commandFlow = MutableSharedFlow<String>(extraBufferCapacity = 10)
    val commandFlow = _commandFlow.asSharedFlow()

    fun openVideo(video: YouTubeVideo) {
        _currentVideo.value = video
        _isMiniPlayerActive.value = false
        _isPlaying.value = true
        VideoPlaybackTracker.setPlaying(video.id, true)
    }

    fun minimizeToMiniPlayer() {
        if (_currentVideo.value != null) {
            _isMiniPlayerActive.value = true
        }
    }

    fun expandToFullScreen() {
        if (_currentVideo.value != null) {
            _isMiniPlayerActive.value = false
        }
    }

    fun setPlaying(playing: Boolean) {
        _isPlaying.value = playing
        _currentVideo.value?.let { vid ->
            VideoPlaybackTracker.setPlaying(vid.id, playing)
        }
    }

    fun togglePlayPause() {
        val next = !_isPlaying.value
        setPlaying(next)
        _commandFlow.tryEmit(if (next) "PLAY" else "PAUSE")
        if (next) {
            SharedVideoPlayerManager.playVideo()
        } else {
            SharedVideoPlayerManager.pauseVideo()
        }
    }

    fun closePlayer() {
        _currentVideo.value?.let { vid ->
            VideoPlaybackTracker.clear(vid.id)
        }
        SharedVideoPlayerManager.releasePlayer()
        _currentVideo.value = null
        _isMiniPlayerActive.value = false
        _isPlaying.value = false
    }
}
