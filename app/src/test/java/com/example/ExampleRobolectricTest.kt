package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.ai.MDStudioPromptSpec
import com.example.data.repository.AIPreferences
import com.example.data.repository.ReadingPreferences
import com.example.data.samples.SampleDocuments
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Inked MD", appName)
    }

    @Test
    fun `test initial sample documents are populated with rich markdown in English`() {
        val samples = SampleDocuments.getInitialDocuments()
        assertTrue(samples.isNotEmpty())
        val welcomeDoc = samples.firstOrNull { it.title.contains("Welcome") }
        assertNotNull(welcomeDoc)
        assertTrue(welcomeDoc!!.content.contains("[definition]"))
        assertTrue(welcomeDoc.content.contains("```mermaid"))
        assertTrue(welcomeDoc.content.contains("---pagebreak---"))

        // Check that all sample documents are in English
        samples.forEach { doc ->
            assertFalse("Document '${doc.title}' should not contain Persian characters", doc.title.any { it in '\u0600'..'\u06FF' })
        }
    }

    @Test
    fun `test sample documents contain valid KaTeX formulas and Mermaid diagrams`() {
        val samples = SampleDocuments.getInitialDocuments()
        val welcome = samples.first { it.title.contains("Welcome") }
        assertTrue(welcome.content.contains("$$"))
        assertTrue(welcome.content.contains("\\frac"))
        assertTrue(welcome.content.contains("\\int"))
        assertTrue(welcome.content.contains("\\begin{pmatrix}"))
        assertTrue(welcome.content.contains("```mermaid"))
        assertTrue(welcome.content.contains("stateDiagram-v2"))
        assertTrue(welcome.content.contains("pie title"))
    }

    @Test
    fun `test prompt spec generates complete authoring rules`() {
        val prompt = MDStudioPromptSpec.buildFullPrompt(
            templatePrompt = "Analyze {{INPUT}}",
            userQuery = "Operating Systems",
            fullDocument = "Sample doc content"
        )
        assertTrue(prompt.contains("[definition]"))
        assertTrue(prompt.contains("KaTeX"))
        assertTrue(prompt.contains("Mermaid"))
        assertTrue(prompt.contains("Operating Systems"))
    }

    @Test
    fun `test technical documentation specification rules`() {
        val rules = MDStudioPromptSpec.SPECIFICATION_RULES
        assertTrue(rules.contains("You are an expert technical writer"))
        assertTrue(rules.contains("# Heading Structure"))
        assertTrue(rules.contains("# Document Formatting"))
        assertTrue(rules.contains("# Code Blocks"))
        assertTrue(rules.contains("# Mathematics"))
        assertTrue(rules.contains("# Tables"))
        assertTrue(rules.contains("# Diagrams"))
        assertTrue(rules.contains("# MD Studio Semantic Callout Boxes"))
        assertTrue(rules.contains("[definition]"))
        assertTrue(rules.contains("[important]"))
        assertTrue(rules.contains("[exam]"))
        assertTrue(rules.contains("---pagebreak---"))
    }

    @Test
    fun `test reading and ai default preferences`() {
        val readingPrefs = ReadingPreferences()
        assertEquals("light", readingPrefs.theme)
        assertEquals("Inter", readingPrefs.fontFamily)
        assertEquals(13, readingPrefs.fontSizePt)

        val aiPrefs = AIPreferences()
        assertEquals("gemini", aiPrefs.selectedProvider)
    }
}
