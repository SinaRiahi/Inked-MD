package com.example.ui.renderer

import android.webkit.JavascriptInterface

data class TocItem(
    val level: Int,
    val title: String,
    val id: String
)

class InkedBridge(
    private val onProgress: (percent: Float, isScrollingDown: Boolean) -> Unit,
    private val onTocReady: (List<TocItem>) -> Unit,
    private val onCopyCode: (String) -> Unit,
    private val onImageTapped: (src: String, alt: String) -> Unit,
    private val onMermaidTapped: (svgHtml: String, diagramSource: String) -> Unit,
    private val onSearchCount: (count: Int, current: Int) -> Unit,
    private val onReady: () -> Unit
) {

    @JavascriptInterface
    fun onReadingProgress(percent: Float, isScrollingDown: Boolean) {
        onProgress(percent, isScrollingDown)
    }

    @JavascriptInterface
    fun onTableOfContentsReady(jsonToc: String) {
        try {
            val list = mutableListOf<TocItem>()
            val jsonArray = org.json.JSONArray(jsonToc)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    TocItem(
                        level = obj.getInt("level"),
                        title = obj.getString("title"),
                        id = obj.getString("id")
                    )
                )
            }
            onTocReady(list)
        } catch (e: Exception) {
            onTocReady(emptyList())
        }
    }

    @JavascriptInterface
    fun onCodeCopy(codeText: String) {
        onCopyCode(codeText)
    }

    @JavascriptInterface
    fun onImageClick(src: String, alt: String) {
        onImageTapped(src, alt)
    }

    @JavascriptInterface
    fun onMermaidClick(svgHtml: String, diagramSource: String) {
        onMermaidTapped(svgHtml, diagramSource)
    }

    @JavascriptInterface
    fun onSearchMatchesCount(count: Int, currentIndex: Int) {
        onSearchCount(count, currentIndex)
    }

    @JavascriptInterface
    fun onEngineReady() {
        onReady()
    }
}
