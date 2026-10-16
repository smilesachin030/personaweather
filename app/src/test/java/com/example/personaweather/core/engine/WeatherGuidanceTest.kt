package com.example.personaweather.core.engine

import com.example.personaweather.core.model.Persona
import com.example.personaweather.core.model.PollenReadings
import com.example.personaweather.core.model.PollenSpecies
import com.example.personaweather.core.model.Rule
import com.example.personaweather.core.model.WeatherRow
import com.example.personaweather.ui.localization.AppStrings
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherGuidanceTest {
    @Test
    fun calendarEventAdviceIsOrganiserSpecificAndRequiresEventPlannerPersona() {
        val start = LocalDateTime.of(2025, 6, 10, 12, 0)
        val forecast = listOf(weatherRow(start, uvIndex = 9.0, aqiValue = 180))
        val attendeePersona = Persona(
            id = "health_conscious",
            label = "Health-conscious",
            rules = listOf(
                Rule("hc_aqi_alert", "aqi_value", ">=", 150, "alert", "Avoid outdoor activity."),
            ),
        )
        val eventPlanner = Persona(
            id = "event_planner",
            label = "Event Planner",
            rules = listOf(
                Rule(
                    "ep_uv_caution",
                    "uv_index",
                    ">=",
                    8,
                    "caution",
                    "Identify shaded spots and direct attendees there.",
                ),
            ),
        )

        val enabledEvaluation = CalendarEventWeatherEvaluator.evaluate(
            series = forecast,
            eventStart = start,
            eventEnd = start.plusHours(1),
            personas = listOf(attendeePersona, eventPlanner),
            isCoastal = false,
            isOutdoor = true,
            isOrganizer = true,
        )
        val nonOrganizerEvaluation = CalendarEventWeatherEvaluator.evaluate(
            series = forecast,
            eventStart = start,
            eventEnd = start.plusHours(1),
            personas = listOf(attendeePersona, eventPlanner),
            isCoastal = false,
            isOutdoor = true,
            isOrganizer = false,
        )
        val disabledEvaluation = CalendarEventWeatherEvaluator.evaluate(
            series = forecast,
            eventStart = start,
            eventEnd = start.plusHours(1),
            personas = listOf(attendeePersona),
            isCoastal = false,
            isOutdoor = true,
            isOrganizer = true,
        )

        assertEquals(listOf("event_planner"), enabledEvaluation?.advisories?.map { it.personaId })
        assertTrue(enabledEvaluation?.advisories?.single()?.message?.contains("attendees") == true)
        assertEquals(listOf("health_conscious"), nonOrganizerEvaluation?.advisories?.map { it.personaId })
        assertTrue(disabledEvaluation?.advisories.isNullOrEmpty())
    }

    @Test
    fun packingTipsReflectForecastAndStayEmptyWhenNoExtraGearIsNeeded() {
        val start = LocalDateTime.of(2025, 6, 10, 9, 0)
        val forecast = (0 until 24).map { offset ->
            weatherRow(
                start.plusHours(offset.toLong()),
                uvIndex = if (offset == 3) 4.0 else 0.0,
                rainChancePercent = if (offset == 5) 45.0 else 0.0,
            )
        }

        assertEquals(listOf("sun_protection", "raincoat"), Insights.packingTips(forecast, 0))

        val clearForecast = forecast.map {
            it.copy(uvIndex = 0.0, rainChancePercent = 0.0, feelsLike = 24.0, aqiValue = 30)
        }
        assertTrue(Insights.packingTips(clearForecast, 0).isEmpty())
    }

    @Test
    fun pollenReadingsKeepSpeciesConcentrationsAndMissingValuesDistinct() {
        val readings = PollenReadings(grass = 2.5, birch = 0.0)

        assertEquals(2.5, readings.value(PollenSpecies.GRASS) ?: -1.0, 0.0)
        assertEquals(0.0, readings.value(PollenSpecies.BIRCH) ?: -1.0, 0.0)
        assertTrue(readings.value(PollenSpecies.RAGWEED) == null)
        assertTrue(readings.hasAnyValue())
        assertFalse(PollenReadings().hasAnyValue())
    }

    @Test
    fun roadOutlookDoesNotHideRainProbabilityConflict() {
        val time = LocalDateTime.of(2025, 6, 10, 9, 0)
        val outlook = Insights.roadOutlook(
            listOf(weatherRow(time).copy(condition = "Showers")),
            nowIndex = 0,
        )
        val clearOutlook = Insights.roadOutlook(listOf(weatherRow(time)), nowIndex = 0)

        assertTrue(outlook?.rainIndicated == true)
        assertEquals(0.0, outlook?.rainChancePercent ?: -1.0, 0.0)
        assertTrue(outlook?.hasConflictingRainChance == true)
        assertFalse(clearOutlook?.rainIndicated == true)
        assertFalse(clearOutlook?.hasConflictingRainChance == true)
    }

    @Test
    fun runningWindowDoesNotInventABestTimeWhenForecastConditionsAreTied() {
        val start = LocalDateTime.of(2025, 6, 10, 5, 0)
        val forecast = (0 until 6).map { offset -> weatherRow(start.plusHours(offset.toLong())) }

        assertEquals(RunningWindow.NoClearBestWindow, Insights.bestRunningWindow(forecast, 0))
    }

    @Test
    fun runningWindowRejectsUnsafeForecastAndKeepsMissingDataDistinct() {
        val start = LocalDateTime.of(2025, 6, 10, 5, 0)
        val wetForecast = (0 until 6).map { offset ->
            weatherRow(start.plusHours(offset.toLong()), rainChancePercent = 80.0)
        }
        val incompleteForecast = listOf(weatherRow(start.plusHours(5)))

        assertEquals(RunningWindow.NotRecommended, Insights.bestRunningWindow(wetForecast, 0))
        assertEquals(null, Insights.bestRunningWindow(incompleteForecast, 0))
    }

    @Test
    fun runningWindowReturnsUniqueBestWhenConditionsActuallyDiffer() {
        val start = LocalDateTime.of(2025, 6, 10, 5, 0)
        val forecast = (0 until 4).map { offset ->
            val temperature = when (offset) {
                0 -> 27.0
                1 -> 29.0
                else -> 30.0
            }
            weatherRow(start.plusHours(offset.toLong())).copy(temperature = temperature)
        }

        val window = Insights.bestRunningWindow(forecast, 0)

        assertTrue(window is RunningWindow.Recommended)
        assertEquals(start, (window as RunningWindow.Recommended).start)
    }

    @Test
    fun schoolCommuteOutlooksChooseGearIndependentlyForDropOffAndPickup() {
        val date = java.time.LocalDate.of(2025, 6, 10)
        val forecast = listOf(
            weatherRow(date.atTime(7, 0), rainChancePercent = 10.0),
            weatherRow(date.atTime(8, 0)).copy(condition = "Showers"),
            weatherRow(date.atTime(16, 0), rainChancePercent = 70.0),
            weatherRow(date.atTime(17, 0), rainChancePercent = 45.0),
        )

        val dropOff = Insights.commuteOutlook(forecast, date, 7, 9)
        val pickup = Insights.commuteOutlook(forecast, date, 16, 18)

        assertEquals(RainGearGuidance.RAINCOAT, dropOff?.rainGearGuidance)
        assertEquals(RainGearGuidance.UMBRELLA, pickup?.rainGearGuidance)
        assertEquals(date.atTime(7, 0), dropOff?.peakTime)
        assertEquals(date.atTime(16, 0), pickup?.peakTime)
        assertEquals(70.0, pickup?.peak?.rainChancePercent ?: -1.0, 0.0)
    }

    @Test
    fun commuteDoesNotClaimRainGearIsUnneededWhenForecastSignalsRain() {
        val date = java.time.LocalDate.of(2025, 6, 10)
        val forecast = listOf(
            weatherRow(date.atTime(16, 0)).copy(condition = "Light rain"),
        )

        val outlook = Insights.commuteOutlook(forecast, date, 16, 18)
        assertEquals(RainGearGuidance.RAINCOAT, outlook?.rainGearGuidance)
        assertTrue(outlook?.hasRainProbabilityConflict == true)
    }

    @Test
    fun commuteShowsWindowInsteadOfArbitraryPeakTimeWhenRainChancesAreTied() {
        val date = java.time.LocalDate.of(2025, 6, 10)
        val forecast = listOf(
            weatherRow(date.atTime(7, 0), rainChancePercent = 20.0),
            weatherRow(date.atTime(8, 0), rainChancePercent = 20.0),
        )

        val outlook = Insights.commuteOutlook(forecast, date, 7, 9)

        assertEquals(null, outlook?.peakTime)
        assertEquals(RainGearGuidance.NONE, outlook?.rainGearGuidance)
    }

    @Test
    fun commuteTemplatesUseTheNewActionFormatInTamilAndHindi() {
        val tamil = AppStrings.forCode(AppStrings.TAMIL)
        val hindi = AppStrings.forCode(AppStrings.HINDI)

        assertTrue(tamil.format("morning_rain", "Drop-off · %1\$s — %2\$s", "tomorrow", "carry an umbrella", 70).contains("carry an umbrella"))
        assertTrue(hindi.format("pickup_rain", "Pickup · %1\$s — %2\$s", "tomorrow", "carry a raincoat", 70).contains("carry a raincoat"))
    }

    @Test
    fun familyAdviceUsesSchoolCommuteWindowAndKeepsProbabilityInEvidence() {
        val start = LocalDateTime.of(2025, 6, 10, 0, 0)
        val forecast = (0 until 48).map { offset ->
            val time = start.plusHours(offset.toLong())
            val rain = when {
                time.hour == 2 && offset >= 24 -> 99.0
                time.hour == 16 && offset >= 24 -> 65.0
                else -> 0.0
            }
            weatherRow(time, rainChancePercent = rain)
        }

        val advice = WeatherActionSuggestionPolicy.recommend(
            forecast,
            nowIndex = 0,
            personas = listOf(Persona(id = "parent_family", label = "Family")),
        ).single()

        assertTrue(advice.message.contains("4:00 PM"))
        assertTrue(advice.message.contains("school pickup"))
        assertFalse(advice.message.contains("65%"))
        assertTrue(advice.evidence.contains("65%"))
        assertFalse(advice.message.contains("2:00 AM"))
    }

    @Test
    fun familyAdviceDoesNotTreatOvernightRainAsSchoolCommuteRain() {
        val start = LocalDateTime.of(2025, 6, 10, 0, 0)
        val forecast = (0 until 48).map { offset ->
            val time = start.plusHours(offset.toLong())
            weatherRow(
                time,
                rainChancePercent = if (offset >= 24 && time.hour == 2) 95.0 else 0.0,
            )
        }

        val advice = WeatherActionSuggestionPolicy.recommend(
            forecast,
            nowIndex = 0,
            personas = listOf(Persona(id = "parent_family", label = "Family")),
        )

        assertTrue(advice.isEmpty())
    }

    @Test
    fun uvAdviceKeepsIndexValueInEvidenceInsteadOfActionCopy() {
        val start = LocalDateTime.of(2025, 6, 10, 6, 0)
        val forecast = (0 until 13).map { offset ->
            weatherRow(start.plusHours(offset.toLong()), uvIndex = if (offset == 6) 8.0 else 0.0)
        }

        val advice = WeatherActionSuggestionPolicy.recommend(
            forecast,
            nowIndex = 0,
            personas = listOf(Persona(id = "health_conscious", label = "Health")),
        ).single()

        assertTrue(advice.message.contains("12:00 PM"))
        assertFalse(advice.message.contains("UV index reaches"))
        assertTrue(advice.evidence.contains("UV index reaches 8"))
    }

    @Test
    fun marineAdviceUsesForecastTimeAndKeepsWaveMeasurementInEvidence() {
        val start = LocalDateTime.of(2025, 6, 10, 6, 0)
        val forecast = (0 until 13).map { offset ->
            weatherRow(
                start.plusHours(offset.toLong()),
                waveHeightM = if (offset == 6) 2.4 else 0.5,
            )
        }

        val advice = WeatherActionSuggestionPolicy.recommend(
            forecast,
            nowIndex = 0,
            personas = listOf(Persona(id = "beach_surfer", label = "Beach")),
        ).single()

        assertTrue(advice.message.contains("12:00 PM"))
        assertTrue(advice.message.contains("Do not swim or surf"))
        assertFalse(advice.message.contains("2.4 m"))
        assertTrue(advice.evidence.contains("2.4 m"))
    }

    private fun weatherRow(
        time: LocalDateTime,
        uvIndex: Double = 0.0,
        rainChancePercent: Double = 0.0,
        feelsLike: Double = 25.0,
        aqiValue: Int = 30,
        waveHeightM: Double? = null,
    ) = WeatherRow(
        id = time.hour,
        locationId = "test",
        timestamp = time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")),
        date = time.toLocalDate().toString(),
        hour = time.hour,
        temperature = 25.0,
        feelsLike = feelsLike,
        humidity = 50.0,
        windSpeed = 5.0,
        windDirection = "N",
        condition = "Clear",
        pressureHpa = 1013.0,
        dewPoint = 15.0,
        uvIndex = uvIndex,
        rainChancePercent = rainChancePercent,
        rainfallMm = 0.0,
        visibilityKm = 10.0,
        fogFlag = 0,
        aqiValue = aqiValue,
        aqiCategory = "Good",
        dominantPollutant = "",
        pm25 = 0.0,
        pm10 = 0.0,
        no2 = 0.0,
        o3 = 0.0,
        co = 0.0,
        so2 = 0.0,
        pollenGrass = "low",
        pollenTree = "low",
        pollenRagweed = "low",
        waveHeightM = waveHeightM,
        wavePeriodSec = null,
        waveDirection = null,
        waterTemperature = null,
        tideHeightM = null,
        soilMoisturePercent = 30.0,
        heatAlert = 0,
        comfortIndex = "Good",
    )
}
