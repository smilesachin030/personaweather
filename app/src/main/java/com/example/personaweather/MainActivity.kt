package com.example.personaweather

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.lifecycle.lifecycleScope
import com.example.personaweather.notify.ClothesRainAlarmWorker
import com.example.personaweather.ui.home.AlertsScreen
import com.example.personaweather.ui.home.CalendarEventsScreen
import com.example.personaweather.ui.home.EditCardsSheet
import com.example.personaweather.ui.home.HomeScreen
import com.example.personaweather.ui.home.LocationSheet
import com.example.personaweather.ui.home.PersonaSheet
import com.example.personaweather.ui.home.SettingsScreen
import com.example.personaweather.ui.home.HomeViewModel
import com.example.personaweather.ui.home.CalendarEventForecast
import com.example.personaweather.ui.localization.AppStrings
import com.example.personaweather.ui.localization.LocalAppStrings
import com.example.personaweather.ui.onboarding.OnboardingWizardScreen
import com.example.personaweather.ui.theme.LocalHighContrast
import com.example.personaweather.ui.theme.PersonaWeatherTheme
import com.example.personaweather.ui.trip.TripPlannerScreen
import com.example.personaweather.ui.trip.TripPlannerViewModel
import com.example.personaweather.tts.Speaker
import com.example.personaweather.core.engine.CalendarEventWeatherEvaluator
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.CancellationException
import com.example.personaweather.widget.WidgetIntentExtras
import com.example.personaweather.widget.WidgetRefreshScheduler
import kotlinx.coroutines.launch

private enum class AppDestination { HOME, TRIP, SETTINGS, ALERTS, CALENDAR }
private enum class HomeSheet { LOCATION, PERSONA, CARDS }

class MainActivity : ComponentActivity() {
    private val showOnboarding = mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as PersonaWeatherApp
        intent.getStringExtra(WidgetIntentExtras.LOCATION_ID)?.let { locationId ->
            app.container.userPrefs.locationId = locationId
        }
        val catalog = app.container.rulesCatalog
        showOnboarding.value = !app.container.userPrefs.onboarded
        val initialDestination = if (
            intent.getStringExtra(WidgetIntentExtras.DESTINATION) == WidgetIntentExtras.DESTINATION_ALERTS &&
            app.container.userPrefs.onboarded
        ) AppDestination.ALERTS else AppDestination.HOME

        setContent {
            var language by remember { mutableStateOf(app.container.userPrefs.language) }
            var highContrast by remember { mutableStateOf(app.container.userPrefs.highContrastEnabled) }
            var largeText by remember { mutableStateOf(app.container.userPrefs.largeTextEnabled) }
            var showVoiceInstallPrompt by remember { mutableStateOf(false) }
            var speakerRevision by remember { mutableStateOf(0) }
            val baseDensity = LocalDensity.current
            val voiceInstallerLauncher = rememberLauncherForActivityResult(StartActivityForResult()) {
                speakerRevision += 1
            }
            val speaker = remember(language, speakerRevision) {
                Speaker(this@MainActivity, language) { showVoiceInstallPrompt = true }
            }
            DisposableEffect(speaker) {
                onDispose { speaker.close() }
            }
            CompositionLocalProvider(
                LocalAppStrings provides AppStrings.forCode(language),
                LocalHighContrast provides highContrast,
                LocalDensity provides Density(
                    baseDensity.density,
                    maxOf(baseDensity.fontScale, if (largeText) 1.3f else 1f),
                ),
            ) {
            Box(Modifier.fillMaxSize().statusBarsPadding()) {
            PersonaWeatherTheme(darkTheme = true) {
            val onboardingLocations by produceState(initialValue = emptyList<com.example.personaweather.core.model.Location>()) {
                value = app.container.weatherDataSource.locations()
            }
            val destination = remember { mutableStateOf(initialDestination) }
            val activeSheet = remember { mutableStateOf<HomeSheet?>(null) }
            val notificationsEnabled = remember { mutableStateOf(app.container.userPrefs.notificationsEnabled) }
            val clothAlarmEnabled = remember { mutableStateOf(app.container.userPrefs.clothAlarmEnabled) }
            val notificationPermissionGranted = remember { mutableStateOf(app.notificationHelper.hasPermission()) }
            val pendingNotificationEnable = remember { mutableStateOf(false) }
            val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                notificationPermissionGranted.value = granted
                if (pendingNotificationEnable.value) {
                    pendingNotificationEnable.value = false
                    notificationsEnabled.value = granted
                    app.container.userPrefs.notificationsEnabled = granted
                    ClothesRainAlarmWorker.reconcile(
                        this@MainActivity,
                        enabled = granted && clothAlarmEnabled.value,
                    )
                }
            }
            val calendarPermissionGranted = remember { mutableStateOf(app.container.calendarEventSource.hasPermission(this@MainActivity)) }
            val calendarEvents = remember { mutableStateOf<List<com.example.personaweather.data.CalendarEvent>>(emptyList()) }
            val calendarLoading = remember { mutableStateOf(false) }
            val calendarLoadError = remember { mutableStateOf<String?>(null) }
            val calendarForecasts = remember { mutableStateOf<Map<String, CalendarEventForecast>>(emptyMap()) }
            val analyzingEventKey = remember { mutableStateOf<String?>(null) }
            fun refreshCalendarEvents() {
                calendarPermissionGranted.value = app.container.calendarEventSource.hasPermission(this@MainActivity)
                if (!calendarPermissionGranted.value) {
                    calendarEvents.value = emptyList()
                    calendarForecasts.value = emptyMap()
                    calendarLoadError.value = null
                    return
                }
                calendarLoading.value = true
                calendarLoadError.value = null
                lifecycleScope.launch {
                    try {
                        val now = java.time.Instant.now()
                        val events = app.container.calendarEventSource.upcomingEvents(
                            now,
                            now.plus(30L, java.time.temporal.ChronoUnit.DAYS),
                        )
                        calendarEvents.value = events
                        calendarForecasts.value = emptyMap()
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (failure: Exception) {
                        calendarLoadError.value = failure.message ?: "Unable to load calendar events."
                    } finally {
                        calendarLoading.value = false
                    }
                }
            }
            val calendarPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                calendarPermissionGranted.value = granted
                if (granted) {
                    refreshCalendarEvents()
                }
            }
            if (showOnboarding.value) {
                OnboardingWizardScreen(
                    catalog = catalog,
                    locations = onboardingLocations,
                    selectedLocationId = app.container.userPrefs.locationId,
                    notificationPermissionGranted = notificationPermissionGranted.value,
                    onRequestNotificationPermission = {
                        if (Build.VERSION.SDK_INT >= 33) notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        else notificationPermissionGranted.value = true
                    },
                    onDone = {
                        showOnboarding.value = false
                        app.container.userPrefs.onboarded = true
                        ClothesRainAlarmWorker.reconcile(
                            this@MainActivity,
                            enabled = app.container.userPrefs.notificationsEnabled &&
                                app.container.userPrefs.clothAlarmEnabled &&
                                notificationPermissionGranted.value,
                        )
                        app.container.userPrefs.personas = it.personas.map { persona -> persona.id }
                        it.selectedLocation?.let { location ->
                            app.container.userPrefs.rememberLocation(location)
                        }
                        app.container.userPrefs.locationId = it.selectedLocationId
                        app.container.userPrefs.cardsOrder = it.cardOrder
                        destination.value = AppDestination.HOME
                    },
                    language = language,
                    onLanguageChange = { selected ->
                        language = selected
                        app.container.userPrefs.language = selected
                    },
                )
            } else {
                val homeViewModel = remember {
                    HomeViewModel(
                        prefs = app.container.userPrefs,
                        catalog = catalog,
                        dataSource = app.container.weatherDataSource,
                        notificationHelper = app.notificationHelper,
                        onHomeLoaded = { WidgetRefreshScheduler.enqueueDebounced(this@MainActivity) },
                    )
                }
                val state by homeViewModel.uiState.collectAsState()
                BackHandler(enabled = destination.value != AppDestination.HOME || activeSheet.value != null) {
                    if (activeSheet.value != null) activeSheet.value = null
                    else destination.value = AppDestination.HOME
                }

                when (destination.value) {
                    AppDestination.HOME -> HomeScreen(
                        state = state,
                        onLocationClick = { activeSheet.value = HomeSheet.LOCATION },
                        onSelectSavedDestination = homeViewModel::setLocation,
                        onEditCardsClick = { activeSheet.value = HomeSheet.CARDS },
                        onTripPlannerClick = { destination.value = AppDestination.TRIP },
                        onCalendarClick = {
                            destination.value = AppDestination.CALENDAR
                            refreshCalendarEvents()
                        },
                        onSettingsClick = { destination.value = AppDestination.SETTINGS },
                        onAlertsClick = { destination.value = AppDestination.ALERTS },
                        onRetryClick = homeViewModel::load,
                        onListen = { text -> speaker.speak(text) },
                        onSmartActionConfirm = homeViewModel::confirmSmartAction,
                        onSmartActionDismiss = homeViewModel::dismissSmartAction,
                    )
                    AppDestination.TRIP -> {
                        val tripViewModel = remember {
                            TripPlannerViewModel(app.container.userPrefs, catalog, app.container.weatherDataSource)
                        }
                        val tripState by tripViewModel.uiState.collectAsState()
                        TripPlannerScreen(
                            state = tripState,
                            onBack = { destination.value = AppDestination.HOME },
                            onRouteSelect = tripViewModel::selectRoute,
                            onDepartureChange = tripViewModel::selectDeparture,
                            onUseSuggestion = tripViewModel::useSuggestion,
                        )
                    }
                    AppDestination.SETTINGS -> SettingsScreen(
                        notificationsEnabled = notificationsEnabled.value && notificationPermissionGranted.value,
                        clothAlarmEnabled = clothAlarmEnabled.value,
                        onBack = { destination.value = AppDestination.HOME },
                        onPersonas = { activeSheet.value = HomeSheet.PERSONA },
                        onLocation = { activeSheet.value = HomeSheet.LOCATION },
                        onNotificationsChange = { enabled ->
                            if (enabled && Build.VERSION.SDK_INT >= 33 && !app.notificationHelper.hasPermission()) {
                                pendingNotificationEnable.value = true
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                notificationsEnabled.value = enabled
                                app.container.userPrefs.notificationsEnabled = enabled
                                ClothesRainAlarmWorker.reconcile(
                                    this@MainActivity,
                                    enabled = enabled && clothAlarmEnabled.value &&
                                        app.notificationHelper.hasPermission(),
                                )
                            }
                        },
                        onClothAlarmChange = { enabled ->
                            clothAlarmEnabled.value = enabled
                            app.container.userPrefs.clothAlarmEnabled = enabled
                            ClothesRainAlarmWorker.reconcile(
                                this@MainActivity,
                                enabled = enabled && notificationsEnabled.value &&
                                    app.notificationHelper.hasPermission(),
                            )
                        },
                        onAlerts = { destination.value = AppDestination.ALERTS },
                        onReset = {
                            app.container.userPrefs.clear()
                            ClothesRainAlarmWorker.reconcile(this@MainActivity, enabled = false)
                            language = app.container.userPrefs.language
                            highContrast = app.container.userPrefs.highContrastEnabled
                            largeText = app.container.userPrefs.largeTextEnabled
                            notificationsEnabled.value = app.container.userPrefs.notificationsEnabled
                            clothAlarmEnabled.value = app.container.userPrefs.clothAlarmEnabled
                            destination.value = AppDestination.HOME
                            showOnboarding.value = true
                        },
                        language = language,
                        onLanguageChange = { selected ->
                            language = selected
                            app.container.userPrefs.language = selected
                        },
                        highContrast = highContrast,
                        largeText = largeText,
                        onHighContrastChange = { enabled ->
                            highContrast = enabled
                            app.container.userPrefs.highContrastEnabled = enabled
                        },
                        onLargeTextChange = { enabled ->
                            largeText = enabled
                            app.container.userPrefs.largeTextEnabled = enabled
                        },
                    )
                    AppDestination.ALERTS -> AlertsScreen(state = state, onBack = { destination.value = AppDestination.HOME })
                    AppDestination.CALENDAR -> CalendarEventsScreen(
                        hasPermission = calendarPermissionGranted.value,
                        isLoading = calendarLoading.value,
                        events = calendarEvents.value,
                        eventForecasts = calendarForecasts.value,
                        analyzingEventKey = analyzingEventKey.value,
                        locations = state.locations,
                        currentLocationId = state.location.id,
                        isEventPlannerEnabled = state.selectedPersonas.any { it.id == "event_planner" },
                        loadError = calendarLoadError.value,
                        onBack = { destination.value = AppDestination.HOME },
                        onRefresh = { refreshCalendarEvents() },
                        onRequestPermission = {
                            if (Build.VERSION.SDK_INT >= 23) {
                                calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                            } else {
                                calendarPermissionGranted.value = true
                                refreshCalendarEvents()
                            }
                        },
                        onAnalyzeEvent = { event, isOutdoor, isOrganizer, location ->
                            analyzingEventKey.value = event.instanceKey
                            lifecycleScope.launch {
                                try {
                                    val series = app.container.weatherDataSource.series(location)
                                    val zone = location.timeZone?.let(ZoneId::of) ?: ZoneId.systemDefault()
                                    val eventStart: java.time.LocalDateTime
                                    val eventEnd: java.time.LocalDateTime
                                    if (event.isAllDay) {
                                        eventStart = event.start.atZone(ZoneOffset.UTC).toLocalDate().atStartOfDay(zone).toLocalDateTime()
                                        val endInstant = event.end ?: event.start.plus(1, java.time.temporal.ChronoUnit.DAYS)
                                        eventEnd = endInstant.atZone(ZoneOffset.UTC).toLocalDate().atStartOfDay(zone).toLocalDateTime()
                                    } else {
                                        eventStart = event.start.atZone(zone).toLocalDateTime()
                                        eventEnd = (event.end ?: event.start.plus(1, java.time.temporal.ChronoUnit.HOURS))
                                            .atZone(zone).toLocalDateTime()
                                    }
                                    val evaluation = CalendarEventWeatherEvaluator.evaluate(
                                        series = series,
                                        eventStart = eventStart,
                                        eventEnd = eventEnd,
                                        personas = state.selectedPersonas,
                                        isCoastal = location.isCoastal,
                                        isOutdoor = isOutdoor,
                                        isOrganizer = isOrganizer,
                                    )
                                    calendarForecasts.value = calendarForecasts.value + (
                                        event.instanceKey to if (evaluation == null) {
                                            CalendarEventForecast(
                                                locationName = location.name,
                                                isOutdoor = isOutdoor,
                                                isOrganizer = isOrganizer,
                                                error = "Forecast coverage does not include the full event duration.",
                                            )
                                        } else {
                                            CalendarEventForecast(
                                                locationName = location.name,
                                                isOutdoor = isOutdoor,
                                                isOrganizer = isOrganizer,
                                                weatherRows = evaluation.weatherRows,
                                                advisories = evaluation.advisories,
                                            )
                                        }
                                    )
                                } catch (cancellation: CancellationException) {
                                    throw cancellation
                                } catch (failure: Exception) {
                                    calendarForecasts.value = calendarForecasts.value + (
                                        event.instanceKey to CalendarEventForecast(
                                            locationName = location.name,
                                            isOutdoor = isOutdoor,
                                            isOrganizer = isOrganizer,
                                            error = failure.message ?: "Unable to load weather for this event.",
                                        )
                                    )
                                } finally {
                                    if (analyzingEventKey.value == event.instanceKey) {
                                        analyzingEventKey.value = null
                                    }
                                }
                            }
                        },
                    )
                }

                if (activeSheet.value == HomeSheet.LOCATION) {
                    LocationSheet(
                        locations = state.locations,
                        selectedLocationId = state.location.id,
                        currentAqi = state.currentRow?.aqiValue,
                        searchQuery = state.locationSearchQuery,
                        searchResults = state.locationSearchResults,
                        isSearching = state.isSearchingLocations,
                        searchError = state.locationSearchError,
                        onSearchQueryChange = homeViewModel::searchLocations,
                        onQuickCity = homeViewModel::searchLocations,
                        savedDestinationIds = state.savedDestinationIds.toSet(),
                        onSelect = { location -> homeViewModel.setLocation(location); activeSheet.value = null },
                        onToggleSaved = homeViewModel::toggleSavedDestination,
                        onDismiss = { activeSheet.value = null },
                    )
                }
                if (activeSheet.value == HomeSheet.PERSONA) {
                    PersonaSheet(
                        personas = catalog.personas.values.toList(),
                        selectedIds = state.selectedPersonas.map { it.id }.toSet(),
                        onApply = { ids -> homeViewModel.setPersonas(ids); activeSheet.value = null },
                        onDismiss = { activeSheet.value = null },
                    )
                }
                if (activeSheet.value == HomeSheet.CARDS) {
                    EditCardsSheet(
                        allCards = catalog.cards.values.toList(),
                        defaultIds = (state.selectedPersonas.flatMap { it.defaultCards } + "trip_planner_card").distinct(),
                        orderedIds = state.orderedCardIds,
                        disabledIds = state.disabledCards,
                        isCoastal = state.location.isCoastal,
                        onSave = homeViewModel::setCardConfiguration,
                        onReset = homeViewModel::resetCardConfiguration,
                        onDismiss = { activeSheet.value = null },
                    )
                }
            }
            if (showVoiceInstallPrompt) {
                AlertDialog(
                    onDismissRequest = { showVoiceInstallPrompt = false },
                    title = { Text(LocalAppStrings.current.text("voice_not_available", "Speech voice unavailable")) },
                    text = { Text(LocalAppStrings.current.text("install_voice_description", "Install the selected language voice in Android text-to-speech settings.")) },
                    confirmButton = {
                        TextButton(onClick = {
                            showVoiceInstallPrompt = false
                            runCatching {
                                voiceInstallerLauncher.launch(Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA))
                            }.onFailure {
                                showVoiceInstallPrompt = true
                            }
                        }) { Text(LocalAppStrings.current.text("install", "Install")) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showVoiceInstallPrompt = false }) {
                            Text(LocalAppStrings.current.text("close", "Close"))
                        }
                    },
                )
            }
            }
            }
            }
        }
    }
}