package com.knowlily.browser.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.knowlily.browser.model.BookmarkItem
import com.knowlily.browser.repository.BookmarksRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BookmarksViewModel @Inject constructor(
    application: Application,
    private val repo: BookmarksRepository
) : AndroidViewModel(application) {

    val bookmarksList: LiveData<List<BookmarkItem>> = repo.bookmarksFlow.asLiveData()

    fun addBookmark(url: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            onResult(repo.add(url))
        }
    }

    fun clearBookmarks() {
        viewModelScope.launch { repo.clear() }
    }
}
