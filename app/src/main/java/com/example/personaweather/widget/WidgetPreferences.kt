package com.example.personaweather.widget

import android.content.Context

enum class WidgetFocus(val label: String) {
    CURRENT("Current conditions"),
    HOURLY("Hourly outlook"),
    PERSONAS("Persona advice"),
}

object WidgetPreferences {
    private const val FILE_NAME = "persona_weather_widget_locations"
    private const val LOCATION_SUFFIX = ".location"
    private const val PERSONAS_SUFFIX = ".personas"
    private const val FOCUS_SUFFIX = ".focus"

    fun location(context: Context, widgetId: Int, fallback: String): String =
        preferences(context).getString(key(widgetId, LOCATION_SUFFIX), null)
            ?: preferences(context).getString(widgetId.toString(), fallback)
            ?: fallback

    fun personaIds(context: Context, widgetId: Int, fallback: List<String>): List<String> =
        preferences(context).getString(key(widgetId, PERSONAS_SUFFIX), null)
            ?.split(',')
            ?.filter(String::isNotBlank)
            ?: fallback

    fun focus(context: Context, widgetId: Int): WidgetFocus =
        preferences(context).getString(key(widgetId, FOCUS_SUFFIX), null)
            ?.let { saved -> WidgetFocus.entries.firstOrNull { it.name == saved } }
            ?: WidgetFocus.PERSONAS

    fun save(context: Context, widgetId: Int, locationId: String, personaIds: List<String>, focus: WidgetFocus) {
        preferences(context).edit()
            .putString(key(widgetId, LOCATION_SUFFIX), locationId)
            .putString(key(widgetId, PERSONAS_SUFFIX), personaIds.distinct().joinToString(","))
            .putString(key(widgetId, FOCUS_SUFFIX), focus.name)
            .remove(widgetId.toString())
            .apply()
    }

    fun remove(context: Context, widgetId: Int) {
        preferences(context).edit()
            .remove(key(widgetId, LOCATION_SUFFIX))
            .remove(key(widgetId, PERSONAS_SUFFIX))
            .remove(key(widgetId, FOCUS_SUFFIX))
            .remove(widgetId.toString())
            .apply()
    }

    private fun preferences(context: Context) =
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    private fun key(widgetId: Int, suffix: String) = "$widgetId$suffix"
}