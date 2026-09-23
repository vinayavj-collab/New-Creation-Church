package com.example.ui.admin

import android.content.Intent
import android.widget.Toast
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChurchAttendanceRecord
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAttendanceScreen(
    attendanceRecords: List<ChurchAttendanceRecord>,
    onRecordAttendance: (ChurchAttendanceRecord) -> Unit,
    onDeleteAttendance: (String) -> Unit,
    onExportReport: () -> String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var recordToDelete by remember { mutableStateOf<ChurchAttendanceRecord?>(null) }
    var selectedServiceFilter by remember { mutableStateOf("ALL") }

    val serviceOptions = listOf(
        "ALL" to "सभी सभाएं",
        "SUNDAY" to "रविवार आराधना",
        "YOUTH" to "युवा संगति",
        "WOMEN" to "महिला संगति",
        "PRAYER" to "प्रार्थना सभा"
    )

    val filteredRecords = remember(attendanceRecords, selectedServiceFilter) {
        attendanceRecords.filter { record ->
            when (selectedServiceFilter) {
                "ALL" -> true
                "SUNDAY" -> record.serviceType.contains("रविवार", ignoreCase = true) || record.serviceType.contains("Sunday", ignoreCase = true)
                "YOUTH" -> record.serviceType.contains("युवा", ignoreCase = true) || record.serviceType.contains("Youth", ignoreCase = true)
                "WOMEN" -> record.serviceType.contains("महिला", ignoreCase = true) || record.serviceType.contains("Women", ignoreCase = true)
                "PRAYER" -> record.serviceType.contains("प्रार्थना", ignoreCase = true) || record.serviceType.contains("Prayer", ignoreCase = true)
                else -> true
            }
        }
    }

    val totalAttendanceSum = remember(attendanceRecords) {
        attendanceRecords.sumOf { it.totalCount }
    }
    val avgAttendance = remember(attendanceRecords) {
        if (attendanceRecords.isNotEmpty()) totalAttendanceSum / attendanceRecords.size else 0
    }
    val totalVisitors = remember(attendanceRecords) {
        attendanceRecords.sumOf { it.newVisitorsCount }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "कलीसिया सभा उपस्थिति ट्रैकर",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${attendanceRecords.size} सभाओं का रिकॉर्ड",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("attendance_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val uri = com.example.util.CsvExportHelper.exportAttendanceToCsv(context, attendanceRecords)
                            if (uri != null) {
                                com.example.util.CsvExportHelper.shareCsvFile(
                                    context,
                                    uri,
                                    "कलीसिया उपस्थिति रिकॉर्ड CSV रिपोर्ट"
                                )
                            } else {
                                Toast.makeText(context, "CSV फ़ाइल बनाने में त्रुटि हुई", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.testTag("export_attendance_csv_button")
                    ) {
                        Icon(
                            Icons.Default.FileDownload,
                            contentDescription = "CSV डाउनलोड (Export CSV)",
                            tint = com.example.ui.theme.GoldWarm
                        )
                    }
                    IconButton(
                        onClick = {
                            val report = onExportReport()
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Church Attendance Report")
                                putExtra(Intent.EXTRA_TEXT, report)
                            }
                            context.startActivity(Intent.createChooser(intent, "उपस्थिति रिपोर्ट साझा करें"))
                        },
                        modifier = Modifier.testTag("export_attendance_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Export Text")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("उपस्थिति दर्ज करें") },
                modifier = Modifier.testTag("add_attendance_fab")
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Stats summary card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "$totalAttendanceSum", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(text = "कुल उपस्थिति", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Divider(modifier = Modifier.height(36.dp).width(1.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "$avgAttendance", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                        Text(text = "औसत प्रति सभा", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Divider(modifier = Modifier.height(36.dp).width(1.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "$totalVisitors", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                        Text(text = "नये आगंतुक", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Filter chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                items(serviceOptions) { (key, label) ->
                    FilterChip(
                        selected = selectedServiceFilter == key,
                        onClick = { selectedServiceFilter = key },
                        label = { Text(label, fontSize = 13.sp) },
                        leadingIcon = if (selectedServiceFilter == key) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }

            if (filteredRecords.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.CoPresent,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "कोई उपस्थिति रिकॉर्ड नहीं मिला",
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { showAddDialog = true }) {
                            Text("पहली उपस्थिति दर्ज करें")
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredRecords, key = { it.id }) { record ->
                        AttendanceRecordCard(
                            record = record,
                            onDelete = { recordToDelete = record }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddAttendanceDialog(
            onDismiss = { showAddDialog = false },
            onSave = { record ->
                onRecordAttendance(record)
                showAddDialog = false
            }
        )
    }

    if (recordToDelete != null) {
        AlertDialog(
            onDismissRequest = { recordToDelete = null },
            title = { Text("रिकॉर्ड हटाएं?") },
            text = { Text("क्या आप सचमुच ${recordToDelete?.dateString} की उपस्थिति का रिकॉर्ड हटाना चाहते हैं?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        recordToDelete?.let { onDeleteAttendance(it.id) }
                        recordToDelete = null
                    }
                ) {
                    Text("हटाएं", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { recordToDelete = null }) {
                    Text("रद्द करें")
                }
            }
        )
    }
}

@Composable
fun AttendanceRecordCard(
    record: ChurchAttendanceRecord,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
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
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${record.totalCount}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = record.serviceType,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "📅 ${record.dateString}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Breakdown pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CountBadge(label = "पुरुष", count = record.countMen, color = Color(0xFF1976D2), modifier = Modifier.weight(1f))
                CountBadge(label = "महिला", count = record.countWomen, color = Color(0xFFD81B60), modifier = Modifier.weight(1f))
                CountBadge(label = "बच्चे", count = record.countChildren, color = Color(0xFF388E3C), modifier = Modifier.weight(1f))
                if (record.newVisitorsCount > 0) {
                    CountBadge(label = "आगंतुक", count = record.newVisitorsCount, color = Color(0xFFF57C00), modifier = Modifier.weight(1f))
                }
            }

            if (record.topicOrPreacher.isNotBlank() || record.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                if (record.topicOrPreacher.isNotBlank()) {
                    Text(
                        text = "📖 वचन/प्रचारक: ${record.topicOrPreacher}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (record.notes.isNotBlank()) {
                    Text(
                        text = "📝 ${record.notes}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun CountBadge(label: String, count: Int, color: Color, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.12f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "$count", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = color)
            Text(text = label, fontSize = 11.sp, color = color)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAttendanceDialog(
    onDismiss: () -> Unit,
    onSave: (ChurchAttendanceRecord) -> Unit
) {
    val todayFormatted = remember {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
    }
    var dateString by remember { mutableStateOf(todayFormatted) }
    var serviceType by remember { mutableStateOf("रविवार मुख्य आराधना (Sunday Worship)") }
    var menCount by remember { mutableIntStateOf(0) }
    var womenCount by remember { mutableIntStateOf(0) }
    var childrenCount by remember { mutableIntStateOf(0) }
    var visitorsCount by remember { mutableIntStateOf(0) }
    var topicOrPreacher by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val serviceTypes = listOf(
        "रविवार मुख्य आराधना (Sunday Worship)",
        "रविवार संध्या आराधना (Sunday Evening)",
        "युवा संगति (Youth Fellowship)",
        "महिला संगति (Women Fellowship)",
        "कुटीर प्रार्थना (Cottage Prayer)",
        "उपवास व मध्यस्थता प्रार्थना (Fasting Prayer)",
        "विशेष संगति / उत्सव (Special Event)"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("सभा उपस्थिति दर्ज करें") },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = dateString,
                        onValueChange = { dateString = it },
                        label = { Text("दिनांक (DD/MM/YYYY)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Text("सभा का प्रकार:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(serviceTypes) { st ->
                            FilterChip(
                                selected = serviceType == st,
                                onClick = { serviceType = st },
                                label = { Text(st.substringBefore(" ("), fontSize = 12.sp) }
                            )
                        }
                    }
                }
                item {
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                    Text("संख्या विवरण (कुल: ${menCount + womenCount + childrenCount})", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                item {
                    CounterRow(label = "👨 पुरुष (Men)", count = menCount, onCountChange = { menCount = it.coerceAtLeast(0) })
                }
                item {
                    CounterRow(label = "👩 महिला (Women)", count = womenCount, onCountChange = { womenCount = it.coerceAtLeast(0) })
                }
                item {
                    CounterRow(label = "👶 बच्चे (Children)", count = childrenCount, onCountChange = { childrenCount = it.coerceAtLeast(0) })
                }
                item {
                    CounterRow(label = "🤝 नये आगंतुक (Visitors)", count = visitorsCount, onCountChange = { visitorsCount = it.coerceAtLeast(0) })
                }
                item {
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                    OutlinedTextField(
                        value = topicOrPreacher,
                        onValueChange = { topicOrPreacher = it },
                        label = { Text("प्रचारक का नाम / वचन का विषय") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("अन्य टिप्पणी / गवाही") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val record = ChurchAttendanceRecord(
                        serviceType = serviceType,
                        dateString = dateString.trim(),
                        countMen = menCount,
                        countWomen = womenCount,
                        countChildren = childrenCount,
                        totalCount = menCount + womenCount + childrenCount,
                        newVisitorsCount = visitorsCount,
                        topicOrPreacher = topicOrPreacher.trim(),
                        notes = notes.trim()
                    )
                    onSave(record)
                }
            ) {
                Text("सहेजें (Save)")
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
fun CounterRow(label: String, count: Int, onCountChange: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { onCountChange(count - 1) },
                enabled = count > 0,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Decrease")
            }
            Text(
                text = "$count",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.widthIn(min = 32.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            IconButton(
                onClick = { onCountChange(count + 1) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.AddCircleOutline, contentDescription = "Increase")
            }
            Spacer(modifier = Modifier.width(4.dp))
            OutlinedButton(
                onClick = { onCountChange(count + 5) },
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Text("+5", fontSize = 11.sp)
            }
        }
    }
}
