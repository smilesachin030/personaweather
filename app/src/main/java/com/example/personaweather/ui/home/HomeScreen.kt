package com.example.personaweather.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Umbrella
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.personaweather.BuildConfig
import com.example.personaweather.core.engine.Labels
import com.example.personaweather.core.engine.Insights
import com.example.personaweather.core.engine.EventComfortIndex
import com.example.personaweather.core.engine.Tuning
import com.example.personaweather.core.model.DailySummary
import com.example.personaweather.core.model.PollenSpecies
import com.example.personaweather.core.model.WeatherRow
import com.example.personaweather.ui.components.GlassCard
import com.example.personaweather.ui.components.SeverityBanner
import com.example.personaweather.ui.components.SeverityLevel
import com.example.personaweather.ui.components.WeatherGlyph
import com.example.personaweather.ui.localization.LocalAppStrings
import com.example.personaweather.ui.theme.PersonaWeatherPalette
import com.example.personaweather.ui.theme.PersonaWeatherTheme
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    state: HomeUiState,
    onLocationClick: () -> Unit = {},
    onSelectSavedDestination: (String) -> Unit = {},
    onEditCardsClick: () -> Unit = {},
    onCalendarClick: () -> Unit = {},
    onTripPlannerClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onAlertsClick: () -> Unit = {},
    onRetryClick: () -> Unit = {},
    onListen: (String) -> Unit = {},
    onSmartActionConfirm: () -> Unit = {},
    onSmartActionDismiss: () -> Unit = {},
) {
    if (state.error != null) {
        Box(Modifier.fillMaxSize()) {
            Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF102B43)) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(LocalAppStrings.current.text("forecast_unavailable", "Weather data unavailable"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(state.error, color = Color.White.copy(alpha = 0.75f))
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onLocationClick) { Text(LocalAppStrings.current.text("choose_location", "Choose a location")) }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onRetryClick) { Text(LocalAppStrings.current.text("retry", "Retry")) }
                }
            }
        }
        return
    }

    if (state.isLoading && state.currentRow == null) {
        Box(Modifier.fillMaxSize()) {
            Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF102B43)) {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = PersonaWeatherPalette.trip)
                    Spacer(Modifier.height(12.dp))
                    Text(LocalAppStrings.current.text("loading_forecast", "Loading local forecast…"), color = Color.White.copy(alpha = 0.8f))
                    Spacer(Modifier.height(8.dp))
                    Text("Version ${BuildConfig.VERSION_NAME}", color = Color.White.copy(alpha = 0.55f), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        return
    }

    PersonaWeatherTheme(darkTheme = true) {
        val strings = LocalAppStrings.current
        val selectedPersonaLabels = state.selectedPersonas
            .map { strings.personaLabel(it.id, it.label) }
            .joinToString(" · ")
        val gradient = skyGradient(state.currentRow)
        val topColor by animateColorAsState(gradient[0], tween(900), label = "sky-top")
        val middleColor by animateColorAsState(gradient[1], tween(900), label = "sky-middle")
        val bottomColor by animateColorAsState(gradient[2], tween(900), label = "sky-bottom")
        val scrollState = rememberScrollState()
        val density = LocalDensity.current
        val compactHeaderVisible = scrollState.value > with(density) { 150.dp.roundToPx() }
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(topColor, middleColor, bottomColor),
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    TopBar(
                        timeText = state.simTime.format(DateTimeFormatter.ofPattern("EEE dd MMM · h:mm a", LocalAppStrings.current.locale)),
                        locationName = LocalAppStrings.current.locationName(state.location.name),
                        onSettingsClick = onSettingsClick,
                        onCalendarClick = onCalendarClick,
                        onLocationClick = onLocationClick,
                        onEditCardsClick = onEditCardsClick,
                    )
                    Text(
                        text = state.dataSourceNotice ?: LocalAppStrings.current.dataSourceLabel(state.dataSourceLabel),
                        color = Color.White.copy(alpha = 0.72f),
                        style = MaterialTheme.typography.labelSmall,
                    )
                    if (state.currentRow != null) HeroCard(state)

                    if (state.currentRow == null) {
                        GlassCard(
                            title = LocalAppStrings.current.text("forecast_unavailable", "Forecast unavailable"),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                LocalAppStrings.current.text("current_conditions_missing", "Current forecast unavailable."),
                                color = Color.White.copy(alpha = 0.88f),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = onRetryClick) {
                                    Text(LocalAppStrings.current.text("retry", "Retry"))
                                }
                                TextButton(onClick = onLocationClick) {
                                    Text(LocalAppStrings.current.text("choose_location", "Choose a location"))
                                }
                            }
                        }
                    } else {
                        if (state.advisories.isNotEmpty()) {
                            Text(
                                LocalAppStrings.current.text("top_advice", "What matters now"),
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.semantics { heading() },
                            )
                            AdvisoryBlock(state.advisories, onAlertsClick, onListen)
                        } else {
                            SeverityBanner(text = LocalAppStrings.current.text("all_clear", "All clear — nothing needs your attention right now"), level = SeverityLevel.LOW)
                        }

                        SmartActionCard(state = state, onConfirm = onSmartActionConfirm, onDismiss = onSmartActionDismiss)

                        if (state.smartDeviceState.isOn) {
                            SeverityBanner(text = state.smartDeviceState.label, level = SeverityLevel.LOW)
                        }

                        GlassCard(
                            title = LocalAppStrings.current.text("today", "Today"),
                            accent = PersonaWeatherPalette.alert,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(LocalAppStrings.current.rainSentence(state.rainSentence), color = Color.White.copy(alpha = 0.9f))
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                val headline = LocalAppStrings.current.text("headline:${state.headline}", state.headline)
                                Text(headline, modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.75f))
                                IconButton(onClick = { onListen(headline) }) {
                                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = LocalAppStrings.current.text("listen", "Listen"), tint = Color.White)
                                }
                            }
                        }

                        HourlyStrip(state)
                        MetricChart(state)

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                LocalAppStrings.current.text("for_you", "For you"),
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.semantics { heading() },
                            )
                            if (state.selectedPersonas.isNotEmpty()) {
                                Text(
                                    strings.format(
                                        "personalized_for",
                                        "Prioritized for %1\$s",
                                        selectedPersonaLabels,
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.78f),
                                )
                            }
                        }
                        CardGrid(state, onTripPlannerClick, onSelectSavedDestination)
                        DailyForecast(state.dailySummaries, true)
                        Text(LocalAppStrings.current.text("dataset_attribution", "Weather forecast: Open-Meteo · Air quality: CAMS"), color = Color.White.copy(alpha = 0.46f), style = MaterialTheme.typography.labelSmall)
                    }
                }
                AnimatedVisibility(
                    visible = compactHeaderVisible && state.currentRow != null,
                    modifier = Modifier.align(Alignment.TopCenter).padding(horizontal = 16.dp),
                    enter = fadeIn(tween(180)) + slideInVertically(tween(180)) { -it / 2 },
                    exit = fadeOut(tween(120)) + slideOutVertically(tween(120)) { -it / 2 },
                ) {
                    Surface(
                        color = Color(0xF2102B43),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            WeatherGlyph(icon = weatherGlyph(state.currentRow), modifier = Modifier.size(34.dp), tint = Color.White)
                            Column(Modifier.weight(1f)) {
                                Text(LocalAppStrings.current.locationName(state.location.name), color = Color.White, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                                Text(state.simTime.format(DateTimeFormatter.ofPattern("EEE d MMM · h:mm a", LocalAppStrings.current.locale)), color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.labelSmall, maxLines = 1)
                            }
                            state.currentRow?.let { row ->
                                Text("${row.temperature.toInt()}°", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                WeatherGlyph(icon = weatherGlyph(row), modifier = Modifier.size(28.dp), tint = Color.White)
                            }
                        }
                    }
                }
                if (state.currentRow != null) WeatherBuddyFloatingEntry(state)
            }
        }
    }
}

@Composable
private fun TopBar(
    timeText: String,
    locationName: String,
    onSettingsClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onLocationClick: () -> Unit,
    onEditCardsClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Row(
                Modifier.weight(1f).heightIn(min = 48.dp).border(1.dp, Color.White.copy(alpha = 0.24f), RoundedCornerShape(12.dp))
                    .background(Color(0xFF102B43).copy(alpha = 0.94f), RoundedCornerShape(12.dp))
                    .clickable(role = Role.Button) { onLocationClick() }.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = PersonaWeatherPalette.trip)
                Text(locationName, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = onCalendarClick, modifier = Modifier.size(48.dp).background(Color(0xFF102B43), RoundedCornerShape(12.dp))) {
                Icon(Icons.Default.Event, contentDescription = LocalAppStrings.current.text("upcoming_events", "Upcoming events"), tint = Color.White)
            }
            IconButton(onClick = onSettingsClick, modifier = Modifier.size(48.dp).background(Color(0xFF102B43), RoundedCornerShape(12.dp))) {
                Icon(Icons.Default.Settings, contentDescription = LocalAppStrings.current.text("settings", "Settings"), tint = Color.White)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Row(
                Modifier.weight(1f).heightIn(min = 48.dp).border(1.dp, Color.White.copy(alpha = 0.24f), RoundedCornerShape(12.dp))
                    .background(Color(0xFF102B43).copy(alpha = 0.94f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Default.AccessTime, contentDescription = null, tint = Color.White)
                Text(timeText, color = Color.White, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = onEditCardsClick, modifier = Modifier.size(48.dp).background(Color(0xFF102B43), RoundedCornerShape(12.dp))) {
                Icon(Icons.Default.Edit, contentDescription = LocalAppStrings.current.text("edit_cards", "Edit Home cards"), tint = Color.White)
            }
        }
    }
}

@Composable
private fun SmartActionCard(
    state: HomeUiState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val smartAction = state.smartAction ?: return
    val strings = LocalAppStrings.current
    val actionLabel = when (smartAction.deviceType.lowercase()) {
        "ac" -> "AC"
        "light" -> strings.text("smart_action_light_device", "Light")
        else -> strings.text("smart_action_device", "Device")
    }
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        accent = PersonaWeatherPalette.alert,
        title = strings.text("smart_home", "Smart home"),
    ) {
        Text(text = strings.smartActionTitle(smartAction.title), color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = strings.smartActionMessage(smartAction.message), color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodyMedium)
        Text(
            text = strings.text("smart_action_demo_notice", "Demo only • no real device is connected"),
            color = Color.White.copy(alpha = 0.7f),
            style = MaterialTheme.typography.labelSmall,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onConfirm) {
                Text(
                    when (smartAction.deviceType.lowercase()) {
                        "ac" -> strings.text("smart_action_turn_on_ac", "Turn on AC")
                        "light" -> strings.text("smart_action_turn_on_light", "Turn on Light")
                        else -> strings.format("smart_action_turn_on_device", "Turn on %1\$s", actionLabel)
                    },
                )
            }
            TextButton(onClick = onDismiss) { Text(strings.text("smart_action_dismiss", "Dismiss")) }
        }
    }
}

@Composable
private fun HeroCard(state: HomeUiState) {
    val row = state.currentRow
    val todayRows = state.weatherRows.filter { it.date == state.simTime.toLocalDate().toString() }
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        accent = PersonaWeatherPalette.trip,
        title = LocalAppStrings.current.text("now", "Now"),
    ) {
        if (row == null) {
            Text(
                LocalAppStrings.current.text("current_conditions_missing", "Current forecast unavailable."),
                color = Color.White.copy(alpha = 0.88f),
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text("${row.temperature.toInt()}°", style = MaterialTheme.typography.displaySmall, color = Color.White, fontWeight = FontWeight.Bold)
                    Text(LocalAppStrings.current.condition(Labels.condition(row.condition)), color = Color.White.copy(alpha = 0.88f), style = MaterialTheme.typography.bodyLarge)
                }
                WeatherGlyph(icon = weatherGlyph(row), tint = Color.White)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SeverityBanner(
                    text = LocalAppStrings.current.term(row.aqiCategory),
                    level = when {
                        row.aqiValue >= 150 -> SeverityLevel.CRITICAL
                        row.aqiValue >= 100 -> SeverityLevel.HIGH
                        else -> SeverityLevel.LOW
                    },
                )
                Text(LocalAppStrings.current.format("aqi_value", "AQI %1\$d", row.aqiValue), color = Color.White.copy(alpha = 0.9f))
            }
            val high = todayRows.maxOfOrNull { it.temperature }?.toInt()
            val low = todayRows.minOfOrNull { it.temperature }?.toInt()
            val feelsLike = row.feelsLike.toInt()
            Text(
                buildList {
                    if (high != null && low != null) add("H:$high°  L:$low°")
                    add(LocalAppStrings.current.format("feels_like", "Feels like %1\$d°", feelsLike))
                }.joinToString(" · "),
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun AdvisoryBlock(
    advisories: List<com.example.personaweather.core.model.Advisory>,
    onAlertsClick: () -> Unit,
    onListen: (String) -> Unit,
) {
    val strings = LocalAppStrings.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        advisories.take(1).forEachIndexed { index, advisory ->
            val level = when (advisory.severity) {
                com.example.personaweather.core.model.Severity.ALERT -> SeverityLevel.CRITICAL
                com.example.personaweather.core.model.Severity.CAUTION -> SeverityLevel.MODERATE
            }
            val message = strings.advisory(advisory.message, advisory.messageTa, advisory.messageHi)
            val severityLabel = strings.text(
                if (advisory.severity == com.example.personaweather.core.model.Severity.ALERT) "severity_alert" else "severity_caution",
                if (advisory.severity == com.example.personaweather.core.model.Severity.ALERT) "Alert" else "Caution",
            )
            val personaLabel = strings.text("persona_${advisory.personaId}", advisory.personaId.replace('_', ' '))
            val accessibilityDescription = "$severityLabel, ${advisoryFieldLabel(advisory.field, strings)}, $message, $personaLabel"
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(240, delayMillis = (index * 40).coerceAtMost(120))) +
                    expandVertically(tween(240, delayMillis = (index * 40).coerceAtMost(120))),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    SeverityBanner(text = message, level = level, modifier = Modifier.weight(1f), accessibilityDescription = accessibilityDescription)
                    IconButton(onClick = { onListen(message) }) {
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = strings.text("listen", "Listen"), tint = Color.White)
                    }
                }
            }
        }
        if (advisories.size > 1) {
            TextButton(onClick = onAlertsClick) {
                Text(strings.text("see_all_alerts", "See all %d alerts").replace("%d", advisories.size.toString()))
            }
        }
    }
}

private fun advisoryFieldLabel(field: String, strings: com.example.personaweather.ui.localization.AppStrings): String = when {
    field == "aqi_value" -> strings.text("card_aqi_card", "Air quality")
    field.startsWith("pollen_") -> strings.text("card_pollen_card", "Pollen")
    field == "uv_index" -> strings.text("card_uv_card", "UV")
    field == "heat_alert" -> strings.text("card_heat_card", "Heat")
    field == "wind_speed" -> strings.text("card_running_window_card", "Wind")
    field == "wave_height_m" -> strings.text("card_marine_card", "Sea")
    field == "visibility_km" || field == "fog_flag" -> strings.text("card_travel_alerts_card", "Visibility")
    field == "rain_chance_percent" || field == "rainfall_mm" -> strings.text("rain", "Rain")
    field == "soil_moisture_percent" -> strings.text("card_agriculture_card", "Soil")
    field == "comfort_index" -> strings.text("card_event_planning_card", "Comfort")
    else -> field.replace('_', ' ')
}

@Composable
private fun HourlyStrip(state: HomeUiState) {
    val rows = state.hourlyRows
    if (rows.isEmpty()) return
    val strings = LocalAppStrings.current
    GlassCard(
        title = strings.text("hourly", "Hourly"),
        accent = PersonaWeatherPalette.trip,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            rows.forEachIndexed { index, row ->
                Column(
                    modifier = Modifier
                        .width(54.dp)
                        .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(12.dp))
                        .semantics(mergeDescendants = true) {
                            contentDescription = buildString {
                                append(
                                    strings.format(
                                        "hourly_forecast_accessibility",
                                        "%1\$s: %2\$d degrees, %3\$s, %4\$d percent chance of rain",
                                        if (index == 0) strings.text("now", "Now")
                                        else row.time.format(DateTimeFormatter.ofPattern("h a", strings.locale)),
                                        row.temperature.toInt(),
                                        strings.condition(Labels.condition(row.condition)),
                                        row.rainChancePercent.toInt(),
                                    ),
                                )
                                if (row.hour == 6) append(", ${strings.text("sunrise", "Sunrise")}")
                                else if (row.hour == 18) append(", ${strings.text("sunset", "Sunset")}")
                            }
                        }
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(if (index == 0) strings.text("now", "Now") else row.time.format(DateTimeFormatter.ofPattern("ha", strings.locale)), color = Color.White.copy(alpha = 0.75f), fontSize = 10.sp)
                    if (row.hour == 6) Text(strings.text("sunrise", "Sunrise"), color = PersonaWeatherPalette.uv, fontSize = 8.sp, maxLines = 1)
                    else if (row.hour == 18) Text(strings.text("sunset", "Sunset"), color = PersonaWeatherPalette.uv, fontSize = 8.sp, maxLines = 1)
                    Text("${row.temperature.toInt()}°", color = Color.White, fontWeight = FontWeight.Bold)
                    WeatherGlyph(icon = weatherGlyph(row), modifier = Modifier.size(24.dp), tint = Color.White)
                    if (row.rainChancePercent >= 20.0) Text("${row.rainChancePercent.toInt()}%", color = PersonaWeatherPalette.humidity, fontSize = 10.sp)
                }
            }
        }
    }
}

private enum class ChartMetric(val labelKey: String, val label: String, val color: Color) {
    TEMPERATURE("metric_temperature", "Temp", Color(0xFFFF9D6C)),
    RAIN("metric_rain", "Rain", Color(0xFF68BFFF)),
    WIND("metric_wind", "Wind", Color(0xFF89D7B2)),
    AQI("metric_aqi", "AQI", Color(0xFFFFC857)),
}

@Composable
private fun MetricChart(state: HomeUiState) {
    var selected by remember { mutableStateOf(ChartMetric.TEMPERATURE) }
    val rows = state.hourlyRows
    if (rows.isEmpty()) return
    val strings = LocalAppStrings.current
    val selectedLabel = strings.text(selected.labelKey, selected.label)
    GlassCard(title = LocalAppStrings.current.text("next_24_hours", "Next 24 hours"), accent = selected.color, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChartMetric.values().forEach { metric ->
                FilterChip(
                    selected = metric == selected,
                    onClick = { selected = metric },
                    label = { Text(LocalAppStrings.current.text(metric.labelKey, metric.label)) },
                )
            }
        }
        val values = rows.map { row ->
            when (selected) {
                ChartMetric.TEMPERATURE -> row.temperature
                ChartMetric.RAIN -> row.rainChancePercent
                ChartMetric.WIND -> row.windSpeed
                ChartMetric.AQI -> row.aqiValue.toDouble()
            }
        }
        val minimum = if (selected == ChartMetric.RAIN) 0.0 else values.minOrNull() ?: 0.0
        val maximum = if (selected == ChartMetric.RAIN) 100.0 else values.maxOrNull() ?: 1.0
        val spread = (maximum - minimum).takeIf { it > 0.0 } ?: 1.0
        val unit = when (selected) {
            ChartMetric.TEMPERATURE -> "°C"
            ChartMetric.RAIN -> "%"
            ChartMetric.WIND -> " km/h"
            ChartMetric.AQI -> " AQI"
        }
        Canvas(
            Modifier.fillMaxWidth().height(118.dp).semantics {
                contentDescription = strings.format(
                    "forecast_chart_accessibility",
                    "%1\$s forecast values: %2\$s",
                    selectedLabel,
                    rows.mapIndexed { index, row ->
                        val time = if (index == 0) strings.text("now", "Now")
                        else row.time.format(DateTimeFormatter.ofPattern("h a", strings.locale))
                        "$time ${values[index].toInt()}$unit"
                    }.joinToString(", "),
                )
            },
        ) {
            repeat(3) { line ->
                val y = size.height * line / 2f
                drawLine(Color.White.copy(alpha = 0.12f), Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
            }
            val points = values.mapIndexed { index, value ->
                Offset(
                    x = size.width * index / (values.size - 1).coerceAtLeast(1),
                    y = size.height - ((value - minimum) / spread).toFloat() * (size.height - 12.dp.toPx()) - 6.dp.toPx(),
                )
            }
            if (selected == ChartMetric.RAIN) {
                val barWidth = (size.width / (values.size * 1.8f)).coerceAtLeast(2.dp.toPx())
                points.forEach { point ->
                    drawLine(selected.color, Offset(point.x, size.height), point, barWidth, StrokeCap.Round)
                }
            } else if (points.size > 1) {
                val path = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    points.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(path, selected.color, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
                points.forEach { drawCircle(selected.color, 2.5.dp.toPx(), it) }
            }
        }
        Text(strings.format("forecast_summary", "Next %1\$s: %2\$d%3\$s → %4\$d%3\$s", selectedLabel, values.first().toInt(), unit, values.last().toInt()), color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun DailyForecast(summaries: List<DailySummary>, isLive: Boolean) {
    if (summaries.isEmpty()) return
    val strings = LocalAppStrings.current
    val minTemperature = summaries.minOf { it.minTemp }
    val maxTemperature = summaries.maxOf { it.maxTemp }
    val temperatureSpan = (maxTemperature - minTemperature).takeIf { it > 0.0 } ?: 1.0
    val title = if (isLive) strings.text("five_day_forecast", "5-day forecast") else strings.text("daily_forecast", "7-day forecast")
    GlassCard(title = title, accent = PersonaWeatherPalette.outlook, modifier = Modifier.fillMaxWidth()) {
        summaries.forEachIndexed { index, day ->
            val date = LocalDate.parse(day.date)
            val label = if (index == 0) strings.text("daily_today", "Today") else date.format(DateTimeFormatter.ofPattern("EEE", strings.locale))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {
                    contentDescription = strings.format(
                        "daily_forecast_accessibility",
                        "%1\$s: %2\$s, low %3\$d degrees, high %4\$d degrees, rain chance %5\$d percent",
                        label,
                        strings.condition(Labels.condition(day.dominantCondition)),
                        day.minTemp.toInt(),
                        day.maxTemp.toInt(),
                        day.maxRain.toInt(),
                    )
                },
            ) {
                Text(label, color = Color.White, modifier = Modifier.width(42.dp), fontSize = 12.sp)
                WeatherGlyph(icon = when {
                    day.dominantCondition == "Thunderstorm" -> "storm"
                    day.maxRain >= 50.0 -> "rain"
                    day.dominantCondition.contains("cloud", true) -> "cloud"
                    else -> "sun"
                }, modifier = Modifier.size(24.dp), tint = Color.White)
                if (day.maxRain >= 30.0) Text("${day.maxRain.toInt()}%", color = PersonaWeatherPalette.humidity, modifier = Modifier.width(34.dp), fontSize = 11.sp)
                else Spacer(Modifier.width(34.dp))
                Text("${day.minTemp.toInt()}°", color = Color.White.copy(alpha = 0.74f), fontSize = 11.sp)
                BoxWithConstraints(Modifier.weight(1f).height(10.dp).background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(99.dp))) {
                    val start = ((day.minTemp - minTemperature) / temperatureSpan).toFloat().coerceIn(0f, 1f)
                    val end = ((day.maxTemp - minTemperature) / temperatureSpan).toFloat().coerceIn(start, 1f)
                    Box(
                        Modifier.offset(x = maxWidth * start).fillMaxWidth((end - start).coerceAtLeast(0.04f))
                            .height(10.dp).background(Brush.horizontalGradient(listOf(PersonaWeatherPalette.humidity, PersonaWeatherPalette.heat)), RoundedCornerShape(99.dp)),
                    )
                }
                Text("${day.maxTemp.toInt()}°", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                Box(Modifier.size(7.dp).background(aqiColor(day.avgAqi.toInt()), RoundedCornerShape(99.dp)))
            }
        }
    }
}

@Composable
private fun CardGrid(
    state: HomeUiState,
    onTripPlannerClick: () -> Unit,
    onSelectSavedDestination: (String) -> Unit,
) {
    val wide = setOf("aqi_card", "pollen_card", "running_window_card", "marine_card", "travel_alerts_card", "family_commute_card", "agriculture_card", "event_planning_card", "cloth_drying_card", "trip_planner_card", "daily_outlook_card")
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        var pending: Pair<String, Int>? = null
        state.displayCards.forEachIndexed { index, cardId ->
            if (cardId in wide) {
                pending?.let { (single, singleIndex) ->
                    AnimatedHomeCard(single, state, onTripPlannerClick, onSelectSavedDestination, singleIndex)
                    pending = null
                }
                AnimatedHomeCard(cardId, state, onTripPlannerClick, onSelectSavedDestination, index)
            } else if (pending == null) {
                pending = cardId to index
            } else {
                val (firstCard, firstIndex) = pending!!
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Box(Modifier.weight(1f)) { AnimatedHomeCard(firstCard, state, onTripPlannerClick, onSelectSavedDestination, firstIndex) }
                    Box(Modifier.weight(1f)) { AnimatedHomeCard(cardId, state, onTripPlannerClick, onSelectSavedDestination, index) }
                }
                pending = null
            }
        }
        pending?.let { (cardId, index) -> AnimatedHomeCard(cardId, state, onTripPlannerClick, onSelectSavedDestination, index) }
    }
}

@Composable
private fun AnimatedHomeCard(
    cardId: String,
    state: HomeUiState,
    onTripPlannerClick: () -> Unit,
    onSelectSavedDestination: (String) -> Unit,
    index: Int,
) {
    val delay = (index * 40).coerceAtMost(240)
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(260, delayMillis = delay)) +
            slideInVertically(tween(260, delayMillis = delay)) { it / 10 },
    ) {
        HomeCard(cardId, state, onTripPlannerClick, onSelectSavedDestination)
    }
}

private fun aqiColor(value: Int): Color = when {
    value <= 50 -> Color(0xFF40C98A)
    value <= 100 -> Color(0xFF8AB85C)
    value <= 150 -> Color(0xFFFFBE55)
    value <= 200 -> Color(0xFFFF806A)
    else -> Color(0xFFB774AA)
}

@Composable
private fun HomeCard(
    cardId: String,
    state: HomeUiState,
    onTripPlannerClick: () -> Unit,
    onSelectSavedDestination: (String) -> Unit,
) {
    val row = state.currentRow
    val accent = when (cardId) {
        "aqi_card" -> PersonaWeatherPalette.aqi
        "pollen_card" -> PersonaWeatherPalette.pollen
        "uv_card" -> PersonaWeatherPalette.uv
        "humidity_card" -> PersonaWeatherPalette.humidity
        "running_window_card" -> PersonaWeatherPalette.run
        "heat_card" -> PersonaWeatherPalette.heat
        "marine_card" -> PersonaWeatherPalette.marine
        "travel_alerts_card" -> PersonaWeatherPalette.alert
        "family_commute_card" -> PersonaWeatherPalette.commute
        "agriculture_card" -> PersonaWeatherPalette.agriculture
        "commute_traffic_card" -> PersonaWeatherPalette.traffic
        "event_planning_card" -> PersonaWeatherPalette.event
        "cloth_drying_card" -> PersonaWeatherPalette.drying
        "trip_planner_card" -> PersonaWeatherPalette.trip
        else -> PersonaWeatherPalette.outlook
    }

    val cardTitle = when (cardId) {
        "aqi_card" -> LocalAppStrings.current.text("card_aqi_card", "Air Quality")
        "pollen_card" -> LocalAppStrings.current.text("card_pollen_card", "Pollen")
        "uv_card" -> LocalAppStrings.current.text("card_uv_card", "UV Index")
        "humidity_card" -> LocalAppStrings.current.text("card_humidity_card", "Humidity")
        "running_window_card" -> LocalAppStrings.current.text("card_running_window_card", "Best Running")
        "heat_card" -> LocalAppStrings.current.text("card_heat_card", "Heat")
        "marine_card" -> LocalAppStrings.current.text("card_marine_card", "Sea Conditions")
        "travel_alerts_card" -> LocalAppStrings.current.text("card_travel_alerts_card", "Travel")
        "family_commute_card" -> LocalAppStrings.current.text("card_family_commute_card", "Family Commute")
        "agriculture_card" -> LocalAppStrings.current.text("card_agriculture_card", "Soil")
        "commute_traffic_card" -> LocalAppStrings.current.text("card_commute_traffic_card", "Roads")
        "event_planning_card" -> LocalAppStrings.current.text("card_event_planning_card", "Event Outlook")
        "cloth_drying_card" -> LocalAppStrings.current.text("card_cloth_drying_card", "Drying")
        "trip_planner_card" -> LocalAppStrings.current.text("card_trip_planner_card", "Trip")
        else -> LocalAppStrings.current.text("card_outlook_card", "Outlook")
    }
    GlassCard(title = LocalAppStrings.current.text("card_$cardId", cardTitle), accent = accent, modifier = Modifier.fillMaxWidth().then(
        if (cardId in setOf("trip_planner_card", "travel_alerts_card", "commute_traffic_card")) {
            Modifier.clickable(role = Role.Button, onClick = onTripPlannerClick)
        } else Modifier,
    )) {
        when (cardId) {
            "aqi_card" -> AqiCardBody(row)
            "pollen_card" -> PollenCardBody(row, state.hourlyRows)
            "uv_card" -> UvCardBody(row)
            "humidity_card" -> HumidityCardBody(row, state.hourlyRows)
            "running_window_card" -> RunningCardBody(state.weatherRows, state.simTime)
            "heat_card" -> HeatCardBody(row, state.hourlyRows)
            "marine_card" -> MarineCardBody(row, state.weatherRows, state.simTime, state.location.isCoastal)
            "travel_alerts_card" -> TravelCardBody(row, state.weatherRows, state.simTime, state.advisories, state.locations.filter { it.id in state.savedDestinationIds }, onSelectSavedDestination)
            "family_commute_card" -> FamilyCardBody(state.weatherRows, state.simTime)
            "agriculture_card" -> AgricultureCardBody(row, state.weatherRows, state.simTime)
            "commute_traffic_card" -> CommuteCardBody(row, state.weatherRows, state.simTime)
            "event_planning_card" -> EventCardBody(row, state.dailySummaries)
            "cloth_drying_card" -> ClothCardBody(state.weatherRows, state.simTime)
            "trip_planner_card" -> TripCardBody()
            else -> OutlookCardBody(row)
        }
    }
}

@Composable
private fun AqiCardBody(row: WeatherRow?) {
    if (row == null) {
        Text(LocalAppStrings.current.text("unavailable", "Unavailable"), color = Color.White.copy(alpha = 0.8f))
        return
    }
    val aqi = row.aqiValue
    Text("${aqi}", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
    Text(LocalAppStrings.current.term(Labels.aqi(aqi)), color = Color.White.copy(alpha = 0.8f))
    Spacer(modifier = Modifier.height(8.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF46D18C),
                        Color(0xFFFFC857),
                        Color(0xFFFF8A65),
                        Color(0xFFEE6C83),
                        Color(0xFF815DFF),
                    )
                ),
                RoundedCornerShape(999.dp),
            )
    )
}

@Composable
private fun PollenCardBody(row: WeatherRow?, forecast: List<WeatherRow>) {
    val strings = LocalAppStrings.current
    val pollenRows = (forecast.take(Tuning.HOURLY_ITEMS) + listOfNotNull(row)).distinctBy { it.time }
    if (pollenRows.none { it.pollenReadings.hasAnyValue() }) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                strings.text("pollen_forecast_unavailable", "Pollen forecasts aren't available for this location."),
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
            )
        }
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PollenSpecies.values().forEach { species ->
            val current = row?.pollenReadings?.value(species)
            val peak = pollenRows.mapNotNull { pollenRow ->
                pollenRow.pollenReadings.value(species)?.let { value -> pollenRow to value }
            }.maxByOrNull { it.second }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    strings.text("pollen_species_${species.id}", species.fallbackName),
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    current?.let {
                        strings.format("pollen_current", "Now · %1\$s grains/m³", "%.1f".format(strings.locale, it))
                    } ?: strings.text("pollen_current_unavailable", "Current level unavailable"),
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    peak?.let { (peakRow, value) ->
                        strings.format(
                            "pollen_peak",
                            "24h peak · %1\$s grains/m³ around %2\$s",
                            "%.1f".format(strings.locale, value),
                            peakRow.time.format(DateTimeFormatter.ofPattern("h a", strings.locale)),
                        )
                    } ?: strings.text("pollen_peak_unavailable", "No forecast for this pollen type"),
                    color = Color.White.copy(alpha = 0.74f),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun UvCardBody(row: WeatherRow?) {
    if (row == null) {
        Text(LocalAppStrings.current.text("unavailable", "Unavailable"), color = Color.White.copy(alpha = 0.8f))
        return
    }
    val uv = row.uvIndex
    Text("${uv}", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
    Text(LocalAppStrings.current.term(Labels.uv(uv)), color = Color.White.copy(alpha = 0.8f))
}

@Composable
private fun HumidityCardBody(row: WeatherRow?, forecast: List<WeatherRow>) {
    if (row == null) {
        Text(LocalAppStrings.current.text("unavailable", "Unavailable"), color = Color.White.copy(alpha = 0.8f))
        return
    }
    val humidity = row.humidity
    val nearTerm = forecast.take(6).map { it.humidity }
    Text("${humidity.toInt()}%", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
    val strings = LocalAppStrings.current
    Text(strings.format("dew_point_value", "Dew point %1\$d° · %2\$s", row.dewPoint.toInt(), strings.term(Labels.dewPoint(row.dewPoint))), color = Color.White.copy(alpha = 0.8f))
    if (nearTerm.isNotEmpty()) {
        Text(strings.format("next_six_hours", "Next 6h %1\$d–%2\$d%%", nearTerm.min().toInt(), nearTerm.max().toInt()), color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun RunningCardBody(series: List<WeatherRow>, simTime: LocalDateTime) {
    val strings = LocalAppStrings.current
    val nowIndex = series.indexOfFirst { it.time >= simTime }.let { if (it < 0) series.size else it }
    val upcoming = series.drop(nowIndex).take(24)
    val heatAlert = upcoming.firstOrNull { it.heatAlert == 1 }
    when (val window = Insights.bestRunningWindow(series, nowIndex)) {
        is com.example.personaweather.core.engine.RunningWindow.Recommended -> {
            val timeFormat = DateTimeFormatter.ofPattern("h a", LocalAppStrings.current.locale)
            Text("${window.start.format(timeFormat)}–${window.end.format(timeFormat)}", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
            Text(strings.format("running_conditions", "AQI %1\$d · %2\$d° · UV %3\$d", window.aqi, window.temperature.toInt(), window.uv.toInt()), color = Color.White.copy(alpha = 0.8f))
            series.firstOrNull { it.time == window.start }?.let { runRow ->
                Text(strings.format("running_wind", "Wind %1\$d km/h", runRow.windSpeed.toInt()), color = Color.White.copy(alpha = 0.8f))
            }
        }
        com.example.personaweather.core.engine.RunningWindow.NotRecommended ->
            Text(strings.text("run_not_recommended", "No safe running time in this forecast"), style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
        com.example.personaweather.core.engine.RunningWindow.NoClearBestWindow ->
            Text(strings.text("run_no_clear_best", "No clear best running time in this forecast"), style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
        null -> Text(strings.text("run_window_unavailable", "No running window available"), style = MaterialTheme.typography.titleMedium, color = Color.White)
    }
    if (heatAlert != null) {
        Text(
            strings.format("upcoming_heat_alert", "Heat alert around %1\$s · feels like %2\$d°", heatAlert.time.format(DateTimeFormatter.ofPattern("h a", strings.locale)), heatAlert.feelsLike.toInt()),
            color = PersonaWeatherPalette.heat,
            style = MaterialTheme.typography.bodySmall,
        )
    }
    val daylight = series.firstOrNull { it.time.toLocalDate() == simTime.toLocalDate() }
        ?: series.firstOrNull()
    Spacer(Modifier.height(8.dp))
    if (daylight?.sunriseLocalTime == null && daylight?.sunsetLocalTime == null) {
        Text(
            strings.text("sun_times_unavailable", "Sunrise and sunset times unavailable"),
            color = Color.White.copy(alpha = 0.72f),
            style = MaterialTheme.typography.bodySmall,
        )
    } else {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            daylight.sunriseLocalTime?.let { sunrise ->
                Column(Modifier.weight(1f)) {
                    Text(strings.text("sunrise", "Sunrise"), color = PersonaWeatherPalette.uv, style = MaterialTheme.typography.labelMedium)
                    Text(formatSunTime(sunrise, strings.locale), color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
            daylight.sunsetLocalTime?.let { sunset ->
                Column(Modifier.weight(1f)) {
                    Text(strings.text("sunset", "Sunset"), color = PersonaWeatherPalette.uv, style = MaterialTheme.typography.labelMedium)
                    Text(formatSunTime(sunset, strings.locale), color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

private fun formatSunTime(value: String, locale: java.util.Locale): String =
    LocalTime.parse(value).format(DateTimeFormatter.ofPattern("h:mm a", locale))

@Composable
private fun HeatCardBody(row: WeatherRow?, forecast: List<WeatherRow>) {
    if (row == null) {
        Text(LocalAppStrings.current.text("unavailable", "Unavailable"), color = Color.White.copy(alpha = 0.8f))
        return
    }
    val feels = row.feelsLike
    val peak = forecast.maxByOrNull { it.feelsLike }
    Text("${feels.toInt()}°", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
    Text(
        peak?.let { LocalAppStrings.current.format("hottest_around", "Hottest around %1\$s (%2\$d°)", it.time.format(DateTimeFormatter.ofPattern("h a", LocalAppStrings.current.locale)), it.feelsLike.toInt()) } ?: LocalAppStrings.current.text("current_feels_like", "Current feels-like temperature"),
        color = Color.White.copy(alpha = 0.8f),
    )
}

@Composable
private fun MarineCardBody(row: WeatherRow?, series: List<WeatherRow>, simTime: LocalDateTime, isCoastal: Boolean) {
    if (!isCoastal) {
        Text(LocalAppStrings.current.text("inland_marine_unavailable", "Sea conditions aren't available for inland areas."), color = Color.White.copy(alpha = 0.8f))
        return
    }
    if (row == null) {
        Text(LocalAppStrings.current.text("unavailable", "Unavailable"), color = Color.White.copy(alpha = 0.8f))
        return
    }
    val strings = LocalAppStrings.current
    val nowIndex = series.indexOfFirst { it.time >= simTime }.coerceAtLeast(0)
    val events = Insights.tideEvents(series, nowIndex).take(2)
    row.waveHeightM?.let { waveHeight ->
        Text(
            strings.format("wave_height_value", "Wave height · %1$.1f m", waveHeight),
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold,
        )
        Text(strings.term(Labels.waveHeight(waveHeight)), color = Color.White.copy(alpha = 0.8f))
    } ?: Text(
        strings.text("wave_forecast_unavailable", "Wave-height forecast unavailable"),
        color = Color.White.copy(alpha = 0.8f),
    )
    Text(
        strings.format("marine_details", "Water %1\$s° · swell %2\$s s · %3\$s", row.waterTemperature?.toInt()?.toString() ?: "—", row.wavePeriodSec?.toString() ?: "—", row.waveDirection ?: "—"),
        color = Color.White.copy(alpha = 0.8f),
    )
    Text(
        strings.text(
            "tide_forecast_notice",
            "Tide times are hourly forecast estimates, not navigation guidance.",
        ),
        color = Color.White.copy(alpha = 0.68f),
        style = MaterialTheme.typography.bodySmall,
    )
    if (events.isEmpty()) {
        Text(strings.text("tide_times_unavailable", "Tide timings unavailable in this forecast"), color = Color.White.copy(alpha = 0.76f), style = MaterialTheme.typography.bodySmall)
    }
    events.forEach { event ->
        val label = if (event.type == com.example.personaweather.core.engine.TideType.HIGH) strings.text("high_tide", "High tide") else strings.text("low_tide", "Low tide")
        Text("$label · ${event.time.format(DateTimeFormatter.ofPattern("h:mm a", strings.locale))} · ${"%.2f".format(event.height)} m", color = Color.White.copy(alpha = 0.76f), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun TravelCardBody(
    row: WeatherRow?,
    series: List<WeatherRow>,
    simTime: LocalDateTime,
    advisories: List<com.example.personaweather.core.model.Advisory>,
    savedDestinations: List<com.example.personaweather.core.model.Location>,
    onSelectDestination: (String) -> Unit,
) {
    val strings = LocalAppStrings.current
    if (row == null) {
        Text(strings.text("unavailable", "Unavailable"), color = Color.White.copy(alpha = 0.8f))
        return
    }
    val nowIndex = series.indexOfFirst { it.time >= simTime }.coerceAtLeast(0)
    val upcoming = series.drop(nowIndex).take(6)
    val rainPeak = upcoming.maxOfOrNull { it.rainChancePercent } ?: row.rainChancePercent
    Text("${row.visibilityKm.toInt()} km", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
    Text(strings.format("rain_chance_six_hours", "Rain chance next 6h: %1\$d%%", rainPeak.toInt()), color = Color.White.copy(alpha = 0.8f))
    advisories.filter { it.severity == com.example.personaweather.core.model.Severity.ALERT }.take(2).forEach { advisory ->
        Text(strings.format("travel_severe_alert", "Severe weather · %1\$s", advisory.message), color = PersonaWeatherPalette.alert, style = MaterialTheme.typography.bodySmall)
    }
    val tips = Insights.packingTips(series, nowIndex)
    Text(strings.text("packing_suggestions", "Packing suggestions"), color = Color.White, fontWeight = FontWeight.SemiBold)
    if (tips.isEmpty()) {
        Text(strings.text("packing_none", "No extra weather-specific items suggested."), color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            tips.forEach { tip ->
                val (icon, fallback) = when (tip) {
                    "sun_protection" -> Icons.Outlined.WbSunny to "Sunscreen and a hat"
                    "umbrella" -> Icons.Outlined.Umbrella to "Umbrella"
                    "raincoat" -> Icons.Outlined.Checkroom to "Raincoat"
                    "water" -> Icons.Outlined.WaterDrop to "Water bottle"
                    else -> Icons.Outlined.Air to "A well-fitting mask if sensitive to air pollution"
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(icon, contentDescription = null, tint = PersonaWeatherPalette.alert, modifier = Modifier.size(20.dp))
                    Text(strings.text("packing_tip_$tip", fallback), color = Color.White.copy(alpha = 0.86f), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
    Text(strings.text("saved_destinations", "Saved destinations"), color = Color.White, fontWeight = FontWeight.SemiBold)
    if (savedDestinations.isEmpty()) {
        Text(strings.text("no_saved_destinations", "Save localities from the location picker for quick access."), color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.bodySmall)
    } else {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            savedDestinations.forEach { location ->
                FilterChip(selected = false, onClick = { onSelectDestination(location.id) }, label = { Text(LocalAppStrings.current.locationName(location.name), maxLines = 1) })
            }
        }
    }
}

@Composable
private fun FamilyCardBody(series: List<WeatherRow>, simTime: LocalDateTime) {
    val strings = LocalAppStrings.current
    val morningDate = if (simTime.hour < 9) simTime.toLocalDate() else simTime.toLocalDate().plusDays(1)
    val pickupDate = if (simTime.hour < 18) simTime.toLocalDate() else simTime.toLocalDate().plusDays(1)
    val morning = Insights.commuteOutlook(series, morningDate, 7, 9)
    val pickup = Insights.commuteOutlook(series, pickupDate, 16, 18)
    Text(
        morning?.let {
            strings.format(
                "morning_rain",
                "Drop-off · %1\$s · rain chance peaks %2\$s · %3\$d%%",
                morningDate.format(DateTimeFormatter.ofPattern("EEE d", strings.locale)),
                it.peakTime?.format(DateTimeFormatter.ofPattern("h a", strings.locale))
                    ?: strings.text("commute_morning_window", "across the 7–9 AM drop-off window"),
                it.peak.rainChancePercent.toInt(),
            )
        }
            ?: strings.text("morning_forecast_unavailable", "Drop-off forecast unavailable"),
        color = Color.White.copy(alpha = 0.85f),
    )
    morning?.let { outlook ->
        Text(
            commuteGearLabel(strings, outlook.rainGearGuidance),
            color = Color.White.copy(alpha = 0.72f),
            style = MaterialTheme.typography.bodySmall,
        )
        if (outlook.hasRainProbabilityConflict) {
            Text(strings.text("rain_forecast_conflict", "Rain is indicated, but the probability estimate doesn't match."), color = PersonaWeatherPalette.humidity, style = MaterialTheme.typography.bodySmall)
        }
    }
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        pickup?.let {
            strings.format(
                "pickup_rain",
                "Pickup · %1\$s · rain chance peaks %2\$s · %3\$d%%",
                pickupDate.format(DateTimeFormatter.ofPattern("EEE d", strings.locale)),
                it.peakTime?.format(DateTimeFormatter.ofPattern("h a", strings.locale))
                    ?: strings.text("commute_pickup_window", "across the 4–6 PM pickup window"),
                it.peak.rainChancePercent.toInt(),
            )
        }
            ?: strings.text("pickup_forecast_unavailable", "Pickup forecast unavailable"),
        color = Color.White.copy(alpha = 0.85f),
    )
    pickup?.let { outlook ->
        Text(
            commuteGearLabel(strings, outlook.rainGearGuidance),
            color = Color.White.copy(alpha = 0.72f),
            style = MaterialTheme.typography.bodySmall,
        )
        if (outlook.hasRainProbabilityConflict) {
            Text(strings.text("rain_forecast_conflict", "Rain is indicated, but the probability estimate doesn't match."), color = PersonaWeatherPalette.humidity, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun commuteGearLabel(
    strings: com.example.personaweather.ui.localization.AppStrings,
    guidance: com.example.personaweather.core.engine.RainGearGuidance,
): String = when (guidance) {
    com.example.personaweather.core.engine.RainGearGuidance.UMBRELLA ->
        strings.text("commute_pack_umbrella", "Carry an umbrella")
    com.example.personaweather.core.engine.RainGearGuidance.RAINCOAT ->
        strings.text("commute_pack_raincoat", "Carry rain gear")
    com.example.personaweather.core.engine.RainGearGuidance.NONE ->
        strings.text("commute_no_rain_gear", "Rain gear is not indicated by this forecast")
}

@Composable
private fun AgricultureCardBody(row: WeatherRow?, series: List<WeatherRow>, simTime: LocalDateTime) {
    if (row == null) {
        Text(LocalAppStrings.current.text("unavailable", "Unavailable"), color = Color.White.copy(alpha = 0.8f))
        return
    }
    val next24 = series.filter { it.time >= simTime && it.time < simTime.plusHours(24) }
    val nextDayMoisture = series.firstOrNull { it.time.toLocalDate() == simTime.toLocalDate().plusDays(1) }?.soilMoisturePercent
    val rainForecast = next24.sumOf { it.rainfallMm }
    val soil = row.soilMoisturePercent
    Text(LocalAppStrings.current.format("soil_moisture", "Soil %1\$d%%", row.soilMoisturePercent.toInt()), style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
    Text(
        LocalAppStrings.current.format("soil_rain", "Rain forecast next 24h: %1\$d mm", rainForecast.toInt()),
        color = Color.White.copy(alpha = 0.8f),
    )
    nextDayMoisture?.let { Text(LocalAppStrings.current.format("tomorrow_soil", "Tomorrow soil moisture %1\$d%%", it.toInt()), color = Color.White.copy(alpha = 0.8f)) }
    val guidance = when {
        soil <= 25.0 && rainForecast < 2.0 -> "Soil is dry · water before planting"
        rainForecast >= 5.0 -> "Rain expected · hold irrigation"
        else -> "Soil moisture is adequate"
    }
    Text(LocalAppStrings.current.text("planting_guidance", "Field guidance: $guidance"), color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun CommuteCardBody(row: WeatherRow?, series: List<WeatherRow>, simTime: LocalDateTime) {
    val strings = LocalAppStrings.current
    val nowIndex = series.indexOfFirst { it.time >= simTime }.coerceAtLeast(0)
    val nextTwo = series.drop(nowIndex).take(3)
    val referenceRow = nextTwo.firstOrNull() ?: row
    if (referenceRow == null) {
        Text(strings.text("unavailable", "Unavailable"), color = Color.White.copy(alpha = 0.8f))
        return
    }
    val rainPeak = nextTwo.maxOfOrNull { it.rainChancePercent } ?: referenceRow.rainChancePercent
    val visibility = nextTwo.minOfOrNull { it.visibilityKm } ?: referenceRow.visibilityKm
    val roadStatus = when {
        nextTwo.any { it.fogFlag == 1 } || visibility <= 5.0 -> strings.text("poor_visibility", "Poor visibility")
        rainPeak >= 50.0 -> strings.text("wet", "Wet")
        else -> strings.text("road_clear", "No weather disruption")
    }
    Text(strings.format("roads_status", "Road weather: %1\$s", roadStatus), color = Color.White.copy(alpha = 0.9f), fontWeight = FontWeight.SemiBold)
    Text(strings.format("rain_next_two_hours", "Rain in next 2h: max %1\$d%%", rainPeak.toInt()), color = Color.White.copy(alpha = 0.8f))
    Text(strings.format("commute_visibility", "Visibility %1\$d km%2\$s", visibility.toInt(), if (nextTwo.any { it.fogFlag == 1 }) " · fog" else ""), color = Color.White.copy(alpha = 0.8f))
}

@Composable
private fun EventCardBody(row: WeatherRow?, daily: List<DailySummary>) {
    val strings = LocalAppStrings.current
    if (row != null) {
        val comfort = EventComfortIndex.assess(
            rainChancePercent = row.rainChancePercent,
            aqi = row.aqiValue,
            temperatureC = row.temperature,
            feelsLikeC = row.feelsLike,
        )
        Text(
            strings.format("event_comfort_score", "Outdoor comfort %1\$d/100 · %2\$s", comfort.score, strings.term(comfort.label)),
            color = Color.White,
            fontWeight = FontWeight.Bold,
        )
        Text(
            strings.format(
                "event_comfort_breakdown",
                "Deductions: rain -%1\$d · AQI -%2\$d · temperature -%3\$d",
                comfort.rainPenalty,
                comfort.aqiPenalty,
                comfort.temperaturePenalty,
            ),
            color = Color.White.copy(alpha = 0.72f),
            style = MaterialTheme.typography.bodySmall,
        )
        val temperatureCondition = when {
            row.temperature < Tuning.EVENT_COMFORT_FREEZING_C -> strings.text("frost_risk", "frost risk")
            row.temperature < Tuning.EVENT_COMFORT_COOL_C -> strings.text("cold_conditions", "cold")
            row.feelsLike >= Tuning.EVENT_COMFORT_HEAT_C -> strings.text("heat_stress", "heat")
            else -> "${row.temperature.toInt()}°C"
        }
        Text(
            strings.format(
                "event_comfort_factors",
                "Rain %1\$d%% · AQI %2\$d · %3\$s",
                row.rainChancePercent.toInt(),
                row.aqiValue,
                temperatureCondition,
            ),
            color = Color.White.copy(alpha = 0.8f),
        )
    } else if (daily.isEmpty()) {
        Text(strings.text("unavailable", "Unavailable"), color = Color.White.copy(alpha = 0.8f))
    }
    daily.minByOrNull { it.eventScore }?.let { best ->
        val date = LocalDate.parse(best.date).format(DateTimeFormatter.ofPattern("EEE d MMM", strings.locale))
        Text(strings.format("best_event_day", "Best outdoor day: %1\$s · rain risk %2\$d%%", date, best.maxRain.toInt()), color = PersonaWeatherPalette.event)
    }
    Text(strings.format("forecast_days_available", "%1\$d-day outlook", daily.size), color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun ClothCardBody(series: List<WeatherRow>, simTime: LocalDateTime) {
    val window = Insights.dryingWindow(series, series.indexOfFirst { it.time >= simTime }.coerceAtLeast(0))
    if (window == null) {
        Text(LocalAppStrings.current.text("no_drying_window", "No 3-hour dry window"), style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
        Text(LocalAppStrings.current.text("try_later_forecast", "Try again later in the forecast"), color = Color.White.copy(alpha = 0.8f))
    } else {
        val timeFormat = DateTimeFormatter.ofPattern("h a", LocalAppStrings.current.locale)
        Text("${window.start.format(timeFormat)}–${window.end.format(timeFormat)}", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
        Text(LocalAppStrings.current.format("drying_humidity", "Drying window · %1\$d%% humidity", window.averageHumidity.toInt()), color = Color.White.copy(alpha = 0.8f))
    }
}

@Composable
private fun TripCardBody() {
    Text(LocalAppStrings.current.text("route_card", "Trip weather"), color = Color.White.copy(alpha = 0.9f))
    Text(LocalAppStrings.current.text("route_card_details", "Check conditions at each stop along your route"), color = Color.White, fontWeight = FontWeight.SemiBold)
    Text(LocalAppStrings.current.text("open_route_forecast", "Open route forecast →"), color = PersonaWeatherPalette.trip, fontWeight = FontWeight.Medium)
}

@Composable
private fun OutlookCardBody(row: WeatherRow?) {
    val strings = LocalAppStrings.current
    if (row == null) {
        Text(strings.text("unavailable", "Unavailable"), color = Color.White.copy(alpha = 0.8f))
        return
    }
    Text(strings.format("best_day", "Best day: %1\$s", row.date), color = Color.White, fontWeight = FontWeight.Bold)
    Text(strings.format("comfort_value", "Comfort %1\$s", strings.term(row.comfortIndex)), color = Color.White.copy(alpha = 0.8f))
}

private fun weatherGlyph(row: WeatherRow?): String = when {
    row == null -> "sun"
    row.condition == "Thunderstorm" -> "storm"
    row.condition == "Showers" || row.rainChancePercent >= 50.0 -> "rain"
    row.fogFlag == 1 -> "fog"
    row.condition.contains("cloud", ignoreCase = true) -> "cloud"
    else -> "sun"
}

private fun skyGradient(row: WeatherRow?): List<Color> = when {
    row == null -> listOf(Color(0xFF183B55), Color(0xFF102B43), Color(0xFF091B2A))
    row.condition == "Thunderstorm" -> listOf(Color(0xFF26303F), Color(0xFF364155), Color(0xFF56607A))
    row.condition == "Showers" -> listOf(Color(0xFF3A4A63), Color(0xFF56677F), Color(0xFF7D8CA0))
    row.fogFlag == 1 -> listOf(Color(0xFF586573), Color(0xFF737F8C), Color(0xFF929EAA))
    row.aqiValue >= 150 -> listOf(Color(0xFF554E49), Color(0xFF746A61), Color(0xFF91877D))
    row.hour >= 19 || row.hour <= 4 -> listOf(Color(0xFF0B1330), Color(0xFF1B2A5C), Color(0xFF2F3F86))
    row.hour in 5..6 || row.hour in 17..18 -> listOf(Color(0xFF3B4A8C), Color(0xFFB4658E), Color(0xFFF2A65A))
    row.condition.startsWith("Scattered") -> listOf(Color(0xFF5D7288), Color(0xFF7F92A6), Color(0xFFA7B4C2))
    row.condition == "Partly cloudy" -> listOf(Color(0xFF4F86C6), Color(0xFF7FA6CC), Color(0xFFB8CBDD))
    else -> listOf(Color(0xFF2E7BE6), Color(0xFF62AEF2), Color(0xFFA8D4FA))
}
