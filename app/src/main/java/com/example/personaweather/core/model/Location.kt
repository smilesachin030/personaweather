package com.example.personaweather.core.model

data class Location(
    val id: String,
    val name: String,
    val lat: Double,
    val lon: Double,
    val distanceKmFromHub: Double = 0.0,
    val direction: String = "",
    val character: String = "",
    val isCoastal: Boolean = false,
    val adminArea: String? = null,
    val countryCode: String = "IN",
    val geocodingId: Long? = null,
    val timeZone: String? = null,
)
