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
import com.example.data.model.AppLanguage
import com.example.data.model.ThemeMode
import com.example.data.model.appStrings
import com.example.ui.components.FavoriteCategoriesDialog
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onAboutClick: () -> Unit,
    onCustomizeHomeClick: () -> Unit,
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
                "Vinay Kumar AVJ - Fellowship Events • Videos • Photos • Memories\nDownload the official app to stay connected with fellowship events and worship!"
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
            // 1. Language & Localization (App Language Setting)
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

                        // Active status badge
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = when (settings.appLanguage) {
                                        AppLanguage.ENGLISH -> "Active: English"
                                        AppLanguage.HINDI -> "सक्रिय: हिंदी (Hindi)"
                                        AppLanguage.SYSTEM -> "Active: ${strings.langSystem}"
                                    },
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Quick Switch Chips (English, Hindi, System)
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

            // 2. Appearance
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

            // 3. Home Screen Customization & Categories
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

            // 4. Content Sources (CRITICAL: Personal Vlog default OFF)
            item {
                SettingsSectionHeader(title = strings.secContentSources, icon = Icons.Default.Visibility)
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
                            title = strings.fellowshipEvents,
                            subtitle = strings.fellowshipEventsSub,
                            checked = settings.showFellowshipEvents,
                            onCheckedChange = { viewModel.updateShowFellowshipEvents(it) }
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp))

                        SettingsSwitchRow(
                            title = strings.youTubeVideos,
                            subtitle = strings.youTubeVideosSub,
                            checked = settings.showYouTube,
                            onCheckedChange = { viewModel.updateShowYouTube(it) }
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp))

                        SettingsSwitchRow(
                            title = strings.personalVlog,
                            subtitle = strings.personalVlogSub,
                            checked = settings.showPersonalVlog,
                            onCheckedChange = { viewModel.updateShowPersonalVlog(it) }
                        )
                    }
                }
            }

            // 5. Notifications
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

            // 6. Other & Storage
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
fun SettingsSectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 18.dp, end = 16.dp, top = 20.dp, bottom = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 0.8.sp
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
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
