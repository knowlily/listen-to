package com.knowlily.browser.repository

import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.knowlily.browser.model.HistoryItem

class HistoryRepository(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val historyList = MutableLiveData<List<HistoryItem>>(emptyList())

    fun save(url: String) {
        prefs.edit().putString(System.currentTimeMillis().toString(), url).apply()
        loadAll()
    }

    fun loadAll() {
        val items = prefs.all.mapNotNull { (key, value) ->
            if (value !is String) return@mapNotNull null
            val ts = key.toLongOrNull() ?: System.currentTimeMillis()
            HistoryItem(value, ts)
        }.sortedByDescending { it.timestamp }
        historyList.value = items
    }

    fun clear() {
        prefs.edit().clear().apply()
        historyList.value = emptyList()
    }

    companion object {
        const val PREFS_NAME = "browser_history"
    }
}
