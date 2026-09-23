package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LocalAppProfile
import com.example.ui.theme.GoldWarm
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    isPersonalVlogAllowed: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val openLink = { url: String ->
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
                title = { Text("About", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
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
            item {
                val activeProfile = LocalAppProfile.current
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = activeProfile.appIconRes),
                            contentDescription = activeProfile.displayNameEnglish,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Fit
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = activeProfile.displayNameEnglish,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )

                    Text(
                        text = activeProfile.subtitleEnglish,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )

                    Text(
                        text = activeProfile.displayNameHindi,
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.outline,
                            fontWeight = FontWeight.Medium
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "About This Application",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "This application serves as a dedicated central hub for fellowship events, sermons, event reports, praise songs, and memories.\n\nThe primary focus of this app is \"Fellowship Events\", unifying public archives and YouTube channels into one fast, seamless, and offline-capable mobile experience.",
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 22.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "✨ What's New in Version 1.4:",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = NavyPrimary)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "• 📝 Rich Notes Editor: Bold, Italic, Underline, Strikethrough, Lists, Alignment & Verse Linking with dedicated Notes Window.\n• ⭐ Favorites & Bookmarks: Save, star, and organize inspiring verses.\n• 📸 Photo Verse Creator: Generate shareable picture quotes with scenic backgrounds.\n• 🔴 Words of Jesus in Red: Highlight Christ's words with custom color options.\n• ⏱️ Screen Timeout: Configurable keep-screen-on and auto screen-off timers.\n• ⚙️ Full Reading Customization: Subheadings, verse numbers, paragraph indents & text justification toggles.",
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "OFFICIAL SOURCES & LINKS",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        AboutLinkRow(
                            title = "Fellowship Events Blog",
                            subtitle = "vinaykumaravj.blogspot.com",
                            icon = Icons.Default.Language,
                            onClick = { openLink("https://vinaykumaravj.blogspot.com/") }
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp))

                        AboutLinkRow(
                            title = "Vinay Kumar AVJ YouTube",
                            subtitle = "youtube.com/@vinaykumaravj",
                            icon = Icons.Default.PlayCircle,
                            onClick = { openLink("https://youtube.com/@vinaykumaravj") }
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp))

                        AboutLinkRow(
                            title = "AVJ Worship YouTube",
                            subtitle = "youtube.com/@vinaykumaravjworship",
                            icon = Icons.Default.PlayCircle,
                            onClick = { openLink("https://youtube.com/@vinaykumaravjworship") }
                        )

                        if (isPersonalVlogAllowed) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp))

                            AboutLinkRow(
                                title = "Personal Life Blog (पर्सनल लाइफ़)",
                                subtitle = "vinayavj.blogspot.com",
                                icon = Icons.Default.Language,
                                onClick = { openLink("https://vinayavj.blogspot.com/") }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AboutLinkRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
        }
        Icon(
            imageVector = Icons.Default.OpenInNew,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(16.dp)
        )
    }
}
