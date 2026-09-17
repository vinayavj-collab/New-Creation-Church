package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun SimpleTimePickerDialog(
    title: String,
    initialHour: Int,
    initialMinute: Int,
    onTimeSelected: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedHour by remember { mutableIntStateOf(initialHour) }
    var selectedMinute by remember { mutableIntStateOf(initialMinute) }
    var isPm by remember { mutableStateOf(initialHour >= 12) }
    var hour12 by remember { mutableIntStateOf(if (initialHour % 12 == 0) 12 else initialHour % 12) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Time Display
                val formattedTime = String.format(
                    java.util.Locale.US,
                    "%02d:%02d %s",
                    hour12,
                    selectedMinute,
                    if (isPm) "PM" else "AM"
                )

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // AM / PM Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    FilterChip(
                        selected = !isPm,
                        onClick = { isPm = false },
                        label = { Text("AM (सुबह/पूर्वाह्न)") }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = isPm,
                        onClick = { isPm = true },
                        label = { Text("PM (दोपहर/शाम)") }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Hour Quick Grid
                Text(
                    text = "घंटा चुनें (Hour):",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf(5, 6, 7, 8, 9, 10).forEach { h ->
                        InputChip(
                            selected = hour12 == h,
                            onClick = { hour12 = h },
                            label = { Text("$h") }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf(11, 12, 1, 2, 3, 4).forEach { h ->
                        InputChip(
                            selected = hour12 == h,
                            onClick = { hour12 = h },
                            label = { Text("$h") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Minute Quick Grid
                Text(
                    text = "मिनट चुनें (Minute):",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf(0, 15, 30, 45).forEach { m ->
                        InputChip(
                            selected = selectedMinute == m,
                            onClick = { selectedMinute = m },
                            label = { Text(String.format(java.util.Locale.US, "%02d", m)) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("रद्द करें")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val computedHour = if (isPm) {
                                if (hour12 == 12) 12 else hour12 + 12
                            } else {
                                if (hour12 == 12) 0 else hour12
                            }
                            onTimeSelected(computedHour, selectedMinute)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("सेट करें")
                    }
                }
            }
        }
    }
}
