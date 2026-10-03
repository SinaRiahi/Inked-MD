package com.example.data.repository

import com.example.data.database.PromptDao
import com.example.data.database.PromptTemplateEntity
import com.example.data.samples.SampleDocuments
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class PromptRepository(
    private val promptDao: PromptDao
) {
    val allPrompts: Flow<List<PromptTemplateEntity>> = promptDao.getAllPrompts()

    suspend fun initializePromptsIfNeeded() = withContext(Dispatchers.IO) {
        val initial = SampleDocuments.getInitialPrompts()
        if (promptDao.getPromptCount() == 0) {
            promptDao.insertAll(initial)
        } else {
            initial.filter { it.isBuiltIn }.forEach { p ->
                val existing = promptDao.getPromptByTitle(p.title)
                if (existing == null) {
                    promptDao.insertPrompt(p)
                }
            }
        }
    }

    suspend fun savePrompt(prompt: PromptTemplateEntity): Long = withContext(Dispatchers.IO) {
        if (prompt.id == 0L) {
            promptDao.insertPrompt(prompt)
        } else {
            promptDao.updatePrompt(prompt)
            prompt.id
        }
    }

    suspend fun deletePrompt(prompt: PromptTemplateEntity) = withContext(Dispatchers.IO) {
        promptDao.deletePrompt(prompt)
    }
}
