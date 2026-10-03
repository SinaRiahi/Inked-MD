package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val uriString: String? = null,
    val lastModified: Long = System.currentTimeMillis(),
    val lastOpenedTimestamp: Long = System.currentTimeMillis(),
    val readingProgress: Float = 0f,
    val isFavorite: Boolean = false,
    val coverStyle: String = "none", // none, classic, modern, academic, executive, technical
    val coverSubtitle: String = "",
    val coverAuthor: String = "",
    val coverDate: String = "",
    val coverStatus: String = "FINAL",
    val coverEyebrow: String = "MD STUDIO REPORT"
)
