package com.example.personaweather.core.engine

import com.example.personaweather.core.model.Advisory
import com.example.personaweather.core.model.Persona
import com.example.personaweather.core.model.Rule
import com.example.personaweather.core.model.Severity
import com.example.personaweather.core.model.WeatherRow

object AdvisoryEngine {
    private data class Candidate(
        val advisory: Advisory,
        val personaIndex: Int,
        val ruleIndex: Int,
    )

    fun evaluate(
        series: List<WeatherRow>,
        nowIndex: Int,
        personas: List<Persona>,
        isCoastal: Boolean,
        fieldFilter: Set<String>? = null,
    ): List<Advisory> {
        val selected = linkedMapOf<Pair<String, String>, Candidate>()
        personas.forEachIndexed { personaIndex, persona ->
            if (persona.requiresCoastal && !isCoastal) return@forEachIndexed
            persona.rules.forEachIndexed { ruleIndex, rule ->
                if (fieldFilter != null && rule.field !in fieldFilter) return@forEachIndexed
                val observed = observedValue(rule, series, nowIndex) ?: return@forEachIndexed
                if (!trips(rule, observed)) return@forEachIndexed
                val severity = rule.toSeverity() ?: return@forEachIndexed
                val candidate = Candidate(
                    Advisory(
                        ruleId = rule.id,
                        personaId = persona.id,
                        field = rule.field,
                        severity = severity,
                        message = rule.message,
                        messageTa = rule.messageTa,
                        messageHi = rule.messageHi,
                        observed = observed,
                        isLookahead = rule.lookaheadHours != null,
                    ),
                    personaIndex,
                    ruleIndex,
                )
                val key = persona.id to rule.field
                val previous = selected[key]
                if (previous == null || severity.rank() > previous.advisory.severity.rank() ||
                    severity == previous.advisory.severity && ruleIndex > previous.ruleIndex
                ) {
                    selected[key] = candidate
                }
            }
        }

        return selected.values.sortedWith(
            compareByDescending<Candidate> { it.advisory.severity == Severity.ALERT }
                .thenBy { it.personaIndex }
                .thenBy { it.ruleIndex },
        ).map { it.advisory }
    }

    fun observedValue(rule: Rule, series: List<WeatherRow>, index: Int): Any? {
        if (rule.lookaheadHours == null) return series.getOrNull(index)?.fieldValue(rule.field)
        if (index !in series.indices || rule.lookaheadHours < 0) return null
        val end = minOf(series.size, index + rule.lookaheadHours + 1)
        val values = series.subList(index, end).mapNotNull {
            (it.fieldValue(rule.field) as? Number)?.toDouble()
        }
        if (values.isEmpty()) return null
        return when (rule.aggregate?.lowercase()) {
            "max" -> values.maxOrNull()
            "min" -> values.minOrNull()
            "sum" -> values.sum()
            "avg" -> values.average()
            else -> null
        }
    }

    fun trips(rule: Rule, observed: Any?): Boolean {
        if (observed == null) return false
        return when (val expected = rule.value) {
            is Number -> {
                val actual = (observed as? Number)?.toDouble() ?: return false
                val target = expected.toDouble()
                when (rule.operator) {
                    ">=" -> actual >= target
                    "<=" -> actual <= target
                    ">" -> actual > target
                    "<" -> actual < target
                    "==" -> actual == target
                    "!=" -> actual != target
                    else -> false
                }
            }
            is String -> when (rule.operator) {
                "==" -> observed.toString().equals(expected, ignoreCase = true)
                "!=" -> !observed.toString().equals(expected, ignoreCase = true)
                else -> false
            }
            else -> false
        }
    }

    private fun Rule.toSeverity(): Severity? = when (severity.lowercase()) {
        "alert" -> Severity.ALERT
        "caution" -> Severity.CAUTION
        else -> null
    }

    private fun Severity.rank(): Int = when (this) {
        Severity.CAUTION -> 0
        Severity.ALERT -> 1
    }
}