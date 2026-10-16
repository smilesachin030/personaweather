package com.example.personaweather.core.model

import java.time.LocalDateTime

data class Location(
    val id: String,
    val name: String,
    val lat: Double,
    val lon: Double,
    val distanceKmFromHub: Double,
    val direction: String,
    val character: String,
    val isCoastal: Boolean
)

data class WeatherRow(
    val id: Long,
    val locationId: String,
    val timestamp: String,
    val date: String,
    val hour: Int,
    val temperature: Double,
    val feelsLike: Double,
    val humidity: Double,
    val windSpeed: Double,
    val windDirection: String,
    val condition: String,
    val pressureHpa: Double,
    val dewPoint: Double,
    val uvIndex: Double,
    val rainChancePercent: Double,
    val rainfallMm: Double,
    val visibilityKm: Double,
    val fogFlag: Int,
    val aqiValue: Int,
    val aqiCategory: String,
    val dominantPollutant: String,
    val pm2_5: Double,
    val pm10: Double,
    val no2: Double,
    val o3: Double,
    val co: Double,
    val so2: Double,
    val pollenGrass: String,
    val pollenTree: String,
    val pollenRagweed: String,
    val waveHeightM: Double?,
    val wavePeriodSec: Double?,
    val waveDirection: String?,
    val waterTemperature: Double?,
    val tideHeightM: Double?,
    val soilMoisturePercent: Double,
    val frostAlert: Int,
    val heatAlert: Int,
    val bestRunningHour: Int,
    val comfortIndex: String
) {
    val time: LocalDateTime get() = LocalDateTime.parse(timestamp)

    fun fieldValue(name: String): Any? = when (name) {
        "id" -> id
        "location_id" -> locationId
        "timestamp" -> timestamp
        "date" -> date
        "hour" -> hour
        "temperature" -> temperature
        "feels_like" -> feelsLike
        "humidity" -> humidity
        "wind_speed" -> windSpeed
        "wind_direction" -> windDirection
        "condition" -> condition
        "pressure_hpa" -> pressureHpa
        "dew_point" -> dewPoint
        "uv_index" -> uvIndex
        "rain_chance_percent" -> rainChancePercent
        "rainfall_mm" -> rainfallMm
        "visibility_km" -> visibilityKm
        "fog_flag" -> fogFlag
        "aqi_value" -> aqiValue
        "aqi_category" -> aqiCategory
        "dominant_pollutant" -> dominantPollutant
        "pm2_5" -> pm2_5
        "pm10" -> pm10
        "no2" -> no2
        "o3" -> o3
        "co" -> co
        "so2" -> so2
        "pollen_grass" -> pollenGrass
        "pollen_tree" -> pollenTree
        "pollen_ragweed" -> pollenRagweed
        "wave_height_m" -> waveHeightM
        "wave_period_sec" -> wavePeriodSec
        "wave_direction" -> waveDirection
        "water_temperature" -> waterTemperature
        "tide_height_m" -> tideHeightM
        "soil_moisture_percent" -> soilMoisturePercent
        "frost_alert" -> frostAlert
        "heat_alert" -> heatAlert
        "best_running_hour" -> bestRunningHour
        "comfort_index" -> comfortIndex
        else -> null
    }
}

data class Rule(
    val id: String,
    val field: String,
    val op: String,
    val value: Any,
    val severity: Severity,
    val message: String,
    val messageTa: String? = null,
    val lookaheadHours: Int? = null,
    val aggregate: String? = null
)

data class Persona(
    val id: String,
    val label: String,
    val requiresCoastal: Boolean,
    val defaultCards: List<String>,
    val rules: List<Rule>
)

data class CardSpec(
    val id: String,
    val label: String,
    val fields: List<String>,
    val requiresCoastal: Boolean = false,
    val derived: String? = null
)

enum class Severity { CAUTION, ALERT }

data class Advisory(
    val ruleId: String,
    val personaId: String,
    val field: String,
    val severity: Severity,
    val message: String,
    val messageTa: String?,
    val observed: Any?,
    val isLookahead: Boolean
)

data class DailySummary(
    val date: String,
    val minTemp: Double,
    val maxTemp: Double,
    val maxRain: Double,
    val avgAqi: Double,
    val maxAqi: Int,
    val totalRainMm: Double,
    val dominantCondition: String,
    val eventScore: Int
)

data class Waypoint(val locationId: String, val label: String, val minutesFromStart: Int)