package com.openbrows.app.utils

import android.webkit.WebChromeClient
import android.webkit.WebView

class BrowserWebChromeClient(
    private val onProgressChangedCallback: (Int) -> Unit,
    private val onTitleReceivedCallback: (String?) -> Unit
) : WebChromeClient() {

    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        onProgressChangedCallback(newProgress)
    }

    override fun onReceivedTitle(view: WebView?, title: String?) {
        super.onReceivedTitle(view, title)
        onTitleReceivedCallback(title)
    }
}
