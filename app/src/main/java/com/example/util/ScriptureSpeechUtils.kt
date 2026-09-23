package com.example.util

import com.example.data.bible.model.VerseOfTheDay

object ScriptureSpeechUtils {

    /**
     * Formats a structured VerseOfTheDay for speech:
     * First announces Book + Chapter + Verse (e.g. "यूहन्ना अध्याय 3, वचन 16। ... "),
     * followed by the verse text.
     */
    fun formatVerseForSpeech(verse: VerseOfTheDay): String {
        val book = verse.bookNameHindi.ifBlank { verse.bookNameEnglish }
        return "$book अध्याय ${verse.chapter}, वचन ${verse.verseNumber}। ... ${verse.textHindi}"
    }

    /**
     * Converts scripture text/references into spoken Hindi text format.
     * Ensures book + chapter + verse is ALWAYS announced BEFORE the verse body.
     */
    fun formatScriptureTextForSpeech(text: String?): String {
        if (text.isNullOrBlank()) return ""

        val trimmed = text.trim()

        // 1. If text is in format "Verse Text - Book Chapter:Verse" or "Verse Text — Book Chapter:Verse"
        val dashSplit = trimmed.split(Regex("""\s*[-–—]\s*"""))
        if (dashSplit.size == 2) {
            val part1 = dashSplit[0].trim()
            val part2 = dashSplit[1].trim()

            // Check if part2 is a reference (e.g., "यूहन्ना 3:16", "भजन संहिता 23:1", "रोमियों 8:28", "लूका 10:10-12")
            val refPattern = Regex("""^([^\d]+)\s*(\d+)\s*:\s*(\d+)(?:\s*-\s*(\d+))?$""")
            val match = refPattern.find(part2)
            if (match != null) {
                val book = match.groupValues[1].trim()
                val chapter = match.groupValues[2]
                val verseStart = match.groupValues[3]
                val verseEnd = match.groupValues.getOrNull(4)
                val verseStr = if (!verseEnd.isNullOrBlank()) "वचन $verseStart से $verseEnd" else "वचन $verseStart"
                return "$book अध्याय $chapter, $verseStr। ... $part1"
            }
        }

        // 2. Matches chapter:verse format like 1:10, 10:10, 3:16-17, 12:1-5 in existing text
        val verseRefRegex = Regex("""(\d+)\s*:\s*(\d+)(?:\s*-\s*(\d+))?""")
        val result = verseRefRegex.replace(trimmed) { matchResult ->
            val chapter = matchResult.groupValues[1]
            val verseStart = matchResult.groupValues[2]
            val verseEnd = matchResult.groupValues.getOrNull(3)

            if (!verseEnd.isNullOrBlank()) {
                "अध्याय $chapter, वचन $verseStart से $verseEnd। ... "
            } else {
                "अध्याय $chapter, वचन $verseStart। ... "
            }
        }

        return result.replace(Regex("""\s*[-–—]\s*"""), "। ... ")
    }
}

