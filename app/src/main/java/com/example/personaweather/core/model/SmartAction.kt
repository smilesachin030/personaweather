package com.example.personaweather.core.model

data class SmartAction(
    val deviceType: String,
    val title: String,
    val message: String,
    val simulated: Boolean = true,
    val trigger: String,
)

data class SmartDeviceState(
    val deviceType: String? = null,
    val isOn: Boolean = false,
    val label: String = "No device connected",
)
