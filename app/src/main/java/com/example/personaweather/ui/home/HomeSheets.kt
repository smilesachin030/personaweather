package com.example.personaweather.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.personaweather.core.model.CardSpec
import com.example.personaweather.core.model.Advisory
import com.example.personaweather.ui.localization.LocalAppStrings
import com.example.personaweather.core.model.Location
import com.example.personaweather.core.model.Persona
import com.example.personaweather.core.model.Severity
import com.example.personaweather.core.model.WeatherRow
import com.example.personaweather.data.CalendarEvent
import com.example.personaweather.data.IndianCityCatalog
import com.example.personaweather.ui.localization.AppStrings
import com.example.personaweather.ui.components.personaIcon
import com.example.personaweather.ui.theme.PersonaWeatherPalette
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class CalendarEventForecast(
    val locationName: String,
    val isOutdoor: Boolean,
    val isOrganizer: Boolean,
    val weatherRows: List<WeatherRow> = emptyList(),
    val advisories: List<Advisory> = emptyList(),
    val sourceLabel: String? = null,
    val error: String? = null,
)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun LocationSheet(
    locations: List<Location>,
    selectedLocationId: String,
    currentAqi: Int?,
    searchQuery: String,
    searchResults: List<Location>,
    isSearching: Boolean,
    searchError: String?,
    onSearchQueryChange: (String) -> Unit,
    onQuickCity: (String) -> Unit,
    savedDestinationIds: Set<String>,
    onSelect: (Location) -> Unit,
    onToggleSaved: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val filtered = if (searchQuery.trim().length >= 2) searchResults else locations
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(LocalAppStrings.current.text("location_title", "Choose a location"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                label = { Text(LocalAppStrings.current.text("search_places", "Search places")) },
                placeholder = { Text(LocalAppStrings.current.text("search_india_places", "Search cities and localities across India")) },
            )
            if (isSearching) Text(LocalAppStrings.current.text("searching_places", "Searching places…"), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            if (searchError != null) Text(searchError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            Column(Modifier.height(420.dp).verticalScroll(rememberScrollState())) {
                if (searchQuery.isBlank()) {
                    Text(LocalAppStrings.current.text("major_indian_cities", "Major Indian cities"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    IndianCityCatalog.cities.chunked(2).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { city ->
                                FilterChip(
                                    selected = false,
                                    onClick = { onQuickCity(city.name) },
                                    label = { Text(LocalAppStrings.current.locationName(city.name), maxLines = 1) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                    Text(LocalAppStrings.current.text("saved_localities", "Saved and nearby"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (searchQuery.trim().length >= 2 && filtered.isEmpty() && !isSearching && searchError == null) {
                    Text(
                        LocalAppStrings.current.text("location_unavailable", "Sorry, this location is currently unavailable in this version. We’re working on adding it."),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                filtered.forEach { location ->
                    Row(
                        Modifier.fillMaxWidth()
                            .selectable(
                                selected = location.id == selectedLocationId,
                                role = Role.RadioButton,
                                onClick = { onSelect(location) },
                            )
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BoxDot(if (location.id == selectedLocationId && currentAqi != null) aqiColor(currentAqi) else Color.Gray)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(LocalAppStrings.current.locationName(location.name), fontWeight = FontWeight.SemiBold)
                            Text(
                                listOfNotNull(location.adminArea?.let(LocalAppStrings.current::regionName), LocalAppStrings.current.text("live_forecast", "Live forecast"))
                                    .joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (location.isCoastal) Text(LocalAppStrings.current.text("coastal", "Coastal"), color = PersonaWeatherPalette.marine, style = MaterialTheme.typography.labelSmall)
                        if (location.id == selectedLocationId) Text(LocalAppStrings.current.text("selected_location", "Selected"), color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 8.dp), style = MaterialTheme.typography.labelSmall)
                        IconButton(onClick = { onToggleSaved(location.id) }) {
                            Icon(
                                imageVector = if (location.id in savedDestinationIds) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = LocalAppStrings.current.format(if (location.id in savedDestinationIds) "saved_destination_remove" else "saved_destination_add", if (location.id in savedDestinationIds) "Remove %1\$s from saved destinations" else "Save %1\$s as a destination", location.name),
                                tint = if (location.id in savedDestinationIds) PersonaWeatherPalette.event else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun PersonaSheet(
    personas: List<Persona>,
    selectedIds: Set<String>,
    onApply: (List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    var selected by remember(selectedIds) { mutableStateOf(selectedIds) }
    val appStrings = LocalAppStrings.current
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().heightIn(max = 680.dp).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(appStrings.text("persona_sheet_title", "Your personas"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(appStrings.text("persona_sheet_description", "Choose every perspective you want in your forecast."), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(Modifier.weight(1f, fill = false).heightIn(max = 460.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                personas.forEach { persona ->
                    val active = persona.id in selected
                    val personaCardsText = persona.defaultCards.joinToString(" · ") { cardId ->
                        appStrings.cardLabel(cardId, cardId.removeSuffix("_card").replace('_', ' '))
                    }
                    Surface(
                        modifier = Modifier.fillMaxWidth().toggleable(value = active, role = Role.Checkbox) {
                            selected = if (active) selected - persona.id else selected + persona.id
                        },
                        color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(
                                imageVector = personaIcon(persona.id),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp),
                            )
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(appStrings.personaLabel(persona.id, persona.label), fontWeight = FontWeight.SemiBold)
                                Text(personaCardsText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (active) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
            Button(onClick = { onApply(selected.toList()) }, enabled = selected.isNotEmpty(), modifier = Modifier.fillMaxWidth()) { Text(LocalAppStrings.current.text("done", "Done")) }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun EditCardsSheet(
    allCards: List<CardSpec>,
    defaultIds: List<String>,
    orderedIds: List<String>,
    disabledIds: Set<String>,
    isCoastal: Boolean,
    onSave: (List<String>, Set<String>) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    var order by remember(orderedIds, allCards) { mutableStateOf((orderedIds + allCards.map { it.id }).distinct()) }
    var disabled by remember(disabledIds) { mutableStateOf(disabledIds) }
    val specs = allCards.associateBy { it.id }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(LocalAppStrings.current.text("edit_cards", "Edit cards"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(LocalAppStrings.current.text("edit_cards_description", "Reorder cards and choose what appears on Home."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(Modifier.height(440.dp).verticalScroll(rememberScrollState())) {
                order.forEachIndexed { index, cardId ->
                    val spec = specs[cardId] ?: return@forEachIndexed
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(LocalAppStrings.current.cardLabel(cardId, spec.label), fontWeight = FontWeight.Medium)
                            if (spec.requiresCoastal && !isCoastal) Text(LocalAppStrings.current.text("coastal_only", "Coastal only"), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                        }
                        if (cardId == "trip_planner_card" && index > 0) {
                            TextButton(onClick = {
                                val reordered = order.toMutableList()
                                val movedCard = reordered.removeAt(index)
                                reordered.add(0, movedCard)
                                order = reordered
                            }) { Text(LocalAppStrings.current.text("move_to_top", "Top")) }
                        }
                        IconButton(onClick = {
                            if (index > 0) {
                                val reordered = order.toMutableList()
                                val movedCard = reordered.removeAt(index)
                                reordered.add(index - 1, movedCard)
                                order = reordered
                            }
                        }, enabled = index > 0) { Icon(Icons.Default.KeyboardArrowUp, contentDescription = LocalAppStrings.current.format("move_card_up", "Move %1\$s up", LocalAppStrings.current.text("card_$cardId", spec.label))) }
                        IconButton(onClick = {
                            if (index < order.lastIndex) {
                                val reordered = order.toMutableList()
                                val movedCard = reordered.removeAt(index)
                                reordered.add(index + 1, movedCard)
                                order = reordered
                            }
                        }, enabled = index < order.lastIndex) { Icon(Icons.Default.KeyboardArrowDown, contentDescription = LocalAppStrings.current.format("move_card_down", "Move %1\$s down", LocalAppStrings.current.text("card_$cardId", spec.label))) }
                        Switch(checked = cardId !in disabled, onCheckedChange = { enabled ->
                            disabled = if (enabled) disabled - cardId else disabled + cardId
                        })
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = {
                    val allIds = allCards.map { it.id }
                    order = (defaultIds + allIds).distinct()
                    disabled = allIds.toSet() - defaultIds.toSet()
                    onReset()
                }) { Text(LocalAppStrings.current.text("reset_card_defaults", "Reset to defaults")) }
                Button(onClick = { onSave(order, disabled); onDismiss() }) { Text(LocalAppStrings.current.text("done", "Done")) }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
fun CalendarEventsScreen(
    hasPermission: Boolean,
    isLoading: Boolean,
    events: List<CalendarEvent>,
    eventForecasts: Map<String, CalendarEventForecast>,
    analyzingEventKey: String?,
    locations: List<Location>,
    currentLocationId: String,
    isEventPlannerEnabled: Boolean,
    loadError: String?,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onRequestPermission: () -> Unit,
    onAnalyzeEvent: (CalendarEvent, Boolean, Boolean, Location) -> Unit,
) {
    val strings = LocalAppStrings.current
    var eventToAnalyze by remember { mutableStateOf<CalendarEvent?>(null) }
    var selectedOutdoor by remember(eventToAnalyze?.instanceKey) { mutableStateOf<Boolean?>(null) }
    var selectedOrganizer by remember(eventToAnalyze?.instanceKey) { mutableStateOf<Boolean?>(null) }
    val defaultLocation = locations.firstOrNull { it.id == currentLocationId } ?: locations.firstOrNull()
    var selectedLocation by remember(eventToAnalyze?.instanceKey) {
        val eventPlace = eventToAnalyze?.location?.trim()
        val eventMatches = eventPlace?.let { query ->
            locations.filter { it.name.equals(query, ignoreCase = true) || query.contains(it.name, ignoreCase = true) }
        }
        val eventMatch = eventMatches?.singleOrNull()
        mutableStateOf(eventMatch ?: if (eventPlace.isNullOrBlank()) defaultLocation else null)
    }
    var eventLocation by remember(eventToAnalyze?.instanceKey) {
        mutableStateOf(eventToAnalyze?.location?.takeIf(String::isNotBlank) ?: defaultLocation?.name.orEmpty())
    }
    val locationSuggestions = remember(eventLocation, locations) {
        val query = eventLocation.trim()
        if (query.length < 2) emptyList()
        else locations.filter { location ->
            location.name.contains(query, ignoreCase = true) ||
                location.adminArea.orEmpty().contains(query, ignoreCase = true) ||
                query.contains(location.name, ignoreCase = true)
        }.take(6)
    }
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) { Icon(Icons.Default.Close, contentDescription = strings.text("back_to_home", "Back to home")) }
            Text(strings.text("upcoming_events", "Upcoming events"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onRefresh) { Text(strings.text("refresh", "Refresh")) }
        }

        if (!hasPermission) {
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(strings.text("calendar_access_off", "Calendar access is off"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(strings.text("allow_access_to_read_calendars", "Allow access to read Google and other synced calendars so PersonaWeather can suggest weather advice for your next event."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = onRequestPermission, modifier = Modifier.fillMaxWidth()) { Text(strings.text("allow_calendar_access", "Allow calendar access")) }
                }
            }
            return
        }

        if (isLoading) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                Text(strings.text("loading_your_next_events", "Loading your next events…"))
            }
        }

        if (loadError != null) {
            Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.medium) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(loadError, color = MaterialTheme.colorScheme.onErrorContainer)
                    TextButton(onClick = onRefresh) { Text(strings.text("retry", "Retry")) }
                }
            }
        }

        if (events.isEmpty() && !isLoading && loadError == null) {
            Text(strings.text("no_upcoming_events_30_days", "No upcoming events in the next 30 days."), color = MaterialTheme.colorScheme.onSurfaceVariant)
            return
        }

        events.forEach { event ->
            val forecast = eventForecasts[event.instanceKey]
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                        Text(event.title, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        if (!event.location.isNullOrBlank()) {
                            Text(strings.text("outdoor_question", "Outdoor?"), color = PersonaWeatherPalette.uv, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    if (!event.location.isNullOrBlank()) {
                        Text(event.location, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(formatCalendarTime(event, strings), style = MaterialTheme.typography.bodyMedium)
                    if (!event.calendarName.isNullOrBlank()) {
                        Text(event.calendarName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(
                        onClick = { eventToAnalyze = event },
                        enabled = analyzingEventKey == null,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (analyzingEventKey == event.instanceKey) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(strings.text("check_event_weather", "Check event weather"))
                    }
                    if (forecast != null) {
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(
                                strings.format(
                                    "event_forecast_place_context",
                                    "%1\$s forecast · %2\$s · %3\$s · %4\$s",
                                    forecast.locationName,
                                    if (forecast.isOutdoor) strings.text("outdoor", "Outdoor") else strings.text("indoor", "Indoor"),
                                    if (forecast.isOrganizer) strings.text("event_role_organizer", "Organizer") else strings.text("event_role_non_organizer", "Non-organiser"),
                                    strings.text("during_event", "during event"),
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            forecast.sourceLabel?.let { source ->
                                Text(source, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (forecast.error != null) {
                                Text(forecast.error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                            } else if (forecast.weatherRows.isNotEmpty()) {
                                val highestAqi = forecast.weatherRows.maxBy { it.aqiValue }
                                val peakTemperature = forecast.weatherRows.maxOf { it.temperature }
                                val peakRainChance = forecast.weatherRows.maxOf { it.rainChancePercent }
                                if (forecast.isOutdoor) {
                                    Text(
                                        strings.format(
                                            "event_weather_summary",
                                            "High %1\$.0f° · Rain chance up to %2\$.0f%% · Peak AQI %3\$d (%4\$s)",
                                            peakTemperature,
                                            peakRainChance,
                                            highestAqi.aqiValue,
                                            highestAqi.aqiCategory,
                                        ),
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                } else {
                                    Text(
                                        strings.format("event_indoor_air_summary", "Outdoor AQI forecast: %1\$d (%2\$s)", highestAqi.aqiValue, highestAqi.aqiCategory),
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    Text(
                                        strings.text("event_indoor_air_caveat", "Outdoor air-quality estimate only; indoor air may differ."),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                if (!isEventPlannerEnabled) {
                                    Text(
                                        strings.text("event_planner_persona_needed", "Enable the Event Planner persona from Home to get organiser-specific suggestions."),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                } else if (forecast.advisories.isEmpty()) {
                                    Text(strings.text("event_no_configured_advice", "No configured advisories were triggered during this event."), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                                } else {
                                    forecast.advisories.forEach { advisory ->
                                        Text(
                                            "${if (advisory.severity == Severity.ALERT) "Alert" else "Caution"}: ${strings.advisory(advisory.message, advisory.messageTa, advisory.messageHi)}",
                                            color = if (advisory.severity == Severity.ALERT) MaterialTheme.colorScheme.error else PersonaWeatherPalette.uv,
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    eventToAnalyze?.let { event ->
        AlertDialog(
            onDismissRequest = { eventToAnalyze = null },
            title = { Text(strings.text("event_weather_context_title", "Check weather for this event")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(strings.text("event_weather_context_prompt", "Choose whether this event is outdoors, then confirm a forecast locality. Calendar location text stays on this device."))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = selectedOutdoor == true, onClick = { selectedOutdoor = true }, label = { Text(strings.text("outdoor", "Outdoor")) })
                        FilterChip(selected = selectedOutdoor == false, onClick = { selectedOutdoor = false }, label = { Text(strings.text("indoor", "Indoor")) })
                    }
                    if (isEventPlannerEnabled) {
                        Text(strings.text("event_role_prompt", "Who are you attending as?"))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = selectedOrganizer == true,
                                onClick = { selectedOrganizer = true },
                                label = { Text(strings.text("event_role_organizer", "Organizer")) },
                            )
                            FilterChip(
                                selected = selectedOrganizer == false,
                                onClick = { selectedOrganizer = false },
                                label = { Text(strings.text("event_role_non_organizer", "Non-organiser")) },
                            )
                        }
                    }
                    OutlinedTextField(
                        value = eventLocation,
                        onValueChange = {
                            eventLocation = it
                            selectedLocation = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(strings.text("event_location", "Event location")) },
                        placeholder = { Text(defaultLocation?.name.orEmpty()) },
                    )
                    Column(Modifier.heightIn(max = 180.dp).verticalScroll(rememberScrollState())) {
                        locationSuggestions.take(6).forEach { suggestion ->
                            FilterChip(
                                selected = selectedLocation?.id == suggestion.id,
                                onClick = { selectedLocation = suggestion; eventLocation = suggestion.name },
                                label = {
                                    Text(
                                        listOfNotNull(suggestion.name, suggestion.adminArea).distinct().joinToString(", "),
                                        maxLines = 1,
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        if (locationSuggestions.isEmpty()) {
                            Text(
                                strings.text("event_location_pick_verified", "Choose a matching saved forecast locality. Exact addresses are not sent to a search service."),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedLocation?.let { place ->
                            val outdoor = selectedOutdoor ?: return@TextButton
                            val organizer = selectedOrganizer ?: !isEventPlannerEnabled
                            onAnalyzeEvent(event, outdoor, organizer, place)
                        }
                        eventToAnalyze = null
                    },
                    enabled = selectedOutdoor != null && selectedLocation != null &&
                        (!isEventPlannerEnabled || selectedOrganizer != null),
                ) { Text(strings.text("analyze_event_weather", "Show advice")) }
            },
            dismissButton = { TextButton(onClick = { eventToAnalyze = null }) { Text(strings.text("cancel", "Cancel")) } },
        )
    }
}

private fun formatCalendarTime(event: CalendarEvent, strings: AppStrings): String {
    val formatter = DateTimeFormatter.ofPattern("EEE, d MMM • h:mm a", strings.locale)
    val start = LocalDateTime.ofInstant(event.start, ZoneId.systemDefault())
    val end = event.end?.let { LocalDateTime.ofInstant(it, ZoneId.systemDefault()) }
    return if (event.isAllDay) {
        if (end != null) strings.format("all_day_time_range", "All day • %1\$s to %2\$s", start.format(formatter), end.format(formatter))
        else strings.format("all_day_event_time", "All day • %1\$s", start.format(formatter))
    } else {
        if (end != null) strings.format("event_time_range", "%1\$s to %2\$s", start.format(formatter), end.format(formatter))
        else start.format(formatter)
    }
}

@Composable
fun SettingsScreen(
    notificationsEnabled: Boolean,
    clothAlarmEnabled: Boolean,
    onBack: () -> Unit,
    onPersonas: () -> Unit,
    onLocation: () -> Unit,
    onNotificationsChange: (Boolean) -> Unit,
    onClothAlarmChange: (Boolean) -> Unit,
    onAlerts: () -> Unit,
    onReset: () -> Unit,
    language: String,
    onLanguageChange: (String) -> Unit,
    highContrast: Boolean,
    largeText: Boolean,
    onHighContrastChange: (Boolean) -> Unit,
    onLargeTextChange: (Boolean) -> Unit,
) {
    var showAbout by remember { mutableStateOf(false) }
    val strings = LocalAppStrings.current
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) { Icon(Icons.Default.Close, contentDescription = strings.text("close", "Close settings")) }
            Text(
                strings.text("settings", "Settings"),
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            strings.text("language", "Language"),
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = language == "en",
                onClick = { onLanguageChange("en") },
                label = { Text(strings.text("english", "English")) },
            )
            FilterChip(
                selected = language == "ta",
                onClick = { onLanguageChange("ta") },
                label = { Text(strings.text("tamil", "தமிழ்")) },
            )
            FilterChip(
                selected = language == "hi",
                onClick = { onLanguageChange("hi") },
                label = { Text(strings.text("hindi", "हिन्दी")) },
            )
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    strings.text("weather_source", "Weather source"),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    strings.text("live_mode_help", "Live forecasts from Open-Meteo require internet. Air quality and marine data are modeled forecasts; pollen readings depend on forecast coverage for your location."),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        SettingsToggle(strings.text("high_contrast", "High contrast"), highContrast, onHighContrastChange)
        SettingsToggle(strings.text("large_text", "Larger text"), largeText, onLargeTextChange)
        SettingsRow(strings.text("your_personas", "Your personas"), strings.text("personas_help", "Choose whose advice appears"), onPersonas)
        SettingsRow(strings.text("location", "Location"), strings.text("location_help", "Change your forecast locality"), onLocation)
        SettingsToggle(strings.text("notifications_label", "Weather notifications"), notificationsEnabled, onNotificationsChange)
        SettingsToggle(strings.text("clothes_alarm", "Clothes rain alarm"), clothAlarmEnabled, onClothAlarmChange)
        SettingsRow(strings.text("upcoming_alerts", "Upcoming alerts"), strings.text("alerts_help", "Current and next 12 hours"), onAlerts)
        SettingsRow(strings.text("about", "About"), strings.text("about_help", "Weather and environmental forecasts"), { showAbout = true })
        SettingsRow(strings.text("reset_app", "Reset app"), strings.text("reset_help", "Clear preferences and restart onboarding"), onReset, color = MaterialTheme.colorScheme.error)
    }
    if (showAbout) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text(strings.text("about_title", "About PersonaWeather")) },
            text = { Text(strings.text("about_description", "Weather forecasts and air quality: Open-Meteo CAMS models. Marine waves and sea level: Open-Meteo using DWD/ECMWF models. These are forecasts, not station observations. Pollen forecasts are shown when available for the selected location; sea-level estimates are not for navigation.")) },
            confirmButton = { TextButton(onClick = { showAbout = false }) { Text(strings.text("done", "Done")) } },
        )
    }
}

@Composable
fun AlertsScreen(
    state: HomeUiState,
    onBack: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.Close, contentDescription = "Back to Home") }
            Text(LocalAppStrings.current.text("alerts", "Alerts"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
            Text(LocalAppStrings.current.text("now", "Now"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (state.currentRow == null) {
            Text(LocalAppStrings.current.text("current_forecast_unavailable", "Current forecast unavailable."), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else if (state.advisories.isEmpty()) {
            Text(LocalAppStrings.current.text("all_clear", "All clear — nothing needs your attention right now."), color = PersonaWeatherPalette.agriculture)
        } else {
            state.advisories.forEach { advisory ->
                AlertRow(LocalAppStrings.current.advisory(advisory.message, advisory.messageTa, advisory.messageHi), advisory.personaId, advisory.severity)
            }
        }
        Text(LocalAppStrings.current.text("upcoming_12_hours", "Upcoming · next 12 hours"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (state.upcomingAdvisories.isEmpty()) {
            Text(LocalAppStrings.current.text("no_upcoming_alerts", "No new alerts are expected in the next 12 hours."), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            state.upcomingAdvisories.forEach { item ->
                AlertRow(
                    message = "${LocalAppStrings.current.format("around_time", "Around %1\$s", item.time.format(java.time.format.DateTimeFormatter.ofPattern("h:mm a", LocalAppStrings.current.locale)))} · ${LocalAppStrings.current.advisory(item.advisory.message, item.advisory.messageTa, item.advisory.messageHi)}",
                    persona = item.advisory.personaId,
                    severity = item.advisory.severity,
                )
            }
        }
    }
}

@Composable
private fun AlertRow(message: String, persona: String, severity: Severity) {
    val color = if (severity == Severity.ALERT) PersonaWeatherPalette.alert else PersonaWeatherPalette.uv
    Surface(color = color.copy(alpha = 0.13f), shape = MaterialTheme.shapes.medium) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val strings = LocalAppStrings.current
            Text(
                "${strings.text(if (severity == Severity.ALERT) "severity_alert" else "severity_caution", severity.name)} · ${strings.personaLabel(persona)}",
                color = color,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(message, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun SettingsRow(label: String, supporting: String, onClick: () -> Unit, color: Color = MaterialTheme.colorScheme.onSurface) {
    Surface(onClick = onClick, color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text(label, color = color, fontWeight = FontWeight.SemiBold)
            Text(supporting, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SettingsToggle(label: String, checked: Boolean, onChanged: (Boolean) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium) {
        Row(
            Modifier.fillMaxWidth()
                .heightIn(min = 48.dp)
                .toggleable(value = checked, role = Role.Switch, onValueChange = onChanged)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, fontWeight = FontWeight.Medium)
            Switch(checked = checked, onCheckedChange = null)
        }
    }
}

@Composable
private fun BoxDot(color: Color) {
    Spacer(Modifier.size(10.dp).clip(CircleShape).background(color))
}

private fun aqiColor(value: Int): Color = when {
    value <= 50 -> Color(0xFF26A873)
    value <= 100 -> Color(0xFF8AAE42)
    value <= 150 -> Color(0xFFE4A532)
    value <= 200 -> Color(0xFFE36A4D)
    else -> Color(0xFF915B9E)
}