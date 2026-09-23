package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.UserProfileData
import com.example.ui.theme.GoldWarm
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay

enum class ActivationStep {
    ENTER_SERIAL_AND_P1,
    ENTER_P2_OTP,
    COMPLETE_PROFILE_FORM,
    PASSWORD_RESET_FLOW
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UserSelfServiceActivationDialog(
    viewModel: MainViewModel,
    initialMode: String = "activation", // "activation" or "forgot_password"
    onDismiss: () -> Unit,
    onActivationComplete: (UserProfileData) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var currentStep by remember {
        mutableStateOf(if (initialMode == "forgot_password") ActivationStep.PASSWORD_RESET_FLOW else ActivationStep.ENTER_SERIAL_AND_P1)
    }

    // Step A & Reset inputs
    var serialInput by remember { mutableStateOf("") }
    var p1PasswordInput by remember { mutableStateOf("") }
    var confirmP1Input by remember { mutableStateOf("") }
    var isP1Visible by remember { mutableStateOf(false) }

    // Step B inputs
    var p2OtpInput by remember { mutableStateOf("") }
    var isOtpRequested by remember { mutableStateOf(false) }
    var otpRequestTime by remember { mutableLongStateOf(0L) }
    var remainingSeconds by remember { mutableLongStateOf(600L) }

    // Step C: Self-Service Profile Demographics
    var activatedProfile by remember { mutableStateOf<UserProfileData?>(null) }
    var fullNameInput by remember { mutableStateOf("") }
    var genderInput by remember { mutableStateOf("Male") }
    var dobInput by remember { mutableStateOf("1998-05-15") }
    var phoneInput by remember { mutableStateOf("") }
    var locationInput by remember { mutableStateOf("") }
    var congregationInput by remember { mutableStateOf("") }
    var isBaptizedInput by remember { mutableStateOf(false) }
    var baptismDateInput by remember { mutableStateOf("") }
    var faithStatusInput by remember { mutableStateOf("Regular Believer") }
    val selectedInterests = remember { mutableStateListOf<String>() }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showQrScanner by remember { mutableStateOf(false) }

    // Live countdown timer for P2 OTP (10 mins = 600s)
    LaunchedEffect(otpRequestTime) {
        if (otpRequestTime > 0L) {
            while (true) {
                val elapsed = (System.currentTimeMillis() - otpRequestTime) / 1000L
                val rem = 600L - elapsed
                remainingSeconds = if (rem > 0L) rem else 0L
                delay(1000L)
            }
        }
    }

    // Calculate age from DOB
    val calculatedAge = remember(dobInput) {
        try {
            val parts = dobInput.split("-", "/")
            val year = parts.firstOrNull { it.length == 4 }?.toIntOrNull() ?: 1998
            val curYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
            (curYear - year).coerceAtLeast(0)
        } catch (_: Exception) { 26 }
    }

    val calculatedBracket = remember(calculatedAge) {
        when {
            calculatedAge < 13 -> "Kids (0-12)"
            calculatedAge in 13..25 -> "Youth (13-25)"
            calculatedAge in 26..59 -> "Adults (26-59)"
            else -> "Seniors (60+)"
        }
    }

    Dialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            border = BorderStroke(1.dp, GoldWarm.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .wrapContentHeight()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Icon & Title
                Surface(
                    shape = CircleShape,
                    color = GoldWarm.copy(alpha = 0.15f),
                    border = BorderStroke(1.5.dp, GoldWarm),
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = when (currentStep) {
                                ActivationStep.ENTER_SERIAL_AND_P1 -> Icons.Default.Badge
                                ActivationStep.ENTER_P2_OTP -> Icons.Default.Key
                                ActivationStep.COMPLETE_PROFILE_FORM -> Icons.Default.AssignmentInd
                                ActivationStep.PASSWORD_RESET_FLOW -> Icons.Default.LockReset
                            },
                            contentDescription = null,
                            tint = GoldWarm,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                Text(
                    text = when (currentStep) {
                        ActivationStep.ENTER_SERIAL_AND_P1 -> "नया सदस्य एक्टिवेशन (First-Time Activation)"
                        ActivationStep.ENTER_P2_OTP -> "P2 सुरक्षा पुष्टिकरण (Dual Authentication)"
                        ActivationStep.COMPLETE_PROFILE_FORM -> "सदस्य प्रोफ़ाइल फॉर्म (Self-Service Form)"
                        ActivationStep.PASSWORD_RESET_FLOW -> "P1 पासवर्ड रिकवरी (Forgot Password)"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(14.dp))

                // STEP 1: Enter Serial & Choose P1 Password
                if (currentStep == ActivationStep.ENTER_SERIAL_AND_P1) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = GoldWarm.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, GoldWarm.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showQrScanner = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = GoldWarm, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "📷 QR कोड स्कैन करें (Scan Serial QR)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = "कलीसिया/डायोसिस द्वारा जारी सदस्यता सीरियल आईडी (जैसे NCC1, DIO5) व अपना निजी P1 पासवर्ड दर्ज करें:",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(
                        value = serialInput,
                        onValueChange = { serialInput = it.uppercase().trim(); errorMessage = null },
                        label = { Text("सदस्यता सीरियल आईडी (Serial ID) *") },
                        placeholder = { Text("उदा. NCC1 / DIO5") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null, tint = GoldWarm) },
                        trailingIcon = {
                            IconButton(onClick = { showQrScanner = true }) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = "QR स्कैन करें", tint = GoldWarm)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("activation_serial_input")
                    )

                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(
                        value = p1PasswordInput,
                        onValueChange = { p1PasswordInput = it; errorMessage = null },
                        label = { Text("अपना निजी P1 पासवर्ड बनाएं *") },
                        placeholder = { Text("न्यूनतम 4 अक्षर/अंक") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = if (isP1Visible) VisualTransformation.None else PasswordVisualTransformation(),
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        trailingIcon = {
                            IconButton(onClick = { isP1Visible = !isP1Visible }) {
                                Icon(if (isP1Visible) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(
                        value = confirmP1Input,
                        onValueChange = { confirmP1Input = it; errorMessage = null },
                        label = { Text("P1 पासवर्ड की पुनः पुष्टि करें *") },
                        placeholder = { Text("समान पासवर्ड दोबारा दर्ज करें") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = if (isP1Visible) VisualTransformation.None else PasswordVisualTransformation(),
                        leadingIcon = { Icon(Icons.Default.LockClock, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (errorMessage != null) {
                        Spacer(Modifier.height(6.dp))
                        Text(errorMessage ?: "", color = MaterialTheme.colorScheme.error, fontSize = 11.sp, textAlign = TextAlign.Center)
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("रद्द करें", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                if (serialInput.isBlank()) {
                                    errorMessage = "कृपया अपना सीरियल नंबर दर्ज करें।"
                                    return@Button
                                }
                                if (p1PasswordInput.length < 4) {
                                    errorMessage = "P1 पासवर्ड न्यूनतम 4 अक्षरों का होना चाहिए।"
                                    return@Button
                                }
                                if (p1PasswordInput != confirmP1Input) {
                                    errorMessage = "पासवर्ड मेल नहीं खा रहे हैं।"
                                    return@Button
                                }

                                isLoading = true
                                errorMessage = null
                                viewModel.requestLiveP2ForSerial(serialInput) { success, sessionId, err ->
                                    isLoading = false
                                    if (success) {
                                        isOtpRequested = true
                                        otpRequestTime = System.currentTimeMillis()
                                        currentStep = ActivationStep.ENTER_P2_OTP
                                        Toast.makeText(context, "10-मिनट का P2 OTP आपकी हाइयर ऑथोरिटी को भेज दिया गया है।", Toast.LENGTH_LONG).show()
                                    } else {
                                        errorMessage = err ?: "P2 OTP जनरेट नहीं हो सका। कृपया जांचें।"
                                    }
                                }
                            },
                            enabled = !isLoading,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1.4f)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("पी-2 जनरेट करें", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { currentStep = ActivationStep.PASSWORD_RESET_FLOW }) {
                        Text("P1 पासवर्ड भूल गए? (Password Reset)", fontSize = 11.sp, color = GoldWarm)
                    }
                }

                // STEP 2: Enter P2 OTP (Dual Authentication)
                else if (currentStep == ActivationStep.ENTER_P2_OTP) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "🆔 सदस्यता आईडी: $serialInput",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "आपकी हायर अथॉरिटी की स्क्रीन पर 6-अंकीय OTP प्रदर्शित हो रहा है। उनसे OTP लेकर यहां दर्ज करें:",
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "⏳ OTP वैधता: %02d:%02d शेष".format(remainingSeconds / 60, remainingSeconds % 60),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (remainingSeconds > 0) Color(0xFF10B981) else MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = p2OtpInput,
                        onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) { p2OtpInput = it; errorMessage = null } },
                        label = { Text("6-अंकीय P2 OTP *") },
                        placeholder = { Text("उदा. 123456") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null, tint = GoldWarm) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("activation_p2_input")
                    )

                    if (errorMessage != null) {
                        Spacer(Modifier.height(6.dp))
                        Text(errorMessage ?: "", color = MaterialTheme.colorScheme.error, fontSize = 11.sp, textAlign = TextAlign.Center)
                    }

                    Spacer(Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { currentStep = ActivationStep.ENTER_SERIAL_AND_P1 },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("पीछे", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                if (p2OtpInput.length != 6) {
                                    errorMessage = "कृपया पूरा 6-अंकीय OTP दर्ज करें।"
                                    return@Button
                                }
                                isLoading = true
                                errorMessage = null
                                viewModel.activateUserSelfService(serialInput, p1PasswordInput, p2OtpInput) { success, profile, err ->
                                    isLoading = false
                                    if (success && profile != null) {
                                        activatedProfile = profile
                                        fullNameInput = profile.fullName.ifBlank { profile.displayName }
                                        phoneInput = profile.phoneNumber.ifBlank { profile.phone }
                                        locationInput = profile.city.ifBlank { profile.location }
                                        congregationInput = profile.congregationName.ifBlank { profile.churchName }
                                        isBaptizedInput = profile.isBaptized || profile.baptismStatus
                                        baptismDateInput = profile.baptismDate
                                        currentStep = ActivationStep.COMPLETE_PROFILE_FORM
                                        Toast.makeText(context, "P1 व P2 सत्यापन सफल! अब अपना प्रोफ़ाइल विवरण भरें।", Toast.LENGTH_SHORT).show()
                                    } else {
                                        errorMessage = err ?: "P2 OTP अमान्य या समाप्त हो चुका है।"
                                    }
                                }
                            },
                            enabled = !isLoading,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1.4f)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("सत्यापित करें", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // STEP 3: Mandatory User-Driven Self-Service Profile Form
                else if (currentStep == ActivationStep.COMPLETE_PROFILE_FORM) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 480.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = GoldWarm.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, GoldWarm.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Verified, contentDescription = null, tint = GoldWarm, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text("सक्रिय सदस्यता आईडी: $serialInput", fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                        Text("यह विवरण कलीसिया एनालिटिक्स व सदस्य डायरेक्टरी में स्वतः सिंक हो जाएगा।", fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }

                        // Section 1: व्यक्तिगत जानकारी (Personal)
                        item {
                            Text("1. व्यक्तिगत विवरण (Personal Details) *", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(4.dp))
                            OutlinedTextField(
                                value = fullNameInput,
                                onValueChange = { fullNameInput = it },
                                label = { Text("पूरा नाम (Full Name) *") },
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(6.dp))
                            OutlinedTextField(
                                value = phoneInput,
                                onValueChange = { phoneInput = it },
                                label = { Text("मोबाइल नंबर (Phone) *") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Gender & DOB Row
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("लिंग (Gender):", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        listOf("Male", "Female").forEach { g ->
                                            FilterChip(
                                                selected = genderInput == g,
                                                onClick = { genderInput = g },
                                                label = { Text(if (g == "Male") "पुरुष" else "महिला", fontSize = 10.sp) }
                                            )
                                        }
                                    }
                                }

                                Column(modifier = Modifier.weight(1.2f)) {
                                    Text("जन्मतिथि (DOB):", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                    OutlinedTextField(
                                        value = dobInput,
                                        onValueChange = { dobInput = it },
                                        placeholder = { Text("YYYY-MM-DD") },
                                        singleLine = true,
                                        supportingText = { Text("आयु: $calculatedAge वर्ष ($calculatedBracket)", fontSize = 9.sp) },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        // Section 2: आत्मिक विवरण (Spiritual)
                        item {
                            Spacer(Modifier.height(4.dp))
                            Text("2. आत्मिक विवरण (Spiritual Journey) *", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(4.dp))
                            OutlinedTextField(
                                value = congregationInput,
                                onValueChange = { congregationInput = it },
                                label = { Text("कलीसिया / गांव का नाम (Congregation/Village)") },
                                placeholder = { Text("उदा. सेंट थॉमस चर्च / खैराबाद") },
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Default.Church, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(6.dp))
                            OutlinedTextField(
                                value = locationInput,
                                onValueChange = { locationInput = it },
                                label = { Text("शहर / जिला / राज्य (Location)") },
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Baptism Checkbox
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isBaptizedInput = !isBaptizedInput }
                                    .padding(vertical = 4.dp)
                            ) {
                                Checkbox(checked = isBaptizedInput, onCheckedChange = { isBaptizedInput = it })
                                Spacer(Modifier.width(6.dp))
                                Text("क्या आपने बपतिस्मा लिया है? (Is Baptized)", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                            }
                            if (isBaptizedInput) {
                                OutlinedTextField(
                                    value = baptismDateInput,
                                    onValueChange = { baptismDateInput = it },
                                    label = { Text("बपतिस्मा की तारीख / वर्ष (Baptism Date)") },
                                    placeholder = { Text("उदा. 2021-12-25") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        // Section 3: सेवकाई रुचियां (Ministry Interest Tags)
                        item {
                            Spacer(Modifier.height(4.dp))
                            Text("3. सेवकाई में रुचि (Ministry Interest Tags):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(4.dp))
                            val availableTags = listOf("Worship & Choir", "Youth Ministry", "Prayer Intercession", "Media & Tech", "Sunday School", "Hospitality")
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                availableTags.forEach { tag ->
                                    val isSelected = selectedInterests.contains(tag)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            if (isSelected) selectedInterests.remove(tag) else selectedInterests.add(tag)
                                        },
                                        label = { Text(tag, fontSize = 10.sp) },
                                        leadingIcon = if (isSelected) {
                                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp)) }
                                        } else null
                                    )
                                }
                            }
                        }

                        if (errorMessage != null) {
                            item {
                                Text(errorMessage ?: "", color = MaterialTheme.colorScheme.error, fontSize = 11.sp, textAlign = TextAlign.Center)
                            }
                        }

                        item {
                            Spacer(Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    if (fullNameInput.trim().length < 2) {
                                        errorMessage = "कृपया अपना पूरा नाम लिखें।"
                                        return@Button
                                    }
                                    isLoading = true
                                    val finalProfile = (activatedProfile ?: UserProfileData()).copy(
                                        serialNumber = serialInput,
                                        fullName = fullNameInput.trim(),
                                        displayName = fullNameInput.trim(),
                                        phoneNumber = phoneInput.trim(),
                                        phone = phoneInput.trim(),
                                        gender = genderInput,
                                        dateOfBirth = dobInput.trim(),
                                        city = locationInput.trim(),
                                        location = locationInput.trim(),
                                        congregationName = congregationInput.trim(),
                                        churchName = congregationInput.trim(),
                                        isBaptized = isBaptizedInput,
                                        baptismStatus = isBaptizedInput,
                                        baptismDate = baptismDateInput.trim(),
                                        faithStatus = faithStatusInput,
                                        interests = selectedInterests.toList(),
                                        ministryInterest = selectedInterests.joinToString(", "),
                                        isVerifiedVishwasi = true,
                                        isVerified = true,
                                        status = "active"
                                    )

                                    viewModel.submitSelfServiceProfile(finalProfile) { success, _ ->
                                        isLoading = false
                                        if (success) {
                                            viewModel.updateUserProfile(finalProfile)
                                            Toast.makeText(context, "प्रोफ़ाइल सुरक्षित हो गई! आपका स्वागत है। 🙏", Toast.LENGTH_LONG).show()
                                            onActivationComplete(finalProfile)
                                            onDismiss()
                                        }
                                    }
                                },
                                enabled = !isLoading,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("submit_self_service_profile_button")
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                } else {
                                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("प्रोफ़ाइल सुरक्षित करें व आरंभ करें", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // STEP 4: Password Reset Flow (Forgot P1 Password)
                else if (currentStep == ActivationStep.PASSWORD_RESET_FLOW) {
                    Text(
                        text = "अपना सीरियल आईडी और नया P1 पासवर्ड दर्ज करें। हायर अथॉरिटी की स्क्रीन पर रिकवरी OTP भेजा जाएगा:",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = serialInput,
                        onValueChange = { serialInput = it.uppercase().trim(); errorMessage = null },
                        label = { Text("सदस्यता सीरियल आईडी (Serial ID) *") },
                        placeholder = { Text("उदा. NCC1") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null, tint = GoldWarm) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = p1PasswordInput,
                        onValueChange = { p1PasswordInput = it; errorMessage = null },
                        label = { Text("नया P1 पासवर्ड *") },
                        placeholder = { Text("न्यूनतम 4 अक्षर") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = if (isP1Visible) VisualTransformation.None else PasswordVisualTransformation(),
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(8.dp))

                    if (!isOtpRequested) {
                        Button(
                            onClick = {
                                if (serialInput.isBlank() || p1PasswordInput.length < 4) {
                                    errorMessage = "कृपया सीरियल आईडी व न्यूनतम 4 अक्षरों का नया पासवर्ड भरें।"
                                    return@Button
                                }
                                isLoading = true
                                errorMessage = null
                                viewModel.requestPasswordResetP2(serialInput) { success, _, err ->
                                    isLoading = false
                                    if (success) {
                                        isOtpRequested = true
                                        otpRequestTime = System.currentTimeMillis()
                                        Toast.makeText(context, "रिकवरी P2 OTP हायर अथॉरिटी को भेजा गया है।", Toast.LENGTH_SHORT).show()
                                    } else {
                                        errorMessage = err ?: "OTP जनरेट नहीं हो सका।"
                                    }
                                }
                            },
                            enabled = !isLoading,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("10-मिनट का रिकवरी P2 जनरेट करें", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text(
                            text = "⏳ रिकवरी OTP वैधता: %02d:%02d शेष".format(remainingSeconds / 60, remainingSeconds % 60),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (remainingSeconds > 0) Color(0xFF10B981) else MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(6.dp))

                        OutlinedTextField(
                            value = p2OtpInput,
                            onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) p2OtpInput = it },
                            label = { Text("हायर अथॉरिटी से प्राप्त 6-अंकीय OTP *") },
                            placeholder = { Text("उदा. 123456") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = GoldWarm) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(10.dp))

                        Button(
                            onClick = {
                                if (p2OtpInput.length != 6) {
                                    errorMessage = "कृपया पूरा 6-अंकीय OTP दर्ज करें।"
                                    return@Button
                                }
                                isLoading = true
                                errorMessage = null
                                viewModel.confirmPasswordResetWithP2(serialInput, p1PasswordInput, p2OtpInput) { success, err ->
                                    isLoading = false
                                    if (success) {
                                        Toast.makeText(context, "P1 पासवर्ड सफलतापूर्वक अपडेट हो गया! ✅", Toast.LENGTH_LONG).show()
                                        onDismiss()
                                    } else {
                                        errorMessage = err ?: "OTP अमान्य है।"
                                    }
                                }
                            },
                            enabled = !isLoading,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("नया पासवर्ड सेट करें", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (errorMessage != null) {
                        Spacer(Modifier.height(6.dp))
                        Text(errorMessage ?: "", color = MaterialTheme.colorScheme.error, fontSize = 11.sp, textAlign = TextAlign.Center)
                    }

                    Spacer(Modifier.height(10.dp))
                    TextButton(onClick = { currentStep = ActivationStep.ENTER_SERIAL_AND_P1 }) {
                        Text("एक्टिवेशन स्क्रीन पर लौटें", fontSize = 11.sp)
                    }
                }
            }
        }
    }

    if (showQrScanner) {
        QrScannerDialog(
            onSerialScanned = { scannedSerial, scannedP2 ->
                serialInput = scannedSerial
                if (!scannedP2.isNullOrBlank()) {
                    p2OtpInput = scannedP2
                }
                showQrScanner = false
            },
            onDismiss = { showQrScanner = false }
        )
    }
}
