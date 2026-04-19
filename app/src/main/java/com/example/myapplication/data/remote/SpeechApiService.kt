package com.example.myapplication.data.remote.api

import com.example.myapplication.data.remote.dto.SpeechRequestDto
import com.example.myapplication.data.remote.dto.SpeechResponseDto
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface SpeechApiService {

    @POST("v1/speech:recognize")
    suspend fun recognizeSpeech(
        @Query("key") apiKey: String,
        @Body request: SpeechRequestDto
    ): SpeechResponseDto
}