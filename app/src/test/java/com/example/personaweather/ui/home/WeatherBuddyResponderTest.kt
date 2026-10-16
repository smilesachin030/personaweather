package com.example.personaweather.ui.home

import com.example.personaweather.core.model.Location
import com.example.personaweather.core.model.WeatherRow
import com.example.personaweather.ui.localization.AppStrings
import java.time.LocalDateTime
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherBuddyResponderTest {
    @Test
    fun answersCurrentWeatherFromAvailableLocalData() {
        val state = HomeUiState(
            location = Location("chennai", "Chennai", 13.0, 80.2),
            currentRow = weatherRow("2026-09-28T09:00", 10.0),
        )

        val reply = WeatherBuddyResponder.reply("What's the weather now?", state, AppStrings.English)

        assertTrue(reply.contains("Chennai"))
        assertTrue(reply.contains("Clear"))
        assertTrue(reply.contains("29°C"))
        assertTrue(reply.contains("30°C"))
    }

    @Test
    fun answersRainQuestionWithHighestAvailableForecastChance() {
        val state = HomeUiState(
            location = Location("chennai", "Chennai", 13.0, 80.2),
            hourlyRows = listOf(
                weatherRow("2026-09-28T09:00", 10.0),
                weatherRow("2026-09-28T13:00", 65.0),
                weatherRow("2026-09-28T15:00", 40.0),
            ),
        )

        val reply = WeatherBuddyResponder.reply("Will it rain soon?", state, AppStrings.English)

        assertTrue(reply.contains("65%"))
        assertTrue(reply.contains("1 PM"))
    }

    @Test
    fun reportsUnavailableDataInsteadOfInventingAnAnswer() {
        val reply = WeatherBuddyResponder.reply("What's the weather now?", HomeUiState(), AppStrings.English)

        assertTrue(reply.contains("not available"))
    }

    @Test
    fun answersLocalizedSuggestedPromptsAndUnsupportedQuestionsClearly() {
        val state = HomeUiState(hourlyRows = listOf(weatherRow("2026-09-28T13:00", 65.0)))
        val tamilReply = WeatherBuddyResponder.reply("விரைவில் மழை பெய்யுமா?", state, AppStrings.Tamil)
        val unsupportedReply = WeatherBuddyResponder.reply("Tell me a joke", state, AppStrings.English)

        assertTrue(tamilReply.contains("65%"))
        assertTrue(unsupportedReply.contains("local forecast"))
    }

    @Test
    fun recommendsGoingWhenTomorrowForecastIsClear() {
        val state = HomeUiState(
            location = Location("chennai", "Chennai", 13.0, 80.2),
            simTime = LocalDateTime.parse("2026-09-28T09:00"),
            weatherRows = listOf(weatherRow("2026-09-29T09:00", 0.0).copy(uvIndex = 3.0)),
        )

        val reply = WeatherBuddyResponder.reply("Can I go out tomorrow?", state, AppStrings.English)

        assertTrue(reply.startsWith("Yes"))
        assertTrue(reply.contains("Chennai"))
        assertTrue(reply.contains("AQI 52"))
    }

    @Test
    fun recommendsPrecautionsForRainAndStrongSun() {
        val state = HomeUiState(
            simTime = LocalDateTime.parse("2026-09-28T09:00"),
            weatherRows = listOf(weatherRow("2026-09-29T09:00", 55.0)),
        )

        val reply = WeatherBuddyResponder.reply("Should I go outside tomorrow?", state, AppStrings.English)

        assertTrue(reply.contains("conditions are mixed"))
        assertTrue(reply.contains("umbrella or raincoat"))
        assertTrue(reply.contains("sunscreen and a hat"))
    }

    @Test
    fun recommendsAvoidingOutdoorPlansWhenFogMakesVisibilityPoor() {
        val state = HomeUiState(
            simTime = LocalDateTime.parse("2026-09-28T09:00"),
            weatherRows = listOf(weatherRow("2026-09-29T09:00", 0.0).copy(fogFlag = 1, visibilityKm = 1.0, uvIndex = 2.0)),
        )

        val reply = WeatherBuddyResponder.reply("Can I go out tomorrow?", state, AppStrings.English)

        assertTrue(reply.contains("avoid or postpone"))
        assertTrue(reply.contains("slow, careful driving"))
    }

    @Test
    fun doesNotGiveTomorrowAdviceWhenForecastOrDestinationIsUnavailable() {
        val noForecast = WeatherBuddyResponder.reply(
            "Can I go out tomorrow?",
            HomeUiState(),
            AppStrings.English,
        )
        val otherLocation = Location("dvhoo", "Dvhoo", 0.0, 0.0)
        val destinationMismatch = WeatherBuddyResponder.reply(
            "Can I go to Dvhoo tomorrow?",
            HomeUiState(
                location = Location("chennai", "Chennai", 13.0, 80.2),
                locations = listOf(otherLocation),
                simTime = LocalDateTime.parse("2026-09-28T09:00"),
                weatherRows = listOf(weatherRow("2026-09-29T09:00", 0.0)),
            ),
            AppStrings.English,
        )

        assertTrue(noForecast.contains("isn't available"))
        assertTrue(destinationMismatch.contains("Dvhoo"))
    }

    private fun weatherRow(timestamp: String, rainChance: Double) = WeatherRow(
        id = 1,
        locationId = "chennai",
        timestamp = timestamp,
        date = timestamp.substringBefore('T'),
        hour = timestamp.substringAfter('T').substringBefore(':').toInt(),
        temperature = 29.0,
        feelsLike = 30.0,
        humidity = 64.0,
        windSpeed = 8.0,
        windDirection = "NE",
        condition = "Clear",
        pressureHpa = 1012.0,
        dewPoint = 24.0,
        uvIndex = 7.0,
        rainChancePercent = rainChance,
        rainfallMm = 0.0,
        visibilityKm = 9.0,
        fogFlag = 0,
        aqiValue = 52,
        aqiCategory = "Moderate",
        dominantPollutant = "PM2.5",
        pm25 = 12.0,
        pm10 = 20.0,
        no2 = 0.0,
        o3 = 0.0,
        co = 0.0,
        so2 = 0.0,
        pollenGrass = "Unavailable",
        pollenTree = "Unavailable",
        pollenRagweed = "Unavailable",
        waveHeightM = null,
        wavePeriodSec = null,
        waveDirection = null,
        waterTemperature = null,
        tideHeightM = null,
        soilMoisturePercent = 45.0,
        heatAlert = 0,
        comfortIndex = "Good",
    )
}
