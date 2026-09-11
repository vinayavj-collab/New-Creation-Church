package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.FavoriteCategoriesDialog
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onAboutClick: () -> Unit,
    onCustomizeHomeClick: () -> Unit,
    onSyncCenterClick: () -> Unit = {},
    onBackupRestoreClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = appStrings()
    val settings by viewModel.settings.collectAsState()
    val fellowshipCategories by viewModel.fellowshipCategories.collectAsState()

    var showFavCategoriesDialog by remember { mutableStateOf(false) }

    val shareApp = {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(
                Intent.EXTRA_TEXT,
                "Vinay Kumar AVJ - Fellowship Events • Videos • Photos • Holy Bible • Christian Songs\nDownload the official app to stay connected with fellowship events and worship!"
            )
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, strings.shareApp))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(strings.settingsTitle, fontWeight = FontWeight.Bold)
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            // 0. App Update System
            item {
                AppUpdateSection(viewModel = viewModel)
            }

            // 1. Language & Localization
            item {
                SettingsSectionHeader(title = strings.secLanguage, icon = Icons.Default.Language)
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Translate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.appInterfaceLanguage,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = strings.appLanguageDesc,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick Switch Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = settings.appLanguage == AppLanguage.ENGLISH,
                                onClick = {
                                    if (settings.appLanguage != AppLanguage.ENGLISH) {
                                        viewModel.updateAppLanguage(AppLanguage.ENGLISH)
                                        Toast.makeText(context, "Language switched to English", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                label = { Text("English") },
                                leadingIcon = if (settings.appLanguage == AppLanguage.ENGLISH) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = settings.appLanguage == AppLanguage.HINDI,
                                onClick = {
                                    if (settings.appLanguage != AppLanguage.HINDI) {
                                        viewModel.updateAppLanguage(AppLanguage.HINDI)
                                        Toast.makeText(context, "भाषा हिंदी में बदली गई", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                label = { Text("हिंदी (Hindi)") },
                                leadingIcon = if (settings.appLanguage == AppLanguage.HINDI) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        FilterChip(
                            selected = settings.appLanguage == AppLanguage.SYSTEM,
                            onClick = {
                                if (settings.appLanguage != AppLanguage.SYSTEM) {
                                    viewModel.updateAppLanguage(AppLanguage.SYSTEM)
                                    Toast.makeText(context, strings.langSystem, Toast.LENGTH_SHORT).show()
                                }
                            },
                            label = { Text(strings.langSystem) },
                            leadingIcon = if (settings.appLanguage == AppLanguage.SYSTEM) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // 2. Personal Vlog Display Mode (Requirement 13)
            item {
                SettingsSectionHeader(title = "PERSONAL VLOG DISPLAY MODE", icon = Icons.Default.Person)
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Choose how Personal Vlogs are displayed in the app:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        PersonalVlogMode.entries.forEach { mode ->
                            val isSelected = settings.personalVlogMode == mode
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.updatePersonalVlogMode(mode) }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { viewModel.updatePersonalVlogMode(mode) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = when (mode) {
                                            PersonalVlogMode.HIDDEN -> "Hidden (Default - Fellowship Events only)"
                                            PersonalVlogMode.SECONDARY -> "Secondary Section (Blogs sub-tab)"
                                            PersonalVlogMode.HOME_AND_SECONDARY -> "Home + Secondary Section"
                                            PersonalVlogMode.PRIORITY_OVERRIDE -> "Priority / Override"
                                        },
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. YouTube Default Tab (Requirement 17)
            item {
                SettingsSectionHeader(title = "डिफ़ॉल्ट यूट्यूब चैनल / टैब (DEFAULT YOUTUBE TAB)", icon = Icons.Default.PlayCircle)
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "ऐप में YouTube स्क्रीन खोलते समय कौन सा चैनल पहले दिखेगा:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = settings.youtubeDefaultTab == YouTubeDefaultTab.ALL,
                                onClick = { viewModel.updateYouTubeDefaultTab(YouTubeDefaultTab.ALL) },
                                label = { Text("All Channels") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = settings.youtubeDefaultTab == YouTubeDefaultTab.AVJ_WORSHIP,
                                onClick = { viewModel.updateYouTubeDefaultTab(YouTubeDefaultTab.AVJ_WORSHIP) },
                                label = { Text("Worship") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = settings.youtubeDefaultTab == YouTubeDefaultTab.VINAY_KUMAR_AVJ,
                                onClick = { viewModel.updateYouTubeDefaultTab(YouTubeDefaultTab.VINAY_KUMAR_AVJ) },
                                label = { Text("Vinay Kumar AVJ") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = settings.youtubeDefaultTab == YouTubeDefaultTab.NEW_CREATION_CHURCH,
                                onClick = { viewModel.updateYouTubeDefaultTab(YouTubeDefaultTab.NEW_CREATION_CHURCH) },
                                label = { Text("New Creation Church") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 4. Bible Reading Style (Requirement 21)
            item {
                SettingsSectionHeader(title = "BIBLE READING STYLE", icon = Icons.Default.MenuBook)
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = settings.bibleReadingStyle == BibleReadingStyle.PRINTED_BIBLE,
                                onClick = { viewModel.updateBibleReadingStyle(BibleReadingStyle.PRINTED_BIBLE) },
                                label = { Text("Printed Bible (Default)") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = settings.bibleReadingStyle == BibleReadingStyle.PARAGRAPH,
                                onClick = { viewModel.updateBibleReadingStyle(BibleReadingStyle.PARAGRAPH) },
                                label = { Text("Paragraph Style") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 5. Main Navigation Bar Customization (User Request 4)
            item {
                SettingsSectionHeader(title = "मुख्य नेविगेशन बार (BOTTOM NAVIGATION)", icon = Icons.Default.Navigation)
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "4th Tab चयन (Choose 4th Navigation Tab)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "नीचे मुख्य नेविगेशन बार में 4थी जगह क्या दिखाना चाहते हैं:",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = settings.customFourthTab == CustomFourthTab.PHOTOS,
                                onClick = { viewModel.updateCustomFourthTab(CustomFourthTab.PHOTOS) },
                                label = { Text("📸 Photos") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = settings.customFourthTab == CustomFourthTab.BIBLE,
                                onClick = { viewModel.updateCustomFourthTab(CustomFourthTab.BIBLE) },
                                label = { Text("📖 Bible") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = settings.customFourthTab == CustomFourthTab.READING_PLAN,
                                onClick = { viewModel.updateCustomFourthTab(CustomFourthTab.READING_PLAN) },
                                label = { Text("📅 Reading Plan") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = settings.customFourthTab == CustomFourthTab.SONG_BOOK,
                                onClick = { viewModel.updateCustomFourthTab(CustomFourthTab.SONG_BOOK) },
                                label = { Text("🎵 Songs") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = settings.customFourthTab == CustomFourthTab.NOTES,
                                onClick = { viewModel.updateCustomFourthTab(CustomFourthTab.NOTES) },
                                label = { Text("📝 Study Notes") },
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // 6. Blogger Photo Grid Layout Customization (User Request 3)
            item {
                SettingsSectionHeader(title = "ब्लॉगर फ़ोटो लेआउट (BLOG PHOTO GRID)", icon = Icons.Default.GridView)
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "आर्टिकल में फ़ोटो प्रदर्शन (Photos in Line)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "ब्लॉग आर्टिकल में फ़ोटो को एक लाइन में ग्रिड (1, 2, 3, 4) के अनुसार दिखाएं ताकि कम स्पेस में ज्यादा फ़ोटो दिख सकें:",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = settings.bloggerPhotoLayout == BloggerPhotoLayout.SINGLE,
                                onClick = { viewModel.updateBloggerPhotoLayout(BloggerPhotoLayout.SINGLE) },
                                label = { Text("1 (Single)") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = settings.bloggerPhotoLayout == BloggerPhotoLayout.GRID_2,
                                onClick = { viewModel.updateBloggerPhotoLayout(BloggerPhotoLayout.GRID_2) },
                                label = { Text("2 (Grid 2)") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = settings.bloggerPhotoLayout == BloggerPhotoLayout.GRID_3,
                                onClick = { viewModel.updateBloggerPhotoLayout(BloggerPhotoLayout.GRID_3) },
                                label = { Text("3 (Grid 3)") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = settings.bloggerPhotoLayout == BloggerPhotoLayout.GRID_4,
                                onClick = { viewModel.updateBloggerPhotoLayout(BloggerPhotoLayout.GRID_4) },
                                label = { Text("4 (Grid 4)") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 7. Data Saver Mode (Requirement 28)
            item {
                SettingsSectionHeader(title = "DATA USAGE & MEDIA", icon = Icons.Default.DataUsage)
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        SettingsSwitchRow(
                            title = "Data Saver Mode",
                            subtitle = "Reduce mobile data by loading compressed thumbnails and on-demand full resolution images.",
                            checked = settings.dataSaverEnabled,
                            onCheckedChange = { viewModel.updateDataSaver(it) }
                        )
                    }
                }
            }

            // 6. Appearance & Theme
            item {
                SettingsSectionHeader(title = strings.secAppearance, icon = Icons.Default.Brightness4)
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = strings.appTheme,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = settings.themeMode == ThemeMode.SYSTEM,
                                onClick = { viewModel.updateThemeMode(ThemeMode.SYSTEM) },
                                label = { Text(strings.themeSystem) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = settings.themeMode == ThemeMode.LIGHT,
                                onClick = { viewModel.updateThemeMode(ThemeMode.LIGHT) },
                                label = { Text(strings.themeLight) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = settings.themeMode == ThemeMode.DARK,
                                onClick = { viewModel.updateThemeMode(ThemeMode.DARK) },
                                label = { Text(strings.themeDark) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 7. Home Screen Customization & Categories
            item {
                SettingsSectionHeader(title = strings.secFeedCustomization, icon = Icons.Default.Tune)
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        SettingsClickableRow(
                            title = strings.customizeHomeSections,
                            subtitle = strings.customizeHomeSectionsSub,
                            icon = Icons.Default.ViewAgenda,
                            onClick = onCustomizeHomeClick
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp))

                        SettingsClickableRow(
                            title = "${strings.favoriteCategories} (${settings.favoriteCategories.size})",
                            subtitle = strings.favoriteCategoriesSub,
                            icon = Icons.Default.Favorite,
                            onClick = { showFavCategoriesDialog = true }
                        )
                    }
                }
            }

            // 8. Notifications
            item {
                SettingsSectionHeader(title = strings.secNotifications, icon = Icons.Default.Notifications)
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        SettingsSwitchRow(
                            title = strings.eventReminders,
                            subtitle = strings.eventRemindersSub,
                            checked = settings.notifyUpcomingReminders,
                            onCheckedChange = { viewModel.updateNotifyUpcomingReminders(it) }
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp))

                        SettingsSwitchRow(
                            title = strings.fellowshipAnnounce,
                            subtitle = strings.fellowshipAnnounceSub,
                            checked = settings.notifyFellowshipEvents,
                            onCheckedChange = { viewModel.updateNotifyFellowshipEvents(it) }
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp))

                        SettingsSwitchRow(
                            title = strings.newYouTubeVideos,
                            subtitle = strings.newYouTubeVideosSub,
                            checked = settings.notifyYouTube,
                            onCheckedChange = { viewModel.updateNotifyYouTube(it) }
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp))

                        SettingsSwitchRow(
                            title = strings.personalVlogNotify,
                            subtitle = strings.personalVlogNotifySub,
                            checked = settings.notifyPersonalVlog,
                            onCheckedChange = { viewModel.updateNotifyPersonalVlog(it) }
                        )
                    }
                }
            }

            // 9. Sync & Backup Management
            item {
                SettingsSectionHeader(title = "SYNC & BACKUP TOOLS", icon = Icons.Default.CloudSync)
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        SettingsClickableRow(
                            title = "Sync Center",
                            subtitle = "Check sync timestamps and refresh all feeds",
                            icon = Icons.Default.Sync,
                            onClick = onSyncCenterClick
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp))

                        SettingsClickableRow(
                            title = "Backup & Restore",
                            subtitle = "Export or restore study notes, bookmarks & lyrics",
                            icon = Icons.Default.CloudUpload,
                            onClick = onBackupRestoreClick
                        )
                    }
                }
            }

            // 10. Other & Storage
            item {
                SettingsSectionHeader(title = strings.secOtherStorage, icon = Icons.Default.Info)
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        SettingsClickableRow(
                            title = strings.shareApp,
                            subtitle = strings.shareAppSub,
                            icon = Icons.Default.Share,
                            onClick = shareApp
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp))

                        SettingsClickableRow(
                            title = strings.aboutVinay,
                            subtitle = strings.aboutVinaySub,
                            icon = Icons.Default.Info,
                            onClick = onAboutClick
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp))

                        SettingsClickableRow(
                            title = strings.clearCache,
                            subtitle = strings.clearCacheSub,
                            icon = Icons.Default.Delete,
                            onClick = {
                                viewModel.clearCache()
                                Toast.makeText(context, strings.cacheClearedToast, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }

    if (showFavCategoriesDialog) {
        FavoriteCategoriesDialog(
            availableCategories = fellowshipCategories,
            favoriteCategories = settings.favoriteCategories,
            onToggleCategory = { viewModel.toggleFavoriteCategory(it) },
            onDismiss = { showFavCategoriesDialog = false }
        )
    }
}

@Composable
fun SettingsSectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 0.5.sp
            )
        )
    }
}

@Composable
fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun AppUpdateSection(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val updateState by viewModel.updateState.collectAsState()
    var showRepoDialog by remember { mutableStateOf(false) }
    var tempRepoPath by remember { mutableStateOf(updateState.repoPath) }
    var dismissedLater by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        SettingsSectionHeader(title = "APP UPDATE (ऐप अपडेट)", icon = Icons.Default.SystemUpdate)

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {

                // GitHub Repo Path Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "GitHub Repository",
                            style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Text(
                            text = updateState.repoPath,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    IconButton(
                        onClick = {
                            tempRepoPath = updateState.repoPath
                            showRepoDialog = true
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Repository",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Info Grid: Current Version | Latest Version
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Current Version",
                            style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Text(
                            text = "v${updateState.currentVersionName} (Build ${updateState.currentVersionCode})",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Latest Version",
                            style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Text(
                            text = if (updateState.latestVersionCode > 0) "v${updateState.latestVersionName} (Build ${updateState.latestVersionCode})" else "अज्ञात (N/A)",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (updateState.isUpdateAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Last Checked
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Last Checked: ",
                        style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Text(
                        text = viewModel.appUpdateManager.formatLastCheckedTime(updateState.lastCheckedTimestamp),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Status Banner
                val statusContainerColor = when {
                    updateState.isUpdateAvailable -> MaterialTheme.colorScheme.primaryContainer
                    updateState.errorMessage != null -> MaterialTheme.colorScheme.errorContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }

                val statusTextColor = when {
                    updateState.isUpdateAvailable -> MaterialTheme.colorScheme.onPrimaryContainer
                    updateState.errorMessage != null -> MaterialTheme.colorScheme.onErrorContainer
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = statusContainerColor,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = when {
                                    updateState.isUpdateAvailable -> Icons.Default.SystemUpdate
                                    updateState.errorMessage != null -> Icons.Default.Warning
                                    else -> Icons.Default.CheckCircle
                                },
                                contentDescription = null,
                                tint = statusTextColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = updateState.statusMessage,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = statusTextColor
                                )
                            )
                        }

                        if (updateState.errorMessage != null && updateState.errorMessage != updateState.statusMessage) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = updateState.errorMessage!!,
                                style = MaterialTheme.typography.bodySmall.copy(color = statusTextColor)
                            )
                        }
                    }
                }

                // Release Notes (If available)
                if (updateState.releaseNotes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Release Notes (${updateState.releaseTitle}):",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = updateState.releaseNotes,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                // Downloading Progress Bar
                if (updateState.isDownloading) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "APK डाउनलोड हो रहा है... ${updateState.downloadProgressPercentage}%",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { updateState.downloadProgressPercentage / 100f },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // [Check for Updates] Button
                    OutlinedButton(
                        onClick = {
                            dismissedLater = false
                            viewModel.checkForAppUpdates(force = true)
                        },
                        enabled = !updateState.isChecking && !updateState.isDownloading,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (updateState.isChecking) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("जांच रहे हैं...")
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Check for Updates")
                        }
                    }

                    // If Update Available: [Update Now]
                    if (updateState.isUpdateAvailable) {
                        Button(
                            onClick = {
                                viewModel.downloadAndInstallAppUpdate()
                            },
                            enabled = !updateState.isDownloading,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Update Now")
                        }
                    }
                }

                // If Update Available and not dismissed, show Later option button
                if (updateState.isUpdateAvailable && !dismissedLater) {
                    Spacer(modifier = Modifier.height(6.dp))
                    TextButton(
                        onClick = { dismissedLater = true },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Later (बाद में)", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    if (showRepoDialog) {
        AlertDialog(
            onDismissRequest = { showRepoDialog = false },
            title = { Text("GitHub Repository Path") },
            text = {
                Column {
                    Text(
                        text = "अपनी GitHub repository का path प्रविष्ट करें (e.g. username/repository):",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = tempRepoPath,
                        onValueChange = { tempRepoPath = it },
                        singleLine = true,
                        label = { Text("owner/repo") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateGitHubRepoPath(tempRepoPath)
                        showRepoDialog = false
                    }
                ) {
                    Text("सहेजें (Save)")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRepoDialog = false }) {
                    Text("रद्द करें (Cancel)")
                }
            }
        )
    }
}
