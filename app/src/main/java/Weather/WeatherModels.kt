package com.example.dementiaclock.weather

data class WeatherResponse(
    val main: MainWeather,
    val weather: List<Weather>,
    val wind: Wind,
    val name: String
)

data class MainWeather(
    val temp: Double,
    val humidity: Int,
    val feels_like: Double
)

data class Weather(
    val description: String,
    val icon: String
)

data class Wind(
    val speed: Double
)
