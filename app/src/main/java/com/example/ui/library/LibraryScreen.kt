package com.example.ui.library

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.DocumentEntity
import com.example.data.database.PromptTemplateEntity
import com.example.data.repository.AIPreferences
import com.example.data.repository.ReadingPreferences
import com.example.ui.ai.AIAssistantSheet
import com.example.ui.ai.AIProviderSettingsDialog
import com.example.ui.ai.PromptLibrarySheet
import com.example.ui.reader.ReadingPreferencesSheet
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    documents: List<DocumentEntity>,
    prompts: List<PromptTemplateEntity>,
    readingPreferences: ReadingPreferences,
    aiPreferences: AIPreferences,
    onOpenDocument: (DocumentEntity) -> Unit,
    onNewDocument: (title: String) -> Unit,
    onImportUri: (Uri) -> Unit,
    onDeleteDocument: (DocumentEntity) -> Unit,
    onToggleFavorite: (DocumentEntity) -> Unit,
    onSaveNewPrompt: (PromptTemplateEntity) -> Unit,
    onUpdateReadingPreferences: (ReadingPreferences) -> Unit,
    onUpdateAIPreferences: (AIPreferences) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") } // "All", "Favorites"
    var sortBy by remember { mutableStateOf("Recent") } // "Recent", "Title", "Size"

    var showNewDocDialog by remember { mutableStateOf(false) }
    var newDocTitle by remember { mutableStateOf("") }

    var showAISettings by remember { mutableStateOf(false) }
    var showPromptLibrary by remember { mutableStateOf(false) }
    var showAIAssistant by remember { mutableStateOf(false) }
    var showPreferencesSheet by remember { mutableStateOf(false) }

    // SAF file picker launcher for .md and text files
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { onImportUri(it) }
    }

    // Filter & Sort
    val filteredDocs = documents
        .filter { doc ->
            (selectedFilter == "All" || (selectedFilter == "Favorites" && doc.isFavorite)) &&
            (searchQuery.isBlank() || doc.title.contains(searchQuery, ignoreCase = true) || doc.content.contains(searchQuery, ignoreCase = true))
        }
        .sortedWith { a, b ->
            when (sortBy) {
                "Title" -> a.title.compareTo(b.title, ignoreCase = true)
                "Size" -> b.content.length.compareTo(a.content.length)
                else -> b.lastOpenedTimestamp.compareTo(a.lastOpenedTimestamp)
            }
        }

    val totalWords = remember(documents) {
        documents.sumOf { doc ->
            doc.content.split("\\s+".toRegex()).filter { it.isNotBlank() }.size
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                actions = {
                    IconButton(onClick = { showAIAssistant = true }) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "AI Assistant", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { showPromptLibrary = true }) {
                        Icon(Icons.Default.Description, contentDescription = "Prompt Library")
                    }
                    IconButton(onClick = { showPreferencesSheet = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Preferences")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showNewDocDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New Document", fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Hero Studio Overview Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                                    Color(0xFF3B5DE7)
                                )
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Document Studio",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "${documents.size} Documents • $totalWords Words",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Text(
                            text = "Distraction-free Markdown reading, editing, KaTeX math, Mermaid diagrams, and PDF export.",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                        )

                        // Quick action shortcuts
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { showNewDocDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.weight(1f).height(38.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("New Note", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    filePickerLauncher.launch(
                                        arrayOf("text/markdown", "text/x-markdown", "text/plain", "*/*")
                                    )
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = Color.White.copy(alpha = 0.25f),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.weight(1f).height(38.dp)
                            ) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Open File", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by title or content…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            )

            // Filter Chips & Sort
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("All", "Favorites").forEach { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(if (filter == "All") "All Documents" else "Favorites") },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            sortBy = when (sortBy) {
                                "Recent" -> "Title"
                                "Title" -> "Size"
                                else -> "Recent"
                            }
                        }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Sort,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Sort: $sortBy",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Document Cards List
            if (filteredDocs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Text(
                            text = "No documents found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (searchQuery.isNotEmpty()) "Try adjusting your search terms" else "Create a new note or open an existing Markdown file",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredDocs, key = { it.id }) { doc ->
                        DocumentItemCard(
                            document = doc,
                            onClick = { onOpenDocument(doc) },
                            onToggleFavorite = { onToggleFavorite(doc) },
                            onDelete = { onDeleteDocument(doc) },
                            modifier = Modifier.animateItem()
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    // New Document Dialog
    if (showNewDocDialog) {
        AlertDialog(
            onDismissRequest = { showNewDocDialog = false },
            title = { Text("New Markdown Document") },
            text = {
                OutlinedTextField(
                    value = newDocTitle,
                    onValueChange = { newDocTitle = it },
                    label = { Text("Document Title") },
                    placeholder = { Text("e.g. Distributed Systems Architecture") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val t = if (newDocTitle.isNotBlank()) newDocTitle.trim() else "Untitled Document"
                        onNewDocument(t)
                        newDocTitle = ""
                        showNewDocDialog = false
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewDocDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialogs & Sheets
    if (showAISettings) {
        AIProviderSettingsDialog(
            preferences = aiPreferences,
            onSavePreferences = onUpdateAIPreferences,
            onDismiss = { showAISettings = false }
        )
    }

    if (showPromptLibrary) {
        PromptLibrarySheet(
            prompts = prompts,
            onSelectPrompt = { prompt ->
                showPromptLibrary = false
                showAIAssistant = true
            },
            onSaveNewPrompt = onSaveNewPrompt,
            onDismiss = { showPromptLibrary = false }
        )
    }

    if (showAIAssistant) {
        AIAssistantSheet(
            documentContent = "",
            selectedText = "",
            aiPreferences = aiPreferences,
            onOpenAISettings = {
                showAIAssistant = false
                showAISettings = true
            },
            onOpenPromptLibrary = {
                showAIAssistant = false
                showPromptLibrary = true
            },
            onApplyGeneratedMarkdown = { _, _ -> },
            onSaveAsNewNote = { title, content ->
                onNewDocument(title)
            },
            onDismiss = { showAIAssistant = false }
        )
    }

    if (showPreferencesSheet) {
        ReadingPreferencesSheet(
            preferences = readingPreferences,
            onPreferencesChanged = onUpdateReadingPreferences,
            onDismiss = { showPreferencesSheet = false }
        )
    }
}

@Composable
fun DocumentItemCard(
    document: DocumentEntity,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.ENGLISH) }
    val wordCount = remember(document.content) {
        document.content.split("\\s+".toRegex()).filter { it.isNotBlank() }.size
    }
    val readingTimeMin = (wordCount / 200).coerceAtLeast(1)

    val docTypeTag = remember(document) {
        when {
            document.coverStyle == "technical" -> "SPEC"
            document.coverStyle == "academic" -> "ACADEMIC"
            document.coverStyle == "modern" -> "STUDY"
            document.coverStyle == "executive" -> "EXECUTIVE"
            document.content.contains("```mermaid") -> "DIAGRAM"
            document.content.contains("$$") -> "MATH"
            else -> "DOC"
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = docTypeTag,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = document.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            if (document.isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Favorite",
                            tint = if (document.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Options")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Open in Reader") },
                                onClick = {
                                    showMenu = false
                                    onClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete Document") },
                                onClick = {
                                    showMenu = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }

            // Snippet preview
            val snippet = remember(document.content) {
                document.content
                    .replace("#", "")
                    .replace("*", "")
                    .replace("`", "")
                    .replace("[", "")
                    .replace("]", "")
                    .replace("$", "")
                    .take(130)
                    .trim()
            }
            if (snippet.isNotBlank()) {
                Text(
                    text = snippet,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Metadata row (Reading progress, word count, date)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${dateFormat.format(Date(document.lastModified))} • $wordCount words",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline
                )

                Text(
                    text = if (document.readingProgress > 0f) "${document.readingProgress.toInt()}% read • ~$readingTimeMin min" else "~$readingTimeMin min read",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (document.readingProgress > 0f) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { document.readingProgress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.5.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}
