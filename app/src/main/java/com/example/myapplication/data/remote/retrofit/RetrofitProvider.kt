package com.example.myapplication.data.remote.retrofit

import com.example.myapplication.data.remote.api.SpeechApiService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitProvider {

    private const val BASE_URL = "https://speech.googleapis.com/"

    val speechApiService: SpeechApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SpeechApiService::class.java)
    }
}