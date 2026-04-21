package com.example.myapplication.ui.pdf

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PdfViewModel : ViewModel() {

    private val _pdfUriString = MutableLiveData<String?>()
    val pdfUriString: LiveData<String?> = _pdfUriString

    private val _currentPageIndex = MutableLiveData(0)
    val currentPageIndex: LiveData<Int> = _currentPageIndex

    private val _whiteboardVisible = MutableLiveData(true)
    val whiteboardVisible: LiveData<Boolean> = _whiteboardVisible

    private val _currentWhitePageIndex = MutableLiveData(0)
    val currentWhitePageIndex: LiveData<Int> = _currentWhitePageIndex

    private val _whitePageCount = MutableLiveData(1)
    val whitePageCount: LiveData<Int> = _whitePageCount

    private val _textModeEnabled = MutableLiveData(false)
    val textModeEnabled: LiveData<Boolean> = _textModeEnabled

    fun setPdfUri(uriString: String?) {
        if (!uriString.isNullOrEmpty() && _pdfUriString.value != uriString) {
            _pdfUriString.value = uriString
            _currentPageIndex.value = 0
            _currentWhitePageIndex.value = 0
            _whitePageCount.value = 1
        }
    }

    fun setCurrentPage(index: Int) {
        _currentPageIndex.value = index.coerceAtLeast(0)
    }

    fun setWhiteboardVisible(visible: Boolean) {
        _whiteboardVisible.value = visible
    }

    fun toggleWhiteboard() {
        _whiteboardVisible.value = !(_whiteboardVisible.value ?: true)
    }

    fun setCurrentWhitePageIndex(index: Int) {
        _currentWhitePageIndex.value = index.coerceAtLeast(0)
    }

    fun setWhitePageCount(count: Int) {
        val safeCount = count.coerceAtLeast(1)
        _whitePageCount.value = safeCount

        val current = _currentWhitePageIndex.value ?: 0
        if (current >= safeCount) {
            _currentWhitePageIndex.value = safeCount - 1
        }
    }

    fun nextWhitePage() {
        val current = _currentWhitePageIndex.value ?: 0
        val count = _whitePageCount.value ?: 1
        if (current < count - 1) {
            _currentWhitePageIndex.value = current + 1
        }
    }

    fun prevWhitePage() {
        val current = _currentWhitePageIndex.value ?: 0
        if (current > 0) {
            _currentWhitePageIndex.value = current - 1
        }
    }

    fun addWhitePageAndGoToLast() {
        val newCount = (_whitePageCount.value ?: 1) + 1
        _whitePageCount.value = newCount
        _currentWhitePageIndex.value = newCount - 1
    }

    fun setTextModeEnabled(enabled: Boolean) {
        _textModeEnabled.value = enabled
    }
}