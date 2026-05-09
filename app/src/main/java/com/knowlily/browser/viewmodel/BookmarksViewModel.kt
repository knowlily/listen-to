package com.knowlily.browser.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.knowlily.browser.model.BookmarkItem
import com.knowlily.browser.repository.BookmarksRepository

class BookmarksViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = BookmarksRepository(application)

    val bookmarksList: LiveData<List<BookmarkItem>> = repo.bookmarksList

    init {
        repo.loadAll()
    }

    fun addBookmark(url: String): Boolean = repo.add(url)
    fun clearBookmarks() = repo.clear()
    fun bookmarkExists(url: String): Boolean = repo.exists(url)
}
