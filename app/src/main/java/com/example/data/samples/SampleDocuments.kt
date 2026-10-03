package com.example.data.samples

import com.example.data.database.DocumentEntity
import com.example.data.database.PromptTemplateEntity

object SampleDocuments {

    private const val M = "$"

    fun getInitialDocuments(): List<DocumentEntity> {
        val now = System.currentTimeMillis()
        return listOf(
            DocumentEntity(
                title = "Welcome to Inked MD",
                content = """
# Welcome to Inked MD 🖋️

Inked MD is a local-first, distraction-free Markdown studio, reader, editor, and PDF exporter for Android with offline KaTeX mathematical typesetting and Mermaid diagram rendering.

[definition]
**Local-First Architecture:** An approach where all user notes and documents are stored locally on the device, ensuring 100% functionality without mandatory cloud accounts, subscriptions, or network connectivity.
[/definition]

## Core Capabilities

- [x] Full GitHub Flavored Markdown (GFM)
- [x] Complete Offline KaTeX Mathematical Typesetting
- [x] Complete Offline Mermaid Graph & Diagram Engine (All types)
- [x] 9 Custom MD Studio Semantic Callouts
- [x] Real-time In-document Search with active match counter
- [x] Navigable Table of Contents with Unicode-safe anchors
- [x] Vector PDF Export with Cover Pages & Page Breaks

---

## 1. Mathematical Formulas & LaTeX Typesetting

Inked MD supports all mathematical formulas, characters, matrices, and expressions using KaTeX:

[example]
**Inline Mathematics:**
The Planck-Einstein relation states that energy is ${M}E = h \nu${M}, and relativistic energy is ${M}E = m c^2${M}. The circle area formula is ${M}A = \pi r^2${M}.
[/example]

### Display Equations & Calculus

The Gaussian normal distribution probability density function:

${M}${M}
f(x) = \frac{1}{\sigma \sqrt{2\pi}} e^{-\frac{1}{2}\left(\frac{x - \mu}{\sigma}\right)^2}
${M}${M}

Fundamental Theorem of Calculus and Definite Integral:

${M}${M}
\int_{a}^{b} f(x)\,dx = F(b) - F(a) = \lim_{n \to \infty} \sum_{i=1}^{n} f(x_i^*) \Delta x
${M}${M}

### Linear Algebra & Matrices

Rotation transformation matrix in two-dimensional Euclidean space:

${M}${M}
R(\theta) = \begin{pmatrix}
\cos\theta & -\sin\theta \\
\sin\theta & \cos\theta
\end{pmatrix}
\begin{bmatrix}
x \\
y
\end{bmatrix}
=
\begin{bmatrix}
x\cos\theta - y\sin\theta \\
x\sin\theta + y\cos\theta
\end{bmatrix}
${M}${M}

---pagebreak---

## 2. Mermaid Diagrams (All Graph Types)

### A. System Architecture Flowchart

```mermaid
graph TD
    User([User Device]) -->|Markdown Input| Editor[Inked MD Editor]
    Editor --> Parser[GFM AST Parser]
    Parser --> KaTeX[KaTeX Math Engine]
    Parser --> Mermaid[Mermaid Graph Engine]
    KaTeX --> Renderer[Document View]
    Mermaid --> Renderer
    Renderer --> Reader[Distraction-Free Reader]
    Renderer --> PDF[Vector PDF Engine]
```

### B. OAuth 2.0 Authorization Sequence Diagram

```mermaid
sequenceDiagram
    participant Client as Mobile Client
    participant Auth as Identity Provider
    participant API as Resource Server
    Client->>Auth: Authorization Request (PKCE)
    Auth-->>Client: Authorization Code
    Client->>Auth: Exchange Code for Access Token
    Auth-->>Client: Access Token + Refresh Token
    Client->>API: GET /api/v1/documents (Bearer Token)
    API-->>Client: 200 OK (Encrypted Markdown)
```

### C. State Machine Diagram

```mermaid
stateDiagram-v2
    [*] --> Draft: Create Note
    Draft --> InReview: Request Feedback
    InReview --> Approved: Pass Verification
    InReview --> ChangesNeeded: Revisions Flagged
    ChangesNeeded --> Draft: Edit Content
    Approved --> ExportedPDF: Print / Export
    ExportedPDF --> [*]
```

### D. Storage Utilization Pie Chart

```mermaid
pie title Document Storage Allocation
    "Notes & Technical Specs" : 48
    "Mathematical Formulas" : 22
    "Embedded Diagrams" : 18
    "Export Cache" : 12
```

## 3. Semantic Callouts

[important]
Critical warnings and high-priority rules are highlighted using important callouts.
[/important]

[tip]
Tap any Mermaid diagram or image to open the full-screen viewer with fluid pinch-to-zoom and pan gestures!
[/tip]

[exam]
**Exam High-Yield Point:**
Shannon's Channel Capacity Theorem sets the theoretical upper bound on error-free information rate:
${M}${M}
C = B \log_2 \left(1 + \frac{S}{N}\right)
${M}${M}
[/exam]

## 4. Interactive Code Cards

```kotlin
// Inked MD Offline Document Engine
class DocumentEngine(private val context: Context) {
    suspend fun renderMarkdown(source: String): RenderResult {
        return withContext(Dispatchers.Default) {
            val ast = parseGfm(source)
            RenderResult.Success(ast)
        }
    }
}
```
                """.trimIndent(),
                lastModified = now,
                lastOpenedTimestamp = now,
                readingProgress = 0f,
                coverStyle = "modern",
                coverSubtitle = "Complete KaTeX, Mermaid & GFM Guide",
                coverAuthor = "Inked MD Engineering",
                coverDate = "2026",
                coverStatus = "RELEASE",
                coverEyebrow = "PRODUCT SPECIFICATION"
            )
        )
    }

    fun getInitialPrompts(): List<PromptTemplateEntity> {
        return listOf(
            PromptTemplateEntity(
                title = "Technical Documentation (General Doc)",
                description = "Generate publication-quality documentation with chapters, math, Mermaid diagrams, GFM tables, and MD Studio callouts.",
                category = "Documentation",
                isBuiltIn = true,
                promptTemplate = """
Generate clean, professional, publication-quality documentation for:
{{INPUT}}

Document context:
{{SELECTION}}

Requirements:
- Follow the MD Studio technical writer specification strictly.
- Use H1 (#) for chapters, H2 (##) for major sections.
- Keep paragraphs concise, dense, and informative.
- Include KaTeX math (${M}inline${M} or ${M}${M}block${M}${M}) and proper LaTeX matrices where relevant.
- Include focused Mermaid diagrams (```mermaid) that do not require horizontal scrolling.
- Use MD Studio semantic callout boxes ([definition], [important], [example], [tip], [warning], [note], [exam], [reminder], [secondary]) strategically.
- Use ---pagebreak--- between major chapters.
                """.trimIndent()
            ),

            PromptTemplateEntity(
                title = "Create Study Notes",
                description = "Transform any concept, topic, or lecture into structured MD Studio notes with callouts, math, and diagrams.",
                category = "Study",
                isBuiltIn = true,
                promptTemplate = """
Create comprehensive, high-yield study notes about:
{{INPUT}}

Requirements:
- Organize with clear H1, H2, and H3 headings.
- Use MD Studio callouts: [definition] for key terms, [important] for critical rules, [exam] for high-yield exam takeaways, and [example] for real-world illustrations.
- Include KaTeX math (${M}inline${M} or ${M}${M}block${M}${M}) where applicable.
- Include a Mermaid diagram (```mermaid) illustrating the flow or structure.
- Include a summary table and a checklist of review tasks (- [ ]).
                """.trimIndent()
            ),

            PromptTemplateEntity(
                title = "Explain Concept Comprehensively",
                description = "Deep dive explanation with analogies, step-by-step breakdown, and technical accuracy.",
                category = "Study",
                isBuiltIn = true,
                promptTemplate = """
Explain the following concept thoroughly:
{{INPUT}}

Context / Document snippet:
{{SELECTION}}

Requirements:
- Provide an intuitive high-level analogy first.
- Break down the underlying mechanism step-by-step.
- Use [definition] for core terms and [tip] for practical application advice.
- Conclude with a comparison table highlighting trade-offs.
                """.trimIndent()
            ),

            PromptTemplateEntity(
                title = "Summarize Document",
                description = "Generate an executive summary with bullet takeaways, key metrics, and action items.",
                category = "Analysis",
                isBuiltIn = true,
                promptTemplate = """
Please summarize the following document concisely:

{{DOCUMENT}}

Requirements:
- Executive Summary (1 paragraph).
- Top 5 Key Takeaways in a bullet list.
- Use [important] callout for the main conclusion.
- Action items formatted as task lists (- [ ]).
                """.trimIndent()
            ),

            PromptTemplateEntity(
                title = "Generate Mermaid Diagram",
                description = "Create a clean flowchart, sequence diagram, or mindmap for the given process or system.",
                category = "Diagrams",
                isBuiltIn = true,
                promptTemplate = """
Generate an offline Mermaid diagram (```mermaid) representing:
{{INPUT}}

Document context:
{{SELECTION}}

Requirements:
- Use clean node labels and informative edge descriptions.
- Keep the diagram compact and clear on mobile screens.
- Provide a brief 2-sentence explanation preceding the diagram.
                """.trimIndent()
            ),

            PromptTemplateEntity(
                title = "Exam High-Yield Review",
                description = "Extract must-know facts, formulas, tricky edge cases, and probable quiz questions.",
                category = "Study",
                isBuiltIn = true,
                promptTemplate = """
Analyze the following material and create an Exam High-Yield Review sheet:

{{DOCUMENT}}

Requirements:
- Group facts under [exam] callouts.
- Include all essential mathematical formulas using KaTeX (${M}${M}...${M}${M}).
- Add a "Common Traps & Mistakes" section using [warning] callouts.
- Provide 3 sample multiple-choice questions with answer explanations.
                """.trimIndent()
            ),

            PromptTemplateEntity(
                title = "Fix Markdown & Formatting",
                description = "Format messy text into clean GitHub Flavored Markdown with proper headings, lists, and callouts.",
                category = "Editing",
                isBuiltIn = true,
                promptTemplate = """
Review and polish the following text into pristine, beautifully structured MD Studio Markdown:

{{INPUT}}

Requirements:
- Fix heading hierarchies (H1 -> H2 -> H3).
- Wrap code in appropriate language fences.
- Enhance definitions with [definition] or [important] callouts.
- Ensure all tables and task lists are valid GFM.
                """.trimIndent()
            )
        )
    }
}
