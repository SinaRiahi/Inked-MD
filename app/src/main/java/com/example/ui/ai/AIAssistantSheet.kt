package com.example.ui.ai

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.AIService
import com.example.data.ai.MDStudioPromptSpec
import com.example.data.database.PromptTemplateEntity
import com.example.data.repository.AIPreferences
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIAssistantSheet(
    documentContent: String,
    selectedText: String = "",
    aiPreferences: AIPreferences,
    onOpenAISettings: () -> Unit,
    onOpenPromptLibrary: () -> Unit,
    onApplyGeneratedMarkdown: (generated: String, replaceAll: Boolean) -> Unit,
    onSaveAsNewNote: (title: String, content: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val aiService = remember { AIService() }

    var userQuery by remember {
        mutableStateOf(if (selectedText.isNotBlank()) selectedText else "")
    }
    var selectedAction by remember { mutableStateOf("Study Notes") }
    var isGenerating by remember { mutableStateOf(false) }
    var generatedResult by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val quickActions = listOf(
        "Study Notes",
        "Explain",
        "Summarize",
        "Exam Review",
        "Mermaid Diagram",
        "Translate",
        "Fix Markdown"
    )

    fun getActionTemplate(action: String): String {
        return when (action) {
            "Study Notes" -> "Create comprehensive study notes about: {{INPUT}}\nUse MD Studio callouts, KaTeX math formulas, and a checklist."
            "Explain" -> "Explain the following in detail with real-world examples: {{INPUT}}\nContext: {{SELECTION}}"
            "Summarize" -> "Summarize the document concisely with key takeaways:\n{{DOCUMENT}}"
            "Exam Review" -> "Extract high-yield exam takeaways using [exam] callouts and key formulas for:\n{{DOCUMENT}}"
            "Mermaid Diagram" -> "Generate an offline Mermaid diagram (```mermaid) illustrating:\n{{INPUT}}"
            "Translate" -> "Translate the following content into fluent, natural English while preserving formatting, callouts, and code:\n{{INPUT}}"
            "Fix Markdown" -> "Fix and enhance the markdown formatting, callouts, and tables for:\n{{INPUT}}"
            else -> "{{INPUT}}"
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
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
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "AI Studio Assistant",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onOpenAISettings) {
                        Icon(Icons.Default.Settings, contentDescription = "AI Settings")
                    }
                }
            }

            // Provider Pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Provider: ${aiPreferences.selectedProvider.replaceFirstChar { it.uppercase() }}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Configure Key ⚙️",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable { onOpenAISettings() }
                )
            }

            // Quick Actions Chips
            Text("Select Action:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                quickActions.take(4).forEach { action ->
                    FilterChip(
                        selected = selectedAction == action,
                        onClick = { selectedAction = action },
                        label = { Text(action, fontSize = 12.sp) }
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                quickActions.drop(4).forEach { action ->
                    FilterChip(
                        selected = selectedAction == action,
                        onClick = { selectedAction = action },
                        label = { Text(action, fontSize = 12.sp) }
                    )
                }
            }

            // User Query Input
            OutlinedTextField(
                value = userQuery,
                onValueChange = { userQuery = it },
                label = { Text("Topic, prompt, or instructions") },
                placeholder = { Text("e.g. TCP congestion control mechanisms") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            // Primary Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        isGenerating = true
                        errorMessage = null
                        scope.launch {
                            val template = getActionTemplate(selectedAction)
                            val fullPrompt = MDStudioPromptSpec.buildFullPrompt(
                                templatePrompt = template,
                                userQuery = userQuery,
                                fullDocument = documentContent,
                                selectionText = selectedText
                            )
                            val result = aiService.generate(fullPrompt, aiPreferences)
                            isGenerating = false
                            result.onSuccess { text ->
                                generatedResult = text
                            }.onFailure { err ->
                                errorMessage = err.message ?: "Failed to generate content."
                            }
                        }
                    },
                    enabled = !isGenerating && userQuery.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.height(18.dp).width(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generating…")
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.height(18.dp).padding(end = 4.dp))
                        Text("Generate with AI")
                    }
                }
            }

            // External Chatbot Workflow Section (Copy & Share Prompt)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "External Chatbot Workflow (No API Key Required)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Copy or share this ready-to-use prompt directly to DeepSeek, ChatGPT, or Claude with full MD Studio rules included.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val template = getActionTemplate(selectedAction)
                                val shareable = MDStudioPromptSpec.buildShareableExternalPrompt(
                                    title = selectedAction,
                                    userQuery = userQuery,
                                    selectionText = selectedText,
                                    fullDocument = documentContent
                                )
                                clipboardManager.setText(AnnotatedString(shareable))
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.height(16.dp).padding(end = 4.dp))
                            Text("Copy Prompt", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                val shareable = MDStudioPromptSpec.buildShareableExternalPrompt(
                                    title = selectedAction,
                                    userQuery = userQuery,
                                    selectionText = selectedText,
                                    fullDocument = documentContent
                                )
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "MD Studio Prompt: $selectedAction")
                                    putExtra(Intent.EXTRA_TEXT, shareable)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Prompt to Chatbot"))
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.height(16.dp).padding(end = 4.dp))
                            Text("Share Prompt", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Error display
            if (errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Result Preview & Review Workflow
            if (generatedResult != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text("Generated Markdown Preview:", fontWeight = FontWeight.Bold)

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = generatedResult ?: "",
                        fontSize = 12.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            generatedResult?.let { onApplyGeneratedMarkdown(it, false) }
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Append / Insert", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            generatedResult?.let { onApplyGeneratedMarkdown(it, true) }
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Replace Doc", fontSize = 12.sp)
                    }
                }

                OutlinedButton(
                    onClick = {
                        val title = userQuery.take(30).trim().ifBlank { "AI Generated Note" }
                        generatedResult?.let { onSaveAsNewNote(title, it) }
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save as New Note 📄")
                }
            }
        }
    }
}
