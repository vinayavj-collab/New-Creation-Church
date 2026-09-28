package com.example.ui.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.context.ActiveBranchContextState
import com.example.data.context.ChurchBranchInfo
import com.example.data.context.DefaultChurches
import com.example.ui.theme.GoldWarm

/**
 * YouTube Studio Style Adaptive Branch Context Pill for AppBar.
 * Single-branch: Static badge without arrow or tap action.
 * Multi-branch: Interactive Dropdown Pill with active branch name & selector.
 */
@Composable
fun BranchContextDropdownPill(
    contextState: ActiveBranchContextState,
    tierAccentColor: Color,
    onOpenSwitcher: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!contextState.isMultiBranchUser) {
        // Single-Branch User: Display static church badge without dropdown arrow or click
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            modifier = modifier.testTag("branch_context_static_pill")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Church,
                    contentDescription = null,
                    tint = tierAccentColor,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = contextState.activeBranchInfo.branchName,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    } else {
        // Multi-Branch User: Interactive Dropdown Pill (YouTube Studio Style)
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (contextState.isGlobalScope) {
                Color(0xFF3B82F6).copy(alpha = 0.14f)
            } else {
                tierAccentColor.copy(alpha = 0.12f)
            },
            border = BorderStroke(
                width = 0.8.dp,
                color = if (contextState.isGlobalScope) Color(0xFF3B82F6).copy(alpha = 0.5f) else tierAccentColor.copy(alpha = 0.4f)
            ),
            modifier = modifier
                .clip(RoundedCornerShape(14.dp))
                .clickable { onOpenSwitcher() }
                .testTag("branch_context_dropdown_pill")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Icon(
                    imageVector = if (contextState.isGlobalScope) Icons.Default.Public else Icons.Default.Church,
                    contentDescription = null,
                    tint = if (contextState.isGlobalScope) Color(0xFF60A5FA) else tierAccentColor,
                    modifier = Modifier.size(13.dp)
                )

                Text(
                    text = if (contextState.isGlobalScope) {
                        "🌐 ग्लोबल ओवरव्यू"
                    } else {
                        contextState.activeBranchInfo.branchName
                    },
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (contextState.isGlobalScope) Color(0xFF93C5FD) else tierAccentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 140.dp)
                )

                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Switch Branch",
                    tint = if (contextState.isGlobalScope) Color(0xFF93C5FD) else tierAccentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Clean Material 3 Modal BottomSheet for Role-Adaptive Church Branch Context Switching.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BranchContextBottomSheet(
    contextState: ActiveBranchContextState,
    tierAccentColor: Color,
    onSelectBranch: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "कलीसिया शाखा चयन (Branch Context)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "सक्रिय कलीसिया या डायसिस कंसोल चुनें",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Option 1: Global / Diocese Console (If eligible)
            val isGlobalEligible = contextState.isMultiBranchUser && (
                contextState.userRoleTier.contains("master", ignoreCase = true) ||
                contextState.userRoleTier.contains("bishop", ignoreCase = true) ||
                contextState.accessibleBranches.size >= DefaultChurches.ALL_BRANCHES.size
            )

            if (isGlobalEligible) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (contextState.isGlobalScope) {
                        Color(0xFF3B82F6).copy(alpha = 0.16f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    },
                    border = BorderStroke(
                        width = if (contextState.isGlobalScope) 1.5.dp else 0.5.dp,
                        color = if (contextState.isGlobalScope) Color(0xFF3B82F6) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            onSelectBranch(DefaultChurches.GLOBAL_SCOPE_ID)
                        }
                        .testTag("branch_option_global")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(Color(0xFF3B82F6).copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Public,
                                    contentDescription = null,
                                    tint = Color(0xFF60A5FA),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "🌐 ग्लोबल ओवरव्यू / डायसिस कंसोल",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "समस्त पंजीकृत शाखाओं का संयुक्त डेटा व नियंत्रण",
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (contextState.isGlobalScope) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Active",
                                tint = Color(0xFF3B82F6),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            // Section: My Assigned Churches
            Text(
                text = "मेरी कलीसियाएं (My Assigned Churches)",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(contextState.accessibleBranches) { branch ->
                    val isSelected = !contextState.isGlobalScope && contextState.activeScope == branch.branchId

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) {
                            tierAccentColor.copy(alpha = 0.14f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        },
                        border = BorderStroke(
                            width = if (isSelected) 1.2.dp else 0.5.dp,
                            color = if (isSelected) tierAccentColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onSelectBranch(branch.branchId)
                            }
                            .testTag("branch_option_${branch.branchId}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(tierAccentColor.copy(alpha = 0.16f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Church,
                                        contentDescription = null,
                                        tint = tierAccentColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = branch.branchName,
                                            fontSize = 12.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (branch.isMainBranch) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = GoldWarm.copy(alpha = 0.18f)
                                            ) {
                                                Text(
                                                    text = "Main",
                                                    fontSize = 8.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = GoldWarm,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }

                                    Text(
                                        text = "${branch.location} • ${branch.pastorInCharge}",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Active",
                                    tint = tierAccentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
