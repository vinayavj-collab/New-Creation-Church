package com.example.ui.admin

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChurchMember
import com.example.ui.theme.GoldWarm

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMembersScreen(
    members: List<ChurchMember>,
    userProfiles: List<com.example.data.model.UserProfileData> = emptyList(),
    viewModel: com.example.ui.viewmodel.MainViewModel? = null,
    onSaveMember: (ChurchMember) -> Unit,
    onDeleteMember: (String) -> Unit,
    onExportReport: () -> String,
    onRebindFamily: ((targetSerial: String, newFamilyId: String, newRole: String, remarks: String) -> Unit)? = null,
    onTransferBranch: ((targetSerial: String, newBranchId: String, remarks: String) -> Unit)? = null,
    onIssueTransferCertificate: ((targetSerial: String, dest: String, isMarriedOut: Boolean, remarks: String) -> Unit)? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    var selectedFilter by remember { mutableStateOf("ALL") }

    var showAddMemberBottomSheet by remember { mutableStateOf(false) }
    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingMember by remember { mutableStateOf<ChurchMember?>(null) }
    var memberToDelete by remember { mutableStateOf<ChurchMember?>(null) }
    var activeLifecycleMember by remember { mutableStateOf<com.example.data.model.UserProfileData?>(null) }
    var memberForFamilySplit by remember { mutableStateOf<com.example.data.model.UserProfileData?>(null) }

    val filterOptions = listOf(
        "ALL" to "सभी (${members.size})",
        "ACTIVE" to "सक्रिय (${members.count { it.status.contains("Active") || it.status.contains("सक्रिय") }})",
        "BAPTIZED" to "बपतिस्मा (${members.count { it.baptismStatus.contains("Baptized") || it.baptismStatus.contains("बपतिस्मा") }})",
        "HEAD" to "परिवार मुखिया (${members.count { it.familyRole.contains("Head") || it.familyRole.contains("मुखिया") }})"
    )

    val filteredMembers = remember(members, searchQuery, selectedFilter) {
        members.filter { member ->
            val matchesFilter = when (selectedFilter) {
                "ALL" -> true
                "ACTIVE" -> member.status.contains("Active", ignoreCase = true) || member.status.contains("सक्रिय", ignoreCase = true)
                "BAPTIZED" -> member.baptismStatus.contains("Baptized", ignoreCase = true) || member.baptismStatus.contains("बपतिस्मा", ignoreCase = true)
                "HEAD" -> member.familyRole.contains("Head", ignoreCase = true) || member.familyRole.contains("मुखिया", ignoreCase = true)
                else -> true
            }
            val matchesSearch = searchQuery.isBlank() ||
                    member.name.contains(searchQuery, ignoreCase = true) ||
                    member.phone.contains(searchQuery, ignoreCase = true) ||
                    member.familyName.contains(searchQuery, ignoreCase = true) ||
                    member.address.contains(searchQuery, ignoreCase = true)

            matchesFilter && matchesSearch
        }
    }

    val activeCount = remember(members) { members.count { it.status.contains("सक्रिय") || it.status.contains("Active") } }
    val childCount = remember(members) { members.count { it.familyRole.contains("बच्चा") || it.familyRole.contains("Child") } }
    val baptizedCount = remember(members) { members.count { it.baptismStatus.contains("बपतिस्मा") || it.baptismStatus.contains("Baptized") } }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (viewModel != null) {
                        showAddMemberBottomSheet = true
                    } else {
                        editingMember = null
                        showAddEditDialog = true
                    }
                },
                icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                text = { Text("नया सदस्य जोड़ें") },
                modifier = Modifier.testTag("add_member_fab")
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Action & Metrics Header Bar (Compact Toggle Search Mode)
            AnimatedContent(
                targetState = isSearchActive,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "MemberDirectoryHeaderSearch"
            ) { searchActive ->
                if (searchActive) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("नाम, फोन, परिवार या पता खोजें...", fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        if (searchQuery.isNotEmpty()) {
                                            searchQuery = ""
                                        } else {
                                            isSearchActive = false
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "बंद करें", modifier = Modifier.size(18.dp))
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .focusRequester(focusRequester)
                                .testTag("member_search_field"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                            )
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "कलीसिया सदस्य डायरेक्टरी",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { isSearchActive = true },
                                modifier = Modifier
                                    .size(34.dp)
                                    .testTag("toggle_member_search_button")
                            ) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = "खोजें",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    val uri = com.example.util.CsvExportHelper.exportMembersToCsv(context, members)
                                    if (uri != null) {
                                        com.example.util.CsvExportHelper.shareCsvFile(
                                            context,
                                            uri,
                                            "कलीसिया सदस्य डायरेक्टरी CSV रिपोर्ट"
                                        )
                                    } else {
                                        Toast.makeText(context, "CSV फ़ाइल बनाने में त्रुटि हुई", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .size(34.dp)
                                    .testTag("export_members_csv_button")
                            ) {
                                Icon(
                                    Icons.Default.FileDownload,
                                    contentDescription = "CSV डाउनलोड",
                                    tint = com.example.ui.theme.GoldWarm,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    val report = onExportReport()
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, "Church Member Directory")
                                        putExtra(Intent.EXTRA_TEXT, report)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "सदस्य डायरेक्टरी साझा करें"))
                                },
                                modifier = Modifier
                                    .size(34.dp)
                                    .testTag("export_members_button")
                            ) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = "Export Text",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            LaunchedEffect(isSearchActive) {
                if (isSearchActive) {
                    try {
                        focusRequester.requestFocus()
                    } catch (_: Exception) {}
                }
            }

            // Horizontal Scroll Metrics (YouTube Studio Style - Prevents Squishing on Mobile)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.widthIn(min = 115.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "${members.size}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(text = "कुल सदस्य", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.widthIn(min = 115.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "$activeCount", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1976D2))
                            Text(text = "सक्रिय सदस्य", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.widthIn(min = 115.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "$baptizedCount", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                            Text(text = "बपतिस्मा प्राप्त", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.widthIn(min = 115.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "$childCount", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF57C00))
                            Text(text = "बच्चे (Children)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Filter chips immediately below metrics (Maximizes vertical list space)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
            ) {
                items(filterOptions) { (key, label) ->
                    FilterChip(
                        selected = selectedFilter == key,
                        onClick = { selectedFilter = key },
                        label = { Text(label, fontSize = 13.sp) },
                        leadingIcon = if (selectedFilter == key) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }

            if (filteredMembers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.PeopleOutline,
                            contentDescription = null,
                            modifier = Modifier.size(60.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "कोई सदस्य नहीं मिला" else "कोई सदस्य पंजीकृत नहीं है",
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = {
                            editingMember = null
                            showAddEditDialog = true
                        }) {
                            Text("पहला सदस्य जोड़ें")
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredMembers, key = { it.id }) { member ->
                        MemberCard(
                            member = member,
                            onEdit = {
                                editingMember = member
                                showAddEditDialog = true
                            },
                            onDelete = {
                                memberToDelete = member
                            },
                            onCall = { phone ->
                                if (phone.isNotBlank()) {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                    context.startActivity(intent)
                                } else {
                                    Toast.makeText(context, "फोन नंबर उपलब्ध नहीं है", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onOpenLifecycle = {
                                val matchedProfile = userProfiles.find {
                                    it.userId == member.id ||
                                    it.serialNumber.equals(member.id, ignoreCase = true) ||
                                    it.phoneNumber == member.phone ||
                                    it.fullName.equals(member.name, ignoreCase = true) ||
                                    it.displayName.equals(member.name, ignoreCase = true)
                                } ?: com.example.data.model.UserProfileData(
                                    userId = member.id,
                                    serialNumber = if (member.id.startsWith("NCC") || member.id.startsWith("mem_")) member.id.replace("mem_", "NCC") else "NCC${(10..99).random()}",
                                    fullName = member.name,
                                    displayName = member.name,
                                    phoneNumber = member.phone,
                                    phone = member.phone,
                                    familyId = member.familyName.ifBlank { "${member.id}-F" },
                                    familyRole = if (member.familyRole.contains("Head", ignoreCase = true) || member.familyRole.contains("मुखिया", ignoreCase = true)) "head" else "member",
                                    isFamilyHead = member.familyRole.contains("Head", ignoreCase = true) || member.familyRole.contains("मुखिया", ignoreCase = true),
                                    isBaptized = member.baptismStatus.contains("Baptized", ignoreCase = true) || member.baptismStatus.contains("बपतिस्मा", ignoreCase = true),
                                    baptismStatus = member.baptismStatus.contains("Baptized", ignoreCase = true) || member.baptismStatus.contains("बपतिस्मा", ignoreCase = true),
                                    membershipStatus = if (member.status.contains("Active", ignoreCase = true) || member.status.contains("सक्रिय", ignoreCase = true)) "active" else "transferred_external"
                                )
                                activeLifecycleMember = matchedProfile
                            },
                            onOpenFamilySplit = {
                                val matchedProfile = userProfiles.find {
                                    it.userId == member.id ||
                                    it.serialNumber.equals(member.id, ignoreCase = true) ||
                                    it.phoneNumber == member.phone ||
                                    it.fullName.equals(member.name, ignoreCase = true) ||
                                    it.displayName.equals(member.name, ignoreCase = true)
                                } ?: com.example.data.model.UserProfileData(
                                    userId = member.id,
                                    serialNumber = if (member.id.startsWith("NCC") || member.id.startsWith("mem_")) member.id.replace("mem_", "NCC") else "NCC${(10..99).random()}",
                                    fullName = member.name,
                                    displayName = member.name,
                                    phoneNumber = member.phone,
                                    phone = member.phone,
                                    familyId = member.familyName.ifBlank { "${member.id}-F" },
                                    familyRole = if (member.familyRole.contains("Head", ignoreCase = true) || member.familyRole.contains("मुखिया", ignoreCase = true)) "head" else "member",
                                    isFamilyHead = member.familyRole.contains("Head", ignoreCase = true) || member.familyRole.contains("मुखिया", ignoreCase = true)
                                )
                                memberForFamilySplit = matchedProfile
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddMemberBottomSheet && viewModel != null) {
        AddMemberBottomSheet(
            viewModel = viewModel,
            onDismiss = { showAddMemberBottomSheet = false },
            onSuccess = {
                showAddMemberBottomSheet = false
            }
        )
    }

    if (memberForFamilySplit != null && viewModel != null) {
        FamilySplitBottomSheet(
            viewModel = viewModel,
            targetMember = memberForFamilySplit!!,
            onDismiss = { memberForFamilySplit = null },
            onSuccess = {
                memberForFamilySplit = null
            }
        )
    }

    if (showAddEditDialog) {
        AddEditMemberDialog(
            initialMember = editingMember,
            onDismiss = { showAddEditDialog = false },
            onSave = { savedMember ->
                onSaveMember(savedMember)
                showAddEditDialog = false
            }
        )
    }

    if (memberToDelete != null) {
        AlertDialog(
            onDismissRequest = { memberToDelete = null },
            title = { Text("सदस्य हटाएं?") },
            text = { Text("क्या आप सचमुच '${memberToDelete?.name}' का रिकॉर्ड हटाना चाहते हैं?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        memberToDelete?.let { onDeleteMember(it.id) }
                        memberToDelete = null
                    }
                ) {
                    Text("हटाएं", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { memberToDelete = null }) {
                    Text("रद्द करें")
                }
            }
        )
    }

    if (activeLifecycleMember != null) {
        val target = activeLifecycleMember!!
        MemberLifecycleBottomSheet(
            member = target,
            allProfiles = userProfiles,
            onRebindFamily = { newFamilyId, newRole, remarks ->
                val sn = target.serialNumber.ifBlank { target.userId }
                onRebindFamily?.invoke(sn, newFamilyId, newRole, remarks)
                Toast.makeText(context, "परिवार लिंकेज संपन्न: $newFamilyId", Toast.LENGTH_SHORT).show()
                activeLifecycleMember = null
            },
            onTransferBranch = { newBranchId, remarks ->
                val sn = target.serialNumber.ifBlank { target.userId }
                onTransferBranch?.invoke(sn, newBranchId, remarks)
                Toast.makeText(context, "कलीसिया शाखा स्थानांतरित", Toast.LENGTH_SHORT).show()
                activeLifecycleMember = null
            },
            onIssueTransferCertificate = { dest, isMarriedOut, remarks ->
                val sn = target.serialNumber.ifBlank { target.userId }
                onIssueTransferCertificate?.invoke(sn, dest, isMarriedOut, remarks)
                activeLifecycleMember = null
            },
            onDismiss = { activeLifecycleMember = null }
        )
    }
}

@Composable
fun MemberCard(
    member: ChurchMember,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCall: (String) -> Unit,
    onOpenLifecycle: (() -> Unit)? = null,
    onOpenFamilySplit: (() -> Unit)? = null
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = member.name.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = member.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (member.familyName.isNotBlank()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "(${member.familyName})",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (member.phone.isNotBlank()) {
                        Text(
                            text = "📞 ${member.phone}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Badges row
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                SuggestionChip(
                    onClick = {},
                    label = { Text(member.familyRole, fontSize = 11.sp) }
                )
                SuggestionChip(
                    onClick = {},
                    label = { Text(member.baptismStatus, fontSize = 11.sp) }
                )
                if (member.status.isNotBlank()) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text(member.status, fontSize = 11.sp) }
                    )
                }
            }

            if (member.address.isNotBlank() || member.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                if (member.address.isNotBlank()) {
                    Text(
                        text = "📍 ${member.address}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (member.notes.isNotBlank()) {
                    Text(
                        text = "📝 ${member.notes}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }

            if (member.phone.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = { onCall(member.phone) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("कॉल करें", fontSize = 12.sp)
                    }
                }
            }

            if (onOpenLifecycle != null || onOpenFamilySplit != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (onOpenLifecycle != null) {
                        OutlinedButton(
                            onClick = onOpenLifecycle,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_member_lifecycle_${member.id}")
                        ) {
                            Icon(
                                Icons.Default.SyncAlt,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "🔄 लिंकेज / TC",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (onOpenFamilySplit != null && !member.familyRole.contains("Head", ignoreCase = true) && !member.familyRole.contains("मुखिया", ignoreCase = true)) {
                        OutlinedButton(
                            onClick = onOpenFamilySplit,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            border = BorderStroke(1.dp, GoldWarm.copy(alpha = 0.7f)),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_family_split_${member.id}")
                        ) {
                            Icon(
                                Icons.Default.CallSplit,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = GoldWarm
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "👑 परिवार विभाजन",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldWarm
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditMemberDialog(
    initialMember: ChurchMember?,
    onDismiss: () -> Unit,
    onSave: (ChurchMember) -> Unit
) {
    var name by remember { mutableStateOf(initialMember?.name ?: "") }
    var phone by remember { mutableStateOf(initialMember?.phone ?: "") }
    var email by remember { mutableStateOf(initialMember?.email ?: "") }
    var address by remember { mutableStateOf(initialMember?.address ?: "") }
    var familyName by remember { mutableStateOf(initialMember?.familyName ?: "") }
    var familyRole by remember { mutableStateOf(initialMember?.familyRole ?: "मुखिया (Head)") }
    var baptismStatus by remember { mutableStateOf(initialMember?.baptismStatus ?: "बपतिस्मा प्राप्त (Baptized)") }
    var birthDate by remember { mutableStateOf(initialMember?.birthDate ?: "") }
    var anniversaryDate by remember { mutableStateOf(initialMember?.anniversaryDate ?: "") }
    var status by remember { mutableStateOf(initialMember?.status ?: "सक्रिय (Active)") }
    var notes by remember { mutableStateOf(initialMember?.notes ?: "") }

    val roleOptions = listOf("मुखिया (Head)", "पत्नी (Spouse)", "पुत्र (Son)", "पुत्री (Daughter)", "वरिष्ठ (Elder)", "युवा (Youth)")
    val baptismOptions = listOf("बपतिस्मा प्राप्त (Baptized)", "बपतिस्मा प्रतीक्षारत (Candidate)", "अतिथि (Visitor)")
    val statusOptions = listOf("सक्रिय (Active)", "अनियमित (Irregular)", "स्थानांतरित (Relocated)")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initialMember == null) "नया सदस्य पंजीकृत करें" else "सदस्य विवरण संपादित करें")
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("पूरा नाम *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("मोबाइल नंबर") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = familyName,
                        onValueChange = { familyName = it },
                        label = { Text("परिवार का नाम / उपनाम") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Text("परिवार में पद:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(roleOptions) { r ->
                            FilterChip(
                                selected = familyRole == r,
                                onClick = { familyRole = r },
                                label = { Text(r, fontSize = 12.sp) }
                            )
                        }
                    }
                }
                item {
                    Text("बपतिस्मा स्थिति:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(baptismOptions) { b ->
                            FilterChip(
                                selected = baptismStatus == b,
                                onClick = { baptismStatus = b },
                                label = { Text(b, fontSize = 12.sp) }
                            )
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("निवास पता") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = birthDate,
                            onValueChange = { birthDate = it },
                            label = { Text("जन्म तिथि") },
                            placeholder = { Text("DD/MM/YYYY") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = anniversaryDate,
                            onValueChange = { anniversaryDate = it },
                            label = { Text("विवाह वर्षगांठ") },
                            placeholder = { Text("DD/MM/YYYY") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
                item {
                    Text("सदस्य स्थिति:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(statusOptions) { s ->
                            FilterChip(
                                selected = status == s,
                                onClick = { status = s },
                                label = { Text(s, fontSize = 12.sp) }
                            )
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("विशेष टिप्पणी / प्रार्थना अनुरोध") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val member = (initialMember ?: ChurchMember()).copy(
                            name = name.trim(),
                            phone = phone.trim(),
                            email = email.trim(),
                            address = address.trim(),
                            familyName = familyName.trim(),
                            familyRole = familyRole,
                            baptismStatus = baptismStatus,
                            birthDate = birthDate.trim(),
                            anniversaryDate = anniversaryDate.trim(),
                            status = status,
                            notes = notes.trim()
                        )
                        onSave(member)
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("सहेजें (Save Member)", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("रद्द करें")
            }
        }
    )
}
