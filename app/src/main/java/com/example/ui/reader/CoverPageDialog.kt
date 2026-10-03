package com.example.ui.reader

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
import androidx.compose.ui.unit.dp
import com.example.data.database.DocumentEntity

@Composable
fun CoverPageDialog(
    document: DocumentEntity,
    onSaveCover: (DocumentEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var style by remember { mutableStateOf(document.coverStyle) }
    var subtitle by remember { mutableStateOf(document.coverSubtitle) }
    var author by remember { mutableStateOf(document.coverAuthor) }
    var date by remember { mutableStateOf(document.coverDate) }
    var status by remember { mutableStateOf(document.coverStatus) }
    var eyebrow by remember { mutableStateOf(document.coverEyebrow) }

    val styles = listOf(
        "none" to "None",
        "classic" to "Classic",
        "modern" to "Modern",
        "academic" to "Academic",
        "executive" to "Executive",
        "technical" to "Technical"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cover Page & Metadata") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Select Style:")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    styles.take(3).forEach { (s, label) ->
                        FilterChip(
                            selected = style == s,
                            onClick = { style = s },
                            label = { Text(label) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    styles.drop(3).forEach { (s, label) ->
                        FilterChip(
                            selected = style == s,
                            onClick = { style = s },
                            label = { Text(label) }
                        )
                    }
                }

                if (style != "none") {
                    OutlinedTextField(
                        value = subtitle,
                        onValueChange = { subtitle = it },
                        label = { Text("Subtitle") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = author,
                        onValueChange = { author = it },
                        label = { Text("Author / Department") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("Date") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = eyebrow,
                        onValueChange = { eyebrow = it },
                        label = { Text("Eyebrow / Header Tag") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = status,
                        onValueChange = { status = it },
                        label = { Text("Status (e.g. DRAFT, FINAL, CONFIDENTIAL)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSaveCover(
                    document.copy(
                        coverStyle = style,
                        coverSubtitle = subtitle,
                        coverAuthor = author,
                        coverDate = date,
                        coverStatus = status,
                        coverEyebrow = eyebrow
                    )
                )
                onDismiss()
            }) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
