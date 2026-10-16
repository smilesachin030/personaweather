package com.example.personaweather.data

import com.example.personaweather.core.model.Location
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class OpenMeteoGeocodingClient(
    private val fetchResponse: (String) -> String = ::fetch,
) {
    suspend fun search(query: String, languageCode: String = "en"): List<Location> {
        val normalizedQuery = query.trim()
        if (normalizedQuery.length < MIN_QUERY_LENGTH) return emptyList()
        val language = languageCode.substringBefore('-').lowercase().let {
            it.takeIf { code -> code in SUPPORTED_LANGUAGES } ?: "en"
        }
        val body = withContext(Dispatchers.IO) { fetchResponse(searchUrl(normalizedQuery, language)) }
        val response = JSONObject(body)
        check(!response.optBoolean("error")) {
            response.optString("reason", "Place search is unavailable.")
        }
        val results = response.optJSONArray("results") ?: return emptyList()
        return buildList {
            for (index in 0 until results.length()) {
                val result = results.optJSONObject(index) ?: continue
                if (!result.optString("country_code").equals("IN", ignoreCase = true)) continue
                val id = result.optLong("id", -1L).takeIf { it > 0 } ?: continue
                val name = result.optString("name").takeIf(String::isNotBlank) ?: continue
                val latitude = result.optDouble("latitude", Double.NaN)
                val longitude = result.optDouble("longitude", Double.NaN)
                if (!isWithinIndia(latitude, longitude)) continue

                add(
                    Location(
                        id = "open-meteo:IN:$id",
                        name = name,
                        lat = latitude,
                        lon = longitude,
                        character = "Live forecast",
                        adminArea = result.optString("admin1").takeIf(String::isNotBlank),
                        countryCode = "IN",
                        geocodingId = id,
                        timeZone = result.optString("timezone").takeIf(String::isNotBlank),
                    ),
                )
            }
        }.distinctBy(Location::id)
    }

    private fun searchUrl(query: String, language: String): String =
        "$ENDPOINT?name=${URLEncoder.encode(query, StandardCharsets.UTF_8.name())}" +
            "&count=$RESULT_LIMIT&language=$language&format=json&countryCode=IN"

    private fun isWithinIndia(latitude: Double, longitude: Double): Boolean =
        latitude.isFinite() && longitude.isFinite() &&
            latitude in INDIA_MIN_LATITUDE..INDIA_MAX_LATITUDE &&
            longitude in INDIA_MIN_LONGITUDE..INDIA_MAX_LONGITUDE

    private companion object {
        const val MIN_QUERY_LENGTH = 2
        const val RESULT_LIMIT = 10
        val SUPPORTED_LANGUAGES = setOf("en", "hi", "ta")
        const val INDIA_MIN_LATITUDE = 6.0
        const val INDIA_MAX_LATITUDE = 38.0
        const val INDIA_MIN_LONGITUDE = 68.0
        const val INDIA_MAX_LONGITUDE = 98.0
        const val CONNECTION_TIMEOUT_MS = 10_000
        const val READ_TIMEOUT_MS = 10_000
        const val ENDPOINT = "https://geocoding-api.open-meteo.com/v1/search"

        fun fetch(url: String): String {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = CONNECTION_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.requestMethod = "GET"
            return try {
                val statusCode = connection.responseCode
                val stream = if (statusCode in 200..299) connection.inputStream else connection.errorStream
                val body = stream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }.orEmpty()
                check(statusCode in 200..299) { "Place search failed ($statusCode)." }
                body
            } finally {
                connection.disconnect()
            }
        }
    }
}