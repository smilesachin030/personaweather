package com.example.personaweather.notify

import com.example.personaweather.core.engine.AdvisoryEngine
import com.example.personaweather.core.model.Advisory
import com.example.personaweather.core.model.Persona
import com.example.personaweather.core.model.RulesCatalog
import com.example.personaweather.core.model.WeatherRow

object ClothesRainAlarm {
    fun persona(catalog: RulesCatalog): Persona? {
        val ownerAndRule = catalog.personas.values
            .asSequence()
            .mapNotNull { owner ->
                owner.rules.firstOrNull { it.id == NotificationHelper.CLOTHES_ALERT_RULE }
                    ?.let { owner to it }
            }
            .firstOrNull() ?: return null
        val (owner, rule) = ownerAndRule
        return owner.copy(rules = listOf(rule))
    }

    fun evaluate(
        series: List<WeatherRow>,
        nowIndex: Int,
        catalog: RulesCatalog,
        isCoastal: Boolean,
    ): List<Advisory> {
        val alarmPersona = persona(catalog) ?: return emptyList()
        return AdvisoryEngine.evaluate(
            series = series,
            nowIndex = nowIndex,
            personas = listOf(alarmPersona),
            isCoastal = isCoastal,
        )
    }
}
