package com.example.personaweather.data

import android.content.Context

class AppContainer(context: Context) {
    val userPrefs = UserPrefs(context)
    val calendarEventSource: CalendarEventSource = DeviceCalendarEventSource(context)
    val weatherDataSource: OpenMeteoWeatherDataSource = OpenMeteoWeatherDataSource(
        locationsProvider = { KnownLocations.all + userPrefs.rememberedLocations },
        responseCacheDirectory = context.getDir("open_meteo_cache", Context.MODE_PRIVATE),
    )
    val rulesCatalog: com.example.personaweather.core.model.RulesCatalog = RulesLoader.loadFromAsset(context)
}
