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
}
