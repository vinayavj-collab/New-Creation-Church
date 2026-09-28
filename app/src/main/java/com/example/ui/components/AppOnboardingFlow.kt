package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.LocalAppProfile
import com.example.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Visual, engaging, and comprehensive Onboarding Flow for New Creation Church app.
 *
 * Covers:
 * 1. App Spiritual Purpose & Welcome
 * 2. Primary Fellowship Events gathering content
 * 3. Daily Spiritual Growth: Reading Plans, Song Book, Audio Messages
 * 4. Prayer Requests & Community Intercession
 * 5. Home Sections customization, Android Homescreen Widget, Notifications, & Updates
 */
@Composable
fun AppOnboardingFlow(
    onComplete: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 5 })
    val isLastPage = pagerState.currentPage == 4

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("app_onboarding_screen"),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            val activeProfile = LocalAppProfile.current

            // Top Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Church Brand Tag with App Brand Logo
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Image(
                        painter = painterResource(id = activeProfile.headerLogoRes),
                        contentDescription = activeProfile.displayNameEnglish,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .height(34.dp)
                            .wrapContentWidth()
                    )
                }

                // Skip Button (Accessible and always available)
                TextButton(
                    onClick = onSkip,
                    modifier = Modifier.testTag("onboarding_skip_button"),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(
                        text = "छोड़ें (Skip)",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }

            // Pager Content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                when (page) {
                    0 -> OnboardingWelcomePage()
                    1 -> OnboardingFellowshipEventsPage()
                    2 -> OnboardingSpiritualGrowthPage()
                    3 -> OnboardingPrayerRequestsPage()
                    4 -> OnboardingCustomizationAndToolsPage()
                }
            }

            // Bottom Navigation & Page Indicators
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                shadowElevation = 8.dp,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Previous Button
                    if (pagerState.currentPage > 0) {
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("onboarding_prev_button"),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "पिछला",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("पिछला", fontSize = 13.sp)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(80.dp))
                    }

                    // Dot Indicators
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(5) { index ->
                            val isSelected = pagerState.currentPage == index
                            Box(
                                modifier = Modifier
                                    .height(8.dp)
                                    .width(if (isSelected) 24.dp else 8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) GoldWarm else MaterialTheme.colorScheme.outlineVariant
                                    )
                                    .clickable {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(index)
                                        }
                                    }
                            )
                        }
                    }

                    // Next / Get Started Button
                    Button(
                        onClick = {
                            if (isLastPage) {
                                onComplete()
                            } else {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isLastPage) GoldWarm else MaterialTheme.colorScheme.primary,
                            contentColor = if (isLastPage) Color.Black else MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.testTag(
                            if (isLastPage) "onboarding_finish_button" else "onboarding_next_button"
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = if (isLastPage) "शुरू करें 🕊️" else "आगे बढ़ें",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            imageVector = if (isLastPage) Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// PAGE 1: App Purpose & Welcome
// -------------------------------------------------------------
@Composable
private fun OnboardingWelcomePage() {
    val activeProfile = LocalAppProfile.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))

        // Hero Brand Logo Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = activeProfile.splashLogoRes),
                contentDescription = activeProfile.displayNameEnglish,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .height(84.dp)
                    .wrapContentWidth()
            )
        }

        Spacer(Modifier.height(16.dp))

        // Title
        Text(
            text = "${activeProfile.displayNameHindi} में आपका स्वागत है!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = activeProfile.subtitleEnglish,
            style = MaterialTheme.typography.labelLarge,
            color = GoldWarm,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = "यह ऐप आपको और आपके परिवार को परमेश्वर के जीवंत वचन, चर्च की आत्मिक संगति और निरंतर प्रार्थना में सुदृढ़ रखने हेतु समर्पित है।",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(Modifier.height(24.dp))

        // Purpose Highlights Cards
        OnboardingFeatureHighlightCard(
            icon = Icons.Default.AutoStories,
            iconColor = GoldWarm,
            title = "दैनिक आत्मिक आहार (Daily Word)",
            description = "पवित्र बाइबिल (हिंदी व अंग्रेजी), दैनिक भक्ति संदेश और सुबह का वचन।"
        )

        Spacer(Modifier.height(10.dp))

        OnboardingFeatureHighlightCard(
            icon = Icons.Default.Groups,
            iconColor = Color(0xFF38BDF8),
            title = "चर्च संगति (Church Fellowship)",
            description = "रविवार आराधना, कुटीर संगति और विशेष चर्च कार्यक्रमों से हमेशा जुड़े रहें।"
        )

        Spacer(Modifier.height(10.dp))

        OnboardingFeatureHighlightCard(
            icon = Icons.Default.VolunteerActivism,
            iconColor = Color(0xFFF43F5E),
            title = "प्रार्थना व मध्यस्थता (Prayer Support)",
            description = "24/7 प्रार्थना निवेदन भेजें, पास्टर्स और प्रार्थना समूह आपकी सहायता में तत्पर हैं।"
        )

        Spacer(Modifier.height(24.dp))
    }
}

// -------------------------------------------------------------
// PAGE 2: Highlight Primary Feature: Fellowship Events
// -------------------------------------------------------------
@Composable
private fun OnboardingFellowshipEventsPage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))

        // Primary Feature Spotlight Badge
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = GoldWarm.copy(alpha = 0.15f),
            border = androidx.compose.foundation.BorderStroke(1.dp, GoldWarm.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.Star, contentDescription = null, tint = GoldWarm, modifier = Modifier.size(16.dp))
                Text(
                    text = "प्रमुख चर्च केंद्र • PRIMARY FEATURE",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = GoldWarm,
                    letterSpacing = 0.5.sp
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        Text(
            text = "Fellowship Events (संगति और सभाएं)",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = "चर्च की मुख्य संगतियों, आराधना व प्रार्थना सभाओं की पूरी जानकारी होम स्क्रीन पर प्रमुखता से उपलब्ध है।",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 19.sp
        )

        Spacer(Modifier.height(18.dp))

        // Mock Interactive Gathering Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, GoldWarm.copy(alpha = 0.35f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(GoldWarm),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Celebration, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text("रविवार मुख्य आराधना (Sunday Service)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("न्यू क्रिएशन चर्च • प्रातः 09:00 AM", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.15f)
                    ) {
                        Text(
                            "लाइव & ऑफलाइन",
                            color = Color(0xFF10B981),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(Modifier.height(10.dp))

                // Fellowship Feature bullets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    FellowshipActionPill(icon = Icons.Default.Alarm, label = "रिमाइंडर सेट")
                    FellowshipActionPill(icon = Icons.Default.LocationOn, label = "स्थान मैप")
                    FellowshipActionPill(icon = Icons.Default.Share, label = "मित्रों को बुलाएं")
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Key Points
        OnboardingInfoBullet(
            icon = Icons.Default.EventAvailable,
            title = "आगामी कार्यक्रम सूची (Calendar Schedule)",
            subtitle = "कुटीर सभा, युवा संगति, उपवास प्रार्थना व विशेष सम्मेलनों का पूरा विवरण।"
        )

        Spacer(Modifier.height(10.dp))

        OnboardingInfoBullet(
            icon = Icons.Default.NotificationsActive,
            title = "समय पर अलर्ट व सूचना (Timely Reminders)",
            subtitle = "सभा शुरू होने से पूर्व नोटिफिकेशन प्राप्त करें ताकि आपकी कोई भी संगति न छूटे।"
        )

        Spacer(Modifier.height(24.dp))
    }
}

// -------------------------------------------------------------
// PAGE 3: Reading Plans, Song Book & Audio Messages
// -------------------------------------------------------------
@Composable
private fun OnboardingSpiritualGrowthPage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(Color(0xFF38BDF8).copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.MenuBook, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(40.dp))
        }

        Spacer(Modifier.height(14.dp))

        Text(
            text = "आत्मिक विकास के सशक्त साधन",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = "पवित्र बाइबिल पठन, मसीही गीतों का खजाना और आत्मिक उपदेश आपके निरंतर विकास के लिए:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(18.dp))

        // Reading Plans Highlight
        OnboardingToolCard(
            icon = Icons.Default.BookmarkBorder,
            iconTint = GoldWarm,
            title = "Reading Plans (बाइबिल पठन योजनाएं)",
            description = "30-दिवसीय विषयवार एवं 1-वर्षीय सम्पूर्ण बाइबिल योजनाएं। रंगीन मार्कर व चैप्टर प्रोग्रेस ट्रैकर के साथ प्रतिदिन वचन पढ़ें।"
        )

        Spacer(Modifier.height(12.dp))

        // Song Book Highlight
        OnboardingToolCard(
            icon = Icons.Default.LibraryMusic,
            iconTint = Color(0xFFA855F7),
            title = "Song Book (गीत और भजन संग्रह)",
            description = "100+ आराधना व स्तुति गीतों के बोल (Lyrics), गिटार कॉर्ड्स, पसंदीदा प्लेलिस्ट और ऑफलाइन पढ़ने की सुविधा।"
        )

        Spacer(Modifier.height(12.dp))

        // Audio Message Highlight
        OnboardingToolCard(
            icon = Icons.Default.Headphones,
            iconTint = Color(0xFF10B981),
            title = "Audio Messages (पास्टोरल संदेश)",
            description = "चर्च पास्टर्स के उपदेश और आत्मिक ऑडियो संदेश। मिनी-प्लेयर व बैकग्राउंड प्लेबैक के साथ किसी भी समय सुनें।"
        )

        Spacer(Modifier.height(24.dp))
    }
}

// -------------------------------------------------------------
// PAGE 4: Prayer Requests & Community Intercession
// -------------------------------------------------------------
@Composable
private fun OnboardingPrayerRequestsPage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(Color(0xFFF43F5E).copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.VolunteerActivism, contentDescription = null, tint = Color(0xFFE11D48), modifier = Modifier.size(40.dp))
        }

        Spacer(Modifier.height(14.dp))

        Text(
            text = "प्रार्थना निवेदन और मध्यस्थता",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = "\"एक दूसरे के लिये प्रार्थना करो ताकि चंगे हो जाओ\" (याकूब 5:16)",
            style = MaterialTheme.typography.bodyMedium,
            color = GoldWarm,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(18.dp))

        // Step by step guide card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                PrayerStepRow(
                    step = "1",
                    title = "प्रार्थना निवेदन भेजें (Submit Request)",
                    desc = "होम स्क्रीन या साइड मेन्यू में 'प्रार्थना निवेदन' पर जाकर '+' दबाएं। अपनी आवश्यकता और विवरण दर्ज करें।"
                )
                PrayerStepRow(
                    step = "2",
                    title = "गोपनीयता विकल्प (Privacy Options)",
                    desc = "निवेदन को 'सार्वजनिक (Public)', 'केवल पास्टर (Pastor Only)' या 'गोपनीय' रख सकते हैं।"
                )
                PrayerStepRow(
                    step = "3",
                    title = "तत्काल अलर्ट (Urgent Prayer Alert)",
                    desc = "संकट की घड़ी में 'Urgent' मार्क करें जिससे सभी मध्यस्थ प्रार्थना योद्धाओं को तत्काल सूचना पहुंचे।"
                )
                PrayerStepRow(
                    step = "4",
                    title = "प्रार्थना टाइमर व गवाही (Timer & Testimony)",
                    desc = "व्यक्तिगत शांत समय हेतु इन-बिल्ट प्रार्थना टाइमर का उपयोग करें और प्रभु के उत्तर की गवाही साझा करें।"
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

// -------------------------------------------------------------
// PAGE 5: Customization, Widget, Notifications & Updates
// -------------------------------------------------------------
@Composable
private fun OnboardingCustomizationAndToolsPage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(GoldWarm.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Tune, contentDescription = null, tint = GoldWarm, modifier = Modifier.size(40.dp))
        }

        Spacer(Modifier.height(14.dp))

        Text(
            text = "कस्टमाइज़ेशन, विजेट और अपडेट्स",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = "ऐप को अपनी आवश्यकतानुसार नियंत्रित करें और हर नए फीचर से जुड़े रहें:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(18.dp))

        // Widget Feature Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, GoldWarm.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(GoldWarm.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Widgets, contentDescription = null, tint = GoldWarm, modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text("Today's Verse Widget (वचन विजेट)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("एंड्रॉइड होम स्क्रीन पर वचन सेट करें", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "• सक्षम करने का तरीका: मोबाइल की होम स्क्रीन पर खाली स्थान पर 2 सेकंड दबाए रखें ➔ 'Widgets' चुनें ➔ 'New Creation Church' विजेट ड्रैग करके स्क्रीन पर रखें।",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Home Screen Sections Toggle Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.ViewAgenda, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text("सेक्शन्स चालू / बंद करना (Home Sections)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("अपनी पसंद से होम स्क्रीन सजाएं", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "• होम स्क्रीन के शीर्ष पर ⚙️ कस्टमाइज़र दबाकर आप 'संगति सभाएं', 'दैनिक भक्ति', 'गीत संग्रह', 'बाइबिल क्विज़' व 'वीडियो' सेक्शन्स को चालू या बंद कर सकते हैं और उनका क्रम बदल सकते हैं।",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Notifications & Updates Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF10B981).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text("सूचनाएं व अपडेट्स (Notifications & Updates)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("वचन अलार्म और नवीनतम संस्करण", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "• प्रतिदिन सुबह का वचन और शाम की प्रार्थना सूचनाएं समय पर पाएं। होम स्क्रीन पर नया अपडेट आने पर 'अपडेट कार्ड' से सीधे 1-टैप में ऐप अपडेट कर सकते हैं।",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

// -------------------------------------------------------------
// Helper Composables for Clean Design
// -------------------------------------------------------------
@Composable
private fun OnboardingFeatureHighlightCard(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(
                    description,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

@Composable
private fun OnboardingInfoBullet(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(26.dp)
                .clip(CircleShape)
                .background(GoldWarm.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = GoldWarm, modifier = Modifier.size(14.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 15.sp)
        }
    }
}

@Composable
private fun OnboardingToolCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(Modifier.height(2.dp))
                Text(description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp)
            }
        }
    }
}

@Composable
private fun PrayerStepRow(
    step: String,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(Color(0xFFE11D48)),
            contentAlignment = Alignment.Center
        ) {
            Text(step, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            Text(desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 15.sp)
        }
    }
}

@Composable
private fun FellowshipActionPill(
    icon: ImageVector,
    label: String
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(13.dp), tint = GoldWarm)
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Medium)
        }
    }
}
