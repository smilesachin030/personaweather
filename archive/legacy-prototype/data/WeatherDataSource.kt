package com.example.personaweather.data

import com.example.personaweather.core.model.Location
import com.example.personaweather.core.model.WeatherRow
import java.time.LocalDateTime

interface WeatherDataSource {
    suspend fun locations(): List<Location>
    suspend fun series(locationId: String): List<WeatherRow>
    suspend fun dataRange(): Pair<LocalDateTime, LocalDateTime>
}