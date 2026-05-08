package com.example.simplebrowser.plugin

/**
 * 夜间模式插件 — 为任意网页注入暗色 CSS。
 * 在 onPageFinished 后通过 evaluateJavascript 插入样式。
 */
class DarkModePlugin : BrowserPlugin {

    override val id = "darkmode"
    override val name = "夜间模式"
    override val description = "为网页注入暗色主题样式，保护夜间阅读"
    override val version = "1.0"
    override var isEnabled = false // 默认关闭，避免与系统主题冲突

    override fun injectJavaScript(): String? {
        return """
(function(){
  if(document.documentElement.getAttribute('data-claude-dark')) return;
  document.documentElement.setAttribute('data-claude-dark','1');
  var css='html{filter:invert(0.9) hue-rotate(180deg)}img,video,canvas,iframe,[style*=background-image]{filter:invert(0.9) hue-rotate(180deg)}';
  var style=document.createElement('style');
  style.id='claude-dark-style';
  style.textContent=css;
  document.head.appendChild(style);
})();
""".trimIndent()
    }
}
