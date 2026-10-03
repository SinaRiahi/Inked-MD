package com.example.ui.pdf

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView

object PdfExportManager {

    fun exportToPdf(context: Context, webView: WebView, documentTitle: String) {
        try {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            if (printManager == null) return

            val jobName = "${documentTitle.replace("[^a-zA-Z0-9._-]".toRegex(), "_")}_MDStudio"
            val printAdapter = webView.createPrintDocumentAdapter(jobName)

            val attributes = PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .setResolution(PrintAttributes.Resolution("pdf_res", "PDF Resolution", 300, 300))
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .build()

            printManager.print(jobName, printAdapter, attributes)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
