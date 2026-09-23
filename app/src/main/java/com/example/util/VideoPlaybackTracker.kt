package com.example.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * Singleton repository for preserving video playback state (position and play/pause status)
 * across Configuration changes, Picture-in-Picture mode transitions, and screen rotations.
 */
object VideoPlaybackTracker {
    private val playbackPositions = ConcurrentHashMap<String, Float>()
    private val playbackDurations = ConcurrentHashMap<String, Float>()
    private val playbackPlaying = ConcurrentHashMap<String, Boolean>()
    var activeVideoId: String? = null

    private val _currentPosition = MutableStateFlow(0f)
    val currentPosition = _currentPosition.asStateFlow()

    private val _currentDuration = MutableStateFlow(0f)
    val currentDuration = _currentDuration.asStateFlow()

    fun setPosition(videoId: String, seconds: Float) {
        if (videoId.isNotBlank() && seconds >= 0f) {
            playbackPositions[videoId] = seconds
            if (activeVideoId == videoId || activeVideoId == null) {
                activeVideoId = videoId
                _currentPosition.value = seconds
            }
        }
    }

    fun setDuration(videoId: String, durationSeconds: Float) {
        if (videoId.isNotBlank() && durationSeconds > 0f) {
            playbackDurations[videoId] = durationSeconds
            if (activeVideoId == videoId || activeVideoId == null) {
                activeVideoId = videoId
                _currentDuration.value = durationSeconds
            }
        }
    }

    fun getPosition(videoId: String): Float {
        return playbackPositions[videoId] ?: 0f
    }

    fun getDuration(videoId: String): Float {
        return playbackDurations[videoId] ?: 0f
    }

    fun setPlaying(videoId: String, isPlaying: Boolean) {
        if (videoId.isNotBlank()) {
            playbackPlaying[videoId] = isPlaying
            if (isPlaying) {
                activeVideoId = videoId
                _currentPosition.value = getPosition(videoId)
                _currentDuration.value = getDuration(videoId)
            }
        }
    }

    fun isPlaying(videoId: String): Boolean {
        return playbackPlaying[videoId] ?: true
    }

    fun clear(videoId: String) {
        playbackPositions.remove(videoId)
        playbackDurations.remove(videoId)
        playbackPlaying.remove(videoId)
        if (activeVideoId == videoId) {
            activeVideoId = null
            _currentPosition.value = 0f
            _currentDuration.value = 0f
        }
    }
}
