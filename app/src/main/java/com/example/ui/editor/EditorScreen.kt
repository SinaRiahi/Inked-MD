package com.example.ui.editor

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.DocumentEntity
import com.example.data.repository.AIPreferences
import com.example.data.repository.ReadingPreferences
import com.example.ui.ai.AIAssistantSheet
import com.example.ui.pdf.PdfExportManager
import com.example.ui.renderer.MDStudioWebController
import com.example.ui.renderer.MDStudioWebView
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    initialDocument: DocumentEntity,
    readingPreferences: ReadingPreferences,
    aiPreferences: AIPreferences,
    onSaveDocument: (DocumentEntity) -> Unit,
    onNavigateBack: () -> Unit,
    onOpenAISettings: () -> Unit,
    onOpenPromptLibrary: () -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(initialDocument.title) }
    var textValue by remember {
        mutableStateOf(TextFieldValue(initialDocument.content, TextRange(initialDocument.content.length)))
    }

    // Undo / Redo history
    val undoStack = remember { mutableStateListOf<String>() }
    val redoStack = remember { mutableStateListOf<String>() }

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Edit, 1: Preview
    var showFindReplace by remember { mutableStateOf(false) }
    var findText by remember { mutableStateOf("") }
    var replaceText by remember { mutableStateOf("") }
    var showAIAssistant by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var showSaveAsDialog by remember { mutableStateOf(false) }
    var saveAsTitle by remember { mutableStateOf(title + " (Copy)") }
    var showMoreMenu by remember { mutableStateOf(false) }

    val webController = remember { MDStudioWebController() }

    // Export to SAF file launcher
    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri, "wt")?.use { stream ->
                    stream.write(textValue.text.toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(context, "Saved to Markdown file successfully", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to export: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val hasUnsavedChanges = textValue.text != initialDocument.content || title != initialDocument.title

    // Document Statistics
    val wordCount = remember(textValue.text) {
        textValue.text.split("\\s+".toRegex()).filter { it.isNotBlank() }.size
    }
    val charCount = textValue.text.length
    val readingTimeMin = (wordCount / 200).coerceAtLeast(1)
    val headingCount = remember(textValue.text) {
        textValue.text.lines().count { it.trimStart().startsWith("#") }
    }

    BackHandler {
        if (hasUnsavedChanges) {
            showExitDialog = true
        } else {
            onNavigateBack()
        }
    }

    fun updateContentWithUndo(newText: String) {
        undoStack.add(textValue.text)
        redoStack.clear()
        textValue = TextFieldValue(newText, TextRange(newText.length))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    BasicTextField(
                        value = title,
                        onValueChange = { title = it },
                        textStyle = TextStyle(
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFamily = FontFamily.SansSerif
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (hasUnsavedChanges) showExitDialog = true else onNavigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Undo
                    IconButton(
                        onClick = {
                            if (undoStack.isNotEmpty()) {
                                val prev = undoStack.removeAt(undoStack.lastIndex)
                                redoStack.add(textValue.text)
                                textValue = TextFieldValue(prev, TextRange(prev.length))
                            }
                        },
                        enabled = undoStack.isNotEmpty()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
                    }

                    // Redo
                    IconButton(
                        onClick = {
                            if (redoStack.isNotEmpty()) {
                                val next = redoStack.removeAt(redoStack.lastIndex)
                                undoStack.add(textValue.text)
                                textValue = TextFieldValue(next, TextRange(next.length))
                            }
                        },
                        enabled = redoStack.isNotEmpty()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo")
                    }

                    // Find & Replace Toggle
                    IconButton(onClick = { showFindReplace = !showFindReplace }) {
                        Icon(Icons.Default.FindReplace, contentDescription = "Find and Replace")
                    }

                    // AI Assistant
                    IconButton(onClick = { showAIAssistant = true }) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "AI Studio", tint = MaterialTheme.colorScheme.primary)
                    }

                    // Save
                    IconButton(onClick = {
                        val updated = initialDocument.copy(
                            title = title,
                            content = textValue.text,
                            lastModified = System.currentTimeMillis()
                        )
                        onSaveDocument(updated)
                        Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.Save, contentDescription = "Save")
                    }

                    // More Menu (Save As, Export .md, Export PDF)
                    IconButton(onClick = { showMoreMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }

                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Save As Copy…") },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = null) },
                            onClick = {
                                showMoreMenu = false
                                showSaveAsDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Export to .md File") },
                            leadingIcon = { Icon(Icons.Default.Save, contentDescription = null) },
                            onClick = {
                                showMoreMenu = false
                                val defaultName = "${title.replace("[^a-zA-Z0-9._-]".toRegex(), "_")}.md"
                                exportFileLauncher.launch(defaultName)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Export to PDF") },
                            leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null) },
                            onClick = {
                                showMoreMenu = false
                                webController.webView?.let { wb ->
                                    PdfExportManager.exportToPdf(context, wb, title)
                                }
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Find & Replace Bar
            if (showFindReplace) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = findText,
                                onValueChange = { findText = it },
                                label = { Text("Find") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = replaceText,
                                onValueChange = { replaceText = it },
                                label = { Text("Replace") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = {
                                if (findText.isNotBlank()) {
                                    val replaced = textValue.text.replace(findText, replaceText)
                                    updateContentWithUndo(replaced)
                                }
                            }) {
                                Text("Replace All")
                            }
                            TextButton(onClick = { showFindReplace = false }) {
                                Text("Close")
                            }
                        }
                    }
                }
            }

            // Contextual AI Actions Bar for Selected Text
            if (textValue.selection.length > 0) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Selection (${textValue.selection.length} chars):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        FilterChip(
                            selected = false,
                            onClick = { showAIAssistant = true },
                            label = { Text("✨ Explain with AI", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = false,
                            onClick = { showAIAssistant = true },
                            label = { Text("✨ Summarize", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = false,
                            onClick = { showAIAssistant = true },
                            label = { Text("✨ Polish & Fix", fontSize = 11.sp) }
                        )
                    }
                }
            }

            BoxWithConstraints(modifier = Modifier.fillMaxSize().weight(1f)) {
                val isTabletWide = maxWidth >= 720.dp

                if (isTabletWide) {
                    // Split screen on large tablets/foldables
                    Row(modifier = Modifier.fillMaxSize()) {
                        // Left: Editor
                        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            EditorToolbar(
                                onInsertText = { prefix, suffix ->
                                    val start = textValue.selection.start
                                    val end = textValue.selection.end
                                    val selected = textValue.text.substring(start, end)
                                    val replacement = prefix + selected + suffix
                                    val newText = textValue.text.replaceRange(start, end, replacement)
                                    updateContentWithUndo(newText)
                                }
                            )
                            OutlinedTextField(
                                value = textValue,
                                onValueChange = { textValue = it },
                                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp),
                                modifier = Modifier.fillMaxSize().padding(8.dp)
                            )
                        }

                        // Right: Live Preview
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            MDStudioWebView(
                                markdownContent = textValue.text,
                                coverConfig = null,
                                readingPreferences = readingPreferences,
                                controller = webController
                            )
                        }
                    }
                } else {
                    // Mobile phones: Tab switcher between EDIT and PREVIEW
                    Column(modifier = Modifier.fillMaxSize()) {
                        TabRow(selectedTabIndex = selectedTab) {
                            Tab(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                text = { Text("EDIT") }
                            )
                            Tab(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                text = { Text("PREVIEW") }
                            )
                        }

                        if (selectedTab == 0) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                EditorToolbar(
                                    onInsertText = { prefix, suffix ->
                                        val start = textValue.selection.start
                                        val end = textValue.selection.end
                                        val selected = textValue.text.substring(start, end)
                                        val replacement = prefix + selected + suffix
                                        val newText = textValue.text.replaceRange(start, end, replacement)
                                        updateContentWithUndo(newText)
                                    }
                                )
                                OutlinedTextField(
                                    value = textValue,
                                    onValueChange = { textValue = it },
                                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp),
                                    modifier = Modifier.fillMaxSize().padding(8.dp)
                                )
                            }
                        } else {
                            MDStudioWebView(
                                markdownContent = textValue.text,
                                coverConfig = null,
                                readingPreferences = readingPreferences,
                                controller = webController
                            )
                        }
                    }
                }
            }

            // Real-time Document Statistics Status Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$wordCount words • $charCount characters • $headingCount headings",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "~$readingTimeMin min read",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }

    // AI Assistant Sheet
    if (showAIAssistant) {
        val selectedText = if (textValue.selection.length > 0) {
            textValue.text.substring(textValue.selection.start, textValue.selection.end)
        } else ""

        AIAssistantSheet(
            documentContent = textValue.text,
            selectedText = selectedText,
            aiPreferences = aiPreferences,
            onOpenAISettings = onOpenAISettings,
            onOpenPromptLibrary = onOpenPromptLibrary,
            onApplyGeneratedMarkdown = { generated, replaceAll ->
                if (replaceAll) {
                    updateContentWithUndo(generated)
                } else {
                    val start = textValue.selection.start
                    val end = textValue.selection.end
                    val newText = if (start < end) {
                        textValue.text.replaceRange(start, end, generated)
                    } else {
                        textValue.text + "\n\n" + generated
                    }
                    updateContentWithUndo(newText)
                }
            },
            onSaveAsNewNote = { newTitle, newContent ->
                val newDoc = DocumentEntity(
                    title = newTitle,
                    content = newContent,
                    lastModified = System.currentTimeMillis()
                )
                onSaveDocument(newDoc)
            },
            onDismiss = { showAIAssistant = false }
        )
    }

    // Save As Dialog
    if (showSaveAsDialog) {
        AlertDialog(
            onDismissRequest = { showSaveAsDialog = false },
            title = { Text("Save As Copy") },
            text = {
                OutlinedTextField(
                    value = saveAsTitle,
                    onValueChange = { saveAsTitle = it },
                    label = { Text("Document Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    val newDoc = DocumentEntity(
                        title = saveAsTitle.ifBlank { "Untitled Copy" },
                        content = textValue.text,
                        lastModified = System.currentTimeMillis()
                    )
                    onSaveDocument(newDoc)
                    showSaveAsDialog = false
                    Toast.makeText(context, "Saved as $saveAsTitle", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Save Copy")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveAsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Unsaved Changes Confirmation Dialog
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Unsaved Changes") },
            text = { Text("You have unsaved changes in this document. Save before exiting?") },
            confirmButton = {
                Button(onClick = {
                    val updated = initialDocument.copy(
                        title = title,
                        content = textValue.text,
                        lastModified = System.currentTimeMillis()
                    )
                    onSaveDocument(updated)
                    showExitDialog = false
                    onNavigateBack()
                }) {
                    Text("Save & Exit")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    onNavigateBack()
                }) {
                    Text("Discard")
                }
            }
        )
    }
}
