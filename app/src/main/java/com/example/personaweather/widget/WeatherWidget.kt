package com.example.personaweather.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.personaweather.MainActivity
import com.example.personaweather.PersonaWeatherApp
import com.example.personaweather.R
import com.example.personaweather.core.model.Severity
import com.example.personaweather.ui.localization.AppStrings
import kotlinx.coroutines.CancellationException

object WidgetIntentExtras {
    const val LOCATION_ID = "com.example.personaweather.widget.LOCATION_ID"
    const val DESTINATION = "com.example.personaweather.widget.DESTINATION"
    const val DESTINATION_ALERTS = "alerts"
    const val WIDGET_LOCATIONS_PREFS = "persona_weather_widget_locations"
}

class WidgetText(private val strings: AppStrings) {
    fun refresh() = strings.text("widget_refresh", "Refresh")
    fun weatherUnavailable() = strings.text("widget_weather_unavailable", "Weather unavailable")
    fun openAppRetry() = strings.text("widget_open_app_retry", "Open PersonaWeather to retry")
    fun alert() = strings.text("widget_alert", "ALERT")
    fun caution() = strings.text("widget_caution", "CAUTION")
    fun aqiLabel() = strings.text("widget_aqi", "AQI")
    fun feelsLikeLabel() = strings.text("widget_feels_like", "feels like")
    fun allClear() = strings.text("widget_all_clear", "All clear · nothing needs attention")
    fun expandHourly() = strings.text("widget_expand_hourly", "Expand for the hourly outlook")
    fun choosePersonas() = strings.text("widget_choose_personas", "Choose one or more personas to see tailored advice")
    fun noAdvice() = strings.text("widget_no_advice", "No active advice right now")
    fun moreSelected(count: Int) = strings.format("widget_more_selected", "+%1\$d more selected", count)
    fun dataSourceLabel(value: String) = strings.dataSourceLabel(value)
    fun severityLabel(severity: Severity) = if (severity == Severity.ALERT) alert() else caution()
    fun locationName(name: String) = strings.locationName(name)
    fun condition(value: String) = strings.condition(value)
    fun term(value: String) = strings.term(value)
}

object PersonaWeatherWidget : GlanceAppWidget() {
    override val sizeMode = androidx.glance.appwidget.SizeMode.Responsive(
        setOf(DpSize(180.dp, 100.dp), DpSize(250.dp, 160.dp), DpSize(320.dp, 220.dp)),
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as PersonaWeatherApp
        val widgetId = androidx.glance.appwidget.GlanceAppWidgetManager(context).getAppWidgetId(id)
        val locationId = WidgetPreferences.location(context, widgetId, app.container.userPrefs.locationId)
        val personaIds = WidgetPreferences.personaIds(context, widgetId, app.container.userPrefs.personas)
        val focus = WidgetPreferences.focus(context, widgetId)
        val snapshot = try {
            WidgetSnapshotUseCase(app).load(locationId, personaIds, focus)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            null
        }

        val widgetText = WidgetText(AppStrings.forCode(app.container.userPrefs.language))
        provideContent { WidgetContent(snapshot, widgetText) }
    }
}

@androidx.compose.runtime.Composable
private fun WidgetContent(snapshot: WidgetSnapshot?, widgetText: WidgetText) {
    val size = LocalSize.current
    val medium = size.width >= 240.dp || size.height >= 160.dp
    val spacious = size.width >= 300.dp && size.height >= 200.dp
    LazyColumn(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(snapshot.backgroundResource()))
            .padding(if (medium) 16.dp else 12.dp),
    ) {
        if (snapshot == null) {
            item {
                Column {
                    Text(widgetText.weatherUnavailable(), style = textStyle(16, FontWeight.Bold))
                    Text(widgetText.openAppRetry(), style = textStyle(12))
                }
            }
        } else {
            item {
                Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
                    Column(modifier = GlanceModifier.clickable(actionRunCallback<OpenWidgetAction>())) {
                        Text(widgetText.locationName(snapshot.location.name), style = textStyle(14, FontWeight.Bold))
                        Text(
                            "${widgetText.dataSourceLabel(snapshot.sourceLabel)} · ${snapshot.observedAt}",
                            style = textStyle(9, alpha = 0.82f),
                            maxLines = 1,
                        )
                    }
                    Text(
                        widgetText.refresh(),
                        modifier = GlanceModifier.clickable(actionRunCallback<RefreshWidgetAction>()).padding(8.dp),
                        style = textStyle(10, FontWeight.Bold),
                    )
                }
            }

            item {
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .background(ColorProvider(Color.Black.copy(alpha = 0.16f)))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.Vertical.CenterVertically,
                ) {
                    Text(snapshot.glyph, style = textStyle(if (spacious) 40 else 32))
                    Spacer(GlanceModifier.width(10.dp))
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                            Text("${snapshot.temperature}°", style = textStyle(if (spacious) 38 else 32, FontWeight.Normal))
                            if (snapshot.advisory != null) {
                                Spacer(GlanceModifier.width(8.dp))
                                Text(
                                    widgetText.severityLabel(snapshot.advisory.severity),
                                    style = textStyle(9, FontWeight.Bold, color = if (snapshot.advisory.severity == Severity.ALERT) Color(0xFFFFAEA5) else Color(0xFFFFD486)),
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                            Text(widgetText.condition(snapshot.condition), style = textStyle(12, FontWeight.Medium), maxLines = 1)
                            Spacer(GlanceModifier.width(8.dp))
                            Text("${widgetText.aqiLabel()} ${snapshot.aqi}", style = textStyle(10, FontWeight.Bold), maxLines = 1)
                        }
                    }
                    Column(horizontalAlignment = Alignment.Horizontal.End) {
                        Text(widgetText.term(snapshot.aqiCategory), style = textStyle(9, FontWeight.Bold, alpha = 0.9f), maxLines = 1)
                        Text("${snapshot.temperature}° ${widgetText.feelsLikeLabel()}", style = textStyle(8, alpha = 0.7f), maxLines = 1)
                    }
                }
            }

            when (snapshot.focus) {
                WidgetFocus.CURRENT -> item {
                    CurrentAdvice(snapshot, medium, widgetText)
                }
                WidgetFocus.HOURLY -> item {
                    HourlyOutlook(snapshot, medium, widgetText)
                }
                WidgetFocus.PERSONAS -> {
                    if (snapshot.personaAdvice.isEmpty()) {
                        item {
                            Text(
                                widgetText.choosePersonas(),
                                modifier = GlanceModifier.padding(top = 8.dp),
                                style = textStyle(11),
                            )
                        }
                    } else {
                        items(snapshot.personaAdvice) { persona ->
                            PersonaAdviceItem(persona, widgetText)
                        }
                    }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun CurrentConditions(snapshot: WidgetSnapshot, spacious: Boolean, widgetText: WidgetText) {
    // Kept for compatibility with any future call sites; the hero section above is the primary visual.
    Row(
        modifier = GlanceModifier.fillMaxWidth().padding(top = 6.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Text(snapshot.glyph, style = textStyle(if (spacious) 34 else 28))
        Spacer(GlanceModifier.width(8.dp))
        Text("${snapshot.temperature}°", style = textStyle(if (spacious) 36 else 30, FontWeight.Normal))
        Spacer(GlanceModifier.width(10.dp))
        Column {
            Text(widgetText.condition(snapshot.condition), style = textStyle(13, FontWeight.Medium), maxLines = 1)
            Text("AQI ${snapshot.aqi} · ${widgetText.term(snapshot.aqiCategory)}", style = textStyle(11, alpha = 0.84f), maxLines = 1)
        }
    }
}

@androidx.compose.runtime.Composable
private fun CurrentAdvice(snapshot: WidgetSnapshot, medium: Boolean, widgetText: WidgetText) {
    Column(modifier = GlanceModifier.fillMaxWidth().padding(top = 8.dp)) {
        snapshot.advisory?.let { AdvisoryLine(it, medium, widgetText) }
            ?: Text(
                widgetText.allClear(),
                modifier = GlanceModifier
                    .background(ColorProvider(Color(0xFF2E9D67).copy(alpha = 0.22f)))
                    .padding(8.dp),
                style = textStyle(12),
            )
    }
}

@androidx.compose.runtime.Composable
private fun HourlyOutlook(snapshot: WidgetSnapshot, medium: Boolean, widgetText: WidgetText) {
    if (!medium) {
        Text(widgetText.expandHourly(), modifier = GlanceModifier.padding(top = 8.dp), style = textStyle(11, alpha = 0.85f))
        return
    }
    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .background(ColorProvider(Color.Black.copy(alpha = 0.12f)))
            .padding(8.dp),
    ) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
            snapshot.nextHours.forEachIndexed { index, hour ->
                Column(modifier = GlanceModifier.width(68.dp), horizontalAlignment = Alignment.Horizontal.CenterHorizontally) {
                    Text(hour.label, style = textStyle(10, alpha = 0.82f))
                    Text(hour.glyph, style = textStyle(19))
                    Text("${hour.temperature}°", style = textStyle(12, FontWeight.Medium))
                    Text("${hour.rainChance}%", style = textStyle(9, color = Color(0xFFD0EBFF)))
                }
                if (index < snapshot.nextHours.lastIndex) Spacer(GlanceModifier.width(3.dp))
            }
        }
        snapshot.advisory?.let { AdvisoryLine(it, true, widgetText) }
    }
}

@androidx.compose.runtime.Composable
private fun PersonaAdviceItem(persona: WidgetPersonaAdvice, widgetText: WidgetText) {
    val advisory = persona.advisory
    val tone = when (advisory?.severity) {
        Severity.ALERT -> Color(0xFFFFAEA5)
        Severity.CAUTION -> Color(0xFFFFD486)
        else -> Color.White
    }
    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .clickable(actionRunCallback<OpenAlertsAction>()),
    ) {
        Text(persona.personaLabel, style = textStyle(11, FontWeight.Bold, alpha = 0.92f), maxLines = 1)
        Text(advisory?.message ?: widgetText.noAdvice(), style = textStyle(11, color = tone), maxLines = 2)
    }
}

@androidx.compose.runtime.Composable
private fun AdvisoryLine(advisory: WidgetAdvisory, medium: Boolean, widgetText: WidgetText) {
    val severityColor = if (advisory.severity == Severity.ALERT) Color(0xFFFFAEA5) else Color(0xFFFFD486)
    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .background(ColorProvider(Color.White.copy(alpha = 0.14f)))
            .clickable(actionRunCallback<OpenAlertsAction>())
            .padding(8.dp),
    ) {
        Text(
            "${widgetText.severityLabel(advisory.severity)} · ${advisory.title}",
            style = textStyle(11, FontWeight.Bold, color = severityColor),
            maxLines = 1,
        )
        Text(advisory.message, style = textStyle(12), maxLines = if (medium) 3 else 2)
    }
}

private fun WidgetSnapshot?.backgroundResource(): Int = when (this?.sky) {
    WidgetSky.STORM -> R.drawable.widget_gradient_storm
    WidgetSky.RAIN -> R.drawable.widget_gradient_rain
    WidgetSky.CLOUDY -> R.drawable.widget_gradient_cloudy
    WidgetSky.HAZE -> R.drawable.widget_gradient_haze
    WidgetSky.NIGHT -> R.drawable.widget_gradient_night
    WidgetSky.SUNSET -> R.drawable.widget_gradient_sunset
    WidgetSky.CLEAR, null -> R.drawable.widget_gradient_clear
}

private fun textStyle(
    size: Int,
    weight: FontWeight = FontWeight.Normal,
    color: Color = Color.White,
    alpha: Float = 1f,
) = TextStyle(
    color = ColorProvider(color.copy(alpha = alpha)),
    fontSize = size.sp,
    fontWeight = weight,
)

private suspend fun launchWidgetDestination(context: Context, glanceId: GlanceId, destination: String? = null) {
    val widgetId = GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
    val app = context.applicationContext as PersonaWeatherApp
    val locationId = WidgetPreferences.location(context, widgetId, app.container.userPrefs.locationId)
    val intent = Intent(context, MainActivity::class.java)
        .putExtra(WidgetIntentExtras.LOCATION_ID, locationId)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    if (destination != null) intent.putExtra(WidgetIntentExtras.DESTINATION, destination)
    context.startActivity(intent)
}

class OpenWidgetAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        launchWidgetDestination(context, glanceId)
    }
}

class OpenAlertsAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        launchWidgetDestination(context, glanceId, WidgetIntentExtras.DESTINATION_ALERTS)
    }
}

class RefreshWidgetAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        PersonaWeatherWidget.updateAll(context)
    }
}