package com.example.personaweather.data

import android.content.Context
import com.example.personaweather.core.model.CardSpec
import com.example.personaweather.core.model.Persona
import com.example.personaweather.core.model.Rule
import com.example.personaweather.core.model.RulesCatalog
import org.json.JSONObject

class RulesLoader {
    companion object {
        fun loadFromAsset(context: Context, assetPath: String = "persona_rules.json"): RulesCatalog {
            val json = context.assets.open(assetPath).bufferedReader().use { it.readText() }
            return fromJson(json)
        }

        fun fromJson(jsonText: String): RulesCatalog {
            val root = JSONObject(jsonText)
            val cards = mutableMapOf<String, CardSpec>()
            val cardsJson = root.optJSONObject("cards") ?: JSONObject()
            val cardNames = cardsJson.keys()
            while (cardNames.hasNext()) {
                val cardId = cardNames.next()
                val json = cardsJson.getJSONObject(cardId)
                cards[cardId] = CardSpec(
                    id = cardId,
                    label = json.optString("label", cardId),
                    fields = json.optJSONArray("fields")?.let { array ->
                        List(array.length()) { index -> array.getString(index) }
                    } ?: emptyList(),
                    requiresCoastal = json.optBoolean("requires_coastal", false),
                    derived = json.optString("derived").takeIf { it.isNotEmpty() },
                )
            }

            val personas = mutableMapOf<String, Persona>()
            val personasJson = root.optJSONObject("personas") ?: JSONObject()
            val personaNames = personasJson.keys()
            while (personaNames.hasNext()) {
                val personaId = personaNames.next()
                val json = personasJson.getJSONObject(personaId)
                val defaultCards = json.optJSONArray("default_cards")?.let { array ->
                    List(array.length()) { index -> array.getString(index) }
                } ?: emptyList()
                val rules = json.optJSONArray("rules")?.let { array ->
                    List(array.length()) { index ->
                        val ruleJson = array.getJSONObject(index)
                        Rule(
                            id = ruleJson.optString("id"),
                            field = ruleJson.optString("field"),
                            operator = ruleJson.optString("operator"),
                            value = parseValue(ruleJson.opt("value")),
                            severity = ruleJson.optString("severity"),
                            message = ruleJson.optString("message"),
                            lookaheadHours = if (ruleJson.has("lookahead_hours")) ruleJson.getInt("lookahead_hours") else null,
                            aggregate = if (ruleJson.has("aggregate")) ruleJson.getString("aggregate") else null,
                            messageTa = ruleJson.optString("message_ta").takeIf { it.isNotBlank() },
                            messageHi = ruleJson.optString("message_hi").takeIf { it.isNotBlank() },
                        )
                    }
                } ?: emptyList()
                personas[personaId] = Persona(
                    id = personaId,
                    label = json.optString("label", personaId),
                    requiresCoastal = json.optBoolean("requires_coastal", false),
                    defaultCards = defaultCards,
                    rules = rules,
                )
            }

            return RulesCatalog(cards = cards, personas = personas)
        }

        private fun parseValue(value: Any?): Any? = when (value) {
            null -> null
            is Number -> value
            is Boolean -> value
            is String -> value
            else -> value.toString()
        }
    }
}
