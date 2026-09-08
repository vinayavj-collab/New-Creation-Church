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
}

