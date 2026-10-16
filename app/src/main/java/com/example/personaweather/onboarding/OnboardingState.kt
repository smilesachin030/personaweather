package com.example.personaweather.onboarding

import com.example.personaweather.core.model.Persona
import com.example.personaweather.core.model.Location

sealed interface OnboardingStep {
    data object StepOne : OnboardingStep
    data object StepTwo : OnboardingStep
    data object StepThree : OnboardingStep
    data object StepFour : OnboardingStep
}

data class OnboardingSelection(
    val selectedPersonaIds: Set<String> = emptySet(),
    val selectedLocationId: String? = null,
    val notificationPermissionGranted: Boolean = false,
) {
    fun isValid(): Boolean = selectedPersonaIds.isNotEmpty()
}

data class OnboardingSummary(
    val personas: List<Persona>,
    val cardOrder: List<String>,
    val selectedLocationId: String,
    val notificationPermissionGranted: Boolean,
    val selectedLocation: Location? = null,
)
