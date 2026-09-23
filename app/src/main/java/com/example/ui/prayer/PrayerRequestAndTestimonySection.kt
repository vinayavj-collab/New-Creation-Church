package com.example.ui.prayer

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.lazy.LazyRow
import com.example.data.model.PrayerCategories
import com.example.data.model.PrayerRequestItem
import com.example.data.model.PrayerRequestsConfig
import com.example.data.model.AdminHierarchy
import com.example.data.model.AdminUser
import com.example.ui.theme.GoldWarm
import com.example.ui.viewmodel.MainViewModel
import com.example.util.UserDeviceHelper
import java.text.SimpleDateFormat
import java.util.*

enum class PrayerSortOption(val title: String, val shortTitle: String) {
    NEWEST("Newest (सबसे नया)", "Newest"),
    OLDEST("Oldest (पुराना)", "Oldest"),
    URGENT_FIRST("Urgent First (अति आवश्यक)", "Urgent"),
    CATEGORY("Tag/Topic (विषय अनुसार)", "Tag"),
    ALPHABETICAL("Alphabetical (नाम A-Z)", "A-Z"),
    SERIAL_ASC("Serial (क्रमांक 1-9)", "1-9"),
    SERIAL_DESC("Serial (क्रमांक 9-1)", "9-1"),
    MY_FIRST("My Requests (मेरे पहले)", "Mine")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerRequestAndTestimonySection(
    viewModel: MainViewModel,
    initialTab: Int = 0,
    modifier: Modifier = Modifier,
    showTabRow: Boolean = true,
    onTabChange: ((Int) -> Unit)? = null
) {
    val context = LocalContext.current
    val allRequests by viewModel.prayerRequests.collectAsState()
    val config by viewModel.prayerRequestsConfig.collectAsState()
    val dynamicAdminPin by viewModel.adminPin.collectAsState()

    // Cache latest dynamic admin pin to SharedPreferences for offline resilience
    LaunchedEffect(dynamicAdminPin) {
        if (dynamicAdminPin.isNotBlank()) {
            UserDeviceHelper.saveCachedAdminPin(context, dynamicAdminPin)
        }
    }

    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab) } // 0: निवेदन, 1: उत्तरित प्रार्थना और गवाही
    var searchQuery by remember { mutableStateOf("") }
    var selectedSort by remember { mutableStateOf(PrayerSortOption.NEWEST) }
    var selectedCategoryFilter by remember { mutableStateOf("सभी") }
    var showSortMenu by remember { mutableStateOf(false) }

    val updateTab = { newTab: Int ->
        selectedTab = newTab
        onTabChange?.invoke(newTab)
    }

    val currentAdmin by viewModel.currentAdmin.collectAsState()
    val canReply = remember(currentAdmin) { AdminHierarchy.canReplyToPrayers(currentAdmin) }

    var showAddDialog by remember { mutableStateOf(false) }
    var answeringRequest by remember { mutableStateOf<PrayerRequestItem?>(null) }
    var replyingRequest by remember { mutableStateOf<PrayerRequestItem?>(null) }
    var inspectingItem by remember { mutableStateOf<PrayerRequestItem?>(null) }
    var itemToDelete by remember { mutableStateOf<PrayerRequestItem?>(null) }

    // Separate active requests vs answered testimonies
    val activeRequests = remember(allRequests) {
        allRequests.filter { !it.isAnswered && !it.isPrivate }
    }
    val answeredTestimonies = remember(allRequests) {
        allRequests.filter { it.isAnswered }
    }

    // Filter and sort for the active tab
    val currentList = if (selectedTab == 0) activeRequests else answeredTestimonies

    val filteredList = remember(currentList, searchQuery, selectedSort, selectedCategoryFilter, allRequests) {
        var list = currentList.filter { item ->
            val matchesCategory = if (selectedCategoryFilter == "सभी") true
            else {
                item.getEffectiveTags().contains(selectedCategoryFilter) || item.getEffectiveCategory() == selectedCategoryFilter
            }
            val matchesQuery = if (searchQuery.isBlank()) true
            else {
                val q = searchQuery.trim().lowercase()
                val serialStr = "#${item.serialNumber}"
                item.name.lowercase().contains(q) ||
                    serialStr.lowercase().contains(q) ||
                    item.serialNumber.toString().contains(q) ||
                    item.requestText.lowercase().contains(q) ||
                    item.pastorName.lowercase().contains(q) ||
                    item.city.lowercase().contains(q) ||
                    item.testimonyText.lowercase().contains(q) ||
                    item.getEffectiveCategory().lowercase().contains(q) ||
                    item.getEffectiveTags().any { it.lowercase().contains(q) }
            }
            matchesCategory && matchesQuery
        }

        // Apply Sorting
        when (selectedSort) {
            PrayerSortOption.NEWEST -> list.sortedByDescending { it.timestamp }
            PrayerSortOption.OLDEST -> list.sortedBy { it.timestamp }
            PrayerSortOption.URGENT_FIRST -> list.sortedWith(
                compareByDescending<PrayerRequestItem> { it.isUrgent }
                    .thenByDescending { it.timestamp }
            )
            PrayerSortOption.CATEGORY -> list.sortedWith(
                compareBy<PrayerRequestItem> { it.getEffectiveCategory() }
                    .thenByDescending { it.timestamp }
            )
            PrayerSortOption.ALPHABETICAL -> list.sortedWith(
                compareBy(String.CASE_INSENSITIVE_ORDER) { it.name.trim() }
            )
            PrayerSortOption.SERIAL_ASC -> list.sortedBy { it.serialNumber }
            PrayerSortOption.SERIAL_DESC -> list.sortedByDescending { it.serialNumber }
            PrayerSortOption.MY_FIRST -> list.sortedWith(
                compareByDescending<PrayerRequestItem> {
                    UserDeviceHelper.isMyRequest(context, it.id, it.senderDeviceId)
                }.thenByDescending { it.timestamp }
            )
        }
    }

    if (!config.enabled) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = "प्रार्थना दीवार व्यवस्थापक द्वारा अस्थायी रूप से स्थगित की गई है।",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        return
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Tab Row: निवेदन | उत्तरित प्रार्थना और गवाही
        if (showTabRow) {
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { updateTab(0) },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.VolunteerActivism, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("निवेदन (${activeRequests.size})", fontWeight = FontWeight.Bold)
                        }
                    }
                )

                if (config.allowTestimonies) {
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { updateTab(1) },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Celebration, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("उत्तरित प्रार्थना और गवाही (${answeredTestimonies.size})", fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        // Search Bar & Sort Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        if (selectedTab == 0) "खोजें: नाम, #क्रमांक, परेशानी..."
                        else "खोजें: नाम, #क्रमांक, गवाही...",
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "खोजें", modifier = Modifier.size(18.dp))
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "साफ करें", modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Sort Menu Button
            Box {
                FilledTonalButton(
                    onClick = { showSortMenu = true },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Sort, contentDescription = "सॉर्ट करें", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(selectedSort.shortTitle, style = MaterialTheme.typography.labelSmall)
                }

                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    PrayerSortOption.values().forEach { opt ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = opt.title,
                                    fontWeight = if (selectedSort == opt) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            trailingIcon = {
                                if (selectedSort == opt) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            },
                            onClick = {
                                selectedSort = opt
                                showSortMenu = false
                            }
                        )
                    }
                }
            }
        }

        // Horizontal Tag Filter Chips Row
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item {
                FilterChip(
                    selected = selectedCategoryFilter == "सभी",
                    onClick = { selectedCategoryFilter = "सभी" },
                    label = {
                        Text(
                            text = "सभी (${currentList.size})",
                            fontSize = 12.sp,
                            fontWeight = if (selectedCategoryFilter == "सभी") FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
            items(PrayerCategories.ALL) { cat ->
                val isSelected = selectedCategoryFilter == cat
                val catCount = currentList.count { it.getEffectiveTags().contains(cat) || it.getEffectiveCategory() == cat }
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategoryFilter = if (isSelected) "सभी" else cat },
                    label = {
                        Text(
                            text = "${PrayerCategories.getCategoryIcon(cat)} $cat ($catCount)",
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondary,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                    )
                )
            }
        }

        // Top Action Card for "निवेदन भेजें"
        if (selectedTab == 0) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "✍️ अपना प्रार्थना निवेदन दर्ज करें",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "क्रमांक स्वतः जुड़ेगा और कलीसिया आपके लिए प्रार्थना करेगी",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }

                    Button(
                        onClick = { showAddDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("निवेदन लिखें")
                    }
                }
            }
        } else {
            // Header notice for Testimonies
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Celebration, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "प्रभु यीशु ने जो भले काम किए हैं, उनका धन्यवाद और गवाही दें। (भजन संहिता 107:2)",
                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic)
                    )
                }
            }
        }

        // List Content
        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (selectedTab == 0) Icons.Default.VolunteerActivism else Icons.Default.Celebration,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = if (searchQuery.isNotBlank()) "कोई परिणाम नहीं मिला"
                        else if (selectedTab == 0) "अभी कोई प्रार्थना निवेदन नहीं है"
                        else "अभी कोई गवाही दर्ज नहीं है",
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.outline)
                    )
                    if (selectedTab == 0 && searchQuery.isBlank()) {
                        TextButton(onClick = { showAddDialog = true }) {
                            Text("पहला निवेदन आप लिखें")
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                filteredList.forEach { item ->
                    val isMine = UserDeviceHelper.isMyRequest(context, item.id, item.senderDeviceId)

                    if (selectedTab == 0) {
                        ActivePrayerRequestCard(
                            item = item,
                            isMine = isMine,
                            onPrayClick = {
                                viewModel.incrementPrayingCountWithLimit(context, item.id) { _, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            },
                            onCompleteClick = { answeringRequest = item },
                            onDetailsClick = { inspectingItem = item },
                            onDeleteClick = { itemToDelete = item }
                        )
                    } else {
                        AnsweredTestimonyCard(
                            item = item,
                            isMine = isMine,
                            onDetailsClick = { inspectingItem = item },
                            onDeleteClick = { itemToDelete = item },
                            onRevertClick = {
                                viewModel.revertAnsweredToPrayer(item.id) { success, err ->
                                    if (success) {
                                        Toast.makeText(context, "निवेदन वापस प्रार्थना सूची में स्थानांतरित हो गया! ↺", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, err ?: "त्रुटि हुई", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Dialog: Delete with Authority (Author direct confirm, Admin PIN required for others)
    itemToDelete?.let { item ->
        val isMine = UserDeviceHelper.isMyRequest(context, item.id, item.senderDeviceId)
        DeletePrayerWithAuthorityDialog(
            item = item,
            isMine = isMine,
            dynamicAdminPin = dynamicAdminPin,
            onDismiss = { itemToDelete = null },
            onConfirmedDelete = { role, pin ->
                val deviceId = UserDeviceHelper.getDeviceId(context)
                viewModel.deletePrayerRequest(
                    item = item,
                    deletedByRole = role,
                    deletedByDeviceId = deviceId,
                    adminPinUsed = pin
                ) { success, err ->
                    if (success) {
                        Toast.makeText(context, "प्रार्थना निवेदन (#${item.serialNumber}) सफलतापूर्वक हटा दिया गया ✅", Toast.LENGTH_LONG).show()
                        itemToDelete = null
                        if (inspectingItem?.id == item.id) {
                            inspectingItem = null
                        }
                    } else {
                        Toast.makeText(context, err ?: "हटाने में त्रुटि हुई", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }


    // Dialog: Add New Prayer Request
    if (showAddDialog) {
        val nextSerial = remember(allRequests) {
            val max = allRequests.maxOfOrNull { it.serialNumber } ?: 0
            max + 1
        }
        AddNewPrayerRequestDialog(
            nextSerial = nextSerial,
            onDismiss = { showAddDialog = false },
            onSubmit = { name, city, userRole, pastorOrDetails, problem, isPrivate, isUrgent, category, tags ->
                val deviceId = UserDeviceHelper.getDeviceId(context)
                viewModel.submitPrayerRequest(
                    name = name,
                    city = city,
                    pastorName = pastorOrDetails,
                    userRole = userRole,
                    isUrgent = isUrgent,
                    requestText = problem,
                    isPrivate = isPrivate,
                    senderDeviceId = deviceId,
                    category = category,
                    tags = tags
                ) { success, serial, err ->
                    if (success) {
                        val msg = if (isUrgent) {
                            "अति-आवश्यक / तत्काल प्रार्थना निवेदन क्रमांक #$serial दर्ज हो गया! 🚨 सभी को तत्काल सूचना भेजी गई।"
                        } else {
                            "प्रार्थना निवेदन (#$serial) - [${PrayerCategories.getCategoryIcon(category)} $category] सफलतापूर्वक जुड़ गया! 🙏"
                        }
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        showAddDialog = false
                    } else {
                        Toast.makeText(context, err ?: "त्रुटि हुई", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    // Dialog: Write Testimony (जब 'पूर्ण हुआ' दबाया जाए)
    answeringRequest?.let { item ->
        WriteTestimonyDialog(
            item = item,
            onDismiss = { answeringRequest = null },
            onSubmit = { testimony ->
                viewModel.markPrayerAsAnswered(item.id, testimony) { success, err ->
                    if (success) {
                        Toast.makeText(context, "स्तुति हो! गवाही सफलतापूर्वक जुड़ गई और गवाही सूची में स्थानांतरित हो गई! 🙌", Toast.LENGTH_LONG).show()
                        answeringRequest = null
                        updateTab(1) // Switch to Testimonies tab to show result
                    } else {
                        Toast.makeText(context, err ?: "त्रुटि हुई", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    // Dialog: Full Inspection Details
    inspectingItem?.let { item ->
        PrayerAndTestimonyDetailsDialog(
            item = item,
            canReply = canReply,
            onReplyClick = {
                val target = inspectingItem
                inspectingItem = null
                replyingRequest = target
            },
            onDismiss = { inspectingItem = null },
            onDelete = {
                val target = inspectingItem
                inspectingItem = null
                itemToDelete = target
            },
            onRevert = {
                viewModel.revertAnsweredToPrayer(item.id) { success, err ->
                    if (success) {
                        Toast.makeText(context, "निवेदन वापस प्रार्थना सूची में स्थानांतरित हो गया! ↺", Toast.LENGTH_LONG).show()
                        inspectingItem = null
                        selectedTab = 0
                    } else {
                        Toast.makeText(context, err ?: "त्रुटि हुई", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onShare = {
                val shareText = buildString {
                    append("✝️ प्रभु का धन्यवाद! उत्तरित प्रार्थना और गवाही\n")
                    append("📌 क्रमांक: #${item.serialNumber}\n")
                    append("👤 नाम: ${item.name}\n")
                    if (item.city.isNotBlank()) append("📍 स्थान: ${item.city}\n")
                    if (item.pastorName.isNotBlank()) append("✝️ पास्टर: ${item.pastorName}\n")
                    append("\n❓ प्रार्थना निवेदन: ${item.requestText}\n")
                    if (item.isAnswered && item.testimonyText.isNotBlank()) {
                        append("\n🎉 गवाही: ${item.testimonyText}\n")
                    }
                    if (item.adminReplyText.isNotBlank()) {
                        append("\n✍️ उत्तर / प्रोत्साहन: ${item.adminReplyText} (${item.adminReplyAuthorName})\n")
                    }
                    append("\n— कलीसिया प्रार्थना संगति")
                }
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                }
                context.startActivity(Intent.createChooser(intent, "गवाही शेयर करें"))
            }
        )
    }

    // Dialog: Reply to Prayer Request (Master Admin & Delegated Roles)
    replyingRequest?.let { item ->
        ReplyToPrayerDialog(
            item = item,
            currentAdmin = currentAdmin,
            onDismiss = { replyingRequest = null },
            onSubmit = { replyText ->
                val authorName = currentAdmin?.name?.ifBlank { "Master Admin" } ?: "Master Admin"
                val authorDesig = currentAdmin?.designation?.ifBlank { "Master Admin" } ?: "Master Admin"
                viewModel.replyToPrayerRequest(
                    requestId = item.id,
                    replyText = replyText,
                    authorName = authorName,
                    authorDesignation = authorDesig
                ) { success, err ->
                    if (success) {
                        Toast.makeText(
                            context,
                            if (replyText.isBlank()) "उत्तर हटाया गया" else "प्रार्थना उत्तर व प्रोत्साहन संदेश सफलतापूर्वक भेजा गया! ✍️",
                            Toast.LENGTH_SHORT
                        ).show()
                        replyingRequest = null
                    } else {
                        Toast.makeText(context, err ?: "त्रुटि हुई", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }
}

@Composable
fun ActivePrayerRequestCard(
    item: PrayerRequestItem,
    isMine: Boolean,
    onPrayClick: () -> Unit,
    onCompleteClick: () -> Unit,
    onDetailsClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val dateStr = remember(item.timestamp) {
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        sdf.format(Date(item.timestamp))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onDetailsClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isUrgent) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.22f)
            else if (isMine) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = if (item.isUrgent) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.8f))
        else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (item.isUrgent || isMine) 3.dp else 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Serial Badge + Name + Urgent + Mine indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Serial Number Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (item.isUrgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    ) {
                        Text(
                            text = "#${item.serialNumber}",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    if (item.isUrgent) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.error
                        ) {
                            Text(
                                text = "🚨 तत्काल",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }
                    }

                    Text(
                        text = item.name + if (item.city.isNotBlank()) " (${item.city})" else "",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (isMine) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(
                                text = "⭐ आपका",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.outline)
                    )
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "हटाएं (Delete)",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.75f),
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            // Role and Details Row
            val roleIcon = when (item.userRole) {
                "पास्टर" -> "✝️"
                "अन्य" -> "👥"
                else -> "👤"
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "$roleIcon ${item.userRole.ifBlank { "विश्वासी" }}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (item.pastorName.isNotBlank()) {
                    Text(
                        text = "• ${item.pastorName}",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.secondary),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Prayer Topic / Tags Row
            val itemTags = item.getEffectiveTags()
            if (itemTags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    itemTags.forEach { tag ->
                        val catIcon = PrayerCategories.getCategoryIcon(tag)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(catIcon, fontSize = 11.sp)
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Prayer Request Text
            Text(
                text = item.requestText,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            // Admin Reply / Encouragement Note Preview
            if (item.adminReplyText.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFFD700).copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.WorkspacePremium,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "✍️ ${item.adminReplyAuthorName.ifBlank { "Master Admin" }}: \"${item.adminReplyText}\"",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons Row: प्रार्थना की & पूर्ण हुआ
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = onPrayClick,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.VolunteerActivism, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("प्रार्थना की 🙏 (${item.prayingCount})", style = MaterialTheme.typography.labelSmall)
                }

                Button(
                    onClick = onCompleteClick,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("पूर्ण हुआ ✅", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun AnsweredTestimonyCard(
    item: PrayerRequestItem,
    isMine: Boolean,
    onDetailsClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onRevertClick: () -> Unit
) {
    val answeredDateStr = remember(item.answeredTimestamp) {
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        if (item.answeredTimestamp > 0) sdf.format(Date(item.answeredTimestamp)) else "उत्तरित"
    }

    // Celebratory Entry Animation for Answered Prayer / Testimony Transition
    val scaleAnim = remember { Animatable(0.82f) }
    val alphaAnim = remember { Animatable(0f) }
    val glowAnim = remember { Animatable(0f) }

    LaunchedEffect(item.id) {
        kotlinx.coroutines.coroutineScope {
            launch {
                alphaAnim.animateTo(1f, tween(350))
            }
            launch {
                scaleAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }
            launch {
                glowAnim.animateTo(1f, tween(400))
                glowAnim.animateTo(0.25f, tween(800))
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scaleAnim.value
                scaleY = scaleAnim.value
                alpha = alphaAnim.value
            }
            .clickable { onDetailsClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            (1.5.dp * (1f + glowAnim.value * 0.8f)),
            Color(0xFFFFD700).copy(alpha = (0.35f + glowAnim.value * 0.5f))
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = (2.dp + 3.dp * glowAnim.value))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Serial + Name + Answered Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Text(
                            text = "#${item.serialNumber}",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Text(
                        text = item.name + if (item.city.isNotBlank()) " (${item.city})" else "",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "🎉 उत्तरित ($answeredDateStr)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "हटाएं (Delete)",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.75f),
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Original Prayer Need Snippet
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "मूल प्रार्थना: ${item.requestText}",
                        style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Tags row
            val itemTags = item.getEffectiveTags()
            if (itemTags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    itemTags.forEach { tag ->
                        val catIcon = PrayerCategories.getCategoryIcon(tag)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(catIcon, fontSize = 11.sp)
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Testimony Text
            Text(
                text = "🕊️ गवाही: " + item.testimonyText.ifBlank { "प्रभु ने प्रार्थना का उत्तर दिया और महिमा पाई।" },
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Actions: Details & Revert Option
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onDetailsClick,
                    contentPadding = PaddingValues(horizontal = 6.dp)
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("पूरा विवरण देखें")
                }

                OutlinedButton(
                    onClick = onRevertClick,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("वापस निवेदन में लाएं", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddNewPrayerRequestDialog(
    nextSerial: Int,
    onDismiss: () -> Unit,
    onSubmit: (name: String, city: String, userRole: String, pastorOrDetails: String, problem: String, isPrivate: Boolean, isUrgent: Boolean, category: String, tags: List<String>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var userRole by remember { mutableStateOf("") }
    var pastorOrDetails by remember { mutableStateOf("") }
    var problem by remember { mutableStateOf("") }
    var isPrivate by remember { mutableStateOf(false) }
    var isUrgent by remember { mutableStateOf(false) }
    var selectedTags by remember { mutableStateOf(setOf<String>()) }
    var showUrgentWarningDialog by remember { mutableStateOf(false) }
    var hasAttemptedSubmit by remember { mutableStateOf(false) }

    if (showUrgentWarningDialog) {
        AlertDialog(
            onDismissRequest = { showUrgentWarningDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "⚠️ अति-आवश्यक / तत्काल प्रार्थना चेतावनी",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Text(
                    text = "चेतावनी: बहुत इमरजेंसी या तत्काल आवश्यकता होने पर ही इसे दबाएं!\n\n(जैसे: गंभीर बीमारी, आईसीयू, आकस्मिक दुर्घटना, या जीवन रक्षा)।\n\nअन्यथा इसके बिना ही सामान्य प्रार्थना निवेदन के रूप में आगे बढ़ें। क्या आप सहमति देकर इसे 'तत्काल प्रार्थना' बनाना चाहते हैं?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        isUrgent = true
                        showUrgentWarningDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("सहमति है (हाँ, तत्काल है)")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        isUrgent = false
                        showUrgentWarningDialog = false
                    }
                ) {
                    Text("कैंसिल (रद्द करें)")
                }
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(androidx.compose.foundation.rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "✍️ प्रार्थना निवेदन दर्ज करें",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "क्रमांक #$nextSerial स्वतः आवंटित होगा",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "बंद करें")
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("आपका नाम (वैकल्पिक)") },
                    placeholder = { Text("उदा. विश्वासी भाई / बहन") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("आपका स्थान (शहर / गाँव)") },
                    placeholder = { Text("उदा. इंदौर / नई दिल्ली") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Role Selection (MANDATORY: विश्वासी / पास्टर / अन्य)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "आप कौन हैं? (अनिवार्य) *",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        if (userRole.isNotBlank()) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = userRole,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("विश्वासी", "पास्टर", "अन्य").forEach { role ->
                            val isSelected = userRole == role
                            val icon = when (role) {
                                "पास्टर" -> "✝️"
                                "अन्य" -> "👥"
                                else -> "👤"
                            }
                            FilterChip(
                                selected = isSelected,
                                onClick = { userRole = role },
                                label = {
                                    Text(
                                        text = "$icon $role",
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }

                    if (hasAttemptedSubmit && userRole.isBlank()) {
                        Text(
                            text = "⚠️ कृपया 'विश्वासी', 'पास्टर' या 'अन्य' में से एक विकल्प अवश्य चुनें",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.error)
                        )
                    }

                    if (userRole == "पास्टर") {
                        OutlinedTextField(
                            value = pastorOrDetails,
                            onValueChange = { pastorOrDetails = it },
                            label = { Text("कलीसिया का नाम / पास्टर विवरण (वैकल्पिक)") },
                            placeholder = { Text("उदा. ग्रेस चर्च, इंदौर") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else if (userRole == "अन्य") {
                        OutlinedTextField(
                            value = pastorOrDetails,
                            onValueChange = { pastorOrDetails = it },
                            label = { Text("अन्य पहचान विवरण (वैकल्पिक)") },
                            placeholder = { Text("उदा. प्रचारक, युवा अगुवा, मेहमान आदि") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Prayer Subject / Topics Selection (MANDATORY Tag Selection)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "प्रार्थना विषय / Tag (कम से कम एक चुनें) *",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        if (selectedTags.isNotEmpty()) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = "${selectedTags.size} चुना",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // Display all prayer categories in an adaptive FlowRow layout with clear visual feedback
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PrayerCategories.ALL.forEach { tag ->
                            val isTagSelected = selectedTags.contains(tag)
                            val tagIcon = PrayerCategories.getCategoryIcon(tag)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isTagSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (isTagSelected) 1.5.dp else 1.dp,
                                    color = if (isTagSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                ),
                                shadowElevation = if (isTagSelected) 2.dp else 0.dp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        selectedTags = if (isTagSelected) {
                                            selectedTags - tag
                                        } else {
                                            selectedTags + tag
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = tagIcon,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = tag,
                                        fontSize = 12.sp,
                                        fontWeight = if (isTagSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isTagSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isTagSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (hasAttemptedSubmit && selectedTags.isEmpty()) {
                        Text(
                            text = "⚠️ कृपया कम से कम एक 'प्रार्थना विषय' (Tag) अवश्य चुनें (जैसे: चंगाई, पारिवारिक, आदि)",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.error)
                        )
                    }
                }

                // Urgent Prayer Section with Confirmation trigger
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isUrgent) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isUrgent) 2.dp else 1.dp,
                        color = if (isUrgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            if (!isUrgent) {
                                showUrgentWarningDialog = true
                            } else {
                                isUrgent = false
                            }
                        }
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
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isUrgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = if (isUrgent) Color.White else MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "तत्काल प्रार्थना (अति-आवश्यक)",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isUrgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                    if (isUrgent) {
                                        Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.error) {
                                            Text(
                                                text = "सक्रिय 🚨",
                                                style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontWeight = FontWeight.Bold),
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = if (isUrgent) "यह निवेदन सभी यूज़र्स को तत्काल विंडो में दिखेगा।" else "अति-इमरजेंसी होने पर ही टिक करें (चेतावनी सहित)",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isUrgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }

                        Checkbox(
                            checked = isUrgent,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    showUrgentWarningDialog = true
                                } else {
                                    isUrgent = false
                                }
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.error,
                                checkmarkColor = Color.White
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = problem,
                    onValueChange = { problem = it },
                    label = { Text("आपकी परेशानी / प्रार्थना निवेदन *") },
                    placeholder = { Text("कृपया अपनी प्रार्थना की ज़रूरत यहाँ विस्तार से लिखें...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { isPrivate = !isPrivate }
                        .padding(4.dp)
                ) {
                    Checkbox(checked = isPrivate, onCheckedChange = { isPrivate = it })
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "गोपनीय रखें (केवल पास्टर व प्रार्थना दल को दिखे)",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Button(
                    onClick = {
                        hasAttemptedSubmit = true
                        if (userRole.isBlank() || problem.isBlank() || selectedTags.isEmpty()) {
                            return@Button
                        }
                        val primaryCategory = selectedTags.firstOrNull() ?: "अन्य"
                        val tagsList = selectedTags.toList()
                        onSubmit(name, city, userRole, pastorOrDetails, problem, isPrivate, isUrgent, primaryCategory, tagsList)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = if (isUrgent) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    else ButtonDefaults.buttonColors()
                ) {
                    Icon(
                        imageVector = if (isUrgent) Icons.Default.Warning else Icons.Default.Send,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isUrgent) "🚨 तत्काल निवेदन सबमिट करें (#$nextSerial)" else "निवेदन सबमिट करें (#$nextSerial)"
                    )
                }
            }
        }
    }
}

@Composable
fun WriteTestimonyDialog(
    item: PrayerRequestItem,
    onDismiss: () -> Unit,
    onSubmit: (testimony: String) -> Unit
) {
    var testimony by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 20.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🙌 परमेश्वर ने आपकी प्रार्थना का उत्तर कैसे दिया?",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        )
                        Text(
                            text = "गवाही लिखें (निवेदन क्रमांक: #${item.serialNumber})",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "बंद करें")
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "मूल प्रार्थना: ${item.requestText}",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }

                OutlinedTextField(
                    value = testimony,
                    onValueChange = { testimony = it },
                    label = { Text("परमेश्वर ने आपकी प्रार्थना का उत्तर कैसे दिया? (गवाही लिखें) *") },
                    placeholder = { Text("परमेश्वर की भलाई और इस प्रार्थना के उत्तर के बारे में विस्तार से यहाँ लिखें...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                Button(
                    onClick = {
                        if (testimony.isBlank()) return@Button
                        onSubmit(testimony)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = testimony.isNotBlank()
                ) {
                    Icon(Icons.Default.Celebration, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("गवाही दर्ज करें और सूची में जोड़ें")
                }
            }
        }
    }
}

@Composable
fun PrayerAndTestimonyDetailsDialog(
    item: PrayerRequestItem,
    canReply: Boolean = false,
    onReplyClick: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onRevert: () -> Unit,
    onShare: () -> Unit
) {
    val submittedDate = remember(item.timestamp) {
        val sdf = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault())
        sdf.format(Date(item.timestamp))
    }
    val answeredDate = remember(item.answeredTimestamp) {
        if (item.answeredTimestamp > 0) {
            val sdf = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault())
            sdf.format(Date(item.answeredTimestamp))
        } else "अभी उत्तर की प्रतीक्षा में"
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 20.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Text(
                                text = "#${item.serialNumber}",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Column {
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            if (item.city.isNotBlank()) {
                                Text(
                                    text = "स्थान: ${item.city}",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "बंद करें")
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant)

                // Urgent & Role & Tag Badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (item.isUrgent) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.error
                        ) {
                            Text(
                                text = "🚨 अति-आवश्यक / तत्काल",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    val roleIcon = when (item.userRole) {
                        "पास्टर" -> "✝️"
                        "अन्य" -> "👥"
                        else -> "👤"
                    }
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "$roleIcon ${item.userRole.ifBlank { "विश्वासी" }}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                // Tags Badges in Details
                val itemDetailTags = item.getEffectiveTags()
                if (itemDetailTags.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "विषय (Tags):",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                        )
                        itemDetailTags.forEach { tag ->
                            val icon = PrayerCategories.getCategoryIcon(tag)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = "$icon $tag",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }

                // Info Rows
                if (item.pastorName.isNotBlank()) {
                    Text(
                        text = "विवरण / कलीसिया: ${item.pastorName}",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold)
                    )
                }

                Text(
                    text = "📅 कब निवेदन डाला गया: $submittedDate",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )

                // Problem / Request Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "❓ प्रार्थना का विषय / परेशानी:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.requestText,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Admin Encouragement / Reply Card
                if (item.adminReplyText.isNotBlank()) {
                    val replyDate = remember(item.adminReplyTimestamp) {
                        if (item.adminReplyTimestamp > 0L) {
                            val sdf = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault())
                            sdf.format(Date(item.adminReplyTimestamp))
                        } else ""
                    }
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFFFD700).copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFFD700).copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.WorkspacePremium,
                                        contentDescription = null,
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "✍️ उत्तर व आत्मिक प्रोत्साहन",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF92400E)
                                        )
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFFFD700).copy(alpha = 0.25f)
                                ) {
                                    Text(
                                        text = item.adminReplyAuthorDesignation.ifBlank { "Master Admin" },
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF92400E),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = item.adminReplyText,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )

                            if (item.adminReplyAuthorName.isNotBlank() || replyDate.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (item.adminReplyAuthorName.isNotBlank()) {
                                        Text(
                                            text = "— ${item.adminReplyAuthorName}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Color(0xFF92400E),
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                    if (replyDate.isNotBlank()) {
                                        Text(
                                            text = replyDate,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Master Admin / Delegated Reply Action Button
                if (canReply && onReplyClick != null) {
                    FilledTonalButton(
                        onClick = onReplyClick,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.RateReview, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (item.adminReplyText.isNotBlank()) "✍️ उत्तर / प्रोत्साहन संदेश संपादित करें (Edit Reply)" else "✍️ उत्तर व प्रोत्साहन संदेश भेजें (Reply)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                // If Answered: Testimony Section
                if (item.isAnswered) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "🎉 कब पूर्ण हुआ: $answeredDate",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "🕊️ क्या गवाही लिखी:",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = item.testimonyText.ifBlank { "प्रभु ने प्रार्थना सुन ली!" },
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // Action Buttons: Share & Revert & Delete
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onShare,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("शेयर करें")
                    }

                    if (item.isAnswered) {
                        Button(
                            onClick = onRevert,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("वापस निवेदन")
                        }
                    }

                    OutlinedButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.weight(if (item.isAnswered) 0.8f else 1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("हटाएं")
                    }
                }
            }
        }
    }
}

@Composable
fun ReplyToPrayerDialog(
    item: PrayerRequestItem,
    currentAdmin: AdminUser?,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var replyText by remember { mutableStateOf(item.adminReplyText) }
    val authorName = currentAdmin?.name?.ifBlank { "Master Admin" } ?: "Master Admin"
    val authorDesignation = currentAdmin?.designation?.ifBlank { "Master Admin" } ?: "Master Admin"

    val sampleNotes = listOf(
        "🙏 प्रभु आपकी सहायता करेंगे! हम आपके साथ निरंतर प्रार्थना में जुड़े हैं।",
        "✝️ यहोवा राफा आपको पूर्ण चंगाई और स्वास्थ्य प्रदान करे।",
        "✨ विश्वास रखें, परमेश्वर के लिए सब कुछ संभव है। (लूका 1:37)",
        "🕊️ प्रभु का अनुग्रह, शांति और सामर्थ्य आपके और आपके परिवार के साथ रहे।"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 20.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "✍️ प्रार्थना निवेदन पर उत्तर व प्रोत्साहन",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "निवेदन #${item.serialNumber} • ${item.name} (${item.city.ifBlank { "विश्वासी" }})",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "बंद करें")
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant)

                // Request preview
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "❓ विश्वासी का निवेदन:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.requestText,
                            fontSize = 12.sp,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Author Info Chip
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFFD700).copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(16.dp))
                        Text(
                            text = "उत्तर प्रेषक: $authorName ($authorDesignation)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF92400E)
                        )
                    }
                }

                // Quick suggestions
                Text(
                    text = "💡 त्वरित आत्मिक सांत्वना वचन (Quick Templates):",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    sampleNotes.forEach { note ->
                        Surface(
                            onClick = { replyText = note },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = note,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Text Input
                OutlinedTextField(
                    value = replyText,
                    onValueChange = { if (it.length <= 400) replyText = it },
                    label = { Text("आत्मिक उत्तर / प्रोत्साहन संदेश लिखें") },
                    placeholder = { Text("उदा. प्रभु आपकी सहायता करेंगे! हम आपके साथ प्रार्थना में जुड़े हैं।") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 110.dp),
                    shape = RoundedCornerShape(12.dp),
                    supportingText = {
                        Text("${replyText.length}/400 अक्षर")
                    }
                )

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (item.adminReplyText.isNotBlank()) {
                        OutlinedButton(
                            onClick = { onSubmit("") },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("उत्तर हटाएं")
                        }
                    }

                    Button(
                        onClick = { onSubmit(replyText.trim()) },
                        enabled = replyText.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldWarm, contentColor = Color.Black),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (item.adminReplyText.isNotBlank()) "अपडेट करें" else "उत्तर भेजें", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun DeletePrayerWithAuthorityDialog(
    item: PrayerRequestItem,
    isMine: Boolean,
    dynamicAdminPin: String = "",
    onDismiss: () -> Unit,
    onConfirmedDelete: (role: String, enteredPin: String) -> Unit
) {
    val context = LocalContext.current
    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = if (isMine) Icons.Default.DeleteOutline else Icons.Default.AdminPanelSettings,
                contentDescription = null,
                tint = if (isMine) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = if (isMine) "प्रार्थना निवेदन हटाएं" else "हटाने की ऑथोरिटी (Admin PIN)",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isMine) {
                    Text(
                        text = "क्या आप निश्चित रूप से अपना यह प्रार्थना निवेदन हटाना चाहते हैं?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "#${item.serialNumber} • ${item.name}",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = item.requestText,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Text(
                        text = "⚠️ यह निवेदन डेटाबेस से स्थायी रूप से हटा दिया जाएगा।",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Text(
                        text = "यह निवेदन किसी अन्य विश्वासी का है। इसे हटाने के लिए एडमिन / पास्टर ऑथोरिटी पिन (Security PIN) दर्ज करें:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "#${item.serialNumber} • ${item.name}" + if (item.city.isNotBlank()) " (${item.city})" else "",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = item.requestText,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    OutlinedTextField(
                        value = enteredPin,
                        onValueChange = {
                            enteredPin = it
                            pinError = null
                        },
                        label = { Text("Admin Authority PIN") },
                        placeholder = { Text("सुरक्षा पिन दर्ज करें") },
                        singleLine = true,
                        isError = pinError != null,
                        supportingText = pinError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isMine) {
                        onConfirmedDelete("AUTHOR", "")
                    } else {
                        if (UserDeviceHelper.isValidAdminPin(context, enteredPin, dynamicAdminPin)) {
                            onConfirmedDelete("ADMIN_PIN", enteredPin.trim())
                        } else {
                            pinError = "अमान्य पिन! केवल अधिकृत एडमिन/पास्टर ही हटा सकते हैं।"
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isMine) "हटाएं" else "सत्यापित कर हटाएं")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("रद्द करें")
            }
        }
    )
}

