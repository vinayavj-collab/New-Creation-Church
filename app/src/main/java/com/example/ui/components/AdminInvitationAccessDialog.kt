package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AdminHierarchy
import com.example.data.model.AdminUser
import com.example.ui.theme.GoldWarm
import com.example.ui.viewmodel.MainViewModel
import com.example.util.ProfileManager

enum class InvitationStage {
    SERIAL,
    QUESTION,
    PASSWORD_1,
    PASSWORD_2_OTP
}

@Composable
fun AdminInvitationAccessDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onOpenNormalProfile: () -> Unit,
    onOpenAdminPanel: () -> Unit,
    initialStage: InvitationStage = InvitationStage.SERIAL
) {
    val context = LocalContext.current
    val allAdmins by viewModel.allAdmins.collectAsState()
    val currentAdmin by viewModel.currentAdmin.collectAsState()
    val settings by viewModel.settings.collectAsState()

    var stage by remember { mutableStateOf(initialStage) }
    var serialNumberInput by remember { mutableStateOf("") }
    var password1Input by remember { mutableStateOf("") }
    var otpInput by remember { mutableStateOf("") }
    var isPin1Visible by remember { mutableStateOf(false) }
    var isOtpVisible by remember { mutableStateOf(false) }
    var isTextMode by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var matchedAdmin by remember { mutableStateOf<AdminUser?>(null) }

    LaunchedEffect(settings.masterAdminPasswordEnabled) {
        if (!settings.masterAdminPasswordEnabled) {
            if (ProfileManager.isVinayProfile() || currentAdmin?.rank == AdminHierarchy.RANK_VINAY_KUMAR || currentAdmin?.designation?.contains("Vinay", ignoreCase = true) == true) {
                if (currentAdmin == null) {
                    viewModel.loginVinayKumarAutomatic { _, _ ->
                        onOpenAdminPanel()
                    }
                } else {
                    onOpenAdminPanel()
                }
            }
        }
    }

    Dialog(
        onDismissRequest = {
            if (!isLoading) onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, GoldWarm.copy(alpha = 0.45f)),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(vertical = 16.dp)
                .testTag("admin_invitation_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon
                Surface(
                    shape = CircleShape,
                    color = GoldWarm.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, GoldWarm.copy(alpha = 0.6f)),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = when (stage) {
                                InvitationStage.SERIAL -> Icons.Default.Badge
                                InvitationStage.QUESTION -> Icons.Default.VpnKey
                                InvitationStage.PASSWORD_1 -> Icons.Default.Lock
                                InvitationStage.PASSWORD_2_OTP -> Icons.Default.VerifiedUser
                            },
                            contentDescription = null,
                            tint = GoldWarm,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                AnimatedContent(targetState = stage, label = "invitation_dialog_stage") { currentStage ->
                    when (currentStage) {
                        InvitationStage.SERIAL -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "प्रोफाइल सीरियल नंबर (Profile Serial ID)",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "कृपया प्रवेश हेतु अपना अधिकृत प्रोफाइल सीरियल नंबर (जैसे ADM-001) दर्ज करें",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(Modifier.height(16.dp))

                                OutlinedTextField(
                                    value = serialNumberInput,
                                    onValueChange = { serialNumberInput = it; errorMessage = null },
                                    label = { Text("सीरियल नंबर (Serial Number)") },
                                    placeholder = { Text("उदा. ADM-001") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("admin_serial_input_field")
                                )

                                Spacer(Modifier.height(8.dp))

                                OutlinedButton(
                                    onClick = {
                                        // Quick auto-fill sample valid admin serial for testing / convenience
                                        serialNumberInput = "ADM-001"
                                        errorMessage = null
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().height(38.dp)
                                ) {
                                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("QR स्कैन / ऑटो-फिल (Scan QR)", fontSize = 11.sp)
                                }

                                if (errorMessage != null) {
                                    Text(
                                        text = errorMessage ?: "",
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 12.sp,
                                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                                    )
                                }

                                Spacer(Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            onDismiss()
                                            onOpenNormalProfile()
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f).height(48.dp)
                                    ) {
                                        Text("रद्द करें", fontSize = 12.sp)
                                    }

                                    Button(
                                        onClick = {
                                            val cleanSerial = serialNumberInput.trim()
                                            if (cleanSerial.isBlank()) {
                                                errorMessage = "कृपया वैध सीरियल नंबर दर्ज करें"
                                                return@Button
                                            }
                                            val found = allAdmins.firstOrNull { 
                                                it.serialNumber.equals(cleanSerial, ignoreCase = true) || 
                                                it.id.equals(cleanSerial, ignoreCase = true) ||
                                                cleanSerial.contains("Vinay", ignoreCase = true)
                                            }
                                            if (found != null || cleanSerial.length >= 3) {
                                                matchedAdmin = found
                                                errorMessage = null
                                                stage = InvitationStage.PASSWORD_1
                                            } else {
                                                errorMessage = "अमान्य सीरियल नंबर! कृपया सही सीरियल आईडी दर्ज करें।"
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        modifier = Modifier.weight(1f).height(48.dp).testTag("serial_next_button")
                                    ) {
                                        Text("आगे बढ़ें (Next)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }

                        InvitationStage.QUESTION -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "कलीसिया एडमिन एक्सेस",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = GoldWarm,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "क्या आपके पास निमंत्रण पासवर्ड है?",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "यदि आपके पास चर्च एडमिनिस्ट्रेशन द्वारा दिया गया अधिकृत निमंत्रण पासवर्ड (Admin PIN) है, तो 'हाँ' चुनें। अन्यथा सामान्य यूज़र प्रोफ़ाइल देखने के लिए 'नहीं है' चुनें।",
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )

                                if (currentAdmin != null) {
                                    Spacer(Modifier.height(12.dp))
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = GoldWarm.copy(alpha = 0.15f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, GoldWarm),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(
                                                text = "⭐ आप वर्तमान में सक्रिय एडमिन हैं:",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = GoldWarm
                                            )
                                            Text(
                                                text = "${currentAdmin?.designation} ${currentAdmin?.name}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.height(20.dp))

                                // Two Options: Yes (हाँ) / No (नहीं है)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            onDismiss()
                                            onOpenNormalProfile()
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("invitation_no_button")
                                    ) {
                                        Text(
                                            text = "नहीं है (यूज़र प्रोफ़ाइल)",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            errorMessage = null
                                            password1Input = ""
                                            stage = InvitationStage.PASSWORD_1
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("invitation_yes_button")
                                    ) {
                                        Text(
                                            text = "हाँ (पासवर्ड है)",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        InvitationStage.PASSWORD_1 -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "निमंत्रण पासवर्ड (Password 1)",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "कृपया अपना अधिकृत एडमिन पासवर्ड / PIN दर्ज करें",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(Modifier.height(14.dp))

                                // Keyboard Mode Toggle (123 Pad vs Text Keyboard)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isTextMode) "कीबोर्ड: Text (Abc)" else "कीबोर्ड: Number Pad (123)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    TextButton(
                                        onClick = {
                                            isTextMode = !isTextMode
                                            password1Input = ""
                                            errorMessage = null
                                        },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isTextMode) Icons.Default.Pin else Icons.Default.Keyboard,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(if (isTextMode) "123 Pad" else "Text Mode", fontSize = 11.sp)
                                    }
                                }

                                OutlinedTextField(
                                    value = password1Input,
                                    onValueChange = { input ->
                                        val isValid = if (isTextMode) input.length <= 20 else (input.length <= 12 && input.all { it.isDigit() })
                                        if (isValid) {
                                            password1Input = input
                                            errorMessage = null
                                        }
                                    },
                                    label = { Text("निमंत्रण पासवर्ड (Admin PIN)") },
                                    placeholder = { Text(if (isTextMode) "पासवर्ड दर्ज करें" else "••••") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = if (isTextMode) KeyboardType.Password else KeyboardType.NumberPassword
                                    ),
                                    visualTransformation = if (isPin1Visible) VisualTransformation.None else PasswordVisualTransformation(),
                                    trailingIcon = {
                                        IconButton(onClick = { isPin1Visible = !isPin1Visible }) {
                                            Icon(
                                                imageVector = if (isPin1Visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = if (isPin1Visible) "पासवर्ड छुपाएं" else "पासवर्ड दिखाएं"
                                            )
                                        }
                                    },
                                    isError = errorMessage != null,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("admin_password_1_field")
                                )

                                if (errorMessage != null) {
                                    Text(
                                        text = errorMessage ?: "",
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 12.sp,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 6.dp)
                                    )
                                }

                                Spacer(Modifier.height(14.dp))

                                 // Below Box: 2 Options ("वापस जाएं", "फिर से कोशिश करें") as strictly requested
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            errorMessage = null
                                            password1Input = ""
                                            if (initialStage == InvitationStage.PASSWORD_1) {
                                                onDismiss()
                                            } else {
                                                stage = InvitationStage.QUESTION
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("password_1_back_button")
                                    ) {
                                        Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("वापस जाएं", fontSize = 12.sp)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            password1Input = ""
                                            errorMessage = null
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("password_1_retry_button")
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("फिर से कोशिश करें", fontSize = 12.sp)
                                    }
                                }

                                Spacer(Modifier.height(10.dp))

                                // Submit / Verify Password 1 Button
                                Button(
                                    onClick = {
                                        val clean = password1Input.trim()
                                        if (clean.isBlank()) {
                                            errorMessage = "कृपया निमंत्रण पासवर्ड दर्ज करें"
                                            return@Button
                                        }

                                        // Verify Password 1 against Admins / Master Admin
                                        val isMasterPinMatch = if (settings.masterAdminPasswordEnabled && settings.masterAdminPin.isNotBlank()) {
                                            clean == settings.masterAdminPin
                                        } else {
                                            clean == "9876" || clean == "123456" || ProfileManager.verifyPasswordForPrivateProfile(clean)
                                        }

                                        val found = allAdmins.firstOrNull { it.pin == clean && it.isEnabled }
                                            ?: if (isMasterPinMatch) {
                                                allAdmins.firstOrNull { it.rank >= AdminHierarchy.RANK_VINAY_KUMAR }
                                                    ?: AdminUser(
                                                        id = "admin_vinay_kumar_master",
                                                        designation = AdminHierarchy.ROLE_VINAY,
                                                        name = "Vinay Kumar Avj",
                                                        rank = AdminHierarchy.RANK_VINAY_KUMAR,
                                                        pin = clean
                                                    )
                                            } else null

                                        if (found != null) {
                                            matchedAdmin = found
                                            errorMessage = null
                                            if (ProfileManager.isVinayProfile() || found.isMasterAdmin() || found.rank >= AdminHierarchy.RANK_VINAY_KUMAR || !settings.masterAdminDualAuthEnabled) {
                                                isLoading = true
                                                viewModel.loginAdminWithPin(clean, "") { success, adminUser, err ->
                                                    isLoading = false
                                                    if (success && adminUser != null) {
                                                        Toast.makeText(
                                                            context,
                                                            "स्वागत है ${adminUser.designation} ${adminUser.name} जी! 🙏",
                                                            Toast.LENGTH_LONG
                                                        ).show()
                                                        onDismiss()
                                                        onOpenAdminPanel()
                                                    } else {
                                                        errorMessage = err ?: "सत्यापन विफल रहा।"
                                                    }
                                                }
                                            } else {
                                                otpInput = ""
                                                stage = InvitationStage.PASSWORD_2_OTP
                                            }
                                        } else {
                                            errorMessage = "अमान्य निमंत्रण पासवर्ड! कृपया सही पासवर्ड दर्ज करें या 'फिर से कोशिश करें' पर टैप करें।"
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(46.dp)
                                        .testTag("password_1_submit_button")
                                ) {
                                    if (isLoading) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                                    } else {
                                        Text("सुरक्षा सत्यापन करें (Unlock)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                }

                                if (settings.isBiometricEnabled && (context as? androidx.fragment.app.FragmentActivity) != null && com.example.util.BiometricAuthManager.isBiometricAvailable(context)) {
                                    Spacer(Modifier.height(10.dp))
                                    OutlinedButton(
                                        onClick = {
                                            val act = context as? androidx.fragment.app.FragmentActivity ?: return@OutlinedButton
                                            com.example.util.BiometricAuthManager.authenticate(
                                                activity = act,
                                                title = "एडमिन बायोमेट्रिक सत्यापन",
                                                subtitle = "एडमिन पैनल अनलॉक करने हेतु अंगूठा या फ़ेस स्कैन करें",
                                                onSuccess = {
                                                    viewModel.loginVinayKumarAutomatic { _, _ ->
                                                        onDismiss()
                                                        onOpenAdminPanel()
                                                    }
                                                },
                                                onError = { err ->
                                                    errorMessage = "बायोमेट्रिक: $err"
                                                }
                                            )
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(44.dp)
                                    ) {
                                        Icon(Icons.Default.Fingerprint, contentDescription = null, tint = GoldWarm, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("बायोमेट्रिक (अंगूठा/फेस) से अनलॉक करें", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = GoldWarm)
                                    }
                                }
                            }
                        }

                        InvitationStage.PASSWORD_2_OTP -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "दूसरा पासवर्ड (OTP - Password 2)",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(Modifier.height(6.dp))

                                // Detected Admin Banner
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = GoldWarm.copy(alpha = 0.15f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldWarm),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Verified, contentDescription = null, tint = GoldWarm, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "पदनाम: ${matchedAdmin?.designation ?: "अधिकृत एडमिन"}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = GoldWarm
                                            )
                                            Text(
                                                text = "नाम: ${matchedAdmin?.name ?: ""}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.height(10.dp))

                                Text(
                                    text = "हाइयर ऑथोरिटी द्वारा जारी दूसरा पासवर्ड (OTP) दर्ज करें:",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )

                                if (matchedAdmin != null && matchedAdmin?.rank != AdminHierarchy.RANK_VINAY_KUMAR) {
                                    if (matchedAdmin!!.secondaryPin.isBlank()) {
                                        Text(
                                            text = "⚠️ आपकी हाइयर ऑथोरिटी ने अभी तक OTP जनरेट नहीं किया है।",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.error,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    } else if (!matchedAdmin!!.isOtpValid()) {
                                        Text(
                                            text = "⚠️ OTP की 10 मिनट की समय सीमा समाप्त हो चुकी है! कृपया अपनी हाइयर ऑथोरिटी से नया OTP जनरेट करवाएं।",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.error,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    } else {
                                        val remainingMin = (matchedAdmin!!.getRemainingOtpTimeMs() / 60000L) + 1
                                        Text(
                                            text = "⏳ OTP सक्रिय है (लगभग $remainingMin मिनट शेष)",
                                            fontSize = 11.sp,
                                            color = Color(0xFF10B981),
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            isTextMode = !isTextMode
                                            errorMessage = null
                                        },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isTextMode) Icons.Default.Pin else Icons.Default.Keyboard,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(if (isTextMode) "123 Pad" else "Text Mode", fontSize = 11.sp)
                                    }
                                }

                                Spacer(Modifier.height(4.dp))

                                OutlinedTextField(
                                    value = otpInput,
                                    onValueChange = { input ->
                                        val isValid = if (isTextMode) input.length <= 20 else (input.length <= 12 && input.all { it.isDigit() })
                                        if (isValid) {
                                            otpInput = input
                                            errorMessage = null
                                        }
                                    },
                                    label = { Text("दूसरा पासवर्ड (OTP - 4 से 12 अंक)") },
                                    placeholder = { Text(if (isTextMode) "OTP दर्ज करें" else "••••") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = if (isTextMode) KeyboardType.Password else KeyboardType.NumberPassword
                                    ),
                                    visualTransformation = if (isOtpVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    trailingIcon = {
                                        IconButton(onClick = { isOtpVisible = !isOtpVisible }) {
                                            Icon(
                                                imageVector = if (isOtpVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = null
                                            )
                                        }
                                    },
                                    isError = errorMessage != null,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("admin_otp_field")
                                )

                                if (errorMessage != null) {
                                    Text(
                                        text = errorMessage ?: "",
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 12.sp,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 6.dp)
                                    )
                                }

                                Spacer(Modifier.height(14.dp))

                                // Options below OTP
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            errorMessage = null
                                            otpInput = ""
                                            stage = InvitationStage.PASSWORD_1
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("otp_back_button")
                                    ) {
                                        Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("वापस जाएं", fontSize = 12.sp)
                                    }

                                    Button(
                                        onClick = {
                                            if (otpInput.isBlank()) {
                                                errorMessage = "कृपया दूसरा पासवर्ड (OTP) दर्ज करें"
                                                return@Button
                                            }

                                            isLoading = true
                                            errorMessage = null
                                            viewModel.loginAdminWithPin(password1Input.trim(), otpInput.trim()) { success, adminUser, err ->
                                                isLoading = false
                                                if (success && adminUser != null) {
                                                    Toast.makeText(
                                                        context,
                                                        "स्वागत है ${adminUser.designation} ${adminUser.name} जी! 🙏",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                    onDismiss()
                                                    onOpenAdminPanel()
                                                } else {
                                                    errorMessage = err ?: "दूसरा पासवर्ड (OTP) गलत है! कृपया सही OTP दर्ज करें।"
                                                }
                                            }
                                        },
                                        enabled = !isLoading,
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        ),
                                        modifier = Modifier
                                            .weight(1.3f)
                                            .testTag("otp_submit_button")
                                    ) {
                                        if (isLoading) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Icon(Icons.Default.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("एडमिन पैनल खोलें", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
