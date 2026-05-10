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
            if (config.grantList.isNotEmpty()) {
                put("grantList", org.json.JSONArray(config.grantList))
            }
            if (config.requireUrls.isNotEmpty()) {
                put("requireUrls", org.json.JSONArray(config.requireUrls))
            }
            if (config.resourceMap.isNotEmpty()) {
                put("resourceMap", JSONObject(config.resourceMap))
            }
            if (config.runAt != "document-end") {
                put("runAt", config.runAt)
            }
            if (config.excludePatterns.isNotEmpty()) {
                put("excludePatterns", org.json.JSONArray(config.excludePatterns))
            }
            if (config.includePatterns.isNotEmpty()) {
                put("includePatterns", org.json.JSONArray(config.includePatterns))
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
            val config = jsonToConfig(json)
            UserPlugin(config)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load plugin from ${file.name}: ${e.message}")
            null
        }
    }

    fun parseConfig(jsonString: String): UserPluginConfig? {
        return try {
            jsonToConfig(JSONObject(jsonString.trim()))
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse plugin JSON: ${e.message}")
            null
        }
    }

    /** Parse a .user.js content string into a UserPluginConfig */
    fun parseUserscriptContent(jsContent: String): UserPluginConfig? {
        val meta = UserscriptParser.parse(jsContent) ?: return null
        val code = UserscriptParser.extractCode(jsContent)
        val id = "userscript_" + meta.name.lowercase()
            .replace(Regex("[^a-z0-9]"), "_")
            .replace(Regex("_+"), "_")
            .trim('_')

        return UserPluginConfig(
            id = id,
            name = meta.name,
            description = meta.description,
            version = meta.version,
            type = PluginType.USERSCRIPT,
            content = code,
            matchPatterns = meta.matchPatterns,
            injectAt = if (meta.runAt == "document-start") InjectAt.PAGE_STARTED else InjectAt.PAGE_FINISHED,
            enabled = true,
            grantList = meta.grantList,
            requireUrls = meta.requireUrls,
            resourceMap = meta.resourceMap,
            runAt = meta.runAt,
            excludePatterns = meta.excludePatterns,
            includePatterns = meta.includePatterns
        )
    }

    private fun jsonToConfig(json: JSONObject): UserPluginConfig {
        val type = when (json.optString("type", "javascript").lowercase()) {
            "css" -> PluginType.CSS
            "adblock" -> PluginType.ADBLOCK
            "userscript" -> PluginType.USERSCRIPT
            else -> PluginType.JAVASCRIPT
        }
        val injectAt = when (json.optString("injectAt", "page_finished").lowercase()) {
            "page_started" -> InjectAt.PAGE_STARTED
            else -> InjectAt.PAGE_FINISHED
        }

        fun jsonArrayToList(key: String): List<String> {
            val arr = json.optJSONArray(key) ?: return emptyList()
            return (0 until arr.length()).map { arr.getString(it) }
        }

        val resourceMap = mutableMapOf<String, String>()
        val rmObj = json.optJSONObject("resourceMap")
        if (rmObj != null) {
            val keys = rmObj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                resourceMap[k] = rmObj.getString(k)
            }
        }

        return UserPluginConfig(
            id = json.getString("id"),
            name = json.getString("name"),
            description = json.optString("description", ""),
            version = json.optString("version", "1.0"),
            type = type,
            content = json.getString("content"),
            matchPatterns = jsonArrayToList("matchPatterns"),
            injectAt = injectAt,
            enabled = json.optBoolean("enabled", true),
            grantList = jsonArrayToList("grantList"),
            requireUrls = jsonArrayToList("requireUrls"),
            resourceMap = resourceMap,
            runAt = json.optString("runAt", "document-end"),
            excludePatterns = jsonArrayToList("excludePatterns"),
            includePatterns = jsonArrayToList("includePatterns")
        )
    }
}
