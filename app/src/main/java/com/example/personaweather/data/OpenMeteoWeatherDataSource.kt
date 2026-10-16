package com.example.personaweather.data

import com.example.personaweather.core.engine.Labels
import com.example.personaweather.core.engine.EventComfortIndex
import com.example.personaweather.core.model.Location
import com.example.personaweather.core.model.PollenReadings
import com.example.personaweather.core.model.WeatherRow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.concurrent.ConcurrentHashMap

class OpenMeteoWeatherDataSource(
    private val locationsProvider: suspend () -> List<Location>,
    private val responseCacheDirectory: File,
) : WeatherDataSource {
    private val seriesCache = ConcurrentHashMap<String, CachedSeries>()
    private val sourceLabels = ConcurrentHashMap<String, String>()
    private val dailyRainChanceCache = ConcurrentHashMap<String, Map<LocalDate, Double>>()

    override suspend fun locations(): List<Location> =
        locationsProvider().distinctBy(Location::id)

    override suspend fun series(locationId: String): List<WeatherRow> {
        val location = locations().firstOrNull { it.id == locationId }
            ?: throw IllegalArgumentException("Unknown location: $locationId")
        return series(location)
    }

    override suspend fun series(location: Location): List<WeatherRow> = withContext(Dispatchers.IO) {
        val cacheKey = cacheKey(location)
        val now = System.currentTimeMillis()
        seriesCache[cacheKey]?.takeIf { now - it.cachedAtMillis < CACHE_TTL_MS }?.let { cached ->
            sourceLabels[location.id] = cachedLabel(cached.cachedAtMillis, fromDisk = false)
            return@withContext cached.rows
        }
        val locationCache = File(responseCacheDirectory, cacheKey).apply { mkdirs() }
        val response = coroutineScope {
            val weather = async { requestJson(weatherUrl(location), File(locationCache, "weather.json")) }
            val air = async { requestJson(airQualityUrl(location), File(locationCache, "air.json")) }
            val marine = if (location.isCoastal) async {
                requestJson(marineUrl(location), File(locationCache, "marine.json"))
            } else {
                null
            }
            Triple(weather.await(), air.await(), marine?.await())
        }
        val loaded = mapRows(location, response.first.json, response.second.json, response.third?.json)
        check(loaded.isNotEmpty()) { "Open-Meteo returned no complete forecast for ${location.name}." }
        val usedDiskCache = response.first.usedDiskCache || response.second.usedDiskCache || response.third?.usedDiskCache == true
        val dataTimestamp = listOfNotNull(response.first.savedAtMillis, response.second.savedAtMillis, response.third?.savedAtMillis).minOrNull()
        sourceLabels[location.id] = cachedLabel(dataTimestamp, fromDisk = usedDiskCache)
        seriesCache[cacheKey] = CachedSeries(System.currentTimeMillis(), loaded)
        loaded
    }

    fun statusLabel(locationId: String): String = sourceLabels[locationId] ?: "Open-Meteo forecast"

    suspend fun dailyRainChanceByDate(location: Location): Map<LocalDate, Double> {
        val cached = dailyRainChanceCache[location.id]
        if (cached != null) return cached
        val cacheKey = cacheKey(location)
        val locationCache = File(responseCacheDirectory, cacheKey)
        val response = requestJson(weatherUrlDaily(location), File(locationCache, "weather_daily.json"))
        val dailyRainChance = mapDailyRainChance(response.json)
        dailyRainChanceCache[location.id] = dailyRainChance
        return dailyRainChance
    }

    override suspend fun dataRange(): Pair<LocalDateTime, LocalDateTime> {
        val start = LocalDate.now(ZoneId.of("Asia/Kolkata")).atStartOfDay()
        return start to start.plusDays(4).withHour(23)
    }

    private fun mapRows(
        location: Location,
        weatherResponse: JSONObject,
        airResponse: JSONObject,
        marineResponse: JSONObject?,
    ): List<WeatherRow> {
        val weather = weatherResponse.getJSONObject("hourly")
        val air = airResponse.getJSONObject("hourly")
        val marine = marineResponse?.optJSONObject("hourly")
        dailyRainChanceCache[location.id] = mapDailyRainChance(weatherResponse)
        val sunTimesByDate = mapSunTimes(weatherResponse)
        val times = weather.getJSONArray("time")
        val airTimes = air.getJSONArray("time")
        val airIndex = airTimes.indices().associateBy { airTimes.getString(it) }
        val marineIndex = marine?.getJSONArray("time")?.let { array ->
            array.indices().associateBy { array.getString(it) }
        }.orEmpty()

        return buildList {
            for (index in 0 until times.length()) {
                val timestamp = times.getString(index)
                val airPosition = airIndex[timestamp] ?: continue
                val aqi = air.number("us_aqi", airPosition)?.toInt() ?: continue
                val time = LocalDateTime.parse(timestamp, TIMESTAMP_FORMAT)
                val temperature = weather.number("temperature_2m", index) ?: continue
                val humidity = weather.number("relative_humidity_2m", index) ?: continue
                val weatherCode = weather.number("weather_code", index)?.toInt() ?: continue
                val rainChance = weather.number("precipitation_probability", index) ?: continue
                val apparentTemperature = weather.number("apparent_temperature", index) ?: continue
                val windSpeed = weather.number("wind_speed_10m", index) ?: continue
                val windDirection = weather.number("wind_direction_10m", index)?.let(::compassDirection) ?: continue
                val pressure = weather.number("pressure_msl", index) ?: continue
                val dewPoint = weather.number("dew_point_2m", index) ?: continue
                val uvIndex = weather.number("uv_index", index) ?: continue
                val rainfall = weather.number("precipitation", index) ?: continue
                val visibility = weather.number("visibility", index) ?: continue
                val soilMoisture = weather.number("soil_moisture_0_to_1cm", index) ?: continue
                val pm25 = air.number("pm2_5", airPosition) ?: continue
                val pm10 = air.number("pm10", airPosition) ?: continue
                val no2 = air.number("nitrogen_dioxide", airPosition) ?: continue
                val o3 = air.number("ozone", airPosition) ?: continue
                val co = air.number("carbon_monoxide", airPosition) ?: continue
                val so2 = air.number("sulphur_dioxide", airPosition) ?: continue
                val pollutionAqis = listOf(
                    "PM2.5" to air.number("us_aqi_pm2_5", airPosition),
                    "PM10" to air.number("us_aqi_pm10", airPosition),
                    "NO2" to air.number("us_aqi_nitrogen_dioxide", airPosition),
                    "O3" to air.number("us_aqi_ozone", airPosition),
                    "SO2" to air.number("us_aqi_sulphur_dioxide", airPosition),
                    "CO" to air.number("us_aqi_carbon_monoxide", airPosition),
                )
                val dominantPollutant = pollutionAqis.maxByOrNull { it.second ?: Double.NEGATIVE_INFINITY }
                    ?.takeIf { it.second != null }?.first ?: "Unavailable"
                val marinePosition = marineIndex[timestamp]

                add(
                    WeatherRow(
                        id = index,
                        locationId = location.id,
                        timestamp = timestamp,
                        date = time.toLocalDate().toString(),
                        hour = time.hour,
                        temperature = temperature,
                        feelsLike = apparentTemperature,
                        humidity = humidity,
                        windSpeed = windSpeed,
                        windDirection = windDirection,
                        condition = displayCondition(weatherCode),
                        pressureHpa = pressure,
                        dewPoint = dewPoint,
                        uvIndex = uvIndex,
                        rainChancePercent = rainChance,
                        rainfallMm = rainfall,
                        visibilityKm = visibility / 1000.0,
                        fogFlag = if (weatherCode == 45 || weatherCode == 48) 1 else 0,
                        aqiValue = aqi,
                        aqiCategory = Labels.aqi(aqi),
                        dominantPollutant = dominantPollutant,
                        pm25 = pm25,
                        pm10 = pm10,
                        no2 = no2,
                        o3 = o3,
                        co = co / 1000.0,
                        so2 = so2,
                        pollenGrass = "Unavailable",
                        pollenTree = "Unavailable",
                        pollenRagweed = "Unavailable",
                        pollenReadings = PollenReadings(
                            grass = air.number("grass_pollen", airPosition),
                            alder = air.number("alder_pollen", airPosition),
                            birch = air.number("birch_pollen", airPosition),
                            mugwort = air.number("mugwort_pollen", airPosition),
                            olive = air.number("olive_pollen", airPosition),
                            ragweed = air.number("ragweed_pollen", airPosition),
                        ),
                        waveHeightM = marine?.number("wave_height", marinePosition),
                        wavePeriodSec = marine?.number("wave_period", marinePosition),
                        waveDirection = marine?.number("wave_direction", marinePosition)?.let(::compassDirection),
                        waterTemperature = marine?.number("sea_surface_temperature", marinePosition),
                        tideHeightM = marine?.number("sea_level_height_msl", marinePosition),
                        soilMoisturePercent = soilMoisture * 100.0,
                        heatAlert = if (temperature >= 34.0) 1 else 0,
                        comfortIndex = EventComfortIndex.assess(
                            rainChancePercent = rainChance,
                            aqi = aqi,
                            temperatureC = temperature,
                            feelsLikeC = apparentTemperature,
                        ).label,
                        sunriseLocalTime = sunTimesByDate[time.toLocalDate()]?.first,
                        sunsetLocalTime = sunTimesByDate[time.toLocalDate()]?.second,
                    ),
                )
            }
        }
    }

    private fun weatherUrl(location: Location): String = buildUrl(
        "https://api.open-meteo.com/v1/forecast",
        location,
        "hourly" to "temperature_2m,relative_humidity_2m,dew_point_2m,apparent_temperature,precipitation_probability,precipitation,weather_code,wind_speed_10m,wind_direction_10m,pressure_msl,uv_index,visibility,soil_moisture_0_to_1cm",
        "daily" to "temperature_2m_max,temperature_2m_min,precipitation_probability_max,precipitation_sum,sunrise,sunset",
        "forecast_days" to "5",
        "timezone" to "Asia/Kolkata",
    )

    private fun weatherUrlDaily(location: Location): String = buildUrl(
        "https://api.open-meteo.com/v1/forecast",
        location,
        "daily" to "precipitation_probability_max",
        "forecast_days" to "5",
        "timezone" to "Asia/Kolkata",
    )

    private fun airQualityUrl(location: Location): String = buildUrl(
        "https://air-quality-api.open-meteo.com/v1/air-quality",
        location,
        "hourly" to "us_aqi,us_aqi_pm2_5,us_aqi_pm10,us_aqi_nitrogen_dioxide,us_aqi_ozone,us_aqi_sulphur_dioxide,us_aqi_carbon_monoxide,pm2_5,pm10,nitrogen_dioxide,ozone,carbon_monoxide,sulphur_dioxide,grass_pollen,alder_pollen,birch_pollen,mugwort_pollen,olive_pollen,ragweed_pollen",
        "forecast_days" to "5",
        "domains" to "auto",
        "timezone" to "Asia/Kolkata",
    )

    private fun marineUrl(location: Location): String = buildUrl(
        "https://marine-api.open-meteo.com/v1/marine",
        location,
        "hourly" to "wave_height,wave_period,wave_direction,sea_surface_temperature,sea_level_height_msl",
        "forecast_days" to "5",
        "cell_selection" to "sea",
        "timezone" to "Asia/Kolkata",
    )

    private fun mapDailyRainChance(response: JSONObject): Map<LocalDate, Double> {
        val daily = response.optJSONObject("daily") ?: return emptyMap()
        val dates = daily.optJSONArray("time") ?: return emptyMap()
        val probabilities = daily.optJSONArray("precipitation_probability_max") ?: return emptyMap()
        val builder = linkedMapOf<LocalDate, Double>()
        for (index in 0 until minOf(dates.length(), probabilities.length())) {
            val date = dates.optString(index).takeIf(String::isNotBlank) ?: continue
            val value = probabilities.optDouble(index, Double.NaN)
            if (!value.isNaN()) {
                builder[LocalDate.parse(date)] = value
            }
        }
        return builder
    }

    private fun mapSunTimes(response: JSONObject): Map<LocalDate, Pair<String?, String?>> {
        val daily = response.optJSONObject("daily") ?: return emptyMap()
        val dates = daily.optJSONArray("time") ?: return emptyMap()
        val sunrise = daily.optJSONArray("sunrise") ?: return emptyMap()
        val sunset = daily.optJSONArray("sunset") ?: return emptyMap()
        return buildMap {
            for (index in 0 until dates.length()) {
                val dateText = dates.optString(index).takeIf(String::isNotBlank) ?: continue
                val date = try {
                    LocalDate.parse(dateText)
                } catch (_: DateTimeParseException) {
                    continue
                }
                put(date, sunrise.localTimeAt(index) to sunset.localTimeAt(index))
            }
        }
    }

    private fun buildUrl(base: String, location: Location, vararg parameters: Pair<String, String>): String {
        val query = buildList {
            add("latitude=${location.lat}")
            add("longitude=${location.lon}")
            parameters.forEach { (key, value) ->
                add("$key=${URLEncoder.encode(value, StandardCharsets.UTF_8.name())}")
            }
        }.joinToString("&")
        return "$base?$query"
    }

    private fun requestJson(url: String, cacheFile: File): ApiResponse {
        return try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = CONNECTION_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.requestMethod = "GET"
            val body = try {
                val statusCode = connection.responseCode
                val stream = if (statusCode in 200..299) connection.inputStream else connection.errorStream
                val responseBody = stream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }.orEmpty()
                check(statusCode in 200..299) { "Open-Meteo request failed ($statusCode)." }
                responseBody
            } finally {
                connection.disconnect()
            }
            val json = JSONObject(body).also { response ->
                check(!response.optBoolean("error")) { response.optString("reason", "Open-Meteo request failed.") }
            }
            val savedAt = System.currentTimeMillis()
            saveResponse(cacheFile, body)
            ApiResponse(json, usedDiskCache = false, savedAtMillis = savedAt)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            if (!cacheFile.isFile) throw failure
            val savedAt = cacheFile.lastModified()
            ApiResponse(JSONObject(cacheFile.readText(StandardCharsets.UTF_8)), usedDiskCache = true, savedAtMillis = savedAt)
        }
    }

    private fun saveResponse(cacheFile: File, body: String) {
        val cacheDirectory = cacheFile.parentFile ?: error("Forecast cache path has no parent directory.")
        check(cacheDirectory.isDirectory || cacheDirectory.mkdirs()) { "Could not create the forecast cache directory." }
        val temporary = File(cacheDirectory, "${cacheFile.name}.tmp")
        temporary.writeText(body, StandardCharsets.UTF_8)
        try {
            Files.move(temporary.toPath(), cacheFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (_: java.nio.file.AtomicMoveNotSupportedException) {
            Files.move(temporary.toPath(), cacheFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun cachedLabel(savedAtMillis: Long?, fromDisk: Boolean): String {
        val timestamp = savedAtMillis?.let {
            DateTimeFormatter.ofPattern("MMM d, h:mm a", java.util.Locale.ENGLISH)
                .withZone(ZoneId.of("Asia/Kolkata"))
                .format(Instant.ofEpochMilli(it))
        }
        val source = if (fromDisk) "Open-Meteo saved forecast" else "Open-Meteo forecast"
        return "$source${timestamp?.let { " · updated $it" }.orEmpty()}"
    }

    private fun cacheKey(location: Location): String {
        val identity = "${location.id}|${location.lat}|${location.lon}"
        return MessageDigest.getInstance("SHA-256")
            .digest(identity.toByteArray(StandardCharsets.UTF_8))
            .take(CACHE_KEY_BYTES)
            .joinToString("") { byte -> "%02x".format(byte) }
    }

    private fun JSONObject.number(name: String, index: Int?): Double? {
        if (index == null) return null
        val array = optJSONArray(name) ?: return null
        if (index !in 0 until array.length() || array.isNull(index)) return null
        return array.optDouble(index).takeIf(Double::isFinite)
    }

    private fun displayCondition(code: Int): String = when (code) {
        0 -> "Clear"
        1 -> "Mainly clear"
        2 -> "Partly cloudy"
        3 -> "Overcast"
        45, 48 -> "Fog"
        51, 53, 55 -> "Drizzle"
        56, 57 -> "Freezing drizzle"
        61, 63, 65 -> "Rain"
        66, 67 -> "Freezing rain"
        71, 73, 75, 77 -> "Snow"
        80, 81, 82 -> "Rain showers"
        85, 86 -> "Snow showers"
        95 -> "Thunderstorm"
        96, 99 -> "Thunderstorm with hail"
        else -> "Unknown"
    }

    private fun compassDirection(degrees: Double): String {
        val directions = listOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW")
        return directions[(degrees / 22.5).toInt() % directions.size]
    }

    private fun JSONArray.localTimeAt(index: Int): String? {
        if (index !in 0 until length() || isNull(index)) return null
        val value = optString(index).substringAfter('T', "")
        return try {
            LocalTime.parse(value).format(DateTimeFormatter.ofPattern("HH:mm"))
        } catch (_: DateTimeParseException) {
            null
        }
    }

    private fun JSONArray.indices(): IntRange = 0 until length()

    private data class CachedSeries(val cachedAtMillis: Long, val rows: List<WeatherRow>)
    private data class ApiResponse(val json: JSONObject, val usedDiskCache: Boolean, val savedAtMillis: Long)

    companion object {
        private const val CONNECTION_TIMEOUT_MS = 10_000
        private const val READ_TIMEOUT_MS = 10_000
        private const val CACHE_TTL_MS = 15 * 60 * 1000L
        private const val CACHE_KEY_BYTES = 16
        private val TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
    }
}
