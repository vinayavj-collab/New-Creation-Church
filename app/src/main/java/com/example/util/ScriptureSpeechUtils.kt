package com.example.util

object ScriptureSpeechUtils {

    /**
     * Converts scripture references with chapter:verse pattern (e.g. "लूका 1:10", "युहन्ना 10:10", "3:16-17")
     * into spoken Hindi text format ("लूका अध्याय 1, वचन 10").
     *
     * Adds pause punctuation (। ... ) after verse number to ensure a ~1-2 second gap before reading the verse body.
     */
    fun formatScriptureTextForSpeech(text: String?): String {
        if (text.isNullOrBlank()) return ""

        // Matches chapter:verse format like 1:10, 10:10, 3:16-17, 12:1-5
        val verseRefRegex = Regex("""(\d+)\s*:\s*(\d+)(?:\s*-\s*(\d+))?""")

        val result = verseRefRegex.replace(text) { matchResult ->
            val chapter = matchResult.groupValues[1]
            val verseStart = matchResult.groupValues[2]
            val verseEnd = matchResult.groupValues.getOrNull(3)

            if (!verseEnd.isNullOrBlank()) {
                "अध्याय $chapter, वचन $verseStart से $verseEnd। ... "
            } else {
                "अध्याय $chapter, वचन $verseStart। ... "
            }
        }

        // Replace dash separators between reference and verse text with pause punctuation to ensure 1-2 second gap
        return result.replace(Regex("""\s*[-–—]\s*"""), "। ... ")
    }
}
