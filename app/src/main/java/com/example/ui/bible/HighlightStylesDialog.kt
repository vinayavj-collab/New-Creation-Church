package com.example.ui.bible

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.bible.model.ReadingPlanHighlightStyle

private val PRESET_FILL_COLORS = listOf(
    "#FDE68A" to "Amber Gold",
    "#A7F3D0" to "Mint Green",
    "#BAE6FD" to "Sky Blue",
    "#FECDD3" to "Rose Pink",
    "#DDD6FE" to "Soft Lavender",
    "#FED7AA" to "Sunset Peach",
    "#A5F3FC" to "Aqua Cyan",
    "#E2E8F0" to "Silver Slate"
)

private val PRESET_STROKE_COLORS = listOf(
    "#D97706" to "Deep Amber",
    "#059669" to "Emerald",
    "#0284C7" to "Ocean Blue",
    "#E11D48" to "Crimson",
    "#7C3AED" to "Royal Purple",
    "#EA580C" to "Terracotta",
    "#0891B2" to "Teal",
    "#475569" to "Dark Slate"
)

@Composable
fun HighlightStylesDialog(
    currentStyle: ReadingPlanHighlightStyle,
    onDismissRequest: () -> Unit,
    onSaveStyle: (ReadingPlanHighlightStyle) -> Unit
) {
    var isVisible by remember { mutableStateOf(currentStyle.isVisible) }
    var fillColorHex by remember { mutableStateOf(currentStyle.windowFillColorHex) }
    var strokeColorHex by remember { mutableStateOf(currentStyle.strokeColorHex) }
    var alpha by remember { mutableFloatStateOf(currentStyle.alpha) }
    var borderThicknessDp by remember { mutableFloatStateOf(currentStyle.borderThicknessDp) }
    var cornerRadiusDp by remember { mutableFloatStateOf(currentStyle.cornerRadiusDp) }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "हाइलाइट शैलियाँ",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Highlight Styles (Reading Plan Window)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismissRequest) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Scrollable Controls
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // Live Preview Card
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "👀 लाइव पूर्वावलोकन (Live Preview):",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            val parsedFillColor = try {
                                Color(android.graphics.Color.parseColor(fillColorHex))
                            } catch (_: Exception) {
                                Color(0xFFFDE68A)
                            }
                            val parsedStrokeColor = try {
                                Color(android.graphics.Color.parseColor(strokeColorHex))
                            } catch (_: Exception) {
                                Color(0xFFD97706)
                            }

                            // Sample Verse Text with DrawBehind Window Overlay
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .drawBehind {
                                        if (isVisible) {
                                            val cRadiusPx = cornerRadiusDp.dp.toPx()
                                            val strokePx = borderThicknessDp.dp.toPx()
                                            drawRoundRect(
                                                color = parsedFillColor.copy(alpha = alpha),
                                                topLeft = Offset.Zero,
                                                size = size,
                                                cornerRadius = CornerRadius(cRadiusPx, cRadiusPx)
                                            )
                                            if (strokePx > 0f) {
                                                drawRoundRect(
                                                    color = parsedStrokeColor.copy(alpha = (alpha * 1.5f).coerceIn(0f, 1f)),
                                                    topLeft = Offset(strokePx / 2f, strokePx / 2f),
                                                    size = Size(
                                                        (size.width - strokePx).coerceAtLeast(0f),
                                                        (size.height - strokePx).coerceAtLeast(0f)
                                                    ),
                                                    cornerRadius = CornerRadius(cRadiusPx, cRadiusPx),
                                                    style = Stroke(width = strokePx)
                                                )
                                            }
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = "¹⁶ क्योंकि परमेश्वर ने जगत से ऐसा प्रेम रखा कि उसने अपना एकलौता पुत्र दे दिया, ताकि जो कोई उस पर विश्वास करे वह नाश न हो, परन्तु अनन्त जीवन पाए।",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = FontFamily.Serif,
                                        lineHeight = 22.sp
                                    )
                                )
                            }
                        }
                    }

                    // 1. Visibility Toggle
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "विंडो ओवरले दृश्यता (Visibility)",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = "दैनिक पठन असाइनमेंट पर रंगीन बॉक्स दिखाएं",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isVisible,
                                onCheckedChange = { isVisible = it }
                            )
                        }
                    }

                    // 2. Window Fill Color Picker
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "विंडो भरने का रंग (Window Fill Color)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(
                                        try { Color(android.graphics.Color.parseColor(fillColorHex)) } catch (_: Exception) { Color.Yellow }
                                    )
                                    .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            )
                        }

                        // Presets Swatches
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            PRESET_FILL_COLORS.take(4).forEach { (hex, _) ->
                                val color = Color(android.graphics.Color.parseColor(hex))
                                val isSelected = fillColorHex.equals(hex, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(color)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { fillColorHex = hex },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            PRESET_FILL_COLORS.drop(4).forEach { (hex, _) ->
                                val color = Color(android.graphics.Color.parseColor(hex))
                                val isSelected = fillColorHex.equals(hex, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(color)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { fillColorHex = hex },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }

                    // 3. Outer Line / Stroke Color Picker
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "बाहरी बॉर्डर / स्ट्रोक रंग (Border Color)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(
                                        try { Color(android.graphics.Color.parseColor(strokeColorHex)) } catch (_: Exception) { Color.DarkGray }
                                    )
                                    .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            )
                        }

                        // Presets Swatches
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            PRESET_STROKE_COLORS.take(4).forEach { (hex, _) ->
                                val color = Color(android.graphics.Color.parseColor(hex))
                                val isSelected = strokeColorHex.equals(hex, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(color)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { strokeColorHex = hex },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            PRESET_STROKE_COLORS.drop(4).forEach { (hex, _) ->
                                val color = Color(android.graphics.Color.parseColor(hex))
                                val isSelected = strokeColorHex.equals(hex, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(color)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { strokeColorHex = hex },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }

                    // 4. Transparency / Alpha Slider
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "पारदर्शिता (Transparency / Alpha)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = "${(alpha * 100).toInt()}%",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = alpha,
                            onValueChange = { alpha = it },
                            valueRange = 0.05f..1.0f,
                            steps = 19
                        )
                    }

                    // 5. Border Thickness Slider
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "बॉर्डर मोटाई (Border Thickness)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = "%.1f dp".format(borderThicknessDp),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = borderThicknessDp,
                            onValueChange = { borderThicknessDp = it },
                            valueRange = 0.5f..6.0f,
                            steps = 11
                        )
                    }

                    // 6. Corner Radius Slider
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "कोनों का घुमाव (Corner Radius)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = "${cornerRadiusDp.toInt()} dp",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = cornerRadiusDp,
                            onValueChange = { cornerRadiusDp = it },
                            valueRange = 0.0f..24.0f,
                            steps = 24
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            val defaultStyle = ReadingPlanHighlightStyle()
                            isVisible = defaultStyle.isVisible
                            fillColorHex = defaultStyle.windowFillColorHex
                            strokeColorHex = defaultStyle.strokeColorHex
                            alpha = defaultStyle.alpha
                            borderThicknessDp = defaultStyle.borderThicknessDp
                            cornerRadiusDp = defaultStyle.cornerRadiusDp
                        }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("डिफ़ॉल्ट रीसेट")
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onDismissRequest) {
                            Text("रद्द करें")
                        }
                        Button(
                            onClick = {
                                val updated = ReadingPlanHighlightStyle(
                                    isVisible = isVisible,
                                    windowFillColorHex = fillColorHex,
                                    strokeColorHex = strokeColorHex,
                                    alpha = alpha,
                                    borderThicknessDp = borderThicknessDp,
                                    cornerRadiusDp = cornerRadiusDp
                                )
                                onSaveStyle(updated)
                                onDismissRequest()
                            }
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("सहेजें व लागू करें")
                        }
                    }
                }
            }
        }
    }
}
