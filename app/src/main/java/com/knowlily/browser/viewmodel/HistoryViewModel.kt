package com.knowlily.browser.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.knowlily.browser.model.HistoryItem
import com.knowlily.browser.repository.HistoryRepository

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = HistoryRepository(application)

    val historyList: LiveData<List<HistoryItem>> = repo.historyList

    init {
        repo.loadAll()
    }

    fun clearHistory() = repo.clear()
    fun refresh() = repo.loadAll()
}
