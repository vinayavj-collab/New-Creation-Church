package com.example.data.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.regex.Pattern

data class UpcomingEvent(
    val post: BlogPost,
    val title: String,
    val dateString: String,
    val timeString: String?,
    val locationString: String?,
    val startTimestamp: Long
)

object EventExtractor {

    private val DATE_PATTERNS = listOf(
        Pattern.compile("(?:date|दिनांक|तारीख|dt)\\s*[:\\-–]\\s*([^<\\n\\r,]+)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("📅\\s*([^<\\n\\r,]+)"),
        Pattern.compile("\\b(\\d{1,2}(?:st|nd|rd|th)?\\s+(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\s+\\d{4})\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(\\d{1,2}[/\\-]\\d{1,2}[/\\-]\\d{2,4})\\b")
    )

    private val TIME_PATTERNS = listOf(
        Pattern.compile("(?:time|समय|timing)\\s*[:\\-–]\\s*([^<\\n\\r,]+)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("⏰\\s*([^<\\n\\r,]+)"),
        Pattern.compile("\\b(\\d{1,2}(?::\\d{2})?\\s*(?:AM|PM|am|pm))\\b")
    )

    private val LOCATION_PATTERNS = listOf(
        Pattern.compile("(?:location|venue|स्थान|जगह|place|address)\\s*[:\\-–]\\s*([^<\\n\\r]+)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("📍\\s*([^<\\n\\r]+)")
    )

    fun isUpcoming(post: BlogPost): Boolean {
        return post.labels.any { it.equals("Upcoming", ignoreCase = true) }
    }

    fun extractUpcomingEvent(post: BlogPost): UpcomingEvent? {
        if (!isUpcoming(post)) return null

        val textToScan = (post.title + "\n" + post.plainTextExcerpt + "\n" + post.contentHtml)
            .replace(Regex("<[^>]*>"), " ") // strip tags for clean text scanning

        var parsedDate: String? = null
        for (pattern in DATE_PATTERNS) {
            val matcher = pattern.matcher(textToScan)
            if (matcher.find()) {
                val group = matcher.group(1)?.trim()
                if (!group.isNullOrBlank() && group.length in 4..40) {
                    parsedDate = group
                    break
                }
            }
        }

        var parsedTime: String? = null
        for (pattern in TIME_PATTERNS) {
            val matcher = pattern.matcher(textToScan)
            if (matcher.find()) {
                val group = matcher.group(1)?.trim()
                if (!group.isNullOrBlank() && group.length in 3..30) {
                    parsedTime = group
                    break
                }
            }
        }

        var parsedLocation: String? = null
        for (pattern in LOCATION_PATTERNS) {
            val matcher = pattern.matcher(textToScan)
            if (matcher.find()) {
                val group = matcher.group(1)?.trim()
                if (!group.isNullOrBlank() && group.length in 3..60) {
                    parsedLocation = group
                    break
                }
            }
        }

        val finalDate = parsedDate ?: post.publishedDate
        val timestamp = parseToTimestamp(finalDate, parsedTime) ?: post.publishedTimestamp

        return UpcomingEvent(
            post = post,
            title = post.title,
            dateString = finalDate,
            timeString = parsedTime,
            locationString = parsedLocation,
            startTimestamp = timestamp
        )
    }

    private fun parseToTimestamp(dateStr: String, timeStr: String?): Long? {
        val formats = listOf(
            "dd MMM yyyy",
            "d MMM yyyy",
            "MMMM dd, yyyy",
            "dd/MM/yyyy",
            "yyyy-MM-dd"
        )
        for (fmt in formats) {
            try {
                val sdf = SimpleDateFormat(fmt, Locale.ENGLISH)
                val d = sdf.parse(dateStr)
                if (d != null) {
                    val cal = Calendar.getInstance()
                    cal.time = d
                    if (!timeStr.isNullOrBlank()) {
                        val timeLower = timeStr.lowercase(Locale.ENGLISH)
                        val isPm = timeLower.contains("pm")
                        val digits = timeStr.replace(Regex("[^0-9:]"), "").split(":")
                        var hour = digits.getOrNull(0)?.toIntOrNull() ?: 10
                        val min = digits.getOrNull(1)?.toIntOrNull() ?: 0
                        if (isPm && hour < 12) hour += 12
                        if (!isPm && hour == 12) hour = 0
                        cal.set(Calendar.HOUR_OF_DAY, hour)
                        cal.set(Calendar.MINUTE, min)
                    } else {
                        cal.set(Calendar.HOUR_OF_DAY, 10)
                        cal.set(Calendar.MINUTE, 0)
                    }
                    return cal.timeInMillis
                }
            } catch (e: Exception) {
                // Try next
            }
        }
        return null
    }

    fun generateEventShareText(event: UpcomingEvent): String {
        val dateLine = "📅 Date: ${event.dateString}"
        val timeLine = event.timeString?.let { "\n⏰ Time: $it" } ?: ""
        val locationLine = event.locationString?.let { "\n📍 Location: $it" } ?: ""
        val excerpt = event.post.plainTextExcerpt.take(200).trim()

        return """
FELLOWSHIP EVENTS

${event.title}

$dateLine$timeLine$locationLine

$excerpt

Read more:
${event.post.url}
        """.trimIndent()
    }
}
