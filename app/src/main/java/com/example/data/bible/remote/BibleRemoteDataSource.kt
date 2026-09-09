package com.example.data.bible.remote

import android.util.Log
import com.example.data.bible.local.BibleVerseEntity
import com.example.data.bible.model.BibleTranslation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.util.concurrent.TimeUnit

class BibleRemoteDataSource(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()
) {
    /**
     * Fetches public domain / open CC-BY-SA chapter from authorized public Bible API service.
     * Translation mapping:
     * - "HIN_IRV" -> bolls translation "HIN" (Indian Revised Version)
     * - "ENG_WEB" -> bolls translation "WEB" (World English Bible)
     */
    suspend fun fetchChapterVerses(
        translationId: String,
        bookId: Int,
        chapter: Int
    ): List<BibleVerseEntity>? = withContext(Dispatchers.IO) {
        val bollsTranslation = when (translationId) {
            BibleTranslation.HINDI_IRV.id -> "HIOV"
            BibleTranslation.ENGLISH_WEB.id -> "WEB"
            else -> "HIOV"
        }

        val url = "https://bolls.life/get-chapter/$bollsTranslation/$bookId/$chapter/"

        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "VinayKumarAVJ-Fellowship-App/1.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val jsonArray = JSONArray(body)
                val results = mutableListOf<BibleVerseEntity>()

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val verseNum = obj.optInt("verse", i + 1)
                    val rawText = obj.optString("text", "")
                    
                    val finalText = com.example.data.bible.local.BibleLocalDataSource.decodeAndSanitizeVerseText(rawText)

                    if (finalText.isNotEmpty()) {
                        results.add(
                            BibleVerseEntity(
                                translationId = translationId,
                                bookId = bookId,
                                chapter = chapter,
                                verse = verseNum,
                                text = finalText
                            )
                        )
                    }
                }
                if (results.isNotEmpty()) {
                    results.sortBy { it.verse }
                    results
                } else null
            }
        } catch (e: Exception) {
            Log.e("BibleRemoteDataSource", "Online fetch error for $translationId $bookId:$chapter", e)
            null
        }
    }
}
