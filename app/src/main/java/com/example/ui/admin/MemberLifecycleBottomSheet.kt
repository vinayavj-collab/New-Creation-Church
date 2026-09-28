package com.example.ui.admin

import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.context.ChurchBranchInfo
import com.example.data.context.DefaultChurches
import com.example.data.model.UserProfileData
import com.example.ui.theme.GoldWarm
import com.example.util.ChurchTransferCertificatePdfHelper

enum class LifecycleTab(val titleHindi: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    FAMILY("परिवार लिंकेज (Marriage)", Icons.Default.FamilyRestroom),
    BRANCH("शाखा ट्रांसफर", Icons.Default.Church),
    CERTIFICATE("स्थानांतरण पत्र (TC)", Icons.Default.Description)
}

/**
 * Universal Member Lifecycle & Transfer Action Sheet.
 * Handles marriage family re-binding, inter-branch transfers, and external TC generation
 * while strictly guaranteeing that the member's permanent sequential serial number is never modified.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberLifecycleBottomSheet(
    member: UserProfileData,
    allProfiles: List<UserProfileData>,
    branches: List<ChurchBranchInfo> = DefaultChurches.ALL_BRANCHES,
    onRebindFamily: (newFamilyId: String, newRole: String, remarks: String) -> Unit,
    onTransferBranch: (newBranchId: String, remarks: String) -> Unit,
    onIssueTransferCertificate: (destinationChurchOrCity: String, isMarriedOut: Boolean, remarks: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(LifecycleTab.FAMILY) }

    // Family Re-bind state
    var familySearchQuery by remember { mutableStateOf("") }
    var selectedFamilyHead by remember { mutableStateOf<UserProfileData?>(null) }
    var selectedFamilyRole by remember { mutableStateOf("spouse") }
    var familyRemarks by remember { mutableStateOf("विवाह उपरांत परिवार लिंकेज") }

    // Branch Transfer state
    var selectedBranchId by remember { mutableStateOf(branches.firstOrNull { it.branchId != member.homeBranchId }?.branchId ?: "branch_ncc_khwr") }
    var branchRemarks by remember { mutableStateOf("निवास / वैवाहिक स्थानांतरण") }

    // Certificate state
    var isMarriedOut by remember { mutableStateOf(true) }
    var destinationChurchOrCity by remember { mutableStateOf("") }
    var certificateRemarks by remember { mutableStateOf("सपरिवार नवीन कलीसिया में सम्मिलित") }
    var isGeneratingPdf by remember { mutableStateOf(false) }

    // Filtered Family Head search candidates
    val candidateFamilyHeads = remember(familySearchQuery, allProfiles, member) {
        if (familySearchQuery.isBlank()) {
            allProfiles.filter { it.isFamilyHead || it.familyRole == "head" || it.gender.equals("Male", ignoreCase = true) }
                .filter { it.serialNumber != member.serialNumber }
                .take(6)
        } else {
            allProfiles.filter { it.serialNumber != member.serialNumber }
                .filter {
                    it.serialNumber.contains(familySearchQuery, ignoreCase = true) ||
                    it.fullName.contains(familySearchQuery, ignoreCase = true) ||
                    it.displayName.contains(familySearchQuery, ignoreCase = true) ||
                    it.phoneNumber.contains(familySearchQuery, ignoreCase = true)
                }
                .take(8)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
        ) {
            // Member Identity Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = member.fullName.ifBlank { member.displayName.ifBlank { "M" } }.take(1).uppercase(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = member.fullName.ifBlank { member.displayName.ifBlank { "कलीसिया सदस्य" } },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = GoldWarm.copy(alpha = 0.2f),
                                border = BorderStroke(0.5.dp, GoldWarm)
                            ) {
                                Text(
                                    text = member.serialNumber.ifBlank { "NCC-MEM" },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldWarm,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = "स्थायी SN: ${member.serialNumber} (अपरिवर्तनीय) • परिवार ID: ${member.familyId.ifBlank { "fam_${member.serialNumber}" }}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Navigation Segment Tabs
            TabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .testTag("lifecycle_tab_row")
            ) {
                LifecycleTab.values().forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = {
                            Text(
                                text = tab.titleHindi,
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        icon = { Icon(tab.icon, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // =========================================================================
            // TAB A: FAMILY RE-BINDING (MARRIAGE)
            // =========================================================================
            if (selectedTab == LifecycleTab.FAMILY) {
                Text(
                    text = "विवाह उपरांत परिवार लिंकेज (Family Re-binding)",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "सदस्य का स्थायी सीरियल नंबर (${member.serialNumber}) यथावत रहेगा। केवल पारिवारिक ID पति/ससुराल के परिवार में स्थानांतरित होगी।",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = familySearchQuery,
                    onValueChange = { familySearchQuery = it },
                    label = { Text("पति / परिवार मुखिया खोजें (SN या नाम दर्ज करें)") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("family_search_field")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Candidate List
                Text(
                    text = if (selectedFamilyHead != null) "चयनित नया परिवार मुखिया:" else "संभावित मुखिया / परिवार चुनें:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(4.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 160.dp)
                ) {
                    items(candidateFamilyHeads) { candidate ->
                        val isChosen = selectedFamilyHead?.userId == candidate.userId ||
                                (selectedFamilyHead == null && candidate.serialNumber == familySearchQuery.trim())

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isChosen) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = BorderStroke(if (isChosen) 1.2.dp else 0.5.dp, if (isChosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    selectedFamilyHead = candidate
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = if (isChosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column {
                                        Text(
                                            text = candidate.fullName.ifBlank { candidate.displayName.ifBlank { candidate.serialNumber } },
                                            fontSize = 12.5.sp,
                                            fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Medium
                                        )
                                        Text(
                                            text = "SN: ${candidate.serialNumber} • परिवार ID: ${candidate.familyId.ifBlank { "${candidate.serialNumber}-F" }}",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (isChosen) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = familyRemarks,
                    onValueChange = { familyRemarks = it },
                    label = { Text("विवाह टिप्पणी / पास्टोरल नोट") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        val targetFamilyId = selectedFamilyHead?.let {
                            it.familyId.ifBlank { "${it.serialNumber}-F" }
                        } ?: if (familySearchQuery.isNotBlank()) "${familySearchQuery.trim().uppercase()}-F" else "${member.serialNumber}-F"

                        onRebindFamily(targetFamilyId, selectedFamilyRole, familyRemarks)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("confirm_family_rebind_button")
                ) {
                    Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("परिवार लिंकेज पुष्टि करें", fontWeight = FontWeight.Bold)
                }
            }

            // =========================================================================
            // TAB B: BRANCH TRANSFER
            // =========================================================================
            if (selectedTab == LifecycleTab.BRANCH) {
                Text(
                    text = "कलीसिया शाखा स्थानांतरण (Branch Transfer)",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "सदस्य का स्थायी सीरियल नंबर व बपतिस्मा रिकॉर्ड सुरक्षित रहेगा। सुधि ट्रैकर व उपस्थिति रोस्टर नई शाखा में स्वतः री-इंडेक्स होगा।",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "गंतव्य कलीसिया शाखा चुनें:",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    branches.forEach { branch ->
                        val isSelected = selectedBranchId == branch.branchId
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = BorderStroke(if (isSelected) 1.2.dp else 0.5.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedBranchId = branch.branchId }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Church, contentDescription = null, tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                    Column {
                                        Text(branch.branchName, fontSize = 12.5.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
                                        Text("${branch.location} • ${branch.pastorInCharge}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = branchRemarks,
                    onValueChange = { branchRemarks = it },
                    label = { Text("स्थानांतरण का कारण / टिप्पणी") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        onTransferBranch(selectedBranchId, branchRemarks)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("confirm_branch_transfer_button")
                ) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("शाखा स्थानांतरण संपन्न करें", fontWeight = FontWeight.Bold)
                }
            }

            // =========================================================================
            // TAB C: TRANSFER CERTIFICATE (TC)
            // =========================================================================
            if (selectedTab == LifecycleTab.CERTIFICATE) {
                Text(
                    text = "स्थानांतरण व अनुशंसा पत्र (Transfer Certificate - TC)",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "विवाह या अन्य नगर में स्थानांतरण पर आधिकारिक चर्च अनुशंसा पत्र (PDF) जारी करें।",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = isMarriedOut,
                        onClick = { isMarriedOut = true },
                        label = { Text("विवाह उपरांत (Married Out)", fontSize = 11.sp) },
                        leadingIcon = if (isMarriedOut) { { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) } } else null,
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = !isMarriedOut,
                        onClick = { isMarriedOut = false },
                        label = { Text("बाहरी नगर (Relocated)", fontSize = 11.sp) },
                        leadingIcon = if (!isMarriedOut) { { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) } } else null,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = destinationChurchOrCity,
                    onValueChange = { destinationChurchOrCity = it },
                    label = { Text("गंतव्य कलीसिया या नगर का नाम दर्ज करें") },
                    placeholder = { Text("उदा. सेंट पॉल्स चर्च, दिल्ली / बिलासपुर") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("destination_church_field")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = certificateRemarks,
                    onValueChange = { certificateRemarks = it },
                    label = { Text("पास्टोरल टिप्पणी / अनुशंसा नोट") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        if (destinationChurchOrCity.isBlank()) {
                            Toast.makeText(context, "कृपया गंतव्य कलीसिया या नगर का नाम दर्ज करें", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isGeneratingPdf = true
                        onIssueTransferCertificate(destinationChurchOrCity, isMarriedOut, certificateRemarks)
                    },
                    enabled = !isGeneratingPdf,
                    colors = ButtonDefaults.buttonColors(containerColor = GoldWarm),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("issue_transfer_cert_button")
                ) {
                    if (isGeneratingPdf) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("📜 स्थानांतरण पत्र (TC) जारी करें व PDF साझा करें", fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            }
        }
    }
}
