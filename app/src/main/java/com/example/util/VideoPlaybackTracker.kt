package com.example.util

import java.util.concurrent.ConcurrentHashMap

/**
 * Singleton repository for preserving video playback state (position and play/pause status)
 * across Configuration changes, Picture-in-Picture mode transitions, and screen rotations.
 */
object VideoPlaybackTracker {
    private val playbackPositions = ConcurrentHashMap<String, Float>()
    private val playbackPlaying = ConcurrentHashMap<String, Boolean>()
    var activeVideoId: String? = null

    fun setPosition(videoId: String, seconds: Float) {
        if (videoId.isNotBlank() && seconds > 0f) {
            playbackPositions[videoId] = seconds
        }
    }

    fun getPosition(videoId: String): Float {
        return playbackPositions[videoId] ?: 0f
    }

    fun setPlaying(videoId: String, isPlaying: Boolean) {
        if (videoId.isNotBlank()) {
            playbackPlaying[videoId] = isPlaying
            if (isPlaying) {
                activeVideoId = videoId
            }
        }
    }

    fun isPlaying(videoId: String): Boolean {
        return playbackPlaying[videoId] ?: true
    }

    fun clear(videoId: String) {
        playbackPositions.remove(videoId)
        playbackPlaying.remove(videoId)
        if (activeVideoId == videoId) {
            activeVideoId = null
        }
    }
}
