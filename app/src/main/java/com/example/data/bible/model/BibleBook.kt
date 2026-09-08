package com.example.data.bible.model

data class BibleBook(
    val id: Int, // 1 to 66
    val nameHindi: String,
    val nameEnglish: String,
    val abbreviationHindi: String,
    val abbreviationEnglish: String,
    val testament: Testament,
    val chapterCount: Int,
    val category: String // e.g. "व्यवस्था", "इतिहास", "काव्य", "भविष्यद्वाणी", "सुसमाचार", "पत्रियां"
)
