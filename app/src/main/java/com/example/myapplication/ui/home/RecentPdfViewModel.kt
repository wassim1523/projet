package com.example.myapplication.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.local.recentpdf.RecentPdfEntity
import com.example.myapplication.data.repository.RecentPdfRepository
import kotlinx.coroutines.launch

class RecentPdfViewModel(
    private val repository: RecentPdfRepository
) : ViewModel() {

    val recentPdfs: LiveData<List<RecentPdfEntity>> = repository.getRecentPdfs()

    fun addRecentPdf(uri: String, name: String) {
        viewModelScope.launch {
            repository.addRecentPdf(uri, name)
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    fun deleteByUri(uri: String) {
        viewModelScope.launch {
            repository.deleteByUri(uri)
        }
    }
}

class RecentPdfViewModelFactory(
    private val repository: RecentPdfRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return RecentPdfViewModel(repository) as T
    }
}