package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.bible.model.BibleQuizItem

@Composable
fun DailyBibleQuizCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val quiz = remember { BibleQuizItem.getQuizForToday() }

    var selectedOptionIndex by remember { mutableStateOf<Int?>(null) }
    var isSubmitted by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header row
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
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Help,
                                    contentDescription = "Quiz Icon",
                                    tint = MaterialTheme.colorScheme.onSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "❓ आज का बाइबिल क्विज़",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "प्रत्येक दिन 1 बहुविकल्पीय प्रश्न (MCQ)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.padding(2.dp)
                    ) {
                        Text(
                            text = quiz.category,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Question Text
                Text(
                    text = quiz.questionHindi,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Options list
                quiz.optionsHindi.forEachIndexed { index, optionText ->
                    val isSelected = selectedOptionIndex == index
                    val isCorrect = index == quiz.correctIndex

                    val containerColor = when {
                        isSubmitted && isCorrect -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                        isSubmitted && isSelected && !isCorrect -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
                        isSelected -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                        else -> MaterialTheme.colorScheme.surface
                    }

                    val borderColor = when {
                        isSubmitted && isCorrect -> MaterialTheme.colorScheme.primary
                        isSubmitted && isSelected && !isCorrect -> MaterialTheme.colorScheme.error
                        isSelected -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(enabled = !isSubmitted) {
                                selectedOptionIndex = index
                                isSubmitted = true
                            },
                        shape = RoundedCornerShape(10.dp),
                        color = containerColor,
                        tonalElevation = if (isSelected) 2.dp else 0.dp
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
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSubmitted && isCorrect) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${('A' + index)}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSubmitted && isCorrect) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Text(
                                    text = optionText,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (isSubmitted) {
                                if (isCorrect) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Correct",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                } else if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Cancel,
                                        contentDescription = "Incorrect",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Explanation & Reference when submitted
                if (isSubmitted) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Explanation",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "स्पष्टीकरण (${quiz.reference}):",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = quiz.explanationHindi,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(8.dp))

                // Bottom row with Reset/Share
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSubmitted) {
                        TextButton(
                            onClick = {
                                selectedOptionIndex = null
                                isSubmitted = false
                            },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = "फिर से प्रयास करें (Reset)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    } else {
                        Text(
                            text = "उत्तर चुनने के लिए विकल्प पर क्लिक करें",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = {
                            val shareIntent = android.content.Intent().apply {
                                action = android.content.Intent.ACTION_SEND
                                type = "text/plain"
                                putExtra(
                                    android.content.Intent.EXTRA_TEXT,
                                    "❓ आज का बाइबिल क्विज़:\n\n\"${quiz.questionHindi}\"\n\nसंदर्भ: ${quiz.reference}\n\n- New Creation App से साझा किया गया"
                                )
                            }
                            context.startActivity(android.content.Intent.createChooser(shareIntent, "क्विज़ साझा करें"))
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Quiz",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
