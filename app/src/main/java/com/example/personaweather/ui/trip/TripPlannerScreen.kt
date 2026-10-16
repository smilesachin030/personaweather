package com.example.personaweather.ui.trip

import android.graphics.Paint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.personaweather.core.engine.Insights
import com.example.personaweather.core.engine.RouteResult
import com.example.personaweather.core.engine.RouteVerdict
import com.example.personaweather.core.engine.Tuning
import com.example.personaweather.core.engine.WaypointWeather
import com.example.personaweather.core.model.Location
import com.example.personaweather.core.model.RouteConfig
import com.example.personaweather.core.model.RouteDefinition
import com.example.personaweather.core.model.Severity
import com.example.personaweather.ui.components.GlassCard
import com.example.personaweather.ui.components.WeatherGlyph
import com.example.personaweather.ui.localization.LocalAppStrings
import com.example.personaweather.ui.theme.PersonaWeatherPalette
import com.example.personaweather.ui.theme.PersonaWeatherTheme
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min

private val tripTimeFormat = DateTimeFormatter.ofPattern("h:mm a")
private val tripDateFormat = DateTimeFormatter.ofPattern("EEE, MMM d")
private val routeGreen = Color(0xFF40C98A)
private val routeAmber = Color(0xFFFFBE55)
private val routeRed = Color(0xFFFF6577)
private val routeBlue = Color(0xFF73C8F4)

@Composable
fun TripPlannerScreen(
    state: TripPlannerUiState,
    onBack: () -> Unit,
    onRouteSelect: (String) -> Unit,
    onDepartureChange: (LocalDateTime) -> Unit,
    onUseSuggestion: () -> Unit,
) {
    BackHandler(onBack = onBack)
    PersonaWeatherTheme(darkTheme = true) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF102D3B), MaterialTheme.colorScheme.background),
                    ),
                ),
        ) {
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PersonaWeatherPalette.trip)
                }
                state.error != null -> Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    PlannerTopBar(onBack)
                    Text(LocalAppStrings.current.text("trip_unavailable", "Trip planner unavailable"), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(Modifier.height(8.dp))
                    Text(state.error, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f))
                }
                else -> Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    PlannerTopBar(onBack)
                    RouteSelector(state.availableRoutes, state.route.id, onRouteSelect)
                    RouteHeader(state.route)
                    state.offlineStatus?.let { status ->
                        GlassCard(
                            title = LocalAppStrings.current.text("offline_trip_pack", "Offline trip forecast"),
                            accent = PersonaWeatherPalette.trip,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                LocalAppStrings.current.text("offline_trip_status", status),
                                color = Color.White.copy(alpha = 0.82f),
                                style = MaterialTheme.typography.bodySmall,
                            )
                            if (state.offlineCoverageStart != null && state.offlineCoverageEnd != null) {
                                val formatter = DateTimeFormatter.ofPattern("EEE, d MMM · h:mm a", LocalAppStrings.current.locale)
                                Text(
                                    LocalAppStrings.current.format(
                                        "offline_trip_coverage",
                                        "Available departure window: %1\$s to %2\$s",
                                        state.offlineCoverageStart.format(formatter),
                                        state.offlineCoverageEnd.format(formatter),
                                    ),
                                    color = Color.White.copy(alpha = 0.72f),
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                    }
                    state.departure?.let { departure ->
                        DepartureSelector(
                            departure = departure,
                            referenceDeparture = state.referenceDeparture ?: departure,
                            isRecalculating = state.isRecalculating,
                            routeDurationMinutes = state.route.estimatedDurationMinutes,
                            onDepartureChange = onDepartureChange,
                        )
                    }
                    state.result?.let { result ->
                        VerdictCard(result, state.suggestion, onUseSuggestion)
                        RouteMapCard(state.locations, result)
                        TimelineCard(result)
                        ExposureCard(result)
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun PlannerTopBar(onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = LocalAppStrings.current.text("back", "Back"), tint = MaterialTheme.colorScheme.onBackground)
        }
        Text(LocalAppStrings.current.text("trip_planner", "Trip planner"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
private fun RouteSelector(
    routes: List<RouteDefinition>,
    selectedRouteId: String,
    onRouteSelect: (String) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        routes.forEach { route ->
            FilterChip(
                selected = route.id == selectedRouteId,
                onClick = { onRouteSelect(route.id) },
                label = { Text(LocalAppStrings.current.routeTitle(route.id, route.title), maxLines = 1) },
            )
        }
    }
}

@Composable
private fun RouteHeader(route: RouteDefinition) {
    GlassCard(title = LocalAppStrings.current.text("sample_route", "ROUTE"), accent = PersonaWeatherPalette.trip, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(LocalAppStrings.current.routeTitle(route.id, route.title), style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
            Text("≈${route.estimatedDistanceKm} km · ~${route.estimatedDurationMinutes} min", color = routeBlue, fontWeight = FontWeight.SemiBold)
        }
        Text("${route.waypoints.size} stops · about ${route.estimatedDurationMinutes} min", color = Color.White.copy(alpha = 0.84f))
        Text(LocalAppStrings.current.text("route_forecast_note", "Preset route stops; weather forecasts are retrieved from Open-Meteo."), color = Color.White.copy(alpha = 0.58f), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun DepartureSelector(
    departure: LocalDateTime,
    referenceDeparture: LocalDateTime,
    isRecalculating: Boolean,
    routeDurationMinutes: Int,
    onDepartureChange: (LocalDateTime) -> Unit,
) {
    val strings = LocalAppStrings.current
    GlassCard(title = LocalAppStrings.current.text("departure", "DEPARTURE"), accent = routeBlue, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val presets = listOf(
                LocalAppStrings.current.text("now", "Now") to referenceDeparture,
                strings.text("preset_thirty_minutes", "+30 min") to referenceDeparture.plusMinutes(30),
                strings.text("preset_one_hour", "+1 h") to referenceDeparture.plusHours(1),
                strings.text("preset_five_pm", "5 PM") to referenceDeparture.withHour(17).withMinute(0),
            )
            presets.forEach { (label, time) ->
                FilterChip(
                    selected = departure == time,
                    onClick = { onDepartureChange(time) },
                    label = { Text(label, maxLines = 1) },
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = { onDepartureChange(departure.minusMinutes(15)) }) {
                Icon(Icons.Default.Remove, contentDescription = strings.text("hour_earlier", "15 minutes earlier"), tint = Color.White)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(departure.format(tripTimeFormat), color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(departure.format(tripDateFormat.withLocale(strings.locale)), color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelMedium)
            }
            IconButton(onClick = { onDepartureChange(departure.plusMinutes(15)) }) {
                Icon(Icons.Default.Add, contentDescription = strings.text("hour_later", "15 minutes later"), tint = Color.White)
            }
        }
        Text(strings.format("route_arrival", "Arrive ≈ %1\$s", departure.plusMinutes(routeDurationMinutes.toLong()).format(tripTimeFormat.withLocale(strings.locale))), color = Color.White.copy(alpha = 0.78f))
        if (isRecalculating) LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = routeBlue)
    }
}

@Composable
private fun VerdictCard(
    result: RouteResult,
    suggestion: com.example.personaweather.core.engine.DepartureSuggestion?,
    onUseSuggestion: () -> Unit,
) {
    val strings = LocalAppStrings.current
    val verdictColor = verdictColor(result.verdict)
    GlassCard(title = LocalAppStrings.current.text("route_outlook", "ROUTE OUTLOOK"), accent = verdictColor, modifier = Modifier.fillMaxWidth()) {
        Surface(
            color = Color(0xFF102B43),
            shape = RoundedCornerShape(999.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, verdictColor.copy(alpha = 0.72f)),
        ) {
            Text(
                strings.text("route_verdict_${result.verdict.name.lowercase()}", result.verdict.name.lowercase().replaceFirstChar { it.uppercase() }),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(routeSummary(result, strings), color = Color.White, style = MaterialTheme.typography.bodyLarge)
        suggestion?.let {
            Surface(color = Color.White.copy(alpha = 0.08f), shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(strings.format("route_leave_at", "Leave at %1\$s", it.departure.format(tripTimeFormat.withLocale(strings.locale))), color = routeGreen, fontWeight = FontWeight.SemiBold)
                    Text(suggestionDescription(it.route, strings), color = Color.White.copy(alpha = 0.78f), style = MaterialTheme.typography.bodySmall)
                    Button(onClick = onUseSuggestion) { Text(LocalAppStrings.current.text("use_this_time", "Use this time")) }
                }
            }
        }
    }
}

@Composable
private fun RouteMapCard(locations: List<Location>, result: RouteResult) {
    GlassCard(title = LocalAppStrings.current.text("route_map", "ROUTE MAP"), accent = PersonaWeatherPalette.trip, modifier = Modifier.fillMaxWidth()) {
        RouteMap(locations, result)
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            LegendDot(routeGreen, LocalAppStrings.current.text("route_verdict_clear", "Clear"))
            LegendDot(routeAmber, LocalAppStrings.current.text("route_verdict_caution", "Caution"))
            LegendDot(routeRed, LocalAppStrings.current.text("route_verdict_alert", "Alert"))
        }
    }
}

@Composable
private fun RouteMap(locations: List<Location>, result: RouteResult) {
    val routeLocations = result.stops.mapNotNull { stop -> locations.firstOrNull { it.id == stop.waypoint.locationId } }
    if (routeLocations.size != result.stops.size) {
        Text(LocalAppStrings.current.text("route_locations_missing", "Route locations are not available in the local dataset."), color = Color.White.copy(alpha = 0.75f))
        return
    }
    val strings = LocalAppStrings.current
    val routeDescription = result.stops.joinToString(" → ") { stop ->
        val verdict = stopVerdict(stop)
        val verdictText = strings.text("route_verdict_${verdict.name.lowercase()}", verdict.name.lowercase())
        "${strings.routePlaceName(stop.waypoint.label)}: $verdictText"
    }
    Canvas(
        Modifier.fillMaxWidth().height(190.dp).semantics {
            contentDescription = strings.format("route_map_description", "Route stops: %1\$s", routeDescription)
        },
    ) {
        val meanLatitude = routeLocations.map { it.lat }.average() * Math.PI / 180.0
        val xScale = cos(meanLatitude).toFloat()
        val minLatitude = routeLocations.minOf { it.lat } - 1.0
        val maxLatitude = routeLocations.maxOf { it.lat } + 1.0
        val minLongitude = routeLocations.minOf { it.lon } - 1.0
        val maxLongitude = routeLocations.maxOf { it.lon } + 1.0
        val allLocations = locations.filter { location ->
            location.lat in minLatitude..maxLatitude && location.lon in minLongitude..maxLongitude
        }.let { nearby -> (nearby + routeLocations).distinctBy { it.id } }
        val projected = allLocations.associate { it.id to Offset((it.lon * xScale).toFloat(), it.lat.toFloat()) }
        val minX = projected.values.minOf { it.x }
        val maxX = projected.values.maxOf { it.x }
        val minY = projected.values.minOf { it.y }
        val maxY = projected.values.maxOf { it.y }
        val padX = 36.dp.toPx()
        val padY = 28.dp.toPx()
        val spanX = max(0.001f, maxX - minX)
        val spanY = max(0.001f, maxY - minY)
        val scale = min((size.width - 2 * padX) / spanX, (size.height - 2 * padY) / spanY)
        val mapWidth = spanX * scale
        val mapHeight = spanY * scale
        val left = (size.width - mapWidth) / 2f
        val top = (size.height - mapHeight) / 2f
        fun screen(location: Location): Offset {
            val point = projected.getValue(location.id)
            return Offset(left + (point.x - minX) * scale, top + (maxY - point.y) * scale)
        }

        allLocations.filter { location -> result.stops.none { it.waypoint.locationId == location.id } }.forEach { location ->
            drawCircle(Color.White.copy(alpha = 0.24f), radius = 2.5.dp.toPx(), center = screen(location))
        }
        val routePath = Path().apply {
            moveTo(screen(routeLocations.first()).x, screen(routeLocations.first()).y)
            routeLocations.drop(1).forEach { location -> lineTo(screen(location).x, screen(location).y) }
        }
        drawPath(routePath, Color.White.copy(alpha = 0.62f), style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round))

        result.stops.forEachIndexed { index, stop ->
            val point = screen(routeLocations[index])
            val color = verdictColor(stopVerdict(stop))
            drawCircle(Color.Black.copy(alpha = 0.25f), 10.dp.toPx(), Offset(point.x, point.y + 2.dp.toPx()))
            drawCircle(color, 8.dp.toPx(), point)
            val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = android.graphics.Color.WHITE
                textSize = 11.sp.toPx()
                typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
            }
            val label = strings.locationName(shortStopName(stop.waypoint.label))
            val labelY = point.y + if (index % 2 == 0) 25.dp.toPx() else -12.dp.toPx()
            drawContext.canvas.nativeCanvas.drawText(label, point.x - labelPaint.measureText(label) / 2f, labelY, labelPaint)
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(label, color = Color.White.copy(alpha = 0.76f), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun TimelineCard(result: RouteResult) {
    GlassCard(title = LocalAppStrings.current.text("stop_by_stop", "STOP-BY-STOP"), accent = PersonaWeatherPalette.trip, modifier = Modifier.fillMaxWidth()) {
        result.stops.forEachIndexed { index, stop ->
            StopTimelineRow(stop, isLast = index == result.stops.lastIndex)
        }
    }
}

@Composable
private fun StopTimelineRow(stop: WaypointWeather, isLast: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(16.dp)) {
            Box(Modifier.size(12.dp).clip(CircleShape).background(verdictColor(stopVerdict(stop))))
            if (!isLast) Box(Modifier.width(2.dp).height(96.dp).background(Color.White.copy(alpha = 0.24f)))
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(LocalAppStrings.current.routePlaceName(stop.waypoint.label), color = Color.White, fontWeight = FontWeight.SemiBold)
                Text(stop.eta.format(tripTimeFormat.withLocale(LocalAppStrings.current.locale)), color = routeBlue, fontWeight = FontWeight.Medium)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WeatherGlyph(weatherGlyph(stop.row.condition), tint = Color.White)
                Text("${stop.row.temperature.toInt()}° · ${LocalAppStrings.current.condition(stop.row.condition)}", color = Color.White.copy(alpha = 0.88f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                MetricChip(LocalAppStrings.current.format("route_metric_rain", "Rain %1\$d%%", stop.row.rainChancePercent.toInt()), Color(0xFF267EBD))
                MetricChip(LocalAppStrings.current.format("route_metric_aqi", "AQI %1\$d", stop.row.aqiValue), aqiColor(stop.row.aqiValue))
                if (stop.row.visibilityKm <= 5.0) MetricChip(LocalAppStrings.current.format("route_metric_visibility", "Visibility %1\$d km", stop.row.visibilityKm.toInt()), routeAmber.copy(alpha = 0.4f))
            }
            stop.advisories.forEach { advisory ->
                Text("${LocalAppStrings.current.text("persona_${advisory.personaId}", advisory.personaId.replace('_', ' '))} · ${LocalAppStrings.current.advisory(advisory.message, advisory.messageTa, advisory.messageHi)}", color = verdictColor(if (advisory.severity == Severity.ALERT) RouteVerdict.ALERT else RouteVerdict.CAUTION), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun MetricChip(label: String, color: Color) {
    Surface(color = color.copy(alpha = 0.72f), shape = RoundedCornerShape(6.dp)) {
        Text(label, modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp), color = Color.White, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

@Composable
private fun ExposureCard(result: RouteResult) {
    val averageAqi = result.stops.map { it.row.aqiValue }.average().toInt()
    val wetStops = result.stops.count { it.row.rainChancePercent >= 50.0 }
    GlassCard(title = LocalAppStrings.current.text("route_exposure", "ROUTE EXPOSURE"), accent = PersonaWeatherPalette.aqi, modifier = Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            ExposureMetric("$averageAqi", LocalAppStrings.current.text("average_aqi", "Average AQI"), Modifier.weight(1f))
            ExposureMetric("$wetStops / ${result.stops.size}", LocalAppStrings.current.text("rain_stops", "Stops with rain ≥ 50%"), Modifier.weight(1f))
        }
    }
}

@Composable
private fun ExposureMetric(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(value, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(label, color = Color.White.copy(alpha = 0.74f), style = MaterialTheme.typography.labelMedium)
    }
}

private fun routeSummary(result: RouteResult, strings: com.example.personaweather.ui.localization.AppStrings): String {
    val triggered = result.stops.flatMap { stop -> stop.advisories.map { stop to it } }
        .sortedWith(compareByDescending<Pair<WaypointWeather, com.example.personaweather.core.model.Advisory>> { it.second.severity == Severity.ALERT })
        .take(2)
    if (triggered.isEmpty()) return strings.text("route_summary_empty", "No trip-specific advisories are triggered along this route.")
    return triggered.joinToString(" · ") { (stop, advisory) ->
        val location = strings.locationName(shortStopName(stop.waypoint.label))
        when (advisory.field) {
            "rain_chance_percent" -> strings.format(
                if (stop.row.rainChancePercent >= Tuning.UMBRELLA_CHANCE) "route_summary_pack_umbrella" else "route_summary_pack_raincoat",
                if (stop.row.rainChancePercent >= Tuning.UMBRELLA_CHANCE) {
                    "Rain is likely near %1\$s. Pack an umbrella before setting out."
                } else {
                    "Showers are possible near %1\$s. Pack a raincoat before setting out."
                },
                location,
            )
            "aqi_value" -> strings.format(
                if (stop.row.aqiValue >= Tuning.UNHEALTHY_AQI) "route_summary_air_alert" else "route_summary_air_caution",
                if (stop.row.aqiValue >= Tuning.UNHEALTHY_AQI) {
                    "Limit outdoor exertion near %1\$s; consider moving indoor stops inside."
                } else {
                    "If sensitive to air pollution, reduce outdoor exertion near %1\$s."
                },
                location,
            )
            else -> "${strings.advisory(advisory.message, advisory.messageTa, advisory.messageHi)} · $location"
        }
    }
}

private fun suggestionDescription(result: RouteResult, strings: com.example.personaweather.ui.localization.AppStrings): String {
    val rainIndicated = result.stops.any { Insights.indicatesRain(it.row) }
    val maxAqi = result.stops.maxOf { it.row.aqiValue }
    return listOfNotNull(
        strings.text(
            if (rainIndicated) "route_rain_possible" else "route_better_leave",
            if (rainIndicated) "Rain is possible along the route" else "No rain indicated by the forecast",
        ),
        if (maxAqi < 100) strings.text("route_better_moderate_air", "air moderate") else null,
    ).ifEmpty { listOf(strings.text("route_better_score", "lower route advisory score")) }.joinToString(" · ")
}

private fun stopVerdict(stop: WaypointWeather): RouteVerdict = when {
    stop.advisories.any { it.severity == Severity.ALERT } -> RouteVerdict.ALERT
    stop.advisories.any { it.severity == Severity.CAUTION } -> RouteVerdict.CAUTION
    else -> RouteVerdict.CLEAR
}

private fun verdictColor(verdict: RouteVerdict): Color = when (verdict) {
    RouteVerdict.CLEAR -> routeGreen
    RouteVerdict.CAUTION -> routeAmber
    RouteVerdict.ALERT -> routeRed
}

private fun aqiColor(aqi: Int): Color = when {
    aqi <= 50 -> Color(0xFF2E966C)
    aqi <= 100 -> Color(0xFF6E9E4B)
    aqi <= 150 -> Color(0xFFB57E32)
    else -> Color(0xFFB94F5B)
}

private fun weatherGlyph(condition: String): String = when {
    condition.contains("storm", ignoreCase = true) || condition.contains("thunder", ignoreCase = true) -> "storm"
    condition.contains("rain", ignoreCase = true) || condition.contains("shower", ignoreCase = true) -> "rain"
    condition.contains("fog", ignoreCase = true) || condition.contains("mist", ignoreCase = true) -> "fog"
    condition.contains("cloud", ignoreCase = true) || condition.contains("overcast", ignoreCase = true) -> "cloud"
    else -> "clear"
}

private fun personaLabel(id: String): String = when (id) {
    "health_conscious" -> "Health"
    else -> id.replace('_', ' ').replaceFirstChar { it.uppercase() }
}

private fun shortStopName(label: String): String = label.substringAfter("· ", label)
