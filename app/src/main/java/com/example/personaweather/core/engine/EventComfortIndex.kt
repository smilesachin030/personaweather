package com.example.personaweather.core.engine

import kotlin.math.roundToInt

data class EventComfortAssessment(
    val score: Int,
    val label: String,
    val rainPenalty: Int,
    val aqiPenalty: Int,
    val temperaturePenalty: Int,
)

object EventComfortIndex {
    fun assess(
        rainChancePercent: Double,
        aqi: Int,
        temperatureC: Double,
        feelsLikeC: Double,
    ): EventComfortAssessment {
        val rainPenalty = (rainChancePercent.coerceIn(0.0, 100.0) * Tuning.EVENT_COMFORT_RAIN_WEIGHT)
            .roundToInt()
        val aqiPenalty = when {
            aqi <= Tuning.EVENT_COMFORT_AQI_GOOD_MAX -> 0
            aqi <= Tuning.EVENT_COMFORT_AQI_MODERATE_MAX -> Tuning.EVENT_COMFORT_AQI_MODERATE_PENALTY
            aqi <= Tuning.EVENT_COMFORT_AQI_SENSITIVE_MAX -> Tuning.EVENT_COMFORT_AQI_SENSITIVE_PENALTY
            aqi <= Tuning.EVENT_COMFORT_AQI_UNHEALTHY_MAX -> Tuning.EVENT_COMFORT_AQI_UNHEALTHY_PENALTY
            aqi <= Tuning.EVENT_COMFORT_AQI_VERY_UNHEALTHY_MAX -> Tuning.EVENT_COMFORT_AQI_VERY_UNHEALTHY_PENALTY
            else -> Tuning.EVENT_COMFORT_AQI_HAZARDOUS_PENALTY
        }
        val temperaturePenalty = when {
            temperatureC < Tuning.EVENT_COMFORT_FREEZING_C -> Tuning.EVENT_COMFORT_FREEZING_PENALTY
            temperatureC < Tuning.EVENT_COMFORT_COLD_C -> Tuning.EVENT_COMFORT_COLD_PENALTY
            temperatureC < Tuning.EVENT_COMFORT_COOL_C -> Tuning.EVENT_COMFORT_COOL_PENALTY
            feelsLikeC >= Tuning.EVENT_COMFORT_EXTREME_HEAT_C -> Tuning.EVENT_COMFORT_EXTREME_HEAT_PENALTY
            feelsLikeC >= Tuning.EVENT_COMFORT_HIGH_HEAT_C -> Tuning.EVENT_COMFORT_HIGH_HEAT_PENALTY
            feelsLikeC >= Tuning.EVENT_COMFORT_HEAT_C -> Tuning.EVENT_COMFORT_HEAT_PENALTY
            feelsLikeC >= Tuning.EVENT_COMFORT_WARM_C -> Tuning.EVENT_COMFORT_WARM_PENALTY
            else -> 0
        }
        val score = (100 - rainPenalty - aqiPenalty - temperaturePenalty).coerceIn(0, 100)
        val label = when {
            score >= Tuning.EVENT_COMFORT_GOOD_SCORE -> "Good"
            score >= Tuning.EVENT_COMFORT_FAIR_SCORE -> "Fair"
            else -> "Poor"
        }
        return EventComfortAssessment(
            score = score,
            label = label,
            rainPenalty = rainPenalty,
            aqiPenalty = aqiPenalty,
            temperaturePenalty = temperaturePenalty,
        )
    }
}
