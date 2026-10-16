package com.example.personaweather.data

import com.example.personaweather.core.model.Location

object KnownLocations {
    val all: List<Location> = listOf(
        Location("nungambakkam", "Nungambakkam", 13.08, 80.22, 4.0, "West", "Residential", false),
        Location("velachery", "Velachery", 12.98, 80.22, 14.0, "South", "Residential", false),
        Location("mahabalipuram", "Mahabalipuram", 12.62, 80.17, 55.0, "East", "Coastal / tourist", true),
        Location("guindy_alandur", "Guindy / Alandur", 13.01, 80.21, 12.0, "Central", "IT corridor", false),
        Location("t_nagar", "T. Nagar", 13.04, 80.23, 6.0, "Central", "Commercial", false),
        Location("tambaram", "Tambaram", 12.92, 80.12, 25.0, "South", "Suburban residential", false),
        Location("sriperumbudur", "Sriperumbudur", 12.97, 79.94, 34.0, "West", "Industrial", false),
    )

    fun byId(): Map<String, Location> = all.associateBy(Location::id)
}
