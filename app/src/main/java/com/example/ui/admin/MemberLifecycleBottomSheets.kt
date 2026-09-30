package com.example.ui.admin

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.UserProfileData
import com.example.ui.theme.GoldWarm
import com.example.ui.theme.NavyPrimary
import com.example.ui.viewmodel.MainViewModel

/**
 * COMPONENT A: "नया सदस्य जोड़ें" (ADD MEMBER MODAL BOTTOM SHEET)
 * Atomic sequential SN allocation + Family Binding
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddMemberBottomSheet(
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onSuccess: (UserProfileData) -> Unit
) {
    val context = LocalContext.current
    val allProfiles by viewModel.appUserProfiles.collectAsStateWithLifecycle()
    val churchPrefixes by viewModel.churchPrefixes.collectAsStateWithLifecycle()

    var selectedMode by remember { mutableIntStateOf(0) } // 0 = New Head, 1 = Existing Family Member
    var selectedPrefix by remember { mutableStateOf("NCC") }

    // Estimate next SN preview
    val nextEstimatedCount = remember(allProfiles, selectedPrefix) {
        val matching = allProfiles.filter { it.serialNumber.startsWith(selectedPrefix, ignoreCase = true) }
        val maxNum = matching.mapNotNull {
            it.serialNumber.removePrefix(selectedPrefix).takeWhile { char -> char.isDigit() }.toLongOrNull()
        }.maxOrNull() ?: 0L
        maxNum + 1L
    }

    // Form Fields
    var fullName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var baptismStatus by remember { mutableStateOf("बपतिस्मा प्राप्त") } // "बपतिस्मा प्राप्त", "बपतिस्मा प्रतीक्षारत"
    var residentialAddress by remember { mutableStateOf("") }
    var copyAddressFromHead by remember { mutableStateOf(false) }

    // Mode 2 specific fields
    var selectedHeadProfile by remember { mutableStateOf<UserProfileData?>(null) }
    var headSearchQuery by remember { mutableStateOf("") }
    var selectedFamilyRole by remember { mutableStateOf("पत्नी") } // "पत्नी", "पुत्र", "पुत्री", "अन्य आश्रित"

    var isSubmitting by remember { mutableStateOf(false) }

    // Available Family Heads for Autocomplete
    val familyHeads = remember(allProfiles, headSearchQuery) {
        val heads = allProfiles.filter { it.familyRole == "head" || it.isFamilyHead }
        if (headSearchQuery.isBlank()) heads.take(6)
        else heads.filter {
            it.fullName.contains(headSearchQuery, ignoreCase = true) ||
                    it.displayName.contains(headSearchQuery, ignoreCase = true) ||
                    it.serialNumber.contains(headSearchQuery, ignoreCase = true) ||
                    it.phone.contains(headSearchQuery)
        }
    }

    // Copy address from head when checkbox toggled
    LaunchedEffect(copyAddressFromHead, selectedHeadProfile) {
        if (copyAddressFromHead && selectedHeadProfile != null) {
            residentialAddress = selectedHeadProfile!!.city.ifBlank { selectedHeadProfile!!.location }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp)
                .imePadding()
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "नया सदस्य पंजीकृत करें",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "कलीसिया समुदाय सदस्यता व परिवार लिंकेज",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(Modifier.height(12.dp))

            // Dynamic Sequential SN Preview Badge
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = GoldWarm.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, GoldWarm.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ConfirmationNumber, contentDescription = null, tint = GoldWarm, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "आवंटित सदस्य आईडी:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = "$selectedPrefix$nextEstimatedCount",
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        color = GoldWarm
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Mode Selector Segmented Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedMode == 0,
                    onClick = { selectedMode = 0 },
                    label = {
                        Text(
                            text = "👨‍👩‍👧 नया परिवार प्रमुख",
                            fontSize = 12.sp,
                            fontWeight = if (selectedMode == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GoldWarm.copy(alpha = 0.25f),
                        selectedLabelColor = GoldWarm
                    )
                )

                FilterChip(
                    selected = selectedMode == 1,
                    onClick = { selectedMode = 1 },
                    label = {
                        Text(
                            text = "➕ मौजूदा परिवार में सदस्य",
                            fontSize = 12.sp,
                            fontWeight = if (selectedMode == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GoldWarm.copy(alpha = 0.25f),
                        selectedLabelColor = GoldWarm
                    )
                )
            }

            Spacer(Modifier.height(14.dp))

            // Form Content
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Mode 2: Existing Family Head Autocomplete Picker
                if (selectedMode == 1) {
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("परिवार मुखिया चुनें (Family Head) *", fontSize = 12.sp, fontWeight = FontWeight.Bold)

                                if (selectedHeadProfile != null) {
                                    val head = selectedHeadProfile!!
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = GoldWarm.copy(alpha = 0.18f),
                                        border = BorderStroke(1.dp, GoldWarm.copy(alpha = 0.6f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text(
                                                    text = "✓ चयनित मुखिया: ${head.fullName} (${head.serialNumber})",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "परिवार ID: ${head.serialNumber}-F",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            IconButton(onClick = { selectedHeadProfile = null }, modifier = Modifier.size(24.dp)) {
                                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                } else {
                                    OutlinedTextField(
                                        value = headSearchQuery,
                                        onValueChange = { headSearchQuery = it },
                                        placeholder = { Text("मुखिया का नाम या SN खोजें (उदा. NCC12, विनय)...", fontSize = 11.sp) },
                                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    )

                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        familyHeads.forEach { head ->
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.surface,
                                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        selectedHeadProfile = head
                                                        headSearchQuery = ""
                                                    }
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(head.fullName, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                                    Text(head.serialNumber, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = GoldWarm)
                                                }
                                            }
                                        }
                                    }
                                }

                                // Relationship ChoiceChips inside FlowRow/Wrap
                                Text("मुखिया से संबंध (Relationship):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    val roles = listOf("पत्नी (Spouse)", "पुत्र (Son)", "पुत्री (Daughter)", "अन्य आश्रित (Dependent)")
                                    val roleKeys = listOf("spouse", "son", "daughter", "dependent")
                                    roles.forEachIndexed { idx, label ->
                                        val key = roleKeys[idx]
                                        FilterChip(
                                            selected = selectedFamilyRole == key,
                                            onClick = { selectedFamilyRole = key },
                                            label = { Text(label, fontSize = 11.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = GoldWarm.copy(alpha = 0.25f),
                                                selectedLabelColor = GoldWarm
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Full Name
                item {
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("पूरा नाम (Full Name) *") },
                        placeholder = { Text("उदा. सुनील शर्मा") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // Phone
                item {
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text(if (selectedMode == 0) "मोबाइल नंबर (Mobile Number) *" else "मोबाइल नंबर (वैकल्पिक)") },
                        placeholder = { Text("10 अंकों का मोबाइल नंबर") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // Baptism Status (ChoiceChips)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("बपतिस्मा स्थिति (Baptism Status) *", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("बपतिस्मा प्राप्त", "बपतिस्मा प्रतीक्षारत").forEach { status ->
                                FilterChip(
                                    selected = baptismStatus == status,
                                    onClick = { baptismStatus = status },
                                    label = { Text(status, fontSize = 11.sp) },
                                    leadingIcon = if (baptismStatus == status) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                    } else null
                                )
                            }
                        }
                    }
                }

                // Residential Address
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(
                            value = residentialAddress,
                            onValueChange = { residentialAddress = it },
                            label = { Text("निवास पता (Residential Address)") },
                            placeholder = { Text("शहर / गांव, वार्ड, मकान नं.") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 3,
                            shape = RoundedCornerShape(10.dp)
                        )

                        if (selectedMode == 1 && selectedHeadProfile != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clickable { copyAddressFromHead = !copyAddressFromHead }
                                    .padding(vertical = 2.dp)
                            ) {
                                Checkbox(
                                    checked = copyAddressFromHead,
                                    onCheckedChange = { copyAddressFromHead = it }
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("मुखिया का पता कॉपी करें", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Submit Button
            Button(
                onClick = {
                    if (fullName.isBlank()) {
                        Toast.makeText(context, "कृपया पूरा नाम दर्ज करें", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (selectedMode == 0 && phoneNumber.isBlank()) {
                        Toast.makeText(context, "कृपया परिवार मुखिया का मोबाइल नंबर दर्ज करें", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (selectedMode == 1 && selectedHeadProfile == null) {
                        Toast.makeText(context, "कृपया परिवार मुखिया का चयन करें", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    isSubmitting = true
                    val isNewHead = (selectedMode == 0)
                    val headSn = if (isNewHead) null else selectedHeadProfile?.serialNumber
                    val finalRole = if (isNewHead) "head" else selectedFamilyRole

                    viewModel.createNewMemberWithAtomicSN(
                        prefix = selectedPrefix,
                        fullName = fullName,
                        phone = phoneNumber,
                        baptismStatus = if (baptismStatus == "बपतिस्मा प्राप्त") "baptized" else "awaiting",
                        address = residentialAddress,
                        isNewFamilyHead = isNewHead,
                        headSerialNumber = headSn,
                        familyRole = finalRole,
                        homeBranchId = "branch_ncc_01"
                    ) { success, errorMsg, createdProfile ->
                        isSubmitting = false
                        if (success && createdProfile != null) {
                            Toast.makeText(context, "सदस्य पंजीकृत हुआ! आवंटित ID: ${createdProfile.serialNumber} 🎉", Toast.LENGTH_LONG).show()
                            onSuccess(createdProfile)
                            onDismiss()
                        } else {
                            Toast.makeText(context, "पंजीकरण विफल: ${errorMsg ?: "अज्ञात त्रुटि"}", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                enabled = !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_submit_add_member"),
                colors = ButtonDefaults.buttonColors(containerColor = GoldWarm, contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("पंजीकृत हो रहा है...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("सदस्य सहेजें (Save & Mint SN)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

/**
 * COMPONENT B: "परिवार मुखिया प्रमोट व विभाजन" (FAMILY SPLIT & MIGRATION BOTTOM SHEET)
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FamilySplitBottomSheet(
    viewModel: MainViewModel,
    targetMember: UserProfileData,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val allProfiles by viewModel.appUserProfiles.collectAsStateWithLifecycle()

    val sourceFamilyId = targetMember.familyId.ifBlank { "${targetMember.serialNumber}-F" }
    val newGeneratedFamilyId = "${targetMember.serialNumber}-F"

    val isAlreadyHead = targetMember.familyRole == "head" && targetMember.isFamilyHead

    // Potential moving members: Members currently in sourceFamilyId, excluding original head and target member
    val coFamilyMembers = remember(allProfiles, sourceFamilyId, targetMember) {
        allProfiles.filter {
            it.familyId == sourceFamilyId &&
                    it.userId != targetMember.userId &&
                    it.serialNumber != targetMember.serialNumber &&
                    !it.isFamilyHead && it.familyRole != "head"
        }
    }

    // Checked members for migration: member.userId -> newRole
    val migratingMembersMap = remember { mutableStateMapOf<String, String>() }
    var isProcessing by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp)
                .imePadding()
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "परिवार विभाजन व सदस्य स्थानांतरण",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "विवाह / नया परिवार निर्माण • मल्टी-मेंबर माइग्रेशन",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(Modifier.height(10.dp))

            if (isAlreadyHead) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "ℹ️ ${targetMember.fullName} (${targetMember.serialNumber}) पहले से ही अपने परिवार ($sourceFamilyId) का मुख्य प्रशासक/मुखिया है।",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("बंद करें")
                }
                return@ModalBottomSheet
            }

            // Target Member & New Family ID Highlight
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = GoldWarm.copy(alpha = 0.15f)),
                border = BorderStroke(1.2.dp, GoldWarm.copy(alpha = 0.7f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "नया परिवार मुखिया (Promoted Head):",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "👑 ${targetMember.fullName}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = GoldWarm,
                            contentColor = Color.Black
                        ) {
                            Text(
                                text = targetMember.serialNumber,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = GoldWarm.copy(alpha = 0.3f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "मूल परिवार: $sourceFamilyId", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "नया परिवार ID: $newGeneratedFamilyId", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GoldWarm)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Co-family members migration list
            Text(
                text = "नए परिवार में स्थानांतरित करने हेतु सदस्य चुनें (${migratingMembersMap.size} चयनित):",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            if (coFamilyMembers.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "मूल परिवार में कोई अन्य अधीनस्थ सदस्य नहीं है। केवल ${targetMember.fullName} को नए परिवार मुखिया के रूप में प्रमोट किया जाएगा।",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(coFamilyMembers, key = { it.userId }) { member ->
                        val isChecked = migratingMembersMap.containsKey(member.userId)
                        val currentAssignedRole = migratingMembersMap[member.userId] ?: "spouse"

                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isChecked) GoldWarm.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(
                                if (isChecked) 1.dp else 0.5.dp,
                                if (isChecked) GoldWarm else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checked ->
                                            if (checked) migratingMembersMap[member.userId] = "spouse"
                                            else migratingMembersMap.remove(member.userId)
                                        }
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(member.fullName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("${member.serialNumber} • पूर्व पद: ${member.familyRole}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                if (isChecked) {
                                    Spacer(Modifier.height(6.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text("नए परिवार में पद:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        listOf("पत्नी" to "spouse", "पुत्र" to "son", "पुत्री" to "daughter", "आश्रित" to "dependent").forEach { (label, roleKey) ->
                                            FilterChip(
                                                selected = currentAssignedRole == roleKey,
                                                onClick = { migratingMembersMap[member.userId] = roleKey },
                                                label = { Text(label, fontSize = 10.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = GoldWarm.copy(alpha = 0.3f),
                                                    selectedLabelColor = GoldWarm
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Unchecked members note
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "ℹ️ अनचेक किए गए सदस्य मूल परिवार ($sourceFamilyId) में ही बने रहेंगे।",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }

            Spacer(Modifier.height(14.dp))

            // Atomic Split Button
            Button(
                onClick = {
                    isProcessing = true
                    viewModel.promoteToHeadAndMigrateFamily(
                        promotedMemberSerialOrId = targetMember.serialNumber,
                        migratingMembersMap = migratingMembersMap
                    ) { success, errorMsg ->
                        isProcessing = false
                        if (success) {
                            Toast.makeText(context, "नया परिवार $newGeneratedFamilyId सफलतापूर्वक विभाजित व बनाया गया! 🎉", Toast.LENGTH_LONG).show()
                            onSuccess()
                            onDismiss()
                        } else {
                            Toast.makeText(context, "विभाजन में त्रुटि: ${errorMsg ?: "अज्ञात त्रुटि"}", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                enabled = !isProcessing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_execute_family_split"),
                colors = ButtonDefaults.buttonColors(containerColor = GoldWarm, contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("विभाजन प्रक्रिया जारी...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.CallSplit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("परिवार विभाजन व नया मुखिया लागू करें", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}
