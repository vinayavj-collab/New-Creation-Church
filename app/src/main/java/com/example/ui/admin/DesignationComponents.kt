package com.example.ui.admin

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.GoldWarm

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DesignationsTabContent(
    currentAdmin: AdminUser,
    allDesignations: List<DesignationAuthority>,
    onEditDesignation: (DesignationAuthority) -> Unit,
    onCreateCustomDesignation: () -> Unit,
    onDeleteCustomDesignation: (DesignationAuthority) -> Unit
) {
    val canManageDesignations = currentAdmin.rank == AdminHierarchy.RANK_VINAY_KUMAR ||
            currentAdmin.hasFunction(AdminFunction.MANAGE_DESIGNATIONS.name)

    val sortedDesignations = remember(allDesignations) {
        allDesignations.sortedByDescending { it.rank }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        // Master Authority Card: Vinay Kumar Avj (Profile B)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = GoldWarm.copy(alpha = 0.15f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, GoldWarm.copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(GoldWarm),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = Color(0xFF1E1B4B), modifier = Modifier.size(26.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = AdminHierarchy.ROLE_VINAY_KUMAR,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Default.Verified, contentDescription = null, tint = GoldWarm, modifier = Modifier.size(16.dp))
                            }
                            Text(
                                text = "सर्वोच्च मास्टर अधिकार • (सर्वव्यापी नियंत्रण)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "सभी 14 प्रशासनिक कार्य व अधिकार (All 14 Admin Functions Fully Authorized):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(6.dp))

                    // Group and display all 14 functions
                    AdminFunction.entries.groupBy { it.category }.forEach { (category, functions) ->
                        Text(
                            text = "• $category",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            functions.forEach { fn ->
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Text(
                                        text = "${fn.hindiTitle} ✅",
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section Title & Add Custom Designation Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("पदनाम अधिकार सूची (Hierarchy Roles)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("प्रत्येक पद के लिए अनुमत अधिकारों को जोड़ें या हटाएं", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                if (canManageDesignations) {
                    FilledTonalButton(
                        onClick = onCreateCustomDesignation,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("नया पदनाम", fontSize = 11.sp)
                    }
                }
            }
        }

        // List of Designations
        items(sortedDesignations, key = { it.id }) { designation ->
            DesignationCard(
                designation = designation,
                canManage = canManageDesignations && (currentAdmin.rank > designation.rank || currentAdmin.rank == AdminHierarchy.RANK_VINAY_KUMAR),
                onEditFunctions = { onEditDesignation(designation) },
                onDelete = { onDeleteCustomDesignation(designation) }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DesignationCard(
    designation: DesignationAuthority,
    canManage: Boolean,
    onEditFunctions: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            when (designation.rank) {
                                AdminHierarchy.RANK_VINAY_KUMAR -> GoldWarm
                                AdminHierarchy.RANK_BISHOP -> Color(0xFF9333EA)
                                AdminHierarchy.RANK_DEPUTY_BISHOP -> Color(0xFF3B82F6)
                                AdminHierarchy.RANK_PASTOR -> Color(0xFF10B981)
                                else -> Color(0xFF6B7280)
                            }.copy(alpha = 0.2f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "L${designation.rank}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = designation.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        if (designation.isCustom) {
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text("कस्टम पद", fontSize = 9.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                            }
                        }
                    }

                    Text(
                        text = "${designation.description} • ${designation.allowedFunctions.size} अधिकार सक्रिय",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (canManage) {
                    OutlinedButton(
                        onClick = onEditFunctions,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("अधिकार बदलें", fontSize = 11.sp)
                    }

                    if (designation.isCustom) {
                        Spacer(Modifier.width(4.dp))
                        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Display active functions chips
            if (designation.allowedFunctions.isEmpty()) {
                Text("कोई विशेष अधिकार असाइन नहीं", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    designation.allowedFunctions.forEach { fnKey ->
                        val fn = AdminFunction.fromKey(fnKey)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        ) {
                            Text(
                                text = fn?.hindiTitle ?: fnKey,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditDesignationFunctionsDialog(
    designation: DesignationAuthority,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit
) {
    val selectedFunctions = remember { mutableStateListOf<String>().apply { addAll(designation.allowedFunctions) } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "अधिकार बदलें: ${designation.name}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "इस पदनाम के लिए अनुमत प्रशासनिक कार्यों को चुनें या हटाएं",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = {
                            selectedFunctions.clear()
                            selectedFunctions.addAll(AdminFunction.entries.map { it.name })
                        }) {
                            Text("सभी चुनें", fontSize = 11.sp)
                        }

                        TextButton(onClick = {
                            selectedFunctions.clear()
                        }) {
                            Text("सभी हटाएं", fontSize = 11.sp)
                        }
                    }
                }

                AdminFunction.entries.groupBy { it.category }.forEach { (category, functions) ->
                    item {
                        Text(
                            text = category,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                        )
                    }

                    items(functions) { fn ->
                        val isChecked = selectedFunctions.contains(fn.name)
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isChecked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                else MaterialTheme.colorScheme.surface
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                0.5.dp,
                                if (isChecked) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isChecked) selectedFunctions.remove(fn.name)
                                    else selectedFunctions.add(fn.name)
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        if (checked) selectedFunctions.add(fn.name)
                                        else selectedFunctions.remove(fn.name)
                                    },
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = fn.hindiTitle,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = fn.description,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedFunctions.toList()) }) {
                Text("सुरक्षित करें")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("रद्द करें")
            }
        }
    )
}

@Composable
fun EditAdminAssignedFunctionsDialog(
    admin: AdminUser,
    creatorAdmin: AdminUser,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit
) {
    val selectedFunctions = remember { mutableStateListOf<String>().apply { addAll(admin.assignedFunctions) } }

    val delegatableFunctions = remember(creatorAdmin) {
        if (creatorAdmin.rank == AdminHierarchy.RANK_VINAY_KUMAR) {
            AdminFunction.entries.map { it.name }
        } else {
            creatorAdmin.assignedFunctions
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "अधिकार असाइन करें: ${admin.designation} ${admin.name}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = "आप अपने अधिकृत कार्यों में से इन्हें यह अधिकार दे सकते हैं",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(AdminFunction.entries.filter { delegatableFunctions.contains(it.name) }) { fn ->
                    val isChecked = selectedFunctions.contains(fn.name)
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isChecked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            else MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isChecked) selectedFunctions.remove(fn.name)
                                else selectedFunctions.add(fn.name)
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    if (checked) selectedFunctions.add(fn.name)
                                    else selectedFunctions.remove(fn.name)
                                },
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = fn.hindiTitle,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = fn.description,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedFunctions.toList()) }) {
                Text("अधिकार अपडेट करें")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("रद्द करें")
            }
        }
    )
}

@Composable
fun CreateCustomDesignationDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, rank: Int, functions: List<String>, description: String) -> Unit
) {
    var nameInput by remember { mutableStateOf("") }
    var rankInput by remember { mutableIntStateOf(AdminHierarchy.RANK_PURANIYA) }
    var descInput by remember { mutableStateOf("") }
    val selectedFunctions = remember { mutableStateListOf<String>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("नया कस्टम पदनाम बनाएं", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("पदनाम का नाम (Role Name)") },
                        placeholder = { Text("उदा. युवा अगुआ / यूथ लीडर") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Text("अधिकार श्रेणी:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(1 to "पुरनिया", 2 to "पास्टर", 3 to "उप बिशप", 4 to "बिशप").forEach { (lvl, lbl) ->
                            FilterChip(
                                selected = rankInput == lvl,
                                onClick = { rankInput = lvl },
                                label = { Text(lbl, fontSize = 10.sp) }
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = descInput,
                        onValueChange = { descInput = it },
                        label = { Text("विवरण (Description)") },
                        placeholder = { Text("इस पद के कार्य व दायित्व...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Text("शुरुआती अधिकार चुनें:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                items(AdminFunction.entries) { fn ->
                    val isChecked = selectedFunctions.contains(fn.name)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isChecked) selectedFunctions.remove(fn.name)
                                else selectedFunctions.add(fn.name)
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                if (checked) selectedFunctions.add(fn.name)
                                else selectedFunctions.remove(fn.name)
                            },
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(fn.hindiTitle, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nameInput.isNotBlank()) {
                        onConfirm(nameInput.trim(), rankInput, selectedFunctions.toList(), descInput.trim())
                    }
                }
            ) {
                Text("पदनाम बनाएं")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("रद्द करें") }
        }
    )
}
