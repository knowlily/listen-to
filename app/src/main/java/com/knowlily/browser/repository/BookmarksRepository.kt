package com.knowlily.browser.repository

import com.knowlily.browser.data.BookmarksDao
import com.knowlily.browser.model.BookmarkItem
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookmarksRepository @Inject constructor(
    private val dao: BookmarksDao
) {
    val bookmarksFlow: Flow<List<BookmarkItem>> = dao.getAllFlow()

    suspend fun add(url: String): Boolean {
        if (dao.exists(url)) return false
        dao.insert(BookmarkItem(url = url, timestamp = System.currentTimeMillis()))
        return true
    }

    suspend fun clear() {
        dao.deleteAll()
    }

    suspend fun search(query: String): List<BookmarkItem> = dao.search(query)

    suspend fun exportToJson(): String {
        val items = dao.getAll()
        val jsonArray = org.json.JSONArray()
        for (item in items) {
            val obj = org.json.JSONObject()
            obj.put("url", item.url)
            obj.put("timestamp", item.timestamp)
            jsonArray.put(obj)
        }
        return jsonArray.toString(2)
    }

    suspend fun importFromJson(json: String): Int {
        val jsonArray = org.json.JSONArray(json)
        var count = 0
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val url = obj.optString("url") ?: continue
            if (url.isNotEmpty() && !dao.exists(url)) {
                val ts = obj.optLong("timestamp", System.currentTimeMillis())
                dao.insert(BookmarkItem(url = url, timestamp = ts))
                count++
            }
        }
        return count
    }
}
