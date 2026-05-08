package com.example.simplebrowser.plugin

import android.webkit.WebView

/**
 * 浏览器插件统一接口。
 * 所有插件必须实现此接口，由 PluginManager 统一管理生命周期。
 */
interface BrowserPlugin {

    /** 插件唯一标识 */
    val id: String

    /** 显示名称 */
    val name: String

    /** 简短描述 */
    val description: String

    /** 插件版本 */
    val version: String

    /** 是否启用 */
    var isEnabled: Boolean

    /** 插件初始化（注册时调用一次） */
    fun onInit() {}

    /** 插件销毁（卸载时调用） */
    fun onDestroy() {}

    /**
     * URL 加载前回调。
     * @return null 表示允许原 URL 加载；返回非 null 字符串则替换原 URL。
     */
    fun onUrlLoading(url: String): String? = null

    /** 页面加载完成回调 */
    fun onPageFinished(webView: WebView, url: String) {}

    /** 页面开始加载回调 */
    fun onPageStarted(webView: WebView, url: String) {}

    /**
     * 返回要注入页面的 JavaScript（可选，null 或空字符串则不注入）。
     * 在 onPageFinished 之后自动注入。
     */
    fun injectJavaScript(): String? = null
}
