package com.example.ui.components

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.GoldWarm
import com.example.ui.viewmodel.MainViewModel
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.widget.BibleVerseWidgetProvider
import kotlinx.coroutines.launch

/**
 * Haptic feedback helper to enhance tactile feel during onboarding interactions and step completions.
 */
private fun triggerHaptic(view: View, isSuccess: Boolean = false) {
    try {
        if (isSuccess && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    } catch (_: Exception) {}
}

/**
 * Pixel-Perfect Original Build 99 App Tour Guide (5 Full-Screen Slides)
 * Followed by the Church Experience Setup Flow (Intro -> Notifications -> Wallpaper -> Widget).
 */
@Composable
fun DailyWallpaperOnboardingDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    
    // tourPhase: 1 for App Tour Guide (5 slides), 2 for Setup Intro, 3 for Notification, 4 for Wallpaper, 5 for Widget
    var currentPhase by remember { mutableIntStateOf(1) }

    val adminScripture by viewModel.adminTodayScripture.collectAsStateWithLifecycle()

    // Trigger haptic feedback on step transition / step completion
    LaunchedEffect(currentPhase) {
        if (currentPhase > 1) {
            triggerHaptic(view, isSuccess = true)
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.updateNotifyUpcomingReminders(isGranted)
        viewModel.updateNotifyFellowshipEvents(isGranted)
        if (isGranted) {
            triggerHaptic(view, isSuccess = true)
            Toast.makeText(context, "🔔 सूचना परमिशन स्वीकृत!", Toast.LENGTH_SHORT).show()
        }
        currentPhase = 4 // Advance to Wallpaper step
    }

    Dialog(
        onDismissRequest = { /* Full screen onboarding modal */ },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF0A0E1A) // Original Deep Dark Background
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0A0E1A))
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                AnimatedContent(
                    targetState = currentPhase,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> -width } + fadeOut()
                            )
                        } else {
                            (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> width } + fadeOut()
                            )
                        }
                    },
                    label = "MainOnboardingPhaseAnimation"
                ) { phase ->
                    when (phase) {
                        // 1. Original 5-Slide Full-Screen App Tour Guide
                        1 -> OriginalAppTourGuideScreen(
                            onFinishTour = { currentPhase = 2 },
                            onSkipAll = { currentPhase = 2 }
                        )

                        // 2. Church Experience Setup Intro
                        2 -> SetupStep2Intro(
                            onNext = { currentPhase = 3 },
                            onBack = { currentPhase = 1 }
                        )

                        // 3. Notification Permission
                        3 -> SetupStep3Notification(
                            onAllow = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        viewModel.updateNotifyUpcomingReminders(true)
                                        viewModel.updateNotifyFellowshipEvents(true)
                                        currentPhase = 4
                                    }
                                } else {
                                    viewModel.updateNotifyUpcomingReminders(true)
                                    viewModel.updateNotifyFellowshipEvents(true)
                                    currentPhase = 4
                                }
                            },
                            onSkip = { currentPhase = 4 },
                            onBack = { currentPhase = 2 }
                        )

                        // 4. Set Wallpaper Now
                        4 -> SetupStep4Wallpaper(
                            viewModel = viewModel,
                            scripture = adminScripture,
                            onNext = { currentPhase = 5 },
                            onBack = { currentPhase = 3 }
                        )

                        // 5. Widget Pinning & Final Finish
                        5 -> SetupStep5Widget(
                            onPinWidget = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    try {
                                        val appWidgetManager = AppWidgetManager.getInstance(context)
                                        val myProvider = ComponentName(context, BibleVerseWidgetProvider::class.java)
                                        if (appWidgetManager.isRequestPinAppWidgetSupported) {
                                            appWidgetManager.requestPinAppWidget(myProvider, null, null)
                                            Toast.makeText(context, "📌 विजेट जोड़ने का अनुरोध भेजा गया!", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        // Ignore if unsupported
                                    }
                                }
                            },
                            onFinish = {
                                viewModel.updateWelcomeDialogDismissed(true)
                                Toast.makeText(context, "🌟 कलीसियाई सेटअप पूरा हुआ! ऐप में आपका स्वागत है।", Toast.LENGTH_LONG).show()
                                onDismiss()
                            },
                            onBack = { currentPhase = 4 }
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================
// ORIGINAL 5-SLIDE APP TOUR GUIDE (Recreated from exact screenshots)
// =========================================================================
@Composable
private fun OriginalAppTourGuideScreen(
    onFinishTour: () -> Unit,
    onSkipAll: () -> Unit
) {
    val view = LocalView.current
    val pagerState = rememberPagerState(pageCount = { 5 })
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        // Top App Header: Logo + "New Creation Church" + "छोड़ें (Skip)"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.church_brand_logo),
                    contentDescription = "Church Logo",
                    modifier = Modifier.size(34.dp)
                )
                Text(
                    text = "New Creation Church",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "छोड़ें (Skip)",
                color = Color(0xFF94A3B8),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable {
                        triggerHaptic(view)
                        onSkipAll()
                    }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }

        // Pager for 5 Slides
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { page ->
            when (page) {
                0 -> TourSlide1Welcome()
                1 -> TourSlide2Fellowship()
                2 -> TourSlide3SpiritualGrowth()
                3 -> TourSlide4PrayerSupport()
                4 -> TourSlide5Customization()
            }
        }

        // Bottom Navigation Bar (Prev / Dots / Next-Start)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Button: Previous
            if (pagerState.currentPage > 0) {
                OutlinedButton(
                    onClick = {
                        triggerHaptic(view)
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text("← पिछला", fontSize = 12.sp, color = Color.White)
                }
            } else {
                Spacer(modifier = Modifier.width(80.dp))
            }

            // Center: 5 Dots Indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 5) {
                    val isSelected = pagerState.currentPage == i
                    Surface(
                        shape = CircleShape,
                        color = if (isSelected) GoldWarm else Color(0xFF334155),
                        modifier = Modifier.size(if (isSelected) 18.dp else 6.dp, 6.dp)
                    ) {}
                }
            }

            // Right Button: Next or Start
            if (pagerState.currentPage < 4) {
                Button(
                    onClick = {
                        triggerHaptic(view)
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text("आगे बढ़ें →", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = {
                        triggerHaptic(view, isSuccess = true)
                        onFinishTour()
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldWarm),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text("शुरू करें 🚀 ✔", fontSize = 12.sp, color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// SLIDE 1: न्यू क्रिएशन चर्च में आपका स्वागत है!
// -------------------------------------------------------------
@Composable
private fun TourSlide1Welcome() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Extra Large Grand Boundary-less Church Logo
        Image(
            painter = painterResource(id = R.drawable.church_brand_logo),
            contentDescription = "New Creation Church Logo",
            modifier = Modifier
                .size(210.dp)
                .padding(bottom = 6.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "न्यू क्रिएशन चर्च में आपका स्वागत है!",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Grace • Fellowship • Truth",
            color = GoldWarm,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "यह ऐप आपको और आपके परिवार को परमेश्वर के जीवंत वचन, चर्च की आत्मिक संगति और निरंतर प्रार्थना में सुदृढ़ रखने हेतु समर्पित है।",
            color = Color(0xFF94A3B8),
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 3 Feature Cards
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OriginalTourFeatureCard(
                icon = Icons.Default.MenuBook,
                iconBg = Color(0xFFF59E0B).copy(alpha = 0.2f),
                iconTint = Color(0xFFF59E0B),
                title = "दैनिक आत्मिक आहार (Daily Word)",
                subtitle = "पवित्र बाइबिल (हिंदी व अंग्रेजी), दैनिक भक्ति संदेश और सुबह का वचन।"
            )
            OriginalTourFeatureCard(
                icon = Icons.Default.Groups,
                iconBg = Color(0xFF3B82F6).copy(alpha = 0.2f),
                iconTint = Color(0xFF3B82F6),
                title = "चर्च संगति (Church Fellowship)",
                subtitle = "रविवार आराधना, कुटीर संगति और विशेष चर्च कार्यक्रमों से हमेशा जुड़े रहें।"
            )
            OriginalTourFeatureCard(
                icon = Icons.Default.VolunteerActivism,
                iconBg = Color(0xFFEF4444).copy(alpha = 0.2f),
                iconTint = Color(0xFFEF4444),
                title = "प्रार्थना व मध्यस्थता (Prayer Support)",
                subtitle = "24/7 प्रार्थना निवेदन भेजें, पास्टर्स और प्रार्थना समूह आपकी सहायता में तत्पर हैं।"
            )
        }
    }
}

// -------------------------------------------------------------
// SLIDE 2: Fellowship Events (संगति और सभाएं)
// -------------------------------------------------------------
@Composable
private fun TourSlide2Fellowship() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Tag Chip: ★ प्रमुख चर्च केंद्र • PRIMARY FEATURE
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = GoldWarm.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, GoldWarm.copy(alpha = 0.6f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "★ प्रमुख चर्च केंद्र • PRIMARY FEATURE",
                    color = GoldWarm,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Fellowship Events (संगति और सभाएं)",
            color = Color.White,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "चर्च की मुख्य संगतियों, आराधना व प्रार्थना सभाओं की पूरी जानकारी होम स्क्रीन पर प्रमुखता से उपलब्ध है।",
            color = Color(0xFF94A3B8),
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Highlighted Sunday Service Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Campaign, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(20.dp))
                            }
                        }
                        Column {
                            Text("रविवार मुख्य आराधना (Sunday Service)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("न्यू क्रिएशन चर्च • प्रातः 09:30 AM", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.2f)
                    ) {
                        Text(
                            "लाइव &\nऑफलाइन",
                            color = Color(0xFF10B981),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action Chips Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    EventActionChip(icon = Icons.Default.Alarm, label = "रिमाइंडर सेट", modifier = Modifier.weight(1f))
                    EventActionChip(icon = Icons.Default.Place, label = "स्थान मैप", modifier = Modifier.weight(1f))
                    EventActionChip(icon = Icons.Default.Share, label = "मित्रों को बुलाएं", modifier = Modifier.weight(1.1f))
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2 Bullet Cards
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OriginalTourBulletRow(
                icon = Icons.Default.CalendarMonth,
                iconTint = Color(0xFFF59E0B),
                title = "आगामी कार्यक्रम सूची (Calendar Schedule)",
                desc = "कुटीर सभा, युवा संगति, उपवास प्रार्थना व विशेष सम्मेलनों का पूरा विवरण।"
            )
            OriginalTourBulletRow(
                icon = Icons.Default.NotificationsActive,
                iconTint = Color(0xFFF59E0B),
                title = "समय पर अलर्ट व सूचना (Timely Reminders)",
                desc = "सभा शुरू होने से पूर्व नोटिफिकेशन प्राप्त करें ताकि आपकी कोई भी संगति न छूटे।"
            )
        }
    }
}

// -------------------------------------------------------------
// SLIDE 3: आत्मिक विकास के सशक्त साधन
// -------------------------------------------------------------
@Composable
private fun TourSlide3SpiritualGrowth() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            color = Color(0xFF0369A1).copy(alpha = 0.2f),
            border = BorderStroke(1.5.dp, Color(0xFF0EA5E9))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "आत्मिक विकास के सशक्त साधन",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "पवित्र बाइबिल पठन, मसीही गीतों का खजाना और आत्मिक उपदेश आपके निरंतर विकास के लिए:",
            color = Color(0xFF94A3B8),
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OriginalTourFeatureCard(
                icon = Icons.Default.Bookmark,
                iconBg = Color(0xFFF59E0B).copy(alpha = 0.2f),
                iconTint = Color(0xFFF59E0B),
                title = "Reading Plans (बाइबिल पठन योजनाएं)",
                subtitle = "30-दिवसीय विषयवार एवं 1-वर्षीय सम्पूर्ण बाइबिल योजनाएं। रंगीन मार्कर व चैप्टर प्रोग्रेस ट्रैकर के साथ प्रतिदिन वचन पढ़ें।"
            )
            OriginalTourFeatureCard(
                icon = Icons.Default.LibraryMusic,
                iconBg = Color(0xFFA855F7).copy(alpha = 0.2f),
                iconTint = Color(0xFFA855F7),
                title = "Song Book (गीत और भजन संग्रह)",
                subtitle = "100+ आराधना व स्तुति गीतों के बोल (Lyrics), गिटार कॉर्ड्स, पसंदीदा प्लेलिस्ट और ऑफलाइन पढ़ने की सुविधा।"
            )
            OriginalTourFeatureCard(
                icon = Icons.Default.Headphones,
                iconBg = Color(0xFF10B981).copy(alpha = 0.2f),
                iconTint = Color(0xFF10B981),
                title = "Audio Messages (पास्टोरल संदेश)",
                subtitle = "चर्च पास्टर्स के उपदेश और आत्मिक ऑडियो संदेश। मिनी-प्लेयर व बैकग्राउंड प्लेबैक के साथ किसी भी समय सुनें।"
            )
        }
    }
}

// -------------------------------------------------------------
// SLIDE 4: प्रार्थना निवेदन और मध्यस्थता
// -------------------------------------------------------------
@Composable
private fun TourSlide4PrayerSupport() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            color = Color(0xFFEF4444).copy(alpha = 0.2f),
            border = BorderStroke(1.5.dp, Color(0xFFEF4444))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.VolunteerActivism,
                    contentDescription = null,
                    tint = Color(0xFFF87171),
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "प्रार्थना निवेदन और मध्यस्थता",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "\"एक दूसरे के लिये प्रार्थना करो ताकि चंगे हो जाओ\" (याकूब 5:16)",
            color = GoldWarm,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Large Card with 4 Numbered Points
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OriginalTourNumberedRow(
                    number = "1",
                    title = "प्रार्थना निवेदन भेजें (Submit Request)",
                    desc = "होम स्क्रीन या साइड मेन्यू में 'प्रार्थना निवेदन' पर जाकर '+' दबाएं। अपनी आवश्यकता और विवरण दर्ज करें।"
                )
                OriginalTourNumberedRow(
                    number = "2",
                    title = "गोपनीयता विकल्प (Privacy Options)",
                    desc = "निवेदन को 'सार्वजनिक (Public)', 'केवल पास्टर (Pastor Only)' या 'गोपनीय' रख सकते हैं।"
                )
                OriginalTourNumberedRow(
                    number = "3",
                    title = "तत्काल अलर्ट (Urgent Prayer Alert)",
                    desc = "संकट की घड़ी में 'Urgent' मार्क करें जिससे सभी मध्यस्थ प्रार्थना योद्धाओं को तत्काल सूचना पहुंचे।"
                )
                OriginalTourNumberedRow(
                    number = "4",
                    title = "प्रार्थना टाइमर व गवाही (Timer & Testimony)",
                    desc = "व्यक्तिगत शांत समय हेतु इन-बिल्ट प्रार्थना टाइमर का उपयोग करें और प्रभु के उत्तर की गवाही साझा करें।"
                )
            }
        }
    }
}

// -------------------------------------------------------------
// SLIDE 5: कस्टमाइज़ेशन, विजेट और अपडेट्स
// -------------------------------------------------------------
@Composable
private fun TourSlide5Customization() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            color = GoldWarm.copy(alpha = 0.2f),
            border = BorderStroke(1.5.dp, GoldWarm)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = GoldWarm,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "कस्टमाइज़ेशन, विजेट और अपडेट्स",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "ऐप को अपनी आवश्यकतानुसार नियंत्रित करें और हर नए फीचर से जुड़े रहें:",
            color = Color(0xFF94A3B8),
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OriginalTourDetailedCard(
                icon = Icons.Default.Widgets,
                iconBg = Color(0xFFF59E0B).copy(alpha = 0.2f),
                iconTint = Color(0xFFF59E0B),
                title = "Today's Verse Widget (वचन विजेट)",
                subtitle = "एंड्रॉइड होम स्क्रीन पर वचन सेट करें",
                instruction = "• सक्षम करने का तरीका: मोबाइल की होम स्क्रीन पर खाली स्थान पर 2 सेकंड दबाए रखें → 'Widgets' चुनें → 'New Creation Church' विजेट ड्रैग करके स्क्रीन पर रखें।"
            )
            OriginalTourDetailedCard(
                icon = Icons.Default.Splitscreen,
                iconBg = Color(0xFF3B82F6).copy(alpha = 0.2f),
                iconTint = Color(0xFF3B82F6),
                title = "सेक्शन्स चालू / बंद करना (Home Sections)",
                subtitle = "अपनी पसंद से होम स्क्रीन सजाएं",
                instruction = "• होम स्क्रीन के शीर्ष पर ⚙ कस्टमाइज़र दबाकर आप 'संगति सभाएं', 'दैनिक भक्ति', 'गीत संग्रह', 'बाइबिल क्विज़' व 'वीडियो' सेक्शन्स को चालू या बंद कर सकते हैं और उनका क्रम बदल सकते हैं।"
            )
            OriginalTourDetailedCard(
                icon = Icons.Default.NotificationsActive,
                iconBg = Color(0xFF10B981).copy(alpha = 0.2f),
                iconTint = Color(0xFF10B981),
                title = "सूचनाएं व अपडेट्स (Notifications & Updates)",
                subtitle = "वचन अलार्म और नवीनतम संस्करण",
                instruction = "• प्रतिदिन सुबह का वचन और शाम की प्रार्थना सूचनाएं समय पर पाएं। होम स्क्रीन पर नया अपडेट आने पर 'अपडेट कार्ड' से सीधे 1-टैप में ऐप अपडेट कर सकते हैं।"
            )
        }
    }
}

// -------------------------------------------------------------
// REUSABLE ORIGINAL COMPONENTS FOR APP TOUR
// -------------------------------------------------------------
@Composable
private fun OriginalTourFeatureCard(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        border = BorderStroke(1.dp, Color(0xFF334155))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = iconBg,
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
                }
            }
            Column {
                Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(2.dp))
                Text(subtitle, color = Color(0xFF94A3B8), fontSize = 11.sp, lineHeight = 15.sp)
            }
        }
    }
}

@Composable
private fun OriginalTourDetailedCard(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    instruction: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        border = BorderStroke(1.dp, Color(0xFF334155))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = iconBg,
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
                    }
                }
                Column {
                    Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(subtitle, color = Color(0xFF94A3B8), fontSize = 10.5.sp)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(instruction, color = Color(0xFFCBD5E1), fontSize = 10.5.sp, lineHeight = 14.sp)
        }
    }
}

@Composable
private fun OriginalTourBulletRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp).padding(top = 2.dp))
        Column {
            Text(title, color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
            Text(desc, color = Color(0xFF94A3B8), fontSize = 11.sp, lineHeight = 15.sp)
        }
    }
}

@Composable
private fun OriginalTourNumberedRow(
    number: String,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = Color(0xFFEF4444),
            modifier = Modifier.size(20.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(number, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        Column {
            Text(title, color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
            Text(desc, color = Color(0xFF94A3B8), fontSize = 11.sp, lineHeight = 15.sp)
        }
    }
}

@Composable
private fun EventActionChip(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF0F172A),
        border = BorderStroke(1.dp, Color(0xFF334155))
    ) {
        Row(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFFCBD5E1), modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, color = Color(0xFFCBD5E1), fontSize = 9.5.sp, fontWeight = FontWeight.Medium)
        }
    }
}

// =========================================================================
// CHURCH EXPERIENCE SETUP PHASES (Steps 2 to 5)
// =========================================================================

// -------------------------------------------------------------
// STEP 2: कलीसियाई अनुभव सेटअप विंडो (Intro Setup Window)
// -------------------------------------------------------------
@Composable
private fun SetupStep2Intro(
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val view = LocalView.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Extra Large Grand Boundary-less Church Logo
        Image(
            painter = painterResource(id = R.drawable.church_brand_logo),
            contentDescription = "New Creation Church Logo",
            modifier = Modifier
                .size(180.dp)
                .padding(bottom = 6.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "कलीसियाई अनुभव सेटअप",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Church Experience Setup",
            color = GoldWarm,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "प्रभु यीशु मसीह के पवित्र नाम में आपका स्वागत है! आइए कुछ आसान चरणों में अपने फोन को आत्मिक संगति, दैनिक वचन व कलीसिया से जोड़ें।",
            color = Color(0xFF94A3B8),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SetupFeatureRow(icon = Icons.Default.NotificationsActive, iconColor = Color(0xFFF59E0B), title = "समय पर सूचनाएं (Notifications)", desc = "प्रातः वचन, रविवार आराधना एवं लाइव सभा अलर्ट्स प्राप्त करें")
                SetupFeatureRow(icon = Icons.Default.Wallpaper, iconColor = Color(0xFF6366F1), title = "दैनिक AI वचन वॉलपेपर (Wallpaper)", desc = "प्रतिदिन नए आत्मिक वचन का वॉलपेपर फोन स्क्रीन पर लगाएं")
                SetupFeatureRow(icon = Icons.Default.Widgets, iconColor = Color(0xFF10B981), title = "होम स्क्रीन विजेट (Widget)", desc = "बिना ऐप खोले सीधे मुख्य स्क्रीन पर आज का वचन पढ़ें")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = {
                    triggerHaptic(view)
                    onBack()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Text("← टूर गाइड", color = Color.White, fontSize = 13.sp)
            }

            Button(
                onClick = {
                    triggerHaptic(view, isSuccess = true)
                    onNext()
                },
                modifier = Modifier
                    .weight(2f)
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
            ) {
                Text("आगे बढ़ें (अनुमति व वॉलपेपर) →", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// -------------------------------------------------------------
// STEP 3: नोटिफिकेशन परमिशन
// -------------------------------------------------------------
@Composable
private fun SetupStep3Notification(
    onAllow: () -> Unit,
    onSkip: () -> Unit,
    onBack: () -> Unit
) {
    val view = LocalView.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            color = Color(0xFFF59E0B).copy(alpha = 0.15f),
            border = BorderStroke(1.5.dp, Color(0xFFF59E0B))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "सूचनाएँ चालू करें (Notifications)",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Stay connected with church alerts & live worship",
            color = Color(0xFFF59E0B),
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("🔔 आपको इन महत्वपूर्ण समयों पर सूचना मिलेगी:", color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                Text("• 🌅 प्रातः 6:00 बजे आज का आत्मिक वचन व मनन", color = Color(0xFFCBD5E1), fontSize = 13.sp)
                Text("• 🔴 रविवार व उपवास सभाओं का सीधा लाइव प्रसारण", color = Color(0xFFCBD5E1), fontSize = 13.sp)
                Text("• 🙏 कलीसियाई विशेष प्रार्थना निवेदन एवं कार्यक्रम", color = Color(0xFFCBD5E1), fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                triggerHaptic(view, isSuccess = true)
                onAllow()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
        ) {
            Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("🔔 सूचना परमिशन स्वीकार करें (Allow)", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = {
                triggerHaptic(view)
                onBack()
            }) {
                Text("← पिछला", color = Color(0xFF94A3B8))
            }
            TextButton(onClick = {
                triggerHaptic(view)
                onSkip()
            }) {
                Text("बाद में निर्णय लें (Skip) →", color = Color(0xFF94A3B8))
            }
        }
    }
}

// -------------------------------------------------------------
// STEP 4: वॉलपेपर लाइव पूर्वावलोकन व सेटअप (Wallpaper Preview & Setup)
// -------------------------------------------------------------
@Composable
private fun SetupStep4Wallpaper(
    viewModel: MainViewModel,
    scripture: com.example.data.model.AdminTodayScripture,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    var isApplying by remember { mutableStateOf(false) }
    var isAppliedSuccessfully by remember { mutableStateOf(false) }
    var selectedStyleIndex by remember { mutableIntStateOf(0) } // 0: Historical, 1: Botanical, 2: Pastel, 3: Living Water
    var isFullScreenPreviewOpen by remember { mutableStateOf(false) }

    val hindiVerse = scripture.hindiText.ifBlank { "प्रभु मेरा चरवाहा है; मुझे कुछ घटी न होगी।" }
    val verseRef = scripture.bookAndVerse.ifBlank { "भजन संहिता 23:1" }

    val stylesList = listOf(
        Triple("🏛 ऐतिहासिक", "Cinematic Oil", listOf(Color(0xFF0C101C), Color(0xFF1C1426), Color(0xFF120C18))),
        Triple("🌿 वॉटरकलर", "Botanical Vine", listOf(Color(0xFF12261C), Color(0xFF1A2E24), Color(0xFF0E1C16))),
        Triple("🕊 स्वर्गीय", "Dreamy Pastel", listOf(Color(0xFF1E1432), Color(0xFF2D193C), Color(0xFF141228))),
        Triple("💧 जीवन का जल", "Storybook", listOf(Color(0xFF0E1E30), Color(0xFF142A3A), Color(0xFF0A1624)))
    )

    val currentGradients = stylesList[selectedStyleIndex].third
    val accentGold = when (selectedStyleIndex) {
        0 -> Color(0xFFFBBF24)
        1 -> Color(0xFF6EE7B7)
        2 -> Color(0xFFE879F9)
        else -> Color(0xFF7DD3FC)
    }

    // Full Screen Preview Dialog
    if (isFullScreenPreviewOpen) {
        Dialog(
            onDismissRequest = { isFullScreenPreviewOpen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(currentGradients)
                    )
            ) {
                // Background Ambient Glow
                Box(
                    modifier = Modifier
                        .size(360.dp)
                        .align(Alignment.Center)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(accentGold.copy(alpha = 0.35f), Color.Transparent)
                            )
                        )
                )

                // Wallpaper Full Screen Mockup Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top Lockscreen Time Mockup
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("07:00", color = Color.White, fontSize = 56.sp, fontWeight = FontWeight.Bold)
                        Text("बुधवार, दैनिक आत्मिक वचन", color = Color(0xFFCBD5E1), fontSize = 14.sp)
                    }

                    // Centered Scripture Card with Sacred Radiant Cross
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = accentGold,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "✝  आज का वचन  ✝",
                            color = accentGold,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "“$hindiVerse”",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            lineHeight = 28.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "— $verseRef —",
                            color = accentGold,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Bottom Floating Action Controls
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                triggerHaptic(view)
                                isFullScreenPreviewOpen = false
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Black.copy(alpha = 0.4f))
                        ) {
                            Text("❌ बंद करें", color = Color.White, fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                triggerHaptic(view)
                                isFullScreenPreviewOpen = false
                                isApplying = true
                                val chosenStyle = when (selectedStyleIndex) {
                                    1 -> com.example.util.DailyWallpaperService.TheologicalStyle.SOFT_BOTANICAL_WATERCOLOR
                                    2 -> com.example.util.DailyWallpaperService.TheologicalStyle.ETHEREAL_DREAMY_PASTEL
                                    3 -> com.example.util.DailyWallpaperService.TheologicalStyle.VIBRANT_SPIRITUAL_STORYBOOK
                                    else -> com.example.util.DailyWallpaperService.TheologicalStyle.CINEMATIC_BIBLICAL_HISTORICAL
                                }
                                viewModel.updateDailyWallpaperEnabled(true)
                                viewModel.applyDailyWallpaperNow(context, selectedStyle = chosenStyle) { success ->
                                    isApplying = false
                                    if (success) {
                                        isAppliedSuccessfully = true
                                        triggerHaptic(view, isSuccess = true)
                                        Toast.makeText(context, "✅ आज का वचन AI वॉलपेपर फोन पर सेट हो गया!", Toast.LENGTH_LONG).show()
                                    } else {
                                        triggerHaptic(view)
                                        Toast.makeText(context, "❌ वॉलपेपर सेट करने में समस्या आई, कृपया दोबारा प्रयास करें।", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1.8f)
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = if (isAppliedSuccessfully) Color(0xFF10B981) else GoldWarm)
                        ) {
                            Icon(Icons.Default.Wallpaper, contentDescription = null, tint = Color(0xFF0F172A), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("✨ यह AI वॉलपेपर लगाएं", color = Color(0xFF0F172A), fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 10.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Step Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFF6366F1).copy(alpha = 0.2f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Wallpaper, contentDescription = null, tint = Color(0xFF818CF8), modifier = Modifier.size(20.dp))
                }
            }
            Column {
                Text(
                    text = "दैनिक AI वॉलपेपर पूर्वावलोकन",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Live Sample Scripture Wallpaper Preview",
                    color = Color(0xFF818CF8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Art Style Selector Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            stylesList.forEachIndexed { index, (label, _, _) ->
                val isSelected = selectedStyleIndex == index
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) GoldWarm.copy(alpha = 0.2f) else Color(0xFF1E293B),
                    border = BorderStroke(1.dp, if (isSelected) GoldWarm else Color(0xFF334155)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            triggerHaptic(view)
                            selectedStyleIndex = index
                        }
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) GoldWarm else Color(0xFFCBD5E1),
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Live Phone Mockup Card (Interactive Sample Preview)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    triggerHaptic(view)
                    isFullScreenPreviewOpen = true
                },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            border = BorderStroke(1.5.dp, accentGold.copy(alpha = 0.8f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .background(
                        Brush.verticalGradient(currentGradients)
                    )
                    .padding(14.dp)
            ) {
                // Radial Glow
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .align(Alignment.Center)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(accentGold.copy(alpha = 0.3f), Color.Transparent)
                            )
                        )
                )

                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Mockup Top Status Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("07:00", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color.Black.copy(alpha = 0.5f)
                        ) {
                            Text("🔍 टैप करके बड़ा देखें", color = accentGold, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }

                    // Center Scripture Verse Layout
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = accentGold, modifier = Modifier.size(14.dp))
                            Text("✝ आज का वचन ✝", color = accentGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "“$hindiVerse”",
                            color = Color.White,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "— $verseRef —",
                            color = accentGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Mockup Bottom Home Bar
                    Surface(
                        modifier = Modifier.size(60.dp, 3.dp),
                        shape = RoundedCornerShape(2.dp),
                        color = Color.White.copy(alpha = 0.4f)
                    ) {}
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Full Screen Preview Button
        OutlinedButton(
            onClick = {
                triggerHaptic(view)
                isFullScreenPreviewOpen = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF475569)),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF1E293B))
        ) {
            Icon(Icons.Default.Visibility, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("🔍 पूर्ण स्क्रीन पूर्वावलोकन देखें (Full Screen Preview)", color = Color(0xFF38BDF8), fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Apply Wallpaper Button
        Button(
            onClick = {
                triggerHaptic(view)
                isApplying = true
                val chosenStyle = when (selectedStyleIndex) {
                    1 -> com.example.util.DailyWallpaperService.TheologicalStyle.SOFT_BOTANICAL_WATERCOLOR
                    2 -> com.example.util.DailyWallpaperService.TheologicalStyle.ETHEREAL_DREAMY_PASTEL
                    3 -> com.example.util.DailyWallpaperService.TheologicalStyle.VIBRANT_SPIRITUAL_STORYBOOK
                    else -> com.example.util.DailyWallpaperService.TheologicalStyle.CINEMATIC_BIBLICAL_HISTORICAL
                }
                viewModel.updateDailyWallpaperEnabled(true)
                viewModel.applyDailyWallpaperNow(context, selectedStyle = chosenStyle) { success ->
                    isApplying = false
                    if (success) {
                        isAppliedSuccessfully = true
                        triggerHaptic(view, isSuccess = true)
                        Toast.makeText(context, "✅ आज का वचन AI वॉलपेपर फोन पर सेट हो गया!", Toast.LENGTH_LONG).show()
                    } else {
                        triggerHaptic(view)
                        Toast.makeText(context, "❌ वॉलपेपर सेट करने में समस्या आई, कृपया दोबारा प्रयास करें।", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = if (isAppliedSuccessfully) Color(0xFF10B981) else GoldWarm),
            enabled = !isApplying
        ) {
            if (isApplying) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color(0xFF0F172A))
                Spacer(modifier = Modifier.width(8.dp))
                Text("वॉलपेपर सेट किया जा रहा है...", color = Color(0xFF0F172A), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            } else if (isAppliedSuccessfully) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("✨ वॉलपेपर सफलतापूर्वक लग गया!", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            } else {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF0F172A), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("✨ वॉलपेपर अभी फोन पर लागू करें (Set Now)", color = Color(0xFF0F172A), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = {
                triggerHaptic(view)
                onBack()
            }) {
                Text("← पिछला", color = Color(0xFF94A3B8))
            }
            TextButton(onClick = {
                triggerHaptic(view, isSuccess = true)
                onNext()
            }) {
                Text(if (isAppliedSuccessfully) "अगले चरण (विजेट) पर जाएं ➔" else "बाद में सेट करें / आगे बढ़ें (Next) ➔", color = Color(0xFF38BDF8))
            }
        }
    }
}

// -------------------------------------------------------------
// STEP 5: विजेट व समापन (Widget Pinning & Final Finish)
// -------------------------------------------------------------
@Composable
private fun SetupStep5Widget(
    onPinWidget: () -> Unit,
    onFinish: () -> Unit,
    onBack: () -> Unit
) {
    val view = LocalView.current
    var isWidgetPinned by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            color = Color(0xFF10B981).copy(alpha = 0.15f),
            border = BorderStroke(1.5.dp, Color(0xFF10B981))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Widgets,
                    contentDescription = null,
                    tint = Color(0xFF34D399),
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "होम स्क्रीन विजेट (Widget)",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Pin Today's Scripture to Home Screen",
            color = Color(0xFF34D399),
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("📱 फोन की मुख्य स्क्रीन पर विजेट का लाभ:", color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                Text("• बिना ऐप खोले सीधे मुख्य स्क्रीन पर आज का वचन पढ़ें", color = Color(0xFFCBD5E1), fontSize = 13.sp)
                Text("• रविवार की उपस्थिति व महत्वपूर्ण घोषणाएं देखें", color = Color(0xFFCBD5E1), fontSize = 13.sp)
                Text("• एक क्लिक में सीधे प्रार्थना और वचन पर पहुंचें", color = Color(0xFFCBD5E1), fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Colorful Primary Action Button for Pinning Widget
        Button(
            onClick = {
                isWidgetPinned = true
                triggerHaptic(view, isSuccess = true)
                onPinWidget()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isWidgetPinned) Color(0xFF10B981) else Color(0xFF2563EB)
            )
        ) {
            Icon(
                if (isWidgetPinned) Icons.Default.CheckCircle else Icons.Default.PushPin,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                if (isWidgetPinned) "✅ विजेट पिन अनुरोध भेजा गया" else "📌 होम स्क्रीन पर विजेट जोड़ें (Pin Widget)",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Uncolored / Subtle Finish Button
        OutlinedButton(
            onClick = {
                triggerHaptic(view, isSuccess = true)
                onFinish()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color(0xFF475569)),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.Transparent,
                contentColor = Color(0xFFCBD5E1)
            )
        ) {
            Text(
                "कलीसियाई यात्रा शुरू करें (Finish & Enter App) ➔",
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFCBD5E1)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        TextButton(onClick = {
            triggerHaptic(view)
            onBack()
        }) {
            Text("← पिछला चरण (वॉलपेपर)", color = Color(0xFF94A3B8), fontSize = 12.sp)
        }
    }
}

@Composable
private fun SetupFeatureRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    desc: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = iconColor.copy(alpha = 0.15f),
            modifier = Modifier.size(38.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
        }
        Column {
            Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(desc, color = Color(0xFF94A3B8), fontSize = 11.sp, lineHeight = 15.sp)
        }
    }
}
