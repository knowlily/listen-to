package com.knowlily.browser.repository

import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.knowlily.browser.model.BookmarkItem

class BookmarksRepository(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val bookmarksList = MutableLiveData<List<BookmarkItem>>(emptyList())

    fun add(url: String): Boolean {
        if (exists(url)) return false
        val key = "${System.currentTimeMillis()}-$url"
        prefs.edit().putString(key, url).apply()
        loadAll()
        return true
    }

    fun loadAll() {
        val items = prefs.all.mapNotNull { (key, value) ->
            if (value !is String) return@mapNotNull null
            val ts = key.substringBefore("-").toLongOrNull() ?: System.currentTimeMillis()
            BookmarkItem(value, ts)
        }.sortedByDescending { it.timestamp }
        bookmarksList.value = items
    }

    fun exists(url: String): Boolean = prefs.all.values.any { it == url }

    fun clear() {
        prefs.edit().clear().apply()
        bookmarksList.value = emptyList()
    }

    companion object {
        const val PREFS_NAME = "browser_bookmarks"
    }
}
