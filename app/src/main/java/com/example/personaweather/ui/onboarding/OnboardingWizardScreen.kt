package com.example.personaweather.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import com.example.personaweather.data.OpenMeteoGeocodingClient
import com.example.personaweather.data.IndianCityCatalog
import com.example.personaweather.core.model.Location
import com.example.personaweather.core.model.Persona
import com.example.personaweather.core.model.RulesCatalog
import com.example.personaweather.onboarding.OnboardingDefaults
import com.example.personaweather.onboarding.OnboardingSummary
import com.example.personaweather.ui.components.personaIcon
import com.example.personaweather.ui.theme.PersonaWeatherTheme
import com.example.personaweather.ui.localization.LocalAppStrings

private val PersonaSelectionSaver = listSaver<Set<String>, String>(
    save = { it.toList() },
    restore = { it.toSet() },
)

private val SelectedLocationSaver = listSaver<Location?, String>(
    save = { location ->
        location?.let {
            listOf(
                it.id,
                it.name,
                it.lat.toString(),
                it.lon.toString(),
                it.distanceKmFromHub.toString(),
                it.direction,
                it.character,
                it.isCoastal.toString(),
                it.adminArea.orEmpty(),
                it.countryCode,
                it.geocodingId?.toString().orEmpty(),
                it.timeZone.orEmpty(),
            )
        } ?: emptyList()
    },
    restore = { saved ->
        if (saved.size != 12) {
            null
        } else {
            val latitude = saved[2].toDoubleOrNull()
            val longitude = saved[3].toDoubleOrNull()
            val distance = saved[4].toDoubleOrNull()
            if (latitude == null || longitude == null || distance == null) {
                null
            } else {
                Location(
                    id = saved[0],
                    name = saved[1],
                    lat = latitude,
                    lon = longitude,
                    distanceKmFromHub = distance,
                    direction = saved[5],
                    character = saved[6],
                    isCoastal = saved[7].toBoolean(),
                    adminArea = saved[8].ifBlank { null },
                    countryCode = saved[9],
                    geocodingId = saved[10].toLongOrNull(),
                    timeZone = saved[11].ifBlank { null },
                )
            }
        }
    },
)

@Composable
fun OnboardingWizardScreen(
    catalog: RulesCatalog,
    locations: List<Location>,
    selectedLocationId: String,
    notificationPermissionGranted: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onDone: (OnboardingSummary) -> Unit,
    language: String,
    onLanguageChange: (String) -> Unit,
) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var selected by rememberSaveable(stateSaver = PersonaSelectionSaver) { mutableStateOf(emptySet()) }
    var locationId by rememberSaveable { mutableStateOf(selectedLocationId) }
    var locationQuery by rememberSaveable { mutableStateOf("") }
    var selectedLocation by rememberSaveable(stateSaver = SelectedLocationSaver) { mutableStateOf(null) }
    var geocodedLocations by remember { mutableStateOf(emptyList<Location>()) }
    var isSearchingLocations by remember { mutableStateOf(false) }
    var locationSearchError by remember { mutableStateOf<String?>(null) }
    val geocodingClient = remember { OpenMeteoGeocodingClient() }
    val personas = catalog.personas.values.sortedBy { it.label }
    val strings = LocalAppStrings.current

    LaunchedEffect(locations, locationId) {
        selectedLocation = locations.firstOrNull { it.id == locationId } ?: selectedLocation
    }
    LaunchedEffect(locationQuery, language) {
        val query = locationQuery.trim()
        geocodedLocations = emptyList()
        locationSearchError = null
        if (query.length < 2) {
            isSearchingLocations = false
            return@LaunchedEffect
        }
        isSearchingLocations = true
        delay(350)
        try {
            geocodedLocations = geocodingClient.search(query, language)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            locationSearchError = error.message ?: "Place search is unavailable."
        }
        isSearchingLocations = false
    }
    val localMatches = locations.filter {
        it.name.contains(locationQuery, ignoreCase = true) ||
            it.adminArea.orEmpty().contains(locationQuery, ignoreCase = true) ||
            it.character.contains(locationQuery, ignoreCase = true)
    }
    val filteredLocations = (geocodedLocations + localMatches).distinctBy { it.id }

    PersonaWeatherTheme(darkTheme = true) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF091B2A), Color(0xFF17384B), Color(0xFF0A2434))))) {
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().semantics {
                            contentDescription = strings.format(
                                "onboarding_progress_accessibility",
                                "Onboarding step %1\$d of 4",
                                step + 1,
                            )
                            progressBarRangeInfo = ProgressBarRangeInfo((step + 1) / 4f, 0f..1f)
                        },
                    ) {
                        repeat(4) { index ->
                            Box(
                                Modifier.weight(1f).height(4.dp)
                                    .background(if (index <= step) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.18f), RoundedCornerShape(99.dp)),
                            )
                        }
                    }
                    Text(
                        LocalAppStrings.current.format("onboarding_step", "Step %1\$d of 4", step + 1),
                        color = Color.White.copy(alpha = 0.72f),
                        style = MaterialTheme.typography.labelMedium,
                    )
                    when (step) {
                        0 -> WelcomeStep(language, onLanguageChange)
                        1 -> PersonaStep(personas, selected) { id -> selected = if (id in selected) selected - id else selected + id }
                        2 -> LocationStep(
                            locations = filteredLocations,
                            query = locationQuery,
                            selectedId = locationId,
                            isSearching = isSearchingLocations,
                            searchError = locationSearchError,
                            onQueryChange = { locationQuery = it },
                            onSelect = { location ->
                                selectedLocation = location
                                locationId = location.id
                            },
                        )
                        else -> AlertsStep(notificationPermissionGranted, onRequestNotificationPermission)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        if (step > 0) TextButton(onClick = { step-- }) { Text(LocalAppStrings.current.text("back", "Back")) } else Spacer(Modifier.size(1.dp))
                        if (step < 3) {
                            Button(onClick = { step++ }, enabled = step != 1 || selected.isNotEmpty()) {
                                Text(if (step == 0) LocalAppStrings.current.text("get_started", "Get started") else LocalAppStrings.current.text("continue", "Continue"))
                            }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { finish(catalog, selected, locationId, selectedLocation, false, onDone) }) { Text(LocalAppStrings.current.text("maybe_later", "Maybe later")) }
                                Button(onClick = { finish(catalog, selected, locationId, selectedLocation, notificationPermissionGranted, onDone) }) { Text(LocalAppStrings.current.text("finish", "Finish")) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeStep(language: String, onLanguageChange: (String) -> Unit) {
    val strings = LocalAppStrings.current
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(strings.text("welcome_title", "Weather that tells you what to do"), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = Color.White)
        Text(strings.text("welcome_description", "Choose what you want weather help with. We’ll bring the most relevant guidance forward."), style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.82f))
        Text(strings.text("language", "Language"), style = MaterialTheme.typography.titleMedium, color = Color.White)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = language == "en", onClick = { onLanguageChange("en") }, label = { Text(strings.text("english", "English")) })
            FilterChip(selected = language == "ta", onClick = { onLanguageChange("ta") }, label = { Text(strings.text("tamil", "தமிழ்")) })
            FilterChip(selected = language == "hi", onClick = { onLanguageChange("hi") }, label = { Text(strings.text("hindi", "हिन्दी")) })
        }
    }
}

@Composable
private fun PersonaStep(personas: List<Persona>, selected: Set<String>, onToggle: (String) -> Unit) {
    val strings = LocalAppStrings.current
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(strings.text("persona_step_title", "What would you like weather help with?"), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
        Text(strings.text("persona_step_description", "Choose one or more needs. Your choices shape advice and alert priority as well as Home cards."), color = Color.White.copy(alpha = 0.82f))
        personas.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { persona ->
                    val active = persona.id in selected
                    Column(
                        Modifier.weight(1f)
                            .heightIn(min = 144.dp)
                            .border(if (active) 1.5.dp else 1.dp, if (active) Color.White else Color.White.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
                            .background(if (active) Color.White.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                            .toggleable(value = active, role = Role.Checkbox) { onToggle(persona.id) }
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Icon(
                                imageVector = personaIcon(persona.id),
                                contentDescription = null,
                                tint = if (active) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.88f),
                                modifier = Modifier.size(26.dp),
                            )
                            if (active) Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Text(strings.personaLabel(persona.id, persona.label), color = Color.White, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                        Text(strings.text("persona_description:${persona.id}", personaDescription(persona.id)), color = Color.White.copy(alpha = 0.82f), style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        if (selected.isEmpty()) {
            Text(
                strings.text("select_persona_hint", "Choose at least one so we can tailor your advice."),
                color = Color.White.copy(alpha = 0.9f),
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            Text(
                strings.format("selected_persona_count", "%1\$d selected", selected.size),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(14.dp),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        strings.text("personalization_preview_title", "What changes for you"),
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        strings.text("personalization_preview_description", "We’ll prioritize relevant advice and alerts, then start Home with these cards. You can adjust the cards later."),
                        color = Color.White.copy(alpha = 0.82f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    val previewCards = personas
                        .filter { it.id in selected }
                        .flatMap { it.defaultCards }
                        .distinct()
                        .take(4)
                    previewCards.forEach { cardId ->
                        Text(
                            "• ${strings.cardLabel(cardId, cardId.removeSuffix("_card").replace('_', ' '))}",
                            color = Color.White.copy(alpha = 0.9f),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LocationStep(
    locations: List<Location>,
    query: String,
    selectedId: String,
    isSearching: Boolean,
    searchError: String?,
    onQueryChange: (String) -> Unit,
    onSelect: (Location) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(LocalAppStrings.current.text("location_step_title", "Where are you?"), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(LocalAppStrings.current.text("search_places", "Search places")) },
            placeholder = { Text(LocalAppStrings.current.text("search_india_places", "Search cities and localities across India")) },
        )
        if (query.isBlank()) {
            Text(LocalAppStrings.current.text("major_cities", "Major Indian cities"), color = Color.White.copy(alpha = 0.78f), style = MaterialTheme.typography.labelLarge)
            IndianCityCatalog.cities.forEach { city ->
                TextButton(onClick = { onQueryChange(city.name) }, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "${LocalAppStrings.current.locationName(city.name)} · ${LocalAppStrings.current.regionName(city.region)}",
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.White,
                    )
                }
            }
        }
        if (isSearching) Text(LocalAppStrings.current.text("searching_places", "Searching places…"), color = Color.White.copy(alpha = 0.75f))
        if (searchError != null) Text(searchError, color = Color(0xFFFF8D8D), style = MaterialTheme.typography.bodySmall)
        if (locations.isEmpty() && query.isBlank()) Text(LocalAppStrings.current.text("loading_localities", "Loading localities…"), color = Color.White.copy(alpha = 0.75f))
        if (query.trim().length >= 2 && locations.isEmpty() && !isSearching && searchError == null) {
            Text(
                LocalAppStrings.current.text("location_unavailable", "Sorry, this location is currently unavailable in this version. We’re working on adding it."),
                color = Color.White.copy(alpha = 0.75f),
            )
        }
        locations.forEach { location ->
            val active = location.id == selectedId
            Row(
                Modifier.fillMaxWidth().heightIn(min = 64.dp)
                    .border(1.dp, if (active) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.14f), RoundedCornerShape(10.dp))
                    .background(if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                    .selectable(selected = active, role = Role.RadioButton) { onSelect(location) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(LocalAppStrings.current.locationName(location.name), color = Color.White, fontWeight = FontWeight.SemiBold)
                    location.adminArea?.let { adminArea ->
                        Text(
                            LocalAppStrings.current.regionName(adminArea),
                            color = Color.White.copy(alpha = 0.82f),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                if (location.isCoastal) {
                    Text(LocalAppStrings.current.text("coastal", "Coastal"), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                }
                if (active) {
                    androidx.compose.material3.Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
        Text(LocalAppStrings.current.text("location_switch_tip", "You can switch any time — try a coastal locality like Mahabalipuram."), color = Color.White.copy(alpha = 0.68f), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun AlertsStep(permissionGranted: Boolean, onRequestPermission: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        androidx.compose.material3.Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
        val strings = LocalAppStrings.current
        Text(strings.text("alerts_step_title", "Alerts that matter"), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
        Text(strings.text("alerts_step_description", "Optional: allow notifications now, or turn them on later in Settings."), color = Color.White.copy(alpha = 0.82f))
        if (permissionGranted) Text(strings.text("notifications_allowed", "Notifications are allowed on this device."), color = MaterialTheme.colorScheme.primary)
        else Button(onClick = onRequestPermission, modifier = Modifier.fillMaxWidth()) { Text(strings.text("allow_notifications", "Allow notifications")) }
    }
}

private fun finish(
    catalog: RulesCatalog,
    selected: Set<String>,
    locationId: String,
    location: Location?,
    permissionGranted: Boolean,
    onDone: (OnboardingSummary) -> Unit,
) {
    onDone(
        OnboardingSummary(
            personas = selected.mapNotNull { catalog.personas[it] },
            cardOrder = OnboardingDefaults.defaultCardOrder(selected.toList(), catalog),
            selectedLocationId = locationId,
            notificationPermissionGranted = permissionGranted,
            selectedLocation = location,
        ),
    )
}

private fun personaDescription(id: String): String = when (id) {
    "health_conscious" -> "AQI, pollen, UV"
    "outdoor_fitness" -> "Best run time, heat, wind"
    "beach_surfer" -> "Waves, tides, sea temperature"
    "traveler" -> "Fog, rain, packing tips"
    "parent_family" -> "School commute, clothes drying"
    "agriculture_gardener" -> "Soil moisture and irrigation"
    "commuter" -> "Rain, fog and route weather"
    "event_planner" -> "Comfort, rain and event days"
    else -> "Weather advice for your routine"
}