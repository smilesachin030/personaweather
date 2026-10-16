package com.example.personaweather

import com.example.personaweather.data.RulesLoader
import com.example.personaweather.ui.localization.AppStrings
import com.example.personaweather.widget.WidgetText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase1SmokeTest {
    @Test
    fun rulesLoader_parsesJsonCatalog() {
        val json = """
            {
              "cards": {
                "aqi_card": { "label": "Air Quality", "fields": ["aqi_value", "aqi_category"] }
              },
              "personas": {
                "health_conscious": {
                  "label": "Health-conscious",
                  "requires_coastal": false,
                  "default_cards": ["aqi_card"],
                  "rules": [
                    {
                      "id": "hc_aqi_caution",
                      "field": "aqi_value",
                      "operator": ">=",
                      "value": 100,
                      "severity": "caution",
                      "message": "Air quality is moderate.",
                      "message_ta": "காற்றின் தரம் மிதமானது.",
                      "message_hi": "वायु गुणवत्ता मध्यम है।"
                    }
                  ]
                }
              }
            }
        """.trimIndent()

        val catalog = RulesLoader.fromJson(json)

        assertTrue(catalog.cards.containsKey("aqi_card"))
        assertEquals(listOf("aqi_card"), catalog.personas["health_conscious"]?.defaultCards)
        assertEquals("hc_aqi_caution", catalog.personas["health_conscious"]?.rules?.first()?.id)
        assertEquals("காற்றின் தரம் மிதமானது.", catalog.personas["health_conscious"]?.rules?.first()?.messageTa)
        assertEquals("वायु गुणवत्ता मध्यम है।", catalog.personas["health_conscious"]?.rules?.first()?.messageHi)
    }

      @Test
      fun tamilAdvisoryTextFallsBackToEnglishWhenMissing() {
        val tamil = AppStrings.forCode(AppStrings.TAMIL)

        assertEquals("தமிழ் செய்தி", tamil.advisory("English message", "தமிழ் செய்தி"))
        assertEquals("English message", tamil.advisory("English message", null))
      }

      @Test
      fun hindiAdvisoryTextUsesHindiAndFallsBackToEnglishWhenMissing() {
        val hindi = AppStrings.forCode(AppStrings.HINDI)

        assertEquals("हिंदी संदेश", hindi.advisory("English message", null, "हिंदी संदेश"))
        assertEquals("English message", hindi.advisory("English message", null, null))
      }

      @Test
      fun hindiPersonaAndCardLabelsUseLocalizedStrings() {
        val hindi = AppStrings.forCode(AppStrings.HINDI)

        assertEquals("स्वास्थ्य-जागरूक", hindi.personaLabel("health_conscious", "Health-conscious"))
        assertEquals("वायु गुणवत्ता", hindi.cardLabel("aqi_card", "AQI"))
        assertEquals("पराग", hindi.cardLabel("pollen_card", "Pollen"))
        assertEquals("दौड़ने का अच्छा समय", hindi.cardLabel("running_window_card", "Running window"))
        assertEquals("मुंबई", hindi.locationName("Mumbai"))
        assertEquals("महाराष्ट्र", hindi.regionName("Maharashtra"))
        assertEquals("घर · वेलाचेरी", hindi.routePlaceName("Home · Velachery"))
        assertEquals("चेन्नई · घर से कार्यालय", hindi.routeTitle("chennai_sample", "Chennai · Home to Office"))
      }

      @Test
      fun widgetTextUsesSelectedAppLanguage() {
        val tamil = WidgetText(AppStrings.forCode(AppStrings.TAMIL))
        val hindi = WidgetText(AppStrings.forCode(AppStrings.HINDI))

        assertEquals("புதுப்பி", tamil.refresh())
        assertEquals("रिफ्रेश", hindi.refresh())
        assertEquals("எச்சரிக்கை", tamil.alert())
        assertEquals("अलर्ट", hindi.alert())
      }
}
