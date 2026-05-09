package com.knowlily.browser.viewmodel

import android.app.Application
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.webkit.WebSettings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import com.knowlily.browser.plugin.AdBlockerPlugin
import com.knowlily.browser.plugin.DarkModePlugin
import com.knowlily.browser.plugin.PluginManager
import com.knowlily.browser.repository.HistoryRepository
import com.knowlily.browser.repository.SettingsRepository

class BrowserViewModel(application: Application) : AndroidViewModel(application) {

    val currentUrl = MutableLiveData("")
    val isLoading = MutableLiveData(false)
    val loadProgress = MutableLiveData(0)
    val canGoBack = MutableLiveData(false)
    val canGoForward = MutableLiveData(false)
    val isBottomNavVisible = MutableLiveData(true)

    var isWebViewConfigured = false

    private val settingsRepo = SettingsRepository(application)
    private val historyRepo = HistoryRepository(application)
    val pluginManager: PluginManager

    val accentColor: LiveData<Int> = settingsRepo.accentColor

    val userAgent: LiveData<String> = settingsRepo.userAgentMode.map { mode ->
        val defaultUA = WebSettings.getDefaultUserAgent(application)
        when (mode) {
            "pc" -> "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 KnowlilyBrowser/1.9"
            else -> "$defaultUA KnowlilyBrowser/1.9"
        }
    }

    init {
        val pm = PluginManager.getInstance(application)
        pm.registerPlugin(AdBlockerPlugin())
        pm.registerPlugin(DarkModePlugin())
        pm.loadUserPlugins()
        pluginManager = pm
    }

    fun loadUrl(rawUrl: String) {
        var url = rawUrl.trim()
        if (url.isEmpty()) return

        // Add https:// if no scheme
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            if (url.contains(".") && !url.contains(" ")) {
                url = "https://$url"
            } else {
                url = "https://www.bing.com/search?q=${java.net.URLEncoder.encode(url, "UTF-8")}"
            }
        }

        // Check network
        val cm = getApplication<Application>().getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork
        val capabilities = network?.let { cm.getNetworkCapabilities(it) }
        if (capabilities == null || !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            currentUrl.value = "about:blank"
            return
        }

        // Plugin URL interception
        val finalUrl = pluginManager.notifyUrlLoading(url)
        currentUrl.value = finalUrl
    }

    fun goBack() { /* handled by WebView in Fragment */ }
    fun goForward() { /* handled by WebView in Fragment */ }
    fun reload() { /* handled by WebView in Fragment */ }

    fun onPageFinished(url: String) {
        if (url != "about:blank") {
            historyRepo.save(url)
        }
    }

    fun updateProgress(progress: Int) {
        loadProgress.value = progress
        isLoading.value = progress < 100
    }
}
