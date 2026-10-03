package com.example.ui.reader

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.DocumentEntity
import com.example.data.repository.AIPreferences
import com.example.data.repository.ReadingPreferences
import com.example.ui.ai.AIAssistantSheet
import com.example.ui.pdf.PdfExportManager
import com.example.ui.renderer.MDStudioWebController
import com.example.ui.renderer.MDStudioWebView
import com.example.ui.renderer.TocItem
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    document: DocumentEntity,
    readingPreferences: ReadingPreferences,
    aiPreferences: AIPreferences,
    onEditDocument: () -> Unit,
    onSaveDocument: (DocumentEntity) -> Unit,
    onReadingPreferencesChanged: (ReadingPreferences) -> Unit,
    onNavigateBack: () -> Unit,
    onOpenAISettings: () -> Unit,
    onOpenPromptLibrary: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val webController = remember { MDStudioWebController() }

    var isAppBarVisible by remember { mutableStateOf(true) }
    var readingProgress by remember { mutableFloatStateOf(document.readingProgress) }
    var tocItems by remember { mutableStateOf<List<TocItem>>(emptyList()) }

    var showTocSheet by remember { mutableStateOf(false) }
    var showPreferencesSheet by remember { mutableStateOf(false) }
    var showCoverDialog by remember { mutableStateOf(false) }
    var showAIAssistant by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }

    // Search state
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchCount by remember { mutableIntStateOf(0) }
    var currentMatchIndex by remember { mutableIntStateOf(0) }

    // Zoom modal state
    var zoomSvgHtml by remember { mutableStateOf<String?>(null) }
    var zoomTitle by remember { mutableStateOf("Diagram Viewer") }

    val coverConfig = remember(document) {
        if (document.coverStyle != "none") {
            JSONObject().apply {
                put("style", document.coverStyle)
                put("title", document.title)
                put("subtitle", document.coverSubtitle)
                put("author", document.coverAuthor)
                put("date", document.coverDate)
                put("status", document.coverStatus)
                put("eyebrow", document.coverEyebrow)
            }
        } else null
    }

    val wordCount = remember(document.content) {
        document.content.split("\\s+".toRegex()).filter { it.isNotBlank() }.size
    }
    val readingTimeMin = (wordCount / 200).coerceAtLeast(1)

    BackHandler {
        if (isSearchActive) {
            isSearchActive = false
            webController.clearSearch()
        } else {
            onNavigateBack()
        }
    }

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    // Tap anywhere to toggle reading controls
                    isAppBarVisible = !isAppBarVisible
                }
        ) {
            // Main Markdown Document WebView
            MDStudioWebView(
                markdownContent = document.content,
                coverConfig = coverConfig,
                readingPreferences = readingPreferences,
                controller = webController,
                onReadingProgress = { percent, isScrollingDown ->
                    readingProgress = percent
                    if (isScrollingDown && isAppBarVisible) {
                        isAppBarVisible = false
                    } else if (!isScrollingDown && !isAppBarVisible) {
                        isAppBarVisible = true
                    }
                },
                onTocReady = { items ->
                    tocItems = items
                },
                onCopyCode = { code ->
                    clipboardManager.setText(AnnotatedString(code))
                    Toast.makeText(context, "Code copied to clipboard", Toast.LENGTH_SHORT).show()
                },
                onImageClick = { src, alt ->
                    val imgHtml = """<img src="$src" alt="$alt" style="max-width:100%;height:auto;"/>"""
                    zoomSvgHtml = imgHtml
                    zoomTitle = if (alt.isNotBlank()) alt else "Image Preview"
                },
                onMermaidClick = { svgHtml, _ ->
                    zoomSvgHtml = svgHtml
                    zoomTitle = "Mermaid Diagram"
                },
                onSearchCount = { count, current ->
                    searchCount = count
                    currentMatchIndex = current
                }
            )

            // Auto-Hiding Top Bar & Reading Progress
            AnimatedVisibility(
                visible = isAppBarVisible,
                enter = slideInVertically { -it },
                exit = slideOutVertically { -it },
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = document.title,
                                    maxLines = 1,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${readingProgress.toInt()}% read • ~$readingTimeMin min read • $wordCount words",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            // Search Toggle
                            IconButton(onClick = {
                                isSearchActive = !isSearchActive
                                if (!isSearchActive) webController.clearSearch()
                            }) {
                                Icon(Icons.Default.Search, contentDescription = "Search")
                            }

                            // Table of Contents
                            IconButton(onClick = { showTocSheet = true }) {
                                Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Table of Contents")
                            }

                            // Reading Preferences
                            IconButton(onClick = { showPreferencesSheet = true }) {
                                Icon(Icons.Default.FormatSize, contentDescription = "Typography and Theme")
                            }

                            // Edit Document
                            IconButton(onClick = onEditDocument) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Note")
                            }

                            // More Actions Menu
                            IconButton(onClick = { showMoreMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More")
                            }

                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("AI Assistant") },
                                    leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                    onClick = {
                                        showMoreMenu = false
                                        showAIAssistant = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Export PDF") },
                                    leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null) },
                                    onClick = {
                                        showMoreMenu = false
                                        webController.webView?.let { wb ->
                                            PdfExportManager.exportToPdf(context, wb, document.title)
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Cover Page Settings") },
                                    onClick = {
                                        showMoreMenu = false
                                        showCoverDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Scroll to Top") },
                                    onClick = {
                                        showMoreMenu = false
                                        webController.scrollToTop()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Scroll to Bottom") },
                                    onClick = {
                                        showMoreMenu = false
                                        webController.scrollToBottom()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Copy Entire Markdown") },
                                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                    onClick = {
                                        showMoreMenu = false
                                        clipboardManager.setText(AnnotatedString(document.content))
                                        Toast.makeText(context, "Full markdown copied to clipboard", Toast.LENGTH_SHORT).show()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Share Markdown") },
                                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                    onClick = {
                                        showMoreMenu = false
                                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/markdown"
                                            putExtra(Intent.EXTRA_SUBJECT, document.title)
                                            putExtra(Intent.EXTRA_TEXT, document.content)
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "Share Document"))
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (document.isFavorite) "Remove from Favorites" else "Add to Favorites") },
                                    leadingIcon = {
                                        Icon(
                                            if (document.isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        showMoreMenu = false
                                        onSaveDocument(document.copy(isFavorite = !document.isFavorite))
                                    }
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                        )
                    )

                    // Thin Reading Progress Bar
                    LinearProgressIndicator(
                        progress = { readingProgress / 100f },
                        modifier = Modifier.fillMaxWidth().height(2.5.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.Transparent
                    )

                    // In-Document Search Panel
                    if (isSearchActive) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            tonalElevation = 4.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = {
                                        searchQuery = it
                                        webController.search(it, true)
                                    },
                                    placeholder = { Text("Search document…") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                    keyboardActions = KeyboardActions(onSearch = {
                                        webController.search(searchQuery, true)
                                    })
                                )

                                if (searchCount > 0) {
                                    Text(
                                        text = "$currentMatchIndex of $searchCount",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                IconButton(
                                    onClick = { webController.search(searchQuery, false) },
                                    enabled = searchCount > 0
                                ) {
                                    Icon(Icons.Default.ArrowUpward, contentDescription = "Previous Match")
                                }

                                IconButton(
                                    onClick = { webController.search(searchQuery, true) },
                                    enabled = searchCount > 0
                                ) {
                                    Icon(Icons.Default.ArrowDownward, contentDescription = "Next Match")
                                }

                                IconButton(onClick = {
                                    isSearchActive = false
                                    webController.clearSearch()
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close Search")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Table of Contents Sheet
    if (showTocSheet) {
        TableOfContentsSheet(
            tocItems = tocItems,
            isRtl = readingPreferences.dirMode == "rtl",
            onHeadingSelected = { anchorId ->
                webController.scrollToHeading(anchorId)
            },
            onDismiss = { showTocSheet = false }
        )
    }

    // Reading Preferences Sheet
    if (showPreferencesSheet) {
        ReadingPreferencesSheet(
            preferences = readingPreferences,
            onPreferencesChanged = onReadingPreferencesChanged,
            onDismiss = { showPreferencesSheet = false }
        )
    }

    // Cover Page Dialog
    if (showCoverDialog) {
        CoverPageDialog(
            document = document,
            onSaveCover = { updated ->
                onSaveDocument(updated)
            },
            onDismiss = { showCoverDialog = false }
        )
    }

    // Diagram / Image Fullscreen Zoom Modal
    zoomSvgHtml?.let { svg ->
        DiagramZoomModal(
            svgHtml = svg,
            title = zoomTitle,
            onDismiss = { zoomSvgHtml = null }
        )
    }

    // AI Studio Assistant Sheet
    if (showAIAssistant) {
        AIAssistantSheet(
            documentContent = document.content,
            selectedText = "",
            aiPreferences = aiPreferences,
            onOpenAISettings = onOpenAISettings,
            onOpenPromptLibrary = onOpenPromptLibrary,
            onApplyGeneratedMarkdown = { generated, replaceAll ->
                val newContent = if (replaceAll) generated else document.content + "\n\n" + generated
                val updated = document.copy(content = newContent, lastModified = System.currentTimeMillis())
                onSaveDocument(updated)
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
}
