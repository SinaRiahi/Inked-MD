package com.example.ui.editor

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EditorToolbar(
    onInsertText: (prefix: String, suffix: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCalloutsMenu by remember { mutableStateOf(false) }
    var showMermaidMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Headings
            TextButton(onClick = { onInsertText("# ", "") }) { Text("H1", fontWeight = FontWeight.Bold) }
            TextButton(onClick = { onInsertText("## ", "") }) { Text("H2", fontWeight = FontWeight.Bold) }
            TextButton(onClick = { onInsertText("### ", "") }) { Text("H3", fontWeight = FontWeight.Bold) }

            // Bold & Italic
            IconButton(onClick = { onInsertText("**", "**") }) {
                Icon(Icons.Default.FormatBold, contentDescription = "Bold", modifier = Modifier.height(20.dp))
            }
            IconButton(onClick = { onInsertText("*", "*") }) {
                Icon(Icons.Default.FormatItalic, contentDescription = "Italic", modifier = Modifier.height(20.dp))
            }

            // Callout Dropdown
            FilledTonalButton(onClick = { showCalloutsMenu = true }) {
                Text("Callout ▼", fontSize = 12.sp)
            }
            DropdownMenu(
                expanded = showCalloutsMenu,
                onDismissRequest = { showCalloutsMenu = false }
            ) {
                listOf(
                    "definition" to "📘 Definition",
                    "important" to "⚠️ Important",
                    "example" to "💡 Example",
                    "tip" to "✨ Tip",
                    "warning" to "🚨 Warning",
                    "note" to "📝 Note",
                    "exam" to "🎯 Exam Key",
                    "reminder" to "🔔 Reminder",
                    "secondary" to "📌 Secondary"
                ).forEach { (tag, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            onInsertText("\n[$tag]\n", "\n[/$tag]\n")
                            showCalloutsMenu = false
                        }
                    )
                }
            }

            // KaTeX Math
            IconButton(onClick = { onInsertText("$", "$") }) {
                Text("$", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            IconButton(onClick = { onInsertText("\n$$\n", "\n$$\n") }) {
                Icon(Icons.Default.Functions, contentDescription = "Display Math", modifier = Modifier.height(20.dp))
            }

            // Mermaid Diagrams
            FilledTonalButton(onClick = { showMermaidMenu = true }) {
                Text("Diagram ▼", fontSize = 12.sp)
            }
            DropdownMenu(
                expanded = showMermaidMenu,
                onDismissRequest = { showMermaidMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Flowchart (graph TD)") },
                    onClick = {
                        val template = "\n```mermaid\ngraph TD\n    A[Start] --> B(Process)\n    B --> C{Decision}\n    C -->|Yes| D[Success]\n    C -->|No| E[Retry]\n```\n"
                        onInsertText(template, "")
                        showMermaidMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Sequence Diagram") },
                    onClick = {
                        val template = "\n```mermaid\nsequenceDiagram\n    participant Client\n    participant Server\n    Client->>Server: Request\n    Server-->>Client: Response 200 OK\n```\n"
                        onInsertText(template, "")
                        showMermaidMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Pie Chart") },
                    onClick = {
                        val template = "\n```mermaid\npie title Resource Allocation\n    \"CPU\" : 45\n    \"Memory\" : 35\n    \"Disk\" : 20\n```\n"
                        onInsertText(template, "")
                        showMermaidMenu = false
                    }
                )
            }

            // Code Block
            IconButton(onClick = { onInsertText("\n```kotlin\n", "\n```\n") }) {
                Icon(Icons.Default.Code, contentDescription = "Code Block", modifier = Modifier.height(20.dp))
            }

            // Table Template
            IconButton(onClick = {
                val tableTmpl = "\n| Header 1 | Header 2 | Header 3 |\n| :--- | :---: | ---: |\n| Cell 1 | Cell 2 | Cell 3 |\n| Cell 4 | Cell 5 | Cell 6 |\n"
                onInsertText(tableTmpl, "")
            }) {
                Icon(Icons.Default.TableChart, contentDescription = "Insert Table", modifier = Modifier.height(20.dp))
            }

            // Task List
            IconButton(onClick = { onInsertText("- [ ] ", "") }) {
                Icon(Icons.AutoMirrored.Filled.FormatListBulleted, contentDescription = "Task List", modifier = Modifier.height(20.dp))
            }

            // Page Break
            TextButton(onClick = { onInsertText("\n---pagebreak---\n", "") }) {
                Text("PageBreak", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            // Footnote
            IconButton(onClick = { onInsertText("[^1]", "\n\n[^1]: Reference note") }) {
                Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = "Footnote", modifier = Modifier.height(20.dp))
            }
        }
    }
}
