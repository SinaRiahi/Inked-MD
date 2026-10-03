package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ReadingPreferences(
    val theme: String = "light", // light, dark, oled
    val fontFamily: String = "Inter",
    val fontSizePt: Int = 13,
    val lineHeight: Float = 1.65f,
    val margin: String = "medium", // compact, medium, wide
    val dirMode: String = "auto" // auto, rtl, ltr
)

data class AIPreferences(
    val selectedProvider: String = "gemini", // gemini, deepseek, openai, custom
    val geminiApiKey: String = "",
    val deepseekApiKey: String = "",
    val openaiApiKey: String = "",
    val customBaseUrl: String = "https://api.openai.com/v1/",
    val customApiKey: String = "",
    val customModel: String = "gpt-4o-mini"
)

class PreferencesRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("inked_md_prefs", Context.MODE_PRIVATE)

    private val _readingPrefs = MutableStateFlow(loadReadingPreferences())
    val readingPrefs: StateFlow<ReadingPreferences> = _readingPrefs.asStateFlow()

    private val _aiPrefs = MutableStateFlow(loadAIPreferences())
    val aiPrefs: StateFlow<AIPreferences> = _aiPrefs.asStateFlow()

    private fun loadReadingPreferences(): ReadingPreferences {
        return ReadingPreferences(
            theme = prefs.getString("theme", "light") ?: "light",
            fontFamily = prefs.getString("fontFamily", "Inter") ?: "Inter",
            fontSizePt = prefs.getInt("fontSizePt", 13),
            lineHeight = prefs.getFloat("lineHeight", 1.65f),
            margin = prefs.getString("margin", "medium") ?: "medium",
            dirMode = prefs.getString("dirMode", "auto") ?: "auto"
        )
    }

    private fun loadAIPreferences(): AIPreferences {
        return AIPreferences(
            selectedProvider = prefs.getString("ai_provider", "gemini") ?: "gemini",
            geminiApiKey = prefs.getString("gemini_api_key", "") ?: "",
            deepseekApiKey = prefs.getString("deepseek_api_key", "") ?: "",
            openaiApiKey = prefs.getString("openai_api_key", "") ?: "",
            customBaseUrl = prefs.getString("custom_base_url", "https://api.openai.com/v1/") ?: "https://api.openai.com/v1/",
            customApiKey = prefs.getString("custom_api_key", "") ?: "",
            customModel = prefs.getString("custom_model", "gpt-4o-mini") ?: "gpt-4o-mini"
        )
    }

    fun updateReadingPreferences(updated: ReadingPreferences) {
        prefs.edit()
            .putString("theme", updated.theme)
            .putString("fontFamily", updated.fontFamily)
            .putInt("fontSizePt", updated.fontSizePt)
            .putFloat("lineHeight", updated.lineHeight)
            .putString("margin", updated.margin)
            .putString("dirMode", updated.dirMode)
            .apply()
        _readingPrefs.value = updated
    }

    fun updateAIPreferences(updated: AIPreferences) {
        prefs.edit()
            .putString("ai_provider", updated.selectedProvider)
            .putString("gemini_api_key", updated.geminiApiKey)
            .putString("deepseek_api_key", updated.deepseekApiKey)
            .putString("openai_api_key", updated.openaiApiKey)
            .putString("custom_base_url", updated.customBaseUrl)
            .putString("custom_api_key", updated.customApiKey)
            .putString("custom_model", updated.customModel)
            .apply()
        _aiPrefs.value = updated
    }
}
