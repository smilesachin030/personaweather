package com.example.personaweather.ui.trip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personaweather.core.engine.DepartureSuggestion
import com.example.personaweather.core.engine.RouteResult
import com.example.personaweather.core.engine.RouteWeather
import com.example.personaweather.core.model.Location
import com.example.personaweather.core.model.Persona
import com.example.personaweather.core.model.RouteConfig
import com.example.personaweather.core.model.RouteDefinition
import com.example.personaweather.core.model.RulesCatalog
import com.example.personaweather.core.model.WeatherRow
import com.example.personaweather.data.UserPrefs
import com.example.personaweather.data.WeatherDataSource
import com.example.personaweather.data.OpenMeteoGeocodingClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.ZoneId

data class TripPlannerUiState(
    val isLoading: Boolean = true,
    val isRecalculating: Boolean = false,
    val error: String? = null,
    val departure: LocalDateTime? = null,
    val referenceDeparture: LocalDateTime? = null,
    val result: RouteResult? = null,
    val suggestion: DepartureSuggestion? = null,
    val locations: List<Location> = emptyList(),
    val route: RouteDefinition = RouteConfig.ROUTES.first(),
    val availableRoutes: List<RouteDefinition> = RouteConfig.ROUTES,
    val offlineStatus: String? = null,
    val offlineCoverageStart: LocalDateTime? = null,
    val offlineCoverageEnd: LocalDateTime? = null,
)

class TripPlannerViewModel(
    private val prefs: UserPrefs,
    private val catalog: RulesCatalog,
    private val dataSource: WeatherDataSource,
    private val geocodingClient: OpenMeteoGeocodingClient = OpenMeteoGeocodingClient(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(TripPlannerUiState())
    val uiState: StateFlow<TripPlannerUiState> = _uiState.asStateFlow()

    private var seriesByLocation: Map<String, List<WeatherRow>> = emptyMap()
    private var coastalByLocation: Map<String, Boolean> = emptyMap()
    private var routePersonas: List<Persona> = emptyList()
    private var activePersonaIds: List<String> = emptyList()
    private var departureRange: ClosedRange<LocalDateTime>? = null
    private var calculationRequestId = 0
    private var activeRoute = RouteConfig.ROUTES.first()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = TripPlannerUiState(isLoading = true, route = activeRoute)
            try {
                val catalogLocations = (dataSource.locations() + prefs.rememberedLocations).distinctBy { it.id }
                val routeLocations = activeRoute.places.map { place ->
                    if (place.geocodingQuery == null) {
                        catalogLocations.firstOrNull { it.id == place.locationId }
                            ?: throw IllegalStateException("Route location ${place.label} is not available.")
                    } else {
                        val remembered = prefs.rememberedLocation(place.locationId)
                        remembered ?: run {
                            val candidates = geocodingClient.search(place.geocodingQuery, "en")
                            val resolved = candidates.firstOrNull { candidate ->
                                candidate.name.equals(place.geocodingQuery, ignoreCase = true) &&
                                    candidate.adminArea.equals(place.expectedAdminArea, ignoreCase = true)
                            } ?: candidates.firstOrNull { candidate ->
                                candidate.adminArea.equals(place.expectedAdminArea, ignoreCase = true)
                            } ?: candidates.firstOrNull()
                                ?: throw IllegalStateException("Could not find ${place.label} for this route.")
                            resolved.copy(id = place.locationId, name = place.label).also(prefs::rememberLocation)
                        }
                    }
                }
                val locationsById = (catalogLocations + routeLocations).associateBy { it.id }
                val loadedSeries = activeRoute.waypoints.associate { waypoint ->
                    val location = locationsById[waypoint.locationId]
                        ?: throw IllegalStateException("Route location ${waypoint.label} is not available.")
                    waypoint.locationId to dataSource.series(location)
                }
                loadedSeries.forEach { (locationId, rows) ->
                    check(rows.isNotEmpty()) { "No forecast data for ${locationsById[locationId]?.name ?: locationId}." }
                }
                val routeOffsets = activeRoute.waypoints.associateBy { it.locationId }
                val earliestDeparture = loadedSeries.map { (locationId, rows) ->
                    rows.first().time.minusMinutes(routeOffsets.getValue(locationId).minutesFromStart.toLong())
                }.maxOrNull() ?: throw IllegalStateException("No forecast coverage is available for this route.")
                val latestDeparture = loadedSeries.map { (locationId, rows) ->
                    rows.last().time.minusMinutes(routeOffsets.getValue(locationId).minutesFromStart.toLong())
                }.minOrNull() ?: throw IllegalStateException("No forecast coverage is available for this route.")
                check(!earliestDeparture.isAfter(latestDeparture)) {
                    "Provider forecasts do not overlap across every stop on this route."
                }

                seriesByLocation = loadedSeries
                departureRange = earliestDeparture..latestDeparture
                coastalByLocation = locationsById.mapValues { it.value.isCoastal }
                activePersonaIds = prefs.personas
                routePersonas = catalog.personas.values.toList()
                val departure = LocalDateTime.now(ZoneId.of("Asia/Kolkata"))
                    .withMinute(0).withSecond(0).withNano(0)
                check(departure in departureRange!!) {
                    "This route's saved forecast does not cover the current time. Available departure window: $earliestDeparture to $latestDeparture."
                }
                val calculation = withContext(Dispatchers.Default) {
                    val current = evaluate(departure)
                    current to findSuggestion(departure, current)
                }
                _uiState.value = TripPlannerUiState(
                    isLoading = false,
                    departure = departure,
                    referenceDeparture = departure,
                    result = calculation.first,
                    suggestion = calculation.second,
                    locations = locationsById.values.toList(),
                    route = activeRoute,
                    availableRoutes = RouteConfig.ROUTES,
                    offlineStatus = "Forecasts for this route are stored on this device as they load. Offline availability ends with the provider forecast window.",
                    offlineCoverageStart = earliestDeparture,
                    offlineCoverageEnd = latestDeparture,
                )
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Throwable) {
                _uiState.value = TripPlannerUiState(
                    isLoading = false,
                    error = failure.message ?: "Unable to load route weather.",
                    route = activeRoute,
                )
            }
        }
    }

    fun selectDeparture(departure: LocalDateTime) {
        if (seriesByLocation.isEmpty()) return
        val clamped = clampDeparture(departure)
        val requestId = ++calculationRequestId
        _uiState.value = _uiState.value.copy(departure = clamped, isRecalculating = true)
        viewModelScope.launch {
            try {
                val calculation = withContext(Dispatchers.Default) {
                    val current = evaluate(clamped)
                    current to findSuggestion(clamped, current)
                }
                if (requestId == calculationRequestId) {
                    _uiState.value = _uiState.value.copy(
                        result = calculation.first,
                        suggestion = calculation.second,
                        isRecalculating = false,
                        error = null,
                    )
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Throwable) {
                if (requestId == calculationRequestId) {
                    _uiState.value = _uiState.value.copy(
                        isRecalculating = false,
                        error = failure.message ?: "Unable to update the route forecast.",
                    )
                }
            }
        }
    }

    fun useSuggestion() {
        _uiState.value.suggestion?.let { selectDeparture(it.departure) }
    }

    fun selectRoute(routeId: String) {
        val route = RouteConfig.ROUTES.firstOrNull { it.id == routeId } ?: return
        if (route.id == activeRoute.id) return
        activeRoute = route
        load()
    }

    private fun evaluate(departure: LocalDateTime): RouteResult = RouteWeather.evaluate(
        departure = departure,
        seriesByLocation = seriesByLocation,
        allPersonas = routePersonas,
        coastalByLocation = coastalByLocation,
        route = activeRoute.waypoints,
        personaIds = activePersonaIds,
    )

    private fun findSuggestion(departure: LocalDateTime, current: RouteResult): DepartureSuggestion? = RouteWeather.bestDeparture(
        departure = departure,
        current = current,
        seriesByLocation = seriesByLocation,
        allPersonas = routePersonas,
        coastalByLocation = coastalByLocation,
        route = activeRoute.waypoints,
        personaIds = activePersonaIds,
    )

    private fun clampDeparture(departure: LocalDateTime): LocalDateTime {
        departureRange?.let { return departure.coerceIn(it.start, it.endInclusive) }
        val waypoints = activeRoute.waypoints
        val routeStart = seriesByLocation[waypoints.first().locationId]?.firstOrNull()?.time ?: return departure
        val routeEnd = seriesByLocation[waypoints.last().locationId]?.lastOrNull()?.time ?: return departure
        val latestDeparture = routeEnd.minusMinutes(waypoints.last().minutesFromStart.toLong())
        return departure.coerceIn(routeStart, maxOf(routeStart, latestDeparture))
    }
}