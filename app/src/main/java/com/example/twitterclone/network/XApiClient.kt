package com.example.twitterclone.network

import com.example.twitterclone.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object XApiClient {
    private const val BASE_URL = "https://api.x.com/"

    private val okHttpClient = OkHttpClient.Builder()
        // No auth header here!
        .build()

    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiService: XApiService = retrofit.create(XApiService::class.java)
}
