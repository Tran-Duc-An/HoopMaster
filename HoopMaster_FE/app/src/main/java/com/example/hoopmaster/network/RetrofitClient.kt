package com.example.hoopmaster.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


object RetrofitClient {
    // 10.0.2.2 là địa chỉ IP trỏ về localhost của máy tính khi dùng Emulator
    private const val BASE_URL = "http://10.0.2.2:3000/"

    val instance: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val apiService: ApiService by lazy {
        instance.create(ApiService::class.java)
    }
}