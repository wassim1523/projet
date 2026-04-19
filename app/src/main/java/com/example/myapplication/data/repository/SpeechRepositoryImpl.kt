package com.example.myapplication.data.repository

import com.example.myapplication.data.remote.dto.SpeechAudioDto
import com.example.myapplication.data.remote.dto.SpeechConfigDto
import com.example.myapplication.data.remote.dto.SpeechRequestDto
import com.example.myapplication.data.remote.retrofit.RetrofitProvider
import com.example.myapplication.domain.model.SpeechResult
import com.example.myapplication.domain.repository.SpeechRepository
import retrofit2.HttpException

class SpeechRepositoryImpl : SpeechRepository {

    private val api = RetrofitProvider.speechApiService

    private val apiKey = "YOUR_API_KEY_HERE"

    override suspend fun transcribeAudio(base64Audio: String): SpeechResult {
        try {
            val request = SpeechRequestDto(
                config = SpeechConfigDto(
                    encoding = "AMR",
                    sampleRateHertz = 8000,
                    languageCode = "en-US"
                ),
                audio = SpeechAudioDto(content = base64Audio)
            )

            val response = api.recognizeSpeech(
                apiKey = apiKey,
                request = request
            )

            val transcript = response.results
                .firstOrNull()
                ?.alternatives
                ?.firstOrNull()
                ?.transcript
                ?: ""

            return SpeechResult(transcript = transcript)

        } catch (e: HttpException) {
            val errorBody = try {
                e.response()?.errorBody()?.string()
            } catch (_: Exception) {
                null
            }

            throw Exception(
                buildString {
                    append("HTTP ${e.code()}")
                    if (!errorBody.isNullOrBlank()) {
                        append(" - ")
                        append(errorBody)
                    }
                }
            )
        } catch (e: Exception) {
            throw Exception(e.message ?: "Speech request failed")
        }
    }
}