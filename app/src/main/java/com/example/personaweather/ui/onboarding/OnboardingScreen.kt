package com.example.personaweather.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.personaweather.core.model.RulesCatalog
import com.example.personaweather.onboarding.OnboardingDefaults
import com.example.personaweather.onboarding.OnboardingSummary
import com.example.personaweather.ui.localization.LocalAppStrings
import com.example.personaweather.ui.theme.PersonaWeatherTheme

@Composable
fun OnboardingScreen(
    catalog: RulesCatalog,
    selectedPersonas: Set<String> = emptySet(),
    onDone: (OnboardingSummary) -> Unit = {},
) {
    val initialSelection = selectedPersonas.ifEmpty { setOf("health_conscious") }
    var selected by remember { mutableStateOf(initialSelection) }
    val personas = catalog.personas.values.sortedBy { it.label }
    val defaultCards = OnboardingDefaults.defaultCardOrder(selected.toList(), catalog)
    val appStrings = LocalAppStrings.current

    PersonaWeatherTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
                                MaterialTheme.colorScheme.background,
                            )
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Welcome", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text("Choose the personas that match your routine.", style = MaterialTheme.typography.bodyLarge)
                        Text("Selected: ${selected.ifEmpty { "none" }}", color = Color.Gray)
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        personas.forEach { persona ->
                            val isSelected = selected.contains(persona.id)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(14.dp),
                                    )
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                                        else Color.Transparent,
                                        RoundedCornerShape(14.dp),
                                    )
                                    .clickable {
                                        selected = if (isSelected) {
                                            selected - persona.id
                                        } else {
                                            selected + persona.id
                                        }
                                    }
                                    .padding(14.dp),
                            ) {
                                Text(
                                    text = appStrings.personaLabel(persona.id, persona.label),
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Color.White.copy(alpha = 0.08f),
                                RoundedCornerShape(20.dp),
                            )
                            .padding(16.dp),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(appStrings.text("suggested_cards", "Suggested cards"), style = MaterialTheme.typography.labelLarge)
                            defaultCards.forEach { card ->
                                Text("• ${appStrings.cardLabel(card, card.removeSuffix("_card").replace('_', ' '))}", color = MaterialTheme.colorScheme.onBackground)
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        Button(
                            enabled = selected.isNotEmpty(),
                            onClick = {
                                val summary = OnboardingSummary(
                                    personas = selected.mapNotNull { catalog.personas[it] },
                                    cardOrder = defaultCards,
                                    selectedLocationId = "nungambakkam",
                                    notificationPermissionGranted = true,
                                )
                                onDone(summary)
                            }
                        ) {
                            Text("Done")
                        }
                    }
                }
            }
        }
    }
}
