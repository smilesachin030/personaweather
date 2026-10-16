package com.example.personaweather.notify

import com.example.personaweather.core.model.Advisory
import com.example.personaweather.core.model.Location
import com.example.personaweather.data.UserPrefs
import java.time.LocalDateTime

interface AlertNotifier {
    fun notifyAlerts(location: Location, simTime: LocalDateTime, advisories: List<Advisory>, prefs: UserPrefs)
}