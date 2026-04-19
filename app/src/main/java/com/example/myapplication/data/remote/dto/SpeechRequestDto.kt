package com.example.myapplication.data.remote.dto

data class SpeechRequestDto(
    val config: SpeechConfigDto,
    val audio: SpeechAudioDto
)

data class SpeechConfigDto(
    val encoding: String = "AMR",
    val sampleRateHertz: Int = 8000,
    val languageCode: String = "en-US"
)

data class SpeechAudioDto(
    val content: String
)