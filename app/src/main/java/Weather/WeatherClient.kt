package com.example.dementiaclock.weather

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object WeatherClient {
    private const val BASE_URL = "https://api.openweathermap.org/data/2.5/"
    private const val API_KEY = "51a01a11da38fcb12d5327773921af90"

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val service: WeatherService = retrofit.create(WeatherService::class.java)
}