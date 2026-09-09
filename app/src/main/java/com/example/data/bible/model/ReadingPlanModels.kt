package com.example.data.bible.model

data class ReadingPlanPortion(
    val bookId: Int,
    val bookNameHindi: String,
    val bookNameEnglish: String,
    val startChapter: Int,
    val endChapter: Int = startChapter
) {
    val displayHindi: String get() = if (startChapter == endChapter) "$bookNameHindi $startChapter" else "$bookNameHindi $startChapter-$endChapter"
    val displayEnglish: String get() = if (startChapter == endChapter) "$bookNameEnglish $startChapter" else "$bookNameEnglish $startChapter-$endChapter"
}

data class ReadingPlanDay(
    val dayNumber: Int,
    val title: String,
    val portions: List<ReadingPlanPortion>
)

data class ReadingPlanInfo(
    val id: String,
    val titleHindi: String,
    val titleEnglish: String,
    val descriptionHindi: String,
    val descriptionEnglish: String,
    val totalDays: Int,
    val category: String,
    val days: List<ReadingPlanDay>
)

object PredefinedReadingPlans {

    // 1. Gospels in 30 Days
    val gospels30Days = ReadingPlanInfo(
        id = "gospels_30",
        titleHindi = "सुसमाचार 30 दिन में",
        titleEnglish = "Gospels in 30 Days",
        descriptionHindi = "मत्ती, मरकुस, लूका और यूहन्ना के सुसमाचारों का 30 दिवसीय अध्ययन।",
        descriptionEnglish = "Read all four Gospels (Matthew, Mark, Luke, John) in 30 days.",
        totalDays = 30,
        category = "New Testament",
        days = (1..30).map { day ->
            val portions = when (day) {
                in 1..7 -> listOf(ReadingPlanPortion(40, "मत्ती", "Matthew", (day - 1) * 4 + 1, kotlin.math.min(day * 4, 28)))
                in 8..12 -> listOf(ReadingPlanPortion(41, "मरकुस", "Mark", (day - 8) * 3 + 1, kotlin.math.min((day - 7) * 3, 16)))
                in 13..21 -> listOf(ReadingPlanPortion(42, "लूका", "Luke", (day - 13) * 3 + 1, kotlin.math.min((day - 12) * 3, 24)))
                else -> listOf(ReadingPlanPortion(43, "यूहन्ना", "John", (day - 22) * 3 + 1, kotlin.math.min((day - 21) * 3, 21)))
            }
            ReadingPlanDay(
                dayNumber = day,
                title = "Day $day",
                portions = portions
            )
        }
    )

    // 2. Proverbs in 31 Days
    val proverbs31Days = ReadingPlanInfo(
        id = "proverbs_31",
        titleHindi = "नीतिवचन 31 दिन में",
        titleEnglish = "Proverbs in 31 Days",
        descriptionHindi = "प्रतिदिन नीतिवचन का 1 अध्याय पढ़कर ईश्वरीय बुद्धि और मार्गदर्शन प्राप्त करें।",
        descriptionEnglish = "One chapter of wisdom from Proverbs for every day of the month.",
        totalDays = 31,
        category = "Wisdom & Poetry",
        days = (1..31).map { day ->
            ReadingPlanDay(
                dayNumber = day,
                title = "Day $day",
                portions = listOf(ReadingPlanPortion(20, "नीतिवचन", "Proverbs", day, day))
            )
        }
    )

    // 3. Psalms in 60 Days
    val psalms60Days = ReadingPlanInfo(
        id = "psalms_60",
        titleHindi = "भजन संहिता 60 दिन में",
        titleEnglish = "Psalms in 60 Days",
        descriptionHindi = "भजन संहिता के सभी 150 अध्यायों की स्तुति और प्रार्थना यात्रा।",
        descriptionEnglish = "Journey through all 150 Psalms of praise and prayers in 60 days.",
        totalDays = 60,
        category = "Wisdom & Poetry",
        days = (1..60).map { day ->
            val start = (day - 1) * 2 + 1
            val end = kotlin.math.min(day * 2 + if (day == 60) 30 else 0, 150)
            ReadingPlanDay(
                dayNumber = day,
                title = "Day $day",
                portions = listOf(ReadingPlanPortion(19, "भजन संहिता", "Psalms", start, end))
            )
        }
    )

    // 4. New Testament in 90 Days
    val nt90Days = ReadingPlanInfo(
        id = "nt_90",
        titleHindi = "नया नियम 90 दिन में",
        titleEnglish = "New Testament in 90 Days",
        descriptionHindi = "पूरे नए नियम (260 अध्याय) को 90 दिनों में क्रमिक रूप से पूरा करें।",
        descriptionEnglish = "Read through the entire New Testament (260 chapters) in 90 days.",
        totalDays = 90,
        category = "New Testament",
        days = (1..90).map { day ->
            val approxChapterIndex = (day - 1) * 3 + 1
            // Map chapter index across NT books (id 40 to 66)
            var currentAccum = 0
            var selectedBook = BibleBookDefinitions.books.first { it.id == 40 }
            var bookChap = 1
            for (book in BibleBookDefinitions.newTestamentBooks) {
                if (currentAccum + book.chapterCount >= approxChapterIndex) {
                    selectedBook = book
                    bookChap = approxChapterIndex - currentAccum
                    break
                }
                currentAccum += book.chapterCount
            }
            val endChap = kotlin.math.min(bookChap + 2, selectedBook.chapterCount)
            ReadingPlanDay(
                dayNumber = day,
                title = "Day $day",
                portions = listOf(
                    ReadingPlanPortion(
                        selectedBook.id,
                        selectedBook.nameHindi,
                        selectedBook.nameEnglish,
                        bookChap,
                        endChap
                    )
                )
            )
        }
    )

    // 5. Whole Bible in 365 Days
    val wholeBible365 = ReadingPlanInfo(
        id = "whole_bible_365",
        titleHindi = "सम्पूर्ण पवित्र बाइबल 365 दिन में",
        titleEnglish = "Whole Bible in 365 Days",
        descriptionHindi = "पुराना और नया नियम मिलाकर प्रतिदिन 3-4 अध्याय की आत्मिक यात्रा।",
        descriptionEnglish = "Read through the entire Bible in one year with daily Old & New Testament readings.",
        totalDays = 365,
        category = "Complete Bible",
        days = (1..365).map { day ->
            // Distribute OT and NT chapters
            val otIndex = ((day - 1) * 929 / 365) + 1
            val ntIndex = ((day - 1) * 260 / 365) + 1

            // Find OT book
            var otAccum = 0
            var otBook = BibleBookDefinitions.oldTestamentBooks.first()
            var otChap = 1
            for (b in BibleBookDefinitions.oldTestamentBooks) {
                if (otAccum + b.chapterCount >= otIndex) {
                    otBook = b
                    otChap = otIndex - otAccum
                    break
                }
                otAccum += b.chapterCount
            }

            // Find NT book
            var ntAccum = 0
            var ntBook = BibleBookDefinitions.newTestamentBooks.first()
            var ntChap = 1
            for (b in BibleBookDefinitions.newTestamentBooks) {
                if (ntAccum + b.chapterCount >= ntIndex) {
                    ntBook = b
                    ntChap = ntIndex - ntAccum
                    break
                }
                ntAccum += b.chapterCount
            }

            ReadingPlanDay(
                dayNumber = day,
                title = "Day $day",
                portions = listOf(
                    ReadingPlanPortion(otBook.id, otBook.nameHindi, otBook.nameEnglish, otChap, kotlin.math.min(otChap + 2, otBook.chapterCount)),
                    ReadingPlanPortion(ntBook.id, ntBook.nameHindi, ntBook.nameEnglish, ntChap, ntChap)
                )
            )
        }
    )

    val allPlans = listOf(
        wholeBible365,
        nt90Days,
        gospels30Days,
        psalms60Days,
        proverbs31Days
    )

    fun getPlanById(id: String): ReadingPlanInfo? {
        return allPlans.find { it.id == id }
    }
}
