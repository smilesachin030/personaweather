package com.example.personaweather.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.glance.appwidget.updateAll
import com.example.personaweather.PersonaWeatherApp
import com.example.personaweather.ui.localization.AppStrings
import kotlinx.coroutines.launch

class WidgetConfigActivity : ComponentActivity() {
    private val widgetId: Int by lazy {
        intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val app = application as PersonaWeatherApp
        val strings = AppStrings.forCode(app.container.userPrefs.language)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 24, 28, 20)
        }
        val title = TextView(this).apply {
            text = strings.text("widget_personalize_title", "PERSONALIZE THIS WIDGET")
            textSize = 18f
        }
        val subtitle = TextView(this).apply {
            text = strings.text("widget_personalize_subtitle", "Choose its focus, locality, and the personas whose advice should appear.")
            textSize = 14f
            setPadding(0, 8, 0, 16)
        }
        val focusLabel = TextView(this).apply { text = strings.text("widget_focus_type", "WIDGET TYPE"); textSize = 12f }
        val focusSpinner = Spinner(this)
        val locationLabel = TextView(this).apply { text = strings.text("widget_locality", "LOCALITY"); textSize = 12f }
        val locationSpinner = Spinner(this)
        val personaHeading = TextView(this).apply {
            text = strings.text("widget_personas_for_this_widget", "PERSONAS FOR THIS WIDGET")
            textSize = 12f
            setPadding(0, 12, 0, 4)
        }
        val personaList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val personaScroll = ScrollView(this).apply {
            isFillViewport = false
            addView(personaList)
        }
        val save = Button(this).apply { text = strings.text("widget_save", "Save widget"); isEnabled = false }
        val cancel = Button(this).apply { text = strings.text("widget_cancel", "Cancel") }
        root.addView(title, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        root.addView(subtitle, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        root.addView(focusLabel, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        root.addView(focusSpinner, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        root.addView(locationLabel, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        root.addView(locationSpinner, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        root.addView(personaHeading, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        root.addView(personaScroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(save, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        root.addView(cancel, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        setContentView(root)

        var locationIds = emptyList<String>()
        val focusOptions = WidgetFocus.entries
        val personaChecks = linkedMapOf<String, CheckBox>()
        lifecycleScope.launch {
            val locations = runCatching { app.container.weatherDataSource.locations() }.getOrElse {
                Toast.makeText(this@WidgetConfigActivity, strings.text("widget_unable_to_load_localities", "Unable to load localities"), Toast.LENGTH_LONG).show()
                finish()
                return@launch
            }
            locationIds = locations.map { it.id }
            focusSpinner.adapter = ArrayAdapter(
                this@WidgetConfigActivity,
                android.R.layout.simple_spinner_dropdown_item,
                focusOptions.map { strings.widgetFocusLabel(it.name, it.label) },
            )
            val locationAdapter = ArrayAdapter(
                this@WidgetConfigActivity,
                android.R.layout.simple_spinner_dropdown_item,
                locations.map { "${strings.locationName(it.name)} · ${strings.text("live_forecast", it.character)}" },
            )
            locationSpinner.adapter = locationAdapter
            val savedLocation = WidgetPreferences.location(this@WidgetConfigActivity, widgetId, app.container.userPrefs.locationId)
            locationSpinner.setSelection(locations.indexOfFirst { it.id == savedLocation }.coerceAtLeast(0))
            focusSpinner.setSelection(WidgetPreferences.focus(this@WidgetConfigActivity, widgetId).ordinal)

            val personaOrder = listOf(
                "health_conscious", "outdoor_fitness", "beach_surfer", "traveler",
                "parent_family", "agriculture_gardener", "commuter", "event_planner",
            )
            val personas = app.container.rulesCatalog.personas.values.sortedBy { personaOrder.indexOf(it.id).let { index -> if (index < 0) Int.MAX_VALUE else index } }
            val savedPersonas = WidgetPreferences.personaIds(
                this@WidgetConfigActivity,
                widgetId,
                app.container.userPrefs.personas,
            ).toSet()
            personas.forEach { persona ->
                val checkBox = CheckBox(this@WidgetConfigActivity).apply {
                    text = strings.personaLabel(persona.id, persona.label)
                    isChecked = persona.id in savedPersonas
                }
                personaChecks[persona.id] = checkBox
                personaList.addView(checkBox, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }
            save.isEnabled = locations.isNotEmpty() && personaChecks.values.any { it.isChecked }
            personaChecks.values.forEach { checkBox ->
                checkBox.setOnCheckedChangeListener { _, _ ->
                    save.isEnabled = locations.isNotEmpty() && personaChecks.values.any { it.isChecked }
                }
            }
        }

        save.setOnClickListener {
            val selectedLocation = locationIds.getOrNull(locationSpinner.selectedItemPosition) ?: return@setOnClickListener
            val selectedPersonas = personaChecks.filterValues { it.isChecked }.keys.toList()
            if (selectedPersonas.isEmpty()) {
                Toast.makeText(this, strings.text("widget_choose_at_least_one_persona", "Choose at least one persona"), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val selectedFocus = focusOptions.getOrNull(focusSpinner.selectedItemPosition) ?: WidgetFocus.PERSONAS
            WidgetPreferences.save(this, widgetId, selectedLocation, selectedPersonas, selectedFocus)
            lifecycleScope.launch {
                PersonaWeatherWidget.updateAll(this@WidgetConfigActivity)
                WidgetRefreshScheduler.enqueueDebounced(this@WidgetConfigActivity)
                setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
                finish()
            }
        }
        cancel.setOnClickListener { finish() }
    }
}