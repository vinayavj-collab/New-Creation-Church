package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.appStrings
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    viewModel: MainViewModel,
    onUpcomingEventsClick: () -> Unit,
    onEventCalendarClick: () -> Unit,
    onSavedClick: () -> Unit,
    onRecentlyViewedClick: () -> Unit,
    onCustomizeHomeClick: () -> Unit,
    onBibleClick: () -> Unit,
    onReadingPlanClick: () -> Unit,
    onDailyPrayerClick: () -> Unit = {},
    onNotesClick: () -> Unit,
    onLyricsClick: () -> Unit,
    onPhotosClick: () -> Unit = {},
    onSyncCenterClick: () -> Unit,
    onBackupRestoreClick: () -> Unit,
    onSearchClick: () -> Unit,
    onCategoriesClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAboutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = appStrings()
    val upcomingEvents by viewModel.upcomingEvents.collectAsState()
    val savedItems by viewModel.savedItems.collectAsState()

    val openUrl = { url: String ->
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            // ignore
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.moreTitle, fontWeight = FontWeight.Bold) }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp)
        ) {
            // Section 1: Bible & Spiritual Resources
            item {
                Text(
                    text = "BIBLE & WORSHIP",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        SettingsClickableRow(
                            title = "दैनिक प्रार्थना व प्रेरक वचन (Daily Prayer)",
                            subtitle = "प्रतिदिन विशेष प्रार्थना व प्रेरक बाइबल वचन",
                            icon = Icons.Default.VolunteerActivism,
                            onClick = onDailyPrayerClick
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp))

                        SettingsClickableRow(
                            title = "हिंदी मसीही गीत पुस्तक (Song Book)",
                            subtitle = "गीत संख्या, वर्णमाला इंडेक्स, सर्च व नए गीत जोड़ें",
                            icon = Icons.Default.LibraryMusic,
                            onClick = onLyricsClick
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp))

                        SettingsClickableRow(
                            title = "Bible Reading Plans",
                            subtitle = "365-Day, Gospels, Psalms & New Testament Guides",
                            icon = Icons.Default.AutoStories,
                            onClick = onReadingPlanClick
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp))

                        SettingsClickableRow(
                            title = "My Study Notes",
                            subtitle = "Personal reflections, sermon notes & color tags",
                            icon = Icons.Default.EditNote,
                            onClick = onNotesClick
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp))

                        SettingsClickableRow(
                            title = "फ़ोटो गैलरी (Photos Gallery)",
                            subtitle = "चर्च एवं सेवा की सभी सुंदर तस्वीरें देखें",
                            icon = Icons.Default.PhotoLibrary,
                            onClick = onPhotosClick
                        )
                    }
                }
            }

            // Section 2: Events & Saved
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = strings.secEventsSaved,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        SettingsClickableRow(
                            title = "${strings.upcomingEvents} (${upcomingEvents.size})",
                            subtitle = strings.upcomingEventsSub,
                            icon = Icons.Default.EventAvailable,
                            onClick = onUpcomingEventsClick
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp))

                        SettingsClickableRow(
                            title = strings.eventCalendar,
                            subtitle = strings.eventCalendarSub,
                            icon = Icons.Default.CalendarMonth,
                            onClick = onEventCalendarClick
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp))

                        SettingsClickableRow(
                            title = "${strings.savedForLater} (${savedItems.size})",
                            subtitle = strings.savedForLaterSub,
                            icon = Icons.Default.Bookmarks,
                            onClick = onSavedClick
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp))

                        SettingsClickableRow(
                            title = strings.recentlyViewed,
                            subtitle = strings.recentlyViewedSub,
                            icon = Icons.Default.History,
                            onClick = onRecentlyViewedClick
                        )
                    }
                }
            }

            // Section 3: Search, Tools & Sync
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "SEARCH & CLOUD TOOLS",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        val isSearchEnabled by viewModel.isSearchEnabled.collectAsState()
                        if (isSearchEnabled) {
                            SettingsClickableRow(
                                title = strings.globalSearch,
                                subtitle = strings.globalSearchSub,
                                icon = Icons.Default.Search,
                                onClick = onSearchClick
                            )

                            HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp))
                        }

                        SettingsClickableRow(
                            title = strings.dynamicLabels,
                            subtitle = strings.dynamicLabelsSub,
                            icon = Icons.Default.Label,
                            onClick = onCategoriesClick
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp))

                        SettingsClickableRow(
                            title = "Sync Center",
                            subtitle = "Check offline feeds status and sync all now",
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

            // Section 4: App Customization & Settings
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = strings.secCustomizationApp,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        SettingsClickableRow(
                            title = strings.customizeHome,
                            subtitle = strings.customizeHomeSub,
                            icon = Icons.Default.Tune,
                            onClick = onCustomizeHomeClick
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp))

                        SettingsClickableRow(
                            title = strings.settings,
                            subtitle = strings.settingsSub,
                            icon = Icons.Default.Settings,
                            onClick = onSettingsClick
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
                            title = strings.shareApp,
                            subtitle = strings.shareAppSub,
                            icon = Icons.Default.Share,
                            onClick = {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "Check out Vinay Kumar AVJ App for Fellowship Events, Preaching, Photos & Holy Bible!\nhttps://vinaykumaravj.blogspot.com/"
                                    )
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, strings.shareApp))
                            }
                        )
                    }
                }
            }

            // Official Channels Quick Links
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = strings.secOfficialChannels,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        SettingsClickableRow(
                            title = strings.worshipChannel,
                            subtitle = strings.worshipChannelSub,
                            icon = Icons.Default.Subscriptions,
                            onClick = { openUrl("https://www.youtube.com/@vinaykumaravjworship") }
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp))

                        SettingsClickableRow(
                            title = strings.mainChannel,
                            subtitle = strings.mainChannelSub,
                            icon = Icons.Default.Subscriptions,
                            onClick = { openUrl("https://www.youtube.com/@vinaykumaravj") }
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp))

                        SettingsClickableRow(
                            title = strings.fellowshipBlog,
                            subtitle = strings.fellowshipBlogSub,
                            icon = Icons.Default.Language,
                            onClick = { openUrl("https://vinaykumaravj.blogspot.com/") }
                        )
                    }
                }
            }

            // Section 6: App Updates (Moved to bottom of More screen)
            item {
                Spacer(modifier = Modifier.height(16.dp))
                com.example.ui.components.AppUpdateSection(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun SettingsClickableRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
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
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}
