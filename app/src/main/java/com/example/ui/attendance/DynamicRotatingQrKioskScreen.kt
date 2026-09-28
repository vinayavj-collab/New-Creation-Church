package com.example.ui.attendance

import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GoldWarm
import com.example.ui.viewmodel.MainViewModel
import com.example.util.AttendanceSecurityHelper
import com.example.util.QrCodeHelper
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicRotatingQrKioskScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val branchSettings by viewModel.branchAttendanceSettings.collectAsState()
    val globalConfig by viewModel.attendanceGlobalConfig.collectAsState()
    val todayRecords by viewModel.todayAttendanceRecords.collectAsState()
    val activeWindow by viewModel.activeServiceWindow.collectAsState()

    val rotationSeconds = globalConfig.dynamicQrRotationSeconds.coerceIn(15, 120)
    var currentPeriodProgress by remember { mutableFloatStateOf(1f) }
    var secondsRemaining by remember { mutableIntStateOf(rotationSeconds) }
    var dynamicPayload by remember { mutableStateOf("") }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Auto-refresh timer loop
    LaunchedEffect(branchSettings, rotationSeconds) {
        while (true) {
            val (payload, expTime) = AttendanceSecurityHelper.generateDynamicRotatingQrPayload(
                branchId = branchSettings.branchId,
                secretKey = branchSettings.secretHmacKey,
                rotationSeconds = rotationSeconds
            )
            dynamicPayload = payload
            qrBitmap = QrCodeHelper.generateQrBitmap(payload, sizePx = 600)

            // Inner countdown loop
            val startTime = System.currentTimeMillis()
            val totalDurationMs = rotationSeconds * 1000L
            while (System.currentTimeMillis() < expTime) {
                val remainingMs = (expTime - System.currentTimeMillis()).coerceAtLeast(0)
                secondsRemaining = (remainingMs / 1000).toInt() + 1
                currentPeriodProgress = (remainingMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
                delay(100)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Tv, contentDescription = null, tint = GoldWarm, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("कलीसिया डायनामिक TV/प्रोजेक्टर कियोस्क", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("dynamic_kiosk_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A), titleContentColor = Color.White, navigationIconContentColor = Color.White)
            )
        },
        containerColor = Color(0xFF0F172A),
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Info
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = branchSettings.branchName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldWarm
                )
                Text(
                    text = activeWindow.activeService?.serviceName ?: "रविवार मुख्य आराधना सभा",
                    fontSize = 14.sp,
                    color = Color.LightGray
                )
            }

            // Central Dynamic QR Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(2.dp, GoldWarm),
                modifier = Modifier
                    .widthIn(max = 380.dp)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "हाजिरी दर्ज करने हेतु अपने ऐप से स्कैन करें",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(16.dp))

                    // QR Display
                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (qrBitmap != null) {
                            Image(
                                bitmap = qrBitmap!!.asImageBitmap(),
                                contentDescription = "Dynamic Rotating Church QR",
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            CircularProgressIndicator(color = GoldWarm)
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Progress Countdown & Time
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { currentPeriodProgress },
                            modifier = Modifier.size(24.dp),
                            color = GoldWarm,
                            strokeWidth = 3.dp,
                            trackColor = Color.DarkGray
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "नया कोड: $secondsRemaining सेकंड में रिफ्रेश होगा",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldWarm
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "🔒 एंटी-प्रॉक्सी सुरक्षा: फोटो शेयरिंग द्वारा हाजिरी अमान्य होगी।",
                        fontSize = 10.sp,
                        color = Color.LightGray,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Live Attendance Ticker & Counter
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1E293B),
                border = BorderStroke(1.dp, Color.DarkGray),
                modifier = Modifier
                    .widthIn(max = 440.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF10B981).copy(alpha = 0.2f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.People, contentDescription = null, tint = Color(0xFF34D399))
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("आज की कुल उपस्थिति", fontSize = 11.sp, color = Color.LightGray)
                            Text(
                                text = "${todayRecords.size} सदस्य",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF10B981)
                    ) {
                        Text(
                            text = "● लाइव सक्रिय",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
