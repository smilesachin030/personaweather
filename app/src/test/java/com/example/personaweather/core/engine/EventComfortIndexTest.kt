package com.example.personaweather.core.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class EventComfortIndexTest {
    @Test
    fun clearMildConditionsScoreAsComfortable() {
        val assessment = EventComfortIndex.assess(
            rainChancePercent = 0.0,
            aqi = 30,
            temperatureC = 22.0,
            feelsLikeC = 22.0,
        )

        assertEquals(100, assessment.score)
        assertEquals("Good", assessment.label)
        assertEquals(0, assessment.rainPenalty)
        assertEquals(0, assessment.aqiPenalty)
        assertEquals(0, assessment.temperaturePenalty)
    }

    @Test
    fun rainPoorAirAndFrostLowerTheComfortScore() {
        val assessment = EventComfortIndex.assess(
            rainChancePercent = 100.0,
            aqi = 400,
            temperatureC = -1.0,
            feelsLikeC = -1.0,
        )

        assertEquals(0, assessment.score)
        assertEquals("Poor", assessment.label)
        assertEquals(35, assessment.rainPenalty)
        assertEquals(40, assessment.aqiPenalty)
        assertEquals(25, assessment.temperaturePenalty)
    }

    @Test
    fun heatAndRainAffectTheDisplayedAssessment() {
        val dry = EventComfortIndex.assess(
            rainChancePercent = 0.0,
            aqi = 50,
            temperatureC = 30.0,
            feelsLikeC = 30.0,
        )
        val rainy = EventComfortIndex.assess(
            rainChancePercent = 100.0,
            aqi = 50,
            temperatureC = 30.0,
            feelsLikeC = 30.0,
        )

        assertEquals(93, dry.score)
        assertEquals(58, rainy.score)
        assertEquals("Poor", rainy.label)
        assertEquals(7, dry.temperaturePenalty)
        assertEquals(35, rainy.rainPenalty)
    }
}
