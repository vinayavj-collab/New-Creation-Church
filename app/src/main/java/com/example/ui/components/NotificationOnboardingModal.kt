package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AppLanguage
import com.example.data.model.LocalAppLanguage

@Composable
fun NotificationOnboardingModal(
    onDismiss: () -> Unit,
    onGrantPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val language = LocalAppLanguage.current
    val isHindi = language == AppLanguage.HINDI

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(tween(250)) + scaleIn(tween(250), initialScale = 0.92f)
        ) {
            Surface(
                modifier = modifier
                    .padding(horizontal = 24.dp, vertical = 24.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .testTag("notification_onboarding_modal"),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Animated/Tonal Icon
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = if (isHindi) "सूचनाएं" else "Notifications",
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Title
                    Text(
                        text = if (isHindi) "आत्मिक संगति व सूचनाएं" else "Stay Connected & Inspired",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Polite Headline
                    Text(
                        text = if (isHindi)
                            "दैनिक वचन, प्रार्थना समय और विशेष सभाओं की जानकारी समय पर पाने के लिए सूचनाओं की अनुमति आवश्यक है।"
                        else
                            "To receive timely Bible verses, daily prayer reminders, and fellowship announcements, please allow notifications.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Benefit Card Items
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            OnboardingBenefitRow(
                                icon = Icons.Default.AutoStories,
                                title = if (isHindi) "दैनिक बाइबल वचन (Daily Verses)" else "Daily Bible Verses",
                                description = if (isHindi)
                                    "हर सुबह आत्मिक विचार और परमेश्वर के वचन की प्रेरणा।"
                                else
                                    "Start your day with uplifting scripture and devotionals."
                            )

                            OnboardingBenefitRow(
                                icon = Icons.Default.AccessTime,
                                title = if (isHindi) "प्रार्थना व पाठ योजना (Prayer Times)" else "Prayer & Reading Reminders",
                                description = if (isHindi)
                                    "नियमित प्रार्थना और बाइबल पठन में निरंतरता बनाए रखें।"
                                else
                                    "Gentle alerts for scheduled prayer times and reading plans."
                            )

                            OnboardingBenefitRow(
                                icon = Icons.Default.Groups,
                                title = if (isHindi) "संगति व नए अपडेट्स (Fellowship & Vlogs)" else "Fellowship & Blog Updates",
                                description = if (isHindi)
                                    "नए ब्लॉग, व्लॉग और आत्मिक सभाओं की सूचना।"
                                else
                                    "Timely notifications for new blog posts, videos, and events."
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Privacy & Respect note
                    Text(
                        text = if (isHindi)
                            "🔒 हम केवल आत्मिक विकास के लिए ही सूचनाएं भेजते हैं। आप इन्हें सेटिंग्स में कभी भी बदल सकते हैं।"
                        else
                            "🔒 We only send uplifting, meaningful notifications. You can change this anytime in Settings.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("notification_onboarding_later_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (isHindi) "बाद में" else "Not Now",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }

                        Button(
                            onClick = onGrantPermission,
                            modifier = Modifier
                                .weight(1.3f)
                                .height(48.dp)
                                .testTag("notification_onboarding_allow_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isHindi) "अनुमति दें" else "Allow",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingBenefitRow(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }
    }
}
