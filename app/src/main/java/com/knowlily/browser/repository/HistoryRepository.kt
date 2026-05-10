package com.knowlily.browser.repository

import com.knowlily.browser.data.HistoryDao
import com.knowlily.browser.model.HistoryItem
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepository @Inject constructor(
    private val dao: HistoryDao
) {
    val historyFlow: Flow<List<HistoryItem>> = dao.getAllFlow()

    suspend fun save(url: String) {
        dao.insert(HistoryItem(url = url, timestamp = System.currentTimeMillis()))
    }

    suspend fun clear() {
        dao.deleteAll()
    }

    suspend fun search(query: String): List<HistoryItem> = dao.search(query)
}
