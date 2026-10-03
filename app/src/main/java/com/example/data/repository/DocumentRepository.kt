package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.database.DocumentDao
import com.example.data.database.DocumentEntity
import com.example.data.samples.SampleDocuments
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

class DocumentRepository(
    private val context: Context,
    private val documentDao: DocumentDao
) {
    val allDocuments: Flow<List<DocumentEntity>> = documentDao.getAllDocuments()

    suspend fun initializeSamplesIfNeeded() = withContext(Dispatchers.IO) {
        val samples = SampleDocuments.getInitialDocuments() // Contains only "Welcome to Inked MD"
        val existing = documentDao.getAllDocumentsList()

        // Remove old prebuilt documents as requested
        existing.forEach { doc ->
            if (doc.title == "Computer Networks: Transport Layer" || doc.title == "Distributed Systems & Cloud Architecture") {
                documentDao.deleteDocument(doc)
            }
        }

        val welcomeSample = samples.firstOrNull() ?: return@withContext
        val welcomeDoc = existing.find { it.title == welcomeSample.title }
        if (welcomeDoc == null) {
            documentDao.insertDocument(welcomeSample)
        } else if (welcomeDoc.content != welcomeSample.content) {
            documentDao.updateDocument(welcomeDoc.copy(content = welcomeSample.content))
        }
    }

    suspend fun getDocumentById(id: Long): DocumentEntity? = withContext(Dispatchers.IO) {
        documentDao.getDocumentById(id)
    }

    suspend fun saveDocument(document: DocumentEntity): Long = withContext(Dispatchers.IO) {
        // If document has a SAF Uri, also attempt to persist content back to Uri
        document.uriString?.let { uriStr ->
            try {
                val uri = Uri.parse(uriStr)
                context.contentResolver.openOutputStream(uri, "wt")?.use { stream ->
                    stream.write(document.content.toByteArray(Charsets.UTF_8))
                }
            } catch (e: Exception) {
                // If SAF write fails, local DB copy still protects user data
            }
        }
        if (document.id == 0L) {
            documentDao.insertDocument(document)
        } else {
            documentDao.updateDocument(document)
            document.id
        }
    }

    suspend fun updateReadingProgress(id: Long, progress: Float) = withContext(Dispatchers.IO) {
        documentDao.updateReadingProgress(id, progress)
    }

    suspend fun toggleFavorite(id: Long, currentVal: Boolean) = withContext(Dispatchers.IO) {
        documentDao.updateFavorite(id, !currentVal)
    }

    suspend fun deleteDocument(document: DocumentEntity) = withContext(Dispatchers.IO) {
        documentDao.deleteDocument(document)
    }

    suspend fun importFromUri(uri: Uri): Long = withContext(Dispatchers.IO) {
        try {
            // Take persistable permission if possible
            try {
                val takeFlags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (ignored: Exception) {
            }

            // Extract file name
            var fileName = "Imported Document"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) {
                    fileName = cursor.getString(nameIndex) ?: fileName
                }
            }
            if (fileName.endsWith(".md", true) || fileName.endsWith(".markdown", true) || fileName.endsWith(".txt", true)) {
                fileName = fileName.substringBeforeLast(".")
            }

            // Read content
            val stringBuilder = StringBuilder()
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        stringBuilder.append(line).append("\n")
                    }
                }
            }

            val content = stringBuilder.toString()
            val existing = documentDao.getDocumentByUri(uri.toString())
            if (existing != null) {
                val updated = existing.copy(
                    content = content,
                    lastOpenedTimestamp = System.currentTimeMillis()
                )
                documentDao.updateDocument(updated)
                existing.id
            } else {
                val doc = DocumentEntity(
                    title = fileName,
                    content = content,
                    uriString = uri.toString(),
                    lastModified = System.currentTimeMillis(),
                    lastOpenedTimestamp = System.currentTimeMillis()
                )
                documentDao.insertDocument(doc)
            }
        } catch (e: Exception) {
            -1L
        }
    }
}
