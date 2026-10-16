package com.example.personaweather.core.model

data class Waypoint(val locationId: String, val label: String, val minutesFromStart: Int)

data class RoutePlace(
    val locationId: String,
    val label: String,
    val minutesFromStart: Int,
    val geocodingQuery: String? = null,
    val expectedAdminArea: String? = null,
)

data class RouteDefinition(
    val id: String,
    val title: String,
    val estimatedDistanceKm: Int,
    val estimatedDurationMinutes: Int,
    val places: List<RoutePlace>,
) {
    val waypoints: List<Waypoint>
        get() = places.map { place ->
            Waypoint(place.locationId, place.label, place.minutesFromStart)
        }
}

object RouteConfig {
    val ROUTES = listOf(
        RouteDefinition(
            id = "chennai_sample",
            title = "Chennai · Home to Office",
            estimatedDistanceKm = 10,
            estimatedDurationMinutes = 60,
            places = listOf(
                RoutePlace("velachery", "Home · Velachery", 0),
                RoutePlace("guindy_alandur", "Guindy / Alandur", 20),
                RoutePlace("t_nagar", "T. Nagar", 45),
                RoutePlace("nungambakkam", "Office · Nungambakkam", 60),
            ),
        ),
        RouteDefinition(
            id = "delhi_ncr_sample",
            title = "Delhi NCR · Commute route",
            estimatedDistanceKm = 42,
            estimatedDurationMinutes = 90,
            places = listOf(
                RoutePlace("delhi_ncr_dwarka", "Dwarka", 0, "Dwarka", "Delhi"),
                RoutePlace("delhi_ncr_gurugram", "Gurugram", 30, "Gurugram", "Haryana"),
                RoutePlace("delhi_ncr_noida", "Noida", 60, "Noida", "Uttar Pradesh"),
                RoutePlace("delhi_ncr_new_delhi", "New Delhi", 90, "New Delhi", "Delhi"),
            ),
        ),
        RouteDefinition(
            id = "mumbai_sample",
            title = "Mumbai · Commute route",
            estimatedDistanceKm = 36,
            estimatedDurationMinutes = 90,
            places = listOf(
                RoutePlace("mumbai_borivali", "Borivali", 0, "Borivali", "Maharashtra"),
                RoutePlace("mumbai_andheri", "Andheri", 30, "Andheri", "Maharashtra"),
                RoutePlace("mumbai_bandra", "Bandra", 60, "Bandra", "Maharashtra"),
                RoutePlace("mumbai_colaba", "Colaba", 90, "Colaba", "Maharashtra"),
            ),
        ),
    )

    val ROUTE: List<Waypoint>
        get() = ROUTES.first().waypoints
}