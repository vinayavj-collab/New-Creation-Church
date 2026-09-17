package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LocalAppProfile
import com.example.data.model.ThemeMode
import com.example.data.model.UserSettings
import com.example.ui.theme.GoldWarm

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SidebarContent(
    currentRouteName: String,
    settings: UserSettings,
    onNavigate: (routeKey: String) -> Unit,
    onCloseSidebar: () -> Unit,
    onToggleTheme: () -> Unit,
    onCheckUpdate: () -> Unit,
    onToggleSidebarPosition: () -> Unit = {},
    onOpenFeedback: () -> Unit = {},
    isUpdateAvailable: Boolean = false,
    drawerPosition: String = "left",
    modifier: Modifier = Modifier
) {
    val cornerShape = if (drawerPosition == "right") {
        RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp)
    } else {
        RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
    }

    // Glass UI Sidebar: background rgba(0,0,0,0.4) with blur styling
    ModalDrawerSheet(
        modifier = modifier
            .width(310.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { /* Consume clicks to prevent background Scrim dismissal */ }
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.12f),
                shape = cornerShape
            ),
        drawerContainerColor = Color(0xCC0B1120), // Dark Glass base with high contrast text
        drawerShape = cornerShape
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x66000000)) // Glass overlay rgba(0,0,0,0.4)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { /* Consume clicks to prevent background Scrim dismissal */ }
                )
                .verticalScroll(rememberScrollState())
        ) {
            val activeProfile = LocalAppProfile.current

            // 1. Top Header Logo (Transparent background, height: 52dp, anti-squash)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 18.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = activeProfile.drawerLogoRes),
                    contentDescription = activeProfile.displayNameEnglish,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .height(52.dp)
                        .wrapContentWidth()
                        .wrapContentHeight()
                )
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.15f), thickness = 1.dp)

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Quick App Controls (App Theme, App Update, Sidebar Position)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                shape = RoundedCornerShape(14.dp),
                color = Color.White.copy(alpha = 0.08f)
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    // App Theme Option
                    SidebarActionItem(
                        label = "App Theme: " + when (settings.themeMode) {
                            ThemeMode.DARK -> "Dark (डार्क)"
                            ThemeMode.LIGHT -> "Light (लाइट)"
                            ThemeMode.SYSTEM -> "System (सिस्टम)"
                            ThemeMode.DYNAMIC -> "Dynamic (वॉलपेपर)"
                        },
                        icon = when (settings.themeMode) {
                            ThemeMode.DARK -> Icons.Default.DarkMode
                            ThemeMode.LIGHT -> Icons.Default.LightMode
                            ThemeMode.DYNAMIC -> Icons.Default.Palette
                            ThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                        },
                        onClick = onToggleTheme
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(horizontal = 12.dp))

                    // Sidebar Left / Right Customization
                    SidebarActionItem(
                        label = "Sidebar: " + if (settings.drawerPosition == "right") "Right (दाईं ओर)" else "Left (बाईं ओर)",
                        icon = Icons.Default.SwapHoriz,
                        onClick = onToggleSidebarPosition
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Features moved from "More":
            // --- Section: BIBLE & WORSHIP ---
            SidebarSectionHeader(title = "BIBLE & WORSHIP")

            SidebarNavItem(
                label = "दैनिक प्रार्थना व प्रेरक वचन (Daily Prayer)",
                subtitle = "प्रतिदिन विशेष प्रार्थना व प्रेरक बाइबल वचन",
                icon = Icons.Default.VolunteerActivism,
                selected = currentRouteName == "DAILY_PRAYER",
                onClick = {
                    onNavigate("DAILY_PRAYER")
                    onCloseSidebar()
                }
            )

            SidebarNavItem(
                label = "हिंदी मसीही गीत पुस्तक (Song Book)",
                subtitle = "गीत संख्या, इंडेक्स व नए गीत",
                icon = Icons.Default.LibraryMusic,
                selected = currentRouteName == "LYRICS",
                onClick = {
                    onNavigate("LYRICS")
                    onCloseSidebar()
                }
            )

            SidebarNavItem(
                label = "My Study Notes (स्टडी नोट्स)",
                subtitle = "व्यक्तिगत मनन, नोट्स व टैग्स",
                icon = Icons.Default.EditNote,
                selected = currentRouteName == "NOTES",
                onClick = {
                    onNavigate("NOTES")
                    onCloseSidebar()
                }
            )

            SidebarNavItem(
                label = "फ़ोटो गैलरी (Photos Gallery)",
                subtitle = "चर्च एवं कलीसिया की तस्वीरें",
                icon = Icons.Default.PhotoLibrary,
                selected = currentRouteName == "PHOTOS",
                onClick = {
                    onNavigate("PHOTOS")
                    onCloseSidebar()
                }
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))

            // --- Section: EVENTS & SAVED ---
            SidebarSectionHeader(title = "EVENTS & SAVED")

            SidebarNavItem(
                label = "Upcoming Events (आगामी कार्यक्रम)",
                subtitle = "तारीख, समय व स्थान विवरण",
                icon = Icons.Default.EventAvailable,
                selected = currentRouteName == "EVENTS",
                onClick = {
                    onNavigate("EVENTS")
                    onCloseSidebar()
                }
            )

            SidebarNavItem(
                label = "इवेंट कैलेंडर (Event Calendar)",
                subtitle = "मासिक कैलेंडर व्यू",
                icon = Icons.Default.CalendarMonth,
                selected = currentRouteName == "CALENDAR",
                onClick = {
                    onNavigate("CALENDAR")
                    onCloseSidebar()
                }
            )

            SidebarNavItem(
                label = "सहेजे गए संग्रह (Saved Items)",
                subtitle = "बुकमार्क किए गए वचन व पोस्ट",
                icon = Icons.Default.Bookmarks,
                selected = currentRouteName == "SAVED",
                onClick = {
                    onNavigate("SAVED")
                    onCloseSidebar()
                }
            )

            SidebarNavItem(
                label = "हाल ही में देखे गए (Recently Viewed)",
                subtitle = "पढ़े गए वचन व संदेश",
                icon = Icons.Default.History,
                selected = currentRouteName == "RECENT",
                onClick = {
                    onNavigate("RECENT")
                    onCloseSidebar()
                }
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))

            // --- Section: SEARCH & CLOUD TOOLS ---
            SidebarSectionHeader(title = "SEARCH & CLOUD TOOLS")

            val isSearchEnabled by com.example.util.RemoteConfigManager.isSearchEnabled.collectAsState()
            if (isSearchEnabled) {
                SidebarNavItem(
                    label = "ग्लोबल सर्च (Global Search)",
                    subtitle = "पोस्ट, वीडियो, बाइबल व गीत खोजें",
                    icon = Icons.Default.Search,
                    selected = currentRouteName == "SEARCH",
                    onClick = {
                        onNavigate("SEARCH")
                        onCloseSidebar()
                    }
                )
            }

            SidebarNavItem(
                label = "विषय एवं श्रेणियाँ (Topics & Labels)",
                subtitle = "डायनामिक कैटेगरीज",
                icon = Icons.Default.Label,
                selected = currentRouteName == "CATEGORIES",
                onClick = {
                    onNavigate("CATEGORIES")
                    onCloseSidebar()
                }
            )

            SidebarNavItem(
                label = "सिंक केंद्र (Sync Center)",
                subtitle = "ऑफ़लाइन फ़ीड स्थिति व तुरंत सिंक",
                icon = Icons.Default.Sync,
                selected = currentRouteName == "SYNC",
                onClick = {
                    onNavigate("SYNC")
                    onCloseSidebar()
                }
            )

            SidebarNavItem(
                label = "बैकअप एवं रिस्टोर (Backup & Restore)",
                subtitle = "नोट्स व डेटा एक्सपोर्ट/इम्पोर्ट",
                icon = Icons.Default.CloudUpload,
                selected = currentRouteName == "BACKUP",
                onClick = {
                    onNavigate("BACKUP")
                    onCloseSidebar()
                }
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))

            // --- Section: CUSTOMIZATION & SETTINGS ---
            SidebarSectionHeader(title = "APP SETTINGS & INFO")

            SidebarNavItem(
                label = "सूचनाएं (Notifications)",
                subtitle = "व्यवस्थापक व घोषणा सूचनाएं",
                icon = Icons.Default.Notifications,
                selected = currentRouteName == "NOTIFICATIONS",
                onClick = {
                    onNavigate("NOTIFICATIONS")
                    onCloseSidebar()
                }
            )

            SidebarNavItem(
                label = "होम कस्टमाइज़ करें (Customize Home)",
                subtitle = "सेक्शन क्रम व दृश्यता",
                icon = Icons.Default.Tune,
                selected = currentRouteName == "CUSTOMIZE_HOME",
                onClick = {
                    onNavigate("CUSTOMIZE_HOME")
                    onCloseSidebar()
                }
            )

            SidebarNavItem(
                label = if (isUpdateAvailable) "ऐप अपडेट (App Update) • नया उपलब्ध!" else "ऐप अपडेट (App Update)",
                subtitle = if (isUpdateAvailable) "नया वर्शन उपलब्ध है, अभी अपडेट करें" else "वर्शन और नए अपडेट की जांच करें",
                icon = if (isUpdateAvailable) Icons.Default.SystemUpdate else Icons.Default.Update,
                selected = isUpdateAvailable,
                onClick = {
                    onCheckUpdate()
                    onCloseSidebar()
                }
            )

            SidebarNavItem(
                label = "सेटिंग्स (Settings)",
                subtitle = "प्रोफ़ाइल, भाषा, फ़ॉन्ट व सूचनाएं",
                icon = Icons.Default.Settings,
                selected = currentRouteName == "SETTINGS",
                onClick = {
                    onNavigate("SETTINGS")
                    onCloseSidebar()
                }
            )

            SidebarNavItem(
                label = "ऐप परिचय (About App)",
                subtitle = "मिशन, वर्शन व संपर्क",
                icon = Icons.Default.Info,
                selected = currentRouteName == "ABOUT",
                onClick = {
                    onNavigate("ABOUT")
                    onCloseSidebar()
                }
            )

            SidebarNavItem(
                label = "प्रतिक्रिया व समस्या रिपोर्ट (Feedback)",
                subtitle = "सुझाव, सुधार या बग रिपोर्ट भेजें",
                icon = Icons.Default.Feedback,
                selected = false,
                onClick = {
                    onOpenFeedback()
                    onCloseSidebar()
                }
            )

            SidebarNavItem(
                label = "ऐप शेयर करें (Share App)",
                subtitle = "मित्रों एवं परिवार के साथ साझा करें",
                icon = Icons.Default.Share,
                selected = false,
                onClick = {
                    onNavigate("SHARE")
                    onCloseSidebar()
                }
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))

            // --- Section: OFFICIAL CHANNELS ---
            SidebarSectionHeader(title = "OFFICIAL CHANNELS")

            SidebarNavItem(
                label = "New Creation Church (यूट्यूब चैनल)",
                subtitle = "मुख्य प्रचार एवं चर्च गतिविधि (@newcreationchurchministry51015)",
                icon = Icons.Default.Subscriptions,
                selected = false,
                onClick = {
                    onNavigate("URL_CHURCH")
                    onCloseSidebar()
                }
            )

            SidebarNavItem(
                label = "Worship Channel (यूट्यूब आराधना)",
                subtitle = "@vinaykumaravjworship",
                icon = Icons.Default.Subscriptions,
                selected = false,
                onClick = {
                    onNavigate("URL_WORSHIP")
                    onCloseSidebar()
                }
            )

            SidebarNavItem(
                label = "Main Channel (यूट्यूब मुख्य संदेश)",
                subtitle = "@vinaykumaravj",
                icon = Icons.Default.Subscriptions,
                selected = false,
                onClick = {
                    onNavigate("URL_MAIN")
                    onCloseSidebar()
                }
            )

            SidebarNavItem(
                label = "Fellowship Blog (कलीसिया ब्लॉग)",
                subtitle = "vinaykumaravj.blogspot.com",
                icon = Icons.Default.Language,
                selected = false,
                onClick = {
                    onNavigate("URL_BLOG")
                    onCloseSidebar()
                }
            )

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun SidebarSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            color = GoldWarm,
            letterSpacing = 1.sp
        ),
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
    )
}

@Composable
private fun SidebarNavItem(
    label: String,
    subtitle: String? = null,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        label = {
            Column(modifier = Modifier.padding(vertical = 2.dp)) {
                Text(
                    text = label,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 13.5.sp,
                    color = if (selected) GoldWarm else Color.White.copy(alpha = 0.92f)
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.55f),
                        lineHeight = 13.sp
                    )
                }
            }
        },
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) GoldWarm else Color.White.copy(alpha = 0.75f),
                modifier = Modifier.size(22.dp)
            )
        },
        selected = selected,
        onClick = onClick,
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = Color.White.copy(alpha = 0.15f),
            unselectedContainerColor = Color.Transparent
        ),
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 1.dp)
    )
}

@Composable
private fun SidebarActionItem(
    label: String,
    icon: ImageVector,
    badgeText: String? = null,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = GoldWarm,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )
                )
            }
            if (badgeText != null) {
                Surface(
                    color = MaterialTheme.colorScheme.error,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = badgeText,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
