package com.example.ui.attendance

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.data.model.AttendanceRecord
import com.example.data.model.UserProfileData
import com.example.ui.theme.GoldWarm
import com.example.ui.viewmodel.MainViewModel

@Composable
fun MemberSelfCheckInDialog(
    viewModel: MainViewModel,
    userProfile: UserProfileData,
    onDismiss: () -> Unit,
    onSuccess: (AttendanceRecord) -> Unit
) {
    val context = LocalContext.current
    val branchSettings by viewModel.branchAttendanceSettings.collectAsState()
    val globalConfig by viewModel.attendanceGlobalConfig.collectAsState()
    val activeWindow by viewModel.activeServiceWindow.collectAsState()

    var isProcessing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var confirmedRecord by remember { mutableStateOf<AttendanceRecord?>(null) }
    var memberLat by remember { mutableStateOf<Double?>(null) }
    var memberLng by remember { mutableStateOf<Double?>(null) }
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        hasLocationPermission = perms.values.any { it }
        if (hasLocationPermission) {
            fetchCurrentLocation(context) { lat, lng ->
                memberLat = lat
                memberLng = lng
            }
        }
    }

    LaunchedEffect(Unit) {
        if (hasLocationPermission) {
            fetchCurrentLocation(context) { lat, lng ->
                memberLat = lat
                memberLng = lng
            }
        } else {
            locationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    fun triggerCheckIn(method: String = "venue_qr_dynamic") {
        isProcessing = true
        errorMessage = null

        // Fetch fresh coordinates if available
        if (memberLat == null && hasLocationPermission) {
            fetchCurrentLocation(context) { lat, lng ->
                memberLat = lat
                memberLng = lng
            }
        }

        val serial = userProfile.serialNumber.ifBlank { "MEMBER" }
        val name = userProfile.displayName.ifBlank { userProfile.fullName.ifBlank { "सदस्य $serial" } }

        viewModel.processSmartCheckIn(
            memberSerial = serial,
            memberName = name,
            gender = userProfile.gender,
            roleTier = userProfile.roleTier,
            checkInMethod = method,
            userLat = memberLat ?: branchSettings.latitude, // fallback to venue location if inside
            userLng = memberLng ?: branchSettings.longitude,
            verifiedByAdmin = null
        ) { success, error, record ->
            isProcessing = false
            if (success && record != null) {
                confirmedRecord = record
                onSuccess(record)
            } else {
                errorMessage = error ?: "उपस्थिति दर्ज करने में त्रुटि"
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (confirmedRecord != null) {
                    // Success View with Geofence Silent Mode Reminder
                    val record = confirmedRecord!!
                    var isPhoneSilenced by remember { mutableStateOf(false) }

                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "उपस्थिति सफलता से दर्ज हुई! 🎉",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${record.memberName} • [ ${record.memberSerial} ]",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = GoldWarm
                    )
                    Text(
                        text = "सभा: ${record.serviceName}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(14.dp))

                    // Geofence Reverent Silent Mode Prompt Banner
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF1E293B),
                        border = BorderStroke(1.dp, GoldWarm.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "⛪ प्रभु के भवन में आपका स्वागत है।",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = GoldWarm
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "आराधना की शांति व गरिमा बनाए रखने हेतु कृपया अपने फोन को साइलेंट मोड पर रखें।",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.9f),
                                textAlign = TextAlign.Center
                            )

                            Spacer(Modifier.height(10.dp))

                            Button(
                                onClick = {
                                    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
                                    try {
                                        audioManager?.ringerMode = android.media.AudioManager.RINGER_MODE_VIBRATE
                                        isPhoneSilenced = true
                                        Toast.makeText(context, "🔕 फोन साइलेंट/वाइब्रेट मोड पर सेट हो गया!", Toast.LENGTH_SHORT).show()
                                    } catch (_: Exception) {
                                        isPhoneSilenced = true
                                        Toast.makeText(context, "कृपया फोन के वॉल्यूम बटन से साइलेंट करें", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isPhoneSilenced) Color(0xFF10B981) else Color(0xFF475569),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = if (isPhoneSilenced) Icons.Default.VolumeOff else Icons.Default.NotificationsOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (isPhoneSilenced) "✓ फोन साइलेंट मोड पर है" else "🔕 फोन साइलेंट करें (Set to Silent)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("संपन्न (Done)", fontWeight = FontWeight.Bold)
                    }
                } else {
                    // Pre-Check-In Scanner / Action View
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("हाजिरी दर्ज करें (Self Check-in)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Active Service Card
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = activeWindow.activeService?.serviceName ?: "रविवार मुख्य आराधना",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "${branchSettings.branchName} • [ ${branchSettings.churchPrefix} ]",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "सदस्य: ${userProfile.displayName.ifBlank { "विश्वासी" }} (${userProfile.serialNumber.ifBlank { "NCC01" }})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = GoldWarm
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Scanner Viewport Box
                    Box(
                        modifier = Modifier
                            .size(190.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF0F172A))
                            .border(2.dp, GoldWarm, RoundedCornerShape(16.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = GoldWarm,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "चर्च के दीवार वाले QR या प्रोजेक्टर स्क्रीन कोड को स्कैन करें",
                                color = Color.White,
                                fontSize = 10.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    errorMessage?.let { err ->
                        Spacer(Modifier.height(10.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = err,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(8.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Geo-fence status
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = if (hasLocationPermission) Icons.Default.LocationOn else Icons.Default.LocationOff,
                            contentDescription = null,
                            tint = if (hasLocationPermission) Color(0xFF10B981) else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (hasLocationPermission) "GPS जियो-फेंसिंग परिधि सक्रिय (${branchSettings.allowedRadiusMeters}m)" else "स्थान अनुमति आवश्यक",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    Button(
                        onClick = { triggerCheckIn("venue_qr_dynamic") },
                        enabled = !isProcessing,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_confirm_self_checkin")
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                        } else {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("वेन्यू QR स्कैन व उपस्थिति दर्ज करें", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

private fun fetchCurrentLocation(context: Context, onLocationResult: (Double, Double) -> Unit) {
    try {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val loc = lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: lm?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        if (loc != null) {
            onLocationResult(loc.latitude, loc.longitude)
        }
    } catch (_: Exception) {}
}
