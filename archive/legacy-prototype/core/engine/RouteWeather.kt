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
data class WaypointWeather(val waypoint: Waypoint, val eta: LocalDateTime, val row: WeatherRow, val advisories: List<Advisory>)
data class RouteResult(val stops: List<WaypointWeather>, val score: Int, val verdict: RouteVerdict)
data class DepartureSuggestion(val departure: LocalDateTime, val route: RouteResult)

object RouteWeather {
    val ROUTE = RouteConfig.ROUTE

    fun roundToNearestHour(time: LocalDateTime): LocalDateTime {
        val base = time.truncatedTo(ChronoUnit.HOURS)
        return if (time.minute >= 30) base.plusHours(1) else base
    }

    fun evaluate(
        departure: LocalDateTime,
        route: List<Waypoint>,
        seriesByLocation: Map<String, List<WeatherRow>>,
        allPersonas: List<Persona>,
        coastalByLocation: Map<String, Boolean>,
        personaIds: List<String> = Tuning.ROUTE_BASE_PERSONAS
    ): RouteResult {
        val personasById = allPersonas.associateBy { it.id }
        val active = (Tuning.ROUTE_BASE_PERSONAS + personaIds).distinct().mapNotNull(personasById::get)
        val stops = route.map { waypoint ->
            val series = requireNotNull(seriesByLocation[waypoint.locationId]) {
                "Missing weather series for ${waypoint.locationId}"
            }
            require(series.isNotEmpty()) { "Empty weather series for ${waypoint.locationId}" }
            val roundedEta = roundToNearestHour(departure.plusMinutes(waypoint.minutesFromStart.toLong()))
            val eta = roundedEta.coerceIn(series.first().time, series.last().time)
            val index = series.binarySearchBy(eta) { it.time }.let { result ->
                if (result >= 0) result else (-result - 1).coerceIn(0, series.lastIndex)
            }
            val row = series[index]
            val advisories = AdvisoryEngine.evaluate(
                series, index, active, coastalByLocation[waypoint.locationId] == true, Tuning.ROUTE_FIELDS
            )
            WaypointWeather(waypoint, eta, row, advisories)
        }
        val score = stops.sumOf { stop -> stop.advisories.sumOf { if (it.severity == Severity.ALERT) 3 else 1 } }
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
        route: List<Waypoint>,
        seriesByLocation: Map<String, List<WeatherRow>>,
        allPersonas: List<Persona>,
        coastalByLocation: Map<String, Boolean>,
        personaIds: List<String> = Tuning.ROUTE_BASE_PERSONAS
    ): DepartureSuggestion? {
        val candidates = generateSequence(Tuning.ROUTE_SEARCH_MINUTES.first) { previous ->
            (previous + Tuning.ROUTE_SEARCH_STEP_MINUTES).takeIf { it <= Tuning.ROUTE_SEARCH_MINUTES.last }
        }.map { offset ->
            val time = departure.plusMinutes(offset.toLong())
            time to evaluate(time, route, seriesByLocation, allPersonas, coastalByLocation, personaIds)
        }
        val best = candidates.minWithOrNull(compareBy<Pair<LocalDateTime, RouteResult>> { it.second.score }.thenBy { it.first })
        return best?.takeIf { it.second.score < current.score }?.let { DepartureSuggestion(it.first, it.second) }
    }
}