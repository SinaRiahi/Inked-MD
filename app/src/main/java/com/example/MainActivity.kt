package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.AppDatabase
import com.example.data.database.DocumentEntity
import com.example.data.repository.DocumentRepository
import com.example.data.repository.PreferencesRepository
import com.example.data.repository.PromptRepository
import com.example.ui.editor.EditorScreen
import com.example.ui.library.LibraryScreen
import com.example.ui.reader.ReaderScreen
import com.example.ui.theme.InkedMDTheme
import kotlinx.coroutines.launch

sealed class Screen {
    object Library : Screen()
    data class Reader(val documentId: Long) : Screen()
    data class Editor(val documentId: Long) : Screen()
}

class MainActivity : ComponentActivity() {

    private lateinit var documentRepository: DocumentRepository
    private lateinit var promptRepository: PromptRepository
    private lateinit var preferencesRepository: PreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = AppDatabase.getDatabase(applicationContext)
        documentRepository = DocumentRepository(applicationContext, db.documentDao())
        promptRepository = PromptRepository(db.promptDao())
        preferencesRepository = PreferencesRepository(applicationContext)

        setContent {
            val readingPrefs by preferencesRepository.readingPrefs.collectAsStateWithLifecycle()
            val aiPrefs by preferencesRepository.aiPrefs.collectAsStateWithLifecycle()
            val documents by documentRepository.allDocuments.collectAsStateWithLifecycle(initialValue = emptyList())
            val prompts by promptRepository.allPrompts.collectAsStateWithLifecycle(initialValue = emptyList())

            val scope = rememberCoroutineScope()
            var currentScreen by remember { mutableStateOf<Screen>(Screen.Library) }

            // Initialize sample documents & prompt library on first launch
            LaunchedEffect(Unit) {
                documentRepository.initializeSamplesIfNeeded()
                promptRepository.initializePromptsIfNeeded()

                // Check for incoming intent (file opening from Telegram, Downloads, etc.)
                handleIncomingIntent(intent) { targetDocId ->
                    if (targetDocId != -1L) {
                        currentScreen = Screen.Reader(targetDocId)
                    }
                }
            }

            InkedMDTheme(themePreference = readingPrefs.theme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            (fadeIn(animationSpec = tween(180, easing = FastOutSlowInEasing)) +
                             scaleIn(initialScale = 0.96f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow)))
                            .togetherWith(
                                fadeOut(animationSpec = tween(140, easing = FastOutSlowInEasing)) +
                                scaleOut(targetScale = 1.02f, animationSpec = tween(140))
                            )
                        },
                        label = "Screen120HzAnimation"
                    ) { screen ->
                        when (screen) {
                            is Screen.Library -> {
                                LibraryScreen(
                                    documents = documents,
                                    prompts = prompts,
                                    readingPreferences = readingPrefs,
                                    aiPreferences = aiPrefs,
                                    onOpenDocument = { doc ->
                                        currentScreen = Screen.Reader(doc.id)
                                    },
                                    onNewDocument = { title ->
                                        scope.launch {
                                            val newDoc = DocumentEntity(
                                                title = title,
                                                content = "# $title\n\nWrite your markdown notes here…\n",
                                                lastModified = System.currentTimeMillis(),
                                                lastOpenedTimestamp = System.currentTimeMillis()
                                            )
                                            val id = documentRepository.saveDocument(newDoc)
                                            currentScreen = Screen.Editor(id)
                                        }
                                    },
                                    onImportUri = { uri ->
                                        scope.launch {
                                            val id = documentRepository.importFromUri(uri)
                                            if (id != -1L) {
                                                currentScreen = Screen.Reader(id)
                                            }
                                        }
                                    },
                                    onDeleteDocument = { doc ->
                                        scope.launch {
                                            documentRepository.deleteDocument(doc)
                                        }
                                    },
                                    onToggleFavorite = { doc ->
                                        scope.launch {
                                            documentRepository.toggleFavorite(doc.id, doc.isFavorite)
                                        }
                                    },
                                    onSaveNewPrompt = { p ->
                                        scope.launch {
                                            promptRepository.savePrompt(p)
                                        }
                                    },
                                    onUpdateReadingPreferences = { updated ->
                                        preferencesRepository.updateReadingPreferences(updated)
                                    },
                                    onUpdateAIPreferences = { updated ->
                                        preferencesRepository.updateAIPreferences(updated)
                                    }
                                )
                            }

                            is Screen.Reader -> {
                                val activeDoc = documents.firstOrNull { it.id == screen.documentId }
                                if (activeDoc != null) {
                                    ReaderScreen(
                                        document = activeDoc,
                                        readingPreferences = readingPrefs,
                                        aiPreferences = aiPrefs,
                                        onEditDocument = {
                                            currentScreen = Screen.Editor(activeDoc.id)
                                        },
                                        onSaveDocument = { updated ->
                                            scope.launch {
                                                documentRepository.saveDocument(updated)
                                            }
                                        },
                                        onReadingPreferencesChanged = { updated ->
                                            preferencesRepository.updateReadingPreferences(updated)
                                        },
                                        onNavigateBack = {
                                            currentScreen = Screen.Library
                                        },
                                        onOpenAISettings = {
                                            // Managed in sheets
                                        },
                                        onOpenPromptLibrary = {
                                            // Managed in sheets
                                        }
                                    )
                                } else {
                                    currentScreen = Screen.Library
                                }
                            }

                            is Screen.Editor -> {
                                val activeDoc = documents.firstOrNull { it.id == screen.documentId }
                                if (activeDoc != null) {
                                    EditorScreen(
                                        initialDocument = activeDoc,
                                        readingPreferences = readingPrefs,
                                        aiPreferences = aiPrefs,
                                        onSaveDocument = { updated ->
                                            scope.launch {
                                                documentRepository.saveDocument(updated)
                                            }
                                        },
                                        onNavigateBack = {
                                            currentScreen = Screen.Reader(activeDoc.id)
                                        },
                                        onOpenAISettings = {
                                            // Managed in sheets
                                        },
                                        onOpenPromptLibrary = {
                                            // Managed in sheets
                                        }
                                    )
                                } else {
                                    currentScreen = Screen.Library
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent) { targetDocId ->
            if (targetDocId != -1L) {
                // Re-trigger opening document
            }
        }
    }

    private fun handleIncomingIntent(intent: Intent?, onDocumentReady: (Long) -> Unit) {
        if (intent == null) return
        when (intent.action) {
            Intent.ACTION_VIEW -> {
                intent.data?.let { uri ->
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                        val id = documentRepository.importFromUri(uri)
                        onDocumentReady(id)
                    }
                }
            }
            Intent.ACTION_SEND -> {
                val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                val streamUri = androidx.core.content.IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                if (streamUri != null) {
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                        val id = documentRepository.importFromUri(streamUri)
                        onDocumentReady(id)
                    }
                } else if (!sharedText.isNullOrBlank()) {
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                        val title = intent.getStringExtra(Intent.EXTRA_SUBJECT)?.take(40) ?: "Shared Document"
                        val doc = DocumentEntity(
                            title = title,
                            content = sharedText,
                            lastModified = System.currentTimeMillis()
                        )
                        val id = documentRepository.saveDocument(doc)
                        onDocumentReady(id)
                    }
                }
            }
        }
    }
}
