package com.knowlily.browser.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.knowlily.browser.model.HistoryItem
import com.knowlily.browser.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    application: Application,
    private val repo: HistoryRepository
) : AndroidViewModel(application) {

    val historyList: LiveData<List<HistoryItem>> = repo.historyFlow.asLiveData()

    fun clearHistory() {
        viewModelScope.launch { repo.clear() }
    }
}
