package com.example.personaweather.core.engine

import com.example.personaweather.core.model.Advisory
import com.example.personaweather.core.model.Persona
import com.example.personaweather.core.model.Rule
import com.example.personaweather.core.model.Severity
import com.example.personaweather.core.model.WeatherRow

object AdvisoryEngine {
    fun evaluate(
        series: List<WeatherRow>,
        nowIndex: Int,
        personas: List<Persona>,
        isCoastal: Boolean,
        fieldFilter: Set<String>? = null
    ): List<Advisory> {
        data class Candidate(val advisory: Advisory, val personaIndex: Int, val ruleIndex: Int)
        val selected = linkedMapOf<Pair<String, String>, Candidate>()

        personas.forEachIndexed personaLoop@{ personaIndex, persona ->
            if (persona.requiresCoastal && !isCoastal) return@personaLoop
            persona.rules.forEachIndexed ruleLoop@{ ruleIndex, rule ->
                if (fieldFilter != null && rule.field !in fieldFilter) return@ruleLoop
                val observed = observedValue(rule, series, nowIndex) ?: return@ruleLoop
                if (!trips(rule, observed)) return@ruleLoop
                val candidate = Candidate(
                    Advisory(rule.id, persona.id, rule.field, rule.severity, rule.message,
                        rule.messageTa, observed, rule.lookaheadHours != null),
                    personaIndex,
                    ruleIndex
                )
                val key = persona.id to rule.field
                val previous = selected[key]
                if (previous == null || rule.severity.ordinal > previous.advisory.severity.ordinal ||
                    rule.severity == previous.advisory.severity && ruleIndex > previous.ruleIndex
                ) selected[key] = candidate
            }
        }

        return selected.values.sortedWith(
            compareByDescending<Candidate> { it.advisory.severity == Severity.ALERT }
                .thenBy { it.personaIndex }
                .thenBy { it.ruleIndex }
        ).map { it.advisory }
    }

    fun observedValue(rule: Rule, series: List<WeatherRow>, index: Int): Any? {
        if (rule.lookaheadHours == null) return series.getOrNull(index)?.fieldValue(rule.field)
        if (index !in series.indices) return null
        val end = minOf(series.size, index + rule.lookaheadHours + 1)
        val numbers = series.subList(index, end).mapNotNull {
            (it.fieldValue(rule.field) as? Number)?.toDouble()
        }
        if (numbers.isEmpty()) return null
        return when (rule.aggregate) {
            "max" -> numbers.maxOrNull()
            "min" -> numbers.minOrNull()
            "sum" -> numbers.sum()
            "avg" -> numbers.average()
            else -> null
        }
    }

    fun trips(rule: Rule, observed: Any?): Boolean {
        if (observed == null) return false
        return when (val expected = rule.value) {
            is Number -> {
                val actual = (observed as? Number)?.toDouble() ?: return false
                val target = expected.toDouble()
                when (rule.op) {
                    ">=" -> actual >= target
                    "<=" -> actual <= target
                    ">" -> actual > target
                    "<" -> actual < target
                    "==" -> actual == target
                    "!=" -> actual != target
                    else -> false
                }
            }
            is String -> when (rule.op) {
                "==" -> observed.toString().equals(expected, ignoreCase = true)
                "!=" -> !observed.toString().equals(expected, ignoreCase = true)
                else -> false
            }
            else -> false
        }
    }
}