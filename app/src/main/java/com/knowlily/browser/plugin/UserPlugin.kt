package com.knowlily.browser.plugin

import android.util.Log
import android.webkit.WebView

enum class PluginType { JAVASCRIPT, CSS, ADBLOCK, USERSCRIPT }

enum class InjectAt { PAGE_STARTED, PAGE_FINISHED }

data class UserPluginConfig(
    val id: String,
    val name: String,
    val description: String,
    val version: String,
    val type: PluginType,
    val content: String,
    val matchPatterns: List<String> = emptyList(),
    val injectAt: InjectAt = InjectAt.PAGE_FINISHED,
    val enabled: Boolean = true,
    // Userscript-specific fields
    val grantList: List<String> = emptyList(),
    val requireUrls: List<String> = emptyList(),
    val resourceMap: Map<String, String> = emptyMap(),
    val runAt: String = "document-end",
    val excludePatterns: List<String> = emptyList(),
    val includePatterns: List<String> = emptyList()
)

class UserPlugin(val config: UserPluginConfig) : BrowserPlugin {

    override val id get() = config.id
    override val name get() = config.name
    override val description get() = config.description
    override val version get() = config.version
    override var isEnabled = config.enabled

    private var currentUrl = ""
    var requireCache: Map<String, String> = emptyMap()

    override fun onUrlLoading(url: String): String? {
        if (config.type != PluginType.ADBLOCK) return null
        if (!matchesUrl(url)) return null
        val host = extractHost(url)
        for (domain in config.content.lines()) {
            val d = domain.trim()
            if (d.isNotEmpty() && host.contains(d)) {
                Log.d("UserPlugin", "[${config.id}] blocked: $url")
                return "about:blank"
            }
        }
        return null
    }

    override fun onPageStarted(webView: WebView, url: String) {
        currentUrl = url
        if (!matchesUrl(url)) return
        when {
            config.type == PluginType.CSS && config.injectAt == InjectAt.PAGE_STARTED -> {
                injectStyleTag(webView)
            }
            config.type == PluginType.USERSCRIPT && config.runAt == "document-start" -> {
                // PluginManager handles shim injection; here we just inject the script
                injectUserscriptCode(webView)
            }
        }
    }

    override fun onPageFinished(webView: WebView, url: String) {
        currentUrl = url
        if (!matchesUrl(url)) return
        if (config.type == PluginType.ADBLOCK) {
            injectAdHideCss(webView)
        }
    }

    override fun injectJavaScript(): String? {
        if (!matchesUrl(currentUrl)) return null
        return when (config.type) {
            PluginType.JAVASCRIPT -> {
                if (config.injectAt == InjectAt.PAGE_FINISHED) config.content else null
            }
            PluginType.CSS -> {
                if (config.injectAt == InjectAt.PAGE_FINISHED) buildStyleInjectionScript() else null
            }
            PluginType.USERSCRIPT -> {
                if (config.runAt != "document-start") config.content else null
            }
            else -> null
        }
    }

    /** Called by PluginManager to inject userscript code at document-start */
    fun getDocumentStartScript(): String? {
        if (config.type != PluginType.USERSCRIPT || config.runAt != "document-start") return null
        return config.content
    }

    private fun injectUserscriptCode(webView: WebView) {
        val code = config.content
        if (code.isNotEmpty()) {
            webView.evaluateJavascript(code, null)
        }
    }

    private fun injectStyleTag(webView: WebView) {
        webView.evaluateJavascript(buildStyleInjectionScript(), null)
    }

    private fun buildStyleInjectionScript(): String {
        val escaped = config.content
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", " ")
            .replace("\r", "")
        return """
(function(){
  var id='up-${config.id}';
  if(document.getElementById(id)) return;
  var s=document.createElement('style');
  s.id=id;
  s.textContent='$escaped';
  document.head.appendChild(s);
})();
""".trimIndent()
    }

    private fun injectAdHideCss(webView: WebView) {
        val selectors = listOf(
            "[id*=ad]", "[class*=ad]", "[id*=banner]", "[class*=banner]",
            "[id*=sponsor]", "[class*=sponsor]", "iframe[src*=ad]",
            "[id*=advertisement]", "[class*=advertisement]"
        ).joinToString(",")
        webView.evaluateJavascript(
            "(function(){'$selectors'.split(',').forEach(function(s){try{document.querySelectorAll(s).forEach(function(e){e.style.display='none'});}catch(e){}});})();",
            null
        )
    }

    fun matchesUrl(url: String): Boolean {
        if (config.includePatterns.isNotEmpty() && !config.includePatterns.any { globMatch(url, it) }) {
            return false
        }
        if (config.excludePatterns.isNotEmpty() && config.excludePatterns.any { excludeMatch(url, it) }) {
            return false
        }
        if (config.matchPatterns.isEmpty()) return true
        return config.matchPatterns.any { matchPattern(url, it) }
    }

    /** Tampermonkey-standard @match pattern matching */
    private fun matchPattern(url: String, pattern: String): Boolean {
        // If pattern uses standard @match format: <scheme>://<host><path>
        if (pattern.contains("://")) {
            return tampermonkeyMatch(url, pattern)
        }
        // Fall back to glob matching
        return globMatch(url, pattern)
    }

    private fun tampermonkeyMatch(url: String, pattern: String): Boolean {
        return try {
            val uri = java.net.URI(url)
            val scheme = uri.scheme ?: ""
            val host = uri.host ?: ""
            val path = uri.path ?: "/"
            val query = uri.query

            val patternUri = java.net.URI(pattern)
            val pScheme = patternUri.scheme ?: ""
            val pHost = patternUri.host ?: ""
            val pPath = patternUri.path ?: "/*"

            // Match scheme
            if (pScheme != "*") {
                if (pScheme != scheme) return false
            } else {
                if (scheme != "http" && scheme != "https") return false
            }

            // Match host
            if (!hostMatch(host, pHost)) return false

            // Match path
            if (!pathMatch(path + (query?.let { "?$it" } ?: ""), pPath)) return false

            true
        } catch (e: Exception) {
            globMatch(url, pattern)
        }
    }

    private fun hostMatch(host: String, pattern: String): Boolean {
        if (pattern == "*") return true
        if (pattern.startsWith("*.")) {
            val suffix = pattern.substring(2)
            return host == suffix || host.endsWith(".$suffix")
        }
        return host.equals(pattern, ignoreCase = true)
    }

    private fun pathMatch(path: String, pattern: String): Boolean {
        if (pattern == "/*") return true
        // Convert glob pattern to regex for path matching
        val regex = pattern
            .replace(".", "\\.")
            .replace("*", ".*")
            .replace("?", ".")
        return try {
            Regex("^$regex$", RegexOption.IGNORE_CASE).containsMatchIn(path)
        } catch (e: Exception) {
            false
        }
    }

    fun excludeMatch(url: String, pattern: String): Boolean {
        return globMatch(url, pattern)
    }

    private fun globMatch(text: String, pattern: String): Boolean {
        val regex = pattern
            .replace(".", "\\.")
            .replace("*", ".*")
            .replace("?", ".")
        return try {
            Regex(regex, RegexOption.IGNORE_CASE).containsMatchIn(text)
        } catch (e: Exception) {
            false
        }
    }

    private fun extractHost(url: String): String {
        return try {
            java.net.URI(url).host?.lowercase() ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    /** Build GM_info-like metadata JSON for the shim */
    fun buildGMMetaJson(): String {
        return """{"script":{"name":"${escapeJs(name)}","description":"${escapeJs(description)}","version":"${escapeJs(version)}","match":${toJsonArray(config.matchPatterns)},"exclude":${toJsonArray(config.excludePatterns)},"runAt":"${escapeJs(config.runAt)}","grant":${toJsonArray(config.grantList)}}}"""
    }

    private fun escapeJs(s: String): String {
        return s.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
    }

    private fun toJsonArray(list: List<String>): String {
        return "[" + list.joinToString(",") { "\"${escapeJs(it)}\"" } + "]"
    }
}
