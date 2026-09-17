package com.example.data.bible.model

data class ReadingPlanHighlightStyle(
    val isVisible: Boolean = true,
    val windowFillColorHex: String = "#FDE68A", // Soft amber highlighter fill
    val strokeColorHex: String = "#D97706",     // Amber border stroke
    val alpha: Float = 0.35f,                  // 0.05f to 1.0f (35%)
    val borderThicknessDp: Float = 2.0f,        // 0.5f to 6.0f
    val cornerRadiusDp: Float = 8.0f            // 0.0f to 24.0f
)
