package com.example.personaweather.onboarding

import com.example.personaweather.core.model.RulesCatalog

object OnboardingDefaults {
    fun defaultCardOrder(personaIds: List<String>, catalog: RulesCatalog): List<String> {
        val ordered = mutableListOf<String>()
        val seen = linkedSetOf<String>()

        personaIds.forEach { personaId ->
            val persona = catalog.personas[personaId] ?: return@forEach
            persona.defaultCards.forEach { cardId ->
                if (cardId in seen) return@forEach
                if (cardId in catalog.cards) {
                    seen += cardId
                    ordered += cardId
                }
            }
        }

        return ordered
    }

    fun defaultPersonas(catalog: RulesCatalog): List<String> = catalog.personas.keys.toList().take(2)
}
