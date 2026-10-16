package com.example.personaweather.core.engine

import com.example.personaweather.core.model.Advisory
import com.example.personaweather.core.model.Persona
import com.example.personaweather.core.model.RouteConfig
import com.example.personaweather.core.model.Severity
import com.example.personaweather.core.model.Waypoint
import com.example.personaweather.core.model.WeatherRow
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

enum class RouteVerdict { CLEAR, CAUTION, ALERT }

data class WaypointWeather(
    val waypoint: Waypoint,
    val eta: LocalDateTime,
    val row: WeatherRow,
    val advisories: List<Advisory>,
)

data class RouteResult(
    val stops: List<WaypointWeather>,
    val score: Int,
    val verdict: RouteVerdict,
)

data class DepartureSuggestion(val departure: LocalDateTime, val route: RouteResult)

object RouteWeather {
    private val basePersonaIds = listOf("commuter", "health_conscious")
    private val routeFields = setOf("rain_chance_percent", "aqi_value", "visibility_km", "fog_flag", "heat_alert")

    fun roundToNearestHour(time: LocalDateTime): LocalDateTime {
        val base = time.truncatedTo(ChronoUnit.HOURS)
        return if (time.minute >= 30) base.plusHours(1) else base
    }

    fun evaluate(
        departure: LocalDateTime,
        seriesByLocation: Map<String, List<WeatherRow>>,
        allPersonas: List<Persona>,
        coastalByLocation: Map<String, Boolean>,
        route: List<Waypoint> = RouteConfig.ROUTE,
        personaIds: List<String> = emptyList(),
    ): RouteResult {
        val personasById = allPersonas.associateBy { it.id }
        val activePersonas = (basePersonaIds + personaIds).distinct().mapNotNull(personasById::get)
        val stops = route.map { waypoint ->
            val series = requireNotNull(seriesByLocation[waypoint.locationId]) {
                "Missing weather series for ${waypoint.locationId}"
            }
            require(series.isNotEmpty()) { "Empty weather series for ${waypoint.locationId}" }

            val eta = roundToNearestHour(departure.plusMinutes(waypoint.minutesFromStart.toLong()))
                .coerceIn(series.first().time, series.last().time)
            val insertionIndex = series.binarySearchBy(eta) { it.time }
            val index = if (insertionIndex >= 0) insertionIndex else (-insertionIndex - 1).coerceIn(0, series.lastIndex)
            WaypointWeather(
                waypoint = waypoint,
                eta = eta,
                row = series[index],
                advisories = AdvisoryEngine.evaluate(
                    series = series,
                    nowIndex = index,
                    personas = activePersonas,
                    isCoastal = coastalByLocation[waypoint.locationId] == true,
                    fieldFilter = routeFields,
                ),
            )
        }
        val score = stops.fold(0) { total, stop ->
            total + stop.advisories.fold(0) { stopTotal, advisory ->
                stopTotal + if (advisory.severity == Severity.ALERT) 3 else 1
            }
        }
        val verdict = when {
            stops.any { stop -> stop.advisories.any { it.severity == Severity.ALERT } } -> RouteVerdict.ALERT
            stops.any { stop -> stop.advisories.any { it.severity == Severity.CAUTION } } -> RouteVerdict.CAUTION
            else -> RouteVerdict.CLEAR
        }
        return RouteResult(stops, score, verdict)
    }

    fun bestDeparture(
        departure: LocalDateTime,
        current: RouteResult,
        seriesByLocation: Map<String, List<WeatherRow>>,
        allPersonas: List<Persona>,
        coastalByLocation: Map<String, Boolean>,
        route: List<Waypoint> = RouteConfig.ROUTE,
        personaIds: List<String> = emptyList(),
    ): DepartureSuggestion? {
        val candidates = (-60..180 step 15).map { offset ->
            val candidateTime = departure.plusMinutes(offset.toLong())
            candidateTime to evaluate(candidateTime, seriesByLocation, allPersonas, coastalByLocation, route, personaIds)
        }
        val best = candidates.minWithOrNull(
            compareBy<Pair<LocalDateTime, RouteResult>> { it.second.score }.thenBy { it.first },
        ) ?: return null
        return best.takeIf { it.second.score < current.score }
            ?.let { DepartureSuggestion(it.first, it.second) }
    }
}