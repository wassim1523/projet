package com.example.myapplication.domain.repository

import com.example.myapplication.domain.model.SpeechResult

interface SpeechRepository {
    suspend fun transcribeAudio(base64Audio: String): SpeechResult
}