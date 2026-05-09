package com.knowlily.browser.plugin

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.File

class UserPluginRepository(private val context: Context) {

    companion object {
        private const val TAG = "UserPluginRepo"
        private const val PLUGINS_DIR = "plugins"
    }

    private val dir: File
        get() = File(context.filesDir, PLUGINS_DIR).also { it.mkdirs() }

    fun loadAll(): List<UserPlugin> {
        val files = dir.listFiles { f -> f.extension == "json" } ?: return emptyList()
        return files.mapNotNull { loadFromFile(it) }
    }

    fun save(config: UserPluginConfig) {
        val json = JSONObject().apply {
            put("id", config.id)
            put("name", config.name)
            put("description", config.description)
            put("version", config.version)
            put("type", config.type.name.lowercase())
            put("content", config.content)
            put("injectAt", config.injectAt.name.lowercase())
            put("enabled", config.enabled)
            if (config.matchPatterns.isNotEmpty()) {
                put("matchPatterns", org.json.JSONArray(config.matchPatterns))
            }
        }
        File(dir, "${config.id}.json").writeText(json.toString(2))
        Log.d(TAG, "Saved plugin: ${config.id}")
    }

    fun delete(id: String): Boolean {
        val deleted = File(dir, "$id.json").delete()
        if (deleted) Log.d(TAG, "Deleted plugin: $id")
        return deleted
    }

    fun exists(id: String): Boolean = File(dir, "$id.json").exists()

    private fun loadFromFile(file: File): UserPlugin? {
        return try {
            val json = JSONObject(file.readText())
            val type = when (json.optString("type", "javascript").lowercase()) {
                "css" -> PluginType.CSS
                "adblock" -> PluginType.ADBLOCK
                else -> PluginType.JAVASCRIPT
            }
            val injectAt = when (json.optString("injectAt", "page_finished").lowercase()) {
                "page_started" -> InjectAt.PAGE_STARTED
                else -> InjectAt.PAGE_FINISHED
            }
            val patterns = mutableListOf<String>()
            val arr = json.optJSONArray("matchPatterns")
            if (arr != null) {
                for (i in 0 until arr.length()) patterns.add(arr.getString(i))
            }
            val config = UserPluginConfig(
                id = json.getString("id"),
                name = json.getString("name"),
                description = json.optString("description", ""),
                version = json.optString("version", "1.0"),
                type = type,
                content = json.getString("content"),
                matchPatterns = patterns,
                injectAt = injectAt,
                enabled = json.optBoolean("enabled", true)
            )
            UserPlugin(config)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load plugin from ${file.name}: ${e.message}")
            null
        }
    }

    fun parseConfig(jsonString: String): UserPluginConfig? {
        return try {
            val json = JSONObject(jsonString.trim())
            val type = when (json.optString("type", "javascript").lowercase()) {
                "css" -> PluginType.CSS
                "adblock" -> PluginType.ADBLOCK
                else -> PluginType.JAVASCRIPT
            }
            val injectAt = when (json.optString("injectAt", "page_finished").lowercase()) {
                "page_started" -> InjectAt.PAGE_STARTED
                else -> InjectAt.PAGE_FINISHED
            }
            val patterns = mutableListOf<String>()
            val arr = json.optJSONArray("matchPatterns")
            if (arr != null) {
                for (i in 0 until arr.length()) patterns.add(arr.getString(i))
            }
            UserPluginConfig(
                id = json.getString("id"),
                name = json.getString("name"),
                description = json.optString("description", ""),
                version = json.optString("version", "1.0"),
                type = type,
                content = json.getString("content"),
                matchPatterns = patterns,
                injectAt = injectAt,
                enabled = json.optBoolean("enabled", true)
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse plugin JSON: ${e.message}")
            null
        }
    }
}
