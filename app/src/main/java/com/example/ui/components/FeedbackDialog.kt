package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.feedback.FeedbackRepository
import com.example.data.feedback.FeedbackType
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FeedbackDialog(
    onDismissRequest: () -> Unit,
    repository: FeedbackRepository? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val feedbackRepo = remember { repository ?: FeedbackRepository(context) }

    var selectedType by remember { mutableStateOf(FeedbackType.BUG_REPORT) }
    var subject by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var userName by remember { mutableStateOf("") }
    var userEmail by remember { mutableStateOf("") }

    var isSubmitting by remember { mutableStateOf(false) }
    var submitSuccess by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = {
            if (!isSubmitting) onDismissRequest()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(vertical = 16.dp)
                .testTag("feedback_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Feedback,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "सुझाव व समस्या रिपोर्ट",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Report Issue / Send Feedback",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        enabled = !isSubmitting,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                if (submitSuccess) {
                    // Success View
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "आपकी प्रतिक्रिया सफलतापूर्वक भेज दी गई है!",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "हमारे ऐप को बेहतर बनाने में मदद करने के लिए धन्यवाद। हमारी टीम जल्द ही इस पर काम करेगी।",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = onDismissRequest,
                            modifier = Modifier.fillMaxWidth(0.6f)
                        ) {
                            Text("ठीक है (Done)")
                        }
                    }
                } else {
                    // Form View
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "प्रतिक्रिया का प्रकार (Category):",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Category Chips
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FeedbackType.entries.forEach { type ->
                                val isSelected = selectedType == type
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedType = type },
                                    label = {
                                        Text(
                                            text = type.titleHindi.substringBefore(" ("),
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = when (type) {
                                                FeedbackType.BUG_REPORT -> Icons.Default.BugReport
                                                FeedbackType.FEATURE_SUGGESTION -> Icons.Default.Lightbulb
                                                FeedbackType.CONTENT_CORRECTION -> Icons.Default.EditNote
                                                FeedbackType.GENERAL_FEEDBACK -> Icons.Default.ThumbUp
                                            },
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Subject
                        OutlinedTextField(
                            value = subject,
                            onValueChange = { subject = it },
                            label = { Text("विषय / शीर्षक (Subject - Optional)") },
                            placeholder = { Text("उदा. बाइबल टेक्स्ट या वीडियो प्ले समस्या") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("feedback_subject_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Message (Required)
                        OutlinedTextField(
                            value = message,
                            onValueChange = {
                                message = it
                                if (errorMessage != null) errorMessage = null
                            },
                            label = { Text("विवरण / संदेश (Details * )") },
                            placeholder = { Text("कृपया अपनी समस्या या सुझाव का विवरण लिखें...") },
                            minLines = 4,
                            maxLines = 8,
                            isError = errorMessage != null,
                            supportingText = {
                                if (errorMessage != null) {
                                    Text(
                                        text = errorMessage ?: "",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                } else {
                                    Text("${message.length} अक्षर")
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("feedback_message_input")
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // User Name (Optional)
                        OutlinedTextField(
                            value = userName,
                            onValueChange = { userName = it },
                            label = { Text("आपका नाम (Name - Optional)") },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Email (Optional)
                        OutlinedTextField(
                            value = userEmail,
                            onValueChange = { userEmail = it },
                            label = { Text("ईमेल / संपर्क (Email - Optional)") },
                            leadingIcon = {
                                Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = onDismissRequest,
                                enabled = !isSubmitting
                            ) {
                                Text("रद्द करें (Cancel)")
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = {
                                    if (message.trim().isBlank()) {
                                        errorMessage = "कृपया संदेश या समस्या का विवरण लिखें"
                                        return@Button
                                    }
                                    isSubmitting = true
                                    errorMessage = null
                                    coroutineScope.launch {
                                        val result = feedbackRepo.submitFeedback(
                                            type = selectedType,
                                            subject = subject.ifBlank { selectedType.titleEnglish },
                                            message = message,
                                            userName = userName,
                                            userEmail = userEmail
                                        )
                                        isSubmitting = false
                                        if (result.isSuccess) {
                                            submitSuccess = true
                                            Toast.makeText(context, "प्रतिक्रिया भेजी गई! धन्यवाद।", Toast.LENGTH_SHORT).show()
                                        } else {
                                            errorMessage = "भेजने में समस्या हुई। कृपया पुनः प्रयास करें।"
                                        }
                                    }
                                },
                                enabled = !isSubmitting,
                                modifier = Modifier.testTag("feedback_submit_button")
                            ) {
                                if (isSubmitting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("भेज रहे हैं...")
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("सबमिट करें (Send)")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
