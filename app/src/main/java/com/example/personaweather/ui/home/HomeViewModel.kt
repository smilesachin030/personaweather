package com.example.personaweather.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personaweather.core.engine.AdvisoryEngine
import com.example.personaweather.core.engine.Insights
import com.example.personaweather.core.engine.SmartActionPolicy
import com.example.personaweather.core.model.Advisory
import com.example.personaweather.core.model.Location
import com.example.personaweather.core.model.Persona
import com.example.personaweather.core.model.RulesCatalog
import com.example.personaweather.core.model.SmartDeviceState
import com.example.personaweather.core.model.WeatherRow
import com.example.personaweather.data.UserPrefs
import com.example.personaweather.data.WeatherDataSource
import com.example.personaweather.data.OpenMeteoWeatherDataSource
import com.example.personaweather.data.OpenMeteoGeocodingClient
import com.example.personaweather.notify.AlertNotifier
import com.example.personaweather.notify.ClothesRainAlarm
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class HomeViewModel(
    private val prefs: UserPrefs,
    private val catalog: RulesCatalog,
    private val dataSource: WeatherDataSource,
    private val notificationHelper: AlertNotifier? = null,
    private val onHomeLoaded: () -> Unit = {},
    private val geocodingClient: OpenMeteoGeocodingClient = OpenMeteoGeocodingClient(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    private var loadJob: Job? = null
    private var loadRequestId = 0
    private var locationSearchJob: Job? = null
    private var locationSearchRequestId = 0

    init {
        load()
    }

    fun load(notifyAlerts: Boolean = true) {
        val requestId = ++loadRequestId
        loadJob?.cancel()
        if (_uiState.value.error != null || _uiState.value.currentRow == null) {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        }
        loadJob = viewModelScope.launch {
            try {
                val locations = (dataSource.locations() + prefs.rememberedLocations).distinctBy { it.id }
                if (requestId == loadRequestId) {
                    _uiState.value = _uiState.value.copy(locations = locations)
                }
                val selectedLocationId = getSelectedLocationId(locations)
                val selectedLocation = resolveSelectedLocation(locations, selectedLocationId)
                if (requestId == loadRequestId) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = true,
                        error = null,
                        location = selectedLocation,
                        locations = locations,
                        currentRow = null,
                        weatherRows = emptyList(),
                        hourlyRows = emptyList(),
                        dailySummaries = emptyList(),
                        advisories = emptyList(),
                    )
                }
                val series = dataSource.series(selectedLocation)
                val activePersonas = resolvePersonas(selectedLocation)
                val simTime = resolveSimTime(series)
                val currentIndex = findNearestIndex(series, simTime)
                val currentRow = series.getOrNull(currentIndex) ?: series.firstOrNull()
                val smartAction = SmartActionPolicy.recommend(currentRow)
                val rainAlarmPersona = if (prefs.clothAlarmEnabled) {
                    ClothesRainAlarm.persona(catalog)
                } else {
                    null
                }
                val advisories = if (series.isEmpty()) emptyList() else AdvisoryEngine.evaluate(
                    series = series,
                    nowIndex = currentIndex.coerceAtLeast(0),
                    personas = activePersonas,
                    isCoastal = selectedLocation.isCoastal,
                )
                val rainAlarmAdvisories = if (prefs.clothAlarmEnabled) {
                    ClothesRainAlarm.evaluate(
                        series = series,
                        nowIndex = currentIndex.coerceAtLeast(0),
                        catalog = catalog,
                        isCoastal = selectedLocation.isCoastal,
                    )
                } else {
                    emptyList()
                }
                val finalAdvisories = (advisories + rainAlarmAdvisories)
                    .distinctBy { it.ruleId to it.personaId }
                if (notifyAlerts) notificationHelper?.notifyAlerts(selectedLocation, simTime, finalAdvisories, prefs)

                val orderedCards = buildOrderedCardIds(activePersonas)
                val disabledCards = effectiveDisabledCards(activePersonas)
                val cards = visibleCardIds(activePersonas, orderedCards, disabledCards)
                val upcomingAdvisories = buildUpcomingAdvisories(
                    series = series,
                    currentIndex = currentIndex,
                    personas = activePersonas + listOfNotNull(rainAlarmPersona),
                    isCoastal = selectedLocation.isCoastal,
                )
                val loadedState = HomeUiState(
                    isLoading = false,
                    error = null,
                    location = selectedLocation,
                    locations = locations,
                    savedDestinationIds = prefs.savedDestinationIds.filter { id -> locations.any { it.id == id } },
                    selectedPersonas = activePersonas,
                    advisories = finalAdvisories,
                    weatherRows = series,
                    hourlyRows = series.drop(currentIndex.coerceAtLeast(0)).take(24),
                    dailySummaries = Insights.dailySummaries(series, simTime.toLocalDate()),
                    currentRow = currentRow,
                    simTime = simTime,
                    rainSentence = if (series.isNotEmpty()) Insights.rainSentence(series, currentIndex.coerceAtLeast(0)) else "No data available",
                    headline = buildHeadline(currentRow, finalAdvisories),
                    hasMarineCard = selectedLocation.isCoastal || currentRow?.waveHeightM != null,
                    cards = cards,
                    orderedCardIds = orderedCards,
                    allCards = catalog.cards.keys.toList(),
                    disabledCards = disabledCards,
                    upcomingAdvisories = upcomingAdvisories,
                    dataSourceLabel = (dataSource as? OpenMeteoWeatherDataSource)?.statusLabel(selectedLocation.id) ?: "Open-Meteo forecast",
                    dataSourceNotice = null,
                    weatherMode = "live",
                    smartAction = smartAction,
                )
                if (requestId == loadRequestId) {
                    _uiState.value = loadedState
                    onHomeLoaded()
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (t: Throwable) {
                if (requestId == loadRequestId) {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = t.message ?: "Unable to load weather data")
                }
            }
        }
    }

    fun setLocation(locationId: String) {
        if (locationId.isBlank()) return
        val location = _uiState.value.locations.firstOrNull { it.id == locationId }
            ?: knownLocations()[locationId]
            ?: prefs.rememberedLocation(locationId)
        if (location == null) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "This location is no longer available. Search for it again.",
            )
            return
        }
        setLocation(location)
    }

    fun setLocation(location: Location) {
        if (location.id.isBlank()) return
        prefs.rememberLocation(location)
        prefs.locationId = location.id
        load()
    }

    fun searchLocations(query: String) {
        val normalizedQuery = query.trim()
        val requestId = ++locationSearchRequestId
        locationSearchJob?.cancel()
        val localMatches = _uiState.value.locations.filter { location ->
            location.name.contains(normalizedQuery, ignoreCase = true) ||
                location.adminArea.orEmpty().contains(normalizedQuery, ignoreCase = true) ||
                location.character.contains(normalizedQuery, ignoreCase = true)
        }
        _uiState.value = _uiState.value.copy(
            locationSearchQuery = query,
            locationSearchResults = localMatches,
            isSearchingLocations = normalizedQuery.length >= MIN_LOCATION_QUERY_LENGTH,
            locationSearchError = null,
        )
        if (normalizedQuery.length < MIN_LOCATION_QUERY_LENGTH) {
            _uiState.value = _uiState.value.copy(isSearchingLocations = false)
            return
        }
        val searchLanguage = prefs.language
        locationSearchJob = viewModelScope.launch {
            delay(LOCATION_SEARCH_DEBOUNCE_MS)
            try {
                val remoteMatches = geocodingClient.search(normalizedQuery, searchLanguage)
                if (requestId == locationSearchRequestId) {
                    val combinedMatches = (remoteMatches + localMatches).distinctBy { it.id }
                    _uiState.value = _uiState.value.copy(
                        locationSearchResults = combinedMatches,
                        isSearchingLocations = false,
                    )
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Exception) {
                if (requestId == locationSearchRequestId) {
                    _uiState.value = _uiState.value.copy(
                        isSearchingLocations = false,
                        locationSearchError = failure.message ?: "Place search is unavailable.",
                    )
                }
            }
        }
    }

    fun toggleSavedDestination(locationId: String) {
        if (locationId !in _uiState.value.locations.map { it.id }) return
        val saved = prefs.savedDestinationIds
        prefs.savedDestinationIds = if (locationId in saved) saved - locationId else saved + locationId
        _uiState.value = _uiState.value.copy(savedDestinationIds = prefs.savedDestinationIds)
    }

    fun setPersonas(personaIds: List<String>) {
        if (personaIds.isEmpty()) return
        prefs.personas = personaIds
        load()
    }

    fun setCardConfiguration(order: List<String>, disabled: Set<String>) {
        val validIds = catalog.cards.keys
        prefs.cardsOrder = order.filter { it in validIds }.distinct()
        prefs.cardsDisabled = disabled.intersect(validIds)
        prefs.cardsCustomized = true
        val personas = _uiState.value.selectedPersonas
        val orderedCards = buildOrderedCardIds(personas)
        val disabledCards = prefs.cardsDisabled
        _uiState.value = _uiState.value.copy(
            cards = visibleCardIds(personas, orderedCards, disabledCards),
            orderedCardIds = orderedCards,
            disabledCards = disabledCards,
        )
    }

    fun resetCardConfiguration() {
        prefs.cardsCustomized = false
        prefs.cardsOrder = emptyList()
        prefs.cardsDisabled = emptySet()
        val personas = _uiState.value.selectedPersonas
        val orderedCards = buildOrderedCardIds(personas)
        val disabledCards = effectiveDisabledCards(personas)
        _uiState.value = _uiState.value.copy(
            cards = visibleCardIds(personas, orderedCards, disabledCards),
            orderedCardIds = orderedCards,
            disabledCards = disabledCards,
        )
    }

    fun confirmSmartAction() {
        val suggestion = _uiState.value.smartAction ?: return
        _uiState.value = _uiState.value.copy(
            smartAction = null,
            smartDeviceState = SmartDeviceState(
                deviceType = suggestion.deviceType,
                isOn = true,
                label = "Demo ${suggestion.deviceType.uppercase()} is on",
            ),
        )
    }

    fun dismissSmartAction() {
        _uiState.value = _uiState.value.copy(smartAction = null)
    }

    private fun getSelectedLocationId(locations: List<Location>): String {
        val configured = prefs.locationId.trim()
        if (configured.isNotBlank() && (locations.any { it.id == configured } || knownLocations().containsKey(configured))) {
            return configured
        }
        throw IllegalStateException("The selected location is unavailable. Search for another location.")
    }

    private fun resolveSelectedLocation(locations: List<Location>, selectedLocationId: String): Location {
        return locations.firstOrNull { it.id == selectedLocationId }
            ?: knownLocations()[selectedLocationId]
            ?: throw IllegalStateException("The selected location is unavailable. Search for another location.")
    }

    private fun resolvePersonas(location: Location): List<Persona> {
        val selected = prefs.personas
        val personas = selected.mapNotNull { catalog.personas[it] }
        return personas.distinctBy { it.id }
            .ifEmpty { catalog.personas.values.toList() }
    }

    private fun resolveSimTime(series: List<WeatherRow>): LocalDateTime {
        val currentHour = LocalDateTime.now(ZoneId.of("Asia/Kolkata")).withMinute(0).withSecond(0).withNano(0)
        return if (series.isNotEmpty()) currentHour.coerceIn(series.first().time, series.last().time) else currentHour
    }

    private fun findNearestIndex(series: List<WeatherRow>, simTime: LocalDateTime): Int {
        if (series.isEmpty()) return 0
        return series.indices.minByOrNull { absMinutes(series[it].time, simTime) } ?: 0
    }

    private fun absMinutes(a: LocalDateTime, b: LocalDateTime): Long {
        return kotlin.math.abs(java.time.Duration.between(a, b).toMinutes())
    }

    private fun defaultCardIds(personas: List<Persona>): List<String> {
        val personaCards = personas.flatMap { it.defaultCards }
        val fallback = if (personaCards.isEmpty()) listOf("aqi_card", "pollen_card", "uv_card", "humidity_card") else emptyList()
        return (personaCards + fallback + "trip_planner_card").distinct().filter { it in catalog.cards }
    }

    private fun buildOrderedCardIds(personas: List<Persona>): List<String> {
        val stableAllCards = listOf(
            "aqi_card", "pollen_card", "uv_card", "humidity_card", "running_window_card", "heat_card",
            "marine_card", "travel_alerts_card", "family_commute_card", "agriculture_card", "commute_traffic_card",
            "event_planning_card", "cloth_drying_card", "trip_planner_card", "daily_outlook_card",
        ).filter { it in catalog.cards } + catalog.cards.keys.filter { it !in STABLE_CARD_IDS }
        return (prefs.cardsOrder.filter { it in catalog.cards } + defaultCardIds(personas) + stableAllCards).distinct()
    }

    private fun effectiveDisabledCards(personas: List<Persona>): Set<String> =
        if (prefs.cardsCustomized) prefs.cardsDisabled
        else catalog.cards.keys - defaultCardIds(personas).toSet()

    private fun visibleCardIds(personas: List<Persona>, order: List<String>, disabled: Set<String>): List<String> {
        val candidates = if (prefs.cardsCustomized) order else defaultCardIds(personas)
        return candidates.filter { it !in disabled }
    }

    private fun buildUpcomingAdvisories(
        series: List<WeatherRow>,
        currentIndex: Int,
        personas: List<Persona>,
        isCoastal: Boolean,
    ): List<UpcomingAdvisory> {
        if (series.isEmpty()) return emptyList()
        val endIndex = minOf(series.lastIndex, currentIndex + 12)
        val firstTripByRule = linkedMapOf<Pair<String, String>, UpcomingAdvisory>()
        for (index in (currentIndex + 1)..endIndex) {
            AdvisoryEngine.evaluate(series, index, personas, isCoastal).forEach { advisory ->
                val key = advisory.personaId to advisory.ruleId
                if (key !in firstTripByRule) firstTripByRule[key] = UpcomingAdvisory(series[index].time, advisory)
            }
        }
        return firstTripByRule.values.toList()
    }

    private fun buildHeadline(currentRow: WeatherRow?, advisories: List<Advisory>): String {
        if (currentRow == null) return "No weather data"
        return when {
            advisories.any { it.severity == com.example.personaweather.core.model.Severity.ALERT } -> "Alerting conditions"
            advisories.any { it.severity == com.example.personaweather.core.model.Severity.CAUTION } -> "Heads up today"
            currentRow.aqiValue >= 100 -> "Air quality watch"
            else -> "Good conditions for the day"
        }
    }

    private fun defaultLocation() = knownLocations()["nungambakkam"] ?: Location(
        id = "nungambakkam",
        name = "Nungambakkam",
        lat = 13.06,
        lon = 80.25,
        distanceKmFromHub = 14.0,
        direction = "West",
        character = "Residential",
        isCoastal = false,
    )

    private fun knownLocations(): Map<String, Location> = mapOf(
        "nungambakkam" to Location("nungambakkam", "Nungambakkam", 13.08, 80.22, 4.0, "West", "Residential", false),
        "velachery" to Location("velachery", "Velachery", 12.98, 80.22, 14.0, "South", "Residential", false),
        "mahabalipuram" to Location("mahabalipuram", "Mahabalipuram", 12.62, 80.17, 55.0, "East", "Coastal / tourist", true),
        "guindy_alandur" to Location("guindy_alandur", "Guindy / Alandur", 13.01, 80.21, 12.0, "Central", "IT corridor", false),
        "t_nagar" to Location("t_nagar", "T. Nagar", 13.04, 80.23, 6.0, "Central", "Commercial", false),
        "tambaram" to Location("tambaram", "Tambaram", 12.92, 80.12, 25.0, "South", "Suburban residential", false),
        "sriperumbudur" to Location("sriperumbudur", "Sriperumbudur", 12.97, 79.94, 34.0, "West", "Industrial", false),
    )

    companion object {
        private const val MIN_LOCATION_QUERY_LENGTH = 2
        private const val LOCATION_SEARCH_DEBOUNCE_MS = 350L
        private val STABLE_CARD_IDS = setOf(
            "aqi_card", "pollen_card", "uv_card", "humidity_card", "running_window_card", "heat_card",
            "marine_card", "travel_alerts_card", "family_commute_card", "agriculture_card", "commute_traffic_card",
            "event_planning_card", "cloth_drying_card", "trip_planner_card", "daily_outlook_card",
        )
    }
}
