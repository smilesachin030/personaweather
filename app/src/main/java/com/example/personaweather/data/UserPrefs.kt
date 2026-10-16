package com.example.personaweather.data

import android.content.Context
import android.content.SharedPreferences
import com.example.personaweather.core.model.Location
import org.json.JSONArray
import org.json.JSONObject

class UserPrefs {
    private val prefs: SharedPreferences

    constructor(context: Context) {
        prefs = context.getSharedPreferences("persona_weather_prefs", Context.MODE_PRIVATE)
    }

    constructor(sharedPreferences: SharedPreferences) {
        prefs = sharedPreferences
    }

    var onboarded: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDED, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDED, value).apply()

    var personas: List<String>
        get() = prefs.getString(KEY_PERSONAS, null)
            ?.split(',')
            ?.filter { it.isNotBlank() }
            ?: defaultPersons
        set(value) = prefs.edit().putString(KEY_PERSONAS, value.joinToString(",")).apply()

    var locationId: String
        get() = prefs.getString(KEY_LOCATION_ID, DEFAULT_LOCATION_ID) ?: DEFAULT_LOCATION_ID
        set(value) = prefs.edit().putString(KEY_LOCATION_ID, value).apply()

    val rememberedLocations: List<Location>
        get() {
            val serialized = prefs.getString(KEY_REMEMBERED_LOCATIONS, null) ?: return emptyList()
            return runCatching {
                val locations = JSONArray(serialized)
                buildList {
                    for (index in 0 until locations.length()) {
                        val item = locations.optJSONObject(index) ?: continue
                        val id = item.optString("id").takeIf(String::isNotBlank) ?: continue
                        val name = item.optString("name").takeIf(String::isNotBlank) ?: continue
                        val lat = item.optDouble("lat", Double.NaN)
                        val lon = item.optDouble("lon", Double.NaN)
                        if (!lat.isFinite() || !lon.isFinite()) continue
                        add(
                            Location(
                                id = id,
                                name = name,
                                lat = lat,
                                lon = lon,
                                distanceKmFromHub = item.optDouble("distanceKmFromHub", 0.0),
                                direction = item.optString("direction"),
                                character = item.optString("character"),
                                isCoastal = item.optBoolean("isCoastal", false),
                                adminArea = item.optString("adminArea").takeIf(String::isNotBlank),
                                countryCode = item.optString("countryCode", "IN"),
                                geocodingId = if (item.isNull("geocodingId")) null else item.optLong("geocodingId"),
                                timeZone = item.optString("timeZone").takeIf(String::isNotBlank),
                            ),
                        )
                    }
                }
            }.getOrDefault(emptyList())
        }

    fun rememberedLocation(locationId: String): Location? =
        rememberedLocations.firstOrNull { it.id == locationId }

    fun rememberLocation(location: Location) {
        if (location.id.isBlank() || !location.lat.isFinite() || !location.lon.isFinite()) return
        val locations = (rememberedLocations.filterNot { it.id == location.id } + location)
        val serialized = JSONArray().apply {
            locations.forEach { place ->
                put(
                    JSONObject()
                        .put("id", place.id)
                        .put("name", place.name)
                        .put("lat", place.lat)
                        .put("lon", place.lon)
                        .put("distanceKmFromHub", place.distanceKmFromHub)
                        .put("direction", place.direction)
                        .put("character", place.character)
                        .put("isCoastal", place.isCoastal)
                        .put("adminArea", place.adminArea)
                        .put("countryCode", place.countryCode)
                        .put("geocodingId", place.geocodingId)
                        .put("timeZone", place.timeZone),
                )
            }
        }
        prefs.edit().putString(KEY_REMEMBERED_LOCATIONS, serialized.toString()).apply()
    }

    var savedDestinationIds: List<String>
        get() = prefs.getString(KEY_SAVED_DESTINATIONS, null)
            ?.split(',')
            ?.filter { it.isNotBlank() }
            ?: emptyList()
        set(value) = prefs.edit().putString(KEY_SAVED_DESTINATIONS, value.distinct().joinToString(",")).apply()

    var cardsOrder: List<String>
        get() = prefs.getString(KEY_CARDS_ORDER, null)
            ?.split(',')
            ?.filter { it.isNotBlank() }
            ?: emptyList()
        set(value) = prefs.edit().putString(KEY_CARDS_ORDER, value.joinToString(",")).apply()

    var cardsDisabled: Set<String>
        get() = prefs.getStringSet(KEY_CARDS_DISABLED, emptySet())?.toSet() ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_CARDS_DISABLED, value.toSet()).apply()

    var cardsCustomized: Boolean
        get() = prefs.getBoolean(KEY_CARDS_CUSTOMIZED, false)
        set(value) = prefs.edit().putBoolean(KEY_CARDS_CUSTOMIZED, value).apply()

    var language: String
        get() = prefs.getString(KEY_LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE
        set(value) = prefs.edit().putString(KEY_LANGUAGE, value).apply()

    var largeTextEnabled: Boolean
        get() = prefs.getBoolean(KEY_LARGE_TEXT_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_LARGE_TEXT_ENABLED, value).apply()

    var highContrastEnabled: Boolean
        get() = prefs.getBoolean(KEY_HIGH_CONTRAST_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_HIGH_CONTRAST_ENABLED, value).apply()

    var clothAlarmEnabled: Boolean
        get() = prefs.getBoolean(KEY_CLOTH_ALARM_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_CLOTH_ALARM_ENABLED, value).apply()

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, value).apply()

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        val defaultPersons: List<String> = listOf("health_conscious", "outdoor_fitness")
        private const val DEFAULT_LOCATION_ID = "nungambakkam"
        private const val DEFAULT_LANGUAGE = "en"
        private const val KEY_ONBOARDED = "onboarded"
        private const val KEY_PERSONAS = "personas"
        private const val KEY_LOCATION_ID = "location_id"
        private const val KEY_REMEMBERED_LOCATIONS = "remembered_locations"
        private const val KEY_SAVED_DESTINATIONS = "saved_destinations"
        private const val KEY_CARDS_ORDER = "cards_order"
        private const val KEY_CARDS_DISABLED = "cards_disabled"
        private const val KEY_CARDS_CUSTOMIZED = "cards_customized"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_LARGE_TEXT_ENABLED = "large_text_enabled"
        private const val KEY_HIGH_CONTRAST_ENABLED = "high_contrast_enabled"
        private const val KEY_CLOTH_ALARM_ENABLED = "cloth_alarm_enabled"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
    }
}
