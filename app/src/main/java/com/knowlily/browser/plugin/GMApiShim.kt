package com.knowlily.browser.plugin

object GMApiShim {

    fun getShimScript(scriptId: String, grantList: List<String>, meta: String): String {
        val sb = StringBuilder()
        sb.append("(function(){")

        sb.append("if(window.__gm_shim_loaded){return;}")
        sb.append("window.__gm_shim_loaded=true;")

        // unsafeWindow
        sb.append("var unsafeWindow=window;")

        // Pending XHR callbacks storage
        sb.append("var __gm_pending={};")

        // GM_info
        sb.append("var GM_info=").append(meta).append(";")

        // GM_log
        sb.append("function GM_log(){var a=Array.prototype.slice.call(arguments);")
        sb.append("console.log('[GM:'+").append(escapeJs(scriptId)).append("]',a.join(' '));};")

        // GM_addStyle (pure JS)
        sb.append("function GM_addStyle(css){")
        sb.append("var s=document.createElement('style');")
        sb.append("s.textContent=css;")
        sb.append("document.head.appendChild(s);};")

        // GM_setValue / GM_getValue / GM_deleteValue
        val needsGMStorage = grantList.any { it == "GM_setValue" || it == "GM_getValue" || it == "GM_deleteValue" }
        if (needsGMStorage || grantList.contains("none") || grantList.isEmpty()) {
            sb.append("function GM_setValue(k,v){__gm_bridge.setValue('").append(scriptId).append("',k,v);};")
            sb.append("function GM_getValue(k,d){return __gm_bridge.getValue('").append(scriptId).append("',k,d||'');};")
            sb.append("function GM_deleteValue(k){__gm_bridge.deleteValue('").append(scriptId).append("',k);};")
        }

        // GM_xmlhttpRequest
        if (grantList.contains("GM_xmlhttpRequest") || grantList.contains("none") || grantList.isEmpty()) {
            sb.append("function GM_xmlhttpRequest(details){")
            sb.append("var id=Math.floor(Math.random()*2147483647);")
            sb.append("var d=JSON.stringify(details);")
            sb.append("__gm_bridge.xmlHttpRequest('").append(scriptId).append("',id,d);")
            sb.append("};")
        }

        // Global callback for XHR results
        sb.append("window.__gm_callback=function(id,error,result){")
        sb.append("console.log('[GM] callback id='+id+' error='+error);};")

        sb.append("})();")
        return sb.toString()
    }

    private fun escapeJs(s: String): String {
        return s.replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
    }
}
