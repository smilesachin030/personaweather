package com.example.personaweather.data

import android.content.Context

class UserPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("persona_weather_prefs", Context.MODE_PRIVATE)

    var onboarded: Boolean
        get() = prefs.getBoolean("onboarded", false)
        set(value) = prefs.edit().putBoolean("onboarded", value).apply()
    var personas: List<String>
        get() = prefs.getString("personas", "").orEmpty().split(',').filter(String::isNotEmpty)
        set(value) = prefs.edit().putString("personas", value.joinToString(",")).apply()
    var locationId: String
        get() = prefs.getString("location_id", "nungambakkam") ?: "nungambakkam"
        set(value) = prefs.edit().putString("location_id", value).apply()
    var cardsOrder: List<String>
        get() = prefs.getString("cards_order", "").orEmpty().split(',').filter(String::isNotEmpty)
        set(value) = prefs.edit().putString("cards_order", value.joinToString(",")).apply()
    var disabledCards: Set<String>
        get() = prefs.getStringSet("cards_disabled", emptySet()).orEmpty().toSet()
        set(value) = prefs.edit().putStringSet("cards_disabled", value.toSet()).apply()
    var cardsCustomized: Boolean
        get() = prefs.getBoolean("cards_customized", false)
        set(value) = prefs.edit().putBoolean("cards_customized", value).apply()
    var language: String
        get() = prefs.getString("language", "en") ?: "en"
        set(value) = prefs.edit().putString("language", value).apply()
    var clothAlarmEnabled: Boolean
        get() = prefs.getBoolean("cloth_alarm_enabled", true)
        set(value) = prefs.edit().putBoolean("cloth_alarm_enabled", value).apply()
    var notificationsEnabled: Boolean
        get() = prefs.getBoolean("notifications_enabled", true)
        set(value) = prefs.edit().putBoolean("notifications_enabled", value).apply()
    var simTime: String?
        get() = prefs.getString("sim_time", null)
        set(value) = prefs.edit().putString("sim_time", value).apply()
}
