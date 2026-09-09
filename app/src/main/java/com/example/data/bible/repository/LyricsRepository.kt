package com.example.data.bible.repository

import com.example.data.bible.local.BibleDao
import com.example.data.bible.local.ChristianSongEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class LyricsRepository(private val bibleDao: BibleDao) {

    fun getAllSongs(): Flow<List<ChristianSongEntity>> = bibleDao.getAllSongs()

    fun getFavoriteSongs(): Flow<List<ChristianSongEntity>> = bibleDao.getFavoriteSongs()

    fun searchSongs(query: String): Flow<List<ChristianSongEntity>> = bibleDao.searchSongs(query)

    suspend fun getSongById(id: Long): ChristianSongEntity? = withContext(Dispatchers.IO) {
        bibleDao.getSongById(id)
    }

    suspend fun addSong(song: ChristianSongEntity): Long = withContext(Dispatchers.IO) {
        bibleDao.insertSong(song)
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

    suspend fun initializePreloadedLyrics() = withContext(Dispatchers.IO) {
        val count = bibleDao.getSongCount()
        if (count == 0) {
            bibleDao.insertSongs(preloadedSongs)
        }
    }

    companion object {
        val preloadedSongs = listOf(
            ChristianSongEntity(
                title = "गाओ हाल्लेलूयाह (Gao Hallelujah)",
                artist = "Vinay Kumar AVJ Worship",
                category = "Hindi Worship",
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
                title = "तेरी स्तुति और प्रशंसा करूँ (Teri Stuti Aur Prashansa Karun)",
                artist = "Vinay Kumar AVJ Worship",
                category = "Hindi Praise",
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
                title = "तोहरे नाम में शांति मिले (Tohre Naam Me Shanti Mile)",
                artist = "Vinay Kumar AVJ Worship",
                category = "Sadri Christian",
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
                title = "Amazing Grace (अद्भुत अनुग्रह)",
                artist = "John Newton / Traditional",
                category = "English Worship",
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
            ),
            ChristianSongEntity(
                title = "यीशु नाम सबसे मीठा है (Yeshu Naam Sabse Meetha Hai)",
                artist = "Vinay Kumar AVJ Worship",
                category = "Hindi Praise",
                content = """[Verse 1]
यीशु नाम सबसे मीठा है,
इस नाम में चंगाई है।
यीशु नाम सबसे ऊंचा है,
इस नाम में रिहाई है।

[Chorus]
जय यीशु, जय यीशु,
जय यीशु बोल मनवा।
तेरे सारे पाप मिटेंगे,
तू पाएगा नया जीवनवा।

[Verse 2]
टूटे मनों को वो जोड़ता है,
प्यासों को जीवन-जल देता है।
जो कोई उस पर विश्वास करे,
उसको सदा का जीवन दे।"""
            )
        )
    }
}
