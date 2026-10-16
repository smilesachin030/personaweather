package com.example.personaweather.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

object PersonaWeatherPalette {
    val aqi = Color(0xFF57C6B3)
    val pollen = Color(0xFF7DE5A7)
    val uv = Color(0xFFFFC857)
    val humidity = Color(0xFF7CC8FF)
    val run = Color(0xFF6EE7B7)
    val heat = Color(0xFFFF8A65)
    val marine = Color(0xFF67C7FF)
    val alert = Color(0xFFEE6C83)
    val commute = Color(0xFF7DB0FF)
    val agriculture = Color(0xFF4CCB7D)
    val traffic = Color(0xFFF3AD5A)
    val event = Color(0xFF9FA8FF)
    val drying = Color(0xFF7DE5A7)
    val trip = Color(0xFF6AC8F5)
    val outlook = Color(0xFFC9A7FF)
}

val LocalHighContrast = staticCompositionLocalOf { false }

@Composable
fun PersonaWeatherTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val highContrast = LocalHighContrast.current
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = if (highContrast) Color(0xFF9FE8FF) else Color(0xFF8CC8FF),
            secondary = if (highContrast) Color(0xFFFFFF8A) else Color(0xFFB8F0D1),
            background = if (highContrast) Color.Black else Color(0xFF071C2E),
            surface = if (highContrast) Color(0xFF050505) else Color(0xFF102B43),
            surfaceVariant = if (highContrast) Color(0xFF171717) else Color(0xFF163758),
            onPrimary = Color(0xFF071C2E),
            onBackground = Color.White,
            onSurface = Color.White,
            onSurfaceVariant = Color(0xFFF2F2F2),
        )
    } else {
        lightColorScheme(
            primary = if (highContrast) Color(0xFF003B66) else Color(0xFF4673FF),
            secondary = if (highContrast) Color(0xFF00543B) else Color(0xFF60D6A3),
            background = if (highContrast) Color.White else Color(0xFFF4F8FF),
            surface = Color.White,
            surfaceVariant = if (highContrast) Color(0xFFE4E4E4) else Color(0xFFE9F1FF),
            onPrimary = Color.White,
            onBackground = Color.Black,
            onSurface = Color.Black,
            onSurfaceVariant = Color(0xFF202020),
        )
    }

    MaterialTheme(
        colorScheme = scheme,
        content = content,
    )
}