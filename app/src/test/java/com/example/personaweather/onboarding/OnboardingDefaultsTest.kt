package com.example.personaweather.onboarding

import com.example.personaweather.core.model.CardSpec
import com.example.personaweather.core.model.Persona
import com.example.personaweather.core.model.RulesCatalog
import org.junit.Assert.assertEquals
import org.junit.Test

class OnboardingDefaultsTest {
    @Test
    fun merges_default_cards_for_selected_personas_without_duplicates() {
        val catalog = RulesCatalog(
            cards = mapOf(
                "aqi_card" to CardSpec("aqi_card", "Air Quality", listOf("aqi_value")),
                "uv_card" to CardSpec("uv_card", "UV", listOf("uv_index")),
                "heat_card" to CardSpec("heat_card", "Heat", listOf("heat_alert")),
            ),
            personas = mapOf(
                "health_conscious" to Persona("health_conscious", "Health-conscious", defaultCards = listOf("aqi_card", "uv_card")),
                "outdoor_fitness" to Persona("outdoor_fitness", "Outdoor Fitness", defaultCards = listOf("uv_card", "heat_card")),
            )
        )

        val result = OnboardingDefaults.defaultCardOrder(listOf("health_conscious", "outdoor_fitness"), catalog)

        assertEquals(listOf("aqi_card", "uv_card", "heat_card"), result)
    }
}
