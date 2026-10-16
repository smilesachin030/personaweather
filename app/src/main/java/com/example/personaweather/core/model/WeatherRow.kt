package com.example.personaweather.core.model

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

enum class PollenSpecies(val id: String, val fallbackName: String) {
    GRASS("grass", "Grass"),
    ALDER("alder", "Alder"),
    BIRCH("birch", "Birch"),
    MUGWORT("mugwort", "Mugwort"),
    OLIVE("olive", "Olive"),
    RAGWEED("ragweed", "Ragweed"),
}

data class PollenReadings(
    val grass: Double? = null,
    val alder: Double? = null,
    val birch: Double? = null,
    val mugwort: Double? = null,
    val olive: Double? = null,
    val ragweed: Double? = null,
) {
    fun value(species: PollenSpecies): Double? = when (species) {
        PollenSpecies.GRASS -> grass
        PollenSpecies.ALDER -> alder
        PollenSpecies.BIRCH -> birch
        PollenSpecies.MUGWORT -> mugwort
        PollenSpecies.OLIVE -> olive
        PollenSpecies.RAGWEED -> ragweed
    }

    fun hasAnyValue(): Boolean = PollenSpecies.values().any { value(it) != null }
}

@Suppress("LargeClass")
data class WeatherRow(
    val id: Int,
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
    val pm25: Double,
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
    val heatAlert: Int,
    val comfortIndex: String,
    val pollenReadings: PollenReadings = PollenReadings(),
    val sunriseLocalTime: String? = null,
    val sunsetLocalTime: String? = null,
) {
    val time: LocalDateTime
        get() = LocalDateTime.parse(timestamp, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"))

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
        "pm2_5" -> pm25
        "pm10" -> pm10
        "no2" -> no2
        "o3" -> o3
        "co" -> co
        "so2" -> so2
        "pollen_grass" -> pollenGrass
        "pollen_tree" -> pollenTree
        "pollen_ragweed" -> pollenRagweed
        "pollen_grass_concentration" -> pollenReadings.grass
        "pollen_alder_concentration" -> pollenReadings.alder
        "pollen_birch_concentration" -> pollenReadings.birch
        "pollen_mugwort_concentration" -> pollenReadings.mugwort
        "pollen_olive_concentration" -> pollenReadings.olive
        "pollen_ragweed_concentration" -> pollenReadings.ragweed
        "wave_height_m" -> waveHeightM
        "wave_period_sec" -> wavePeriodSec
        "wave_direction" -> waveDirection
        "water_temperature" -> waterTemperature
        "tide_height_m" -> tideHeightM
        "soil_moisture_percent" -> soilMoisturePercent
        "heat_alert" -> heatAlert
        "comfort_index" -> comfortIndex
        "sunrise_local_time" -> sunriseLocalTime
        "sunset_local_time" -> sunsetLocalTime
        else -> null
    }
}
