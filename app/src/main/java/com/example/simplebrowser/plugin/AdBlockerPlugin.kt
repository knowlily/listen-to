package com.example.simplebrowser.plugin

import android.webkit.WebView

/**
 * 简易广告拦截插件。
 * 通过域名黑名单拦截常见广告/追踪请求，同时阻止已拦截页面加载后续资源。
 */
class AdBlockerPlugin : BrowserPlugin {

    override val id = "adblocker"
    override val name = "广告拦截"
    override val description = "拦截常见广告和追踪域名"
    override val version = "1.0"
    override var isEnabled = true

    private val blockedHosts = setOf(
        "doubleclick.net", "googlesyndication.com", "googleadservices.com",
        "googletagmanager.com", "googletagservices.com", "facebook.com/plugins",
        "adservice.google.com", "ad.doubleclick.net", "pagead2.googlesyndication.com",
        "ads.youtube.com", "google-analytics.com", "stats.g.doubleclick.net",
        "c.amazon-adsystem.com", "aax.amazon-adsystem.com",
        "adnxs.com", "rubiconproject.com", "pubmatic.com", "openx.net",
        "criteo.com", "criteo.net", "casalemedia.com", "adsrvr.org",
        "moatads.com", "taboola.com", "outbrain.com", "sharethrough.com"
    )

    override fun onUrlLoading(url: String): String? {
        val host = try {
            val uri = java.net.URI(url)
            val hostStr = uri.host ?: ""
            val path = uri.path ?: ""
            (hostStr + path).lowercase()
        } catch (e: Exception) {
            ""
        }

        for (blocked in blockedHosts) {
            if (host.contains(blocked)) {
                return "about:blank"
            }
        }
        return null
    }

    override fun onPageFinished(webView: WebView, url: String) {
        // 注入隐藏常见广告元素的脚本
        val script = """
(function(){
  var selectors=['[id*=ad]','[class*=ad]','[id*=banner]','[class*=banner]',
    '[id*=sponsor]','[class*=sponsor]','iframe[src*=ad]'];
  selectors.forEach(function(s){
    try{document.querySelectorAll(s).forEach(function(e){e.style.display='none'});}catch(e){}
  });
})();
""".trimIndent()
        webView.evaluateJavascript(script, null)
    }
}
