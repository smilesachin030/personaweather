package com.example.personaweather.ui.home

import android.content.SharedPreferences
import com.example.personaweather.data.UserPrefs
import com.example.personaweather.data.WeatherDataSource
import com.example.personaweather.core.model.Location
import com.example.personaweather.core.model.Persona
import com.example.personaweather.core.model.WeatherRow
import com.example.personaweather.notify.AlertNotifier
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private var resetMainDispatcher = false
    private val timestampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")

    @After
    fun tearDown() {
        if (resetMainDispatcher) Dispatchers.resetMain()
    }

    @Test
    fun homeStateFlagsAirQualityAndCoastalAdvice() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        resetMainDispatcher = true

        val prefs = UserPrefs(fakeSharedPreferences())
        prefs.onboarded = true
        prefs.personas = listOf("health_conscious", "beach_surfer")
        prefs.locationId = "nungambakkam"

        val location = Location("nungambakkam", "Nungambakkam", 13.06, 80.25, 4.0, "West", "Residential", false)
        val weatherRows = listOf(weatherRow("2026-09-28T09:00", 178, "Moderate", 52.0, 1.0))
        val viewModel = HomeViewModel(
            prefs = prefs,
            catalog = fakeCatalog(),
            dataSource = fakeDataSource(listOf(location), weatherRows),
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.advisories.any { it.ruleId == "hc_aqi_alert" })
        assertEquals("Nungambakkam", state.location.name)
        assertTrue(state.cards.contains("trip_planner_card"))
        assertEquals(1, state.dailySummaries.size)

        viewModel.toggleSavedDestination(location.id)
        assertEquals(listOf(location.id), viewModel.uiState.value.savedDestinationIds)
        assertEquals(listOf(location.id), prefs.savedDestinationIds)
        viewModel.toggleSavedDestination(location.id)
        assertTrue(viewModel.uiState.value.savedDestinationIds.isEmpty())

        viewModel.setCardConfiguration(state.cards, setOf("aqi_card"))
        assertFalse(viewModel.uiState.value.cards.contains("aqi_card"))
        assertTrue(prefs.cardsDisabled.contains("aqi_card"))

        val reversedOrder = viewModel.uiState.value.orderedCardIds.reversed()
        viewModel.setCardConfiguration(reversedOrder, viewModel.uiState.value.disabledCards)
        assertEquals(reversedOrder, viewModel.uiState.value.orderedCardIds)

        viewModel.setLocation("mahabalipuram")
        advanceUntilIdle()
        val coastalState = viewModel.uiState.value
        assertTrue(coastalState.hasMarineCard)
        assertTrue(coastalState.advisories.any { it.ruleId == "bs_wave_alert" } || coastalState.advisories.any { it.ruleId == "bs_wind_caution" })
    }

    @Test
    fun smartHomeSuggestionAppearsWhenHeatThresholdIsReached() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        resetMainDispatcher = true

        val prefs = UserPrefs(fakeSharedPreferences()).apply {
            onboarded = true
            personas = listOf("health_conscious")
            locationId = "nungambakkam"
        }

        val location = Location("nungambakkam", "Nungambakkam", 13.06, 80.25, 4.0, "West", "Residential", false)
        val weatherRows = listOf(weatherRow("2026-09-28T14:00", 180, "Moderate", 39.0, 1.0).copy(temperature = 35.5, heatAlert = 1))
        val viewModel = HomeViewModel(
            prefs = prefs,
            catalog = fakeCatalog(),
            dataSource = fakeDataSource(listOf(location), weatherRows),
        )

        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.smartAction)
        assertEquals("ac", viewModel.uiState.value.smartAction!!.deviceType)
        assertTrue(viewModel.uiState.value.smartAction!!.title.contains("AC", ignoreCase = true))
    }

    @Test
    fun smartHomeSuggestionIsHiddenBelowHeatThreshold() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        resetMainDispatcher = true

        val prefs = UserPrefs(fakeSharedPreferences()).apply {
            onboarded = true
            personas = listOf("health_conscious")
            locationId = "nungambakkam"
        }

        val location = Location("nungambakkam", "Nungambakkam", 13.06, 80.25, 4.0, "West", "Residential", false)
        val weatherRows = listOf(weatherRow("2026-09-28T14:00", 120, "Moderate", 29.0, 0.0).copy(temperature = 29.0, heatAlert = 0))
        val viewModel = HomeViewModel(
            prefs = prefs,
            catalog = fakeCatalog(),
            dataSource = fakeDataSource(listOf(location), weatherRows),
        )

        advanceUntilIdle()

        assertNull(viewModel.uiState.value.smartAction)
    }

    @Test
    fun slowerPreviousLocationLoadCannotReplaceLatestSelection() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        resetMainDispatcher = true

        val prefs = UserPrefs(fakeSharedPreferences()).apply {
            personas = listOf("health_conscious")
            locationId = "nungambakkam"
        }
        val locations = listOf(
            Location("nungambakkam", "Nungambakkam", 13.06, 80.25, 4.0, "West", "Residential", false),
            Location("velachery", "Velachery", 12.98, 80.22, 14.0, "South", "Residential", false),
        )
        val previousLoadStarted = CompletableDeferred<Unit>()
        val releasePreviousLoad = CompletableDeferred<Unit>()
        val dataSource = object : WeatherDataSource {
            override suspend fun locations(): List<Location> = locations

            override suspend fun series(locationId: String): List<WeatherRow> {
                if (locationId == "nungambakkam") {
                    previousLoadStarted.complete(Unit)
                    withContext(NonCancellable) { releasePreviousLoad.await() }
                }
                return listOf(weatherRow("2026-09-28T09:00", 80, "Low", 8.0, 0.0).copy(locationId = locationId))
            }

            override suspend fun dataRange() = java.time.LocalDateTime.parse("2026-09-28T09:00") to
                java.time.LocalDateTime.parse("2026-09-28T09:00")
        }
        val viewModel = HomeViewModel(prefs, fakeCatalog(), dataSource)
        runCurrent()
        assertTrue(previousLoadStarted.isCompleted)

        viewModel.setLocation("velachery")
        advanceUntilIdle()
        assertEquals("velachery", viewModel.uiState.value.location.id)

        releasePreviousLoad.complete(Unit)
        advanceUntilIdle()
        assertEquals("velachery", viewModel.uiState.value.location.id)
    }

    @Test
    fun languageAndAccessibilityPreferencesPersistAndReset() {
        val prefs = UserPrefs(fakeSharedPreferences())

        assertEquals("en", prefs.language)
        assertFalse(prefs.largeTextEnabled)
        assertFalse(prefs.highContrastEnabled)

        prefs.language = "ta"
        prefs.largeTextEnabled = true
        prefs.highContrastEnabled = true
        assertEquals("ta", prefs.language)
        assertTrue(prefs.largeTextEnabled)
        assertTrue(prefs.highContrastEnabled)

        prefs.clear()
        assertEquals("en", prefs.language)
        assertFalse(prefs.largeTextEnabled)
        assertFalse(prefs.highContrastEnabled)
    }

    @Test
    fun rememberedLiveLocationRestoresAfterPrefsRecreation() {
        val sharedPreferences = fakeSharedPreferences()
        val original = Location(
            id = "openmeteo:in:chennai:12345",
            name = "Chennai",
            lat = 13.0827,
            lon = 80.2707,
            adminArea = "Tamil Nadu",
            countryCode = "IN",
            geocodingId = 12345L,
            timeZone = "Asia/Kolkata",
        )

        UserPrefs(sharedPreferences).rememberLocation(original)

        assertEquals(original, UserPrefs(sharedPreferences).rememberedLocation(original.id))
    }

    @Test
    fun selectingSearchedPlacePersistsAndLoadsIt() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        resetMainDispatcher = true
        val demoLocation = Location("nungambakkam", "Nungambakkam", 13.06, 80.25, 4.0, "West", "Residential", false)
        val liveLocation = Location(
            id = "open-meteo:IN:98765",
            name = "Bengaluru",
            lat = 12.9716,
            lon = 77.5946,
            adminArea = "Karnataka",
        )
        val prefs = UserPrefs(fakeSharedPreferences()).apply { locationId = demoLocation.id }
        val viewModel = HomeViewModel(
            prefs = prefs,
            catalog = fakeCatalog(),
            dataSource = fakeDataSource(listOf(demoLocation), listOf(weatherRow("2026-09-28T09:00", 80, "Low", 8.0, 0.0))),
        )
        advanceUntilIdle()

        viewModel.setLocation(liveLocation)
        advanceUntilIdle()

        assertEquals(liveLocation.id, prefs.locationId)
        assertEquals(liveLocation, prefs.rememberedLocation(liveLocation.id))
        assertEquals(liveLocation.id, viewModel.uiState.value.location.id)
    }

    @Test
    fun staleSelectedLocationKeepsLocalChoicesAvailableAndCanRecover() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        resetMainDispatcher = true
        val location = Location("velachery", "Velachery", 12.98, 80.22, 14.0, "South", "Residential", false)
        val prefs = UserPrefs(fakeSharedPreferences()).apply {
            locationId = "open-meteo:IN:stale"
        }
        val rows = listOf(weatherRow("2026-09-28T09:00", 80, "Low", 8.0, 0.0).copy(locationId = location.id))
        val viewModel = HomeViewModel(prefs, fakeCatalog(), fakeDataSource(listOf(location), rows))

        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.error.orEmpty().contains("selected location is unavailable"))
        assertEquals(listOf(location), viewModel.uiState.value.locations)

        viewModel.setLocation(location)
        advanceUntilIdle()

        assertEquals(null, viewModel.uiState.value.error)
        assertEquals(location.id, viewModel.uiState.value.location.id)
    }

    @Test
    fun upcomingAlertsShowsFirstFutureTripWithinTwelveHours() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        resetMainDispatcher = true
        val prefs = UserPrefs(fakeSharedPreferences())
        prefs.personas = listOf("health_conscious")
        val location = Location("nungambakkam", "Nungambakkam", 13.06, 80.25, 4.0, "West", "Residential", false)
        val now = LocalDateTime.now(ZoneId.of("Asia/Kolkata")).withMinute(0).withSecond(0).withNano(0)
        val rows = listOf(
            weatherRow(now.format(timestampFormatter), 90, "Low", 10.0, 0.0),
            weatherRow(now.plusHours(1).format(timestampFormatter), 178, "Low", 10.0, 0.0),
        )
        val viewModel = HomeViewModel(prefs, fakeCatalog(), fakeDataSource(listOf(location), rows))

        advanceUntilIdle()

        val upcoming = viewModel.uiState.value.upcomingAdvisories.single()
        assertEquals("hc_aqi_alert", upcoming.advisory.ruleId)
        assertEquals(now.plusHours(1), upcoming.time)
    }

    @Test
    fun coastalLocationDoesNotReaddDeselectedBeachPersona() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        resetMainDispatcher = true

        val prefs = UserPrefs(fakeSharedPreferences())
        prefs.personas = listOf("health_conscious")
        prefs.locationId = "mahabalipuram"

        val location = Location("mahabalipuram", "Mahabalipuram", 12.62, 80.17, 55.0, "East", "Coastal / tourist", true)
        val rows = listOf(weatherRow("2026-09-28T19:00", 110, "Low", 18.0, 2.2))
        val viewModel = HomeViewModel(
            prefs = prefs,
            catalog = fakeCatalog(),
            dataSource = fakeDataSource(listOf(location), rows),
        )

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.selectedPersonas.any { it.id == "beach_surfer" })
        assertEquals(listOf("health_conscious"), prefs.personas)
    }

    @Test
    fun clothesRainAlarmEmitsAnAlertForUpcomingRain() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        resetMainDispatcher = true

        val prefs = UserPrefs(fakeSharedPreferences())
        prefs.personas = listOf("health_conscious")
        val notifier = RecordingNotifier()
        val location = Location("velachery", "Velachery", 12.98, 80.22, 14.0, "South", "Residential", false)
        val rain = listOf(32.0, 55.0, 75.0, 90.0)
        val now = LocalDateTime.now(ZoneId.of("Asia/Kolkata")).withMinute(0).withSecond(0).withNano(0)
        val rows = (0..3).map { index ->
            weatherRow(now.plusHours(index.toLong()).format(timestampFormatter), 60, "Low", 12.0, 0.0)
                .copy(rainChancePercent = rain[index])
        }
        val viewModel = HomeViewModel(
            prefs = prefs,
            catalog = fakeCatalog(),
            dataSource = fakeDataSource(listOf(location), rows),
            notificationHelper = notifier,
        )
        advanceUntilIdle()

        assertFalse(prefs.personas.contains("parent_family"))
        assertTrue(viewModel.uiState.value.advisories.any { it.ruleId == "pf_cloth_alert" })
        assertTrue(notifier.ruleIds.contains("pf_cloth_alert"))
    }

    private fun fakeCatalog() = com.example.personaweather.core.model.RulesCatalog(
        cards = mapOf(
            "aqi_card" to com.example.personaweather.core.model.CardSpec("aqi_card", "Air Quality", listOf("aqi_value")),
            "marine_card" to com.example.personaweather.core.model.CardSpec("marine_card", "Sea Conditions", listOf("wave_height_m"), requiresCoastal = true),
            "trip_planner_card" to com.example.personaweather.core.model.CardSpec("trip_planner_card", "Trip Planner", listOf("rain_chance_percent")),
        ),
        personas = mapOf(
            "health_conscious" to Persona("health_conscious", "Health-conscious", false, listOf("aqi_card"), listOf(
                com.example.personaweather.core.model.Rule("hc_aqi_alert", "aqi_value", ">=", 150, "alert", "Air quality alert"),
            )),
            "beach_surfer" to Persona("beach_surfer", "Beachgoer & Surfer", true, listOf("marine_card"), listOf(
                com.example.personaweather.core.model.Rule("bs_wave_alert", "wave_height_m", ">=", 2.0, "alert", "Rough sea conditions"),
                com.example.personaweather.core.model.Rule("bs_wind_caution", "wind_speed", ">=", 18, "caution", "Strong winds"),
            )),
            "parent_family" to Persona("parent_family", "Parent & Family", false, listOf("cloth_drying_card"), listOf(
                com.example.personaweather.core.model.Rule(
                    "pf_cloth_alert", "rain_chance_percent", ">=", 60, "alert",
                    "Rain expected within 3 hours — bring in any drying clothes.", lookaheadHours = 3, aggregate = "max",
                ),
            )),
        ),
    )

    private class RecordingNotifier : AlertNotifier {
        var ruleIds: List<String> = emptyList()
        override fun notifyAlerts(
            location: Location,
            simTime: java.time.LocalDateTime,
            advisories: List<com.example.personaweather.core.model.Advisory>,
            prefs: UserPrefs,
        ) {
            ruleIds = advisories.filter { it.severity == com.example.personaweather.core.model.Severity.ALERT }.map { it.ruleId }
        }
    }

    private fun fakeDataSource(locations: List<Location>, weatherRows: List<WeatherRow>) = object : WeatherDataSource {
        override suspend fun locations(): List<Location> = locations
        override suspend fun series(locationId: String): List<WeatherRow> = weatherRows
        override suspend fun dataRange(): Pair<java.time.LocalDateTime, java.time.LocalDateTime> =
            java.time.LocalDateTime.of(2026, 9, 28, 9, 0) to java.time.LocalDateTime.of(2026, 9, 28, 10, 0)
    }

    private fun weatherRow(
        timestamp: String,
        aqi: Int,
        pollen: String,
        windSpeed: Double,
        wave: Double,
    ): WeatherRow {
        val time = LocalDateTime.parse(timestamp, timestampFormatter)
        return WeatherRow(
            id = 1,
            locationId = "test",
            timestamp = timestamp,
            date = time.toLocalDate().toString(),
            hour = time.hour,
            temperature = 29.0,
            feelsLike = 30.0,
            humidity = 64.0,
            windSpeed = windSpeed,
            windDirection = "NE",
            condition = "Clear",
            pressureHpa = 1012.0,
            dewPoint = 24.0,
            uvIndex = 7.0,
            rainChancePercent = 10.0,
            rainfallMm = 0.0,
            visibilityKm = 9.0,
            fogFlag = 0,
            aqiValue = aqi,
            aqiCategory = "Unhealthy",
            dominantPollutant = "PM2.5",
            pm25 = 62.0,
            pm10 = 88.0,
            no2 = 0.0,
            o3 = 0.0,
            co = 0.0,
            so2 = 0.0,
            pollenGrass = pollen,
            pollenTree = "Low",
            pollenRagweed = "Low",
            waveHeightM = wave,
            wavePeriodSec = 8.0,
            waveDirection = "SE",
            waterTemperature = 28.0,
            tideHeightM = 1.2,
            soilMoisturePercent = 45.0,
            heatAlert = 0,
            comfortIndex = "Good",
        )
    }

    private fun fakeSharedPreferences(): SharedPreferences = object : SharedPreferences {
        private val state = linkedMapOf<String, Any?>()
        override fun getAll(): MutableMap<String, *> = state.toMutableMap()
        override fun getString(key: String?, defValue: String?): String? = state[key] as? String ?: defValue
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
            (state[key] as? Set<*>)?.filterIsInstance<String>()?.toMutableSet() ?: defValues?.toMutableSet()
        override fun getInt(key: String?, defValue: Int): Int = state[key] as? Int ?: defValue
        override fun getLong(key: String?, defValue: Long): Long = state[key] as? Long ?: defValue
        override fun getFloat(key: String?, defValue: Float): Float = state[key] as? Float ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean): Boolean = state[key] as? Boolean ?: defValue
        override fun contains(key: String?): Boolean = state.containsKey(key)
        override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
            override fun putString(key: String?, value: String?): SharedPreferences.Editor { state[key ?: ""] = value; return this }
            override fun putStringSet(key: String?, newValues: MutableSet<String>?): SharedPreferences.Editor { state[key ?: ""] = newValues; return this }
            override fun putInt(key: String?, value: Int): SharedPreferences.Editor { state[key ?: ""] = value; return this }
            override fun putLong(key: String?, value: Long): SharedPreferences.Editor { state[key ?: ""] = value; return this }
            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor { state[key ?: ""] = value; return this }
            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor { state[key ?: ""] = value; return this }
            override fun remove(key: String?): SharedPreferences.Editor { state.remove(key); return this }
            override fun clear(): SharedPreferences.Editor { state.clear(); return this }
            override fun commit(): Boolean = true
            override fun apply() = Unit
        }
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
    }
}
