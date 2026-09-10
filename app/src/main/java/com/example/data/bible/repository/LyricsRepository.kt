package com.example.data.bible.repository

import com.example.data.bible.local.BibleDao
import com.example.data.bible.local.ChristianSongEntity
import com.example.data.model.BlogPost
import com.example.data.model.BlogSourceType
import com.example.data.remote.BloggerFeedService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.regex.Pattern

class LyricsRepository(
    private val bibleDao: BibleDao,
    private val feedService: BloggerFeedService = BloggerFeedService()
) {

    fun getAllSongs(): Flow<List<ChristianSongEntity>> = bibleDao.getAllSongs()

    fun getFavoriteSongs(): Flow<List<ChristianSongEntity>> = bibleDao.getFavoriteSongs()

    fun searchSongs(query: String): Flow<List<ChristianSongEntity>> = bibleDao.searchSongs(query)

    suspend fun getSongById(id: Long): ChristianSongEntity? = withContext(Dispatchers.IO) {
        bibleDao.getSongById(id)
    }

    suspend fun getSongByNumber(number: Int): ChristianSongEntity? = withContext(Dispatchers.IO) {
        bibleDao.getSongByNumber(number)
    }

    suspend fun getNextSongNumber(): Int = withContext(Dispatchers.IO) {
        (bibleDao.getMaxSongNumber() ?: 0) + 1
    }

    suspend fun addSong(song: ChristianSongEntity): Long = withContext(Dispatchers.IO) {
        val number = if (song.songNumber > 0) song.songNumber else ((bibleDao.getMaxSongNumber() ?: 0) + 1)
        bibleDao.insertSong(song.copy(songNumber = number))
    }

    suspend fun updateSong(song: ChristianSongEntity) = withContext(Dispatchers.IO) {
        bibleDao.updateSong(song)
    }

    suspend fun toggleFavorite(song: ChristianSongEntity) = withContext(Dispatchers.IO) {
        bibleDao.updateSong(song.copy(isFavorite = !song.isFavorite, modifiedAt = System.currentTimeMillis()))
    }

    suspend fun deleteSong(id: Long) = withContext(Dispatchers.IO) {
        bibleDao.deleteSongById(id)
    }

    /**
     * Automatically fetches blog posts from Blogger feeds (Fellowship Events & Personal Vlog)
     * and filters out any post tagged or named with lyrics/songs, auto-numbering and saving them into Song Book.
     */
    suspend fun fetchAndSyncLyricsFromBlogs(): Int = withContext(Dispatchers.IO) {
        try {
            val fellowshipPosts = feedService.fetchBlogPosts(BlogSourceType.FELLOWSHIP_EVENTS)
            val personalPosts = feedService.fetchBlogPosts(BlogSourceType.PERSONAL_VLOG)
            val allPosts = (fellowshipPosts + personalPosts).distinctBy { it.id }
            syncLyricsFromBlogPosts(allPosts)
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    /**
     * Inspects a list of BlogPosts, identifies any lyrics or song posts by tags / title,
     * cleans and formats their HTML content, extracts chords / scale / scripture references,
     * and assigns sequential song numbers.
     */
    suspend fun syncLyricsFromBlogPosts(posts: List<BlogPost>): Int = withContext(Dispatchers.IO) {
        val existingSongs = bibleDao.getAllSongs().first()
        val existingTitles = existingSongs.map { it.title.trim().lowercase() }.toSet()
        val existingBlogIds = existingSongs.mapNotNull { if (it.blogPostId.isNotBlank()) it.blogPostId else null }.toSet()

        var currentMaxNumber = bibleDao.getMaxSongNumber() ?: 0
        var newSongsAdded = 0

        val lyricsTags = setOf(
            "lyrics", "song", "songs", "geet", "गीत", "भजन", "worship", "chords",
            "stuti", "aradhana", "गाने", "गाना", "मसीही गीत", "christian song",
            "christian songs", "sadri song", "sadri geet", "bhajan", "psalm", "hymn", "hymns",
            "स्तुति", "आराधना", "साद्री गीत", "भजन संहिता"
        )

        for (post in posts) {
            val hasMatchingLabel = post.labels.any { label ->
                val l = label.trim().lowercase()
                lyricsTags.any { tag -> l.contains(tag) }
            }
            val titleLower = post.title.trim().lowercase()
            val hasMatchingTitle = lyricsTags.any { tag -> titleLower.contains(tag) } ||
                    titleLower.startsWith("गीत") ||
                    titleLower.contains("lyrics") ||
                    titleLower.contains("chords")

            if (!hasMatchingLabel && !hasMatchingTitle) {
                continue
            }

            // Check if already in database
            if (existingBlogIds.contains(post.id) || existingTitles.contains(titleLower)) {
                continue
            }

            // Clean lyrics content
            val cleanLyrics = cleanHtmlToLyrics(post.contentHtml, post.plainTextExcerpt)
            if (cleanLyrics.length < 15) {
                continue // Skip empty or invalid content
            }

            // Extract Scale / Key
            val scalePattern = Pattern.compile("(?i)(?:Scale|Key|स्केल|स्वर)\\s*[:=-]\\s*([A-G][#b]?(?:m|maj|min)?)")
            val scaleMatcher = scalePattern.matcher(post.contentHtml + " " + post.title)
            val extractedScale = if (scaleMatcher.find()) scaleMatcher.group(1)?.trim() ?: "D" else "D"

            // Extract Scripture / Bible Reference
            val refPattern = Pattern.compile("(?i)(?:Ref|Reference|वचन|संदर्भ|Bible)\\s*[:=-]\\s*([^\n<,]+)")
            val refMatcher = refPattern.matcher(post.contentHtml + " " + post.title)
            val extractedRef = if (refMatcher.find()) refMatcher.group(1)?.trim() ?: "" else ""

            // Determine Category
            val category = when {
                post.labels.any { it.contains("sadri", ignoreCase = true) || it.contains("साद्री", ignoreCase = true) } -> "साद्री मसीही गीत"
                post.labels.any { it.contains("prayer", ignoreCase = true) || it.contains("प्रार्थना", ignoreCase = true) } -> "प्रार्थना व विनती"
                post.labels.any { it.contains("psalm", ignoreCase = true) || it.contains("भजन", ignoreCase = true) } -> "भजन संहिता"
                post.labels.any { it.contains("english", ignoreCase = true) } -> "अंग्रेज़ी व हिंदी"
                post.labels.any { it.contains("christmas", ignoreCase = true) || it.contains("क्रिसमस", ignoreCase = true) } -> "क्रिसमस / सुसमाचार"
                else -> "स्तुति व आराधना"
            }

            currentMaxNumber += 1
            val songEntity = ChristianSongEntity(
                songNumber = currentMaxNumber,
                title = post.title.trim(),
                content = cleanLyrics,
                artist = "Vinay Kumar AVJ",
                category = category,
                keyScale = extractedScale,
                colorHex = "#FFFBEB",
                linkedReferences = extractedRef,
                personalNotes = "Synced from Blog: ${post.publishedDate}",
                blogPostId = post.id,
                isFavorite = false,
                isUserCreated = false,
                createdAt = post.publishedTimestamp.takeIf { it > 0 } ?: System.currentTimeMillis()
            )

            bibleDao.insertSong(songEntity)
            newSongsAdded++
        }

        newSongsAdded
    }

    private fun cleanHtmlToLyrics(html: String, excerpt: String): String {
        if (html.isBlank()) return excerpt.trim()
        val text = html
            .replace("(?i)<br\\s*/?>".toRegex(), "\n")
            .replace("(?i)</p>".toRegex(), "\n\n")
            .replace("(?i)<p[^>]*>".toRegex(), "")
            .replace("(?i)</div>".toRegex(), "\n")
            .replace("(?i)<div[^>]*>".toRegex(), "")
            .replace("(?i)</li>".toRegex(), "\n")
            .replace("(?i)<li[^>]*>".toRegex(), "• ")
            .replace("(?i)</h1>|</h2>|</h3>|</h4>".toRegex(), "\n\n")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("<[^>]*>".toRegex(), "")
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .replace("\n{3,}".toRegex(), "\n\n")
            .trim()

        return if (text.isNotBlank()) text else excerpt.trim()
    }

    suspend fun initializePreloadedLyrics() = withContext(Dispatchers.IO) {
        val count = bibleDao.getSongCount()
        if (count == 0) {
            bibleDao.insertSongs(preloadedSongs)
        }
    }

    companion object {
        val preloadedSongs = listOf(
            ChristianSongEntity(
                songNumber = 1,
                title = "गाओ हाल्लेलूयाह (Gao Hallelujah)",
                artist = "Vinay Kumar AVJ Worship",
                category = "स्तुति व आराधना",
                keyScale = "D",
                content = """[Intro]
[D] [G] [A] [D]

[Verse 1]
[D]                    [G]
गाओ हाल्लेलूयाह, प्रभु यीशु के नाम को,
[A]                   [D]
उसने दिया है हमको नया जीवन आज।
[D]                    [G]
पापों से हमको उसने छुड़ाया,
[A]                   [D]
अपनी क्रूस पर लहू बहाया।

[Chorus]
[D]         [G]
हाल्लेलूयाह, हाल्लेलूयाह,
[A]         [D]
हाल्लेलूयाह, आमीन!
[D]         [G]
महिमा हो तेरी, स्तुति हो तेरी,
[A]         [D]
युगानुयुग तू ही हमारा प्रभु!

[Verse 2]
[D]                    [G]
तू ही हमारा सच्चा चरवाहा,
[A]                   [D]
सच्ची राह पर हमको चलाता।
[D]                    [G]
अंधकार में तू ज्योति हमारी,
[A]                   [D]
तेरे नाम में है जय जयकार हमारी।

[Bridge]
[Bm]           [G]
कोई नाम नहीं ऐसा जहाँ में,
[A]            [D]
जो उद्धार दे सके इंसान को।
[Bm]           [G]
यीशु ही है मार्ग, सत्य, जीवन,
[A]            [D]
उसके चरणों में झुकता हर मन!

[Outro]
[D]         [G]       [A]    [D]
हाल्लेलूयाह... हाल्लेलूयाह... आमीन!"""
            ),
            ChristianSongEntity(
                songNumber = 2,
                title = "तेरी स्तुति और प्रशंसा करूँ (Teri Stuti Aur Prashansa Karun)",
                artist = "Vinay Kumar AVJ Worship",
                category = "स्तुति व आराधना",
                keyScale = "C",
                content = """[Intro]
[C] [F] [G] [C]

[Verse 1]
[C]                [F]
तेरी स्तुति और प्रशंसा करूँ,
[G]                [C]
दिल से मैं तेरा धन्यवाद करूँ।
[C]                [F]
जब तक है सांसें इस काया में,
[G]                [C]
गाता रहूँगा तेरी ही महिमा।

[Chorus]
[C]           [F]
येशु... तू ही मेरा राजा,
[G]           [C]
येशु... तू ही मेरा प्रभु।
[C]           [F]
तेरे बिना जीवन है वीराना,
[G]           [C]
तू ही है जीवन का सच्चा झरना।

[Verse 2]
[C]                [F]
दुख और संकट जब मुझको घेरें,
[G]                [C]
तेरा वचन मुझको संबल दे।
[C]                [F]
तूने थामा है मेरा ये हाथ,
[G]                [C]
छोड़ेगा ना तू कभी मेरा साथ।"""
            ),
            ChristianSongEntity(
                songNumber = 3,
                title = "महिमा, आदर और जलाल (Mahima Aadar Aur Jalal)",
                artist = "Hindi Worship",
                category = "स्तुति व आराधना",
                keyScale = "G",
                content = """[Intro]
[G] [C] [D] [G]

[Verse 1]
[G]                  [C]
महिमा, आदर और जलाल,
[D]                  [G]
हम देते हैं तुझको प्रभु।
[G]                  [C]
हाथ उठाकर गाते हैं,
[D]                  [G]
तू ही है महान खुदा।

[Chorus]
[G]                   [Em]
तू महान है, करता है बड़े काम,
[C]                   [D]
तेरे जैसा कोई नहीं, तेरे जैसा कोई नहीं।
[G]                   [Em]
तू महान है, करता है बड़े काम,
[C]         [D]       [G]
तेरे जैसा कोई नहीं प्रभु!

[Verse 2]
[G]                  [C]
स्वर्ग और पृथ्वी गाते हैं,
[D]                  [G]
तेरी महिमा का गुणगान।
[G]                  [C]
राजाओं का तू राजा है,
[D]                  [G]
प्रभुओं का तू प्रभु महान।"""
            ),
            ChristianSongEntity(
                songNumber = 4,
                title = "यीशु नाम सबसे मीठा है (Yeshu Naam Sabse Meetha Hai)",
                artist = "Vinay Kumar AVJ Worship",
                category = "स्तुति व आराधना",
                keyScale = "D",
                content = """[Verse 1]
[D]                    [G]
यीशु नाम सबसे मीठा है,
[A]                    [D]
इस नाम में चंगाई है।
[D]                    [G]
यीशु नाम सबसे ऊंचा है,
[A]                    [D]
इस नाम में रिहाई है।

[Chorus]
[D]         [G]
जय यीशु, जय यीशु,
[A]         [D]
जय यीशु बोल मनवा।
[D]         [G]
तेरे सारे पाप मिटेंगे,
[A]         [D]
तू पाएगा नया जीवनवा।

[Verse 2]
[D]                    [G]
टूटे मनों को वो जोड़ता है,
[A]                    [D]
प्यासों को जीवन-जल देता है।
[D]                    [G]
जो कोई उस पर विश्वास करे,
[A]                    [D]
उसको सदा का जीवन दे।"""
            ),
            ChristianSongEntity(
                songNumber = 5,
                title = "तू ही मेरी शरण है (Tu Hi Meri Sharan Hai)",
                artist = "Vinay Kumar AVJ",
                category = "प्रार्थना व विनती",
                keyScale = "E",
                content = """[Intro]
[E] [A] [B] [E]

[Verse 1]
[E]                   [A]
तू ही मेरी शरण है प्रभु,
[B]                   [E]
तू ही मेरा बल है।
[E]                   [A]
मुसीबतों के समय में,
[B]                   [E]
तू ही मेरा सहारा है।

[Chorus]
[E]           [A]
मैं ना डरूँगा कभी,
[B]           [E]
चाहे पहाड़ टल जाएँ।
[E]           [A]
मेरा भरोसा तुझ पर है,
[B]           [E]
तू कभी ना छोड़ेगा मुझे।

[Verse 2]
[E]                   [A]
तेरा वचन मेरे पैरों के लिए,
[B]                   [E]
दीपक और ज्योति समान है।
[E]                   [A]
तेरे पंखों की छाँव में,
[B]                   [E]
मुझे मिलती सुरक्षा सदा।"""
            ),
            ChristianSongEntity(
                songNumber = 6,
                title = "आराधना करूँ (Aaradhna Karun)",
                artist = "Hindi Worship",
                category = "समर्पण",
                keyScale = "D",
                content = """[Intro]
[D] [G] [A] [D]

[Verse 1]
[D]                   [G]
आराधना करूँ, पूरे मन से,
[A]                   [D]
आराधना करूँ, पूरे दिल से।
[D]                   [G]
तू ही है मेरा परमेश्वर,
[A]                   [D]
तू ही है मेरा मुक्तिदाता।

[Chorus]
[D]         [G]
पवित्र, पवित्र, पवित्र प्रभु,
[A]         [D]
सेनाओं का यहोवा तू।
[D]         [G]
सारी सृष्टि झुकती है,
[A]         [D]
तेरे पवित्र चरणों में।"""
            ),
            ChristianSongEntity(
                songNumber = 7,
                title = "यहोवा मेरा चरवाहा है (Yahowa Mera Charwaha Hai)",
                artist = "Psalm 23 Worship",
                category = "भजन संहिता",
                keyScale = "G",
                content = """[Intro]
[G] [C] [D] [G]

[Verse 1]
[G]                   [C]
यहोवा मेरा चरवाहा है,
[D]                   [G]
मुझे कुछ घटी न होगी।
[G]                   [C]
हरी-हरी चराइयों में,
[D]                   [G]
मुझे ले चराता है।

[Chorus]
[G]           [C]
सुखदाई जल के पास,
[D]           [G]
मुझे ले जाता है।
[G]           [C]
वो मेरे जी में जी,
[D]           [G]
नया डाल देता है।

[Verse 2]
[G]                   [C]
चाहे घोर अंधकार की तराई से,
[D]                   [G]
होकर मुझे चलना पड़े।
[G]                   [C]
मैं किसी बुराई से ना डरूँगा,
[D]                   [G]
क्योंकि तू मेरे साथ है।"""
            ),
            ChristianSongEntity(
                songNumber = 8,
                title = "तोहरे नाम में शांति मिले (Tohre Naam Me Shanti Mile)",
                artist = "Vinay Kumar AVJ Worship",
                category = "साद्री मसीही गीत",
                keyScale = "E",
                content = """[Intro]
[E] [A] [B] [E]

[Verse 1]
[E]                   [A]
तोहरे नाम में प्रभु शांति मिले,
[B]                   [E]
तोहरे वचन में जीवन खिले।
[E]                   [A]
संसार के दुख-दर्द दूर भगावे,
[B]                   [E]
प्रभु येशु हमके अपन बनावे।

[Chorus]
[E]            [A]
जय जय येशु, जय जय येशु,
[B]            [E]
गावत आही हम सब मिलके।
[E]            [A]
तोहर क्रूस के भारी बलिदान,
[B]            [E]
देला हमके मुक्ति दान।

[Verse 2]
[E]                   [A]
गांव-नगर में तोहरे चरचा,
[B]                   [E]
सबकर मन में तोहरे आशा।
[E]                   [A]
हाथ जोड़ के विनती करीला,
[B]                   [E]
हमके अपन आशीष देवेला।"""
            ),
            ChristianSongEntity(
                songNumber = 9,
                title = "आत्मा में भर दे मुझे (Aatma Me Bhar De Mujhe)",
                artist = "Vinay Kumar AVJ Worship",
                category = "प्रार्थना व विनती",
                keyScale = "A",
                content = """[Intro]
[A] [D] [E] [A]

[Verse 1]
[A]                   [D]
पवित्र आत्मा, आ मेरे दिल में,
[E]                   [A]
अपनी सामर्थ्य से भर दे मुझे।
[A]                   [D]
प्यासी है रूह, सूखा है मन,
[E]                   [A]
जीवन की धारा बहा दे मुझमें।

[Chorus]
[A]           [D]
भर दे मुझे, भर दे मुझे,
[E]           [A]
पवित्र आत्मा के अभिषेक से।
[A]           [D]
नया बना, स्वच्छ कर मुझे,
[E]           [A]
तेरे जैसा बना दे मुझे।"""
            ),
            ChristianSongEntity(
                songNumber = 10,
                title = "Amazing Grace (अद्भुत अनुग्रह)",
                artist = "John Newton / Traditional",
                category = "अंग्रेज़ी व हिंदी",
                keyScale = "G",
                content = """[Intro]
[G] [C] [G] [D] [G]

[Verse 1]
[G]             [C]       [G]
Amazing grace! How sweet the sound
[G]                  [D]
That saved a wretch like me!
[G]              [C]        [G]
I once was lost, but now am found;
[G]         [D]     [G]
Was blind, but now I see.

[Chorus]
[G]            [C]     [G]
'Twas grace that taught my heart to fear,
[G]                  [D]
And grace my fears relieved;
[G]              [C]       [G]
How precious did that grace appear
[G]         [D]      [G]
The hour I first believed.

[Verse 2]
[G]               [C]       [G]
Through many dangers, toils, and snares,
[G]               [D]
I have already come;
[G]               [C]          [G]
'Tis grace hath brought me safe thus far,
[G]           [D]      [G]
And grace will lead me home."""
            )
        )
    }
}
