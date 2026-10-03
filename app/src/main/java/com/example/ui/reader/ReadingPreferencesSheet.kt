package com.example.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.ReadingPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingPreferencesSheet(
    preferences: ReadingPreferences,
    onPreferencesChanged: (ReadingPreferences) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Reading Preferences",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            HorizontalDivider()

            // Themes (Light / Dark / OLED)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Theme",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val themes = listOf(
                        "light" to "Light" to Color(0xFFFFFFFF),
                        "dark" to "Dark" to Color(0xFF1E293B),
                        "oled" to "OLED" to Color(0xFF000000)
                    )

                    themes.forEach { (pair, bg) ->
                        val (key, label) = pair
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    width = if (preferences.theme == key) 2.5.dp else 1.dp,
                                    color = if (preferences.theme == key) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .background(bg)
                                .clickable { onPreferencesChanged(preferences.copy(theme = key)) },
                            color = bg
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (key == "light") Color.Black else Color.White,
                                    fontWeight = if (preferences.theme == key) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }

            // Typography
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Typography",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val fonts = listOf("Inter", "Roboto", "Outfit", "Merriweather", "Lora", "JetBrains Mono")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    fonts.take(3).forEach { font ->
                        FilterChip(
                            selected = preferences.fontFamily == font,
                            onClick = { onPreferencesChanged(preferences.copy(fontFamily = font)) },
                            label = { Text(font) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    fonts.drop(3).forEach { font ->
                        FilterChip(
                            selected = preferences.fontFamily == font,
                            onClick = { onPreferencesChanged(preferences.copy(fontFamily = font)) },
                            label = { Text(font) }
                        )
                    }
                }
            }

            // Font Size
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Font Size",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${preferences.fontSizePt} pt",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = preferences.fontSizePt.toFloat(),
                    onValueChange = { onPreferencesChanged(preferences.copy(fontSizePt = it.toInt())) },
                    valueRange = 10f..22f,
                    steps = 11
                )
            }

            // Line Height
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Line Height",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format("%.2f", preferences.lineHeight),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = preferences.lineHeight,
                    onValueChange = { onPreferencesChanged(preferences.copy(lineHeight = it)) },
                    valueRange = 1.3f..2.2f
                )
            }
        }
    }
}
