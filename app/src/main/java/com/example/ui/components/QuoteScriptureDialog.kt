package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.bible.model.BibleTranslation
import com.example.data.bible.repository.StudyNotesRepository
import com.example.util.VerseReferenceDetector
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuoteScriptureDialog(
    studyNotesRepository: StudyNotesRepository,
    onDismiss: () -> Unit,
    onScriptureEmbed: (htmlBlock: String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var referenceInput by remember { mutableStateOf("") }
    var selectedTranslationId by remember { mutableStateOf("HIOV") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val quickPresets = listOf(
        "यूहन्ना 3:16",
        "रोमियों 8:28",
        "भजन संहिता 23:1-6",
        "फिलिप्पियों 4:13",
        "यशायाह 40:31",
        "मत्ती 28:19-20",
        "1 कुरिन्थियों 13:4-8",
        "नीतिवचन 3:5-6",
        "यहोशू 1:9",
        "इब्रानियों 11:1"
    )

    fun executeEmbed() {
        val trimmed = referenceInput.trim()
        if (trimmed.isEmpty()) {
            errorMessage = "कृपया बाइबल संदर्भ दर्ज करें (उदा. John 3:16)"
            return
        }

        val detected = VerseReferenceDetector.parseSingleReference(trimmed)
            ?: VerseReferenceDetector.detectVerseReferences(trimmed).firstOrNull()

        if (detected == null) {
            errorMessage = "संदर्भ पार्स नहीं हो सका। कृपया 'यूहन्ना 3:16' या 'John 3:16' जैसा प्रारूप लिखें।"
            return
        }

        isLoading = true
        errorMessage = null

        coroutineScope.launch {
            try {
                val result = studyNotesRepository.fetchScriptureText(
                    bookId = detected.bookId,
                    chapter = detected.chapter,
                    startVerse = detected.startVerse,
                    endVerse = detected.endVerse,
                    translationId = selectedTranslationId
                )

                isLoading = false
                if (result != null) {
                    val html = studyNotesRepository.formatScriptureHtmlBlock(
                        referenceLabel = result.first,
                        scriptureText = result.second
                    )
                    onScriptureEmbed(html)
                    Toast.makeText(context, "📖 ${result.first} एम्बेड किया गया!", Toast.LENGTH_SHORT).show()
                    onDismiss()
                } else {
                    errorMessage = "डेटाबेस में वचन नहीं मिला। कृपया संदर्भ की पुनः जांच करें।"
                }
            } catch (e: Exception) {
                isLoading = false
                errorMessage = "त्रुटि: ${e.localizedMessage ?: "वचन प्राप्त करने में विफल"}"
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Title Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.AutoStories,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "पवित्र वचन एम्बेड करें (Auto-Embed)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Reference दर्ज करें और सीधे सुंदर ब्लॉक में जोड़ें",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bible Reference Input
                OutlinedTextField(
                    value = referenceInput,
                    onValueChange = {
                        referenceInput = it
                        errorMessage = null
                    },
                    label = { Text("बाइबल संदर्भ (Bible Reference)") },
                    placeholder = { Text("उदा. John 3:16, यूहन्ना 3:16, भजन 23:1-6") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (referenceInput.isNotEmpty()) {
                            IconButton(onClick = { referenceInput = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { executeEmbed() }),
                    isError = errorMessage != null
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.error, fontSize = 12.sp),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Translation Selector
                Text(
                    text = "अनुवाद चुनें (Select Version):",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "HIOV" to "हिन्दी (HIOV)",
                        "ENG_ESV" to "English (ESV)"
                    ).forEach { (id, label) ->
                        FilterChip(
                            selected = selectedTranslationId == id,
                            onClick = { selectedTranslationId = id },
                            label = { Text(label, fontSize = 11.sp) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Quick Reference Suggestion Chips
                Text(
                    text = "त्वरित संदर्भ (Quick Presets):",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickPresets.forEach { preset ->
                        SuggestionChip(
                            onClick = {
                                referenceInput = preset
                                errorMessage = null
                            },
                            label = { Text(preset, fontSize = 11.sp) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("रद्द करें")
                    }

                    Button(
                        onClick = { executeEmbed() },
                        enabled = !isLoading && referenceInput.isNotBlank(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.weight(1.4f)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("खोज रहे हैं...", fontSize = 12.sp)
                        } else {
                            Icon(Icons.Default.AutoStories, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("वचन एम्बेड करें", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                        }
                    }
                }
            }
        }
    }
}
