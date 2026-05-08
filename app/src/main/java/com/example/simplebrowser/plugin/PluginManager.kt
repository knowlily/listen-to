package com.example.simplebrowser.plugin

import android.content.Context
import android.webkit.WebView
import android.util.Log

/**
 * 插件管理器 — 统一管理所有插件的注册、启用/禁用、生命周期。
 *
 * 使用方式：
 *   val pm = PluginManager.getInstance(context)
 *   pm.registerPlugin(MyPlugin())
 *   pm.enablePlugin("my.plugin.id")
 *
 * 在 WebView 回调中调用：
 *   pm.notifyUrlLoading(url)
 *   pm.notifyPageFinished(webView, url)
 */
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
    }

    private val plugins = mutableMapOf<String, BrowserPlugin>()
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** 注册插件（通常在 Application 或 Activity onCreate 中调用） */
    fun registerPlugin(plugin: BrowserPlugin) {
        if (plugins.containsKey(plugin.id)) {
            Log.w(TAG, "插件已注册: ${plugin.id}")
            return
        }
        // 从持久化存储恢复启用状态
        plugin.isEnabled = prefs.getBoolean("plugin_${plugin.id}", true)
        plugins[plugin.id] = plugin
        plugin.onInit()
        Log.d(TAG, "注册插件: ${plugin.id} (${plugin.name}), 启用=${plugin.isEnabled}")
    }

    /** 卸载插件 */
    fun unregisterPlugin(id: String) {
        plugins[id]?.let {
            it.onDestroy()
            plugins.remove(id)
            Log.d(TAG, "卸载插件: $id")
        }
    }

    /** 获取所有已注册插件 */
    fun getPlugins(): List<BrowserPlugin> = plugins.values.toList()

    /** 获取已启用的插件 */
    fun getEnabledPlugins(): List<BrowserPlugin> = plugins.values.filter { it.isEnabled }

    /** 启用插件 */
    fun enablePlugin(id: String) {
        plugins[id]?.let {
            it.isEnabled = true
            prefs.edit().putBoolean("plugin_$id", true).apply()
            Log.d(TAG, "启用插件: $id")
        }
    }

    /** 禁用插件 */
    fun disablePlugin(id: String) {
        plugins[id]?.let {
            it.isEnabled = false
            prefs.edit().putBoolean("plugin_$id", false).apply()
            Log.d(TAG, "禁用插件: $id")
        }
    }

    // ─── WebView 事件分发 ───────────────────────────────────────────

    /** 分发 URL 加载前事件，返回最终要加载的 URL */
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

    /** 分发页面加载完成事件 */
    fun notifyPageFinished(webView: WebView, url: String) {
        for (plugin in getEnabledPlugins()) {
            try {
                plugin.onPageFinished(webView, url)
            } catch (e: Exception) {
                Log.e(TAG, "插件 ${plugin.id} onPageFinished 异常: ${e.message}", e)
            }
        }
        // 注入 JavaScript
        injectJavaScript(webView)
    }

    /** 分发页面开始加载事件 */
    fun notifyPageStarted(webView: WebView, url: String) {
        for (plugin in getEnabledPlugins()) {
            try {
                plugin.onPageStarted(webView, url)
            } catch (e: Exception) {
                Log.e(TAG, "插件 ${plugin.id} onPageStarted 异常: ${e.message}", e)
            }
        }
    }

    /** 收集所有启用插件的 JavaScript 并注入 */
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
