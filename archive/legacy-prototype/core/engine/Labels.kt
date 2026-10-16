package com.example.personaweather.core.engine

object Labels {
    fun aqi(value: Int): String = when (value) {
        in 0..50 -> "Good"
        in 51..100 -> "Moderate"
        in 101..150 -> "Sensitive"
        in 151..200 -> "Unhealthy"
        else -> "Very unhealthy"
    }

    fun uv(value: Double): String = when {
        value < 3 -> "Low"
        value < 6 -> "Moderate"
        value < 8 -> "High"
        value < 11 -> "Very high"
        else -> "Extreme"
    }

    fun dewPoint(value: Double): String = when {
        value < 16 -> "Dry"
        value < 20 -> "Comfortable"
        value < 24 -> "Humid"
        else -> "Muggy"
    }

    fun waveHeight(value: Double): String = when {
        value < 1.0 -> "Calm"
        value < 1.5 -> "Moderate"
        value < 2.0 -> "Rough"
        else -> "Dangerous"
    }

    fun pollen(value: String): Int = when (value) {
        "Low" -> 1
        "Moderate" -> 2
        "High" -> 3
        else -> 0
    }

    fun condition(value: String): String =
        if (value == "Scattered clouds, chance of rain") "Cloudy, chance of rain" else value
}