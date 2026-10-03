package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "prompt_templates")
data class PromptTemplateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String,
    val promptTemplate: String,
    val category: String, // "Study", "Analysis", "Editing", "Diagrams", "STEM"
    val isBuiltIn: Boolean = false
)
