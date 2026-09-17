package com.example.data.bible.model

import androidx.compose.ui.graphics.Color

data class ActiveReadingPlanItem(
    val planId: String,
    val title: String,
    val totalDays: Int,
    val completedDays: Int,
    val progressPercent: Int,
    val progressFraction: Float,
    val tintColor: Color,
    val currentDayNumber: Int = 1,
    val targetBookId: Int = 43,
    val targetChapter: Int = 1,
    val targetStartChapter: Int = targetChapter,
    val targetEndChapter: Int = targetChapter,
    val targetStartVerse: Int? = 1,
    val targetEndVerse: Int? = null
)

fun parsePlanColor(hex: String, defaultColor: Color): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        defaultColor
    }
}
