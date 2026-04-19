package com.example.myapplication.ui.notes

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SpeechViewModel : ViewModel() {

    private val _speechText = MutableLiveData<String>()
    val speechText: LiveData<String> = _speechText

    private val _isListening = MutableLiveData<Boolean>()
    val isListening: LiveData<Boolean> = _isListening

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    fun setSpeechText(text: String) {
        _speechText.value = text
    }

    fun setListening(listening: Boolean) {
        _isListening.value = listening
    }

    fun setError(message: String) {
        _error.value = message
    }
}