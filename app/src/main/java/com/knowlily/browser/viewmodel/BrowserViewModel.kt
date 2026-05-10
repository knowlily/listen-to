package com.knowlily.browser.viewmodel

import android.app.Application
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.webkit.WebSettings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.knowlily.browser.model.TabItem
import com.knowlily.browser.plugin.PluginManager
import com.knowlily.browser.repository.HistoryRepository
import com.knowlily.browser.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class BrowserViewModel @Inject constructor(
    application: Application,
    private val settingsRepo: SettingsRepository,
    private val historyRepo: HistoryRepository,
    val pluginManager: PluginManager
) : AndroidViewModel(application) {

    val currentUrl = MutableLiveData("")
    private val _navigateUrl = MutableLiveData("")
    val navigateUrl: LiveData<String> = _navigateUrl
    val isLoading = MutableLiveData(false)
    val loadProgress = MutableLiveData(0)
    val canGoBack = MutableLiveData(false)
    val canGoForward = MutableLiveData(false)
    val isBottomNavVisible = MutableLiveData(true)

    val accentColor: LiveData<Int> = settingsRepo.accentColor

    val userAgent: LiveData<String> = settingsRepo.userAgentMode.map { mode ->
        val defaultUA = WebSettings.getDefaultUserAgent(application)
        when (mode) {
            "pc" -> "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 KnowlilyBrowser/1.9"
            else -> "$defaultUA KnowlilyBrowser/1.9"
        }
    }

    // Tab management
    private var nextTabId = 1
    private val _tabs = MutableLiveData<List<TabItem>>(listOf(TabItem(id = 0, title = "新标签页")))
    val tabs: LiveData<List<TabItem>> = _tabs
    private val _activeTabId = MutableLiveData(0)
    val activeTabId: LiveData<Int> = _activeTabId
    private val tabUrls = mutableMapOf<Int, String>()

    fun addTab(isIncognito: Boolean = false) {
        val list = _tabs.value.orEmpty().toMutableList()
        val tab = TabItem(id = nextTabId++, isIncognito = isIncognito)
        list.add(tab)
        _tabs.value = list
        _activeTabId.value = tab.id
        _navigateUrl.value = "https://www.bing.com"
    }

    fun closeTab(id: Int) {
        val list = _tabs.value.orEmpty().toMutableList()
        if (list.size <= 1) return
        val idx = list.indexOfFirst { it.id == id }
        if (idx < 0) return
        list.removeAt(idx)
        _tabs.value = list
        tabUrls.remove(id)
        if (_activeTabId.value == id) {
            _activeTabId.value = list.last().id
        }
    }

    fun switchToTab(id: Int) {
        val list = _tabs.value.orEmpty()
        val oldId = _activeTabId.value ?: return
        if (oldId == id) return
        // Save current tab URL
        currentUrl.value?.let { tabUrls[oldId] = it }
        _activeTabId.value = id
        // Restore target tab URL
        currentUrl.value = tabUrls[id] ?: ""
    }

    fun updateTabTitle(id: Int, title: String) {
        val list = _tabs.value.orEmpty().toMutableList()
        val idx = list.indexOfFirst { it.id == id }
        if (idx >= 0 && list[idx].title != title) {
            list[idx] = list[idx].copy(title = title)
            _tabs.value = list
        }
    }

    fun updateTabUrl(tabId: Int, url: String) {
        tabUrls[tabId] = url
        if (tabId == _activeTabId.value) {
            currentUrl.value = url
        }
    }

    fun getTabUrl(tabId: Int): String? = tabUrls[tabId]

    fun getActiveTab(): TabItem? {
        val id = _activeTabId.value ?: return null
        return _tabs.value?.find { it.id == id }
    }

    fun isActiveTabIncognito(): Boolean = getActiveTab()?.isIncognito ?: false

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
        _navigateUrl.value = finalUrl
    }

    fun goBack() { /* handled by WebView in Fragment */ }
    fun goForward() { /* handled by WebView in Fragment */ }
    fun reload() { /* handled by WebView in Fragment */ }

    fun onPageFinished(url: String) {
        if (url != "about:blank") {
            viewModelScope.launch { historyRepo.save(url) }
        }
    }

    fun updateProgress(progress: Int) {
        loadProgress.value = progress
        isLoading.value = progress < 100
    }
}
