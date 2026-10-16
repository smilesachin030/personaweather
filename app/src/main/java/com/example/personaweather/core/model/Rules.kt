package com.example.personaweather.core.model

data class CardSpec(
    val id: String,
    val label: String,
    val fields: List<String>,
    val requiresCoastal: Boolean = false,
    val derived: String? = null,
)

data class Rule(
    val id: String,
    val field: String,
    val operator: String,
    val value: Any?,
    val severity: String,
    val message: String,
    val lookaheadHours: Int? = null,
    val aggregate: String? = null,
    val messageTa: String? = null,
    val messageHi: String? = null,
)

data class Persona(
    val id: String,
    val label: String,
    val requiresCoastal: Boolean = false,
    val defaultCards: List<String> = emptyList(),
    val rules: List<Rule> = emptyList(),
)

data class RulesCatalog(
    val cards: Map<String, CardSpec>,
    val personas: Map<String, Persona>,
)

enum class Severity { CAUTION, ALERT }

data class Advisory(
    val ruleId: String,
    val personaId: String,
    val field: String,
    val severity: Severity,
    val message: String,
    val messageTa: String? = null,
    val messageHi: String? = null,
    val observed: Any?,
    val isLookahead: Boolean,
)

data class DailySummary(
    val date: String,
    val minTemp: Double,
    val maxTemp: Double,
    val maxRain: Double,
    val avgAqi: Double,
    val maxAqi: Int,
    val totalRainMm: Double,
    val dominantCondition: String,
    val eventScore: Int,
)
