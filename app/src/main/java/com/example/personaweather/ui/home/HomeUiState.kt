package com.example.personaweather.ui.home

import com.example.personaweather.core.model.Advisory
import com.example.personaweather.core.model.DailySummary
import com.example.personaweather.core.model.Location
import com.example.personaweather.core.model.Persona
import com.example.personaweather.core.model.SmartAction
import com.example.personaweather.core.model.SmartDeviceState
import com.example.personaweather.core.model.WeatherRow
import java.time.LocalDateTime

data class UpcomingAdvisory(val time: LocalDateTime, val advisory: Advisory)

data class HomeUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val location: Location = Location("", "Location unavailable", 0.0, 0.0),
    val locations: List<Location> = emptyList(),
    val locationSearchQuery: String = "",
    val locationSearchResults: List<Location> = emptyList(),
    val isSearchingLocations: Boolean = false,
    val locationSearchError: String? = null,
    val savedDestinationIds: List<String> = emptyList(),
    val selectedPersonas: List<Persona> = emptyList(),
    val advisories: List<Advisory> = emptyList(),
    val weatherRows: List<WeatherRow> = emptyList(),
    val hourlyRows: List<WeatherRow> = emptyList(),
    val dailySummaries: List<DailySummary> = emptyList(),
    val currentRow: WeatherRow? = null,
    val simTime: LocalDateTime = LocalDateTime.of(2026, 9, 28, 9, 0),
    val rainSentence: String = "No rain expected in the next 24 hours",
    val headline: String = "Good morning",
    val hasMarineCard: Boolean = false,
    val cards: List<String> = emptyList(),
    val orderedCardIds: List<String> = emptyList(),
    val allCards: List<String> = emptyList(),
    val disabledCards: Set<String> = emptySet(),
    val upcomingAdvisories: List<UpcomingAdvisory> = emptyList(),
    val dataSourceLabel: String = "Open-Meteo forecast",
    val dataSourceNotice: String? = null,
    val weatherMode: String = "live",
    val smartAction: SmartAction? = null,
    val smartDeviceState: SmartDeviceState = SmartDeviceState(),
) {
    val displayCards: List<String>
        get() = cards.ifEmpty { listOf("aqi_card", "pollen_card", "uv_card", "humidity_card") }
}
