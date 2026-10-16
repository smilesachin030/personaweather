package com.example.personaweather.core.engine

import com.example.personaweather.core.model.Persona
import com.example.personaweather.core.model.WeatherAdvice
import com.example.personaweather.core.model.WeatherRow
import java.time.LocalDate

object WeatherActionSuggestionPolicy {
    private data class Candidate(
        val topic: String,
        val priority: Int,
        val personaOrder: Int,
        val advice: WeatherAdvice,
    )

    fun recommend(
        series: List<WeatherRow>,
        nowIndex: Int,
        personas: List<Persona>,
    ): List<WeatherAdvice> {
        val now = series.getOrNull(nowIndex) ?: return emptyList()
        val upcoming = series.drop(nowIndex)
        val candidates = mutableListOf<Candidate>()

        personas.forEachIndexed { personaOrder, persona ->
            when (persona.id) {
                "parent_family" -> addFamilyAdvice(now, upcoming, personaOrder, candidates)
                "commuter" -> addCommuteAdvice(now, upcoming, personaOrder, candidates)
                "traveler" -> addTravelerAdvice(now, upcoming, personaOrder, candidates)
                "health_conscious" -> addHealthAdvice(now, upcoming, personaOrder, candidates)
                "outdoor_fitness" -> addFitnessAdvice(now, upcoming, personaOrder, candidates)
                "beach_surfer" -> addBeachAdvice(upcoming, personaOrder, candidates)
                "agriculture_gardener" -> addAgricultureAdvice(now, upcoming, personaOrder, candidates)
                "event_planner" -> addEventAdvice(now, upcoming, personaOrder, candidates)
            }
        }

        val usedTopics = mutableSetOf<String>()
        return candidates
            .sortedWith(compareByDescending<Candidate> { it.priority }.thenBy { it.personaOrder })
            .filter { usedTopics.add(it.topic) }
            .take(Tuning.MAX_WEATHER_ADVICE)
            .map { it.advice }
    }

    private fun addFamilyAdvice(
        now: WeatherRow,
        upcoming: List<WeatherRow>,
        personaOrder: Int,
        candidates: MutableList<Candidate>,
    ) {
        val tomorrow = upcoming
            .filter { it.date == now.time.toLocalDate().plusDays(1).toString() }
        val tomorrowCommutes = tomorrow.filter { it.hour in 7..8 || it.hour in 16..17 }
        val tomorrowRain = tomorrowCommutes.maxByOrNull { it.rainChancePercent }
        val tomorrowRainChance = tomorrowRain?.rainChancePercent
        if (tomorrow.size == Tuning.COMPLETE_FORECAST_DAY_ROWS &&
            tomorrowCommutes.isNotEmpty() &&
            tomorrowRain != null &&
            tomorrowRainChance != null &&
            tomorrowRainChance >= Tuning.SCHOOL_RAIN_CHANCE
        ) {
            val rainTime = tomorrowRain.time.toLocalTime().format(Tuning.ADVICE_TIME)
            val commutePeriod = if (tomorrowRain.hour in 7..8) "drop-off" else "pickup"
            val commutePeriodTa = if (tomorrowRain.hour in 7..8) "பள்ளிக்குச் செல்லும் நேரம்" else "பள்ளியிலிருந்து திரும்பும் நேரம்"
            val commutePeriodHi = if (tomorrowRain.hour in 7..8) "बच्चों को स्कूल छोड़ने का समय" else "बच्चों को स्कूल से लेने का समय"
            val evidence = "Tomorrow's highest school-commute rain chance is ${tomorrowRainChance.toInt()}% around $rainTime."
            candidates += candidate(
                topic = "rain_protection",
                priority = if (tomorrowRainChance >= Tuning.HIGH_RAIN_CHANCE) 95 else 75,
                personaOrder = personaOrder,
                id = "family_school_rain",
                personaId = "parent_family",
                title = "Family & school travel",
                message = if (tomorrowRainChance >= Tuning.HIGH_RAIN_CHANCE) {
                    "Rain is likely around $rainTime during school $commutePeriod tomorrow. Pack rain gear and check official school or local-authority updates before leaving."
                } else {
                    "Pack a raincoat for showers forecast around $rainTime during school $commutePeriod tomorrow; check the school's official travel updates before leaving."
                },
                messageTa = if (tomorrowRainChance >= Tuning.HIGH_RAIN_CHANCE) {
                    "நாளை $rainTime மணியளவில் $commutePeriodTa மழை பெய்யக்கூடும். மழைக்கான உடைகளைத் தயார் செய்து, புறப்படும் முன் பள்ளியின் அதிகாரப்பூர்வ அறிவிப்பைப் பாருங்கள்."
                } else {
                    "நாளை $rainTime மணியளவில் $commutePeriodTa மழை எதிர்பார்க்கப்படுவதால் மழைக்கோட்டைத் தயார் செய்து, பள்ளியின் அதிகாரப்பூர்வ பயண அறிவிப்பைப் பாருங்கள்."
                },
                messageHi = if (tomorrowRainChance >= Tuning.HIGH_RAIN_CHANCE) {
                    "कल $rainTime बजे $commutePeriodHi बारिश हो सकती है। रेन गियर तैयार रखें और निकलने से पहले स्कूल की आधिकारिक सूचना देखें।"
                } else {
                    "कल $rainTime बजे $commutePeriodHi बारिश का पूर्वानुमान है। रेनकोट तैयार रखें और निकलने से पहले स्कूल की आधिकारिक यात्रा-सूचना देखें।"
                },
                evidence = evidence,
            )
        } else {
            val rain = upcoming.take(Tuning.FAMILY_LOOKAHEAD_HOURS + 1).firstOrNull {
                (it.hour in 7..8 || it.hour in 16..17) &&
                    it.rainChancePercent >= Tuning.RAINCOAT_CHANCE
            }
            if (rain != null) {
                val umbrella = rain.rainChancePercent >= Tuning.UMBRELLA_CHANCE
                candidates += candidate(
                    topic = "rain_protection",
                    priority = if (umbrella) 88 else 68,
                    personaOrder = personaOrder,
                    id = if (umbrella) "family_school_umbrella" else "family_school_raincoat",
                    personaId = "parent_family",
                    title = "Family commute",
                    message = "Rain is possible around ${rain.time.toLocalTime().format(Tuning.ADVICE_TIME)}. ${if (umbrella) "Pack umbrellas for the family." else "Pack light raincoats for the family."}",
                    messageTa = "${rain.time.toLocalTime().format(Tuning.ADVICE_TIME)} மணியளவில் மழை பெய்யலாம். ${if (umbrella) "குடைகளை எடுத்துச் செல்லுங்கள்." else "மெல்லிய மழைக்கோட்டுகளை எடுத்துச் செல்லுங்கள்."}",
                    messageHi = "${rain.time.toLocalTime().format(Tuning.ADVICE_TIME)} बजे बारिश हो सकती है। ${if (umbrella) "परिवार के लिए छाते रखें।" else "परिवार के लिए हल्के रेनकोट रखें।"}",
                    evidence = "Forecast at ${rain.time.toLocalTime().format(Tuning.ADVICE_TIME)}: ${rain.rainChancePercent.toInt()}% rain chance.",
                )
            }
        }

        if (now.aqiValue >= Tuning.UNHEALTHY_AQI) {
            candidates += candidate(
                topic = "air_quality",
                priority = 92,
                personaOrder = personaOrder,
                id = "family_air_quality",
                personaId = "parent_family",
                title = "Children's outdoor time",
                message = "Limit prolonged outdoor exertion for children and follow local health guidance.",
                messageTa = "குழந்தைகள் நீண்ட நேரம் வெளியில் கடுமையாகச் செயல்படுவதைத் தவிர்த்து, உள்ளூர் சுகாதார வழிகாட்டுதலைப் பின்பற்றுங்கள்.",
                messageHi = "बच्चों की लंबे समय तक बाहर की मेहनत वाली गतिविधि कम करें और स्थानीय स्वास्थ्य सलाह मानें।",
                evidence = "Current air quality is ${now.aqiCategory} (AQI ${now.aqiValue}).",
            )
        }
        if (now.heatAlert == 1 || now.feelsLike >= Tuning.HIGH_FEELS_LIKE) {
            candidates += candidate(
                topic = "heat",
                priority = 88,
                personaOrder = personaOrder,
                id = "family_heat",
                personaId = "parent_family",
                title = "Keep children cool",
                message = "Offer children water, plan shade breaks, and avoid strenuous outdoor activity during the hottest hours.",
                messageTa = "குழந்தைகளுக்குத் தண்ணீர் கொடுத்து, நிழலில் ஓய்வு எடுக்கச் செய்து, அதிக வெப்ப நேரத்தில் கடுமையான வெளிப்புறச் செயல்பாட்டைத் தவிர்க்கவும்.",
                messageHi = "बच्चों को पानी दें, छाया में आराम कराएँ और सबसे गर्म समय में कठिन बाहरी गतिविधि से बचें।",
                evidence = "Feels-like temperature: ${now.feelsLike.toInt()}°C${if (now.heatAlert == 1) "; heat alert is active" else ""}.",
            )
        }
    }

    private fun addCommuteAdvice(
        now: WeatherRow,
        upcoming: List<WeatherRow>,
        personaOrder: Int,
        candidates: MutableList<Candidate>,
    ) {
        val rain = firstRain(upcoming, Tuning.RAINCOAT_CHANCE, Tuning.COMMUTE_LOOKAHEAD_HOURS)
        if (rain != null) {
            val umbrella = rain.rainChancePercent >= Tuning.UMBRELLA_CHANCE
            candidates += candidate(
                topic = "rain_protection",
                priority = if (umbrella) 86 else 66,
                personaOrder = personaOrder,
                id = if (umbrella) "commuter_umbrella" else "commuter_raincoat",
                personaId = "commuter",
                title = "Commute rain plan",
                message = "Rain may start around ${rain.time.toLocalTime().format(Tuning.ADVICE_TIME)}. ${if (umbrella) "Carry an umbrella and allow a little extra time." else "Carry a light raincoat."}",
                messageTa = "${rain.time.toLocalTime().format(Tuning.ADVICE_TIME)} மணியளவில் மழை பெய்யலாம். ${if (umbrella) "குடை எடுத்துச் சென்று, சிறிது கூடுதல் நேரம் ஒதுக்குங்கள்." else "மெல்லிய மழைக்கோட்டை எடுத்துச் செல்லுங்கள்."}",
                messageHi = "${rain.time.toLocalTime().format(Tuning.ADVICE_TIME)} के आसपास बारिश हो सकती है। ${if (umbrella) "छाता रखें और थोड़ा अतिरिक्त समय रखें।" else "हल्का रेनकोट रखें।"}",
                evidence = "Rain chance reaches ${rain.rainChancePercent.toInt()}% at ${rain.time.toLocalTime().format(Tuning.ADVICE_TIME)}.",
            )
        }
        if (now.fogFlag == 1 || now.visibilityKm <= Tuning.LOW_VISIBILITY_KM) {
            candidates += candidate(
                topic = "low_visibility",
                priority = if (now.fogFlag == 1) 96 else 83,
                personaOrder = personaOrder,
                id = "commuter_low_visibility",
                personaId = "commuter",
                title = "Low-visibility commute",
                message = "Visibility is poor${if (now.fogFlag == 1) " because of fog" else ""}. Slow down, increase following distance, and use appropriate lights.",
                messageTa = "பார்வைத்திறன் குறைவாக உள்ளது${if (now.fogFlag == 1) " (மூடுபனி)" else ""}. வேகத்தைக் குறைத்து, வாகன இடைவெளியை அதிகரித்து, பொருத்தமான விளக்குகளைப் பயன்படுத்துங்கள்.",
                messageHi = "दृश्यता कम है${if (now.fogFlag == 1) " और कोहरा है" else ""}। गति कम रखें, आगे के वाहन से दूरी बढ़ाएँ और उचित लाइट इस्तेमाल करें।",
                evidence = "Visibility: ${now.visibilityKm} km${if (now.fogFlag == 1) "; fog flag is active" else ""}.",
            )
        }
    }

    private fun addTravelerAdvice(
        now: WeatherRow,
        upcoming: List<WeatherRow>,
        personaOrder: Int,
        candidates: MutableList<Candidate>,
    ) {
        if (now.fogFlag == 1 || now.visibilityKm <= Tuning.LOW_VISIBILITY_KM) {
            candidates += candidate(
                topic = "low_visibility",
                priority = if (now.fogFlag == 1) 97 else 84,
                personaOrder = personaOrder,
                id = "traveler_low_visibility",
                personaId = "traveler",
                title = "Travel visibility",
                message = "Visibility is poor${if (now.fogFlag == 1) " because of fog" else ""}. Check your transport operator for updates and allow extra travel time.",
                messageTa = "பார்வைத்திறன் குறைவாக உள்ளது${if (now.fogFlag == 1) " (மூடுபனி)" else ""}. பயணத்திற்கு கூடுதல் நேரம் ஒதுக்கி, போக்குவரத்து சேவை அறிவிப்புகளைச் சரிபார்க்கவும்.",
                messageHi = "दृश्यता कम है${if (now.fogFlag == 1) " और कोहरा है" else ""}। यात्रा के लिए अतिरिक्त समय रखें और अपने परिवहन ऑपरेटर की सूचना देखें।",
                evidence = "Visibility: ${now.visibilityKm} km${if (now.fogFlag == 1) "; fog flag is active" else ""}.",
            )
        }
        val rain = firstRain(upcoming, Tuning.RAINCOAT_CHANCE, Tuning.TRAVEL_LOOKAHEAD_HOURS)
        if (rain != null) {
            candidates += candidate(
                topic = "rain_protection",
                priority = if (rain.rainChancePercent >= Tuning.UMBRELLA_CHANCE) 82 else 62,
                personaOrder = personaOrder,
                id = "traveler_rain",
                personaId = "traveler",
                title = "Travel rain plan",
                message = "Rain is possible around ${rain.time.toLocalTime().format(Tuning.ADVICE_TIME)}. Pack rain protection and check your operator's updates.",
                messageTa = "${rain.time.toLocalTime().format(Tuning.ADVICE_TIME)} மணியளவில் மழை பெய்யலாம். மழைக்கான பாதுகாப்பை எடுத்துச் சென்று, பயண சேவை அறிவிப்புகளைப் பாருங்கள்.",
                messageHi = "${rain.time.toLocalTime().format(Tuning.ADVICE_TIME)} के आसपास बारिश हो सकती है। बारिश से बचाव का सामान रखें और ऑपरेटर की सूचना देखें।",
                evidence = "Forecast at ${rain.time.toLocalTime().format(Tuning.ADVICE_TIME)}: ${rain.rainChancePercent.toInt()}% rain chance.",
            )
        }
    }

    private fun addHealthAdvice(
        now: WeatherRow,
        upcoming: List<WeatherRow>,
        personaOrder: Int,
        candidates: MutableList<Candidate>,
    ) {
        addSunAdvice(upcoming, personaOrder, candidates)
        addAirAdvice(now, personaOrder, candidates)
        val pollen = listOf(now.pollenGrass, now.pollenTree, now.pollenRagweed)
            .firstOrNull { it.equals("high", true) }
            ?: listOf(now.pollenGrass, now.pollenTree, now.pollenRagweed)
                .firstOrNull { it.equals("moderate", true) }
        if (pollen != null) {
            candidates += candidate(
                topic = "pollen",
                priority = if (pollen.equals("high", true)) 70 else 58,
                personaOrder = personaOrder,
                id = "health_pollen",
                personaId = "health_conscious",
                title = "Pollen note",
                message = "$pollen pollen is forecast. If you have allergies, consider limiting prolonged outdoor exposure and follow your clinician's advice.",
                messageTa = "மகரந்த அளவு $pollen என முன்னறிவிக்கப்படுகிறது. ஒவ்வாமை இருந்தால் நீண்ட நேரம் வெளியில் இருப்பதைக் குறைத்து, மருத்துவரின் ஆலோசனையைப் பின்பற்றுங்கள்.",
                messageHi = "पराग स्तर $pollen रहने का अनुमान है। एलर्जी हो तो लंबे समय तक बाहर रहने से बचें और अपने चिकित्सक की सलाह मानें।",
                evidence = "Pollen forecast: grass ${now.pollenGrass}, tree ${now.pollenTree}, ragweed ${now.pollenRagweed}.",
            )
        }
    }

    private fun addFitnessAdvice(
        now: WeatherRow,
        upcoming: List<WeatherRow>,
        personaOrder: Int,
        candidates: MutableList<Candidate>,
    ) {
        addSunAdvice(upcoming, personaOrder, candidates, fitness = true)
        addAirAdvice(now, personaOrder, candidates, fitness = true)
        if (now.heatAlert == 1 || now.feelsLike >= Tuning.HIGH_FEELS_LIKE) {
            candidates += candidate(
                topic = "heat",
                priority = 89,
                personaOrder = personaOrder,
                id = "fitness_heat",
                personaId = "outdoor_fitness",
                title = "Workout timing",
                message = "Move strenuous exercise to a cooler time or indoors and hydrate.",
                messageTa = "கடுமையான உடற்பயிற்சியை குளிர்ச்சியான நேரத்திற்கோ உள்ளரங்கிற்கோ மாற்றி, தண்ணீர் குடிக்கவும்.",
                messageHi = "कठिन व्यायाम ठंडे समय या घर के अंदर करें और पानी पिएँ।",
                evidence = "Feels-like temperature: ${now.feelsLike.toInt()}°C${if (now.heatAlert == 1) "; heat alert is active" else ""}.",
            )
        }
    }

    private fun addSunAdvice(
        upcoming: List<WeatherRow>,
        personaOrder: Int,
        candidates: MutableList<Candidate>,
        fitness: Boolean = false,
    ) {
        val peak = upcoming.take(Tuning.TRAVEL_LOOKAHEAD_HOURS + 1)
            .filter { it.hour in Tuning.DAYLIGHT_HOURS }
            .maxByOrNull { it.uvIndex }
            ?.takeIf { it.uvIndex >= Tuning.HIGH_UV }
            ?: return
        val veryHigh = peak.uvIndex >= Tuning.VERY_HIGH_UV
        val peakTime = peak.time.toLocalTime().format(Tuning.ADVICE_TIME)
        candidates += candidate(
            topic = "sun_protection",
            priority = if (veryHigh) 78 else 55,
            personaOrder = personaOrder,
            id = if (fitness) "fitness_uv" else "health_uv",
            personaId = if (fitness) "outdoor_fitness" else "health_conscious",
            title = if (fitness) "Outdoor workout" else "Sun protection",
            message = if (veryHigh) {
                "Around $peakTime, use sunscreen, a hat, and shade; move outdoor exercise out of peak sun."
            } else {
                "Use sun protection around $peakTime and seek shade when practical."
            },
            messageTa = if (veryHigh) {
                "$peakTime மணியளவில் சன்ஸ்கிரீன், தொப்பி, நிழலைப் பயன்படுத்துங்கள்; உச்ச வெயிலில் உடற்பயிற்சியைத் தவிர்க்கவும்."
            } else {
                "$peakTime மணியளவில் சூரிய பாதுகாப்பைப் பயன்படுத்தி, இயன்றால் நிழலில் இருங்கள்."
            },
            messageHi = if (veryHigh) {
                "$peakTime बजे सनस्क्रीन, टोपी और छाया का उपयोग करें; तेज धूप में बाहरी व्यायाम टालें।"
            } else {
                "$peakTime बजे धूप से बचाव करें और संभव हो तो छाया में रहें।"
            },
            evidence = "UV index reaches ${peak.uvIndex.toInt()} around $peakTime.",
        )
    }

    private fun addAirAdvice(
        now: WeatherRow,
        personaOrder: Int,
        candidates: MutableList<Candidate>,
        fitness: Boolean = false,
    ) {
        if (now.aqiValue < Tuning.SENSITIVE_AQI) return
        val unhealthy = now.aqiValue >= Tuning.UNHEALTHY_AQI
        candidates += candidate(
            topic = "air_quality",
            priority = if (unhealthy) 91 else 63,
            personaOrder = personaOrder,
            id = if (fitness) "fitness_air_quality" else "health_air_quality",
            personaId = if (fitness) "outdoor_fitness" else "health_conscious",
            title = if (fitness) "Outdoor exercise" else "Air quality",
            message = if (unhealthy) {
                "Limit prolonged outdoor exertion and consider an indoor workout."
            } else {
                "If you are sensitive to air pollution, limit prolonged strenuous activity outdoors."
            },
            messageTa = if (unhealthy) {
                "நீண்ட நேரம் கடுமையாக வெளியில் செயல்படுவதைத் தவிர்த்து, உள்ளரங்க உடற்பயிற்சியைப் பரிசீலிக்கவும்."
            } else {
                "காற்று மாசுக்கு உணர்திறன் இருந்தால், நீண்ட நேர கடுமையான வெளிப்புறச் செயல்பாட்டைக் குறைக்கவும்."
            },
            messageHi = if (unhealthy) {
                "लंबे समय तक बाहर मेहनत वाली गतिविधि सीमित करें; घर के अंदर व्यायाम करें।"
            } else {
                "हवा के प्रदूषण के प्रति संवेदनशील हों तो लंबे समय तक कठिन बाहरी गतिविधि सीमित रखें।"
            },
            evidence = "Current air quality: ${now.aqiCategory} (AQI ${now.aqiValue}).",
        )
    }

    private fun addBeachAdvice(
        upcoming: List<WeatherRow>,
        personaOrder: Int,
        candidates: MutableList<Candidate>,
    ) {
        val forecast = upcoming.take(Tuning.TRAVEL_LOOKAHEAD_HOURS + 1)
            .filter { it.waveHeightM != null }
            .maxByOrNull { it.waveHeightM ?: Double.NEGATIVE_INFINITY }
            ?: return
        val wave = forecast.waveHeightM ?: return
        if (wave < Tuning.ROUGH_WAVE_METRES && forecast.windSpeed < Tuning.STRONG_COASTAL_WIND) return
        val dangerous = wave >= Tuning.DANGEROUS_WAVE_METRES
        val forecastTime = forecast.time.toLocalTime().format(Tuning.ADVICE_TIME)
        candidates += candidate(
            topic = "marine_conditions",
            priority = if (dangerous) 100 else 90,
            personaOrder = personaOrder,
            id = if (dangerous) "surfer_dangerous_sea" else "surfer_rough_sea",
            personaId = "beach_surfer",
            title = "Sea conditions",
            message = if (dangerous) {
                "Sea conditions may be dangerous around $forecastTime. Do not swim or surf; follow local lifeguard instructions."
            } else {
                "Sea conditions may be rough around $forecastTime. Check local beach and lifeguard advice before entering the water."
            },
            messageTa = if (dangerous) {
                "$forecastTime மணியளவில் கடல் நிலை ஆபத்தாக இருக்கலாம். நீந்தவோ சர்ஃப் செய்யவோ வேண்டாம்; உள்ளூர் உயிர்காப்பாளர் அறிவுரையைப் பின்பற்றுங்கள்."
            } else {
                "$forecastTime மணியளவில் கடல் கொந்தளிப்பாக இருக்கலாம். நீரில் இறங்கும் முன் உள்ளூர் கடற்கரை/உயிர்காப்பாளர் அறிவுரையைப் பாருங்கள்."
            },
            messageHi = if (dangerous) {
                "$forecastTime बजे समुद्र की स्थिति खतरनाक हो सकती है। तैरें या सर्फ़िंग न करें; स्थानीय लाइफगार्ड के निर्देश मानें।"
            } else {
                "$forecastTime बजे समुद्र में लहरें तेज़ हो सकती हैं। पानी में जाने से पहले स्थानीय समुद्रतट/लाइफगार्ड की सलाह देखें।"
            },
            evidence = "Forecast around $forecastTime: wave height ${"%.1f".format(java.util.Locale.ROOT, wave)} m; wind ${forecast.windSpeed.toInt()} km/h.",
        )
    }

    private fun addAgricultureAdvice(
        now: WeatherRow,
        upcoming: List<WeatherRow>,
        personaOrder: Int,
        candidates: MutableList<Candidate>,
    ) {
        val next24 = upcoming.take(Tuning.AGRICULTURE_LOOKAHEAD_HOURS)
        val rainfall = next24.sumOf { it.rainfallMm }
        if (next24.size == Tuning.COMPLETE_FORECAST_DAY_ROWS &&
            rainfall >= Tuning.RAINFALL_HOLD_IRRIGATION_MM
        ) {
            candidates += candidate(
                topic = "agriculture_rain",
                priority = 72,
                personaOrder = personaOrder,
                id = "agriculture_rain",
                personaId = "agriculture_gardener",
                title = "Rain and field work",
                message = "Rain is forecast in the next 24 hours. If your crop plan allows, postpone irrigation and spraying; check again before field work.",
                messageTa = "அடுத்த 24 மணிநேரத்தில் மழை முன்னறிவிக்கப்படுகிறது. பயிர் திட்டம் அனுமதித்தால் நீர்ப்பாசனம் மற்றும் தெளிப்பை ஒத்திவைத்து, வயல் பணிக்கு முன் மீண்டும் சரிபார்க்கவும்.",
                messageHi = "अगले 24 घंटों में बारिश का अनुमान है। फसल योजना के अनुसार सिंचाई और छिड़काव टालें; खेत के काम से पहले फिर जाँचें।",
                evidence = "Forecast rainfall across the next 24 hourly rows: ${"%.1f".format(java.util.Locale.ROOT, rainfall)} mm.",
            )
        } else if (next24.size == Tuning.COMPLETE_FORECAST_DAY_ROWS &&
            now.soilMoisturePercent <= Tuning.LOW_SOIL_MOISTURE_PERCENT
        ) {
            candidates += candidate(
                topic = "agriculture_rain",
                priority = 61,
                personaOrder = personaOrder,
                id = "agriculture_irrigation",
                personaId = "agriculture_gardener",
                title = "Soil moisture",
                message = "Soil is dry and little rain is forecast over the next 24 hours. Check the soil and irrigate if your crop needs it.",
                messageTa = "மண் உலர்ந்துள்ளது; அடுத்த 24 மணிநேரத்தில் குறைந்த மழையே எதிர்பார்க்கப்படுகிறது. மண்ணைச் சரிபார்த்து, பயிருக்கு தேவைப்பட்டால் நீர்ப்பாசனம் செய்யுங்கள்.",
                messageHi = "मिट्टी सूखी है और अगले 24 घंटों में कम बारिश का अनुमान है। मिट्टी जाँचें और फसल को ज़रूरत हो तो सिंचाई करें।",
                evidence = "Soil moisture: ${now.soilMoisturePercent.toInt()}%; next-24-hour forecast rainfall: ${"%.1f".format(java.util.Locale.ROOT, rainfall)} mm.",
            )
        }
    }

    private fun addEventAdvice(
        now: WeatherRow,
        upcoming: List<WeatherRow>,
        personaOrder: Int,
        candidates: MutableList<Candidate>,
    ) {
        val wettestDay = upcoming
            .filter { it.date != now.date }
            .groupBy { it.date }
            .mapNotNull { (date, rows) ->
                val chance = rows.maxOfOrNull { it.rainChancePercent } ?: return@mapNotNull null
                date to chance
            }
            .maxByOrNull { it.second }
            ?: return
        if (wettestDay.second < Tuning.EVENT_RAIN_CHANCE) return
        val date = LocalDate.parse(wettestDay.first).format(Tuning.ADVICE_DATE)
        candidates += candidate(
            topic = "event_weather",
            priority = if (wettestDay.second >= Tuning.HIGH_RAIN_CHANCE) 77 else 57,
            personaOrder = personaOrder,
            id = "event_rain_backup",
            personaId = "event_planner",
            title = "Outdoor event backup",
            message = "Rain is possible on $date. Arrange cover or an indoor backup and recheck nearer the event.",
            messageTa = "$date அன்று மழை பெய்யலாம். மூடப்பட்ட இடம் அல்லது உள்ளரங்க மாற்றைத் தயார் செய்து, நிகழ்ச்சி நெருங்கும்போது மீண்டும் சரிபார்க்கவும்.",
            messageHi = "$date को बारिश हो सकती है। छत या घर के अंदर का विकल्प रखें और कार्यक्रम के करीब फिर जाँचें।",
            evidence = "$date maximum rain chance: ${wettestDay.second.toInt()}%.",
        )
    }

    private fun firstRain(rows: List<WeatherRow>, minimumChance: Double, hours: Int): WeatherRow? =
        rows.take(hours + 1).firstOrNull { it.rainChancePercent >= minimumChance }

    private fun candidate(
        topic: String,
        priority: Int,
        personaOrder: Int,
        id: String,
        personaId: String,
        title: String,
        message: String,
        messageTa: String,
        messageHi: String,
        evidence: String,
    ) = Candidate(
        topic = topic,
        priority = priority,
        personaOrder = personaOrder,
        advice = WeatherAdvice(
            id = id,
            personaId = personaId,
            title = title,
            message = message,
            evidence = evidence,
            messageTa = messageTa,
            messageHi = messageHi,
        ),
    )
}
