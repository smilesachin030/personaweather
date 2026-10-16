package com.example.personaweather.ui.home

import com.example.personaweather.ui.localization.AppStrings
import java.time.format.DateTimeFormatter

object WeatherBuddyResponder {
    fun reply(question: String, state: HomeUiState, strings: AppStrings): String {
        val query = question.trim().lowercase(strings.locale)
        if (query.isEmpty()) return strings.text("weather_buddy_fallback", "I can help with your local forecast, air quality, alerts, and basic app questions.")

        val asksLocationHelp = matches(query, strings, "weather_buddy_keywords_location", "location|city|place")
        if (asksLocationHelp && matches(query, strings, "weather_buddy_keywords_change", "change|switch|set|where")) {
            return strings.text("weather_buddy_location_help", "Tap the location name at the top of Home to choose another place.")
        }
        if (matches(query, strings, "weather_buddy_keywords_help", "help|how do i use|what can you")) {
            return strings.text(
                "weather_buddy_help",
                "Ask about the current weather, rain chances, air quality, or alerts. You can change your location from the top of Home.",
            )
        }
        if (asksAboutTomorrowTrip(query, strings)) {
            return tomorrowTripReply(query, state, strings)
        }
        if (matches(query, strings, "weather_buddy_keywords_rain", "rain|umbrella")) {
            val nextRain = state.hourlyRows.maxByOrNull { it.rainChancePercent }
                ?: return strings.text("weather_buddy_rain_unavailable", "Rain forecast information is not available right now.")
            val time = nextRain.time.format(DateTimeFormatter.ofPattern("EEE h a", strings.locale))
            return strings.format(
                "weather_buddy_rain_reply",
                "In the available forecast for %1\$s, the highest rain chance is %2$.0f%% around %3\$s.",
                strings.locationName(state.location.name),
                nextRain.rainChancePercent,
                time,
            )
        }
        if (matches(query, strings, "weather_buddy_keywords_air", "air|aqi|pollution")) {
            val current = state.currentRow
                ?: return strings.text("weather_buddy_air_unavailable", "Air-quality information is not available right now.")
            return strings.format(
                "weather_buddy_air_reply",
                "Air quality in %1\$s is %2\$s (AQI %3\$d).",
                strings.locationName(state.location.name),
                current.aqiCategory,
                current.aqiValue,
            )
        }
        if (matches(query, strings, "weather_buddy_keywords_alert", "alert|advisory|warning|heading out")) {
            val advisory = state.advisories.firstOrNull()
                ?: return strings.text("weather_buddy_no_alerts", "There are no active weather advisories for this location right now.")
            return strings.advisory(
                advisory.message,
                advisory.messageTa,
                advisory.messageHi,
            )
        }
        if (matches(query, strings, "weather_buddy_keywords_current", "weather|temperature|temp|condition|outside|wear")) {
            val current = state.currentRow
                ?: return strings.text("weather_buddy_current_unavailable", "Current weather information is not available right now.")
            return strings.format(
                "weather_buddy_current_reply",
                "Current weather in %1\$s: %2\$s, %3$.0f°C (feels like %4$.0f°C).",
                strings.locationName(state.location.name),
                strings.condition(current.condition),
                current.temperature,
                current.feelsLike,
            )
        }
        return strings.text("weather_buddy_fallback", "I can help with your local forecast, air quality, alerts, and basic app questions.")
    }

    private fun matches(query: String, strings: AppStrings, key: String, fallback: String): Boolean =
        strings.text(key, fallback).split('|').any { keyword ->
            keyword.isNotBlank() && query.contains(keyword.trim(), ignoreCase = true)
        }

    private fun asksAboutTomorrowTrip(query: String, strings: AppStrings): Boolean {
        val asksAboutTomorrow = matches(query, strings, "weather_buddy_keywords_tomorrow", "tomorrow|next day")
        val asksAboutGoing = matches(
            query,
            strings,
            "weather_buddy_keywords_trip",
            "can i go|should i go|go to|travel|trip|visit|outside|outdoor|commute",
        )
        return asksAboutTomorrow && asksAboutGoing
    }

    private fun tomorrowTripReply(query: String, state: HomeUiState, strings: AppStrings): String {
        val tomorrow = state.simTime.toLocalDate().plusDays(1).toString()
        val rows = (state.weatherRows.ifEmpty { state.hourlyRows }).filter { it.date == tomorrow }
        if (rows.isEmpty()) {
            return strings.text(
                "weather_buddy_trip_unavailable",
                "I can't give a safe yes/no for tomorrow because the forecast for ${strings.locationName(state.location.name)} isn't available. Check the forecast again before you leave.",
            )
        }

        val requestedLocation = state.locations.firstOrNull { location ->
            location.name.isNotBlank() && query.contains(location.name, ignoreCase = true)
        }
        if (requestedLocation != null && requestedLocation.id != state.location.id) {
            return strings.format(
                "weather_buddy_trip_location_scope",
                "I can't confirm conditions for %1\$s yet; the forecast loaded here is for %2\$s. Select %1\$s from the location picker first, then ask me again.",
                strings.locationName(requestedLocation.name),
                strings.locationName(state.location.name),
            )
        }
        val mentionedDestination = Regex("""\b(?:to|in|at)\s+([a-z][a-z .'-]{1,30}?)(?=\s+tomorrow|\s+today|[?.!,]|$)""")
            .find(query)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            ?.trimEnd()
        val genericDestinations = setOf("school", "work", "office", "the beach", "beach", "the park", "park", "home", "outside")
        if (
            mentionedDestination != null &&
            mentionedDestination !in genericDestinations &&
            !state.location.name.equals(mentionedDestination, ignoreCase = true)
        ) {
            return strings.format(
                "weather_buddy_trip_location_scope",
                "I can't confirm conditions for %1\$s yet; the forecast loaded here is for %2\$s. Select %1\$s from the location picker first, then ask me again.",
                mentionedDestination.replaceFirstChar { it.uppercase(strings.locale) },
                strings.locationName(state.location.name),
            )
        }

        val peakRain = rows.maxOf { it.rainChancePercent }
        val peakAqi = rows.maxOf { it.aqiValue }
        val lowestVisibility = rows.minOf { it.visibilityKm }
        val peakUv = rows.maxOf { it.uvIndex }
        val highTemperature = rows.maxOf { it.temperature }
        val hasFog = rows.any { it.fogFlag > 0 }
        val conditions = strings.format(
            "weather_buddy_trip_conditions",
            "Peak rain chance %1\$.0f%%, peak AQI %2\$d, lowest visibility %3\$.1f km, peak UV %4\$.0f, and high %5\$.0f°C.",
            peakRain,
            peakAqi,
            lowestVisibility,
            peakUv,
            highTemperature,
        )

        val precautions = buildList {
            if (peakRain >= 30.0) add(strings.text("weather_buddy_trip_tip_rain", "an umbrella or raincoat"))
            if (peakAqi > 100) add(strings.text("weather_buddy_trip_tip_air", "a well-fitting mask"))
            if (hasFog || lowestVisibility < 5.0) add(strings.text("weather_buddy_trip_tip_fog", "extra travel time and slow, careful driving"))
            if (peakUv >= 6.0) add(strings.text("weather_buddy_trip_tip_sun", "sunscreen and a hat"))
            if (highTemperature >= 35.0) add(strings.text("weather_buddy_trip_tip_heat", "water and a break from the hottest hours"))
        }
        val packing = precautions.joinToString(", ").ifEmpty {
            strings.text("weather_buddy_trip_tip_none", "no special weather gear")
        }
        val highRisk = peakRain >= 80.0 || peakAqi >= 151 || lowestVisibility < 2.0 || highTemperature >= 42.0
        val caution = highRisk || peakRain >= 30.0 || peakAqi > 100 || hasFog || lowestVisibility < 5.0 ||
            peakUv >= 6.0 || highTemperature >= 35.0
        val location = strings.locationName(state.location.name)

        return when {
            highRisk -> strings.format(
                "weather_buddy_trip_avoid",
                "I'd avoid or postpone outdoor plans in %1\$s tomorrow if you can. %2\$s If travel is necessary, take extra care and bring %3\$s. Forecasts can change.",
                location,
                conditions,
                packing,
            )
            caution -> strings.format(
                "weather_buddy_trip_caution",
                "You can go to %1\$s tomorrow, but conditions are mixed. %2\$s For safety, bring %3\$s. Forecasts can change.",
                location,
                conditions,
                packing,
            )
            else -> strings.format(
                "weather_buddy_trip_clear",
                "Yes — tomorrow looks suitable for going to %1\$s. %2\$s No significant weather risks show in the available forecast; take %3\$s just in case.",
                location,
                conditions,
                packing,
            )
        }
    }
}
