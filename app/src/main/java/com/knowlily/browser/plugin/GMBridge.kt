package com.knowlily.browser.plugin

import android.content.Context
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class GMBridge(
    private val context: Context,
    private val webView: WebView
) {
    companion object {
        private const val TAG = "GMBridge"
        private const val PREFS_NAME = "gm_storage"
        private var requestCounter = AtomicInteger(0)
        fun nextRequestId(): Int = requestCounter.incrementAndGet()
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val pendingRequests = ConcurrentHashMap<Int, WebView>()
    private val scope = CoroutineScope(Dispatchers.Main)

    @JavascriptInterface
    fun setValue(scriptId: String, key: String, value: String) {
        prefs.edit().putString("${scriptId}_${key}", value).apply()
    }

    @JavascriptInterface
    fun getValue(scriptId: String, key: String, defaultValue: String): String {
        return prefs.getString("${scriptId}_${key}", defaultValue) ?: defaultValue
    }

    @JavascriptInterface
    fun deleteValue(scriptId: String, key: String) {
        prefs.edit().remove("${scriptId}_${key}").apply()
    }

    @JavascriptInterface
    fun xmlHttpRequest(scriptId: String, requestId: Int, detailsJson: String) {
        pendingRequests[requestId] = webView
        scope.launch {
            try {
                val details = JSONObject(detailsJson)
                val method = details.optString("method", "GET").uppercase()
                val urlStr = details.optString("url", "")
                val headers = details.optJSONObject("headers")
                val data = details.optString("data", "")
                val timeout = details.optInt("timeout", 10000)

                val result = withContext(Dispatchers.IO) {
                    executeRequest(urlStr, method, headers, data, timeout)
                }

                invokeCallback(requestId, null, result)
            } catch (e: Exception) {
                Log.e(TAG, "XHR failed: ${e.message}", e)
                invokeCallback(requestId, e.message ?: "Unknown error", null)
            } finally {
                pendingRequests.remove(requestId)
            }
        }
    }

    private fun executeRequest(
        urlStr: String,
        method: String,
        headers: JSONObject?,
        data: String,
        timeout: Int
    ): JSONObject {
        val url = URL(urlStr)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = timeout
            readTimeout = timeout
            instanceFollowRedirects = true
        }

        headers?.let { h ->
            val keys = h.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                conn.setRequestProperty(key, h.getString(key))
            }
        }

        if (method == "POST" || method == "PUT") {
            if (data.isNotEmpty()) {
                conn.doOutput = true
                val writer = OutputStreamWriter(conn.outputStream, "UTF-8")
                writer.write(data)
                writer.flush()
                writer.close()
            }
        }

        val responseCode = conn.responseCode
        val responseStream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
        val responseBody = if (responseStream != null) {
            BufferedReader(InputStreamReader(responseStream, "UTF-8")).readText()
        } else ""

        val responseHeaders = JSONObject()
        conn.headerFields.forEach { (k, v) ->
            if (k != null) responseHeaders.put(k, v.joinToString(", "))
        }

        conn.disconnect()

        return JSONObject().apply {
            put("status", responseCode)
            put("responseText", responseBody)
            put("responseHeaders", responseHeaders.toString())
            put("finalUrl", urlStr)
        }
    }

    private fun invokeCallback(requestId: Int, error: String?, result: JSONObject?) {
        webView.post {
            val errorStr = if (error != null) "\"${escapeJs(error)}\"" else "null"
            val resultStr = if (result != null) result.toString() else "null"
            val js = "__gm_callback($requestId, $errorStr, $resultStr);"
            webView.evaluateJavascript(js, null)
        }
    }

    private fun escapeJs(s: String): String {
        return s.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

}
