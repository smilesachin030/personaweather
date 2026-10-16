package com.example.personaweather.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BeachAccess
import androidx.compose.material.icons.outlined.Commute
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FamilyRestroom
import androidx.compose.material.icons.outlined.FlightTakeoff
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.Yard
import androidx.compose.ui.graphics.vector.ImageVector

fun personaIcon(id: String): ImageVector = when (id) {
    "health_conscious" -> Icons.Outlined.FavoriteBorder
    "outdoor_fitness" -> Icons.Outlined.DirectionsRun
    "beach_surfer" -> Icons.Outlined.BeachAccess
    "traveler" -> Icons.Outlined.FlightTakeoff
    "parent_family" -> Icons.Outlined.FamilyRestroom
    "agriculture_gardener" -> Icons.Outlined.Yard
    "commuter" -> Icons.Outlined.Commute
    "event_planner" -> Icons.Outlined.Event
    else -> Icons.Outlined.WbSunny
}
