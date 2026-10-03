package com.example.data.ai

import com.example.BuildConfig
import com.example.data.repository.AIPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class AIService {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun generate(
        prompt: String,
        prefs: AIPreferences
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            when (prefs.selectedProvider.lowercase()) {
                "deepseek" -> callOpenAICompatible(
                    baseUrl = "https://api.deepseek.com/v1/",
                    apiKey = prefs.deepseekApiKey,
                    model = "deepseek-chat",
                    prompt = prompt
                )
                "openai" -> callOpenAICompatible(
                    baseUrl = "https://api.openai.com/v1/",
                    apiKey = prefs.openaiApiKey,
                    model = "gpt-4o-mini",
                    prompt = prompt
                )
                "custom" -> callOpenAICompatible(
                    baseUrl = if (prefs.customBaseUrl.isNotBlank()) prefs.customBaseUrl else "https://api.openai.com/v1/",
                    apiKey = prefs.customApiKey,
                    model = if (prefs.customModel.isNotBlank()) prefs.customModel else "gpt-4o-mini",
                    prompt = prompt
                )
                else -> {
                    // Default to Gemini (using BuildConfig key or custom key)
                    val apiKey = if (prefs.geminiApiKey.isNotBlank()) prefs.geminiApiKey else BuildConfig.GEMINI_API_KEY
                    if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                        return@withContext Result.failure(
                            Exception("Gemini API Key is not configured. Please enter your API key in AI Settings or the Secrets panel.")
                        )
                    }
                    callGemini(apiKey = apiKey, prompt = prompt)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun callGemini(apiKey: String, prompt: String): Result<String> {
        val model = "gemini-3.5-flash"
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

        val jsonBody = JSONObject().apply {
            val contents = JSONArray().apply {
                put(JSONObject().apply {
                    val parts = JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    }
                    put("parts", parts)
                })
            }
            put("contents", contents)
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.7)
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = httpClient.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            val errorMsg = try {
                val errJson = JSONObject(responseBody)
                errJson.optJSONObject("error")?.optString("message") ?: "HTTP ${response.code}: $responseBody"
            } catch (e: Exception) {
                "HTTP ${response.code}: $responseBody"
            }
            return Result.failure(Exception(errorMsg))
        }

        val json = JSONObject(responseBody)
        val candidates = json.optJSONArray("candidates")
        val candidate = candidates?.optJSONObject(0)
        val content = candidate?.optJSONObject("content")
        val parts = content?.optJSONArray("parts")
        val text = parts?.optJSONObject(0)?.optString("text")

        if (text.isNullOrBlank()) {
            return Result.failure(Exception("Empty response received from Gemini."))
        }

        return Result.success(cleanMarkdownResponse(text))
    }

    private fun callOpenAICompatible(
        baseUrl: String,
        apiKey: String,
        model: String,
        prompt: String
    ): Result<String> {
        if (apiKey.isBlank()) {
            return Result.failure(Exception("API Key for $model is missing. Please configure it in AI Settings."))
        }

        val cleanBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val url = cleanBaseUrl + "chat/completions"

        val jsonBody = JSONObject().apply {
            put("model", model)
            val messages = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", "You are an expert technical writer producing high-quality Markdown for MD Studio.")
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            }
            put("messages", messages)
            put("temperature", 0.7)
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = httpClient.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            val errorMsg = try {
                val errJson = JSONObject(responseBody)
                errJson.optJSONObject("error")?.optString("message") ?: "HTTP ${response.code}: $responseBody"
            } catch (e: Exception) {
                "HTTP ${response.code}: $responseBody"
            }
            return Result.failure(Exception(errorMsg))
        }

        val json = JSONObject(responseBody)
        val choices = json.optJSONArray("choices")
        val choice = choices?.optJSONObject(0)
        val message = choice?.optJSONObject("message")
        val text = message?.optString("content")

        if (text.isNullOrBlank()) {
            return Result.failure(Exception("Empty response from AI service."))
        }

        return Result.success(cleanMarkdownResponse(text))
    }

    private fun cleanMarkdownResponse(raw: String): String {
        var text = raw.trim()
        // Strip accidental outer code fences
        if (text.startsWith("```markdown\n", ignoreCase = true)) {
            text = text.removePrefix("```markdown\n").removePrefix("```Markdown\n")
            if (text.endsWith("```")) {
                text = text.substring(0, text.length - 3).trimEnd()
            }
        } else if (text.startsWith("```\n")) {
            text = text.removePrefix("```\n")
            if (text.endsWith("```")) {
                text = text.substring(0, text.length - 3).trimEnd()
            }
        }
        return text
    }
}
