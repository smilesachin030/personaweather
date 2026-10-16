# PersonaWeather — Complete Implementation Spec for the AI Coder (v1.0)

Native Android (Kotlin + Jetpack Compose) · Offline-first · Persona-driven hyper-local weather & environmental advisory app · Chennai hub, 15 locations

---

## PART A — READ THIS FIRST

### A1. Notes for the human (you)

1. Do not add weather data files to the project. Only the non-weather persona/rule configuration described in A4 may be bundled.
2. Paste the **Master Prompt** (A2) into your AI coder, then attach this whole document.
3. Build **one phase at a time** (Section 17). After each phase, run the check in that phase's "You verify" line, then tell the AI "next".
4. Everything you run is your existing pipeline: `./gradlew assembleDebug` then `adb install -r app/build/outputs/apk/debug/app-debug.apk`.

### A2. Master Prompt (paste this to the AI coder)

```
You are a senior Android engineer. Build the app described in the attached spec, exactly, in phases.

RULES
1. Kotlin + Jetpack Compose + Gradle Kotlin DSL. NO Room, NO Hilt/Dagger, NO Retrofit/OkHttp, NO Navigation library,
   NO Maps SDK. Open-Meteo Weather, Air Quality, and Marine endpoints are the only allowed network calls; the manifest declares INTERNET.
2. Work ONE phase at a time (spec Section 17). At the end of a phase: the project must compile with
   `./gradlew assembleDebug`; list files created/changed; list each acceptance criterion as met / not met.
   Do NOT start the next phase until I say "next".
3. Package `core/` must contain NO `android.*` or `androidx.*` imports (pure Kotlin) so it can move to a backend later.
4. Use only weather data returned by the approved Open-Meteo Weather, Air Quality, and Marine endpoints. Do not bundle, generate, infer, or fall back to synthetic/sample weather. Never invent observations, forecasts, or unsupported fields. Persona/rule configuration may be bundled; weather values may not.
5. No magic numbers in composables. Thresholds live in persona_rules.json or in core/engine/Tuning.kt.
6. When the spec is silent, choose the SIMPLEST option that satisfies the goal and record it in DECISIONS.md.
7. Match the visual design in Section 11 closely — UI quality is a primary success criterion.
8. Write core and provider-contract tests in Section 18. Never include fabricated weather scenarios or values in tests.
```

### A3. What we are building

**Positioning:** a *Contextual Advisory & Decision-Support Engine*, not another weather dashboard. It turns current provider forecasts into persona-specific guidance, always showing source and coverage and never inventing conditions when data is absent.

**The 6 differentiators the UI and code must make obvious (judging criteria):**

| # | Differentiator | Where it shows |
|---|---|---|
| 1 | Persona tailoring: one-time multi-select onboarding, smart default cards, user can edit cards | Onboarding, Home card grid, Edit cards |
| 2 | Persona-specific thresholds rule engine (JSON, no code change to tune) | Advisory banners, card footers |
| 3 | Hyper-local: weather requests use verified coordinates for supported Chennai-area localities; each locality displays only its own provider response | Location switcher |
| 4 | Route weather matched to *arrival time at each stop* (not "weather now") | Trip Planner |
| 5 | Actionable insights: best run window, clothes-drying window, tide highs/lows, best day for an event | Cards |
| 6 | Offline-capable without synthetic data: online provider forecasts are cached and a prepared trip pack carries only real provider data for a planned offline trek; unavailable data stays unavailable | Settings, Home, Trip Planner, source label, About |

### A4. Bundled configuration

Only non-weather configuration may be bundled in `app/src/main/assets/`:

| File | What it is |
|---|---|
| `persona_rules.json` | Persona/card metadata and advisory thresholds only. It must not contain weather observations, forecasts, or example values presented as real conditions. |

Do not bundle `weather_advisory.db`, weather spreadsheets, canned weather responses, or other weather-value assets. Resolve coordinates from the verified locality catalogue and request forecasts from the approved provider. Record the provider, model/source, retrieval time, and coverage with every response.

### A5. Scope

| Priority | Feature |
|---|---|
| **P0 (must ship)** | Provider-backed data loading · current device time · onboarding (multi-select personas, location) · Home (hero, rain sentence, advisory banners, hourly strip, forecast list, persona card grid) · all data-supported card types · location switcher for verified localities · edit cards · Trip Planner · local notifications for real provider-backed alerts · offline cache states without invented data |
| **P1** | 24h chart with metric tabs (Google style) · "Upcoming alerts" screen · best-departure suggestion in Trip Planner · clothes-drying alarm channel with alarm sound · collapsing hero header · provider-returned sunrise/sunset markers |
| **P1** | Open-Meteo Weather, Air Quality, and Marine forecasts as the only weather source; always clearly identify modeled forecasts and unavailable fields |
| **P1** | Reliable offline behavior across the app, plus an explicit pre-trip download that saves a route/date-specific provider forecast pack for a planned trek |
| **P1** | Android home-screen widgets: compact current conditions/advisory and medium hourly outlook, using provider-backed forecasts and persona rules |
| **P2** | Tamil UI + on-device text-to-speech "Listen" button · custom vector weather glyphs · high-contrast / large-text mode |
| **Calendar feature (implemented in app)** | Read upcoming events from Android's native Calendar Provider; require indoor/outdoor confirmation and selection of a verified forecast locality. Keep raw calendar location text on-device, assess the full event duration, and report when provider coverage is insufficient. Indoor events receive clearly caveated outdoor AQI advice only. This is not a Google API-key or OAuth service integration. |
| **PARKED (design hooks only — do NOT build)** | Google Maps/Routes API, Slack, Google Home actuation, rolling 24h AQI-exposure tracker, Sarvam AI speech-to-text |

---

## PART B — TECHNOLOGY

## 1. Tech stack

| Concern | Choice | Why |
|---|---|---|
| Language | Kotlin (2.x) | Required |
| UI | Jetpack Compose + Material 3 (used as a base, heavily restyled) | Decided |
| Build | Gradle Kotlin DSL + version catalog (`libs.versions.toml`); Compose compiler via plugin `org.jetbrains.kotlin.plugin.compose` | Matches existing pipeline |
| minSdk / target | minSdk 26; compileSdk & targetSdk = highest stable SDK installed locally | `java.time` works natively from API 26 |
| State | `AndroidViewModel` + `StateFlow`; collect with `collectAsStateWithLifecycle()` | Decided |
| Data | Open-Meteo APIs plus app-private persistent response cache | No bundled or generated weather data |
| Rules | `assets/persona_rules.json` parsed with built-in `org.json` | Decided |
| Persistence | `SharedPreferences` (small values) | Simplest |
| DI | None. A hand-written `AppContainer` created in the `Application` class | Simplest |
| Navigation | A `sealed interface Screen` + a `MutableList<Screen>` back stack + `BackHandler` | 6 screens; no library needed |
| Icons | `androidx.compose.material:material-icons-extended` for UI chrome; **emoji** for weather glyphs in P0 (colorful, zero dependency) | See Section 11.6 |
| Notifications | `NotificationCompat` from `androidx.core` | Built in |
| Tests | JUnit4 + `kotlinx-coroutines-test`; `testImplementation("org.json:json:<recent>")` so `org.json` works in JVM unit tests | Android's stub `org.json` throws in unit tests |

**Dependencies to declare** (use latest stable versions; pick a Compose BOM): `androidx.core:core-ktx`, `androidx.activity:activity-compose`, `androidx.lifecycle:lifecycle-viewmodel-compose`, `androidx.lifecycle:lifecycle-runtime-compose`, `androidx.compose:compose-bom`, `ui`, `ui-graphics`, `ui-tooling-preview`, `material3`, `material-icons-extended`. `debugImplementation("androidx.compose.ui:ui-tooling")`.
For Phase 10, also add the stable AndroidX Glance app-widget and Glance Material 3 artifacts, plus WorkManager KTX for bounded periodic refreshes.

**`app/build.gradle.kts` skeleton**

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)   // org.jetbrains.kotlin.plugin.compose
}
android {
    namespace = "com.example.personaweather"
    compileSdk = 35   // or the highest installed
    defaultConfig {
        applicationId = "com.example.personaweather"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
    buildFeatures { compose = true }
    testOptions { unitTests.isReturnDefaultValues = true }
}
```

**`AndroidManifest.xml` essentials:** `POST_NOTIFICATIONS` and `INTERNET`. Network failure must not crash the app; without a valid cache or offline trip pack, weather-dependent UI reports unavailable rather than inventing values.

---

## 2. Architecture & package layout

```
com.example.personaweather/
  PersonaWeatherApp.kt              Application; builds AppContainer
  MainActivity.kt                   enableEdgeToEdge(); setContent { AppRoot() }
  core/                             PURE KOTLIN — no android.* / androidx.* imports
    model/   Location, WeatherRow, Persona, Rule, Advisory, CardSpec, Severity, DailySummary, Waypoint
    engine/  AdvisoryEngine, Insights, RouteWeather, Tuning, Labels
    time/    TimeUtils (rounding, timezone, day/night)
  data/
    WeatherDataSource.kt            provider/cache contract (see 3.4)
    OpenMeteoWeatherDataSource.kt   only weather provider; Weather, Air Quality, Marine
    LiveWeatherCache.kt             private per-locality/endpoint cache and freshness metadata
    OfflinePackRepository.kt        versioned, user-managed trip packs in app-private storage (Phase 11)
    RulesLoader.kt                  org.json -> Persona/Rule/CardSpec
    UserPrefs.kt                    SharedPreferences wrapper
    AppContainer.kt
  notify/  NotificationHelper.kt
    widget/  WeatherWidget.kt, WeatherWidgetReceiver.kt, WidgetConfigActivity.kt,
                     WidgetSnapshot.kt, WidgetPreferences.kt, WidgetRefreshWorker.kt
  tts/     Speaker.kt               (P2)
  viewmodel/ AppViewModel.kt, UiState.kt
  ui/
    theme/       Color.kt, Type.kt, Shapes.kt, Gradients.kt, Theme.kt
    components/  GlassCard, SeverityBanner, Gauge, WeatherGlyph, Sparkline, SectionHeader, ChipRow, PersonaTile
    onboarding/  OnboardingScreen
    home/        HomeScreen, HeroSection, RainSentence, HourlyStrip, DailyList, CardGrid, cards/*
    trip/        TripPlannerScreen, RouteMap
    alerts/      AlertsScreen
    settings/    SettingsScreen, EditCardsSheet, LocationSheet, PersonaSheet
    AppRoot.kt                      Screen back stack
```

**Data flow:** `Open-Meteo Weather/Air Quality/Marine → validated provider response → persistent per-locality/endpoint cache → Home/Trip view models → core engine → Compose UI`. Use the device's current time in `Asia/Kolkata`; no simulated clock or alternate weather mode exists.
Every value carries provider/model, retrieval time, and forecast coverage. The UI distinguishes fresh provider responses, cached provider responses, stale last-known responses, and unavailable data. There is no bundled-weather, mock-weather, or sample-weather fallback. Phase 11's offline pack repository stores a self-contained snapshot for a user-selected route and date range, based only on provider responses.
Widgets use the same provider/cache and core engine through `WidgetSnapshotUseCase`; they do not issue independent requests or render fixture weather.

**Rule:** UI never touches SQLite or JSON. ViewModel never builds UI. `core/` knows nothing about Android.

---

## 3. Data layer

### 3.1 Provider and locality contract

Open-Meteo Weather, Air Quality, and Marine endpoints are the only weather sources. Use a verified locality catalogue containing stable IDs, accurate coordinates, display names, and coastal eligibility; the catalogue is metadata, not weather data. Do not infer AQI, pollen, tides, soil state, or other conditions from locality labels.

Every provider response is validated and mapped into timestamped `WeatherRow` values with explicit nullable fields. Retain endpoint/model identity, retrieval time, timezone, and forecast coverage alongside each series. Requests use `Asia/Kolkata` for Chennai localities. A provider value absent from a response remains `null`; unsupported pollen or other unsupported measures display **Unavailable from provider**, never a substituted estimate.

### 3.2 Persistent response cache

Use app-private persistent storage for successful provider responses and metadata. The cache is not an alternate source: it stores only actual responses obtained from the approved provider. Write each endpoint response atomically after validation; a failed refresh must not overwrite the last complete response. Keep endpoint data separate so a missing AQI response cannot be filled with unrelated weather or a fabricated value.

Cache identity includes locality coordinates, endpoint, requested variables, units, timezone, and model parameters. Cache metadata includes provider/model, request/response time, forecast start/end, and schema version. Retain useful responses across process restarts; define a bounded retention policy and storage limits. Use cached values only for timestamps within their real provider coverage and show stale/out-of-range state otherwise.

### 3.3 Request and mapping behavior

Perform requests on `Dispatchers.IO` with bounded connect/read timeouts. Validate HTTP status, JSON shape, units, timestamps, and parallel-array lengths before mapping. Return typed success, partial, unavailable, and failure states; preserve successful independent endpoint data if another endpoint fails. Never turn a parse/network failure into a successful empty forecast.

Normalize provider time series into ascending hourly rows only where the provider actually returned that timestamp. Do not interpolate, extrapolate, fill gaps, or fabricate defaults. Derived advisories and insights may be calculated only from real provider-backed rows and the bundled persona rules. A derived result must carry the underlying data coverage and source.

### 3.4 Data-source contract

```kotlin
interface WeatherDataSource {
    suspend fun locations(): List<Location> // verified supported localities
    suspend fun forecast(locationId: String, from: Instant, through: Instant): ForecastResult
}
class OpenMeteoWeatherDataSource(...) : WeatherDataSource
class CachedWeatherDataSource(...) : WeatherDataSource // returns only persisted provider responses
```

There is no Demo, fixture, synthetic, mock-weather, or bundled-weather data source. The app may use the last valid provider response when offline, with its original timestamps/source labels; otherwise it returns unavailable.

### 3.5 Models (core/model)

`WeatherRow` is a typed, nullable representation of provider-returned values plus its `Instant`, locality ID, provider/model, and endpoint coverage. `fieldValue(name)` returns the mapped value or `null` for unavailable/unsupported columns. Rules must skip nulls. Never assign a plausible-looking default to missing weather.

### 3.6 Current time and forecast bounds

Use the device clock in `Asia/Kolkata` for display and select the matching provider-returned hourly timestamp. Do not let users scrub time, autoplay, or move beyond returned forecast coverage. When current time or a requested trip/event time is outside provider coverage, show **Forecast unavailable for this time** and do not derive an advisory, recommendation, or route verdict for that time.

### 3.7 Provider-data constraints

- Coastal eligibility controls whether the Marine endpoint is requested; a missing marine value remains unavailable.
- Use provider-returned sunrise/sunset only when available; otherwise label the value unavailable rather than using date-specific constants.
- Display the provider's AQI scale/category and provider/model attribution; do not relabel as a local station observation.
- Do not claim station observations unless an approved observation source is added. Current conditions sourced from forecast endpoints must be labeled as model forecasts.
- Use only provider-returned condition codes and variables. Unknown or newly added provider values must degrade to an explicit unavailable/unknown state.

---

## 4. Personalization engine

### 4.1 Personas (from JSON)

`health_conscious · outdoor_fitness · beach_surfer (requires_coastal) · traveler · parent_family · agriculture_gardener · commuter · event_planner`

Onboarding is **multi-select, at least 1**. Persona order in UI = the JSON order above.

### 4.2 Cards

`persona_rules.json → cards` defines 15 card ids with label + the columns each needs. Persona → `default_cards`. **Default card list for a user** = union of the selected personas' `default_cards`, in persona order, de-duplicated (first occurrence wins). Then the user may **enable/disable and reorder** (Section 12.8).

Behavior when personas change later (from the Persona sheet):
- If the user never customized cards (`cards_customized=false`) → replace with the new defaults.
- If they did customize → keep their order and enabled-state, **append** newly implied cards (enabled) at the end, and show a snackbar: "Added N cards for <Persona>".

### 4.3 Rule schema (v2)

```json
{ "id":"pf_cloth_alert", "field":"rain_chance_percent", "operator":">=", "value":60,
  "severity":"alert", "message":"Rain expected within 3 hours — bring in any drying clothes.",
  "lookahead_hours":3, "aggregate":"max" }
```
- `operator`: `>= <= > < == !=`. `value`: number **or** string (string compare is case-insensitive, only `==`/`!=`).
- Optional `lookahead_hours` + `aggregate` (`max|min|sum|avg`): evaluate the aggregate of the field over rows `[now, now + N hours]` **inclusive** (N+1 rows) instead of the single current row.
- Optional `message_ta` (P2).

### 4.4 Engine contract (core/engine/AdvisoryEngine.kt)

```kotlin
enum class Severity { CAUTION, ALERT }

data class Advisory(
    val ruleId: String, val personaId: String, val field: String,
    val severity: Severity, val message: String, val messageTa: String?,
    val observed: Any?, val isLookahead: Boolean
)

object AdvisoryEngine {
    fun evaluate(
        series: List<WeatherRow>,      // one location, hourly ascending
        nowIndex: Int,                 // index of the row for "now"/ETA
        personas: List<Persona>,       // ordered
        isCoastal: Boolean,
        fieldFilter: Set<String>? = null   // used by Trip Planner (Section 9)
    ): List<Advisory>
}
```

**Algorithm (implement literally):**

```kotlin
fun observedValue(rule: Rule, series: List<WeatherRow>, i: Int): Any? {
    if (rule.lookaheadHours == null) return series.getOrNull(i)?.fieldValue(rule.field)
    val slice = series.subList(i, minOf(series.size, i + rule.lookaheadHours + 1))
    val nums = slice.mapNotNull { (it.fieldValue(rule.field) as? Number)?.toDouble() }
    if (nums.isEmpty()) return null
    return when (rule.aggregate) { "max" -> nums.max(); "min" -> nums.min(); "sum" -> nums.sum(); "avg" -> nums.average(); else -> null }
}
fun trips(rule: Rule, observed: Any?): Boolean {
    if (observed == null) return false                       // NULL never trips a rule
    return when (val v = rule.value) {
        is Number -> (observed as? Number)?.toDouble()?.let { compare(it, v.toDouble(), rule.op) } ?: false
        is String -> when (rule.op) { "==" -> observed.toString().equals(v, true); "!=" -> !observed.toString().equals(v, true); else -> false }
        else -> false
    }
}
```

Steps inside `evaluate`:
1. For each persona; **skip persona entirely if `requiresCoastal && !isCoastal`**.
2. Skip rules whose `field` is not in `fieldFilter` (when provided).
3. Evaluate each rule with `observedValue` + `trips`.
4. **De-duplicate per `(personaId, field)`**: keep the highest severity; if equal, the later-defined rule.
5. Sort: `ALERT` before `CAUTION`; then persona order; then rule order.
6. Return the list. **No UI text formatting here.**

### 4.5 RulesLoader (data/RulesLoader.kt)

Parse `persona_rules.json` with `org.json`. `value` may be number or string: `when (val raw = obj.get("value")) { is Number -> raw.toDouble(); is String -> raw }`. Build `Persona(id, label, requiresCoastal, defaultCards, rules)` and `CardSpec(id, label, fields, requiresCoastal, derived)`. If parsing fails → show the error state (Section 12.9), never crash.

### 4.6 Persistence (`UserPrefs`, file `persona_weather_prefs`)

| Key | Type | Notes |
|---|---|---|
| `onboarded` | Boolean | |
| `personas` | String (comma-separated ids) | ordered; StringSet is unordered — do not use it |
| `location_id` | String | default `nungambakkam`; coordinates come from the verified locality catalogue |
| `cards_order` | String (comma-separated card ids) | |
| `cards_disabled` | StringSet | |
| `cards_customized` | Boolean | |
| `language` | String `en`/`ta` | P2 |
| `cloth_alarm_enabled` | Boolean | default true |
| `notifications_enabled` | Boolean | default true |
| `last_successful_refresh` | String ISO | informational; actual current view is determined by provider timestamps and coverage |

**Default location decision:** `nungambakkam` (central Chennai locality, with coordinates verified independently of any weather dataset). The user changes it any time from the top-bar chip; onboarding step 3 lets them choose.

---

## 5. Derived insights (core/engine/Insights.kt) — pure functions

All take `series` + `nowIndex` and are unit-tested. Every threshold is a constant in `Tuning.kt`.

### 5.1 Best running window

- Candidate hours: hours 5–21 within the next 24 rows. Window = 2 consecutive hours.
- Per-hour penalty = `0.5*max(0, temp-28) + 0.05*max(0, aqi-50) + 0.5*uv + 0.05*rain_chance + 0.1*max(0, wind-15)`. Window penalty = sum of its two hours.
- Choose the minimum-penalty window (ties → earliest). If the best penalty ≥ `RUN_NOT_RECOMMENDED_PENALTY = 14.0` → result `NotRecommended` ("Skip the outdoor run — try an indoor workout").
- Output: `start`, `end`, and the row values (AQI, temp, UV) for the sub-line.
- Return a window only when all required provider fields are present across its full interval; otherwise label the result unavailable. Never present a precomputed or hand-authored "best" window.

### 5.2 Clothes-drying window (family persona)

- Look at remaining hours today from `max(now, 08:00)` to `17:00`.
- A hour is "dry" if `rain_chance_percent < 25` (`DRY_MAX_RAIN`).
- Find the **longest** run of consecutive dry hours with length ≥ 3 (`DRY_MIN_WINDOW_HOURS`); tie → lowest average humidity.
- No window today → look at tomorrow (08:00–17:00) and show "Tomorrow 9 AM – 1 PM"; if none either → "No good drying window — dry indoors".
- Status chip: `Good to dry` (a dry window includes now) / `Rain soon — bring in` (a `pf_cloth_alert` advisory is active) / `Wait until 9 AM` (window starts later).

### 5.3 Tide events (coastal only)

From `tide_height_m` over the next 24 rows, find local maxima (HIGH) and minima (LOW): row `i` is a HIGH if `h[i] > h[i-1] && h[i] >= h[i+1]`; LOW mirrored. Return `List<TideEvent(type, time, height)>`. Card shows "Next high tide 4:00 PM" and draws the curve.

### 5.4 Rain sentence (hero sub-line, Apple-style)

Scan the next 24 rows using `RAIN_LIKELY = rain_chance_percent >= 50`:
- Now ≥ 50 → "Rain now, easing around {first later hour with rain < 35}"
- Else first hour ≥ 50 within 24h → "Rain expected around {time} ({pct}%)"
- Else → "No rain expected in the next 24 hours"

### 5.5 Daily summaries (7-day list & event card)

For each of 7 dates from today: `minTemp, maxTemp, maxRain, avgAqi, maxAqi, totalRainMm, dominantCondition` and `eventScore = Σ over hours 08–20 of (Poor=3, Fair=1, Good=0)` using `comfort_index`. `dominantCondition` glyph = Thunderstorm if any Thunderstorm hour; else Showers if `maxRain>=60`; else Scattered if `maxRain>=35`; else Sunny.
Best event day = minimum `eventScore` (tie → earliest). Verified for Velachery from Mon 28 Sep: **best = Sat 3 Oct (score 0)**, worst = Sun 4 Oct (score 36, "Poor", 66% rain).

### 5.6 Packing tips (Traveler card)

From current + next 12 hours: `uv>=8 → "Sunscreen & cap"`, `rain max>=40 → "Umbrella"`, `temp>=33 → "Water bottle, light cottons"`, `aqi max>=100 → "N95 mask"`. Show as chips; if none → "Pack light — nothing special".

### 5.7 "Today for you" headline (hero)

Ordered rules; first match wins:
1. Any `ALERT` advisory → its `message` (prefixed by "Alert:").
2. Rain sentence indicates rain within 3 hours → that sentence.
3. Any `CAUTION` advisory → its `message`.
4. Persona-positive lines, first persona in user order that has one: fitness → "Best run: {window}"; parent_family → "Good drying window {window}" ; event_planner → "Best day this week: {day}"; agriculture → soil advice; commuter → "Smooth commute expected".
5. Default → "Comfortable conditions · AQI {category}".

### 5.8 Persona action advice (Home)

Show up to three short, persona-specific suggestions in a separate **Weather advice** card; do not place weather tips in the Smart Home device-action card. `WeatherActionSuggestionPolicy` evaluates only selected personas and existing provider-backed forecast rows. Every displayed suggestion includes its supporting values ("Why") and must not claim more certainty than the forecast provides. The policy is local and deterministic; it is not a trained model because the weather provider does not supply action labels.

- Rain: within the next 12 forecast hours, ≥35% prompts raincoat advice and ≥60% prompts umbrella advice; commuter advice uses the next 2 hours. Include the forecast time and probability.
- Parent & family: use tomorrow's maximum rain probability (≥40%) for school-travel preparation only when all 24 hourly rows for tomorrow are available. At ≥70%, tell users to check official school/local-authority notices; never predict a school closure or holiday from weather alone. Otherwise use a near-term family rain-gear suggestion. AQI and heat advice stay grounded in current values.
- Health, fitness, travel, surf, agriculture, and events: advise on UV/AQI/pollen, heat, fog/visibility, marine wave/wind, next-24-hour rainfall/soil moisture (only when a full 24 hourly rows are available), and the wettest upcoming forecast day respectively. A missing coastal value is not a calm-sea signal.
- Heat hydration and food (planned extension): extend the existing heat advice for eligible selected personas (health-conscious, outdoor fitness, parent & family, and traveler) when the existing heat-alert or feels-like threshold is met. Keep water as the primary action (drink water regularly; carry a bottle) and optionally name familiar water-rich foods such as watermelon or cucumber as food ideas, not treatment or a substitute for water. Reuse the existing heat trigger and forecast evidence; do not prescribe a fluid amount, electrolytes, or a diet. Merge with an existing heat message and Traveler packing tip rather than showing duplicate water advice. Localize the food examples and allow a neutral food-free hydration message where examples are not suitable.
- Dietary advice is general weather-linked wellness guidance, not medical or personalized nutrition advice. Do not infer a user's condition, allergy, age, or food preference; do not make claims about preventing or treating heat illness. For severe symptoms, defer to official emergency/health guidance rather than food suggestions. Food storage/safety advice is out of scope without appropriate data.
- If multiple personas produce the same concern, keep only the highest-priority message for that concern; sort deterministically and cap the card at three. Safety/severity banners and notifications remain driven by `AdvisoryEngine`, independently of these suggestions.

### 5.9 Label helpers (core/engine/Labels.kt — no Compose colors here)

- **AQI bands:** 0–50 Good, 51–100 Moderate, 101–150 Sensitive, 151–200 Unhealthy, 201+ Very unhealthy.
- **UV:** 0–2 Low, 3–5 Moderate, 6–7 High, 8–10 Very high, 11+ Extreme.
- **Dew point comfort:** <16 Dry, 16–20 Comfortable, 20–24 Humid, ≥24 Muggy.
- **Wave height:** <1.0 Calm, 1.0–1.49 Moderate, 1.5–1.99 Rough (caution), ≥2.0 Dangerous (alert).
- **Pollen:** `Low=1 dot, Moderate=2, High=3`.
- **Condition display names:** `Scattered clouds, chance of rain` → "Cloudy, chance of rain"; others unchanged.

---

## 6. ViewModel & state (viewmodel/)

One `AppViewModel : AndroidViewModel` exposing `StateFlow<AppUiState>`:

```kotlin
data class HomeUiState(
    val location: Location, val now: LocalDateTime, val current: WeatherRow,
    val todayHigh: Double, val todayLow: Double,
    val hourly: List<WeatherRow>,            // 24 rows from now
    val daily: List<DailySummary>,           // 7 days
    val advisories: List<Advisory>,          // for NOW, deduped + sorted
    val headline: String, val rainSentence: String,
    val cards: List<CardSlot>                // enabled cards, ordered, each with its resolved payload
)
sealed interface AppUiState { object Loading; data class Error(val msg: String); data class Ready(val home: HomeUiState, ...) }
```

- Inputs combined with `combine(prefs, currentDeviceHour, selectedLocation, latestForecastState)`; recompute on `Dispatchers.Default` with `flowOn`.
- Persist every preference change immediately through `UserPrefs`.
- Notifications: after provider data refreshes or the current device hour changes, compute new `ALERT` advisories; notify only from in-coverage provider-backed data and only once per `(locationId, forecast date, ruleId)` (Section 13).
- Screen navigation is a plain `mutableStateListOf<Screen>()` in `AppRoot`, with `BackHandler`.

---

## 7. Performance & threading notes

- **Threads:** provider requests and persistent cache reads/writes on `Dispatchers.IO`; engine + insights on `Dispatchers.Default`; only the final `HomeUiState` reaches the main thread.
- **Cache:** keep provider-backed rows by locality/endpoint, keyed by timestamp; never merge fields from distinct timestamps/models into a plausible-looking complete row.
- **Recompute only on change:** derive `HomeUiState` from preferences, current device hour, locality, and latest provider/cache state; use `distinctUntilChanged()` so unrelated preference writes don't recompute.
- **Compose:** give every `LazyColumn` item a stable `key`; keep heavy lists (`hourly`, `daily`) immutable and remembered on `(location, nowIndex)`; draw gauges/curves/route map on `Canvas` (no bitmaps).
- **Budget:** location switch and cached forecast rendering must finish in < 300 ms on a mid-range phone; live requests run asynchronously and never block navigation.

---

## 8. Alerts screen (P1)

Title "Alerts". Two sections: **Now** (all advisories, not capped) and **Upcoming (next 12 hours)**: run the engine for each of the next 12 rows and list the first hour each rule trips as "Around 5 PM · Rain likely — ...". Same banner component as Home.

---

## 9. Trip Planner

### 9.1 User-configured route

The user selects actual supported localities as stops and supplies stop order, planned departure, and estimated travel time between stops. Persist their route as user data, not as a built-in example itinerary. Arbitrary trail/road geometry, automatic directions, distance, and traffic estimates require an explicitly approved real route provider and are out of scope until then. Every stop needs verified coordinates and real provider coverage before weather advice can be shown.

### 9.2 ETA → weather matching

```kotlin
fun LocalDateTime.roundToNearestHour(): LocalDateTime {          // half-up
    val base = truncatedTo(ChronoUnit.HOURS)
    return if (minute >= 30) base.plusHours(1) else base
}
// For each waypoint: eta = departure.plusMinutes(w.minutesFromStart); row = series(w.locationId)[eta.roundToNearestHour()]
```
If the ETA falls outside returned provider coverage or lands on a missing timestamp, return **Forecast unavailable** for that waypoint. Never clamp or interpolate weather values.

### 9.3 Per-waypoint advisories

Evaluate the engine **at each waypoint's own series and ETA row** with:
- personas = `[commuter, health_conscious] ∪ user's active personas` (de-duplicated),
- `fieldFilter = {rain_chance_percent, aqi_value, visibility_km, fog_flag, heat_alert}` (only trip-relevant fields; keeps UV/pollen noise out),
- lookahead rules work naturally because the engine takes the ETA index.

Waypoint severity = highest advisory severity there (none → OK).

### 9.4 Route verdict & scoring

`routeScore = Σ over waypoints Σ advisories (ALERT=3, CAUTION=1)`.
Verdict pill: any ALERT → **"Alert"** (red); else any CAUTION → **"Caution"** (amber); else **"Clear"** (green).
Summary sentence (template): list the worst provider-backed waypoints — "{condition} at {place} ({time}, {provider value}) and {condition} at {place} ({provider value})."

### 9.5 Better departure time (P1)

Evaluate `routeScore` only for departures in the provider's supported forecast horizon. Pick the minimum (ties → earliest). Show only if the alternative has complete coverage and `bestScore < currentScore`: "Leave at {time} instead — {provider-backed summary}" with a button **Use this time**.

### 9.6 Screen spec

1. Top bar: back arrow, title "Trip planner".
2. **Route header** (glass card): user-selected route name, stop count, and distance/duration only if supplied by the user or a supported route source.
3. **Departure selector:** chips `Now · +30 min · +1 h` and a stepper for departure time. Disable times that cannot be evaluated within provider coverage.
4. **Verdict card:** pill + summary sentence + (P1) "better time" suggestion.
5. **Route map card (schematic, drawn on Canvas, no map SDK):** project only the user's actual route stops with verified coordinates; do not add placeholder routes or unrelated locations.
6. **Timeline list:** one row per waypoint: severity dot + vertical connector; place; ETA time; weather glyph + temperature; rain chip (blue) with %; AQI chip (AQI color); visibility if ≤ 5 km; the advisory lines (persona-tagged) under it.
7. **Route exposure strip:** summarize only provider-backed values with per-stop coverage/source metadata; omit metrics that are missing instead of substituting defaults.

Acceptance: for a user-selected supported route and an in-coverage departure, every displayed stop uses the provider response at that stop's ETA; missing/out-of-coverage inputs make the affected result unavailable and cannot yield a "Clear" verdict. Better-departure suggestions are calculated only over times with complete provider-backed route coverage.

---

## PART C — UI/UX

## 10. Design principles (inspired by Apple Weather + Google Weather)

**Take from Apple Weather:** the full-bleed gradient that reflects provider-backed sky conditions, the huge light-weight temperature, the hero that collapses into a compact header while scrolling, frosted modular cards with tiny ALL-CAPS headers, the horizontal hourly strip, the provider-horizon forecast list with range bars, and a sentence-style rain summary generated from returned forecast values.

**Take from Google Weather:** Material-3 clarity and generous spacing, precipitation shown as blue droplet % under each hour, a chart with metric tabs (Temperature · Rain · Wind · AQI), pill-shaped chips, and plain-language summary lines.

**Our own layer on top:** persona chips at the top, severity banners that tell you what to do, and an "advice line" at the bottom of every card.

Principles: (1) **answer first, data second** — every card leads with a decision or a plain-language line; (2) **one glance = one insight**; (3) calm visuals — no ad clutter, generous spacing; (4) color = meaning (severity, AQI, UV); (5) motion is subtle and informative.

## 11. Design system

### 11.1 Layout tokens

| Token | Value |
|---|---|
| Base grid | 4 dp |
| Screen horizontal padding | 16 dp |
| Card corner radius | 24 dp (chips 999 dp pill, banners 20 dp) |
| Card inner padding | 16 dp |
| Gap between cards | 12 dp |
| Min touch target | 48 dp |
| Edge-to-edge | `enableEdgeToEdge()`; gradient draws behind system bars; status-bar icons **white**; use `statusBarsPadding()` on the top bar and `navigationBarsPadding()` + 24 dp on the last list item |

### 11.2 Dynamic background gradient (vertical, top→bottom, 3 stops)

Choose by the **first matching** rule (`Gradients.kt`, input = current `WeatherRow`):

| Priority | Condition | Stops |
|---|---|---|
| 1 | `condition == "Thunderstorm"` | `#1F2735 → #364155 → #56607A` |
| 2 | `condition == "Showers"` | `#3A4A63 → #56677F → #7D8CA0` |
| 3 | `fog_flag == 1` | `#6F7B88 → #8C97A3 → #A9B3BD` |
| 4 | `aqi_value >= 150` (smog) | `#6E645C → #8F857C → #B0A69C` |
| 5 | night (`hour >= 19 || hour <= 4`) | `#0B1330 → #1B2A5C → #2F3F86` |
| 6 | dawn/dusk (`hour in 5..6 || hour in 17..18`) | `#3B4A8C → #B4658E → #F2A65A` |
| 7 | `condition` starts with "Scattered" | `#5D7288 → #7F92A6 → #A7B4C2` |
| 8 | `condition == "Partly cloudy"` | `#4F86C6 → #7FA6CC → #B8CBDD` |
| 9 | otherwise (sunny day) | `#2E7BE6 → #62AEF2 → #A8D4FA` |

Animate a stop with `animateColorAsState(tween(900))` when a successful provider refresh changes its actual condition.

### 11.3 Glass cards (Compose has no true backdrop blur — emulate it)

- Fill: `Color.Black.copy(alpha = 0.22f)` over the gradient (dark glass — keeps white text readable on every gradient).
- Border: `1.dp`, `Color.White.copy(alpha = 0.14f)`.
- Optional top highlight: a `0.5.dp` white 10% line inside the top edge.
- No drop shadow. Content color is always white with alpha tiers: primary 100%, secondary 70%, tertiary 50%.

### 11.4 Semantic colors

| Meaning | Color |
|---|---|
| Caution | `#FFB020` (banner fill amber @ 22% alpha, 1 dp border amber @ 60%) |
| Alert | `#FF5A52` (banner fill red @ 24% alpha, border red @ 65%) |
| OK / good | `#34C759` |
| Rain / precipitation blue | `#64B5F6` |
| AQI bands | 0–50 `#34C759` · 51–100 `#FFD60A` · 101–150 `#FF9F0A` · 151–200 `#FF453A` · 201+ `#BF5AF2` |
| UV bands | Low `#34C759` · Moderate `#FFD60A` · High `#FF9F0A` · Very high `#FF453A` · Extreme `#BF5AF2` |
| Temperature range bar | `#64D2FF → #FFD60A → #FF9F0A → #FF453A` |

Severity must never rely on color alone: always pair with an icon (`Info` for caution, `Warning` for alert) **and** the words "Caution" / "Alert".

### 11.5 Typography (default system font; weights via `FontWeight`)

| Style | Size / weight | Use |
|---|---|---|
| heroTemp | 96 sp / Light | Hero temperature |
| heroLocation | 28 sp / Medium | Location name |
| heroCondition | 20 sp / Medium | Condition text, H/L |
| cardValue | 36 sp / Light | Big number in cards |
| title | 20 sp / SemiBold | Screen titles |
| body | 15 sp / Regular | Advice lines |
| label | 13 sp / Medium | Chips, secondary values |
| cardHeader | 12 sp / SemiBold, ALL CAPS, letterSpacing 0.8 sp, 60% alpha | Card headers |
| micro | 11 sp / Regular | Footnotes |

Use `sp` everywhere and never fix the height of a text container (must survive 130% system font scale).

### 11.6 Icons & weather glyphs

- **Chrome/card-header icons:** Material Icons Extended, 20 dp, white 70%: aqi `Air`, pollen `LocalFlorist`, uv `WbSunny`, humidity `WaterDrop`, run `DirectionsRun`, heat `Thermostat`, marine `Waves`, travel `Flight`, family `School`, agriculture `Agriculture`, commute `DirectionsCar`, event `Event`, clothes `Checkroom`, trip `Route` (fallback `Directions`), outlook `CalendarMonth`, severity `Info`/`Warning`, location `Place`, edit `Tune`. If an icon is missing in the installed icons version, substitute the nearest one and note it in DECISIONS.md.
- **Persona tile icons:** health `HealthAndSafety`, fitness `DirectionsRun`, beach `BeachAccess`, traveler `Flight`, family `FamilyRestroom`, agriculture `Agriculture`, commuter `Commute`, event `Celebration`.
- **Weather glyphs (P0 = emoji, 28–40 sp):**

| Condition / flag | Glyph |
|---|---|
| Sunny | ☀️ |
| Clear (night) | 🌙 |
| Partly cloudy | ⛅ |
| Scattered clouds, chance of rain (day) | 🌦️ |
| Scattered clouds, chance of rain (night) | ☁️ |
| Showers | 🌧️ |
| Thunderstorm | ⛈️ |
| `fog_flag == 1` (overrides) | 🌫️ |

Wrap this in a single `WeatherGlyph(row)` composable so P2 can swap in custom vector art without touching callers.

### 11.7 Motion

- Gauge/marker fills: `animateFloatAsState(tween(700, easing = FastOutSlowInEasing))` from 0 on first show.
- Cards enter with a staggered fade + 12 dp slide-up (delay `index * 40 ms`, cap at 6). Use `Modifier.animateItem()` (or `animateItemPlacement()` on older foundation) for reorder.
- Banners: `AnimatedVisibility(fadeIn + expandVertically)`.
- Severity dot on an ALERT banner pulses (scale 1.0 ↔ 1.25, 1.2 s, infinite).
- Hero collapse (P1): see 12.3.
- Respect "Remove animations" (`Settings.Global.ANIMATOR_DURATION_SCALE == 0` → skip staggering).

### 11.8 Accessibility

Content descriptions for every icon-only button and gauge ("Air quality 178, Unhealthy"); TalkBack reads a banner as "Alert, Rain, <message>, for Parent and Family"; contrast ≥ 4.5:1 for body text (dark-glass cards guarantee this); tap targets ≥ 48 dp; state never conveyed by color alone; support font scale up to 130%.

---

## 12. Screens & components

### 12.1 Navigation

`Screen` = `Onboarding | Home | TripPlanner | Alerts | Settings`. Sheets (`ModalBottomSheet`): `LocationSheet`, `PersonaSheet`, `EditCardsSheet`. First launch → Onboarding; afterwards → Home.

### 12.2 Onboarding (4 steps, `HorizontalPager` disabled for swipe — use Next/Back buttons; progress dots)

1. **Welcome** — dark night gradient, ☁️ glyph, title "Weather that tells you what to do", one-line sub "Pick who you are — we'll show what matters." Button **Get started**. (P2: language chooser English / தமிழ் here.)
2. **Who are you?** — title "Pick all that apply". 2-column grid of 8 **PersonaTile**s (icon 32 dp, label, one-line description, check badge when selected; selected = brighter glass + 1.5 dp white border). **Continue** enabled when ≥ 1 selected.
   Descriptions: Health-conscious "AQI, pollen, UV"; Outdoor fitness "Best run time, heat, wind"; Beachgoer & surfer "Waves, tides, sea temp"; Traveler "Fog, rain, packing tips"; Parent & family "School run, clothes drying"; Agriculture & gardener "Soil moisture, rain, irrigation"; Commuter "Rain, fog, route weather"; Event planner "Best day, comfort, rain risk".
3. **Where are you?** — searchable list of the 15 localities (name, "14 km · Residential"); selecting highlights. Default `nungambakkam`. Note under the list: "You can switch any time — try a coastal one like Adyar or Mahabalipuram."
4. **Alerts** — explain "We'll alert you when conditions matter to you". Button **Allow notifications** (runtime permission on API 33+, via `rememberLauncherForActivityResult(RequestPermission())`) and **Maybe later**. Finish → 1 s "Preparing your dashboard…" shimmer → Home. Persist everything.

### 12.3 Home screen — section order (single `LazyColumn`, background gradient behind everything)

```
┌───────────────────────────────────────┐
│ ▾ {locality}      {local time · source status} ⚙ │  1 top bar
│ [🏃 Fitness] [👪 Family] [🚗 Commuter]  ✎ │  2 persona chips (tap → PersonaSheet)
│                                       │
│               {locality}               │  3 HERO
│              {temp}°                   │    provider value
│        {provider condition}            │
│       H:{high}°   L:{low}°              │
│    {provider-backed rain summary}      │  4 rain sentence pill
│                                       │
│ ┌ {severity} · {field} · {persona} ─┐ │  5 provider-backed advisories
│ │ {rule message from real inputs}   │ │
│ │                         Listen    │ │
│ └────────────────────────────────────┘ │
│ ┌ NEXT 24 HOURS ────────────────────┐ │  6 hourly strip card
│ │ Now  10AM 11AM 12PM 1PM …          │ │
│ │ {provider conditions}              │ │
│ │ {provider temperatures}             │ │
│ │ {provider rain probabilities}        │ │
│ └────────────────────────────────────┘ │
│ FOR YOU                          Edit │  7 persona card grid
│ ┌──────────────────────────────────┐  │
│ │ AIR QUALITY   {AQI} {category}   │  │  (wide)
│ │ {provider pollutant values}      │  │
│ └──────────────────────────────────┘  │
│ ┌─────────────┐ ┌──────────────────┐  │
│ │ UV {value}  │ │ HUMIDITY {value}│  │  (two small)
│ └─────────────┘ └──────────────────┘  │
│ …more cards…                          │
│ 7-DAY FORECAST                        │  8 daily list card
│ Today {condition} {provider range}  │
│ {day} {condition} {provider range}  │
│                                       │
│ Provider/model · updated time · forecast coverage │  9 footer (micro text)
└───────────────────────────────────────┘
```

**Hero details.** Location name (heroLocation), temperature (heroTemp) with the `°` at 60% size, condition display name + glyph, `H:` / `L:` from today's rows, "Feels like {feels_like}°" in label style. Under it the **rain sentence** in a pill (blue-tinted glass). Under that a one-line **"Today for you"** headline (Section 5.7) with a 🔊 button (P2).
**Collapse (P1):** when the list scrolls past the hero, fade the hero out (`graphicsLayer { alpha = 1 - progress }`) and fade in a compact sticky bar showing the selected locality and actual provider-backed conditions.

**Advisory banners.** Height wraps content. Left: severity icon (pulsing dot for ALERT); title row `ALERT · Rain` (12 sp caps) + persona tag chip; body = `message` (15 sp). Max 3 visible; if more → a text button "See all N alerts" → Alerts screen. Swipe-to-dismiss hides a banner for the current provider timestamp only (state in ViewModel, not persisted). Field-to-title mapping is applied only to fields returned by the provider.

**Hourly strip.** `LazyRow` of provider-returned hourly items; item width 64 dp; contents top→bottom: local time label, glyph, temperature, and a rain-probability line when available. Current item has a brighter glass pill behind it. Render sunrise/sunset only when returned by the provider.

**Forecast list.** Render only provider-returned forecast days (up to the provider horizon). Each row: local day label, glyph, rain probability and min/max temperature only when available; AQI indicator only when returned. Never extend the horizon with copied/generated values.

**Card grid.** Cards are rendered from the ordered, enabled list. **Do not nest a lazy grid inside the LazyColumn** (it crashes/measures wrong); instead chunk the cards into rows of `Row { … Modifier.weight(1f) }` with `Modifier.height(IntrinsicSize.Min)` and `fillMaxHeight()` children, as `items` of the LazyColumn. Layout algorithm:

```
rows = []; pending = null
for card in orderedEnabledCards:
    if card.isWide: (flush pending as a full-width single) ; rows += [card]
    else: if pending == null: pending = card else: rows += [pending, card]; pending = null
flush pending as a full-width single
```
- **Wide (span 2):** `aqi_card, running_window_card, marine_card, travel_alerts_card, family_commute_card, agriculture_card, event_planning_card, cloth_drying_card, trip_planner_card, daily_outlook_card`
- **Small (span 1):** `pollen_card, uv_card, humidity_card, heat_card, commute_traffic_card`

### 12.4 Card specifications (each = GlassCard, header row: 20 dp icon + CARD HEADER caps)

Every card ends with an **advice line** (body, 70% alpha): the top advisory for that card's fields if any (with severity color icon), otherwise a default sentence.

| Card id | Content |
|---|---|
| `aqi_card` (wide) | Left: provider-returned AQI number + provider category chip. Right: pollutant names/values only when returned. Below: scale-labeled AQI bar using the provider's stated scale (animated); if AQI is unavailable, show that explicitly. |
| `pollen_card` (small) | Display provider-returned pollen categories only; if unsupported or absent, show unavailable rather than a substituted category. |
| `uv_card` (small) | UV number + label chip (UV colors), thin gradient bar with marker, footer "Sun protection {first hour with uv≥3}–{last}" for today. |
| `humidity_card` (small) | Provider-returned humidity and dew point; show derived comfort wording only when required inputs are present. |
| `running_window_card` (wide) | Header "BEST TIME TO RUN". Compute from complete provider-backed rows within coverage. Show input summary and covered time range; if inputs are incomplete, say unavailable rather than suggesting a time. |
| `heat_card` (small) | Provider-returned feels-like value; show a heat advisory only when its required provider inputs are present and the configured rule trips. |
| `marine_card` (wide, coastal) | Provider-returned wave height/status, period, direction, water temperature, or tide values only when available from the approved Marine endpoint. Never derive navigation advice from modeled marine data. **Inland/unsupported:** explicit unavailable state (12.9). |
| `travel_alerts_card` (wide) | Visibility km, fog chip if flagged, "Rain chance next 6 h: max X%". Disruption risk = High if any ALERT, Medium if any CAUTION, else Low. Packing chips (5.6). Button "Plan a trip →" opens Trip Planner. |
| `family_commute_card` (wide) | User-configurable commute windows populated from provider-returned rain/temperature values. Omit a window if any required inputs are unavailable. |
| `agriculture_card` (wide) | Show soil moisture only when the approved provider returns it. Rainfall guidance uses actual in-coverage forecast values; without both inputs show unavailable, not a default irrigation recommendation. |
| `commute_traffic_card` (small) | "Roads": Clear / Wet / Poor visibility; "Rain in next 2 h: max X%"; small link "Plan a trip →". |
| `event_planning_card` (wide) | Comfort, rain, UV, and AQI only when provider inputs exist; best-day comparisons are limited to days returned by the provider. |
| `cloth_drying_card` (wide) | Header "CLOTHES DRYING". Compute a window only from complete actual provider forecasts; otherwise show unavailable. Rain alarm uses provider-backed alert data only. |
| `trip_planner_card` (wide) | User's configured route, provider-backed coverage and verdict; tap → Trip Planner. |
| `daily_outlook_card` (wide) | Event-oriented forecast comparison only within provider-returned days, with unavailable inputs clearly identified. |

### 12.5 Location sheet

`ModalBottomSheet`, search field, verified locality list: name, optional independently verified locality metadata, a coastal `🌊` marker where appropriate, and provider-backed AQI only when available. Selecting closes the sheet, persists, and starts a real provider refresh; cached values are labeled with their age.

### 12.6 Persona sheet

Same tile grid as onboarding, multi-select, **Done** button; applies the rules in 4.2.

### 12.7 Top bar time and source indicator

Always visible: locality and current device time, plus a concise data-state label (Fresh, Cached, Stale, or Unavailable). The time uses `Asia/Kolkata`; there is no time-scrubbing control.

### 12.8 Edit cards sheet

Opened by "Edit" next to FOR YOU. A list of **all** 15 card definitions: leading up/down arrow buttons (reorder), label, switch (enabled). Cards that need coastal data show a "Coastal only" caption when the current location is inland. Footer buttons: **Reset to persona defaults**, **Done**. Sets `cards_customized = true` on any change.

### 12.9 Empty / loading / error states

- **Loading:** shimmer glass placeholders while the first provider request is in progress.
- **Marine inland:** icon `Waves` at 40% alpha + "Sea conditions aren't available for inland areas. Try Adyar, Royapuram, Ennore or Mahabalipuram." + button "Switch location".
- **Provider/cache/configuration error:** keep navigation available, show a concise error and retry action, and log the exception with tag `PersonaWeather`; never substitute invented weather.
- **No advisories:** show "No active advice" only when relevant provider data is complete; otherwise show "Forecast data unavailable". Never imply safe conditions from missing values.

### 12.10 Settings

Rows: Forecast provider and data status; Your personas → PersonaSheet; Location → LocationSheet; Notifications (switch); Clothes rain alarm (switch); Language; About credits Open-Meteo, CAMS, and the marine model provider, clarifies that forecast values are model outputs (not station observations), and lists unavailable fields. Reset app clears preferences and returns to onboarding.

### 12.11 Home-screen widgets (P1)

Use AndroidX Glance (Glance widgets render through Android's app-widget system; they are not regular in-app Compose screens). Provide responsive sizes and three selectable widget focuses:

- **Current conditions:** locality, provider and data freshness, current forecast temperature and condition, AQI only when available, plus the highest-severity in-coverage advisory (or "No active advice").
- **Hourly outlook:** current conditions plus the next 3 hourly conditions, temperatures, and rain chance; on compact sizes, show a concise prompt to expand.
- **Persona advice:** current conditions plus a concise advice row for each selected persona, including a calm "No active advice" state. Show additional selected personas as a count when the widget is too small to fit them all.
- Widget backgrounds use weather-aware gradients for clear, rain/storm, haze/smog, and night conditions; layout and content density respond to the actual launcher-assigned size.

Widget data contract and behavior:
- `WidgetSnapshotUseCase` resolves the configured locality's provider-backed current row, short hourly outlook, and advisories via the shared provider/cache contract and `AdvisoryEngine`; UI renders only this immutable snapshot. Widgets must not issue independent endpoint requests or show fixture weather.
- Each widget instance's configuration activity selects its focus, locality, and one or more personas. Defaults come from the app's selected locality and personas; saved widget choices remain independent when app settings later change. Available personas come from `persona_rules.json`, and advisory thresholds still come exclusively from the shared rules engine.
- Tapping the widget opens Home for its locality; tapping its advisory opens Alerts. Include an explicit refresh action with a content description.
- Refresh periodically through WorkManager (30-minute interval) and immediately after relevant user-driven changes such as persona changes, widget configuration, or explicit refresh. Respect Android background-work limits and show cached data with its update time when offline.
- Handle loading, unavailable forecast, no-advisory, inland marine data, and removed/invalid widget configuration without crashing. Do not claim forecasts are observations. Keep text readable at supported font scales and expose meaningful Glance semantics.

Implementation stays within the existing no-Room/no-Hilt/no-Navigation-library constraints. No new network permission or widget-only weather source is introduced.

---

## 13. Notifications (notify/NotificationHelper.kt)

- Channels (create at app start, API 26+): `advisories` (IMPORTANCE_HIGH, default sound) and `clothes_alarm` (IMPORTANCE_HIGH, sound = `RingtoneManager.getDefaultUri(TYPE_ALARM)` with `AudioAttributes.USAGE_ALARM`). A channel's sound can't be changed after creation — note in DECISIONS.md.
- Permission: `POST_NOTIFICATIONS` (API 33+) requested in onboarding step 4; if denied, the in-app banners still work.
- Trigger: after a successful provider refresh or when the current device hour changes, for each new `ALERT` advisory whose `ruleId` has not been posted for `(locationId, forecast date)` → post once. Do not send a new weather alert from stale/out-of-coverage data. `pf_cloth_alert` posts on `clothes_alarm` (only if `cloth_alarm_enabled`), everything else on `advisories`.
- Content: title = `"{Field title} alert · {Location}"`, text = `message`, tap → opens the app on Home.
- Do not provide a weather test-alert action that fabricates a weather event. Validate notification delivery through Android's notification permission/settings flow and provider-backed advisory tests.

## 14. Localization & text-to-speech (P2 — design now, build later)

- Do **not** use Android resource switching or AppCompat per-app locales (extra dependencies). Use an in-app `Lang { EN, TA }` and a `Strings` object with two maps and a `t(key)` lookup exposed through a `CompositionLocal`; switching is instant, no restart.
- Advisory text: use `message_ta` from JSON when `language == ta`, fall back to `message`.
- TTS: `TextToSpeech(context, listener)`; language `Locale("ta", "IN")` for Tamil. After init, check `isLanguageAvailable(locale)`; if `LANG_MISSING_DATA`/`LANG_NOT_SUPPORTED`, show a snackbar with an action that launches `TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA`. A 🔊 **Listen** button appears on the "Today for you" headline and on each banner.
- Rural/low-literacy design: large tap targets, icon-first cards, read-aloud advisories. (Speech-to-text via Sarvam AI is **parked**: it needs a network API.)

## 15. Offline, robustness

- **Offline must be a normal app state, not a fatal error.** On loss of connectivity or request failure, retain the current screen and last successful provider data, show its source and "Updated …" time, and allow navigation, persona/card changes, saved locations, and Trip Planner controls to continue. Do not clear good data or replace it with an error screen just because a refresh failed.
- Treat Android connectivity state as a hint, not proof that the internet works. Network calls need bounded connect/read timeouts and explicit failure results. A failed or partial endpoint must not prevent other available endpoints from rendering; show a field as unavailable if neither a usable cache nor a supported fallback exists.
- Cache provider responses privately by locality and endpoint, with retrieval time, provider/source, and forecast start/end. Read a cached response when offline and label it **cached forecast**; do not label a forecast as an observation. If an endpoint has no cache, that field is **Unavailable offline**. Never substitute another provider's data or invent a fallback value.
- If the device time is outside a cached forecast's coverage, do not render its old row as current conditions or derive a current route verdict from it. Keep the snapshot accessible as last-known data, label it stale, and state that a current forecast is unavailable.
- Retain cached data across process death and app updates. A refresh writes a complete endpoint result atomically so an interrupted write cannot corrupt the last good cache. Surface parse/storage errors in app state or a visible notice; do not silently turn them into a successful refresh.
- **Prepare an offline trek pack (Phase 11, P1):** from Trip Planner, let the user choose a saved route/stops and activity date/time window, then tap **Prepare offline pack** while online. Preview the exact locations, date coverage, and data sources before download; show progress, completion time, missing stops/endpoints, and actionable retry errors. Do not report success for a partial pack without identifying what is missing.
- A completed pack is an immutable, versioned snapshot saved in app-private storage and survives app restart. Include the route/stop order and ETAs, provider-returned hourly forecast values needed by the Trip Planner and advisory engine, derived advisories and route verdict, forecast coverage, retrieval time/source per endpoint, and daylight values only if returned by the provider. Include a concise general trek-preparation checklist (not weather claims) plus optional user-entered emergency contact/notes. Keep the payload small; do not add a third-party map SDK.
- In airplane mode, a matching pack opens directly from Trip Planner and supports per-stop timelines, advisories, and the route verdict without any network call. It prominently shows **Downloaded forecast**, its download time, provider/model, and valid forecast window; absent coastal/endpoint values are unavailable. A pack outside its downloaded time coverage cannot produce a forecast or "safe" verdict. Do not imply that GPS tracking, live rerouting, map tiles, emergency dispatch, or changing weather is available offline.
- Allow users to refresh or delete packs before departure. If connectivity drops during preparation, keep the last complete pack intact and report that the new download did not complete. When online, offer a refresh if the trip falls outside the pack's coverage; never refresh automatically in a way that removes the offline copy.
- Use supported localities or user-provided route stops with verified coordinates. Arbitrary trail geometry and map downloads are out of scope until a route/waypoint source is specified; do not invent weather for unsupported coordinates.
- Never crash on NULL columns (marine) — `fieldValue` returns null and rules skip.
- Out-of-coverage timestamps → unavailable; never clamp to another time and imply it is current.
- Guard divide-by-zero in gauges and range bars (single-value series).
- Validate provider JSON parsing and cache access; propagate explicit unavailable/error states to the UI.

---

## PART D — LIVE DATA, PHASES, TESTS

## 16. Real-data operation and validation

- The app has one weather mode: provider-backed forecasts. Do not include a Demo mode, simulated clock, autoplay, fixed-date weather scenario, sample weather alert, canned response, or fixture weather in production or debug UI.
- Home, widgets, Trip Planner, and calendar advice all consume the same validated Open-Meteo response/cache contracts and current device time. Switching locality causes a real provider request or displays valid cached provider data; it never changes to preselected weather values.
- Validate visible conditions only when the provider returns the relevant values during the requested forecast coverage. If it does not, show the actual values/advisories returned or a clear unavailable state. Never force a condition by overriding time, location, weather values, or persona rules.
- Airplane-mode acceptance uses actual responses downloaded before disconnecting. It proves cache/pack behavior, not fabricated weather behavior.
- Keep provider attribution and current forecast horizon visible. Never describe a modeled forecast as an observation or present unavailable pollen, tide, soil, or other unsupported values as measured facts.

## 17. Build phases (one at a time; each ends with a compiling app)

**Phase 0 — Scaffold & configuration.** Gradle project, package layout (Section 2), persona/rule configuration, manifest with INTERNET and POST_NOTIFICATIONS, theme stub. Do not bundle weather datasets or sample responses. *Accept:* `assembleDebug` succeeds; app launches into onboarding/loading/unavailable states.

**Phase 1 — Provider and cache layer.** Open-Meteo Weather/Air Quality/Marine clients, models, response validation, persistent cache, `RulesLoader`, `UserPrefs`, `AppContainer`. *Accept:* provider fields map with correct units/timestamps; missing values remain null; endpoint errors retain last good cache and report explicit status. *You verify:* request a verified locality online, inspect source/model and coverage, then confirm cached values persist after process restart.

**Phase 2 — Engine + insights.** `AdvisoryEngine`, `Insights`, `Labels`, `Tuning`. *Accept:* rules skip missing fields and results remain tied to the input provider coverage; no weather fixtures are packaged or rendered. *You verify:* run provider-payload contract tests and core logic tests.

**Phase 3 — Design system + data-backed components.** Theme, gradients, GlassCard, Gauge, Sparkline, WeatherGlyph, SeverityBanner, and data-supported card composables. Do not include a gallery or preview screen that presents weather values. *Accept:* components render only actual provider values or explicit unavailable states.

**Phase 4 — Onboarding + persistence.** 4-step onboarding, prefs, default cards. *Accept:* picks survive app restart; ≥1 persona required. *You verify:* pick Fitness + Family → kill app → reopen → lands on Home with those chips.

**Phase 5 — Home.** ViewModel, hero, rain sentence, headline, banners, hourly strip, provider-horizon forecast list, card grid, location sheet, persona sheet, edit-cards sheet, current-time/source indicator. *Accept:* only provider-backed values are shown; unavailable and stale states are explicit; location switch remains responsive while requests run.

**Phase 6 — Trip Planner.** User-configured route, ETA matching, per-waypoint provider coverage/advisories, verdict, timeline, schematic map, departure selector, and best-time suggestion. *Accept:* Section 9.6 coverage rules; no route verdict when any required stop/time lacks provider data.

**Phase 7 — Notifications.** Channels, permission flow, alarm sound for clothes. *Accept:* notifications are emitted only from actual in-coverage provider-backed advisories, deduplicated as specified; no test weather alert exists in the app.

**Phase 8 — Polish.** Animations (11.7), collapsing hero (P1), 24h chart with tabs (P1), Alerts screen (P1), accessibility pass, empty/error states, app icon (adaptive, sky-gradient + cloud), splash. *Accept:* usable at 130% font scale; TalkBack labels present; no jank on a mid-range phone.

**Phase 9 (P2) — Tamil + TTS + custom glyphs.**

**Phase 10 (P1) — Home-screen widgets.** Add the Glance widget and receiver, per-instance focus/locality/persona configuration, `WidgetSnapshotUseCase`, weather-aware gradients, and bounded WorkManager refresh. *Accept:* all three widget focuses show provider-backed conditions and persona-specific advice; each configuration survives process/app restart; widget sizes adapt; cache/offline states are explicit. *You verify:* add each focus at compact and expanded sizes; create widget instances for different localities; test actual values, unavailable fields, stale data, and airplane mode with a previously downloaded cache.

**Phase 11 (P1) — Offline resilience + trek packs.** Harden provider refresh/cache failure handling; expose source/freshness/coverage consistently; add a Trip Planner flow to prepare, inspect, open, refresh, and delete a versioned offline trek pack. Persist packs atomically and keep the last complete pack if preparation fails. *Accept:* Section 15 behavior is covered by data/repository and UI tests; a prepared route/date opens after process death and in airplane mode without network calls; each stop shows source/coverage; incomplete/out-of-range packs never produce a route verdict. *You verify:* prepare a pack online, force-stop/relaunch, enable airplane mode, inspect each stop and advisory, try a trip outside its coverage, simulate failed/partial download, and confirm the prior pack remains usable.

### Implementation status snapshot

- [x] **No-synthetic migration (source/assets/tests):** removed the bundled generated weather database/spreadsheet, simulated clock, scenario/autoplay/test-alert and gallery preview UI, weather fixture sources and fixed-weather assertions, demo/live toggle, and production fallbacks that could present non-provider weather.
- [ ] Re-run `./gradlew test` and `./gradlew assembleDebug` after the migration; validate actual online provider responses, exact cached-response behavior offline, and explicit unavailable states.
- [ ] Offline trek packs remain unimplemented until they satisfy Section 15.
- [ ] Device-only checks remain: physical TalkBack pass, 130% font-scale validation, rotation persistence, notification behavior, and Tamil voice availability on a connected handset.

## 17A. Three-person parallel implementation plan

The phases above describe product order. The three contributors work in parallel inside that order, with one integrator responsible for keeping the app buildable.

### Team ownership

| Person | Owns | Primary files | Depends on |
|---|---|---|---|
| A — Integrator / App shell | Project scaffold, shared contracts, `AppContainer`, `AppViewModel`, `UiState`, app root, navigation, integration builds | `PersonaWeatherApp.kt`, `MainActivity.kt`, `data/AppContainer.kt`, `viewmodel/`, `ui/AppRoot.kt`, Gradle and manifest | Decisions from the plan; interfaces from B and C |
| B — Data + pure Kotlin engine | Provider/cache loading, preferences, models, rules, advisories, insights, route calculations, unit tests | `core/`, `data/`, `src/test/` | Provider contracts and Appendices A–C; no UI dependency |
| C — Compose experience | Theme, components, onboarding, home, cards, trip/settings screens, notifications UI states, unavailable/loading/error gallery | `ui/`, `notify/`, UI tests | Stable models and `UiState`; no weather fixtures |

### Shared contract freeze

Before parallel feature work begins, Person A creates the package tree and a small compileable contract containing `WeatherRow`, `Location`, `Persona`, `Rule`, `CardSpec`, `Advisory`, `WeatherDataSource`, `Screen`, and the initial `UiState`. Persons B and C may extend these contracts through pull requests, but must not silently rename or change fields used by another person.

If a contract must change:

1. Open a short issue or shared note describing the old and new shape.
2. Update the producer and consumer in the same pull request when practical.
3. Run the narrowest affected test or build before asking for review.
4. Tell the other two people exactly what changed and whether they need to rebase.

### Parallel work by phase

| Phase | Person A | Person B | Person C |
|---|---|---|---|
| 0 | Scaffold, manifest, build, startup screen | Validate provider contract and locality metadata | Create theme stub and empty/loading/unavailable gallery |
| 1 | Wire `AppContainer` and debug state | Implement provider/cache layer, models, rules, prefs, tests | Build reusable components against unavailable-state contracts |
| 2 | Expose engine results through `UiState` | Implement and test engine, insights, labels, tuning | Render advisory/banner/card states only from provider-backed state |
| 3 | Integrate gallery into app root | Fix provider-contract gaps found by gallery | Implement design system and data-supported card composables |
| 4 | Connect onboarding state and persistence | Provide default-card and preference APIs | Implement onboarding and edit-card flows |
| 5 | Implement ViewModel and screen integration | Support performance/data bugs found by the Judge test | Implement Home, sheets, hero, hourly strip, daily list, and card grid |
| 6 | Integrate route state and navigation | Implement and test route/ETA calculations | Implement Trip Planner visualizations and controls |
| 7 | Integrate provider-backed notification lifecycle | Validate alert inputs and edge cases | Implement notification UI and settings |
| 8–9 | Release integration and acceptance matrix | Regression tests and accessibility data checks | Polish, accessibility labels, animations, Tamil/TTS when approved |
| 10 | Integrate widget contracts, configuration, and refresh lifecycle | Verify per-widget persona filtering and snapshot mapping | Implement responsive Glance focuses, gradient states, and configuration UI |

### Updating-code workflow

- Use one branch per person: `work/integrator`, `work/core-data`, and `work/compose-ui`.
- Keep commits small and single-purpose, for example `core: add lookahead rule evaluation` or `ui: add rain sentence card`.
- Pull or rebase from the integration branch at least twice per day and before opening a pull request.
- Never edit another person's owned file directly without coordinating first.
- Every pull request must state: files changed, contract changes, validation command, and known follow-up work.
- Person A merges only changes that compile with the current branch. Empty/loading/error states are acceptable placeholders; fabricated weather values are not.
- After every merge, run `./gradlew test` and `./gradlew assembleDebug`. Do not wait until the end of a phase to discover integration failures.
- If two branches conflict, preserve the contract already consumed by the other branch and resolve the conflict in the producer first. Do not solve merge conflicts by deleting tests or replacing working code wholesale.

### Prompt for Person A — Integrator / App shell

```text
You are Person A, the integration owner for PersonaWeather. Read PersonaWeather_Implementation_Plan.md before editing.

Own only the app shell, shared contracts, AppContainer, AppViewModel, UiState, AppRoot/navigation, Gradle, manifest, and integration fixes. Do not implement core weather algorithms or duplicate UI components owned by Person C.

Before coding, inspect the current branch because Persons B and C are continuously updating their code. Preserve existing user changes. Keep public models and interfaces stable; if a change is required, document the old and new shape and update affected consumers in the same change.

Work in one small slice at a time. After each slice run the narrowest relevant test, then ./gradlew test and ./gradlew assembleDebug when integration is affected. Report changed files, contract changes, validation results, and any exact action required from Person B or C. Never hide a failing test or resolve a conflict by deleting code.
```

### Prompt for Person B — Data + pure Kotlin engine

```text
You are Person B, owner of PersonaWeather's data layer and pure Kotlin engine. Read PersonaWeather_Implementation_Plan.md and follow Appendix A, Sections 3–5, 9, and 18 exactly.

Own core/, data/, and JVM unit tests. Keep core/ free of android.* and androidx.* imports. Implement only the approved provider contracts, persona_rules.json schema, and specified algorithms; do not invent weather values, fields, thresholds, network calls, Room, Retrofit, or Hilt.

Because other people are editing continuously, inspect the current branch before changing shared models. Prefer additive changes. If a contract must change, announce the change with migration details and update all known consumers. Keep data loading behind WeatherDataSource and keep UI concerns out of core/data.

After every logical slice run the narrowest unit test, then ./gradlew test. Report exact test results, changed contracts, assumptions recorded in DECISIONS.md, and provider/cache APIs that Person A or C must consume.
```

### Prompt for Person C — Compose experience

```text
You are Person C, owner of PersonaWeather's Compose experience. Read PersonaWeather_Implementation_Plan.md, especially Sections 11–12 and 16.

Own ui/, notification-facing UI, the component gallery, onboarding, Home, Trip Planner visuals, settings, and UI tests. Do not access the provider/cache or JSON from composables. Consume ViewModel state and stable core models; while provider state is unavailable, render loading/empty/error states and never substitute weather fixtures.

The app is being edited continuously by Persons A and B. Inspect current contracts before coding and avoid changing shared models from the UI branch. If a needed state is missing, propose the smallest UiState addition to Person A instead of embedding business logic in the screen. Keep thresholds and advisory decisions in core, never in composables.

Work in small screen/component slices. Validate with the gallery or focused UI test, then ./gradlew assembleDebug when integration is affected. Report changed files, required UiState fields, visual states covered, and any unresolved loading/error/empty state. Preserve existing work during merges.
```

### Definition of done for every contribution

The contribution compiles with the current integration branch, has focused tests or a documented manual check, does not add unapproved network endpoints or dependencies, does not break the pure-Kotlin boundary, and includes a short handoff note for contract changes. The phase is complete only after the phase-specific “You verify” check passes on the integrated branch.

## 18. Testing

**Unit tests (core/, JVM):** rule operators; null/missing values never trip; lookahead aggregation over actual returned timestamps; coastal eligibility; de-duplication; sorting; timestamp/timezone handling; provider-gap and out-of-coverage behavior; insight functions refuse incomplete intervals; route score/verdict refuses incomplete stop coverage.

**Provider contract tests:** parse and map Open-Meteo Weather, Air Quality, and Marine response payloads captured from actual provider responses. Keep capture provenance (endpoint, request parameters, model, capture time, and provider attribution) with the test resource. Do not edit captured values to manufacture weather conditions. Add live smoke tests as opt-in/integration tests; they must not gate routine offline builds on network availability.

**Manual matrix:** every persona × {inland, coastal location} — no crash, sensible cards; airplane mode; font scale 130%; rotate the phone (state must survive); kill/relaunch (prefs restored).
Widget matrix: compact and medium sizes; two widget instances with different localities; persona changes; cached/offline state; airplane mode; remove and re-add widget; ensure no weather appears when neither provider nor valid cache has data.
Offline-pack matrix: successful and interrupted preparation; missing endpoint/stop; process restart; airplane-mode route evaluation; stale and out-of-coverage dates; refresh and delete; retain previous pack after failed refresh.

## 19. Data provenance and limitations (be honest in the pitch and in the About dialog)

1. All weather values shown in the app come from Open-Meteo Weather, Air Quality, or Marine responses, or from a persistent cache of those exact responses. The app does not include generated, scripted, or fallback weather.
2. These are model forecasts, not guaranteed local station observations. Clearly identify the provider/model and forecast horizon; do not call them "live observations".
3. Pollen, soil, tide, visibility, and other fields may not be available for every place/time/model. Show unavailable rather than substituting values or deriving unsupported measurements.
4. Offline forecasts are last-downloaded provider data, not current conditions. Display retrieval time and coverage and refuse to issue current alerts or route verdicts outside that coverage.

## 20. Future-proofing hooks

| Feature | Hook already in place |
|---|---|
| Calendar weather advice (implemented) | Android Calendar Provider supplies event instances. The user confirms indoor/outdoor context and selects a verified forecast locality; raw event location text stays on-device. `CalendarEventWeatherEvaluator` checks all overlapping forecast hours for the full event and bounds rule lookahead to that window. Indoor advice is limited to caveated outdoor AQI; events outside coverage are unavailable. |
| Google Maps / Routes API (real route rain) | `RouteWeather` consumes `List<Waypoint(locationId or lat/lon, eta)>`; only the source of waypoints/ETAs changes. |
| Slack "will this event work?" | `core/` is pure Kotlin with no Android imports → move to a small JVM/Ktor service unchanged. |
| Smart-home (Google Home) | Add an outbound `ActionSuggestion` derived from advisories (e.g., "close windows", "switch on air purifier") behind an interface; no code now. |
| Rolling 24h exposure tracker | Needs a writable store. Exposure = Σ(AQI × hours the user logged outdoors), using only AQI values returned by the provider and explicitly recorded user exposure. |
| Weather source | `OpenMeteoWeatherDataSource` maps Weather, Air Quality, and Marine responses into `WeatherRow`; unsupported fields remain unavailable. There is no alternate/demo source. |
| Sarvam speech-to-text / Tamil voice | Sits behind a `VoiceInput` interface; parked. |

---

## APPENDIX A — Provider field mapping

The Open-Meteo endpoint schema is authoritative. Map only variables requested and actually returned by the approved Weather, Air Quality, and Marine endpoints. Record the provider variable name, unit, timestamp/timezone, model, and endpoint with each mapped value. All fields are nullable. The implementation must not preserve or assume the old bundled `weather_timeseries` schema; unsupported fields are absent, not estimated.

## APPENDIX B — Cards ↔ personas ↔ rules (from persona_rules.json v2)

| Persona | Default cards | Rules (severity) |
|---|---|---|
| health_conscious | aqi, pollen, uv, humidity | AQI ≥100 (caution), ≥150 (alert); grass pollen == Moderate (caution); UV ≥8 (caution) |
| outdoor_fitness | running_window, heat, uv, aqi | heat_alert==1 (caution); UV ≥8 (caution); wind ≥20 (caution); AQI ≥150 (alert) |
| beach_surfer *(coastal only)* | marine, uv | wave ≥1.5 (caution), ≥2.0 (alert); wind ≥18 (caution) |
| traveler | travel_alerts, aqi, trip_planner | visibility ≤5 (caution); fog==1 (alert); rain chance ≥60 (caution) |
| parent_family | family_commute, aqi, heat, cloth_drying | rain ≥50 (caution); heat_alert==1 (caution); AQI ≥150 (alert); **rain ≥60 within 3 h (alert, lookahead)** |
| agriculture_gardener | agriculture | soil ≤25 (caution); frost==1 (alert, dormant); **rainfall sum ≥5 mm in next 24 h (caution, lookahead)** |
| commuter | commute_traffic, trip_planner | fog==1 (caution); rain ≥60 (alert); visibility ≤5 (caution); **rain ≥60 within 2 h (caution, lookahead)** |
| event_planner | event_planning, daily_outlook | comfort=="Poor" (alert); rain ≥40 (caution); UV ≥8 (caution) |

These are advisory thresholds, not weather samples. Evaluate a rule only when the exact field is supplied by the selected provider response or is a documented deterministic calculation from provider values. If the provider does not support the required source inputs (for example pollen, soil, or a fog flag), that card/rule is unavailable; do not synthesize a field to make the rule run.

## APPENDIX C — `Tuning.kt` constants (starting values)

```
RUN_HOURS = 5..21          RUN_WINDOW_HOURS = 2      RUN_NOT_RECOMMENDED_PENALTY = 14.0
DRY_HOURS = 8..17          DRY_MAX_RAIN = 25         DRY_MIN_WINDOW_HOURS = 3
RAIN_LIKELY = 50           RAIN_EASING = 35
HOURLY_ITEMS = 24          DAILY_ITEMS = 7           BANNER_MAX_VISIBLE = 3
ROUTE_SEARCH_MINUTES = -60..180 step 15
ROUTE_FIELDS = rain_chance_percent, aqi_value, visibility_km, fog_flag, heat_alert
ROUTE_BASE_PERSONAS = commuter, health_conscious
EVENT_HOURS = 8..20        EVENT_WEIGHTS = Good 0, Fair 1, Poor 3
```

## APPENDIX D — Definition of Done (MVP)

- [ ] `assembleDebug` builds; APK sideloads via adb and launches on a real phone.
- [ ] Only provider responses and their exact cached/offline-pack copies can supply weather; no bundled, generated, canned, or sample weather is used anywhere in the app.
- [ ] A prepared offline trek pack survives process restart and supports the selected route/date in airplane mode with an explicit source, download time, and forecast-coverage label; missing or out-of-range data is never presented as a valid forecast.
- [ ] Onboarding multi-select → Home shows only the chosen personas' cards; user can edit/reorder cards; choices persist.
- [ ] Switching among all 15 locations updates hero, gradient, banners, cards; coastal-only content appears/hides correctly.
- [ ] Provider contract tests use unmodified, provenance-recorded actual provider responses; application integration shows the exact values returned.
- [ ] Trip Planner matches provider forecasts to each route-stop ETA and rejects missing/out-of-coverage data.
- [ ] Local notifications fire once per alert; clothes alarm uses the alarm sound.
- [ ] UI matches Section 11 (gradient sky, glass cards, 96 sp hero temp, staggered entrance, animated gauges) and is usable at 130% font scale.
- [ ] Phase 10 widgets show provider-backed, persona-specific advice; per-widget locality and refresh behavior pass the Section 18 widget matrix, including explicit unavailable states.
- [ ] DECISIONS.md lists every place the spec was silent.
