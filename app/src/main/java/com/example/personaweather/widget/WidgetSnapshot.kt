package com.example.personaweather.widget

import com.example.personaweather.PersonaWeatherApp
import com.example.personaweather.core.engine.AdvisoryEngine
import com.example.personaweather.ui.localization.AppStrings
import com.example.personaweather.core.model.Advisory
import com.example.personaweather.core.model.Location
import com.example.personaweather.core.model.Severity
import com.example.personaweather.core.model.WeatherRow
import com.example.personaweather.data.UserPrefs
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

data class WidgetSnapshot(
    val location: Location,
    val focus: WidgetFocus,
    val observedAt: String,
    val sourceLabel: String,
    val temperature: Int,
    val condition: String,
    val glyph: String,
    val aqi: Int,
    val aqiCategory: String,
    val advisory: WidgetAdvisory?,
    val personaAdvice: List<WidgetPersonaAdvice>,
    val nextHours: List<WidgetHour>,
    val sky: WidgetSky,
)

data class WidgetAdvisory(
    val severity: Severity,
    val title: String,
    val message: String,
)

data class WidgetHour(
    val label: String,
    val temperature: Int,
    val rainChance: Int,
    val glyph: String,
)

data class WidgetPersonaAdvice(
    val personaId: String,
    val personaLabel: String,
    val advisory: WidgetAdvisory?,
)

enum class WidgetSky { CLEAR, CLOUDY, RAIN, STORM, HAZE, NIGHT, SUNSET }

class WidgetSnapshotUseCase(private val app: PersonaWeatherApp) {
    suspend fun load(locationId: String, personaIds: List<String>, focus: WidgetFocus): WidgetSnapshot {
        val container = app.container
        val locations = container.weatherDataSource.locations()
        val location = locations.firstOrNull { it.id == locationId }
            ?: locations.firstOrNull { it.id == container.userPrefs.locationId }
            ?: locations.firstOrNull()
            ?: error("No weather locations are available")
        val rows = container.weatherDataSource.series(location.id)
        check(rows.isNotEmpty()) { "No forecast is available for ${location.name}" }

        val prefs = container.userPrefs
        val appStrings = AppStrings.forCode(prefs.language)
        val requestedTime = LocalDateTime.now(ZoneId.of("Asia/Kolkata"))
        val index = rows.indices.minByOrNull { rowIndex ->
            abs(Duration.between(rows[rowIndex].time, requestedTime).toMinutes())
        } ?: 0
        val current = rows[index]
        check(abs(Duration.between(current.time, requestedTime).toMinutes()) <= MAX_CURRENT_ROW_AGE_MINUTES) {
            "The saved forecast does not cover the current time for ${location.name}."
        }
        val personas = personaIds.mapNotNull(container.rulesCatalog.personas::get)
            .distinctBy { it.id }
        val evaluations = AdvisoryEngine.evaluate(rows, index, personas, location.isCoastal)
        val advisory = evaluations
            .firstOrNull()
            ?.let { it.toWidgetAdvisory(personas.firstOrNull { persona -> persona.id == it.personaId }?.label, prefs.language) }
        val personaAdvice = personas.map { persona ->
            WidgetPersonaAdvice(
                personaId = persona.id,
                personaLabel = appStrings.personaLabel(persona.id, persona.label),
                advisory = evaluations.firstOrNull { it.personaId == persona.id }
                    ?.toWidgetAdvisory(appStrings.personaLabel(persona.id, persona.label), prefs.language),
            )
        }
        val source = container.weatherDataSource.statusLabel(location.id)
        val visibleRows = rows.drop(index).drop(1).take(3)

        return WidgetSnapshot(
            location = location,
            focus = focus,
            observedAt = current.time.format(DateTimeFormatter.ofPattern("EEE, MMM d · h:mm a", appStrings.locale)),
            sourceLabel = source,
            temperature = current.temperature.toInt(),
            condition = current.condition,
            glyph = current.weatherGlyph(),
            aqi = current.aqiValue,
            aqiCategory = current.aqiCategory,
            advisory = advisory,
            personaAdvice = personaAdvice,
            nextHours = visibleRows.map { row ->
                WidgetHour(
                    label = row.time.format(DateTimeFormatter.ofPattern("h a", appStrings.locale)),
                    temperature = row.temperature.toInt(),
                    rainChance = row.rainChancePercent.toInt(),
                    glyph = row.weatherGlyph(),
                )
            },
            sky = current.widgetSky(),
        )
    }

    private companion object {
        const val MAX_CURRENT_ROW_AGE_MINUTES = 90L
    }

    private fun Advisory.toWidgetAdvisory(personaLabel: String?, languageCode: String): WidgetAdvisory {
        val fieldTitle = when (field) {
            "aqi_value" -> if (languageCode == AppStrings.HINDI) "वायु गुणवत्ता" else if (languageCode == AppStrings.TAMIL) "காற்றின் தரம்" else "Air quality"
            "rain_chance_percent", "rainfall_mm" -> if (languageCode == AppStrings.HINDI) "बारिश" else if (languageCode == AppStrings.TAMIL) "மழை" else "Rain"
            "wave_height_m" -> if (languageCode == AppStrings.HINDI) "समुद्र की स्थिति" else if (languageCode == AppStrings.TAMIL) "கடல் நிலை" else "Sea conditions"
            "uv_index" -> "UV"
            "fog_flag" -> if (languageCode == AppStrings.HINDI) "कोहरा" else if (languageCode == AppStrings.TAMIL) "மூடுபனி" else "Fog"
            "visibility_km" -> if (languageCode == AppStrings.HINDI) "दृश्यता" else if (languageCode == AppStrings.TAMIL) "பார்வை" else "Visibility"
            "heat_alert" -> if (languageCode == AppStrings.HINDI) "गर्मी" else if (languageCode == AppStrings.TAMIL) "வெப்பம்" else "Heat"
            "wind_speed" -> if (languageCode == AppStrings.HINDI) "हवा" else if (languageCode == AppStrings.TAMIL) "காற்று" else "Wind"
            "comfort_index" -> if (languageCode == AppStrings.HINDI) "आराम" else if (languageCode == AppStrings.TAMIL) "வசதி" else "Comfort"
            else -> field.replace('_', ' ').replaceFirstChar { it.titlecase(Locale.ENGLISH) }
        }
        val localizedMessage = when (languageCode) {
            AppStrings.TAMIL -> messageTa ?: message
            AppStrings.HINDI -> messageHi ?: message
            else -> message
        }
        val persona = personaLabel?.let { " · $it" }.orEmpty()
        return WidgetAdvisory(severity, "$fieldTitle$persona", localizedMessage)
    }

    private fun WeatherRow.weatherGlyph(): String = when {
        fogFlag == 1 -> "🌫️"
        condition == "Thunderstorm" -> "⛈️"
        condition == "Showers" -> "🌧️"
        condition == "Partly cloudy" -> "⛅"
        condition.startsWith("Scattered") && hour in 5..18 -> "🌦️"
        condition.startsWith("Scattered") -> "☁️"
        condition == "Clear" && (hour >= 19 || hour <= 4) -> "🌙"
        else -> "☀️"
    }

    private fun WeatherRow.widgetSky(): WidgetSky = when {
        condition == "Thunderstorm" -> WidgetSky.STORM
        condition == "Showers" -> WidgetSky.RAIN
        fogFlag == 1 -> WidgetSky.HAZE
        hour >= 19 || hour <= 4 -> WidgetSky.NIGHT
        hour in 5..6 || hour in 17..18 -> WidgetSky.SUNSET
        condition == "Partly cloudy" || condition.startsWith("Scattered") -> WidgetSky.CLOUDY
        aqiValue >= 150 -> WidgetSky.HAZE
        else -> WidgetSky.CLEAR
    }
}