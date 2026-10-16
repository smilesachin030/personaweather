package com.example.personaweather.core.engine

import java.time.format.DateTimeFormatter
import java.util.Locale

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
    const val MAX_WEATHER_ADVICE = 3
    const val COMPLETE_FORECAST_DAY_ROWS = 24
    const val RAINCOAT_CHANCE = 35.0
    const val UMBRELLA_CHANCE = 60.0
    const val SCHOOL_RAIN_CHANCE = 40.0
    const val HIGH_RAIN_CHANCE = 70.0
    const val FAMILY_LOOKAHEAD_HOURS = 12
    const val COMMUTE_LOOKAHEAD_HOURS = 2
    const val TRAVEL_LOOKAHEAD_HOURS = 12
    const val SENSITIVE_AQI = 100
    const val UNHEALTHY_AQI = 150
    const val HIGH_UV = 6.0
    const val VERY_HIGH_UV = 8.0
    const val HIGH_FEELS_LIKE = 34.0
    const val LOW_VISIBILITY_KM = 5.0
    const val ROUGH_WAVE_METRES = 1.5
    const val DANGEROUS_WAVE_METRES = 2.0
    const val STRONG_COASTAL_WIND = 18.0
    const val AGRICULTURE_LOOKAHEAD_HOURS = 24
    const val RAINFALL_HOLD_IRRIGATION_MM = 5.0
    const val LOW_SOIL_MOISTURE_PERCENT = 25.0
    const val EVENT_RAIN_CHANCE = 40.0
    const val EVENT_COMFORT_RAIN_WEIGHT = 0.35
    const val EVENT_COMFORT_AQI_GOOD_MAX = 50
    const val EVENT_COMFORT_AQI_MODERATE_MAX = 100
    const val EVENT_COMFORT_AQI_SENSITIVE_MAX = 150
    const val EVENT_COMFORT_AQI_UNHEALTHY_MAX = 200
    const val EVENT_COMFORT_AQI_VERY_UNHEALTHY_MAX = 300
    const val EVENT_COMFORT_AQI_MODERATE_PENALTY = 8
    const val EVENT_COMFORT_AQI_SENSITIVE_PENALTY = 16
    const val EVENT_COMFORT_AQI_UNHEALTHY_PENALTY = 24
    const val EVENT_COMFORT_AQI_VERY_UNHEALTHY_PENALTY = 32
    const val EVENT_COMFORT_AQI_HAZARDOUS_PENALTY = 40
    const val EVENT_COMFORT_FREEZING_C = 0.0
    const val EVENT_COMFORT_COLD_C = 5.0
    const val EVENT_COMFORT_COOL_C = 10.0
    const val EVENT_COMFORT_FREEZING_PENALTY = 25
    const val EVENT_COMFORT_COLD_PENALTY = 18
    const val EVENT_COMFORT_COOL_PENALTY = 10
    const val EVENT_COMFORT_EXTREME_HEAT_C = 40.0
    const val EVENT_COMFORT_HIGH_HEAT_C = 35.0
    const val EVENT_COMFORT_HEAT_C = 32.0
    const val EVENT_COMFORT_WARM_C = 30.0
    const val EVENT_COMFORT_EXTREME_HEAT_PENALTY = 30
    const val EVENT_COMFORT_HIGH_HEAT_PENALTY = 22
    const val EVENT_COMFORT_HEAT_PENALTY = 14
    const val EVENT_COMFORT_WARM_PENALTY = 7
    const val EVENT_COMFORT_GOOD_SCORE = 80
    const val EVENT_COMFORT_FAIR_SCORE = 60
    val DAYLIGHT_HOURS = 6..17
    val ADVICE_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
    val ADVICE_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.US)
    val EVENT_HOURS = 8..20
}