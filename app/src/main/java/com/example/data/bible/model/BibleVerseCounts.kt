package com.example.data.bible.model

import android.content.Context
import org.json.JSONObject

object BibleVerseCounts {
    private var verseCountCache: Map<Int, Map<Int, Int>>? = null

    fun initialize(context: Context) {
        if (verseCountCache != null) return
        try {
            val jsonStr = context.assets.open("bible/chapter_verse_counts.json")
                .bufferedReader()
                .use { it.readText() }
            val root = JSONObject(jsonStr)
            val result = mutableMapOf<Int, Map<Int, Int>>()
            val keys = root.keys()
            while (keys.hasNext()) {
                val bookIdStr = keys.next()
                val bookId = bookIdStr.toIntOrNull() ?: continue
                val chObj = root.getJSONObject(bookIdStr)
                val chMap = mutableMapOf<Int, Int>()
                val chKeys = chObj.keys()
                while (chKeys.hasNext()) {
                    val chStr = chKeys.next()
                    val chNum = chStr.toIntOrNull() ?: continue
                    val vCount = chObj.getInt(chStr)
                    chMap[chNum] = vCount
                }
                result[bookId] = chMap
            }
            verseCountCache = result
        } catch (e: Exception) {
            // fallback
            verseCountCache = emptyMap()
        }
    }

    fun getVerseCount(bookId: Int, chapter: Int): Int {
        val count = verseCountCache?.get(bookId)?.get(chapter)
        if (count != null && count > 0) return count
        // Reasonable fallback based on chapter
        return when (bookId) {
            19 -> if (chapter == 119) 176 else 25 // Psalms
            else -> 30
        }
    }
}
