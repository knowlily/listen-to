package com.knowlily.browser.plugin

import android.webkit.WebView

interface BrowserPlugin {
    val id: String
    val name: String
    val description: String
    val version: String
    var isEnabled: Boolean

    fun onInit() {}
    fun onDestroy() {}
    fun onUrlLoading(url: String): String? = null
    fun onPageFinished(webView: WebView, url: String) {}
    fun onPageStarted(webView: WebView, url: String) {}
    fun injectJavaScript(): String? = null
}
