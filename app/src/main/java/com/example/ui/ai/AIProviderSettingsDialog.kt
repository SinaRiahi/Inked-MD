package com.example.ui.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.AIPreferences

@Composable
fun AIProviderSettingsDialog(
    preferences: AIPreferences,
    onSavePreferences: (AIPreferences) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedProvider by remember { mutableStateOf(preferences.selectedProvider) }
    var geminiKey by remember { mutableStateOf(preferences.geminiApiKey) }
    var deepseekKey by remember { mutableStateOf(preferences.deepseekApiKey) }
    var openaiKey by remember { mutableStateOf(preferences.openaiApiKey) }
    var customBaseUrl by remember { mutableStateOf(preferences.customBaseUrl) }
    var customKey by remember { mutableStateOf(preferences.customApiKey) }
    var customModel by remember { mutableStateOf(preferences.customModel) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("AI Provider Settings") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Select Default AI Provider:",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("gemini" to "Gemini", "deepseek" to "DeepSeek").forEach { (p, label) ->
                        FilterChip(
                            selected = selectedProvider == p,
                            onClick = { selectedProvider = p },
                            label = { Text(label) }
                        )
                    }
                    listOf("openai" to "OpenAI", "custom" to "Custom").forEach { (p, label) ->
                        FilterChip(
                            selected = selectedProvider == p,
                            onClick = { selectedProvider = p },
                            label = { Text(label) }
                        )
                    }
                }

                when (selectedProvider) {
                    "gemini" -> {
                        OutlinedTextField(
                            value = geminiKey,
                            onValueChange = { geminiKey = it },
                            label = { Text("Gemini API Key (Optional if pre-configured)") },
                            placeholder = { Text("AI Studio or custom key") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "Uses Gemini 3.5 Flash for rapid, high-quality Markdown document generation.",
                            fontSize = 12.sp,
                            color = androidx.compose.ui.graphics.Color.Gray
                        )
                    }
                    "deepseek" -> {
                        OutlinedTextField(
                            value = deepseekKey,
                            onValueChange = { deepseekKey = it },
                            label = { Text("DeepSeek API Key") },
                            placeholder = { Text("sk-...") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "Uses DeepSeek V3 (deepseek-chat) via direct REST API.",
                            fontSize = 12.sp,
                            color = androidx.compose.ui.graphics.Color.Gray
                        )
                    }
                    "openai" -> {
                        OutlinedTextField(
                            value = openaiKey,
                            onValueChange = { openaiKey = it },
                            label = { Text("OpenAI API Key") },
                            placeholder = { Text("sk-...") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "Uses GPT-4o-mini via OpenAI API.",
                            fontSize = 12.sp,
                            color = androidx.compose.ui.graphics.Color.Gray
                        )
                    }
                    "custom" -> {
                        OutlinedTextField(
                            value = customBaseUrl,
                            onValueChange = { customBaseUrl = it },
                            label = { Text("Base URL") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = customKey,
                            onValueChange = { customKey = it },
                            label = { Text("API Key") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = customModel,
                            onValueChange = { customModel = it },
                            label = { Text("Model Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSavePreferences(
                    preferences.copy(
                        selectedProvider = selectedProvider,
                        geminiApiKey = geminiKey,
                        deepseekApiKey = deepseekKey,
                        openaiApiKey = openaiKey,
                        customBaseUrl = customBaseUrl,
                        customApiKey = customKey,
                        customModel = customModel
                    )
                )
                onDismiss()
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
