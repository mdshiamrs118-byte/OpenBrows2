package com.openbrows.app.utils

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.openbrows.app.R

class BrowserWebViewClient(
    private val onPageStartedCallback: (String?) -> Unit,
    private val onPageFinishedCallback: (String?) -> Unit
) : WebViewClient() {

    private val webSchemes = setOf("http", "https", "about", "data", "blob", "javascript", "file")

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val webView = view ?: return false
        val uri = request?.url ?: return false
        val scheme = uri.scheme?.lowercase()

        // Let the WebView handle normal web navigation (keeps POST, headers, history).
        if (scheme == null || scheme in webSchemes) return false

        // Everything else (mailto:, tel:, intent:, market:, ...) goes to other apps.
        try {
            val intent = if (scheme == "intent") {
                Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME).apply {
                    addCategory(Intent.CATEGORY_BROWSABLE)
                    component = null
                    selector = null
                }
            } else {
                Intent(Intent.ACTION_VIEW, uri)
            }
            webView.context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(webView.context, R.string.cannot_open_link, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(webView.context, R.string.cannot_open_link, Toast.LENGTH_SHORT).show()
        }
        return true
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        onPageStartedCallback(url)
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        onPageFinishedCallback(url)
    }
}
