package com.example.personaweather.core.engine

import com.example.personaweather.core.model.Advisory
import com.example.personaweather.core.model.Persona
import com.example.personaweather.core.model.Severity
import com.example.personaweather.core.model.WeatherRow
import java.time.LocalDateTime

data class CalendarEventWeatherEvaluation(
    val weatherRows: List<WeatherRow>,
    val advisories: List<Advisory>,
)

object CalendarEventWeatherEvaluator {
    private val indoorFields = setOf("aqi_value")

    fun evaluate(
        series: List<WeatherRow>,
        eventStart: LocalDateTime,
        eventEnd: LocalDateTime,
        personas: List<Persona>,
        isCoastal: Boolean,
        isOutdoor: Boolean,
        isOrganizer: Boolean,
    ): CalendarEventWeatherEvaluation? {
        if (series.isEmpty() || !eventEnd.isAfter(eventStart)) return null
        val eventPlannerPersona = personas.firstOrNull { it.id == EVENT_PLANNER_PERSONA_ID }
        val advicePersonas = when {
            eventPlannerPersona == null -> emptyList()
            isOrganizer -> listOf(eventPlannerPersona)
            else -> personas.filterNot { it.id == EVENT_PLANNER_PERSONA_ID }
        }
        val firstTime = series.first().time
        val coveredUntil = series.last().time.plusHours(1)
        if (eventStart.isBefore(firstTime) || eventEnd.isAfter(coveredUntil)) return null

        val eventRows = series.withIndex()
            .filter { (_, row) -> row.time.isBefore(eventEnd) && row.time.plusHours(1).isAfter(eventStart) }
        if (eventRows.isEmpty()) return null

        val fieldFilter = if (isOutdoor) null else indoorFields
        val advisoriesByPersonaField = linkedMapOf<Pair<String, String>, Advisory>()
        val finalEventIndex = eventRows.last().index
        eventRows.forEach { (index, _) ->
            AdvisoryEngine.evaluate(
                series = series.subList(index, finalEventIndex + 1),
                nowIndex = 0,
                personas = advicePersonas,
                isCoastal = isCoastal,
                fieldFilter = fieldFilter,
            )
                .forEach { advisory ->
                    val key = advisory.personaId to advisory.field
                    val previous = advisoriesByPersonaField[key]
                    if (previous == null || advisory.severity.rank() > previous.severity.rank()) {
                        advisoriesByPersonaField[key] = advisory
                    } else if (previous.severity == advisory.severity && isWorseObservation(advisory, previous)) {
                        advisoriesByPersonaField[key] = advisory
                    }
                }
        }

        return CalendarEventWeatherEvaluation(
            weatherRows = eventRows.map { it.value },
            advisories = advisoriesByPersonaField.values.sortedByDescending { it.severity == Severity.ALERT },
        )
    }

    private fun Severity.rank(): Int = when (this) {
        Severity.CAUTION -> 0
        Severity.ALERT -> 1
    }

    private fun isWorseObservation(candidate: Advisory, previous: Advisory): Boolean {
        val candidateValue = (candidate.observed as? Number)?.toDouble() ?: return false
        val previousValue = (previous.observed as? Number)?.toDouble() ?: return false
        return if (candidate.field == "visibility_km") candidateValue < previousValue
        else candidateValue > previousValue
    }

    private const val EVENT_PLANNER_PERSONA_ID = "event_planner"
}