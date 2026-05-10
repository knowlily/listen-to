package com.knowlily.browser.plugin

import android.content.Context
import android.util.Log
import android.webkit.WebView
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PluginManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "PluginManager"
        private const val PREFS_NAME = "plugin_settings"
        private val BUILTIN_IDS = setOf("adblocker", "darkmode")
    }

    private val plugins = mutableMapOf<String, BrowserPlugin>()
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val requireCache = mutableMapOf<String, String>()
    private val scope = CoroutineScope(Dispatchers.Main)

    init {
        registerPlugin(AdBlockerPlugin())
        registerPlugin(DarkModePlugin())
        loadUserPlugins()
    }

    fun registerPlugin(plugin: BrowserPlugin) {
        if (plugins.containsKey(plugin.id)) {
            Log.w(TAG, "插件已注册: ${plugin.id}")
            return
        }
        plugin.isEnabled = prefs.getBoolean("plugin_${plugin.id}", plugin.isEnabled)
        plugins[plugin.id] = plugin
        plugin.onInit()

        // Preload @require scripts for userscripts
        if (plugin is UserPlugin && plugin.config.type == PluginType.USERSCRIPT) {
            preloadRequireScripts(plugin)
        }

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
            ?: return Result.failure(IllegalArgumentException("Plugin JSON format invalid"))
        if (plugins.containsKey(config.id)) {
            return Result.failure(IllegalStateException("Plugin already exists: ${config.id}"))
        }
        val plugin = UserPlugin(config)
        repo.save(config)
        registerPlugin(plugin)
        return Result.success(config.id)
    }

    /** Install from a .user.js content string */
    fun installUserscript(jsContent: String): Result<String> {
        val repo = UserPluginRepository(context)
        val config = repo.parseUserscriptContent(jsContent)
            ?: return Result.failure(IllegalArgumentException("Invalid userscript header"))
        if (plugins.containsKey(config.id)) {
            return Result.failure(IllegalStateException("Plugin already exists: ${config.id}"))
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

    fun notifyPageStarted(webView: WebView, url: String) {
        // For userscripts with document-start, inject GM shim first
        val userscripts = getEnabledUserscripts().filter {
            it.config.runAt == "document-start" && it.matchesUrl(url)
        }
        if (userscripts.isNotEmpty()) {
            injectGMShimForScripts(webView, userscripts)
            for (us in userscripts) {
                try {
                    injectRequireScripts(webView, us)
                    val code = us.getDocumentStartScript()
                    if (!code.isNullOrEmpty()) {
                        webView.evaluateJavascript(code, null)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "插件 ${us.id} document-start 注入异常: ${e.message}", e)
                }
            }
        }

        // Call onPageStarted for all enabled plugins
        for (plugin in getEnabledPlugins()) {
            try {
                plugin.onPageStarted(webView, url)
            } catch (e: Exception) {
                Log.e(TAG, "插件 ${plugin.id} onPageStarted 异常: ${e.message}", e)
            }
        }
    }

    fun notifyPageFinished(webView: WebView, url: String) {
        for (plugin in getEnabledPlugins()) {
            try {
                if (plugin is UserPlugin && plugin.config.type == PluginType.USERSCRIPT) {
                    if (plugin.config.runAt == "document-start") continue
                    if (!plugin.matchesUrl(url)) continue
                }
                plugin.onPageFinished(webView, url)
            } catch (e: Exception) {
                Log.e(TAG, "插件 ${plugin.id} onPageFinished 异常: ${e.message}", e)
            }
        }
        injectJavaScript(webView, url)
    }

    private fun injectJavaScript(webView: WebView, url: String) {
        val enabledPlugins = getEnabledPlugins()

        val documentEndUserscripts = enabledPlugins.filterIsInstance<UserPlugin>()
            .filter { it.config.type == PluginType.USERSCRIPT && it.config.runAt != "document-start" && it.matchesUrl(url) }

        val allScripts = mutableListOf<String>()

        if (documentEndUserscripts.isNotEmpty()) {
            allScripts.add(buildGMShimForScripts(documentEndUserscripts))
            for (us in documentEndUserscripts) {
                buildRequireScripts(us)?.let { allScripts.add(it) }
            }
        }

        // Collect userscript JS
        for (us in documentEndUserscripts) {
            us.injectJavaScript()?.let { allScripts.add(it) }
        }

        // Collect other plugin JS
        for (plugin in enabledPlugins) {
            if (plugin is UserPlugin && plugin.config.type == PluginType.USERSCRIPT) continue
            try {
                plugin.injectJavaScript()?.let { allScripts.add(it) }
            } catch (e: Exception) {
                Log.e(TAG, "插件 ${plugin.id} injectJS 异常: ${e.message}", e)
            }
        }

        if (allScripts.isNotEmpty()) {
            val combined = allScripts.joinToString("\n")
            webView.evaluateJavascript(combined, null)
        }
    }

    private fun getEnabledUserscripts(): List<UserPlugin> {
        return getEnabledPlugins().filterIsInstance<UserPlugin>()
            .filter { it.config.type == PluginType.USERSCRIPT }
    }

    private fun injectGMShimForScripts(webView: WebView, scripts: List<UserPlugin>) {
        val shim = buildGMShimForScripts(scripts)
        if (shim.isNotEmpty()) {
            webView.evaluateJavascript(shim, null)
        }
    }

    private fun buildGMShimForScripts(scripts: List<UserPlugin>): String {
        if (scripts.isEmpty()) return ""
        val sb = StringBuilder()
        sb.append("(function(){")
        sb.append("if(window.__gm_shim_all_loaded){return;}")
        sb.append("window.__gm_shim_all_loaded=true;")
        sb.append("window.__gm_storage={};")
        sb.append("var unsafeWindow=window;")
        sb.append("window.GM_addStyle=function(css){var s=document.createElement('style');s.textContent=css;document.head.appendChild(s);};")
        sb.append("window.GM_log=function(){var a=Array.prototype.slice.call(arguments);console.log('[GM]',a.join(' '));};")
        sb.append("window.GM_setValue=function(k,v){try{__gm_bridge.setValue('__global__',k,v);}catch(e){}};")
        sb.append("window.GM_getValue=function(k,d){try{return __gm_bridge.getValue('__global__',k,d||'');}catch(e){return d||'';}};")
        sb.append("window.GM_deleteValue=function(k){try{__gm_bridge.deleteValue('__global__',k);}catch(e){}};")
        sb.append("window.GM_xmlhttpRequest=function(details){var id=Math.floor(Math.random()*2147483647);__gm_bridge.xmlHttpRequest('__global__',id,JSON.stringify(details));};")
        sb.append("var __gm_pending={};")
        for (script in scripts) {
            sb.append("if(typeof GM_info==='undefined')window.GM_info=").append(script.buildGMMetaJson()).append(";")
        }
        sb.append("})();")
        return sb.toString()
    }

    private fun injectRequireScripts(webView: WebView, plugin: UserPlugin) {
        for (url in plugin.config.requireUrls) {
            val cached = requireCache[url]
            if (cached != null) {
                webView.evaluateJavascript(cached, null)
            }
        }
    }

    private fun buildRequireScripts(plugin: UserPlugin): String? {
        val urls = plugin.config.requireUrls
        if (urls.isEmpty()) return null
        val sb = StringBuilder()
        for (url in urls) {
            val cached = requireCache[url]
            if (cached != null) {
                sb.append(cached).append("\n")
            }
        }
        return sb.toString().ifEmpty { null }
    }

    private fun preloadRequireScripts(plugin: UserPlugin) {
        val urls = plugin.config.requireUrls
        if (urls.isEmpty()) return
        scope.launch {
            for (url in urls) {
                if (requireCache.containsKey(url)) continue
                try {
                    val script = withContext(Dispatchers.IO) {
                        downloadScript(url)
                    }
                    if (script != null) {
                        requireCache[url] = script
                        Log.d(TAG, "预加载 @require: $url")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "预加载 @require 失败: $url — ${e.message}")
                }
            }
        }
    }

    private fun downloadScript(urlStr: String): String? {
        return try {
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10000
                readTimeout = 30000
            }
            val reader = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8"))
            val content = reader.readText()
            reader.close()
            conn.disconnect()
            content
        } catch (e: Exception) {
            null
        }
    }
}
