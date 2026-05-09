package com.knowlily.browser.plugin

import android.util.Log
import android.webkit.WebView

enum class PluginType { JAVASCRIPT, CSS, ADBLOCK }

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
    val enabled: Boolean = true
)

class UserPlugin(val config: UserPluginConfig) : BrowserPlugin {

    override val id get() = config.id
    override val name get() = config.name
    override val description get() = config.description
    override val version get() = config.version
    override var isEnabled = config.enabled

    private var currentUrl = ""

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
        if (config.type == PluginType.CSS && config.injectAt == InjectAt.PAGE_STARTED) {
            injectStyleTag(webView)
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
            else -> null
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

    private fun matchesUrl(url: String): Boolean {
        if (config.matchPatterns.isEmpty()) return true
        return config.matchPatterns.any { globMatch(url, it) }
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
}
