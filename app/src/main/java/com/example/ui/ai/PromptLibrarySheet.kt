package com.example.ui.ai

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.MDStudioPromptSpec
import com.example.data.database.PromptTemplateEntity
import org.json.JSONArray
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromptLibrarySheet(
    prompts: List<PromptTemplateEntity>,
    onSelectPrompt: (PromptTemplateEntity) -> Unit,
    onSaveNewPrompt: (PromptTemplateEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var isCreatingNew by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("All") }

    var newTitle by remember { mutableStateOf("") }
    var newCategory by remember { mutableStateOf("Study") }
    var newDescription by remember { mutableStateOf("") }
    var newTemplate by remember { mutableStateOf("Create notes about {{INPUT}} using MD Studio callouts.") }

    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Prompt Library", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { showExportDialog = true }) {
                        Icon(Icons.Default.Share, contentDescription = "Export Prompts")
                    }
                    IconButton(onClick = { showImportDialog = true }) {
                        Icon(Icons.Default.Download, contentDescription = "Import Prompts")
                    }
                    if (!isCreatingNew) {
                        TextButton(onClick = { isCreatingNew = true }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                            Text("New")
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            if (isCreatingNew) {
                // New Prompt Form
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Create Custom Prompt", fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Prompt Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newDescription,
                        onValueChange = { newDescription = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newTemplate,
                        onValueChange = { newTemplate = it },
                        label = { Text("Prompt Template (Variables: {{INPUT}}, {{DOCUMENT}}, {{SELECTION}})") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { isCreatingNew = false }) { Text("Cancel") }
                        Button(
                            onClick = {
                                if (newTitle.isNotBlank()) {
                                    onSaveNewPrompt(
                                        PromptTemplateEntity(
                                            title = newTitle,
                                            description = newDescription,
                                            category = newCategory,
                                            promptTemplate = newTemplate,
                                            isBuiltIn = false
                                        )
                                    )
                                    isCreatingNew = false
                                }
                            },
                            enabled = newTitle.isNotBlank()
                        ) {
                            Text("Save Prompt")
                        }
                    }
                }
            } else {
                // Category Filter
                val categories = listOf("All", "Study", "Analysis", "Diagrams", "Editing")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat) }
                        )
                    }
                }

                val filtered = if (selectedCategory == "All") prompts else prompts.filter { it.category == selectedCategory }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filtered) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectPrompt(item)
                                    onDismiss()
                                },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = item.title,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = item.category,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Text(
                                    text = item.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    // Copy prompt for external chatbot
                                    IconButton(
                                        onClick = {
                                            val fullPrompt = MDStudioPromptSpec.buildShareableExternalPrompt(
                                                title = item.title,
                                                userQuery = item.promptTemplate
                                            )
                                            clipboardManager.setText(AnnotatedString(fullPrompt))
                                            Toast.makeText(context, "Full prompt copied", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Prompt for External Chatbot", modifier = Modifier.height(18.dp))
                                    }

                                    // Share prompt to another app
                                    IconButton(
                                        onClick = {
                                            val fullPrompt = MDStudioPromptSpec.buildShareableExternalPrompt(
                                                title = item.title,
                                                userQuery = item.promptTemplate
                                            )
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_SUBJECT, item.title)
                                                putExtra(Intent.EXTRA_TEXT, fullPrompt)
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Share Prompt to Chatbot"))
                                        }
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = "Share Prompt", modifier = Modifier.height(18.dp))
                                    }

                                    Button(
                                        onClick = {
                                            onSelectPrompt(item)
                                            onDismiss()
                                        },
                                        modifier = Modifier.padding(start = 6.dp)
                                    ) {
                                        Text("Use", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Export Prompts Dialog
    if (showExportDialog) {
        val exportedJson = remember(prompts) {
            val arr = JSONArray()
            prompts.forEach { p ->
                val obj = JSONObject().apply {
                    put("title", p.title)
                    put("description", p.description)
                    put("category", p.category)
                    put("promptTemplate", p.promptTemplate)
                }
                arr.put(obj)
            }
            arr.toString(2)
        }

        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Export Prompt Library (JSON)") },
            text = {
                Column {
                    Text("Copy this portable JSON backup or share it to transfer prompts:", fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = exportedJson,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 8
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    clipboardManager.setText(AnnotatedString(exportedJson))
                    Toast.makeText(context, "Exported JSON copied to clipboard", Toast.LENGTH_SHORT).show()
                    showExportDialog = false
                }) {
                    Text("Copy JSON")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Import Prompts Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import Prompts from JSON") },
            text = {
                Column {
                    Text("Paste an exported MD Studio prompt JSON array:", fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = { importJsonText = it },
                        placeholder = { Text("[{\"title\": \"...\", \"promptTemplate\": \"...\"}]") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 8
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    try {
                        val arr = JSONArray(importJsonText)
                        var importedCount = 0
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            val title = obj.optString("title", "Imported Prompt")
                            val desc = obj.optString("description", "")
                            val cat = obj.optString("category", "Custom")
                            val tmpl = obj.optString("promptTemplate", "")
                            if (tmpl.isNotBlank()) {
                                onSaveNewPrompt(
                                    PromptTemplateEntity(
                                        title = title,
                                        description = desc,
                                        category = cat,
                                        promptTemplate = tmpl,
                                        isBuiltIn = false
                                    )
                                )
                                importedCount++
                            }
                        }
                        Toast.makeText(context, "Imported $importedCount prompts successfully!", Toast.LENGTH_SHORT).show()
                        showImportDialog = false
                        importJsonText = ""
                    } catch (e: Exception) {
                        Toast.makeText(context, "Invalid JSON format: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
