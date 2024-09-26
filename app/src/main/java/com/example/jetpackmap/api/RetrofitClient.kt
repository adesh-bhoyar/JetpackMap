package com.example.jetpackmap.api

import com.example.jetpackmap.utils.Constants
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


object RetrofitClient {

    val instance: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }

    private val retrofit = Retrofit.Builder().baseUrl(Constants.BASE_URL)
        .addConverterFactory(GsonConverterFactory.create()).build()
}