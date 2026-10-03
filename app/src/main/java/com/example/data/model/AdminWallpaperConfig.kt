package com.example.data.model

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

/**
 * Configuration model for the Master Admin AI Wallpaper settings.
 */
@Keep
@Serializable
data class AdminWallpaperConfig(
    val isEnabled: Boolean = true,
    val frequencyPerDay: Int = 1, // 1, 2, 3
    val targetScreen: String = "both", // "both", "lock", "home"
    val showOnboardingPrompt: Boolean = true,
    val customPromptPreset: String = "Cinematic biblical historical context, spiritual divine mood, golden heavenly light rays, sacred atmosphere",
    val selectedThematicStyle: String = "AUTO", // "AUTO", "CINEMATIC", "BOTANICAL", "PASTEL", "LIVING_WATER", "ROCK"
    val lastUpdatedTimestamp: Long = System.currentTimeMillis(),
    val updatedByAdmin: String = "Master Admin"
)
