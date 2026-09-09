package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.PreferencesManager
import com.example.data.model.BlogSourceType
import com.example.data.model.PredefinedPlaylists
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context matches app name`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Vinay Kumar AVJ", appName)
  }

  @Test
  fun `personal vlog is disabled by default`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = PreferencesManager(context)
    assertFalse("Show Personal Vlog must be OFF by default", prefs.settings.value.showPersonalVlog)
    assertTrue("Show Fellowship Events must be ON by default", prefs.settings.value.showFellowshipEvents)
    assertTrue("Show YouTube must be ON by default", prefs.settings.value.showYouTube)
  }

  @Test
  fun `toggling personal vlog updates settings state immediately`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = PreferencesManager(context)
    prefs.updateShowPersonalVlog(true)
    assertTrue("Show Personal Vlog must update to true", prefs.settings.value.showPersonalVlog)
    prefs.updateShowPersonalVlog(false)
    assertFalse("Show Personal Vlog must update to false", prefs.settings.value.showPersonalVlog)
  }

  @Test
  fun `predefined playlists contain all 12 requested items`() {
    assertEquals(12, PredefinedPlaylists.items.size)
    val titles = PredefinedPlaylists.items.map { it.title }
    assertTrue(titles.contains("Vinay's Songs"))
    assertTrue(titles.contains("Kids"))
    assertTrue(titles.contains("हिंदी मसीही गीत"))
    assertTrue(titles.contains("सादरी गीत"))
    assertTrue(titles.contains("English Songs"))
    assertTrue(titles.contains("Other Language Songs"))
    assertTrue(titles.contains("Sermons"))
    assertTrue(titles.contains("प्रचार"))
    assertTrue(titles.contains("गवाही"))
    assertTrue(titles.contains("Shorts"))
    assertTrue(titles.contains("News"))
    assertTrue(titles.contains("अन्य"))
  }

  @Test
  fun `channel details are properly configured`() {
    assertEquals("UClFK75L0wsDMf10Tj77hlsg", PredefinedPlaylists.channelMain.id)
    assertEquals("@vinaykumaravj", PredefinedPlaylists.channelMain.handle)
    assertEquals("UC92tSCn2I6lwcUyAdyS_MMw", PredefinedPlaylists.channelWorship.id)
    assertEquals("@vinaykumaravjworship", PredefinedPlaylists.channelWorship.handle)
  }

  @Test
  fun `decodeAndSanitizeVerseText decodes base64 and cleans html`() {
    // Base64 of "आदि में वचन था"
    val rawBase64 = "4KSH4KS4IOCkquCljeCksOClgeCkluCljeCknOCkvyDgpKrgpLDgpK7gpYfgpLbgpY3gpLXgpLA="
    val sanitized = com.example.data.bible.local.BibleLocalDataSource.decodeAndSanitizeVerseText(rawBase64)
    assertFalse("Must not start with 4KS", sanitized.startsWith("4KS"))
    assertTrue("Must decode to valid text", sanitized.isNotEmpty())

    val htmlRaw = "<p>यह एक <sup>1</sup>परीक्षण <a href=\"#\">वचन</a> है।</p>"
    val sanitizedHtml = com.example.data.bible.local.BibleLocalDataSource.decodeAndSanitizeVerseText(htmlRaw)
    assertEquals("यह एक परीक्षण वचन है।", sanitizedHtml)
  }

  @Test
  fun `offline bible verses are complete and sequentially ordered`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val jsonString = context.assets.open("bible/offline_verses.json").bufferedReader().use { it.readText() }
    val jsonArray = org.json.JSONArray(jsonString)
    assertTrue("Should have verses", jsonArray.length() > 300)

    val chapters = mutableMapOf<Pair<Int, Int>, MutableList<Int>>()
    for (i in 0 until jsonArray.length()) {
      val obj = jsonArray.getJSONObject(i)
      val b = obj.getInt("b")
      val c = obj.getInt("c")
      val v = obj.getInt("v")
      val text = obj.getString("text")

      assertFalse("Verse $b $c:$v contains base64: $text", text.startsWith("4KS"))
      assertFalse("Verse $b $c:$v contains html tags: $text", text.contains("<sup>") || text.contains("</div>"))
      assertTrue("Verse $b $c:$v must not be blank", text.isNotBlank())

      chapters.getOrPut(Pair(b, c)) { mutableListOf() }.add(v)
    }

    for ((key, verses) in chapters) {
      val (b, c) = key
      val sorted = verses.sorted()
      assertEquals("Chapter $b:$c must start at verse 1", 1, sorted.first())
      for (v in 1..sorted.size) {
        assertEquals("Chapter $b:$c missing sequential verse $v", v, sorted[v - 1])
      }
    }
  }

  @Test
  fun `offline bible headings are valid and non corrupt`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val jsonString = context.assets.open("bible/offline_headings.json").bufferedReader().use { it.readText() }
    val jsonArray = org.json.JSONArray(jsonString)
    assertTrue("Should have section headings", jsonArray.length() >= 10)

    for (i in 0 until jsonArray.length()) {
      val obj = jsonArray.getJSONObject(i)
      val h = obj.getString("h")
      val v = obj.getInt("v")
      assertTrue("Heading must not be blank", h.isNotBlank())
      assertTrue("Heading beforeVerse must be >= 1", v >= 1)
      assertFalse("Heading must not contain base64", h.startsWith("4KS"))
      assertFalse("Heading must not contain html", h.contains("<sup>") || h.contains("</div>"))
    }
  }
}

