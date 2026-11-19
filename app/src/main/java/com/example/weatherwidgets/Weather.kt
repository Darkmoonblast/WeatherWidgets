package com.example.weatherwidgets

data class Weather(
    val city: String,
    val description: String,
    val icon: String,
    val temp: Double,
    val feelsLike: Double,
    val humidity: Int,
    val pressure: Int,
    val windSpeed: Double,
    val windDeg: Float,
    val clouds: Int
) {
    val windArrowRotation: Float get() = (windDeg + 180f) % 360f

    val tempF: Double get() = temp * 9 / 5 + 32
    val feelsLikeF: Double get() = feelsLike * 9 / 5 + 32
}