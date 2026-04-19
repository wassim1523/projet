package com.example.myapplication.data.remote.dto

data class SpeechResponseDto(
    val results: List<SpeechResultDto> = emptyList()
)

data class SpeechResultDto(
    val alternatives: List<SpeechAlternativeDto> = emptyList()
)

data class SpeechAlternativeDto(
    val transcript: String = ""
)