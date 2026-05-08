package com.example.hoopmaster.network

import com.example.hoopmaster.core.config.AppConfig
import com.example.hoopmaster.data.api.HoopMasterApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


object RetrofitClient {
    private val BASE_URL = AppConfig.API_BASE_URL

    val instance: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val apiService: ApiService by lazy {
        instance.create(ApiService::class.java)
    }

    val hoopMasterApi: HoopMasterApi by lazy {
        instance.create(HoopMasterApi::class.java)
    }
}
