package com.example.ui.prayer

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.prayer.model.DailyPrayerVerse
import com.example.data.prayer.repository.DailyPrayerRepository
import com.example.data.prayer.repository.FirebaseDailyPrayerManager
import com.example.data.prayer.repository.PersonalPrayerJournalRepository
import com.example.data.repository.FirebaseDataRepository
import com.example.ui.theme.GoldWarm
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyPrimary
import com.example.util.AmbientWorshipAudio
import com.example.util.PrayerCardImageGenerator
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyPrayerScreen(
    initialPrayerId: Int? = null,
    onBackClick: () -> Unit,
    onOpenBible: (bookId: Int, chapter: Int, verse: Int) -> Unit,
    viewModel: com.example.ui.viewmodel.MainViewModel? = null
) {
    val context = LocalContext.current
    val prayerManager = remember { FirebaseDailyPrayerManager.getInstance(context) }
    val fbPrayers by prayerManager.firebasePrayers.collectAsState()
    val customPrayers by prayerManager.customizedPrayers.collectAsState()

    val allPrayers = remember { DailyPrayerRepository.getAllPrayers() }
    val todayPrayer = remember { prayerManager.getEffectiveTodayPrayer() }

    var selectedPrayerId by remember {
        mutableIntStateOf(initialPrayerId ?: todayPrayer.id)
    }

    var refreshTrigger by remember { mutableIntStateOf(0) }

    val currentPrayer = remember(selectedPrayerId, fbPrayers, customPrayers, refreshTrigger) {
        prayerManager.getEffectivePrayer(selectedPrayerId)
    }

    var showEnglishPrayer by remember { mutableStateOf(false) }
    var amenCount by remember { mutableIntStateOf(128 + currentPrayer.id * 7) }
    var hasAmened by remember { mutableStateOf(false) }

    // New Prayer Suite State Managers
    val journalRepo = remember { PersonalPrayerJournalRepository.getInstance(context) }
    val streakCount by journalRepo.streakCount.collectAsState()
    val isCompletedToday by journalRepo.isCompletedToday.collectAsState()

    val ambientAudio = remember { AmbientWorshipAudio.getInstance() }
    val isAmbientPlaying by com.example.util.BackgroundMusicManager.isPlaying.collectAsState()
    LaunchedEffect(Unit) {
        com.example.util.BackgroundMusicManager.init(context)
    }

    val coroutineScope = rememberCoroutineScope()
    var isSharingCard by remember { mutableStateOf(false) }
    var showPrayerTimer by remember { mutableStateOf(false) }
    var showPersonalJournal by remember { mutableStateOf(false) }
    var showCommunityRequestsDialog by remember { mutableStateOf(false) }

    val fbRepo = remember { FirebaseDataRepository.getInstance() }
    val communityRequests by fbRepo.prayerRequests.collectAsState()
    val publicCommunityRequests = remember(communityRequests) {
        communityRequests.filter { !it.isPrivate }.take(4)
    }

    // Quick add name state inside card
    var showQuickAddName by remember { mutableStateOf(false) }
    var quickNameText by remember { mutableStateOf("") }
    var mainTabSelection by remember { mutableIntStateOf(0) } // 0: दैनिक प्रार्थना, 1: निवेदन, 2: उत्तरित प्रार्थना और गवाही

    if (showPrayerTimer) {
        PrayerTimerDialog(
            onDismissRequest = { showPrayerTimer = false },
            onPrayerCompleted = {
                val (streak, isNew) = journalRepo.recordDailyPrayerCompleted()
                if (isNew) {
                    Toast.makeText(context, "🔥 अद्भुत! आपकी प्रार्थना स्ट्रीक $streak दिन हो गई है!", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    if (showPersonalJournal) {
        PersonalPrayerJournalDialog(
            onDismissRequest = { showPersonalJournal = false }
        )
    }

    if (showCommunityRequestsDialog && viewModel != null) {
        com.example.ui.components.PrayerRequestsDialog(
            viewModel = viewModel,
            onDismiss = { showCommunityRequestsDialog = false }
        )
    }

    // Text to speech setup
    var ttsEngine by remember { mutableStateOf<TextToSpeech?>(null) }
    var isTtsReady by remember { mutableStateOf(false) }
    var isSpeaking by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        var tts: TextToSpeech? = null
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val hindiLocale = Locale("hi", "IN")
                val langResult = tts?.setLanguage(hindiLocale)
                if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.language = Locale.US
                }
                isTtsReady = true
            }
        }
        ttsEngine = tts
        onDispose {
            tts?.stop()
            tts?.shutdown()
            com.example.util.BackgroundMusicManager.stopMusic()
        }
    }

    val formattedDate = remember {
        val sdf = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("hi", "IN"))
        sdf.format(Date())
    }

    fun speakPrayer() {
        val tts = ttsEngine ?: return
        if (!isTtsReady) {
            Toast.makeText(context, "ऑडियो तैयार हो रहा है...", Toast.LENGTH_SHORT).show()
            return
        }
        if (isSpeaking) {
            tts.stop()
            isSpeaking = false
            return
        }
        val speechText = buildString {
            append("आज का प्रेरक वचन। ")
            append(currentPrayer.verseTextHindi)
            append("। संदर्भ: ")
            append(currentPrayer.verseReferenceHindi)
            append("। ")
            append(currentPrayer.prayerTitleHindi)
            append("। ")
            append(currentPrayer.prayerHindi)
            append("। आत्मिक अंगीकार: ")
            append(currentPrayer.declarationHindi)
        }
        tts.speak(speechText, TextToSpeech.QUEUE_FLUSH, null, "PRAYER_TTS")
        isSpeaking = true
    }

    fun copyToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "क्लिपबोर्ड पर कॉपी किया गया!", Toast.LENGTH_SHORT).show()
    }

    fun shareContent(subject: String, content: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, content)
        }
        context.startActivity(Intent.createChooser(shareIntent, "शेयर करें (Share)"))
    }

    fun sharePrayerCard() {
        if (isSharingCard) return
        isSharingCard = true
        Toast.makeText(context, "प्रार्थना कार्ड तैयार हो रहा है...", Toast.LENGTH_SHORT).show()
        coroutineScope.launch {
            val success = PrayerCardImageGenerator.generateAndShareCard(context, currentPrayer)
            isSharingCard = false
            if (!success) {
                Toast.makeText(context, "कार्ड शेयर करने में असमर्थ", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "🙏 दैनिक प्रार्थना व प्रेरक वचन",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1
                        )
                        Text(
                            text = "Daily Prayer & Motivational Verses",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                            maxLines = 1
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "पीछे जाएं (Back)"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { sharePrayerCard() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "पोस्टर कार्ड शेयर करें (Share Poster Card)",
                            tint = GoldWarm
                        )
                    }
                    IconButton(
                        onClick = { speakPrayer() }
                    ) {
                        Icon(
                            imageVector = if (isSpeaking) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = "प्रार्थना सुनें (Listen Aloud)",
                            tint = if (isSpeaking) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = {
                            val fullShareText = buildString {
                                append("✨ *दैनिक प्रार्थना व प्रेरक वचन*\n")
                                append("📅 $formattedDate\n")
                                append("🌟 विषय: ${currentPrayer.themeTitleHindi} (${currentPrayer.themeTitleEnglish})\n\n")
                                append("📖 *प्रेरक वचन (${currentPrayer.verseReferenceHindi}):*\n")
                                append("\"${currentPrayer.verseTextHindi}\"\n\n")
                                append("🙏 *${currentPrayer.prayerTitleHindi}:*\n")
                                append("${currentPrayer.prayerHindi}\n\n")
                                append("💎 *आत्मिक अंगीकार (Declaration):*\n")
                                append("${currentPrayer.declarationHindi}\n\n")
                                append("📲 *न्यू क्रिएशन चर्च ऐप द्वारा साझा किया गया*")
                            }
                            shareContent("दैनिक प्रार्थना - ${currentPrayer.themeTitleHindi}", fullShareText)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "शेयर करें (Share All)"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        val effectiveViewModel = viewModel ?: androidx.lifecycle.viewmodel.compose.viewModel<com.example.ui.viewmodel.MainViewModel>()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            PrimaryTabRow(
                selectedTabIndex = mainTabSelection,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = mainTabSelection == 0,
                    onClick = { mainTabSelection = 0 },
                    text = {
                        Text(
                            text = "दैनिक प्रार्थना",
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                )
                Tab(
                    selected = mainTabSelection == 1,
                    onClick = { mainTabSelection = 1 },
                    text = {
                        val count = communityRequests.count { !it.isAnswered && !it.isPrivate }
                        Text(
                            text = if (count > 0) "निवेदन ($count)" else "निवेदन",
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                )
                Tab(
                    selected = mainTabSelection == 2,
                    onClick = { mainTabSelection = 2 },
                    text = {
                        val count = communityRequests.count { it.isAnswered }
                        Text(
                            text = if (count > 0) "उत्तरित प्रार्थना ($count)" else "उत्तरित प्रार्थना और गवाही",
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                )
            }

            when (mainTabSelection) {
                0 -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        // 1. Hero Date & Theme Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = NavyDark
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(NavyDark, NavyPrimary)
                                )
                            )
                            .padding(18.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = GoldWarm.copy(alpha = 0.25f)
                                ) {
                                    val isFirebasePrayer = fbPrayers.containsKey(currentPrayer.id)
                                    Text(
                                        text = if (isFirebasePrayer) "दिवस ${currentPrayer.dayNumber} / ${allPrayers.size} • ${currentPrayer.category} 🔥" else "दिवस ${currentPrayer.dayNumber} / ${allPrayers.size} • ${currentPrayer.category}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = GoldWarm,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }

                                if (currentPrayer.id == todayPrayer.id) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF10B981).copy(alpha = 0.25f)
                                    ) {
                                        Text(
                                            text = "⭐ आज का दिन (Today)",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Color(0xFF34D399),
                                                fontWeight = FontWeight.Bold
                                            ),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = currentPrayer.themeTitleHindi,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )

                            Text(
                                text = currentPrayer.themeTitleEnglish,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Day navigation buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val prevId = if (selectedPrayerId > 1) selectedPrayerId - 1 else allPrayers.size
                                        selectedPrayerId = prevId
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color.White
                                    ),
                                    border = ButtonDefaults.outlinedButtonBorder.copy(
                                        brush = Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.3f), Color.White.copy(alpha = 0.3f)))
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.ChevronLeft, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("पिछला दिन", style = MaterialTheme.typography.labelSmall)
                                }

                                TextButton(
                                    onClick = { selectedPrayerId = todayPrayer.id },
                                    colors = ButtonDefaults.textButtonColors(contentColor = GoldWarm)
                                ) {
                                    Icon(Icons.Default.Today, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("आज की प्रार्थना", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                }

                                OutlinedButton(
                                    onClick = {
                                        val nextId = if (selectedPrayerId < allPrayers.size) selectedPrayerId + 1 else 1
                                        selectedPrayerId = nextId
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color.White
                                    ),
                                    border = ButtonDefaults.outlinedButtonBorder.copy(
                                        brush = Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.3f), Color.White.copy(alpha = 0.3f)))
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("अगला दिन", style = MaterialTheme.typography.labelSmall)
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            // 1.5 PRAYER STREAK & QUICK ACTIONS SUITE
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (streakCount > 0) Color(0xFFFEF3C7) else MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(text = "🔥", fontSize = 22.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (streakCount > 0) "$streakCount दिन की प्रार्थना स्ट्रीक!" else "दैनिक प्रार्थना स्ट्रीक",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = if (isCompletedToday) "आज की प्रार्थना पूरी हुई ✅" else "आज की प्रार्थना पूरी कर स्ट्रीक दर्ज करें",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (isCompletedToday) Color(0xFF059669) else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = if (isCompletedToday) FontWeight.Bold else FontWeight.Normal
                                        )
                                    )
                                }
                            }

                            if (!isCompletedToday) {
                                Button(
                                    onClick = {
                                        val (streak, isNew) = journalRepo.recordDailyPrayerCompleted()
                                        Toast.makeText(context, "प्रार्थना पूर्ण! $streak दिन की स्ट्रीक दर्ज। आमीन! 🙌", Toast.LENGTH_SHORT).show()
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldWarm)
                                ) {
                                    Text("पूर्ण हुआ ✓", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF10B981).copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "✓ पूर्ण",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFF059669),
                                            fontWeight = FontWeight.Bold
                                        ),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            AssistChip(
                                onClick = { com.example.util.BackgroundMusicManager.togglePrayerMusic(context) },
                                label = { Text(if (isAmbientPlaying) "संगीत ऑन" else "शांत संगीत") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (isAmbientPlaying) Icons.Default.MusicNote else Icons.Default.MusicOff,
                                        contentDescription = null,
                                        tint = if (isAmbientPlaying) GoldWarm else MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = if (isAmbientPlaying) GoldWarm.copy(alpha = 0.25f) else Color.Transparent
                                ),
                                shape = RoundedCornerShape(10.dp)
                            )

                            AssistChip(
                                onClick = { showPrayerTimer = true },
                                label = { Text("शांत टाइमर") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Timer,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                shape = RoundedCornerShape(10.dp)
                            )

                            AssistChip(
                                onClick = { sharePrayerCard() },
                                label = { Text("पोस्टर") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Image,
                                        contentDescription = null,
                                        tint = GoldWarm,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                shape = RoundedCornerShape(10.dp)
                            )

                            AssistChip(
                                onClick = { showPersonalJournal = true },
                                label = { Text("डायरी") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.MenuBook,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }
            }

            // 2. MOTIVATIONAL BIBLE VERSE CARD
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.FormatQuote,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "📖 आज का प्रेरक वचन",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                    Text(
                                        text = "Motivational Bible Scripture",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = currentPrayer.verseReferenceHindi,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Hindi Verse Text
                        Text(
                            text = "\"${currentPrayer.verseTextHindi}\"",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 26.sp,
                                fontStyle = FontStyle.Normal
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // English Verse Text
                        Text(
                            text = "\"${currentPrayer.verseTextEnglish}\"",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 22.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Verse Actions Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    onOpenBible(
                                        currentPrayer.verseBookId,
                                        currentPrayer.verseChapter,
                                        currentPrayer.verseNumber
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "बाइबल में पढ़ें",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedIconButton(
                                    onClick = {
                                        copyToClipboard(
                                            "प्रेरक वचन",
                                            "\"${currentPrayer.verseTextHindi}\"\n— ${currentPrayer.verseReferenceHindi}"
                                        )
                                    },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "वचन कॉपी करें",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                OutlinedIconButton(
                                    onClick = {
                                        val verseShare = "📖 *प्रेरक वचन*\n\"${currentPrayer.verseTextHindi}\"\n— *${currentPrayer.verseReferenceHindi}*\n\n\"${currentPrayer.verseTextEnglish}\"\n— ${currentPrayer.verseReferenceEnglish}"
                                        shareContent("प्रेरक वचन - ${currentPrayer.verseReferenceHindi}", verseShare)
                                    },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "वचन शेयर करें",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. CURATED DAILY PRAYER CARD
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.VolunteerActivism,
                                            contentDescription = null,
                                            tint = Color(0xFFD97706),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "🙏 विशेष प्रार्थना (Daily Prayer)",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFB45309)
                                        )
                                    )
                                    Text(
                                        text = currentPrayer.prayerTitleHindi,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                }
                            }

                            // TTS listen button
                            FilledTonalIconButton(
                                onClick = { speakPrayer() },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSpeaking) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = "प्रार्थना सुनें",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Full Prayer Text in Hindi
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = currentPrayer.prayerHindi,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        lineHeight = 24.sp,
                                        fontWeight = FontWeight.Normal
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                if (showEnglishPrayer) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "English Prayer Translation:",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = currentPrayer.prayerEnglish,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            lineHeight = 20.sp,
                                            fontStyle = FontStyle.Italic
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // English prayer toggle
                        TextButton(
                            onClick = { showEnglishPrayer = !showEnglishPrayer },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Icon(
                                imageVector = if (showEnglishPrayer) Icons.Default.VisibilityOff else Icons.Default.Translate,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (showEnglishPrayer) "अंग्रेजी प्रार्थना छुपाएं" else "अंग्रेजी में पढ़ें (English Translation)",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 4. FAITH DECLARATION BOX
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = null,
                                        tint = Color(0xFF059669),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "💎 आज का आत्मिक अंगीकार (Faith Declaration)",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF047857)
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "\"${currentPrayer.declarationHindi}\"",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        lineHeight = 22.sp,
                                        color = Color(0xFF065F46)
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 5. DAILY REFLECTION PROMPT BOX
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Lightbulb,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "💡 आज का आत्मिक मनन (Daily Reflection)",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = currentPrayer.reflectionPromptHindi,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        lineHeight = 22.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 6. INTERCESSION & PRAYER LIST CARD: इनके लिए प्रार्थना करें
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Groups,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.tertiary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "इनके लिए प्रार्थना करें :",
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.tertiary
                                            )
                                        )
                                    }

                                    IconButton(
                                        onClick = { showQuickAddName = !showQuickAddName },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (showQuickAddName) Icons.Default.Close else Icons.Default.AddCircleOutline,
                                            contentDescription = "Add name",
                                            tint = MaterialTheme.colorScheme.tertiary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Quick add name field
                                AnimatedVisibility(visible = showQuickAddName) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = quickNameText,
                                            onValueChange = { quickNameText = it },
                                            placeholder = { Text("नया नाम या विषय लिखें...") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        FilledTonalIconButton(
                                            onClick = {
                                                if (quickNameText.isNotBlank()) {
                                                    val updatedList = currentPrayer.customNamesList.toMutableList().apply {
                                                        add(quickNameText.trim())
                                                    }
                                                    prayerManager.updateCustomPrayer(
                                                        prayerId = currentPrayer.id,
                                                        topic = currentPrayer.topic,
                                                        sermonNotes = currentPrayer.sermonNotes,
                                                        announcements = currentPrayer.announcements,
                                                        customNames = updatedList,
                                                        syncToFirebase = true
                                                    )
                                                    quickNameText = ""
                                                    showQuickAddName = false
                                                    refreshTrigger++
                                                    Toast.makeText(context, "नाम जोड़ा गया!", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = "Save")
                                        }
                                    }
                                }

                                // Intercession list with bullets (* उनके नाम, * और नाम, ...)
                                val names = currentPrayer.allIntercessionNames
                                if (names.isEmpty()) {
                                    Text(
                                        text = "* बीमारों, परिवारों व कलीसिया के लिए प्रार्थना करें",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    names.forEach { name ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 3.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Text(
                                                text = "* ",
                                                style = MaterialTheme.typography.bodyLarge.copy(
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = MaterialTheme.colorScheme.tertiary
                                                )
                                            )
                                            Text(
                                                text = name,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.Medium,
                                                    lineHeight = 22.sp
                                                ),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 7. SERMON NOTES BOX (प्रवचन / संदेश नोट्स)
                        if (currentPrayer.sermonNotes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MenuBook,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "📜 प्रवचन व संदेश नोट्स (Sermon Notes)",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = currentPrayer.sermonNotes,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            lineHeight = 22.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        // 8. ANNOUNCEMENTS BOX (घोषणाएं व विशेष सूचनाएं)
                        if (currentPrayer.announcements.isNotBlank()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFFFEF3C7),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Campaign,
                                            contentDescription = null,
                                            tint = Color(0xFFB45309),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "📢 विशेष घोषणाएं व सूचनाएं (Announcements)",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFB45309)
                                            )
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = currentPrayer.announcements,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            lineHeight = 22.sp,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        color = Color(0xFF78350F)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Amen and Share buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (!hasAmened) {
                                        hasAmened = true
                                        amenCount++
                                        val (streak, isNew) = journalRepo.recordDailyPrayerCompleted()
                                        if (isNew) {
                                            Toast.makeText(context, "🙏 आमीन! आपकी प्रार्थना स्ट्रीक $streak दिन हो गई!", Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, "🙏 आमीन! आपकी प्रार्थना परमेश्वर के सम्मुख पहुँची।", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (hasAmened) Color(0xFF059669) else MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    imageVector = if (hasAmened) Icons.Default.CheckCircle else Icons.Default.Favorite,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (hasAmened) "आमीन! ($amenCount)" else "आमीन कहें ($amenCount)",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    val prayerShare = buildString {
                                        append("🙏 *दैनिक प्रार्थना*\n")
                                        append("🌟 *${currentPrayer.themeTitleHindi}*\n\n")
                                        append("${currentPrayer.prayerHindi}\n\n")
                                        append("💎 *अंगीकार:* \"${currentPrayer.declarationHindi}\"\n\n")
                                        append("📖 *वचन:* ${currentPrayer.verseReferenceHindi}")
                                    }
                                    shareContent("दैनिक प्रार्थना - ${currentPrayer.themeTitleHindi}", prayerShare)
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("शेयर")
                            }
                        }
                    }
                }
            }

            // 5.5 COMMUNITY PRAYER WALL
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolunteerActivism,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "🤝 कलीसिया के प्रार्थना निवेदन",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }

                            if (viewModel != null) {
                                TextButton(
                                    onClick = { showCommunityRequestsDialog = true }
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("निवेदन भेजें")
                                }
                            }
                        }

                        Text(
                            text = "एक-दूसरे के भार उठाएं और इस रीति से मसीह की व्यवस्था को पूरा करें। (गलतियों 6:2)",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontStyle = FontStyle.Italic
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        if (publicCommunityRequests.isEmpty()) {
                            Text(
                                text = "अभी कोई सार्वजनिक निवेदन नहीं है। प्रार्थना निवेदन भेजने के लिए ऊपर टैप करें।",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.outline),
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                publicCommunityRequests.forEach { req ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = req.name + if (req.city.isNotBlank()) " (${req.city})" else "",
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = req.requestText,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    maxLines = 2
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            FilledTonalButton(
                                                onClick = {
                                                    fbRepo.incrementPrayingCount(req.id)
                                                    Toast.makeText(context, "प्रार्थना के लिए धन्यवाद! 🙏", Toast.LENGTH_SHORT).show()
                                                },
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("प्रार्थना की 🙏 (${req.prayingCount})", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Navigation to tabs inside card
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
                                onClick = { mainTabSelection = 1 },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("सभी निवेदन देखें 📜", style = MaterialTheme.typography.labelSmall)
                            }
                            OutlinedButton(
                                onClick = { mainTabSelection = 2 },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("उत्तरित गवाही देखें 🎉", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            // 6. BROWSE ALL DAYS CAROUSEL
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    PaddingValues(horizontal = 16.dp)
                    Text(
                        text = "🗓️ सभी 31 दिनों की प्रार्थनाएं (Browse 31-Day Prayers)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(allPrayers) { prayerItem ->
                            val isSelected = prayerItem.id == selectedPrayerId
                            Card(
                                modifier = Modifier
                                    .width(180.dp)
                                    .clickable { selectedPrayerId = prayerItem.id },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                ),
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                                elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "दिन ${prayerItem.dayNumber}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )
                                        if (prayerItem.id == todayPrayer.id) {
                                            Text(
                                                text = "⭐ आज",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = Color(0xFF059669),
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = prayerItem.themeTitleHindi,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                        maxLines = 2
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = prayerItem.verseReferenceHindi,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
                }
                1 -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        item {
                            PrayerRequestAndTestimonySection(
                                viewModel = effectiveViewModel,
                                initialTab = 0,
                                showTabRow = false,
                                onTabChange = { newTab -> mainTabSelection = if (newTab == 0) 1 else 2 },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                2 -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        item {
                            PrayerRequestAndTestimonySection(
                                viewModel = effectiveViewModel,
                                initialTab = 1,
                                showTabRow = false,
                                onTabChange = { newTab -> mainTabSelection = if (newTab == 0) 1 else 2 },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}
