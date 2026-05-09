package com.knowlily.browser.plugin

import android.content.Context
import android.webkit.WebView
import android.util.Log

class PluginManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "PluginManager"
        private const val PREFS_NAME = "plugin_settings"

        @Volatile
        private var instance: PluginManager? = null

        fun getInstance(context: Context): PluginManager {
            return instance ?: synchronized(this) {
                instance ?: PluginManager(context.applicationContext).also { instance = it }
            }
        }

        private val BUILTIN_IDS = setOf("adblocker", "darkmode")
    }

    private val plugins = mutableMapOf<String, BrowserPlugin>()
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun registerPlugin(plugin: BrowserPlugin) {
        if (plugins.containsKey(plugin.id)) {
            Log.w(TAG, "插件已注册: ${plugin.id}")
            return
        }
        plugin.isEnabled = prefs.getBoolean("plugin_${plugin.id}", plugin.isEnabled)
        plugins[plugin.id] = plugin
        plugin.onInit()
        Log.d(TAG, "注册插件: ${plugin.id} (${plugin.name}), 启用=${plugin.isEnabled}")
    }

    fun unregisterPlugin(id: String) {
        plugins[id]?.let {
            it.onDestroy()
            plugins.remove(id)
            Log.d(TAG, "卸载插件: $id")
        }
    }

    fun loadUserPlugins() {
        val repo = UserPluginRepository(context)
        for (plugin in repo.loadAll()) {
            registerPlugin(plugin)
        }
    }

    fun installPlugin(jsonString: String): Result<String> {
        val repo = UserPluginRepository(context)
        val config = repo.parseConfig(jsonString)
            ?: return Result.failure(IllegalArgumentException("插件 JSON 格式无效"))
        if (plugins.containsKey(config.id)) {
            return Result.failure(IllegalStateException("插件已存在: ${config.id}"))
        }
        val plugin = UserPlugin(config)
        repo.save(config)
        registerPlugin(plugin)
        return Result.success(config.id)
    }

    fun uninstallUserPlugin(id: String): Boolean {
        if (isBuiltinPlugin(id)) {
            Log.w(TAG, "内置插件无法卸载: $id")
            return false
        }
        val repo = UserPluginRepository(context)
        unregisterPlugin(id)
        repo.delete(id)
        return true
    }

    fun isBuiltinPlugin(id: String): Boolean = id in BUILTIN_IDS

    fun getPlugins(): List<BrowserPlugin> = plugins.values.toList()

    fun getEnabledPlugins(): List<BrowserPlugin> = plugins.values.filter { it.isEnabled }

    fun enablePlugin(id: String) {
        plugins[id]?.let {
            it.isEnabled = true
            prefs.edit().putBoolean("plugin_$id", true).apply()
            Log.d(TAG, "启用插件: $id")
        }
    }

    fun disablePlugin(id: String) {
        plugins[id]?.let {
            it.isEnabled = false
            prefs.edit().putBoolean("plugin_$id", false).apply()
            Log.d(TAG, "禁用插件: $id")
        }
    }

    fun notifyUrlLoading(url: String): String {
        var result = url
        for (plugin in getEnabledPlugins()) {
            try {
                val modified = plugin.onUrlLoading(result)
                if (modified != null) {
                    Log.d(TAG, "插件 ${plugin.id} 修改URL: $result -> $modified")
                    result = modified
                }
            } catch (e: Exception) {
                Log.e(TAG, "插件 ${plugin.id} onUrlLoading 异常: ${e.message}", e)
            }
        }
        return result
    }

    fun notifyPageFinished(webView: WebView, url: String) {
        for (plugin in getEnabledPlugins()) {
            try {
                plugin.onPageFinished(webView, url)
            } catch (e: Exception) {
                Log.e(TAG, "插件 ${plugin.id} onPageFinished 异常: ${e.message}", e)
            }
        }
        injectJavaScript(webView)
    }

    fun notifyPageStarted(webView: WebView, url: String) {
        for (plugin in getEnabledPlugins()) {
            try {
                plugin.onPageStarted(webView, url)
            } catch (e: Exception) {
                Log.e(TAG, "插件 ${plugin.id} onPageStarted 异常: ${e.message}", e)
            }
        }
    }

    private fun injectJavaScript(webView: WebView) {
        val scripts = getEnabledPlugins().mapNotNull { plugin ->
            try {
                plugin.injectJavaScript()
            } catch (e: Exception) {
                Log.e(TAG, "插件 ${plugin.id} injectJS 异常: ${e.message}", e)
                null
            }
        }
        if (scripts.isNotEmpty()) {
            val combined = scripts.joinToString("\n")
            webView.evaluateJavascript(combined, null)
        }
    }
}
