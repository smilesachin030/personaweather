package com.example.personaweather.core.engine

import com.example.personaweather.core.model.SmartAction
import com.example.personaweather.core.model.WeatherRow

object SmartActionPolicy {
    fun recommend(row: WeatherRow?): SmartAction? {
        if (row == null) return null

        val heatTrigger = row.heatAlert == 1 || row.temperature >= 34.0
        if (heatTrigger) {
            return SmartAction(
                deviceType = "ac",
                title = "AC suggestion",
                message = "High temperature is forecast for this area. Consider switching the AC on for a cooler room.",
                simulated = true,
                trigger = "temperature >= 34°C or heat alert",
            )
        }

        val nightLightTrigger = (row.hour <= 5 || row.hour >= 18) && row.temperature < 22.0
        if (nightLightTrigger) {
            return SmartAction(
                deviceType = "light",
                title = "Light suggestion",
                message = "It is getting darker and the room is cool. Consider switching a light on for comfort.",
                simulated = true,
                trigger = "nighttime and cool air",
            )
        }

        return null
    }
}
