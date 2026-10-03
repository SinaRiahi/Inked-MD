package com.example.ui.renderer

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.repository.ReadingPreferences
import org.json.JSONObject

class MDStudioWebController {
    var webView: WebView? = null
    var isEngineReady: Boolean = false
    private var pendingMarkdown: String? = null
    private var pendingCoverJson: String? = null
    private var pendingPrefs: ReadingPreferences? = null

    fun setMarkdown(markdown: String, coverConfigJson: String) {
        pendingMarkdown = markdown
        pendingCoverJson = coverConfigJson
        val target = webView ?: return
        val escapedMd = JSONObject.quote(markdown)
        val escapedCover = if (coverConfigJson.isNotBlank()) JSONObject.quote(coverConfigJson) else "null"
        val script = "if (window.setMarkdown) { window.setMarkdown($escapedMd, $escapedCover); }"
        target.post {
            target.evaluateJavascript(script, null)
        }
    }

    fun setReadingPreferences(prefs: ReadingPreferences) {
        pendingPrefs = prefs
        val target = webView ?: return
        val script = "if (window.setReadingPreferences) { window.setReadingPreferences('${prefs.theme}', '${prefs.fontFamily}', ${prefs.fontSizePt}, ${prefs.lineHeight}); }"
        target.post {
            target.evaluateJavascript(script, null)
        }
    }

    fun applyPendingContent() {
        val target = webView ?: return
        pendingPrefs?.let { setReadingPreferences(it) }
        pendingMarkdown?.let { md ->
            val cv = pendingCoverJson ?: ""
            val escapedMd = JSONObject.quote(md)
            val escapedCover = if (cv.isNotBlank()) JSONObject.quote(cv) else "null"
            val script = "if (window.setMarkdown) { window.setMarkdown($escapedMd, $escapedCover); }"
            target.post {
                target.evaluateJavascript(script, null)
            }
        }
    }

    fun scrollToHeading(id: String) {
        val script = "if (window.scrollToHeading) { window.scrollToHeading('$id'); }"
        webView?.evaluateJavascript(script, null)
    }

    fun search(query: String, forward: Boolean) {
        val escapedQ = JSONObject.quote(query)
        val script = "if (window.searchInDocument) { window.searchInDocument($escapedQ, $forward); }"
        webView?.evaluateJavascript(script, null)
    }

    fun clearSearch() {
        val script = "if (window.clearSearch) { window.clearSearch(); }"
        webView?.evaluateJavascript(script, null)
    }

    fun scrollToTop() {
        webView?.evaluateJavascript("window.scrollTo({ top: 0, behavior: 'smooth' });", null)
    }

    fun scrollToBottom() {
        webView?.evaluateJavascript("window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' });", null)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MDStudioWebView(
    markdownContent: String,
    coverConfig: JSONObject?,
    readingPreferences: ReadingPreferences,
    controller: MDStudioWebController = remember { MDStudioWebController() },
    onReadingProgress: (percent: Float, isScrollingDown: Boolean) -> Unit = { _, _ -> },
    onTocReady: (List<TocItem>) -> Unit = {},
    onCopyCode: (String) -> Unit = {},
    onImageClick: (src: String, alt: String) -> Unit = { _, _ -> },
    onMermaidClick: (svgHtml: String, diagramSource: String) -> Unit = { _, _ -> },
    onSearchCount: (count: Int, current: Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val bridge = remember {
        InkedBridge(
            onProgress = onReadingProgress,
            onTocReady = onTocReady,
            onCopyCode = onCopyCode,
            onImageTapped = onImageClick,
            onMermaidTapped = onMermaidClick,
            onSearchCount = onSearchCount,
            onReady = {
                controller.isEngineReady = true
                controller.applyPendingContent()
            }
        )
    }

    val coverJsonStr = remember(coverConfig) { coverConfig?.toString() ?: "" }

    LaunchedEffect(markdownContent, coverJsonStr, readingPreferences) {
        controller.setReadingPreferences(readingPreferences)
        controller.setMarkdown(markdownContent, coverJsonStr)
    }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.apply {
                    javaScriptEnabled = true
                    allowFileAccess = true
                    allowContentAccess = true
                    domStorageEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    builtInZoomControls = false
                    displayZoomControls = false
                }

                addJavascriptInterface(bridge, "InkedBridge")

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        controller.webView = view
                        controller.isEngineReady = true
                        controller.applyPendingContent()
                    }

                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        val url = request?.url?.toString() ?: return false
                        if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("mailto:")) {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // ignore
                            }
                            return true
                        }
                        return false
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                        Log.d("MDStudioWebView", "${consoleMessage?.message()} [${consoleMessage?.sourceId()}:${consoleMessage?.lineNumber()}]")
                        return true
                    }
                }

                controller.webView = this
                loadUrl("file:///android_asset/mdstudio/index.html")
            }
        },
        update = { view ->
            controller.webView = view
            if (controller.isEngineReady) {
                controller.applyPendingContent()
            }
        },
        modifier = modifier.fillMaxSize()
    )
}
