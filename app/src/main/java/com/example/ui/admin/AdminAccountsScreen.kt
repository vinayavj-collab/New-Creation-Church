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
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChurchAccountTransaction
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAccountsScreen(
    transactions: List<ChurchAccountTransaction>,
    onAddTransaction: (ChurchAccountTransaction) -> Unit,
    onDeleteTransaction: (String) -> Unit,
    onExportReport: () -> String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var transactionToDelete by remember { mutableStateOf<ChurchAccountTransaction?>(null) }
    var selectedFilter by remember { mutableStateOf("ALL") }

    val filterOptions = listOf(
        "ALL" to "सभी लेन-देन",
        "INCOME" to "आय / दशमांश",
        "EXPENSE" to "व्यय / खर्चे"
    )

    val totalIncome = remember(transactions) {
        transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    }
    val totalExpense = remember(transactions) {
        transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    }
    val netBalance = remember(totalIncome, totalExpense) {
        totalIncome - totalExpense
    }

    val filteredTransactions = remember(transactions, selectedFilter) {
        transactions.filter { tx ->
            when (selectedFilter) {
                "ALL" -> true
                "INCOME" -> tx.type == "INCOME"
                "EXPENSE" -> tx.type == "EXPENSE"
                else -> true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "कलीसिया दशमांश व वित्तीय लेखा",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${transactions.size} प्रविष्टियां दर्ज",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("accounts_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val uri = com.example.util.CsvExportHelper.exportAccountsToCsv(context, transactions)
                            if (uri != null) {
                                com.example.util.CsvExportHelper.shareCsvFile(
                                    context,
                                    uri,
                                    "कलीसिया वित्तीय विवरण (दशमांश व लेखा) CSV रिपोर्ट"
                                )
                            } else {
                                Toast.makeText(context, "CSV फ़ाइल बनाने में त्रुटि हुई", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.testTag("export_accounts_csv_button")
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
                                putExtra(Intent.EXTRA_SUBJECT, "Church Financial Statement")
                                putExtra(Intent.EXTRA_TEXT, report)
                            }
                            context.startActivity(Intent.createChooser(intent, "लेखा विवरण साझा करें"))
                        },
                        modifier = Modifier.testTag("export_accounts_button")
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
                icon = { Icon(Icons.Default.AddCard, contentDescription = null) },
                text = { Text("लेन-देन दर्ज करें") },
                modifier = Modifier.testTag("add_transaction_fab")
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Financial Summary Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("वर्तमान शेष (Balance):", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(
                            text = "₹${formatAmount(netBalance)}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (netBalance >= 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("कुल आय (Income)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                text = "₹${formatAmount(totalIncome)}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = Color(0xFFD32F2F), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("कुल व्यय (Expense)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                text = "₹${formatAmount(totalExpense)}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD32F2F)
                            )
                        }
                    }
                }
            }

            // Filter chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 8.dp)
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

            if (filteredTransactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.ReceiptLong,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "कोई लेन-देन नहीं मिला",
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { showAddDialog = true }) {
                            Text("पहला लेन-देन दर्ज करें")
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredTransactions, key = { it.id }) { tx ->
                        TransactionItemCard(
                            transaction = tx,
                            onDelete = { transactionToDelete = tx }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddTransactionDialog(
            onDismiss = { showAddDialog = false },
            onSave = { tx ->
                onAddTransaction(tx)
                showAddDialog = false
            }
        )
    }

    if (transactionToDelete != null) {
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = { Text("लेन-देन हटाएं?") },
            text = { Text("क्या आप सचमुच ₹${transactionToDelete?.amount} की यह प्रविष्टि हटाना चाहते हैं?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        transactionToDelete?.let { onDeleteTransaction(it.id) }
                        transactionToDelete = null
                    }
                ) {
                    Text("हटाएं", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) {
                    Text("रद्द करें")
                }
            }
        )
    }
}

@Composable
fun TransactionItemCard(
    transaction: ChurchAccountTransaction,
    onDelete: () -> Unit
) {
    val isIncome = transaction.type == "INCOME"
    val accentColor = if (isIncome) Color(0xFF2E7D32) else Color(0xFFD32F2F)

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isIncome) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = transaction.category,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = (if (isIncome) "+ " else "- ") + "₹" + formatAmount(transaction.amount),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = accentColor
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📅 ${transaction.dateString} • ${transaction.paymentMode}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (transaction.donorOrRecipient.isNotBlank() || transaction.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    if (transaction.donorOrRecipient.isNotBlank()) {
                        Text(
                            text = (if (isIncome) "दाता: " else "प्राप्तकर्ता: ") + transaction.donorOrRecipient,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (transaction.notes.isNotBlank()) {
                        Text(
                            text = "नोट: ${transaction.notes}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    onDismiss: () -> Unit,
    onSave: (ChurchAccountTransaction) -> Unit
) {
    val todayFormatted = remember {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
    }
    var type by remember { mutableStateOf("INCOME") }
    var amountStr by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("दशमांश (Tithe)") }
    var donorOrRecipient by remember { mutableStateOf("") }
    var paymentMode by remember { mutableStateOf("नकद (Cash)") }
    var dateString by remember { mutableStateOf(todayFormatted) }
    var notes by remember { mutableStateOf("") }

    val incomeCategories = listOf(
        "दशमांश (Tithe)",
        "रविवार भेंट (Sunday Offering)",
        "धन्यवाद भेंट (Thanksgiving)",
        "भवन निर्माण कोष (Building Fund)",
        "मिशनरी सहयोग (Missionary Support)",
        "संडे स्कूल भेंट (Sunday School)",
        "अन्य दान / भेंट (Other Donation)"
    )

    val expenseCategories = listOf(
        "बिजली / पानी बिल (Utility Bills)",
        "भवन किराया (Church Rent)",
        "पादरी मानदेय / सहयोग (Pastor Honorarium)",
        "संगीत व उपकरण (Sound & Media)",
        "कलीसिया कार्यक्रम / जलपान (Events & Hospitality)",
        "गरीब सहायता / परोपकार (Charity / Relief)",
        "स्टेशनरी व मुद्रण (Printing & Office)",
        "अन्य व्यय (Miscellaneous)"
    )

    val paymentModes = listOf("नकद (Cash)", "UPI / स्कैनर", "बैंक ट्रांसफर (NEFT/IMPS)", "चेक (Cheque)")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (type == "INCOME") "आय / भेंट प्रविष्टि दर्ज करें" else "व्यय / खर्च प्रविष्टि दर्ज करें") },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    // Type selector tabs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                type = "INCOME"
                                category = incomeCategories.first()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (type == "INCOME") Color(0xFF2E7D32) else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (type == "INCOME") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("आय (Income)")
                        }

                        Button(
                            onClick = {
                                type = "EXPENSE"
                                category = expenseCategories.first()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (type == "EXPENSE") Color(0xFFD32F2F) else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (type == "EXPENSE") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("व्यय (Expense)")
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        label = { Text("राशि (₹) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Text("श्रेणी (Category):", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    val activeCategories = if (type == "INCOME") incomeCategories else expenseCategories
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(activeCategories) { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat.substringBefore(" ("), fontSize = 12.sp) }
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = donorOrRecipient,
                        onValueChange = { donorOrRecipient = it },
                        label = { Text(if (type == "INCOME") "भेंट देने वाले का नाम" else "प्राप्तकर्ता / विक्रेता") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Text("भुगतान का माध्यम:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(paymentModes) { pm ->
                            FilterChip(
                                selected = paymentMode == pm,
                                onClick = { paymentMode = pm },
                                label = { Text(pm.substringBefore(" ("), fontSize = 12.sp) }
                            )
                        }
                    }
                }

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
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("विवरण / टिप्पणी") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountVal = amountStr.toDoubleOrNull() ?: 0.0
                    if (amountVal > 0) {
                        val tx = ChurchAccountTransaction(
                            type = type,
                            category = category,
                            amount = amountVal,
                            donorOrRecipient = donorOrRecipient.trim(),
                            paymentMode = paymentMode,
                            dateString = dateString.trim(),
                            notes = notes.trim()
                        )
                        onSave(tx)
                    }
                },
                enabled = (amountStr.toDoubleOrNull() ?: 0.0) > 0
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

private fun formatAmount(amount: Double): String {
    return NumberFormat.getNumberInstance(Locale("en", "IN")).format(amount)
}
