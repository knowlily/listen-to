package com.knowlily.browser.plugin

data class UserscriptMeta(
    val name: String,
    val description: String = "",
    val version: String = "1.0",
    val namespace: String = "",
    val matchPatterns: List<String> = emptyList(),
    val includePatterns: List<String> = emptyList(),
    val excludePatterns: List<String> = emptyList(),
    val grantList: List<String> = emptyList(),
    val requireUrls: List<String> = emptyList(),
    val resourceMap: Map<String, String> = emptyMap(),
    val runAt: String = "document-end"
)

object UserscriptParser {

    fun parse(scriptContent: String): UserscriptMeta? {
        val headerEnd = scriptContent.indexOf("// ==/UserScript==")
        if (headerEnd < 0) return null
        val headerBlock = scriptContent.substring(0, headerEnd)

        val metaStart = headerBlock.indexOf("// ==UserScript==")
        if (metaStart < 0) return null
        val metaLines = headerBlock.substring(metaStart + "// ==UserScript==".length)
            .lines()
            .map { it.trim() }
            .filter { it.startsWith("// @") }

        var name = ""
        var description = ""
        var version = "1.0"
        var namespace = ""
        var runAt = "document-end"
        val matchPatterns = mutableListOf<String>()
        val includePatterns = mutableListOf<String>()
        val excludePatterns = mutableListOf<String>()
        val grantList = mutableListOf<String>()
        val requireUrls = mutableListOf<String>()
        val resourceMap = mutableMapOf<String, String>()

        for (line in metaLines) {
            val content = line.removePrefix("// @")
            when {
                content.startsWith("name ") -> name = content.removePrefix("name ").trim()
                content.startsWith("description ") -> description = content.removePrefix("description ").trim()
                content.startsWith("version ") -> version = content.removePrefix("version ").trim()
                content.startsWith("namespace ") -> namespace = content.removePrefix("namespace ").trim()
                content.startsWith("match ") -> matchPatterns.add(content.removePrefix("match ").trim())
                content.startsWith("include ") -> includePatterns.add(content.removePrefix("include ").trim())
                content.startsWith("exclude ") -> excludePatterns.add(content.removePrefix("exclude ").trim())
                content.startsWith("run-at ") -> runAt = content.removePrefix("run-at ").trim()
                content.startsWith("grant ") -> grantList.add(content.removePrefix("grant ").trim())
                content.startsWith("require ") -> requireUrls.add(content.removePrefix("require ").trim())
                content.startsWith("resource ") -> {
                    val resourceStr = content.removePrefix("resource ").trim()
                    val spaceIdx = resourceStr.indexOf(' ')
                    if (spaceIdx > 0) {
                        val resName = resourceStr.substring(0, spaceIdx)
                        val resUrl = resourceStr.substring(spaceIdx + 1).trim()
                        resourceMap[resName] = resUrl
                    }
                }
            }
        }

        if (name.isEmpty()) return null

        return UserscriptMeta(
            name = name,
            description = description,
            version = version,
            namespace = namespace,
            matchPatterns = matchPatterns,
            includePatterns = includePatterns,
            excludePatterns = excludePatterns,
            grantList = grantList,
            requireUrls = requireUrls,
            resourceMap = resourceMap,
            runAt = runAt
        )
    }

    /** Extract the JS code body after // ==/UserScript== */
    fun extractCode(scriptContent: String): String {
        val idx = scriptContent.indexOf("// ==/UserScript==")
        if (idx < 0) return scriptContent
        val afterHeader = scriptContent.substring(idx + "// ==/UserScript==".length)
        return afterHeader.trimStart('\n', '\r').trimStart()
    }

    /** Check if content looks like a userscript */
    fun isUserscript(content: String): Boolean {
        return content.contains("// ==UserScript==") && content.contains("// ==/UserScript==")
    }
}
