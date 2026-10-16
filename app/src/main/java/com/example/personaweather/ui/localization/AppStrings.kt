package com.example.personaweather.ui.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import java.util.Locale

class AppStrings private constructor(
    val languageCode: String,
    private val translations: Map<String, String>,
) {
    fun text(key: String, fallback: String): String = translations[key] ?: fallback

    fun format(key: String, fallback: String, vararg arguments: Any): String =
        text(key, fallback).format(Locale.ROOT, *arguments)
    val locale: Locale = when (languageCode) {
        TAMIL -> Locale.forLanguageTag("ta-IN")
        HINDI -> Locale.forLanguageTag("hi-IN")
        else -> Locale.US
    }

    fun term(value: String): String = text("term:$value", value)

    fun condition(value: String): String = text("condition:$value", value)

    fun weather(value: String): String = text("weather:$value", value)

    fun personaLabel(id: String, fallback: String = id.replace('_', ' ')): String =
        text("persona_$id", fallback)

    fun widgetFocusLabel(id: String, fallback: String): String =
        text("widget_focus_$id", fallback)

    fun dataSourceLabel(value: String): String {
        if (languageCode != HINDI) return value
        return when {
            value == "Open-Meteo forecast" -> "Open-Meteo पूर्वानुमान"
            value == "Weather forecast" -> "मौसम पूर्वानुमान"
            value.startsWith("Open-Meteo cached forecast") ->
                value.replaceFirst("Open-Meteo cached forecast", "Open-Meteo का कैश किया हुआ पूर्वानुमान")
                    .replace("updated ", "अपडेट: ")
            else -> value
        }
    }

    fun smartActionTitle(value: String): String = when (value) {
        "AC suggestion" -> text("smart_action_ac_title", value)
        "Light suggestion" -> text("smart_action_light_title", value)
        "Weather tip" -> text("smart_action_weather_title", value)
        "Sun protection" -> text("smart_action_sun_title", value)
        "Air quality tip" -> text("smart_action_air_title", value)
        "Heat tip" -> text("smart_action_heat_title", value)
        else -> value
    }

    fun smartActionMessage(value: String): String = when (value) {
        "High temperature is forecast for this area. Consider switching the AC on for a cooler room." ->
            text("smart_action_ac_message", value)
        "It is getting darker and the room is cool. Consider switching a light on for comfort." ->
            text("smart_action_light_message", value)
        "Carry an umbrella." -> text("smart_action_rain_message", value)
        "Apply sunscreen and wear a cap." -> text("smart_action_sun_message", value)
        "Wear a mask when you step outside." -> text("smart_action_air_message", value)
        "Stay hydrated and avoid the hottest part of the day." -> text("smart_action_heat_message", value)
        else -> value
    }

    fun weatherAdviceTitle(id: String, fallback: String): String =
        text("weather_advice_title_$id", fallback)

    fun cardLabel(id: String, fallback: String = id.removeSuffix("_card").replace('_', ' ')): String =
        text("card_$id", fallback)

    fun locationName(value: String): String = when (languageCode) {
        HINDI -> when (value) {
            "Nungambakkam" -> "नुंगमबक्कम"
            "Velachery" -> "वेलाचेरी"
            "Mahabalipuram" -> "महाबलिपुरम"
            "Guindy / Alandur" -> "गिंडी / आलंदूर"
            "T. Nagar" -> "टी. नगर"
            "Tambaram" -> "ताम्बरम"
            "Sriperumbudur" -> "श्रीपेरुम्बुदुर"
            "Mumbai" -> "मुंबई"
            "Pune" -> "पुणे"
            "Nagpur" -> "नागपुर"
            "Nashik" -> "नासिक"
            "Ahmedabad" -> "अहमदाबाद"
            "Surat" -> "सूरत"
            "Vadodara" -> "वडोदरा"
            "Bengaluru" -> "बेंगलुरु"
            "Mysuru" -> "मैसूरु"
            "Mangaluru" -> "मंगलुरु"
            "Chennai" -> "चेन्नई"
            "Coimbatore" -> "कोयंबटूर"
            "Madurai" -> "मदुरै"
            "Hyderabad" -> "हैदराबाद"
            "Visakhapatnam" -> "विशाखापत्तनम"
            "Vijayawada" -> "विजयवाड़ा"
            "Tirupati" -> "तिरुपति"
            "Kochi" -> "कोच्चि"
            "Thiruvananthapuram" -> "तिरुवनंतपुरम"
            "Kozhikode" -> "कोझिकोड"
            "Kolkata" -> "कोलकाता"
            "Siliguri" -> "सिलीगुड़ी"
            "New Delhi" -> "नई दिल्ली"
            "Jaipur" -> "जयपुर"
            "Jodhpur" -> "जोधपुर"
            "Lucknow" -> "लखनऊ"
            "Kanpur" -> "कानपुर"
            "Varanasi" -> "वाराणसी"
            "Patna" -> "पटना"
            "Gaya" -> "गया"
            "Bhubaneswar" -> "भुवनेश्वर"
            "Cuttack" -> "कटक"
            "Raipur" -> "रायपुर"
            "Bhopal" -> "भोपाल"
            "Indore" -> "इंदौर"
            "Chandigarh" -> "चंडीगढ़"
            "Gurugram" -> "गुरुग्राम"
            "Shimla" -> "शिमला"
            "Dehradun" -> "देहरादून"
            "Srinagar" -> "श्रीनगर"
            "Jammu" -> "जम्मू"
            "Guwahati" -> "गुवाहाटी"
            "Shillong" -> "शिलांग"
            "Imphal" -> "इंफाल"
            "Aizawl" -> "आइज़ोल"
            "Kohima" -> "कोहिमा"
            "Itanagar" -> "ईटानगर"
            "Gangtok" -> "गंगटोक"
            "Ranchi" -> "रांची"
            "Agartala" -> "अगरतला"
            "Dwarka" -> "द्वारका"
            "Noida" -> "नोएडा"
            "Borivali" -> "बोरीवली"
            "Andheri" -> "अंधेरी"
            "Bandra" -> "बांद्रा"
            "Colaba" -> "कोलाबा"
            else -> value
        }
        TAMIL -> when (value) {
            "Nungambakkam" -> "நுங்கம்பாக்கம்"
            "Velachery" -> "வேளச்சேரி"
            "Mahabalipuram" -> "மகாபலிபுரம்"
            "Guindy / Alandur" -> "கிண்டி / ஆலந்தூர்"
            "T. Nagar" -> "டி.நகர்"
            "Tambaram" -> "தாம்பரம்"
            "Sriperumbudur" -> "ஸ்ரீபெரும்புதூர்"
            else -> value
        }
        else -> value
    }

    fun regionName(value: String): String = when (languageCode) {
        HINDI -> when (value) {
            "Maharashtra" -> "महाराष्ट्र"
            "Gujarat" -> "गुजरात"
            "Karnataka" -> "कर्नाटक"
            "Tamil Nadu" -> "तमिलनाडु"
            "Telangana" -> "तेलंगाना"
            "Andhra Pradesh" -> "आंध्र प्रदेश"
            "Kerala" -> "केरल"
            "West Bengal" -> "पश्चिम बंगाल"
            "Delhi" -> "दिल्ली"
            "Rajasthan" -> "राजस्थान"
            "Uttar Pradesh" -> "उत्तर प्रदेश"
            "Bihar" -> "बिहार"
            "Odisha" -> "ओडिशा"
            "Chhattisgarh" -> "छत्तीसगढ़"
            "Madhya Pradesh" -> "मध्य प्रदेश"
            "Chandigarh" -> "चंडीगढ़"
            "Haryana" -> "हरियाणा"
            "Himachal Pradesh" -> "हिमाचल प्रदेश"
            "Uttarakhand" -> "उत्तराखंड"
            "Jammu and Kashmir" -> "जम्मू और कश्मीर"
            "Assam" -> "असम"
            "Meghalaya" -> "मेघालय"
            "Manipur" -> "मणिपुर"
            "Mizoram" -> "मिज़ोरम"
            "Nagaland" -> "नागालैंड"
            "Arunachal Pradesh" -> "अरुणाचल प्रदेश"
            "Sikkim" -> "सिक्किम"
            "Jharkhand" -> "झारखंड"
            "Tripura" -> "त्रिपुरा"
            else -> value
        }
        else -> value
    }

    fun routePlaceName(value: String): String {
        if (languageCode != HINDI) return value
        val (prefix, place) = when {
            value.startsWith("Home · ") -> text("route_home", "घर") to value.removePrefix("Home · ")
            value.startsWith("Office · ") -> text("route_office", "कार्यालय") to value.removePrefix("Office · ")
            else -> return locationName(value)
        }
        return "$prefix · ${locationName(place)}"
    }

    fun routeTitle(id: String, fallback: String): String =
        text("route_title_$id", fallback)

    fun rainSentence(english: String): String {
        when (languageCode) {
            TAMIL -> {
                if (english == "No rain expected in the next 24 hours") {
                    return text("rain_none", "அடுத்த 24 மணிநேரத்தில் மழை எதிர்பார்க்கப்படவில்லை")
                }
                if (english == "Rain now") return text("rain_now", "இப்போது மழை")
                Regex("^Rain now, easing around (.+)$").matchEntire(english)?.let { match ->
                    return format("rain_easing", "இப்போது மழை; %1\$s மணியளவில் குறையும்", match.groupValues[1])
                }
                Regex("^Rain expected around (.+) \\((\\d+)%\\)$").matchEntire(english)?.let { match ->
                    return format("rain_expected", "%1\$s மணியளவில் மழை எதிர்பார்க்கப்படுகிறது (%2\$s%%)", match.groupValues[1], match.groupValues[2])
                }
                return english
            }
            HINDI -> {
                if (english == "No rain expected in the next 24 hours") {
                    return text("rain_none", "अगले 24 घंटों में बारिश की उम्मीद नहीं है")
                }
                if (english == "Rain now") return text("rain_now", "अभी बारिश")
                Regex("^Rain now, easing around (.+)$").matchEntire(english)?.let { match ->
                    return format("rain_easing", "अभी बारिश; %1\$s के आसपास कम होगी", match.groupValues[1])
                }
                Regex("^Rain expected around (.+) \\((\\d+)%\\)$").matchEntire(english)?.let { match ->
                    return format("rain_expected", "%1\$s के आसपास बारिश की उम्मीद है (%2\$s%%)", match.groupValues[1], match.groupValues[2])
                }
                return english
            }
            else -> return english
        }
    }

    fun advisory(english: String, tamil: String?, hindi: String? = null): String =
        when (languageCode) {
            TAMIL -> if (!tamil.isNullOrBlank()) tamil else english
            HINDI -> if (!hindi.isNullOrBlank()) hindi else english
            else -> english
        }

    companion object {
        const val ENGLISH = "en"
        const val TAMIL = "ta"
        const val HINDI = "hi"

        private val tamilStrings = mapOf(
            "weather_buddy_open" to "WeatherBuddy அரட்டையைத் திற",
            "weather_buddy_entry" to "வானிலையைக் கேளுங்கள்",
            "weather_buddy_title" to "WeatherBuddy",
            "weather_buddy_close" to "அரட்டையை மூடு",
            "weather_buddy_subtitle" to "உங்கள் முன்னறிவிப்பை எளிதாகப் புரிந்துகொள்ளுங்கள்",
            "weather_buddy_intro" to "வணக்கம்! %1\$s வானிலை முன்னறிவிப்பைப் புரிந்துகொள்ள உதவுகிறேன். என்ன தெரிந்துகொள்ள விரும்புகிறீர்கள்?",
            "weather_buddy_input" to "கேள்வியை உள்ளிடவும்",
            "weather_buddy_send" to "செய்தியை அனுப்பு",
            "weather_buddy_suggestions" to "இதில் கேட்கலாம்",
            "weather_buddy_typing" to "வானிலை வழிகாட்டி பதிலளிக்கிறது",
            "weather_buddy_prompt_current" to "இப்போதைய வானிலை என்ன?",
            "weather_buddy_prompt_rain" to "விரைவில் மழை பெய்யுமா?",
            "weather_buddy_prompt_air" to "காற்றுத் தரம் எப்படி உள்ளது?",
            "weather_buddy_prompt_trip" to "நாளை நான் வெளியே செல்லலாமா?",
            "weather_buddy_prompt_location" to "இடத்தை எங்கே மாற்றுவது?",
            "weather_buddy_current_reply" to "%1\$s இல் இப்போதைய வானிலை: %2\$s, %3$.0f°C (உணரப்படும் வெப்பநிலை %4$.0f°C).",
            "weather_buddy_current_unavailable" to "இப்போதைய வானிலைத் தகவல் தற்போது கிடைக்கவில்லை.",
            "weather_buddy_rain_reply" to "%1\$s க்கான கிடைக்கும் முன்னறிவிப்பில், அதிகபட்ச மழை வாய்ப்பு %3\$s மணியளவில் %2$.0f%%.",
            "weather_buddy_rain_unavailable" to "மழை முன்னறிவிப்பு தகவல் தற்போது கிடைக்கவில்லை.",
            "weather_buddy_air_reply" to "%1\$s இல் காற்றுத் தரம் %2\$s (AQI %3\$d).",
            "weather_buddy_air_unavailable" to "காற்றுத் தரத் தகவல் தற்போது கிடைக்கவில்லை.",
            "weather_buddy_no_alerts" to "இந்த இடத்திற்கு தற்போது செயலில் உள்ள வானிலை எச்சரிக்கைகள் இல்லை.",
            "weather_buddy_location_help" to "வேறு இடத்தைத் தேர்ந்தெடுக்க Home திரையின் மேலே உள்ள இடத்தின் பெயரைத் தட்டவும்.",
            "weather_buddy_help" to "உள்ளூர் வானிலை, மழை வாய்ப்பு, காற்றுத் தரம் அல்லது எச்சரிக்கைகள் பற்றி கேளுங்கள். Home திரையின் மேலே இடத்தை மாற்றலாம்.",
            "weather_buddy_fallback" to "உள்ளூர் முன்னறிவிப்பு, காற்றுத் தரம், எச்சரிக்கைகள் மற்றும் செயலியைப் பயன்படுத்துவது பற்றி உதவ முடியும்.",
            "weather_buddy_keywords_location" to "இடம்|நகரம்",
            "weather_buddy_keywords_change" to "மாற்ற|எங்கே",
            "weather_buddy_keywords_help" to "உதவி|எப்படி",
            "weather_buddy_keywords_rain" to "மழை|குடை",
            "weather_buddy_keywords_air" to "காற்று|AQI|மாசு",
            "weather_buddy_keywords_alert" to "எச்சரிக்கை|வெளியே செல்ல",
            "weather_buddy_keywords_current" to "வானிலை|வெப்பநிலை|நிலை|வெளியே|அணிய",
            "weather_buddy_keywords_tomorrow" to "நாளை",
            "weather_buddy_keywords_trip" to "செல்ல|போக|பயணம்|வெளியே",
            "weather_buddy_trip_unavailable" to "நாளைக்கான பாதுகாப்பான ஆம்/இல்லை பதிலை வழங்க முன்னறிவிப்பு கிடைக்கவில்லை. வெளியே செல்வதற்கு முன் மீண்டும் வானிலையைச் சரிபார்க்கவும்.",
            "weather_buddy_trip_location_scope" to "%1\$s இடத்திற்கான வானிலையை உறுதிப்படுத்த முடியவில்லை; இப்போது %2\$s இடத்திற்கான முன்னறிவிப்புதான் உள்ளது. இடத் தேர்வில் %1\$s ஐத் தேர்ந்தெடுத்து மீண்டும் கேளுங்கள்.",
            "weather_buddy_trip_conditions" to "அதிகபட்ச மழை வாய்ப்பு %1\$.0f%%, அதிகபட்ச AQI %2\$d, குறைந்த பார்வைத் தூரம் %3\$.1f கிமீ, அதிகபட்ச UV %4\$.0f, அதிகபட்ச வெப்பநிலை %5\$.0f°C.",
            "weather_buddy_trip_clear" to "ஆம் — நாளை %1\$s செல்ல வானிலை சாதகமாகத் தெரிகிறது. %2\$s குறிப்பிடத்தக்க ஆபத்து தெரியவில்லை; முன்னெச்சரிக்கையாக %3\$s எடுத்துச் செல்லுங்கள்.",
            "weather_buddy_trip_caution" to "நாளை %1\$s செல்லலாம், ஆனால் வானிலை சற்று மாறுபடுகிறது. %2\$s பாதுகாப்பிற்காக %3\$s எடுத்துச் செல்லுங்கள்.",
            "weather_buddy_trip_avoid" to "முடிந்தால் நாளை %1\$s இல் வெளிப்புறத் திட்டத்தைத் தவிர்க்கவும் அல்லது ஒத்திவைக்கவும். %2\$s பயணம் அவசியமானால் கவனமாகச் சென்று %3\$s எடுத்துச் செல்லுங்கள்.",
            "weather_buddy_trip_tip_rain" to "குடை அல்லது மழைக்கோட்டு",
            "weather_buddy_trip_tip_air" to "நன்றாகப் பொருந்தும் முகக்கவசம்",
            "weather_buddy_trip_tip_fog" to "கூடுதல் பயண நேரம் மற்றும் மெதுவான, கவனமான வாகன ஓட்டம்",
            "weather_buddy_trip_tip_sun" to "சன்ஸ்கிரீன் மற்றும் தொப்பி",
            "weather_buddy_trip_tip_heat" to "தண்ணீர்; அதிக வெப்ப நேரத்தில் ஓய்வு",
            "weather_buddy_trip_tip_none" to "சிறப்பு வானிலை உபகரணம் தேவையில்லை",
            "weather_advice" to "வானிலை ஆலோசனை",
            "weather_advice_evidence" to "காரணம்: %1\$s",
            "weather_advice_show_why" to "இந்த ஆலோசனை ஏன்?",
            "weather_advice_hide_why" to "காரணத்தை மறை",
            "weather_advice_title_family_school_rain" to "குடும்பம் மற்றும் பள்ளிப் பயணம்",
            "weather_advice_title_family_school_umbrella" to "குடும்பப் பயணம்",
            "weather_advice_title_family_school_raincoat" to "குடும்பப் பயணம்",
            "weather_advice_title_family_air_quality" to "குழந்தைகளின் வெளிப்புற நேரம்",
            "weather_advice_title_family_heat" to "குழந்தைகளை குளிர்ச்சியாக வைத்திருங்கள்",
            "weather_advice_title_commuter_umbrella" to "பயண மழைத் திட்டம்",
            "weather_advice_title_commuter_raincoat" to "பயண மழைத் திட்டம்",
            "weather_advice_title_commuter_low_visibility" to "குறைந்த பார்வைத்திறன் பயணம்",
            "weather_advice_title_traveler_low_visibility" to "பயணப் பார்வைத்திறன்",
            "weather_advice_title_traveler_rain" to "பயண மழைத் திட்டம்",
            "weather_advice_title_health_uv" to "சூரிய பாதுகாப்பு",
            "weather_advice_title_health_air_quality" to "காற்றின் தரம்",
            "weather_advice_title_health_pollen" to "மகரந்தக் குறிப்பு",
            "weather_advice_title_fitness_uv" to "வெளிப்புற உடற்பயிற்சி",
            "weather_advice_title_fitness_air_quality" to "வெளிப்புற உடற்பயிற்சி",
            "weather_advice_title_fitness_heat" to "உடற்பயிற்சி நேரம்",
            "weather_advice_title_surfer_dangerous_sea" to "கடல் நிலை",
            "weather_advice_title_surfer_rough_sea" to "கடல் நிலை",
            "weather_advice_title_agriculture_rain" to "மழை மற்றும் வயல் பணி",
            "weather_advice_title_agriculture_irrigation" to "மண் ஈரப்பதம்",
            "weather_advice_title_event_rain_backup" to "வெளிப்புற நிகழ்வு மாற்றுத் திட்டம்",
            "loading_forecast" to "உள்ளூர் வானிலைத் தகவல் ஏற்றப்படுகிறது…",
            "forecast_unavailable" to "வானிலைத் தகவல் கிடைக்கவில்லை",
            "retry" to "மீண்டும் முயற்சி",
            "today" to "இன்று",
            "all_clear" to "இப்போது கவனிக்க வேண்டிய எச்சரிக்கைகள் இல்லை",
            "hourly" to "மணிநேரம்",
            "next_24_hours" to "அடுத்த 24 மணிநேரம்",
            "for_you" to "உங்களுக்காக",
            "seven_day_forecast" to "7 நாள் முன்னறிவிப்பு",
            "settings" to "அமைப்புகள்",
            "alerts" to "எச்சரிக்கைகள்",
            "now" to "இப்போது",
            "top_advice" to "இப்போது முக்கியமானது",
            "personalized_for" to "முன்னுரிமை: %1\$s",
            "current_conditions_missing" to "தற்போதைய நிலை கிடைக்கவில்லை.",
            "today_temperature_range" to "இன்றைய முன்னறிவிப்பு: %1\$d° முதல் %2\$d° வரை",
            "onboarding_step" to "படி %1\$d / 4",
            "onboarding_progress_accessibility" to "தொடக்க அமைவு, படி %1\$d / 4",
            "hourly_forecast_accessibility" to "%1\$s: %2\$d டிகிரி, %3\$s, மழை வாய்ப்பு %4\$d சதவீதம்",
            "forecast_chart_accessibility" to "%1\$s முன்னறிவிப்பு மதிப்புகள்: %2\$s",
            "daily_forecast_accessibility" to "%1\$s: %2\$s, குறைந்தபட்சம் %3\$d டிகிரி, அதிகபட்சம் %4\$d டிகிரி, மழை வாய்ப்பு %5\$d சதவீதம்",
            "select_persona_hint" to "உங்கள் ஆலோசனையைத் தனிப்பயனாக்க குறைந்தது ஒன்றைத் தேர்ந்தெடுக்கவும்.",
            "search_places" to "இடங்களைத் தேடுங்கள்",
            "alerts_step_description" to "இவை விருப்பமான அறிவிப்புகள். இப்போது அனுமதிக்கலாம் அல்லது பின்னர் அமைப்புகளில் இயக்கலாம்.",
            "upcoming_12_hours" to "அடுத்த 12 மணிநேரம்",
            "no_upcoming_alerts" to "அடுத்த 12 மணிநேரத்தில் புதிய எச்சரிக்கைகள் இல்லை",
            "weather_source" to "வானிலைத் தரவு மூலம்",
            "five_day_forecast" to "5 நாள் முன்னறிவிப்பு",
            "live_mode_help" to "Open-Meteo வானிலை, காற்றுத் தரம் மற்றும் கடல் முன்னறிவிப்புகளை வழங்குகிறது. இணையமில்லாமல் பயன்படுத்த கடைசியாகப் பெற்ற பதில்கள் சாதனத்தில் சேமிக்கப்படும்; அவை வழங்குநரின் முன்னறிவிப்பு காலத்திற்குள் மட்டுமே பொருந்தும். மகரந்த அளவுகள் இடத்திற்கான வழங்குநர் தரவுக் கிடைப்பைப் பொறுத்தது.",
            "unavailable" to "கிடைக்கவில்லை",
            "date" to "தேதி",
            "notifications" to "அறிவிப்புகள்",
            "weather_alerts" to "வானிலை எச்சரிக்கைகள்",
            "clothes_alarm" to "துணி உலர்த்தல் மழை எச்சரிக்கை",
            "allow_notifications" to "அறிவிப்புகளை அனுமதி",
            "permission_granted" to "அறிவிப்பு அனுமதி வழங்கப்பட்டது",
            "reset_onboarding" to "தொடக்க அமைப்பை மீட்டமை",
            "your_personas" to "உங்கள் விருப்பங்கள்",
            "choose_advice" to "எந்த ஆலோசனைகள் காட்டப்பட வேண்டும் என்பதைத் தேர்ந்தெடுக்கவும்",
            "location" to "இடம்",
            "change_location" to "முன்னறிவிப்பு இடத்தை மாற்றவும்",
            "upcoming_alerts" to "வரவிருக்கும் எச்சரிக்கைகள்",
            "about" to "பற்றி",
            "about_description" to "வானிலை, காற்றுத் தரம் மற்றும் கடல் முன்னறிவிப்புகள் Open-Meteo வழங்கும் மாதிரி முன்னறிவிப்புகள்; இவை நிலைய அளவீடுகள் அல்ல. இணையம் இல்லாதபோது, சாதனத்தில் சேமித்த அசல் வழங்குநர் பதில்கள் அவற்றின் காலவரம்பிற்குள் பயன்படுத்தப்படும். மகரந்த அளவுகள் வழங்குநர் அவற்றை வழங்கும் இடங்களில் மட்டுமே காட்டப்படும்; காற்றுத் தரம் மகரந்தத்திற்கு மாற்றாகாது. கடல் மட்ட மதிப்புகள் வழிசெலுத்தலுக்கானவை அல்ல.",
            "reset_app" to "பயன்பாட்டை மீட்டமை",
            "reset_description" to "விருப்பங்களை அழித்து தொடக்க அமைப்பை மீண்டும் தொடங்கவும்",
            "language" to "மொழி",
            "english" to "English",
            "tamil" to "தமிழ்",
            "high_contrast" to "உயர் மாறுபாடு",
            "large_text" to "பெரிய எழுத்து",
            "install_tamil_voice" to "தமிழ் குரல் தரவு நிறுவப்படவில்லை",
            "voice_not_available" to "குரல் கிடைக்கவில்லை",
            "install_voice_description" to "Android உரையிலிருந்து பேச்சு அமைப்புகளில் இந்த மொழிக்கான குரலை நிறுவவும்.",
            "install" to "நிறுவு",
            "close" to "மூடு",
            "edit_cards" to "அட்டைகளைத் திருத்து",
            "suggested_cards" to "பரிந்துரைக்கப்பட்ட அட்டைகள்",
            "change_forecast_location" to "முன்னறிவிப்பு இடத்தை மாற்று",
            "listen" to "வாசித்துக் கேள்",
            "see_all_alerts" to "அனைத்து %d எச்சரிக்கைகளையும் காண்க",
            "today_for_you" to "இன்றைய ஆலோசனை",
            "headline:Alerting conditions" to "எச்சரிக்கை நிலை",
            "headline:Heads up today" to "இன்று கவனம் தேவை",
            "headline:Air quality watch" to "காற்றின் தரத்தைக் கவனிக்கவும்",
            "headline:Good conditions for the day" to "இன்றைய வானிலை ஏற்றது",
            "headline:No weather data" to "வானிலைத் தரவு இல்லை",
            "personas_help" to "யாருக்கான ஆலோசனைகள் காட்டப்பட வேண்டும்",
            "location_help" to "முன்னறிவிப்பு இடத்தை மாற்றவும்",
            "notifications_label" to "வானிலை அறிவிப்புகள்",
            "weather_suggestion" to "வானிலைப் பரிந்துரை",
            "smart_action_device_notice" to "பரிந்துரை மட்டும் — ஸ்மார்ட் சாதனம் இணைக்கப்படவில்லை",
            "notifications_help" to "முக்கிய வானிலை எச்சரிக்கைகளைப் பெறவும்",
            "clothes_alarm_help" to "மழை வரும்போது துணிகளை உள்ளே எடுக்கவும்",
            "alerts_help" to "தற்போதைய மற்றும் அடுத்த 12 மணிநேரம்",
            "about_help" to "வானிலை முன்னறிவிப்புகள் மற்றும் தரவு ஆதாரங்கள்",
            "reset_help" to "விருப்பங்களை அழித்து தொடக்க அமைப்பை மீண்டும் தொடங்கவும்",
            "notifications_label" to "வானிலை அறிவிப்புகள்",
            "about_title" to "PersonaWeather பற்றி",
            "done" to "முடிந்தது",
            "back" to "பின்னால்",
            "continue" to "தொடரவும்",
            "get_started" to "தொடங்குங்கள்",
            "maybe_later" to "பிறகு பார்க்கலாம்",
            "finish" to "முடிக்கவும்",
            "welcome_title" to "உங்களுக்கு உதவும் வானிலை",
            "welcome_description" to "வானிலை உதவி தேவைப்படும் விஷயங்களைத் தேர்ந்தெடுக்கவும். முக்கியமான ஆலோசனைகளை முன்னிலைப்படுத்துவோம்.",
            "persona_step_title" to "வானிலை எதில் உதவ வேண்டும்?",
            "persona_step_description" to "ஒன்று அல்லது பலவற்றைத் தேர்ந்தெடுக்கவும். ஆலோசனைகள், எச்சரிக்கைகள் மற்றும் முகப்பு அட்டைகள் இதற்கேற்ப மாறும்.",
            "selected_persona_count" to "%1\$d தேர்வு",
            "personalization_preview_title" to "உங்களுக்காக என்ன மாறும்",
            "personalization_preview_description" to "தொடர்புடைய ஆலோசனைகளையும் எச்சரிக்கைகளையும் முன்னிலைப்படுத்தி, இந்த அட்டைகளுடன் முகப்பைத் தொடங்குவோம். பின்னர் அட்டைகளை மாற்றலாம்.",
            "location_step_title" to "நீங்கள் எங்கு இருக்கிறீர்கள்?",
            "search_localities" to "இடங்களைத் தேடுங்கள்",
            "loading_localities" to "இடங்கள் ஏற்றப்படுகின்றன…",
            "coastal" to "கடற்கரை",
            "location_switch_tip" to "எப்போது வேண்டுமானாலும் இடத்தை மாற்றலாம். மகாபலிபுரம் போன்ற கடற்கரை இடத்தையும் முயற்சிக்கவும்.",
            "alerts_step_title" to "உங்களுக்கு தேவையான எச்சரிக்கைகள்",
            "alerts_step_description" to "நீங்கள் தேர்ந்தெடுத்த விருப்பங்களைப் பாதிக்கும் நிலைகள் வந்தால் எச்சரிப்போம்.",
            "notifications_allowed" to "இந்தச் சாதனத்தில் அறிவிப்புகள் அனுமதிக்கப்பட்டுள்ளன.",
            "selected" to "தேர்ந்தெடுக்கப்பட்டது",
            "persona_description:health_conscious" to "AQI, மகரந்தம், புறஊதா குறியீடு",
            "persona_description:outdoor_fitness" to "ஓட்டத்திற்கான நேரம், வெப்பம், காற்று",
            "persona_description:beach_surfer" to "அலைகள், கடல் மட்டம், கடல் வெப்பம்",
            "persona_description:traveler" to "பனிமூட்டம், மழை, பயணப் பொருட்கள்",
            "persona_description:parent_family" to "பள்ளிப் பயணம், துணி உலர்த்தல்",
            "persona_description:agriculture_gardener" to "மண் ஈரப்பதம், மழை, நீர்ப்பாசனம்",
            "persona_description:commuter" to "மழை, பனிமூட்டம், பயணப் பாதை",
            "persona_description:event_planner" to "வசதி, மழை, நிகழ்ச்சி நாட்கள்",
            "location_title" to "இடத்தைத் தேர்ந்தெடுக்கவும்",
            "search_15_localities" to "15 இடங்களைத் தேடுங்கள்",
            "selected_location" to "தேர்வு",
            "saved_destination_add" to "%1\$s இலக்காகச் சேமிக்கவும்",
            "saved_destination_remove" to "%1\$s சேமித்த இலக்குகளிலிருந்து நீக்கவும்",
            "persona_sheet_title" to "உங்கள் விருப்பங்கள்",
            "persona_sheet_description" to "முன்னறிவிப்பில் காட்ட வேண்டிய அனைத்து கண்ணோட்டங்களையும் தேர்ந்தெடுக்கவும்.",
            "edit_cards_description" to "அட்டைகளை வரிசைப்படுத்தி முகப்பில் எவை தோன்ற வேண்டும் என்பதைத் தேர்ந்தெடுக்கவும்.",
            "coastal_only" to "கடற்கரை இடங்களுக்கு மட்டும்",
            "move_card_up" to "%1\$s அட்டையை மேலே நகர்த்தவும்",
            "move_card_down" to "%1\$s அட்டையை கீழே நகர்த்தவும்",
            "move_to_top" to "முதலில்",
            "reset_card_defaults" to "இயல்புநிலைக்கு மீட்டமை",
            "hour_label" to "மணி · %1\$s:00",
            "offline_trip_pack" to "இணையமில்லா பயண முன்னறிவிப்பு",
            "previous_hour" to "முந்தைய மணி",
            "next_hour" to "அடுத்த மணி",
            "notifications_section" to "அறிவிப்புகள்",
            "weather_alerts" to "வானிலை எச்சரிக்கைகள்",
            "weather_alerts" to "வானிலை எச்சரிக்கைகள்",
            "airplane_mode_note" to "விமானப் பயன்முறையில் சேமித்த முன்னறிவிப்பைச் சரிபார்க்கவும்.",
            "notification_permission_missing" to "அறிவிப்பு அனுமதி வழங்கப்படவில்லை",
            "next" to "அடுத்து",
            "metric_temperature" to "வெப்பம்",
            "metric_rain" to "மழை",
            "metric_wind" to "காற்று",
            "metric_aqi" to "காற்றுத் தரம்",
            "card_aqi_card" to "காற்றுத் தரம்",
            "card_pollen_card" to "மகரந்தம்",
            "card_uv_card" to "புறஊதா குறியீடு",
            "card_humidity_card" to "ஈரப்பதம்",
            "card_running_window_card" to "ஓட்டத்திற்கு சிறந்த நேரம்",
            "card_heat_card" to "வெப்பம்",
            "card_marine_card" to "கடல் நிலை",
            "card_travel_alerts_card" to "பயண எச்சரிக்கைகள்",
            "card_family_commute_card" to "குடும்பப் பயணம்",
            "card_agriculture_card" to "மண் ஈரப்பதம்",
            "card_commute_traffic_card" to "சாலை நிலை",
            "card_event_planning_card" to "நிகழ்ச்சி முன்னறிவிப்பு",
            "card_cloth_drying_card" to "துணி உலர்த்தல்",
            "card_trip_planner_card" to "பயணத் திட்டம்",
            "card_daily_outlook_card" to "தினசரி முன்னறிவிப்பு",
            "trip_planner" to "பயணத் திட்டம்",
            "trip_unavailable" to "பயணத் திட்டம் கிடைக்கவில்லை",
            "sample_route" to "தேர்ந்தெடுக்கப்பட்ட பாதை",
            "route_stops" to "4 நிறுத்தங்கள் · சுமார் 60 நிமிடங்கள்",
            "route_forecast_note" to "பாதை நிறுத்தங்கள் முன்பே அமைக்கப்பட்டவை; வானிலை முன்னறிவிப்புகள் Open-Meteo-விலிருந்து பெறப்படுகின்றன.",
            "offline_trip_status" to "இந்தப் பாதையின் முன்னறிவிப்புகள் ஏற்றப்படும்போதே சாதனத்தில் சேமிக்கப்படும். வழங்குநரின் முன்னறிவிப்பு காலம் முடிந்ததும் இணையமில்லா பயன்பாடு முடிவடையும்.",
            "offline_trip_coverage" to "கிடைக்கும் புறப்பாட்டு நேர வரம்பு: %1\$s முதல் %2\$s வரை",
            "departure" to "புறப்படும் நேரம்",
            "arrive_approximately" to "வருகை சுமார்",
            "route_outlook" to "பாதை முன்னறிவிப்பு",
            "better_time" to "சிறந்த நேரம்: புறப்படுங்கள்",
            "use_this_time" to "இந்த நேரத்தைத் தேர்ந்தெடு",
            "route_map" to "பாதை வரைபடம்",
            "clear" to "தெளிவு",
            "caution" to "கவனம்",
            "alert" to "எச்சரிக்கை",
            "route_locations_missing" to "இந்த பாதைக்கான இடங்கள் தரவில் இல்லை.",
            "stop_by_stop" to "ஒவ்வொரு நிறுத்தமும்",
            "route_exposure" to "பாதை வெளிப்பாடு",
            "dataset_attribution" to "வானிலை, காற்றுத் தரம் மற்றும் கடல் முன்னறிவிப்பு: Open-Meteo",
            "forecast_summary" to "அடுத்த %1\$s: %2\$d%3\$s → %4\$d%3\$s",
            "trip_route_distance" to "சுமார் 10 கிமீ · சுமார் 60 நிமிடங்கள்",
            "preset_thirty_minutes" to "+30 நிமிடம்",
            "preset_one_hour" to "+1 மணி",
            "preset_five_pm" to "மாலை 5 மணி",
            "daily_today" to "இன்று",
            "label_grass" to "புல்",
            "label_tree" to "மரம்",
            "label_ragweed" to "களை",
            "pollen_species_grass" to "புல்",
            "pollen_species_alder" to "ஆல்டர்",
            "pollen_species_birch" to "பிர்ச்",
            "pollen_species_mugwort" to "மக்வார்ட்",
            "pollen_species_olive" to "ஆலிவ்",
            "pollen_species_ragweed" to "ராக்வீட்",
            "pollen_current" to "தற்போது · %1\$s grains/m³",
            "pollen_peak" to "24 மணி உச்சம் · %1\$s grains/m³ · %2\$s",
            "pollen_current_unavailable" to "தற்போதைய அளவு கிடைக்கவில்லை",
            "pollen_peak_unavailable" to "இந்த மகரந்த வகைக்கான முன்னறிவிப்பு இல்லை",
            "pollen_forecast_unavailable" to "இந்த இடத்திற்கு மகரந்த முன்னறிவிப்பு கிடைக்கவில்லை.",
            "route_arrival" to "வருகை சுமார் %1\$s",
            "route_leave_at" to "%1\$s மணிக்குப் புறப்படுங்கள்",
            "route_summary_empty" to "இந்தப் பாதையில் பயண ஆலோசனைகள் எதுவும் இல்லை.",
            "route_summary_rain" to "%1\$d%% மழை வாய்ப்பு · %2\$s இல்",
            "route_summary_air" to "AQI %1\$d · %2\$s இல்",
            "route_summary_pack_umbrella" to "%1\$s அருகே மழை பெய்யக்கூடும். புறப்படுவதற்கு முன் குடையை எடுத்துச் செல்லுங்கள்.",
            "route_summary_pack_raincoat" to "%1\$s அருகே மழை பெய்யலாம். புறப்படுவதற்கு முன் மழைக்கோட்டை எடுத்துச் செல்லுங்கள்.",
            "route_summary_air_caution" to "காற்று மாசுக்கு உணர்திறன் இருந்தால், %1\$s அருகே கடுமையான வெளிப்புறச் செயல்பாட்டைக் குறைக்கவும்.",
            "route_summary_air_alert" to "%1\$s அருகே வெளிப்புறச் செயல்பாட்டைக் குறைத்து, உள்ளரங்க இடங்களைப் பரிசீலிக்கவும்.",
            "route_better_leave" to "முன்னறிவிப்பில் மழை குறிப்பிடப்படவில்லை",
            "route_rain_possible" to "பாதையில் மழை பெய்ய வாய்ப்புள்ளது",
            "route_better_moderate_air" to "காற்றுத் தரம் மிதமானது",
            "route_better_score" to "பாதை ஆலோசனை மதிப்பெண் குறைவு",
            "route_verdict_clear" to "தெளிவு",
            "route_verdict_caution" to "கவனம்",
            "route_verdict_alert" to "எச்சரிக்கை",
            "route_metric_rain" to "மழை %1\$d%%",
            "route_metric_aqi" to "AQI %1\$d",
            "route_metric_visibility" to "பார்வை %1\$d கிமீ",
            "weather:sun" to "தெளிவான வானம்",
            "weather:clear" to "தெளிவான வானம்",
            "weather:cloud" to "மேகமூட்டம்",
            "weather:rain" to "மழை",
            "weather:storm" to "இடியுடன் மழை",
            "weather:fog" to "பனிமூட்டம்",
            "weather:wind" to "காற்று",
            "weather:heat" to "வெப்பம்",
            "weather:wave" to "கடல் நிலை",
            "weather:weather" to "வானிலை",
            "severity_good" to "நன்று",
            "severity_caution" to "கவனம்",
            "severity_alert" to "எச்சரிக்கை",
            "around_time" to "%1\$s மணியளவில்",
            "check_event_weather" to "இந்த நிகழ்வின் வானிலையைப் பார்க்கவும்",
            "event_forecast_place_context" to "%1\$s முன்னறிவிப்பு · %2\$s · %3\$s · %4\$s",
            "event_role_prompt" to "நிகழ்வில் உங்கள் பங்கு என்ன?",
            "event_role_organizer" to "ஏற்பாட்டாளர்",
            "event_role_non_organizer" to "பங்கேற்பாளர்",
            "during_event" to "நிகழ்வு நேரத்தில்",
            "event_weather_summary" to "அதிகபட்சம் %1\$.0f° · மழை வாய்ப்பு %2\$.0f%% வரை · அதிகபட்ச AQI %3\$d (%4\$s)",
            "event_indoor_air_summary" to "வெளிப்புற AQI முன்னறிவிப்பு: %1\$d (%2\$s)",
            "event_indoor_air_caveat" to "வெளிப்புற காற்றுத் தர மதிப்பீடு மட்டுமே; உட்புறக் காற்று மாறுபடலாம்.",
            "event_no_configured_advice" to "இந்த நிகழ்வு நேரத்தில் விதிகளின்படி ஆலோசனை எதுவும் இல்லை.",
            "event_planner_persona_needed" to "நிகழ்ச்சி ஏற்பாட்டாளர்களுக்கான ஆலோசனைகளைப் பெற, முகப்பிலிருந்து நிகழ்ச்சி திட்டமிடுபவர் நபர்தன்மையை இயக்கவும்.",
            "event_weather_context_title" to "இந்த நிகழ்வின் வானிலையைப் பார்க்கவும்",
            "event_weather_context_prompt" to "நிகழ்வு வெளிப்புறமா என்பதைத் தேர்ந்தெடுத்து, முன்னறிவிப்பு இடத்தை உறுதிப்படுத்தவும். காலண்டர் முகவரி இந்தச் சாதனத்திலேயே இருக்கும்.",
            "outdoor" to "வெளிப்புறம்",
            "indoor" to "உட்புறம்",
            "event_location" to "நிகழ்வு இடம்",
            "event_location_pick_verified" to "பொருந்தும் சேமித்த முன்னறிவிப்பு இடத்தைத் தேர்ந்தெடுக்கவும். முழு முகவரியும் தேடல் சேவைக்கு அனுப்பப்படாது.",
            "analyze_event_weather" to "ஆலோசனையைக் காட்டு",
            "all_day_event_time" to "முழு நாள் · %1\$s",
            "all_day_time_range" to "முழு நாள் · %1\$s முதல் %2\$s வரை",
            "event_time_range" to "%1\$s முதல் %2\$s வரை",
            "daily_forecast" to "7 நாள் முன்னறிவிப்பு",
            "dew_point_value" to "பனிப்புள்ளி %1\$d° · %2\$s",
            "next_six_hours" to "அடுத்த 6 மணி %1\$d–%2\$d%%",
            "running_conditions" to "AQI %1\$d · %2\$d° · புறஊதா %3\$d",
            "peak_feels_like" to "அதிகபட்சம் %1\$d° · அடுத்த 24 மணி",
            "current_feels_like" to "தற்போதைய உணரப்படும் வெப்பநிலை",
            "sunrise" to "சூரிய உதயம்",
            "sunset" to "சூரிய அஸ்தமனம்",
            "sun_times_unavailable" to "சூரிய உதயம் மற்றும் மறைவு நேரங்கள் கிடைக்கவில்லை",
            "inland_marine_unavailable" to "உள்நாட்டு இடங்களில் கடல் நிலை கிடைக்காது.",
            "wave_height_value" to "அலை உயரம் · %1$.1f மீ",
            "wave_forecast_unavailable" to "அலை உயர முன்னறிவிப்பு கிடைக்கவில்லை",
            "tide_forecast_notice" to "அலை நேரங்கள் மணிநேர முன்னறிவிப்பு மதிப்பீடுகள்; வழிசெலுத்தலுக்கானவை அல்ல.",
            "tide_times_unavailable" to "இந்த முன்னறிவிப்பில் அலை நேரங்கள் கிடைக்கவில்லை",
            "marine_details" to "நீர் %1\$s° · அலை இடைவெளி %2\$s வி · %3\$s",
            "high_tide" to "ஏற்ற அலை",
            "low_tide" to "இறங்கு அலை",
            "packing_suggestions" to "எடுத்துச் செல்ல வேண்டியவை",
            "event_comfort_score" to "வெளிப்புற வசதி %1\$d/100 · %2\$s",
            "event_comfort_factors" to "மழை %1\$d%% · AQI %2\$d · %3\$s",
            "event_comfort_breakdown" to "மதிப்பெண் குறைவு: மழை -%1\$d · AQI -%2\$d · வெப்பநிலை -%3\$d",
            "frost_risk" to "உறைபனி அபாயம்",
            "cold_conditions" to "குளிர்",
            "heat_stress" to "வெப்பம்",
            "rain_chance_six_hours" to "அடுத்த 6 மணி மழை வாய்ப்பு: %1\$d%%",
            "morning_rain" to "பள்ளிக்குச் செல்லுதல் · %1\$s · %2\$s நேரத்தில் மழை வாய்ப்பு அதிகம் · %3\$d%%",
            "pickup_rain" to "பள்ளியிலிருந்து அழைத்துவருதல் · %1\$s · %2\$s நேரத்தில் மழை வாய்ப்பு அதிகம் · %3\$d%%",
            "commute_morning_window" to "காலை 7–9 பள்ளிப் பயண நேரம் முழுவதும்",
            "commute_pickup_window" to "மாலை 4–6 பள்ளியிலிருந்து அழைத்துவரும் நேரம் முழுவதும்",
            "commute_today" to "இன்று",
            "commute_tomorrow" to "நாளை",
            "commute_pack_umbrella" to "குடை எடுத்துச் செல்லுங்கள்",
            "commute_pack_raincoat" to "மழைக்கோட்டை எடுத்துச் செல்லுங்கள்",
            "commute_no_rain_gear" to "இந்த முன்னறிவிப்பில் மழைக்கான பொருள் தேவையெனத் தெரியவில்லை",
            "morning_forecast_unavailable" to "காலைப் பயண முன்னறிவிப்பு கிடைக்கவில்லை",
            "pickup_forecast_unavailable" to "மாலைப் பயண முன்னறிவிப்பு கிடைக்கவில்லை",
            "soil_moisture" to "மண் ஈரப்பதம் %1\$d%%",
            "tomorrow_soil_rain" to "நாளை மண் %1\$d%% · அடுத்த 24 மணி மழை %2\$d மிமீ",
            "soil_rain" to "தினசரி மண் ஈரப்பதம் · அடுத்த 24 மணி மழை %1\$d மிமீ",
            "roads_status" to "சாலை: %1\$s",
            "wet" to "ஈரம்",
            "road_clear" to "உடனடி மழை அல்லது பார்வை எச்சரிக்கை இல்லை",
            "road_rain_indicated" to "அடுத்த 2 மணி நேரத்தில் மழை முன்னறிவிக்கப்படுகிறது",
            "rain_forecast_conflict" to "மழை காட்டப்படுகிறது; ஆனால் அதன் வாய்ப்பு மதிப்பீடு இதனுடன் பொருந்தவில்லை.",
            "rain_next_two_hours" to "அடுத்த 2 மணி நேர மழை வாய்ப்பு · அதிகபட்சம் %1\$d%%",
            "uv_current_value" to "தற்போது · %1\$s",
            "uv_peak_today" to "பகல் UV அதிகபட்சம் சுமார் %1\$s · %2\$s",
            "uv_peak_tomorrow" to "நாளைய பகல் UV அதிகபட்சம் சுமார் %1\$s · %2\$s",
            "uv_forecast_unavailable" to "பகல் UV முன்னறிவிப்பு கிடைக்கவில்லை",
            "marine_state" to "%1\$s · அலை உயரம் %2\$s மீ",
            "marine_state_forecast" to "%1\$s · %2\$s · அலை உயரம் %3\$s மீ",
            "marine_forecast_unavailable" to "கடல் முன்னறிவிப்பு கிடைக்கவில்லை",
            "marine_safety_rough" to "கடலில் இறங்கும் முன் உள்ளூர் உயிர்காப்பாளரின் அறிவுரையைப் பாருங்கள்.",
            "marine_safety_general" to "கடல் முன்னறிவிப்பு ஒரு மதிப்பீடு மட்டுமே; நீர் விளையாட்டுக்கு முன் உள்ளூர் கடற்கரை மற்றும் உயிர்காப்பாளர் அறிவுரையைப் பாருங்கள்.",
            "marine_peak_today" to "இன்றைய அதிகபட்ச அலைகள் சுமார் %1\$s · %2\$s",
            "marine_peak_tomorrow" to "நாளைய அதிகபட்ச அலைகள் சுமார் %1\$s · %2\$s",
            "packing_suggestions_next_day" to "அடுத்த 24 மணிநேரத்திற்கான தயாரிப்பு",
            "packing_tip_sun_protection" to "சன்ஸ்கிரீன் மற்றும் தொப்பி",
            "packing_tip_umbrella" to "குடை",
            "packing_tip_raincoat" to "மழைக்கோட்டு",
            "packing_tip_water" to "தண்ணீர் பாட்டில்",
            "packing_tip_air_quality" to "காற்று மாசுக்கு உணர்திறன் இருந்தால் நன்றாகப் பொருந்தும் முகக்கவசம்",
            "packing_none" to "கூடுதல் வானிலை உபகரணம் தேவையில்லை.",
            "rain_none" to "அடுத்த 24 மணிநேரத்தில் மழை எதிர்பார்க்கப்படவில்லை",
            "rain_now" to "இப்போது மழை",
            "rain_easing" to "இப்போது மழை; %1\$s மணியளவில் குறையும்",
            "rain_expected" to "%1\$s மணியளவில் மழை எதிர்பார்க்கப்படுகிறது (%2\$s%%)",
            "rain_uv_aqi" to "மழை %1\$d%% · புறஊதா %2\$d · AQI %3\$d",
            "no_drying_window" to "3 மணி நேர உலர் இடைவெளி இல்லை",
            "try_later_forecast" to "முன்னறிவிப்பில் பின்னர் மீண்டும் பார்க்கவும்",
            "drying_humidity" to "உலர்த்தும் நேரம் · ஈரப்பதம் %1\$d%%",
            "route_card" to "பயண வானிலை",
            "route_card_details" to "உங்கள் பயண வழியின் ஒவ்வொரு நிறுத்தத்திற்குமான நிலை",
            "open_route_forecast" to "பாதை முன்னறிவிப்பைத் திறக்கவும் →",
            "best_day" to "சிறந்த நாள்: %1\$s",
            "comfort_value" to "வசதி %1\$s",
            "aqi_value" to "AQI %1\$d",
            "rain_value" to "மழை %1\$d%%",
            "visibility_value" to "பார்வை %1\$d கிமீ",
            "average_aqi" to "சராசரி காற்றுத் தரம்",
            "rain_stops" to "மழை வாய்ப்புள்ள நிறுத்தங்கள்",
            "no_route_advisories" to "இந்தப் பாதையில் பயண ஆலோசனைகள் எதுவும் இல்லை.",
            "route_rain_summary" to "%1\$d%% மழை · %2\$s",
            "route_aqi_summary" to "AQI %1\$d · %2\$s",
            "route_better_dry" to "அனைத்து நிறுத்தங்களிலும் மழையில்லை",
            "route_better_air" to "காற்றுத் தரம் மிதமானது",
            "route_better_score" to "பாதை ஆலோசனை மதிப்பெண் குறைவு",
            "route_home_office" to "வீடு → அலுவலகம்",
            "route_label_health" to "உடல்நலம்",
            "route_label_fitness" to "உடற்பயிற்சி",
            "route_label_family" to "குடும்பம்",
            "route_label_event" to "நிகழ்ச்சி",
            "route_label_commuter" to "பயணி",
            "route_label_traveler" to "பயணி",
            "route_label_beach" to "கடற்கரை",
            "route_label_gardener" to "தோட்டம்",
            "route_clear" to "தெளிவு",
            "route_verdict_caution" to "கவனம்",
            "route_verdict_alert" to "எச்சரிக்கை",
            "hour_earlier" to "15 நிமிடம் முன்னதாக",
            "hour_later" to "15 நிமிடம் தாமதமாக",
            "average_route_aqi" to "பாதையின் சராசரி AQI",
            "route_stops_rain" to "மழை ≥ 50% உள்ள நிறுத்தங்கள்",
            "condition:Sunny" to "வெயில்",
            "condition:Clear" to "தெளிவான வானம்",
            "condition:Partly cloudy" to "ஓரளவு மேகமூட்டம்",
            "condition:Scattered clouds, chance of rain" to "மேகங்கள், மழைக்கு வாய்ப்பு",
            "condition:Showers" to "மழை",
            "condition:Thunderstorm" to "இடியுடன் மழை",
            "term:Good" to "நன்று",
            "term:Fair" to "பரவாயில்லை",
            "term:Poor" to "மோசம்",
            "term:Moderate" to "மிதமானது",
            "term:Unhealthy" to "ஆரோக்கியமற்றது",
            "term:Sensitive" to "உணர்திறன் உள்ளவர்களுக்கு பாதிப்பு",
            "term:Very unhealthy" to "மிகவும் ஆரோக்கியமற்றது",
            "term:Low" to "குறைவு",
            "term:High" to "அதிகம்",
            "term:Extreme" to "மிக அதிகம்",
            "term:Dry" to "வறண்டது",
            "term:Comfortable" to "சௌகரியம்",
            "term:Humid" to "ஈரப்பதம் அதிகம்",
            "term:Muggy" to "புழுக்கம்",
            "term:Calm" to "அமைதி",
            "term:Rough" to "கொந்தளிப்பு",
            "term:Dangerous" to "ஆபத்து",
            "persona_health_conscious" to "உடல்நல அக்கறை",
            "persona_outdoor_fitness" to "வெளிப்புற உடற்பயிற்சி",
            "persona_beach_surfer" to "கடற்கரை மற்றும் சர்ஃபிங்",
            "persona_traveler" to "பயணி",
            "persona_parent_family" to "பெற்றோர் மற்றும் குடும்பம்",
            "persona_agriculture_gardener" to "விவசாயம் மற்றும் தோட்டம்",
            "persona_commuter" to "தினசரி பயணம்",
            "persona_event_planner" to "நிகழ்ச்சி திட்டமிடல்",
            "hottest_around" to "%1\$s நேரத்தில் அதிக வெப்பம் (%2\$d°)",
            "route_home_office" to "வீடு → அலுவலகம்",
            "hour_earlier" to "15 நிமிடம் முன்னதாக",
            "hour_later" to "15 நிமிடம் தாமதமாக",
            "run_not_recommended" to "இன்று வெளியில் ஓட பரிந்துரைக்கப்படவில்லை",
            "run_no_clear_best" to "இந்த முன்னறிவிப்பில் ஓடுவதற்கான தெளிவான சிறந்த நேரம் இல்லை",
            "run_window_unavailable" to "ஓட்ட நேரம் கணிக்க போதிய தரவு இல்லை",
            "route_map_description" to "பாதை நிறுத்தங்கள்: %1\$s",
            "high_low_feels_like" to "H: %1\$d° · L: %2\$d° · உணரப்படும் வெப்பநிலை %3\$d°",
            "widget_refresh" to "புதுப்பி",
            "widget_weather_unavailable" to "வானிலை கிடைக்கவில்லை",
            "widget_open_app_retry" to "மீண்டும் முயற்சிக்க PersonaWeather ஐத் திறக்கவும்",
            "widget_alert" to "எச்சரிக்கை",
            "widget_caution" to "கவனம்",
            "widget_aqi" to "AQI",
            "widget_feels_like" to "உணரப்படுகிறது",
            "widget_all_clear" to "இப்போது கவனிக்க வேண்டிய எதுவும் இல்லை",
            "widget_expand_hourly" to "மணிநேர முன்னறிவிப்பைப் பார்க்க விரிவாக்கு",
            "widget_choose_personas" to "தனிப்பயனாக்கப்பட்ட ஆலோசனைகளுக்கு ஒன்று அல்லது அதற்கு மேற்பட்ட சுயவிவரங்களைத் தேர்ந்தெடுக்கவும்",
            "widget_no_advice" to "தற்போது செயலில் எந்த ஆலோசனையும் இல்லை",
            "widget_more_selected" to "+%1\$d மேலும் தேர்ந்தெடுக்கப்பட்டது",
        )

        private val hindiStrings = mapOf(
            "weather_buddy_open" to "WeatherBuddy चैट खोलें",
            "weather_buddy_entry" to "मौसम पूछें",
            "weather_buddy_title" to "WeatherBuddy",
            "weather_buddy_close" to "चैट बंद करें",
            "weather_buddy_subtitle" to "अपने पूर्वानुमान को आसानी से समझें",
            "weather_buddy_intro" to "नमस्ते! मैं %1\$s के पूर्वानुमान को समझने में मदद करूँगा। आप क्या जानना चाहेंगे?",
            "weather_buddy_input" to "अपना सवाल लिखें",
            "weather_buddy_send" to "संदेश भेजें",
            "weather_buddy_suggestions" to "यह पूछकर देखें",
            "weather_buddy_typing" to "मौसम सहायक जवाब लिख रहा है",
            "weather_buddy_prompt_current" to "अभी मौसम कैसा है?",
            "weather_buddy_prompt_rain" to "क्या जल्द बारिश होगी?",
            "weather_buddy_prompt_air" to "हवा की गुणवत्ता कैसी है?",
            "weather_buddy_prompt_trip" to "क्या मैं कल बाहर जा सकता हूँ?",
            "weather_buddy_prompt_location" to "स्थान कहाँ बदलूँ?",
            "weather_buddy_current_reply" to "%1\$s में अभी का मौसम: %2\$s, %3$.0f°C (महसूस %4$.0f°C होता है)।",
            "weather_buddy_current_unavailable" to "अभी मौसम की जानकारी उपलब्ध नहीं है।",
            "weather_buddy_rain_reply" to "%1\$s के उपलब्ध पूर्वानुमान में बारिश की सबसे अधिक संभावना %3\$s के आसपास %2$.0f%% है।",
            "weather_buddy_rain_unavailable" to "बारिश का पूर्वानुमान अभी उपलब्ध नहीं है।",
            "weather_buddy_air_reply" to "%1\$s में हवा की गुणवत्ता %2\$s है (AQI %3\$d)।",
            "weather_buddy_air_unavailable" to "हवा की गुणवत्ता की जानकारी अभी उपलब्ध नहीं है।",
            "weather_buddy_no_alerts" to "इस स्थान के लिए अभी कोई सक्रिय मौसम चेतावनी नहीं है।",
            "weather_buddy_location_help" to "दूसरी जगह चुनने के लिए Home स्क्रीन के ऊपर दिख रहे स्थान के नाम पर टैप करें।",
            "weather_buddy_help" to "स्थानीय मौसम, बारिश की संभावना, हवा की गुणवत्ता या अलर्ट पूछें। Home स्क्रीन के ऊपर स्थान बदलें।",
            "weather_buddy_fallback" to "मैं स्थानीय पूर्वानुमान, हवा की गुणवत्ता, अलर्ट और ऐप के उपयोग में मदद कर सकता हूँ।",
            "weather_buddy_keywords_location" to "स्थान|शहर|जगह",
            "weather_buddy_keywords_change" to "बदल|कहाँ",
            "weather_buddy_keywords_help" to "मदद|कैसे",
            "weather_buddy_keywords_rain" to "बारिश|छाता",
            "weather_buddy_keywords_air" to "हवा|AQI|प्रदूषण",
            "weather_buddy_keywords_alert" to "अलर्ट|चेतावनी|बाहर जाने",
            "weather_buddy_keywords_current" to "मौसम|तापमान|स्थिति|बाहर|पहन",
            "weather_buddy_keywords_tomorrow" to "कल",
            "weather_buddy_keywords_trip" to "जाना|जाऊँ|यात्रा|बाहर",
            "weather_buddy_trip_unavailable" to "कल के लिए सुरक्षित हाँ/नहीं बताने हेतु पूर्वानुमान उपलब्ध नहीं है। निकलने से पहले मौसम फिर जाँचें।",
            "weather_buddy_trip_location_scope" to "%1\$s की स्थिति की पुष्टि नहीं कर सकता; यहाँ %2\$s का पूर्वानुमान उपलब्ध है। स्थान चुनने वाले मेनू से %1\$s चुनें और फिर पूछें।",
            "weather_buddy_trip_conditions" to "अधिकतम बारिश की संभावना %1\$.0f%%, अधिकतम AQI %2\$d, न्यूनतम दृश्यता %3\$.1f किमी, अधिकतम UV %4\$.0f, और अधिकतम तापमान %5\$.0f°C।",
            "weather_buddy_trip_clear" to "हाँ — कल %1\$s जाना मौसम के अनुसार ठीक लग रहा है। %2\$s कोई बड़ा मौसम जोखिम नहीं दिख रहा; फिर भी %3\$s साथ रखें।",
            "weather_buddy_trip_caution" to "कल %1\$s जा सकते हैं, लेकिन मौसम मिला-जुला है। %2\$s सुरक्षा के लिए %3\$s साथ रखें।",
            "weather_buddy_trip_avoid" to "हो सके तो कल %1\$s में बाहर की योजना टालें। %2\$s जाना ज़रूरी हो तो सावधानी रखें और %3\$s साथ रखें।",
            "weather_buddy_trip_tip_rain" to "छाता या रेनकोट",
            "weather_buddy_trip_tip_air" to "अच्छी तरह फिट होने वाला मास्क",
            "weather_buddy_trip_tip_fog" to "अतिरिक्त यात्रा समय रखें और धीरे, सावधानी से चलाएँ",
            "weather_buddy_trip_tip_sun" to "सनस्क्रीन और टोपी",
            "weather_buddy_trip_tip_heat" to "पानी और सबसे गर्म समय में विश्राम",
            "weather_buddy_trip_tip_none" to "विशेष मौसम-संबंधी सामान की ज़रूरत नहीं",
            "weather_advice" to "मौसम सलाह",
            "weather_advice_evidence" to "कारण: %1\$s",
            "weather_advice_show_why" to "यह सलाह क्यों?",
            "weather_advice_hide_why" to "कारण छिपाएँ",
            "weather_advice_title_family_school_rain" to "परिवार और स्कूल यात्रा",
            "weather_advice_title_family_school_umbrella" to "परिवार की यात्रा",
            "weather_advice_title_family_school_raincoat" to "परिवार की यात्रा",
            "weather_advice_title_family_air_quality" to "बच्चों का बाहरी समय",
            "weather_advice_title_family_heat" to "बच्चों को ठंडा रखें",
            "weather_advice_title_commuter_umbrella" to "आवागमन की बारिश योजना",
            "weather_advice_title_commuter_raincoat" to "आवागमन की बारिश योजना",
            "weather_advice_title_commuter_low_visibility" to "कम दृश्यता में यात्रा",
            "weather_advice_title_traveler_low_visibility" to "यात्रा दृश्यता",
            "weather_advice_title_traveler_rain" to "यात्रा की बारिश योजना",
            "weather_advice_title_health_uv" to "धूप से बचाव",
            "weather_advice_title_health_air_quality" to "हवा की गुणवत्ता",
            "weather_advice_title_health_pollen" to "पराग सूचना",
            "weather_advice_title_fitness_uv" to "बाहरी व्यायाम",
            "weather_advice_title_fitness_air_quality" to "बाहरी व्यायाम",
            "weather_advice_title_fitness_heat" to "व्यायाम का समय",
            "weather_advice_title_surfer_dangerous_sea" to "समुद्र की स्थिति",
            "weather_advice_title_surfer_rough_sea" to "समुद्र की स्थिति",
            "weather_advice_title_agriculture_rain" to "बारिश और खेत का काम",
            "weather_advice_title_agriculture_irrigation" to "मिट्टी की नमी",
            "weather_advice_title_event_rain_backup" to "बाहरी कार्यक्रम का वैकल्पिक इंतज़ाम",
            "loading_forecast" to "मौसम डेटा लोड हो रहा है…",
            "forecast_unavailable" to "मौसम उपलब्ध नहीं है",
            "retry" to "पुनः प्रयास करें",
            "today" to "आज",
            "all_clear" to "अभी कोई कार्रवाई आवश्यक नहीं है",
            "hourly" to "प्रति घंटा",
            "next_24_hours" to "अगले 24 घंटे",
            "for_you" to "आपके लिए",
            "seven_day_forecast" to "7-दिवसीय पूर्वानुमान",
            "settings" to "सेटिंग्स",
            "alerts" to "अलर्ट",
            "now" to "अभी",
            "top_advice" to "अभी क्या महत्वपूर्ण है",
            "personalized_for" to "%1\$s के लिए प्राथमिकता",
            "current_conditions_missing" to "वर्तमान मौसम जानकारी उपलब्ध नहीं है।",
            "today_temperature_range" to "आज का पूर्वानुमान: %1\$d° से %2\$d°",
            "onboarding_step" to "चरण %1\$d / 4",
            "onboarding_progress_accessibility" to "शुरुआती सेटअप, चरण %1\$d / 4",
            "hourly_forecast_accessibility" to "%1\$s: %2\$d डिग्री, %3\$s, बारिश की संभावना %4\$d प्रतिशत",
            "forecast_chart_accessibility" to "%1\$s पूर्वानुमान मान: %2\$s",
            "daily_forecast_accessibility" to "%1\$s: %2\$s, न्यूनतम %3\$d डिग्री, अधिकतम %4\$d डिग्री, बारिश की संभावना %5\$d प्रतिशत",
            "select_persona_hint" to "सलाह को आपके लिए उपयोगी बनाने के लिए कम से कम एक विकल्प चुनें।",
            "search_places" to "स्थान खोजें",
            "alerts_step_description" to "सूचनाएँ वैकल्पिक हैं। अभी अनुमति दें या बाद में सेटिंग्स में चालू करें।",
            "upcoming_12_hours" to "अगले 12 घंटे",
            "no_upcoming_alerts" to "अगले 12 घंटों में कोई नया अलर्ट नहीं है",
            "weather_source" to "मौसम स्रोत",
            "five_day_forecast" to "5-दिवसीय पूर्वानुमान",
            "language" to "भाषा",
            "english" to "English",
            "hindi" to "हिन्दी",
            "tamil" to "தமிழ்",
            "high_contrast" to "उच्च कंट्रास्ट",
            "large_text" to "बड़ा टेक्स्ट",
            "voice_not_available" to "भाषा आवाज़ उपलब्ध नहीं है",
            "install_voice_description" to "Android की बोलने की सेटिंग्स में इस भाषा की आवाज़ इंस्टॉल करें।",
            "listen" to "सुनें",
            "edit_cards" to "कार्ड बदलें",
            "edit_cards_description" to "कार्ड क्रम बदलें और होम पर कौन-सा दिखे चुनें।",
            "suggested_cards" to "सुझाए गए कार्ड",
            "done" to "हो गया",
            "location_title" to "स्थान चुनें",
            "search_india_places" to "भारत के शहर और इलाकों में खोजें",
            "searching_places" to "स्थान खोजे जा रहे हैं…",
            "major_indian_cities" to "मुख्य भारतीय शहर",
            "saved_localities" to "सहेजे गए और पास के स्थान",
            "coastal" to "तटीय",
            "selected_location" to "चुना गया",
            "live_forecast" to "लाइव पूर्वानुमान",
            "saved_destination_add" to "%1\$s को सेव करें",
            "saved_destination_remove" to "%1\$s को सेव्ड सूची से हटाएँ",
            "persona_sheet_title" to "आपकी प्रोफ़ाइल",
            "persona_sheet_description" to "अपने मौसम दृश्य चुनें।",
            "persona_step_title" to "मौसम में किस बात के लिए मदद चाहिए?",
            "persona_step_description" to "एक या अधिक विकल्प चुनें। सलाह, अलर्ट और होम कार्ड उसी के अनुसार बदलेंगे।",
            "selected_persona_count" to "%1\$d चुने गए",
            "personalization_preview_title" to "आपके लिए क्या बदलेगा",
            "personalization_preview_description" to "हम आपके काम की सलाह और अलर्ट को प्राथमिकता देंगे और होम पर ये कार्ड दिखाएँगे। कार्ड बाद में बदल सकते हैं।",
            "choose_location" to "स्थान चुनें",
            "choose_a_location" to "स्थान चुनें",
            "move_card_up" to "%1\$s को ऊपर ले जाएँ",
            "move_card_down" to "%1\$s को नीचे ले जाएँ",
            "move_to_top" to "ऊपर",
            "reset_card_defaults" to "डिफ़ॉल्ट पर रीसेट करें",
            "coastal_only" to "केवल तटीय स्थान",
            "upcoming_events" to "आगामी कार्यक्रम",
            "check_event_weather" to "कार्यक्रम का मौसम देखें",
            "event_forecast_place_context" to "%1\$s पूर्वानुमान · %2\$s · %3\$s · %4\$s",
            "event_role_prompt" to "कार्यक्रम में आपकी भूमिका क्या है?",
            "event_role_organizer" to "आयोजक",
            "event_role_non_organizer" to "गैर-आयोजक",
            "during_event" to "कार्यक्रम के दौरान",
            "event_weather_summary" to "अधिकतम %1\$.0f° · बारिश की संभावना %2\$.0f%% तक · अधिकतम AQI %3\$d (%4\$s)",
            "event_indoor_air_summary" to "बाहरी AQI पूर्वानुमान: %1\$d (%2\$s)",
            "event_indoor_air_caveat" to "केवल बाहरी वायु गुणवत्ता का अनुमान; अंदर की हवा अलग हो सकती है।",
            "event_no_configured_advice" to "कार्यक्रम के दौरान कोई निर्धारित सलाह सक्रिय नहीं हुई।",
            "event_planner_persona_needed" to "आयोजकों के लिए सुझाव पाने हेतु होम स्क्रीन से Event Planner व्यक्तित्व चालू करें।",
            "event_weather_context_title" to "इस कार्यक्रम का मौसम देखें",
            "event_weather_context_prompt" to "बताएँ कि कार्यक्रम बाहर है या नहीं, फिर पूर्वानुमान स्थान चुनें। कैलेंडर का पता इसी डिवाइस पर रहता है।",
            "outdoor" to "बाहर",
            "indoor" to "अंदर",
            "event_location" to "कार्यक्रम का स्थान",
            "event_location_pick_verified" to "मेल खाता सहेजा हुआ पूर्वानुमान स्थान चुनें। पूरा पता खोज सेवा को नहीं भेजा जाता।",
            "analyze_event_weather" to "सलाह दिखाएँ",
            "refresh" to "रिफ्रेश",
            "calendar_access_off" to "कैलेंडर एक्सेस बंद है",
            "allow_calendar_access" to "कैलेंडर एक्सेस की अनुमति दें",
            "loading_your_next_events" to "आपके अगले कार्यक्रम लोड हो रहे हैं…",
            "no_upcoming_events_30_days" to "अगले 30 दिनों में कोई आगामी कार्यक्रम नहीं है।",
            "outdoor_question" to "बाहरी?",
            "allow_access_to_read_calendars" to "Google और अन्य सिंक किए गए कैलेंडर्स को पढ़ने की अनुमति दें ताकि PersonaWeather आपके अगले इवेंट के लिए मौसम सलाह दे सके।",
            "back_to_settings" to "सेटिंग्स पर वापस",
            "choose_personas_for_widget" to "एक या अधिक प्रोफ़ाइल चुनें",
            "widget_personalize_title" to "इस विजेट को अपने अनुसार बनाएं",
            "widget_focus_type" to "विजेट प्रकार",
            "widget_locality" to "स्थान",
            "widget_personas_for_this_widget" to "इस विजेट के लिए प्रोफ़ाइल",
            "widget_save" to "विजेट सेव करें",
            "widget_cancel" to "रद्द करें",
            "widget_choose_at_least_one_persona" to "कम से कम एक प्रोफ़ाइल चुनें",
            "widget_unable_to_load_localities" to "स्थान लोड नहीं किए जा सके",
            "widget_personalize_subtitle" to "इसके फोकस, स्थान और उन प्रोफ़ाइल चुनें जिनकी सलाह दिखाई देनी है।",
            "persona_health_conscious" to "स्वास्थ्य-जागरूक",
            "persona_outdoor_fitness" to "आउटडोर फिटनेस",
            "persona_beach_surfer" to "तट और सर्फ़िंग",
            "persona_traveler" to "यात्री",
            "persona_parent_family" to "अभिभावक और परिवार",
            "persona_agriculture_gardener" to "कृषि और बगीचा",
            "persona_commuter" to "दैनिक सफ़र",
            "persona_event_planner" to "इवेंट प्लानिंग",
            "persona_description:health_conscious" to "AQI, पराग, UV",
            "persona_description:outdoor_fitness" to "सबसे अच्छा दौड़ समय, गर्मी, हवा",
            "persona_description:beach_surfer" to "लहरें, ज्वार, समुद्र का तापमान",
            "persona_description:traveler" to "कोहरा, बारिश, पैकिंग सुझाव",
            "persona_description:parent_family" to "स्कूल सफ़र, कपड़े सुखाना",
            "persona_description:agriculture_gardener" to "मिट्टी की नमी और सिंचाई",
            "persona_description:commuter" to "बारिश, कोहरा और रास्ते का मौसम",
            "persona_description:event_planner" to "आराम, बारिश और इवेंट दिन",
            "headline:Alerting conditions" to "चेतावनी वाले हालात",
            "headline:Heads up today" to "आज सावधान रहिए",
            "headline:Air quality watch" to "हवा की गुणवत्ता पर नज़र",
            "headline:Good conditions for the day" to "आज का मौसम अच्छा है",
            "headline:No weather data" to "मौसम डेटा उपलब्ध नहीं है",
            "metric_temperature" to "तापमान",
            "metric_rain" to "बारिश",
            "metric_wind" to "हवा",
            "metric_aqi" to "AQI",
            "card_aqi_card" to "वायु गुणवत्ता",
            "card_pollen_card" to "पराग",
            "card_uv_card" to "यूवी",
            "card_humidity_card" to "नमी",
            "card_running_window_card" to "दौड़ने का अच्छा समय",
            "card_heat_card" to "गर्मी",
            "card_marine_card" to "समुद्र की स्थिति",
            "card_travel_alerts_card" to "यात्रा अलर्ट",
            "card_agriculture_card" to "मिट्टी की नमी",
            "card_event_planning_card" to "कार्यक्रम की योजना",
            "term:Good" to "अच्छा",
            "term:Fair" to "उचित",
            "term:Moderate" to "मध्यम",
            "term:Poor" to "बुरा",
            "term:Sensitive" to "संवेदनशील",
            "term:Unhealthy" to "अस्वस्थ",
            "term:Very unhealthy" to "बहुत अस्वस्थ",
            "term:Low" to "कम",
            "term:High" to "उच्च",
            "term:Very high" to "बहुत उच्च",
            "term:Extreme" to "अत्यधिक",
            "term:Dry" to "शुष्क",
            "term:Comfortable" to "आरामदायक",
            "term:Humid" to "आर्द्र",
            "term:Muggy" to "अत्यधिक नम",
            "term:Calm" to "शांत",
            "term:Rough" to "कठोर",
            "term:Dangerous" to "खतरनाक",
            "condition:Sunny" to "धूप",
            "condition:Clear" to "साफ़ मौसम",
            "condition:Partly cloudy" to "कुछ बादल",
            "condition:Cloudy, chance of rain" to "बादल, बारिश की संभावना",
            "condition:Showers" to "बारिश",
            "condition:Thunderstorm" to "तूफ़ान",
            "condition:Fog" to "कोहरा",
            "condition:Wind" to "हवा",
            "condition:Rain" to "बारिश",
            "weather:clear" to "साफ़ मौसम",
            "weather:cloud" to "कुछ बादल",
            "weather:rain" to "बारिश",
            "weather:storm" to "तूफ़ान",
            "weather:fog" to "कोहरा",
            "weather:wind" to "हवा",
            "weather:heat" to "गर्मी",
            "weather:wave" to "समुद्र की स्थिति",
            "weather:weather" to "मौसम",
            "severity_good" to "अच्छा",
            "severity_caution" to "सावधानी",
            "severity_alert" to "अलर्ट",
            "around_time" to "%1\$s के आसपास",
            "all_day_event_time" to "पूरे दिन · %1\$s",
            "all_day_time_range" to "पूरे दिन · %1\$s से %2\$s तक",
            "event_time_range" to "%1\$s से %2\$s तक",
            "rain_none" to "अगले 24 घंटों में बारिश की उम्मीद नहीं है",
            "rain_now" to "अभी बारिश",
            "rain_easing" to "अभी बारिश; %1\$s के आसपास कम होगी",
            "rain_expected" to "%1\$s के आसपास बारिश की उम्मीद है (%2\$s%%)",
            "aqi_value" to "AQI %1\$d",
            "comfort_value" to "आराम %1\$s",
            "dew_point_value" to "ओस बिंदु %1\$d° · %2\$s",
            "hottest_around" to "सबसे गर्म %1\$s (%2\$d°)",
            "current_feels_like" to "वर्तमान महसूस होने वाला तापमान",
            "running_wind" to "हवा %1\$d किमी/घंटा",
            "upcoming_heat_alert" to "गर्मी का अलर्ट %1\$s के आसपास · महसूस %2\$d°",
            "dataset_attribution" to "मौसम पूर्वानुमान: Open-Meteo · वायु गुणवत्ता: CAMS",
            "back" to "पीछे",
            "continue" to "जारी रखें",
            "get_started" to "शुरू करें",
            "maybe_later" to "बाद में",
            "finish" to "पूरा करें",
            "selected" to "चुना गया",
            "widget_refresh" to "रिफ्रेश",
            "widget_weather_unavailable" to "मौसम उपलब्ध नहीं है",
            "widget_open_app_retry" to "फिर से कोशिश करने के लिए PersonaWeather खोलें",
            "widget_alert" to "अलर्ट",
            "widget_caution" to "सावधानी",
            "widget_aqi" to "AQI",
            "widget_feels_like" to "महसूस होता है",
            "widget_all_clear" to "अभी कोई कार्रवाई आवश्यक नहीं है",
            "widget_expand_hourly" to "प्रति घंटा पूर्वानुमान देखें",
            "widget_choose_personas" to "अनुकूलित सलाह देखने के लिए एक या अधिक प्रोफ़ाइल चुनें",
            "widget_no_advice" to "अभी कोई सक्रिय सलाह नहीं है",
            "widget_more_selected" to "+%1\$d और चुने गए",
            "widget_focus_CURRENT" to "मौजूदा मौसम",
            "widget_focus_HOURLY" to "प्रति घंटा पूर्वानुमान",
            "widget_focus_PERSONAS" to "प्रोफ़ाइल सलाह",
            "smart_home" to "स्मार्ट होम",
            "weather_suggestion" to "मौसम सुझाव",
            "smart_action_device_notice" to "सिर्फ़ सुझाव — कोई स्मार्ट डिवाइस कनेक्ट नहीं है",
            "smart_action_turn_on_ac" to "AC चालू करें",
            "smart_action_turn_on_light" to "लाइट चालू करें",
            "smart_action_turn_on_device" to "%1\$s चालू करें",
            "smart_action_light_device" to "लाइट",
            "smart_action_device" to "डिवाइस",
            "smart_action_dismiss" to "हटाएँ",
            "smart_action_ac_title" to "AC का सुझाव",
            "smart_action_light_title" to "लाइट का सुझाव",
            "smart_action_weather_title" to "मौसम सुझाव",
            "smart_action_sun_title" to "धूप से बचाव",
            "smart_action_air_title" to "वायु गुणवत्ता सुझाव",
            "smart_action_heat_title" to "गर्मी से बचाव का सुझाव",
            "smart_action_ac_message" to "इस क्षेत्र में अधिक तापमान का पूर्वानुमान है। कमरे को ठंडा रखने के लिए AC चालू करने पर विचार करें।",
            "smart_action_light_message" to "अंधेरा हो रहा है और कमरा ठंडा है। आराम के लिए लाइट चालू करने पर विचार करें।",
            "smart_action_rain_message" to "छाता साथ रखें।",
            "smart_action_sun_message" to "सनस्क्रीन लगाएँ और टोपी पहनें।",
            "smart_action_air_message" to "बाहर जाते समय मास्क पहनें।",
            "smart_action_heat_message" to "पर्याप्त पानी पिएँ और दिन के सबसे गर्म समय में बाहर जाने से बचें।",
            "notification_channel_alerts" to "मौसम अलर्ट",
            "notification_channel_alerts_description" to "आपके चुने हुए स्थान और प्रोफ़ाइल के लिए महत्वपूर्ण मौसम चेतावनियाँ।",
            "notification_channel_clothes" to "कपड़े सुखाने का अलार्म",
            "notification_channel_clothes_description" to "बाहर रखे कपड़ों तक बारिश पहुँचने की संभावना पर अलार्म।",
            "card_outlook_card" to "मौसम पूर्वानुमान",
            "packing_suggestions" to "सामान रखने के सुझाव",
            "saved_destinations" to "सहेजे गए गंतव्य",
            "no_saved_destinations" to "त्वरित पहुँच के लिए स्थान चुनने वाली सूची से स्थान सहेजें।",
            "tomorrow_soil" to "कल मिट्टी की नमी %1\$d%%",
            "planting_guidance" to "खेत के लिए सुझाव: %1\$s",
            "poor_visibility" to "कम दृश्यता",
            "commute_visibility" to "दृश्यता %1\$d किमी%2\$s",
            "best_event_day" to "बाहरी गतिविधि के लिए सबसे अच्छा दिन: %1\$s · बारिश का जोखिम %2\$d%%",
            "forecast_days_available" to "%1\$d-दिवसीय पूर्वानुमान",
            "calendar_events" to "कैलेंडर कार्यक्रम",
            "calendar_help" to "Google और अन्य सिंक किए गए कैलेंडर देखें",
            "marine_details" to "पानी %1\$s° · लहर %2\$s सेकंड · %3\$s",
            "high_tide" to "ज्वार",
            "low_tide" to "भाटा",
            "sunrise" to "सूर्योदय",
            "sunset" to "सूर्यास्त",
            "sun_times_unavailable" to "सूर्योदय और सूर्यास्त का समय उपलब्ध नहीं",
            "wave_height_value" to "लहर की ऊँचाई · %1$.1f मीटर",
            "wave_forecast_unavailable" to "लहर की ऊँचाई का पूर्वानुमान उपलब्ध नहीं",
            "tide_forecast_notice" to "ज्वार का समय प्रति-घंटे के पूर्वानुमान का अनुमान है, नौवहन के लिए नहीं।",
            "tide_times_unavailable" to "इस पूर्वानुमान में ज्वार का समय उपलब्ध नहीं",
            "packing_suggestions" to "साथ ले जाने के सुझाव",
            "event_comfort_score" to "बाहरी आराम %1\$d/100 · %2\$s",
            "event_comfort_factors" to "बारिश %1\$d%% · AQI %2\$d · %3\$s",
            "event_comfort_breakdown" to "अंक में कमी: बारिश -%1\$d · AQI -%2\$d · तापमान -%3\$d",
            "frost_risk" to "पाला पड़ने का जोखिम",
            "cold_conditions" to "ठंड",
            "heat_stress" to "गर्मी",
            "travel_severe_alert" to "गंभीर मौसम · %1\$s",
            "location_step_title" to "आप कहाँ हैं?",
            "major_cities" to "प्रमुख भारतीय शहर",
            "loading_localities" to "स्थान लोड हो रहे हैं…",
            "location_switch_tip" to "आप कभी भी स्थान बदल सकते हैं। महाबलिपुरम जैसे तटीय स्थान को भी आज़माएँ।",
            "location_unavailable" to "यह स्थान अभी इस संस्करण में उपलब्ध नहीं है। हम इसे जोड़ने पर काम कर रहे हैं।",
            "card_family_commute_card" to "परिवार का सफ़र",
            "card_commute_traffic_card" to "सड़क की स्थिति",
            "card_cloth_drying_card" to "कपड़े सुखाना",
            "card_trip_planner_card" to "यात्रा योजनाकार",
            "card_daily_outlook_card" to "दैनिक मौसम",
            "trip_planner" to "यात्रा योजनाकार",
            "trip_unavailable" to "यात्रा योजना उपलब्ध नहीं है",
            "sample_route" to "चुना गया मार्ग",
            "route_stops" to "4 पड़ाव · लगभग 60 मिनट",
            "route_forecast_note" to "मार्ग के पड़ाव पहले से तय हैं; मौसम पूर्वानुमान Open-Meteo से लिए जाते हैं।",
            "offline_trip_pack" to "ऑफ़लाइन यात्रा पूर्वानुमान",
            "offline_trip_status" to "इस मार्ग का पूर्वानुमान लोड होते समय डिवाइस पर सहेजा जाता है। प्रदाता की पूर्वानुमान अवधि समाप्त होने पर ऑफ़लाइन उपयोग उपलब्ध नहीं रहेगा।",
            "offline_trip_coverage" to "उपलब्ध प्रस्थान समय: %1\$s से %2\$s तक",
            "departure" to "प्रस्थान",
            "arrive_approximately" to "अनुमानित आगमन",
            "route_outlook" to "मार्ग का पूर्वानुमान",
            "better_time" to "बेहतर समय: प्रस्थान करें",
            "use_this_time" to "यह समय चुनें",
            "route_map" to "मार्ग का नक्शा",
            "clear" to "साफ़",
            "caution" to "सावधानी",
            "alert" to "अलर्ट",
            "route_locations_missing" to "इस मार्ग के स्थान डेटा में उपलब्ध नहीं हैं।",
            "stop_by_stop" to "हर पड़ाव का विवरण",
            "route_exposure" to "मार्ग की स्थिति",
            "forecast_summary" to "अगले %1\$s: %2\$d%3\$s → %4\$d%3\$s",
            "trip_route_distance" to "लगभग 10 किमी · लगभग 60 मिनट",
            "daily_today" to "आज",
            "route_summary_empty" to "इस मार्ग पर यात्रा संबंधी कोई सलाह नहीं है।",
            "route_summary_pack_umbrella" to "%1\$s के पास बारिश हो सकती है। निकलने से पहले छाता रखें।",
            "route_summary_pack_raincoat" to "%1\$s के पास बारिश हो सकती है। निकलने से पहले रेनकोट रखें।",
            "route_summary_air_caution" to "हवा के प्रदूषण के प्रति संवेदनशील हों तो %1\$s के पास कठिन बाहरी गतिविधि कम करें।",
            "route_summary_air_alert" to "%1\$s के पास बाहरी गतिविधि सीमित करें और इनडोर पड़ावों पर विचार करें।",
            "route_verdict_clear" to "साफ़",
            "route_verdict_caution" to "सावधानी",
            "route_verdict_alert" to "अलर्ट",
            "weather:sun" to "धूप",
            "daily_forecast" to "दैनिक पूर्वानुमान",
            "next_six_hours" to "अगले 6 घंटे",
            "running_conditions" to "दौड़ने की स्थिति",
            "peak_feels_like" to "सबसे अधिक महसूस तापमान",
            "inland_marine_unavailable" to "समुद्री जानकारी केवल तटीय स्थानों के लिए उपलब्ध है",
            "rain_chance_six_hours" to "अगले 6 घंटों में बारिश की संभावना",
            "morning_rain" to "स्कूल छोड़ने का समय · %1\$s · %2\$s पर बारिश की संभावना सबसे अधिक · %3\$d%%",
            "pickup_rain" to "स्कूल से लेने का समय · %1\$s · %2\$s पर बारिश की संभावना सबसे अधिक · %3\$d%%",
            "commute_morning_window" to "सुबह 7–9 बजे स्कूल जाने के पूरे समय",
            "commute_pickup_window" to "शाम 4–6 बजे स्कूल से लेने के पूरे समय",
            "commute_today" to "आज",
            "commute_tomorrow" to "कल",
            "commute_pack_umbrella" to "छाता साथ रखें",
            "commute_pack_raincoat" to "रेनकोट साथ रखें",
            "commute_no_rain_gear" to "इस पूर्वानुमान में रेन गियर की ज़रूरत नहीं दिखती",
            "soil_moisture" to "मिट्टी की नमी",
            "tomorrow_soil_rain" to "कल की मिट्टी और बारिश",
            "soil_rain" to "मिट्टी और बारिश",
            "roads_status" to "सड़क की स्थिति",
            "wet" to "गीली",
            "road_clear" to "तुरंत बारिश या कम दृश्यता की चेतावनी नहीं",
            "road_rain_indicated" to "अगले 2 घंटों में बारिश का संकेत है",
            "rain_forecast_conflict" to "बारिश का संकेत है, लेकिन उसकी संभावना का अनुमान इससे मेल नहीं खाता।",
            "rain_next_two_hours" to "अगले 2 घंटों में बारिश की संभावना · अधिकतम %1\$d%%",
            "uv_current_value" to "अभी · %1\$s",
            "uv_peak_today" to "दिन का UV स्तर लगभग %1\$s पर सबसे अधिक · %2\$s",
            "uv_peak_tomorrow" to "कल दिन का UV स्तर लगभग %1\$s पर सबसे अधिक · %2\$s",
            "uv_forecast_unavailable" to "दिन के UV का पूर्वानुमान उपलब्ध नहीं",
            "marine_state" to "%1\$s · लहरें %2\$s मीटर",
            "marine_state_forecast" to "%1\$s · %2\$s · लहरें %3\$s मीटर",
            "marine_forecast_unavailable" to "समुद्री पूर्वानुमान उपलब्ध नहीं",
            "marine_safety_rough" to "पानी में उतरने से पहले स्थानीय लाइफगार्ड की सलाह लें।",
            "marine_safety_general" to "समुद्री पूर्वानुमान अनुमान है; पानी की गतिविधियों से पहले स्थानीय समुद्रतट और लाइफगार्ड की सलाह लें।",
            "marine_peak_today" to "आज सबसे ऊँची लहरें लगभग %1\$s · %2\$s",
            "marine_peak_tomorrow" to "कल सबसे ऊँची लहरें लगभग %1\$s · %2\$s",
            "packing_suggestions_next_day" to "अगले 24 घंटों के लिए सामान",
            "packing_tip_sun_protection" to "सनस्क्रीन और टोपी",
            "packing_tip_umbrella" to "छाता",
            "packing_tip_raincoat" to "रेनकोट",
            "packing_tip_water" to "पानी की बोतल",
            "packing_tip_air_quality" to "प्रदूषण से संवेदनशील हों तो अच्छी तरह फिट होने वाला मास्क",
            "packing_none" to "मौसम के लिए अतिरिक्त सामान की ज़रूरत नहीं।",
            "rain_uv_aqi" to "बारिश, UV और AQI",
            "no_drying_window" to "कपड़े सुखाने के लिए उपयुक्त समय नहीं",
            "try_later_forecast" to "बाद के पूर्वानुमान में फिर देखें",
            "drying_humidity" to "कपड़े सुखाने की नमी",
            "route_card" to "यात्रा का मौसम",
            "route_card_details" to "मार्ग के हर पड़ाव पर मौसम देखें",
            "open_route_forecast" to "मार्ग का पूर्वानुमान खोलें",
            "best_day" to "सबसे अच्छा दिन",
            "rain_stops" to "बारिश वाले पड़ाव",
            "no_route_advisories" to "मार्ग पर कोई मौसम सलाह नहीं",
            "route_rain_summary" to "बारिश की संभावना",
            "route_aqi_summary" to "वायु गुणवत्ता",
            "route_better_leave" to "पूर्वानुमान में बारिश का संकेत नहीं है",
            "route_rain_possible" to "मार्ग पर बारिश संभव है",
            "route_better_moderate_air" to "हवा मध्यम",
            "route_better_score" to "मार्ग पर कम मौसम अलर्ट",
            "route_home_office" to "घर से कार्यालय",
            "route_label_health" to "स्वास्थ्य",
            "route_label_fitness" to "फिटनेस",
            "route_label_family" to "परिवार",
            "route_label_event" to "कार्यक्रम",
            "route_label_commuter" to "दैनिक यात्री",
            "route_label_traveler" to "यात्री",
            "route_label_beach" to "समुद्र तट",
            "route_label_gardener" to "बागवानी",
            "route_clear" to "साफ़",
            "hour_earlier" to "एक घंटा पहले",
            "hour_later" to "एक घंटा बाद",
            "average_route_aqi" to "मार्ग का औसत AQI",
            "route_stops_rain" to "बारिश की संभावना वाले पड़ाव",
            "run_not_recommended" to "दौड़ने की सलाह नहीं है",
            "run_no_clear_best" to "इस पूर्वानुमान में दौड़ने का कोई स्पष्ट सबसे अच्छा समय नहीं है",
            "run_window_unavailable" to "दौड़ने का उपयुक्त समय उपलब्ध नहीं",
            "route_map_description" to "मार्ग का नक्शा और मौसम की स्थिति",
            "high_low_feels_like" to "महसूस तापमान: अधिकतम और न्यूनतम",
            "route_better_dry" to "सभी पड़ावों पर बारिश नहीं",
            "route_better_air" to "बेहतर हवा",
            "route_home" to "घर",
            "route_office" to "कार्यालय",
            "route_title_chennai_sample" to "चेन्नई · घर से कार्यालय",
            "route_title_delhi_ncr_sample" to "दिल्ली NCR · यात्रा मार्ग",
            "route_title_mumbai_sample" to "मुंबई · यात्रा मार्ग",
            "condition:Scattered clouds, chance of rain" to "बादल छाए हैं, बारिश की संभावना",
            "live_mode_help" to "Open-Meteo मौसम, वायु गुणवत्ता और समुद्री पूर्वानुमान देता है; ये स्टेशन मापन नहीं, मॉडल पूर्वानुमान हैं। ऑफलाइन उपयोग के लिए नवीनतम मूल प्रदाता उत्तर डिवाइस पर रखे जाते हैं और केवल उनकी पूर्वानुमान अवधि तक मान्य हैं। पराग का डेटा स्थान के अनुसार उपलब्ध होता है।",
            "unavailable" to "उपलब्ध नहीं",
            "date" to "तारीख",
            "notifications" to "सूचनाएँ",
            "weather_alerts" to "मौसम अलर्ट",
            "clothes_alarm" to "कपड़े सुखाने के लिए बारिश अलार्म",
            "allow_notifications" to "सूचनाओं की अनुमति दें",
            "permission_granted" to "सूचना की अनुमति मिल गई",
            "reset_onboarding" to "शुरुआती सेटअप फिर से करें",
            "your_personas" to "आपकी प्रोफ़ाइल",
            "choose_advice" to "चुनें कि कौन-सी सलाह दिखाई जाए",
            "location" to "स्थान",
            "change_location" to "पूर्वानुमान का स्थान बदलें",
            "upcoming_alerts" to "आने वाले अलर्ट",
            "about" to "ऐप के बारे में",
            "about_description" to "मौसम, वायु गुणवत्ता और समुद्री पूर्वानुमान Open-Meteo से आते हैं; ये मॉडल पूर्वानुमान हैं, स्टेशन मापन नहीं। इंटरनेट न होने पर डिवाइस पर रखे मूल प्रदाता उत्तर उनकी कवरेज अवधि में उपलब्ध रहते हैं। पराग का स्तर केवल वहाँ दिखता है जहाँ प्रदाता डेटा देता है; वायु गुणवत्ता पराग का विकल्प नहीं है। समुद्र-स्तर के अनुमान नौवहन के लिए नहीं हैं।",
            "reset_app" to "ऐप रीसेट करें",
            "reset_description" to "सेटिंग मिटाकर शुरुआती सेटअप फिर से शुरू करें",
            "install_tamil_voice" to "तमिल आवाज़ डेटा इंस्टॉल नहीं है",
            "install" to "इंस्टॉल करें",
            "close" to "बंद करें",
            "change_forecast_location" to "पूर्वानुमान का स्थान बदलें",
            "see_all_alerts" to "सभी %d अलर्ट देखें",
            "today_for_you" to "आज आपके लिए",
            "personas_help" to "चुनें कि किसके लिए सलाह दिखाई जाए",
            "location_help" to "पूर्वानुमान का स्थान बदलें",
            "notifications_label" to "मौसम सूचनाएँ",
            "notifications_help" to "महत्वपूर्ण मौसम अलर्ट पाएँ",
            "clothes_alarm_help" to "बारिश आने पर कपड़े अंदर ले आएँ",
            "alerts_help" to "अभी और अगले 12 घंटों के अलर्ट",
            "about_help" to "मौसम पूर्वानुमान और डेटा स्रोत",
            "reset_help" to "ऐप सेटिंग रीसेट करें",
            "about_title" to "PersonaWeather के बारे में",
            "welcome_title" to "ऐसा मौसम जो बताए क्या करना है",
            "welcome_description" to "अपनी ज़रूरतें चुनें — हम आपके लिए ज़रूरी मौसम दिखाएँगे।",
            "search_localities" to "शहर और इलाकों में खोजें",
            "alerts_step_title" to "मौसम अलर्ट",
            "alerts_step_description" to "ज़रूरी मौसम बदलावों की सूचना पाएँ।",
            "notifications_allowed" to "सूचनाओं की अनुमति है",
            "search_15_localities" to "15 स्थानों में खोजें",
            "hour_label" to "घंटा · %1\$s:00",
            "previous_hour" to "पिछला घंटा",
            "next_hour" to "अगला घंटा",
            "notifications_section" to "सूचनाएँ",
            "play" to "चलाएँ",
            "pause" to "रोकें",
            "minus_day" to "−1 दिन",
            "plus_day" to "+1 दिन",
            "hour" to "घंटा",
            "airplane_mode_note" to "एयरप्लेन मोड में सहेजे गए पूर्वानुमान की जाँच करें।",
            "notification_permission_missing" to "सूचना की अनुमति नहीं मिली",
            "next" to "अगला",
            "preset_thirty_minutes" to "+30 मिनट",
            "preset_one_hour" to "+1 घंटा",
            "preset_five_pm" to "शाम 5 बजे",
            "label_grass" to "घास",
            "label_tree" to "पेड़",
            "label_ragweed" to "रैगवीड",
            "pollen_species_grass" to "घास",
            "pollen_species_alder" to "एल्डर",
            "pollen_species_birch" to "बर्च",
            "pollen_species_mugwort" to "मगवॉर्ट",
            "pollen_species_olive" to "जैतून",
            "pollen_species_ragweed" to "रैगवीड",
            "pollen_current" to "अभी · %1\$s grains/m³",
            "pollen_peak" to "24 घंटे का उच्चतम स्तर · %1\$s grains/m³ · %2\$s",
            "pollen_current_unavailable" to "मौजूदा स्तर उपलब्ध नहीं",
            "pollen_peak_unavailable" to "इस पराग प्रकार का पूर्वानुमान नहीं",
            "pollen_forecast_unavailable" to "इस स्थान के लिए पराग पूर्वानुमान उपलब्ध नहीं है।",
            "route_arrival" to "अनुमानित आगमन %1\$s",
            "route_leave_at" to "%1\$s पर निकलें",
            "route_summary_rain" to "%1\$d%% बारिश की संभावना · %2\$s पर",
            "route_summary_air" to "AQI %1\$d · %2\$s पर",
            "route_metric_rain" to "बारिश %1\$d%%",
            "route_metric_aqi" to "AQI %1\$d",
            "route_metric_visibility" to "दृश्यता %1\$d किमी",
            "morning_forecast_unavailable" to "सुबह का पूर्वानुमान उपलब्ध नहीं",
            "pickup_forecast_unavailable" to "स्कूल से लेने के समय का पूर्वानुमान उपलब्ध नहीं",
            "rain_value" to "बारिश %1\$d%%",
            "visibility_value" to "दृश्यता %1\$d किमी",
            "average_aqi" to "औसत AQI",
        )

        val English = AppStrings(ENGLISH, emptyMap())
        val Tamil = AppStrings(TAMIL, tamilStrings)
        val Hindi = AppStrings(HINDI, hindiStrings)

        fun forCode(code: String): AppStrings = when (code) {
            TAMIL -> Tamil
            HINDI -> Hindi
            else -> English
        }
    }
}

val LocalAppStrings = staticCompositionLocalOf { AppStrings.English }

@Composable
fun appText(key: String, fallback: String): String = LocalAppStrings.current.text(key, fallback)
