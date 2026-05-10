package com.knowlily.browser.viewmodel

import android.app.Application
import android.webkit.CookieManager
import android.webkit.WebStorage
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.knowlily.browser.plugin.BrowserPlugin
import com.knowlily.browser.plugin.PluginManager
import com.knowlily.browser.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private val settingsRepo: SettingsRepository,
    private val pluginManager: PluginManager
) : AndroidViewModel(application) {

    val themeMode: LiveData<Int> = settingsRepo.themeMode
    val userAgentMode: LiveData<String> = settingsRepo.userAgentMode
    val accentColor: LiveData<Int> = settingsRepo.accentColor
    val plugins = MutableLiveData<List<BrowserPlugin>>(pluginManager.getPlugins())

    fun setTheme(mode: Int) {
        settingsRepo.setThemeMode(mode)
    }

    fun setUserAgent(mode: String) {
        settingsRepo.setUserAgentMode(mode)
    }

    fun setAccentColor(color: Int) {
        settingsRepo.setAccentColor(color)
    }

    fun clearCache(context: android.content.Context) {
        WebStorage.getInstance().deleteAllData()
        val cm = CookieManager.getInstance()
        cm.removeAllCookies(null)
        cm.flush()
        context.deleteDatabase("webview.db")
        context.deleteDatabase("webviewCache.db")
        context.cacheDir.deleteRecursively()
    }

    fun installPlugin(json: String): Result<String> {
        val result = pluginManager.installPlugin(json)
        result.onSuccess { refreshPlugins() }
        return result
    }

    fun uninstallPlugin(id: String): Boolean {
        val result = pluginManager.uninstallUserPlugin(id)
        if (result) refreshPlugins()
        return result
    }

    fun togglePlugin(id: String, enabled: Boolean) {
        if (enabled) pluginManager.enablePlugin(id) else pluginManager.disablePlugin(id)
        refreshPlugins()
    }

    fun isBuiltinPlugin(id: String): Boolean = pluginManager.isBuiltinPlugin(id)

    fun isHttpsOnly(): Boolean = settingsRepo.isHttpsOnly()

    fun setHttpsOnly(enabled: Boolean) {
        settingsRepo.setHttpsOnly(enabled)
    }

    fun refreshPlugins() {
        plugins.value = pluginManager.getPlugins()
    }
}
