package com.example.personaweather.core.model

data class WeatherAdvice(
    val id: String,
    val personaId: String,
    val title: String,
    val message: String,
    val evidence: String,
    val messageTa: String? = null,
    val messageHi: String? = null,
)
