package com.example.personaweather.data

import android.content.Context
import com.example.personaweather.core.model.CardSpec
import com.example.personaweather.core.model.Persona
import com.example.personaweather.core.model.Rule
import com.example.personaweather.core.model.Severity
import org.json.JSONObject

data class RulesConfig(val personas: List<Persona>, val cards: List<CardSpec>)

class RulesLoader(private val context: Context) {
    fun load(assetName: String = "persona_rules.json"): RulesConfig {
        val text = context.assets.open(assetName).bufferedReader().use { it.readText() }
        val root = JSONObject(text)
        val cardObject = root.getJSONObject("cards")
        val cards = buildList {
            for (id in CARD_ORDER) {
                val card = cardObject.getJSONObject(id)
                add(
                    CardSpec(
                        id = id,
                        label = card.getString("label"),
                        fields = card.getJSONArray("fields").toStringList(),
                        requiresCoastal = card.optBoolean("requires_coastal", false),
                        derived = card.optString("derived").takeIf { it.isNotEmpty() }
                    )
                )
            }
        }
        val personaObject = root.getJSONObject("personas")
        val personas = buildList {
            for (id in PERSONA_ORDER) {
                val item = personaObject.getJSONObject(id)
                val rules = item.getJSONArray("rules").let { array ->
                    buildList {
                        for (index in 0 until array.length()) {
                            val rule = array.getJSONObject(index)
                            val raw = rule.get("value")
                            val value: Any = when (raw) {
                                is Number -> raw.toDouble()
                                is String -> raw
                                else -> error("Unsupported value for rule ${rule.getString("id")}")
                            }
                            add(
                                Rule(
                                    id = rule.getString("id"), field = rule.getString("field"),
                                    op = rule.getString("operator"), value = value,
                                    severity = when (rule.getString("severity")) {
                                        "alert" -> Severity.ALERT
                                        "caution" -> Severity.CAUTION
                                        else -> error("Unknown severity in ${rule.getString("id")}")
                                    },
                                    message = rule.getString("message"),
                                    messageTa = rule.optString("message_ta").takeIf { it.isNotEmpty() },
                                    lookaheadHours = if (rule.has("lookahead_hours")) rule.getInt("lookahead_hours") else null,
                                    aggregate = rule.optString("aggregate").takeIf { it.isNotEmpty() }
                                )
                            )
                        }
                    }
                }
                add(
                    Persona(
                        id = id, label = item.getString("label"),
                        requiresCoastal = item.optBoolean("requires_coastal", false),
                        defaultCards = item.getJSONArray("default_cards").toStringList(), rules = rules
                    )
                )
            }
        }
        return RulesConfig(personas, cards)
    }

    companion object {
        private val PERSONA_ORDER = listOf(
            "health_conscious", "outdoor_fitness", "beach_surfer", "traveler",
            "parent_family", "agriculture_gardener", "commuter", "event_planner"
        )
        private val CARD_ORDER = listOf(
            "aqi_card", "pollen_card", "uv_card", "humidity_card", "running_window_card", "heat_card",
            "marine_card", "travel_alerts_card", "family_commute_card", "agriculture_card",
            "commute_traffic_card", "event_planning_card", "cloth_drying_card", "trip_planner_card",
            "daily_outlook_card"
        )
    }

    private fun org.json.JSONArray.toStringList(): List<String> =
        List(length()) { index -> getString(index) }
}