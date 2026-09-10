package com.example.data.remote

import android.os.Build
import com.example.data.model.BlogPost
import com.example.data.model.BlogSourceType
import com.example.util.BloggerImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class BloggerFeedService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val imgPattern = Pattern.compile("<img[^>]+src\\s*=\\s*['\"]([^'\"]+)['\"]", Pattern.CASE_INSENSITIVE)
    private val youtubePattern = Pattern.compile("(?:youtube\\.com/(?:embed/|watch\\?v=)|youtu\\.be/)([a-zA-Z0-9_-]{11})", Pattern.CASE_INSENSITIVE)
    private val htmlTagPattern = Pattern.compile("<[^>]*>")

    suspend fun fetchBlogPosts(source: BlogSourceType, maxResults: Int = 50): List<BlogPost> = withContext(Dispatchers.IO) {
        val url = "${source.homeUrl}feeds/posts/default?alt=json&max-results=$maxResults"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Android) VinayKumarAVJ/1.0")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext emptyList()
                }
                val body = response.body?.string() ?: return@withContext emptyList()
                parseBloggerJson(body, source)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun parseBloggerJson(jsonString: String, source: BlogSourceType): List<BlogPost> {
        val posts = mutableListOf<BlogPost>()
        try {
            val root = JSONObject(jsonString)
            val feed = root.optJSONObject("feed") ?: return emptyList()
            val entries = feed.optJSONArray("entry") ?: return emptyList()

            for (i in 0 until entries.length()) {
                val entry = entries.getJSONObject(i)
                val id = entry.optJSONObject("id")?.optString("\$t") ?: "post-$i"
                val titleObj = entry.optJSONObject("title")
                val title = titleObj?.optString("\$t")?.trim().orEmpty().ifEmpty { "Untitled Post" }

                val publishedRaw = entry.optJSONObject("published")?.optString("\$t") ?: ""
                val (formattedDate, timestamp) = parseDate(publishedRaw)

                // Labels
                val labels = mutableListOf<String>()
                val catArray = entry.optJSONArray("category")
                if (catArray != null) {
                    for (c in 0 until catArray.length()) {
                        val cat = catArray.getJSONObject(c)
                        val term = cat.optString("term").trim()
                        if (term.isNotEmpty() && !labels.contains(term)) {
                            labels.add(term)
                        }
                    }
                }

                // Content
                val contentObj = entry.optJSONObject("content")
                val contentHtml = contentObj?.optString("\$t") ?: entry.optJSONObject("summary")?.optString("\$t") ?: ""

                // Extract all images
                val allImages = mutableListOf<String>()
                val imgMatcher = imgPattern.matcher(contentHtml)
                while (imgMatcher.find()) {
                    val rawSrc = imgMatcher.group(1) ?: continue
                    val cleanUrl = BloggerImageUtils.sanitizeUrl(rawSrc)
                    if (cleanUrl.isNotEmpty() && !allImages.contains(cleanUrl)) {
                        allImages.add(cleanUrl)
                    }
                }

                // Featured Image
                val thumbUrl = entry.optJSONObject("media\$thumbnail")?.optString("url")
                val featuredImageUrl = when {
                    allImages.isNotEmpty() -> allImages.first()
                    !thumbUrl.isNullOrEmpty() -> BloggerImageUtils.sanitizeUrl(thumbUrl)
                    else -> null
                }

                // Extract YouTube videos embedded in the post
                val videoIds = mutableListOf<String>()
                val ytMatcher = youtubePattern.matcher(contentHtml)
                while (ytMatcher.find()) {
                    val vId = ytMatcher.group(1) ?: continue
                    if (!videoIds.contains(vId)) {
                        videoIds.add(vId)
                    }
                }

                // Original link
                var postUrl = source.homeUrl
                val links = entry.optJSONArray("link")
                if (links != null) {
                    for (l in 0 until links.length()) {
                        val link = links.getJSONObject(l)
                        if (link.optString("rel") == "alternate") {
                            postUrl = link.optString("href")
                            break
                        }
                    }
                }

                // Excerpt
                val plainText = htmlTagPattern.matcher(contentHtml).replaceAll(" ")
                    .replace("&nbsp;", " ")
                    .replace("&amp;", "&")
                    .replace("&quot;", "\"")
                    .replace("&#39;", "'")
                    .replace("&lt;", "<")
                    .replace("&gt;", ">")
                    .replace("\\s+".toRegex(), " ")
                    .trim()
                val excerpt = if (plainText.length > 180) plainText.substring(0, 180).trim() + "..." else plainText

                posts.add(
                    BlogPost(
                        id = id,
                        source = source,
                        title = title,
                        publishedDate = formattedDate,
                        publishedTimestamp = timestamp,
                        labels = labels,
                        featuredImageUrl = featuredImageUrl,
                        allImages = allImages,
                        plainTextExcerpt = excerpt,
                        contentHtml = contentHtml,
                        url = postUrl,
                        embeddedVideoIds = videoIds
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return posts
    }

    private fun getHighResUrl(url: String): String {
        return url.replace("/s72-c/", "/s1600/")
            .replace("/s72-w/", "/s1600/")
            .replace("/w72-h72-p-k-no-nu/", "/s1600/")
            .replace(Regex("/s[0-9]+(-c|-w|-h[0-9]+)?/"), "/s1600/")
    }

    private fun parseDate(isoString: String): Pair<String, Long> {
        if (isoString.isEmpty()) return Pair("Recent", System.currentTimeMillis())
        try {
            // e.g. 2026-09-07T08:45:29.905-07:00
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            val datePart = if (isoString.length >= 19) isoString.substring(0, 19) else isoString
            val date = sdf.parse(datePart)
            if (date != null) {
                val displayFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                return Pair(displayFormat.format(date), date.time)
            }
        } catch (e: Exception) {
            // fallback
        }
        return Pair("Recent", System.currentTimeMillis())
    }
}
