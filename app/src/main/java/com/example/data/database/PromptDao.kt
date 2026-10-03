package com.example.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PromptDao {
    @Query("SELECT * FROM prompt_templates ORDER BY category ASC, title ASC")
    fun getAllPrompts(): Flow<List<PromptTemplateEntity>>

    @Query("SELECT * FROM prompt_templates WHERE id = :id")
    suspend fun getPromptById(id: Long): PromptTemplateEntity?

    @Query("SELECT * FROM prompt_templates WHERE title = :title LIMIT 1")
    suspend fun getPromptByTitle(title: String): PromptTemplateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrompt(prompt: PromptTemplateEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(prompts: List<PromptTemplateEntity>)

    @Update
    suspend fun updatePrompt(prompt: PromptTemplateEntity)

    @Delete
    suspend fun deletePrompt(prompt: PromptTemplateEntity)

    @Query("SELECT COUNT(*) FROM prompt_templates")
    suspend fun getPromptCount(): Int
}
