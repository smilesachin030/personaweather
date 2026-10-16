package com.example.personaweather.core.engine

import java.time.LocalTime

object Tuning {
    val RUN_HOURS = 5..21
    const val RUN_WINDOW_HOURS = 2
    const val RUN_NOT_RECOMMENDED_PENALTY = 14.0
    val DRY_HOURS = 8..17
    const val DRY_MAX_RAIN = 25.0
    const val DRY_MIN_WINDOW_HOURS = 3
    const val RAIN_LIKELY = 50.0
    const val RAIN_EASING = 35.0
    const val HOURLY_ITEMS = 24
    const val DAILY_ITEMS = 7
    val ROUTE_SEARCH_MINUTES = -60..180
    const val ROUTE_SEARCH_STEP_MINUTES = 15
    val ROUTE_FIELDS = setOf("rain_chance_percent", "aqi_value", "visibility_km", "fog_flag", "heat_alert")
    val ROUTE_BASE_PERSONAS = listOf("commuter", "health_conscious")
    val SUNRISE: LocalTime = LocalTime.of(6, 0)
    val SUNSET: LocalTime = LocalTime.of(17, 55)
    val EVENT_HOURS = 8..20
}