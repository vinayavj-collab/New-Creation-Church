package com.example.util

data class YouTubePlayerSettings(
    val hideTitleAndShare: Boolean = true,
    val enableBackgroundPip: Boolean = true,
    val autoplayNext: Boolean = true,
    val showControls: Boolean = true,
    val selectedQuality: String = "auto"
)
