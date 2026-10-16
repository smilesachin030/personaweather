# HISTORICAL / SUPERSEDED: PersonaWeather Implementation Spec

This nested document describes an earlier, synthetic-data architecture and is not an active specification. The current provider-only and offline-cache requirements are maintained in `../PersonaWeather_Implementation_Plan.md`, `../DECISIONS.md`, and `../README.md`. Do not use the historical Demo, bundled-dataset, or fixture instructions below.

Native Android (Kotlin + Jetpack Compose) · Offline-first · Persona-driven hyper-local weather & environmental advisory app · Chennai hub, 15 locations

---

## PART A — READ THIS FIRST

### A1. Notes for the human (you)

1. Put the 2 data files in the project (see A4). `weather_advisory.xlsx` is only for you to eyeball; the app never reads it.
2. Paste the **Master Prompt** (A2) into your AI coder, then attach this whole document.
3. Build **one phase at a time** (Section 17). After each phase, run the check in that phase's "You verify" line, then tell the AI "next".
4. Everything you run is your existing pipeline: `./gradlew assembleDebug` then `adb install -r app/build/outputs/apk/debug/app-debug.apk`.

### A2. Master Prompt (paste this to the AI coder)

```
You are a senior Android engineer. Build the app described in the attached spec, exactly, in phases.

RULES
1. Kotlin + Jetpack Compose + Gradle Kotlin DSL. NO Room, NO Hilt/Dagger, NO Retrofit/OkHttp, NO Navigation library,
   NO Maps SDK, NO network calls of any kind. The manifest must NOT declare the INTERNET permission.
2. Work ONE phase at a time (spec Section 17). At the end of a phase: the project must compile with
   `./gradlew assembleDebug`; list files created/changed; list each acceptance criterion as met / not met.
   Do NOT start the next phase until I say "next".
3. Package `core/` must contain NO `android.*` or `androidx.*` imports (pure Kotlin) so it can move to a backend later.
4. Only use data columns listed in Appendix A and rules from assets/persona_rules.json. Never invent data or fields.
5. No magic numbers in composables. Thresholds live in persona_rules.json or in core/engine/Tuning.kt.
6. When the spec is silent, choose the SIMPLEST option that satisfies the goal and record it in DECISIONS.md.
7. Match the visual design in Section 11 closely — UI quality is a primary success criterion.
8. Write unit tests for core/ using the test vectors in Section 18.
```

### A3. What we are building

**Positioning:** a *Contextual Advisory & Decision-Support Engine*, not another weather dashboard. Generic weather apps say "31°C, partly cloudy". PersonaWeather says **what to do**: "Rain expected within 3 hours — bring in your clothes", "AQI 178 at Nungambakkam — take the long way", "Best run: 8–10 PM".

**The 6 differentiators the UI and code must make obvious (judging criteria):**

| # | Differentiator | Where it shows |
|---|---|---|
| 1 | Persona tailoring: one-time multi-select onboarding, smart default cards, user can edit cards | Onboarding, Home card grid, Edit cards |
| 2 | Persona-specific thresholds rule engine (JSON, no code change to tune) | Advisory banners, card footers |
| 3 | Hyper-local: 15 real Chennai-area localities with genuinely different air/heat/sea values | Location switcher |
| 4 | Route weather matched to *arrival time at each stop* (not "weather now") | Trip Planner |
| 5 | Actionable insights: best run window, clothes-drying window, tide highs/lows, best day for an event | Cards |
| 6 | Offline-first: no INTERNET permission, everything works in airplane mode | Demo panel, About |

### A4. Input files (already produced — do not regenerate)

Copy into `app/src/main/assets/`:

| File | What it is |
|---|---|
| `weather_advisory.db` | SQLite. Tables used by the app: `locations` (15 rows), `weather_timeseries` (7,560 rows = 15 locations × 21 days × 24 hourly). Table `chennai_timeseries` is LEGACY — ignore it. |
| `persona_rules.json` | schema_version 2. 8 personas, 15 card definitions, 28 threshold rules (2 severities: `caution`, `alert`). |

Data range: `2026-09-28` is "today" in the demo; data spans `2026-09-27T00:00` → `2026-10-17T23:00`, hourly, local time (IST, no timezone handling needed).

### A5. Scope

| Priority | Feature |
|---|---|
| **P0 (must ship)** | DB + rules loading · SimClock · onboarding (multi-select personas, location) · Home (hero, rain sentence, advisory banners, hourly strip, 7-day list, persona card grid) · all card types · location switcher (15) · edit cards · Trip Planner (single fixed route) · Demo panel with scenario shortcuts · local notifications for alerts · works offline |
| **P1** | 24h chart with metric tabs (Google style) · "Upcoming alerts" screen · best-departure suggestion in Trip Planner · clothes-drying alarm channel with alarm sound · collapsing hero header · sunrise/sunset markers in hourly strip |
| **P2** | Tamil UI + on-device text-to-speech "Listen" button · custom vector weather glyphs · high-contrast / large-text mode |
| **PARKED (design hooks only — do NOT build)** | Google Calendar, Google Maps/Routes API, Slack, Google Home actuation, rolling 24h AQI-exposure tracker, Sarvam AI speech-to-text, live weather APIs |

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
| Data | Bundled SQLite via `android.database.sqlite` (raw SQL, read-only) | Decided (no Room) |
| Rules | `assets/persona_rules.json` parsed with built-in `org.json` | Decided |
| Persistence | `SharedPreferences` (small values) | Simplest |
| DI | None. A hand-written `AppContainer` created in the `Application` class | Simplest |
| Navigation | A `sealed interface Screen` + a `MutableList<Screen>` back stack + `BackHandler` | 6 screens; no library needed |
| Icons | `androidx.compose.material:material-icons-extended` for UI chrome; **emoji** for weather glyphs in P0 (colorful, zero dependency) | See Section 11.6 |
| Notifications | `NotificationCompat` from `androidx.core` | Built in |
| Tests | JUnit4 + `kotlinx-coroutines-test`; `testImplementation("org.json:json:<recent>")` so `org.json` works in JVM unit tests | Android's stub `org.json` throws in unit tests |

**Dependencies to declare** (use latest stable versions; pick a Compose BOM): `androidx.core:core-ktx`, `androidx.activity:activity-compose`, `androidx.lifecycle:lifecycle-viewmodel-compose`, `androidx.lifecycle:lifecycle-runtime-compose`, `androidx.compose:compose-bom`, `ui`, `ui-graphics`, `ui-tooling-preview`, `material3`, `material-icons-extended`. `debugImplementation("androidx.compose.ui:ui-tooling")`.

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

**`AndroidManifest.xml` essentials:** `<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>` only. **No INTERNET permission** (this is a feature: "works in airplane mode").

---

## 2. Architecture & package layout

```
com.example.personaweather/
  PersonaWeatherApp.kt              Application; builds AppContainer
  MainActivity.kt                   enableEdgeToEdge(); setContent { AppRoot() }
  core/                             PURE KOTLIN — no android.* / androidx.* imports
    model/   Location, WeatherRow, Persona, Rule, Advisory, CardSpec, Severity, DailySummary, Waypoint
    engine/  AdvisoryEngine, Insights, RouteWeather, Tuning, Labels
    time/    TimeUtils (rounding, day/night, sunrise/sunset approx)
  data/
    WeatherDataSource.kt            interface (see 3.4)
    WeatherDatabase.kt              copy asset -> internal storage, open read-only
    SqliteWeatherDataSource.kt      implements WeatherDataSource
    RulesLoader.kt                  org.json -> Persona/Rule/CardSpec
    UserPrefs.kt                    SharedPreferences wrapper
    AppContainer.kt
  notify/  NotificationHelper.kt
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
    demo/        DemoPanel
    AppRoot.kt                      Screen back stack
```

**Data flow:** `SQLite → WeatherDataSource → in-memory List<WeatherRow> per location → AppViewModel (combine prefs + SimClock + series) → core engine (advisories, insights) → HomeUiState → Compose UI`.

**Rule:** UI never touches SQLite or JSON. ViewModel never builds UI. `core/` knows nothing about Android.

---

## 3. Data layer

### 3.1 Schema (read-only)

**`locations`** — `id TEXT PK, name TEXT, lat REAL, lon REAL, distance_km_from_hub REAL, direction TEXT, character TEXT, is_coastal INTEGER(0/1)`

| id | name | km from hub | is_coastal | character |
|---|---|---|---|---|
| nungambakkam | Nungambakkam | 4 | 0 | Traffic junction / mixed |
| kilpauk | Kilpauk | 6 | 0 | Mixed |
| royapuram | Royapuram | 6 | 1 | Port-adjacent, humid |
| t_nagar | T. Nagar | 6 | 0 | Commercial / mixed |
| arumbakkam | Arumbakkam | 7 | 0 | Mixed |
| anna_nagar | Anna Nagar | 8 | 0 | Residential |
| adyar | Adyar | 9 | 1 | Residential, humid |
| guindy_alandur | Guindy / Alandur | 12 | 0 | IT corridor, traffic |
| velachery | Velachery | 14 | 0 | Residential, IT-adjacent |
| perungudi | Perungudi | 16 | 0 | IT corridor |
| tambaram | Tambaram | 25 | 0 | Suburban residential |
| ennore | Ennore | 25 | 1 | Port / industrial |
| avadi | Avadi | 30 | 0 | Industrial / defense town |
| sriperumbudur | Sriperumbudur | 40 | 0 | Industrial belt, inland/drier |
| mahabalipuram | Mahabalipuram | 55 | 1 | Coastal / tourist |

**`weather_timeseries`** — 40 columns; full list with types, units and null rules in **Appendix A**. Key facts: 504 rows per location (hourly), `timestamp` is `yyyy-MM-ddTHH:00` (lexicographically sortable), marine columns (`wave_height_m, wave_period_sec, wave_direction, water_temperature, tide_height_m`) are **NULL for the 11 inland locations**.

### 3.2 Opening the database (use this approach exactly)

Android cannot query a file inside `assets/` directly. Copy it to internal storage once, then open it read-only. **Do not extend `SQLiteOpenHelper`** for this: a pre-built DB has `user_version = 0`, so the helper would try to run `onCreate` on it. Use `SQLiteDatabase.openDatabase(...)`.

```kotlin
class WeatherDatabase(private val context: Context) {
    companion object {
        const val ASSET = "weather_advisory.db"
        const val DB_VERSION = 2            // bump when the asset file is replaced -> forces re-copy
    }
    fun open(): SQLiteDatabase {
        val target = context.getDatabasePath(ASSET)
        val meta = context.getSharedPreferences("app_meta", Context.MODE_PRIVATE)
        if (!target.exists() || meta.getInt("db_version", 0) < DB_VERSION) {
            target.parentFile?.mkdirs()
            context.assets.open(ASSET).use { input -> target.outputStream().use { input.copyTo(it) } }
            meta.edit().putInt("db_version", DB_VERSION).apply()
        }
        return SQLiteDatabase.openDatabase(target.path, null, SQLiteDatabase.OPEN_READONLY)
    }
}
```

Reading nullable columns: use `androidx.core.database.getDoubleOrNull / getIntOrNull / getStringOrNull` (core-ktx) with `cursor.getColumnIndexOrThrow("col")`.

### 3.3 Queries (the only SQL the app needs)

```sql
-- at startup
SELECT id, name, lat, lon, distance_km_from_hub, direction, character, is_coastal
FROM locations ORDER BY distance_km_from_hub;

-- when a location is first needed (cache the result; 504 rows)
SELECT * FROM weather_timeseries WHERE location_id = ? ORDER BY timestamp;

-- data range (for SimClock clamping)
SELECT MIN(timestamp), MAX(timestamp) FROM weather_timeseries;
```

**Design decision: load the whole 504-row series of a location into memory** and do everything else (hourly strip, lookahead rules, daily aggregation, best windows, route) as plain Kotlin list operations. It is tiny, fast, and makes `core/` pure and testable. Load on `Dispatchers.IO`; cache in a `ConcurrentHashMap<String, List<WeatherRow>>`. The Trip Planner loads 4 locations (2,016 rows) — still trivial.

### 3.4 Data-source abstraction (extensibility hook — required)

```kotlin
interface WeatherDataSource {
    suspend fun locations(): List<Location>
    suspend fun series(locationId: String): List<WeatherRow>   // hourly, ascending, contiguous
    suspend fun dataRange(): Pair<LocalDateTime, LocalDateTime>
}
class SqliteWeatherDataSource(...) : WeatherDataSource   // implemented now
// FUTURE: OpenMeteoDataSource : WeatherDataSource        // do NOT build
```

### 3.5 Models (core/model)

`WeatherRow` = typed data class of all 40 columns (see Appendix A) **plus**

```kotlin
val time: LocalDateTime            // parsed from timestamp
fun fieldValue(name: String): Any? // returns Double/Int/String/null for any column name
```
Implement `fieldValue` as a `when(name)` over all column names (used by the rules engine). Unknown name → `null`.

### 3.6 SimClock (the demo's "now")

The dataset is simulated, so the app runs on a **simulated clock**, shown in the top bar.

```kotlin
class SimClock(range: ClosedRange<LocalDateTime>, start: LocalDateTime) {
    val now: StateFlow<LocalDateTime>       // always truncated to the hour
    fun set(t: LocalDateTime)               // clamps into range
    fun stepHours(n: Long)
}
```
- **Default start: `2026-09-28T08:00`.** (Decision: fixed, not the device clock, so the demo behaves identically on any day. Settings has a switch "Use device clock (clamped to dataset range)" for P1.)
- Data date `2026-09-28` is a Monday. Day labels use `DayOfWeek` from the date.
- **Auto-play (Demo panel):** advances +1 hour every 2 seconds, so judges see the hero, gradients and alerts evolve.

### 3.7 Data-quality facts the code must respect

- Never assume a marine value exists; render the "unavailable inland" empty state (Section 12.9).
- `frost_alert` is always 0 in this dataset (Chennai has no frost) — the frost rule stays in JSON but is dormant. Do not build UI around frost.
- `best_running_hour` is a static flag (hours 6 and 7). **Ignore it.** Compute the best running window dynamically (Section 5.1).
- Sunrise/sunset are **not** in the data. Use constants **06:00 / 17:55** (approximate for Chennai in Oct), label as "approx."
- `aqi_category` uses US-style labels: `Good`, `Moderate`, `Unhealthy for Sensitive Groups`, `Unhealthy`, `Very Unhealthy`. Display the DB label as-is.
- `condition` vocabulary (exactly 6 values): `Sunny`, `Clear`, `Partly cloudy`, `Scattered clouds, chance of rain`, `Showers`, `Thunderstorm`.

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
4. **De-duplicate per `(personaId, field)`**: keep the highest severity; if equal, the later-defined rule. (So AQI 178 shows only `hc_aqi_alert`, not also `hc_aqi_caution`.)
5. Sort: `ALERT` before `CAUTION`; then persona order; then rule order.
6. Return the list. **No UI text formatting here.**

### 4.5 RulesLoader (data/RulesLoader.kt)

Parse `persona_rules.json` with `org.json`. `value` may be number or string: `when (val raw = obj.get("value")) { is Number -> raw.toDouble(); is String -> raw }`. Build `Persona(id, label, requiresCoastal, defaultCards, rules)` and `CardSpec(id, label, fields, requiresCoastal, derived)`. If parsing fails → show the error state (Section 12.9), never crash.

### 4.6 Persistence (`UserPrefs`, file `persona_weather_prefs`)

| Key | Type | Notes |
|---|---|---|
| `onboarded` | Boolean | |
| `personas` | String (comma-separated ids) | ordered; StringSet is unordered — do not use it |
| `location_id` | String | default `nungambakkam` |
| `cards_order` | String (comma-separated card ids) | |
| `cards_disabled` | StringSet | |
| `cards_customized` | Boolean | |
| `language` | String `en`/`ta` | P2 |
| `cloth_alarm_enabled` | Boolean | default true |
| `notifications_enabled` | Boolean | default true |
| `sim_time` | String ISO | last demo time, restored on launch |

**Default location decision:** `nungambakkam` (central hub, and the location with the most dramatic air-quality range). The user changes it any time from the top-bar chip; onboarding step 3 lets them choose.

---

## 5. Derived insights (core/engine/Insights.kt) — pure functions

All take `series` + `nowIndex` and are unit-tested. Every threshold is a constant in `Tuning.kt`.

### 5.1 Best running window

- Candidate hours: hours 5–21 within the next 24 rows. Window = 2 consecutive hours.
- Per-hour penalty = `0.5*max(0, temp-28) + 0.05*max(0, aqi-50) + 0.5*uv + 0.05*rain_chance + 0.1*max(0, wind-15)`. Window penalty = sum of its two hours.
- Choose the minimum-penalty window (ties → earliest). If the best penalty ≥ `RUN_NOT_RECOMMENDED_PENALTY = 14.0` → result `NotRecommended` ("Skip the outdoor run — try an indoor workout").
- Output: `start`, `end`, and the row values (AQI, temp, UV) for the sub-line.
- Verified behaviour: Nungambakkam on the polluted morning (Sep 28) → best window **8–10 PM**; Mahabalipuram → **5–7 AM**.

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

### 5.8 Label helpers (core/engine/Labels.kt — no Compose colors here)

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

- Inputs combined with `combine(prefs, simClock.now, selectedLocation)`; recompute on `Dispatchers.Default` with `flowOn`.
- Persist every preference change immediately through `UserPrefs`.
- Notifications: when the sim clock changes, compute new `ALERT` advisories; if a `ruleId` has not been notified for `(locationId, date)` and notifications are enabled → post one (Section 13).
- Screen navigation is a plain `mutableStateListOf<Screen>()` in `AppRoot`, with `BackHandler`.

---

## 7. Performance & threading notes

- **Threads:** DB copy/open/read on `Dispatchers.IO`; engine + insights on `Dispatchers.Default`; only the final `HomeUiState` reaches the main thread.
- **Cache:** one `List<WeatherRow>` per location, kept for the process lifetime; a `Map<String, Int>` from `timestamp` → index per location gives O(1) "now" lookups.
- **Recompute only on change:** derive `HomeUiState` from `combine(prefs, simClock.now, selectedLocation)`; use `distinctUntilChanged()` so unrelated pref writes don't recompute.
- **Compose:** give every `LazyColumn` item a stable `key`; keep heavy lists (`hourly`, `daily`) immutable and remembered on `(location, nowIndex)`; draw gauges/curves/route map on `Canvas` (no bitmaps).
- **Budget:** location switch and sim-hour step must finish in < 300 ms on a mid-range phone (the data is only 504 rows per location).

---

## 8. Alerts screen (P1)

Title "Alerts". Two sections: **Now** (all advisories, not capped) and **Upcoming (next 12 hours)**: run the engine for each of the next 12 rows and list the first hour each rule trips as "Around 5 PM · Rain likely — ...". Same banner component as Home.

---

## 9. Trip Planner (single fixed route)

### 9.1 Route (constants in `core/model/RouteConfig.kt`)

```kotlin
val ROUTE = listOf(
    Waypoint(locationId = "velachery",      label = "Home · Velachery",         minutesFromStart = 0),
    Waypoint(locationId = "guindy_alandur", label = "Guindy / Alandur",         minutesFromStart = 20),
    Waypoint(locationId = "t_nagar",        label = "T. Nagar",                 minutesFromStart = 45),
    Waypoint(locationId = "nungambakkam",   label = "Office · Nungambakkam",    minutesFromStart = 60),
)
```

> **Note — a correction to the earlier plan:** the earlier order (…Nungambakkam → T. Nagar) doubles back geographically (12.9 km). The order above (Velachery → Guindy → T. Nagar → Nungambakkam) is the natural south-to-north path (≈10.3 km straight-line). The waypoints are a config list, so reordering is a one-line change.

### 9.2 ETA → weather matching

```kotlin
fun LocalDateTime.roundToNearestHour(): LocalDateTime {          // half-up
    val base = truncatedTo(ChronoUnit.HOURS)
    return if (minute >= 30) base.plusHours(1) else base
}
// For each waypoint: eta = departure.plusMinutes(w.minutesFromStart); row = series(w.locationId)[eta.roundToNearestHour()]
```
Clamp to the data range. (Stretch: linear interpolation between the two neighbouring hours for numeric fields.)

### 9.3 Per-waypoint advisories

Evaluate the engine **at each waypoint's own series and ETA row** with:
- personas = `[commuter, health_conscious] ∪ user's active personas` (de-duplicated),
- `fieldFilter = {rain_chance_percent, aqi_value, visibility_km, fog_flag, heat_alert}` (only trip-relevant fields; keeps UV/pollen noise out),
- lookahead rules work naturally because the engine takes the ETA index.

Waypoint severity = highest advisory severity there (none → OK).

### 9.4 Route verdict & scoring

`routeScore = Σ over waypoints Σ advisories (ALERT=3, CAUTION=1)`.
Verdict pill: any ALERT → **"Alert"** (red); else any CAUTION → **"Caution"** (amber); else **"Clear"** (green).
Summary sentence (template): list the worst 2 waypoints — "Heavy rain at T. Nagar (~5:00 PM, 75%) and unhealthy air at Nungambakkam (AQI 170)."

### 9.5 Better departure time (P1)

Evaluate `routeScore` for departures from `dep − 60 min` to `dep + 180 min` in 15-minute steps. Pick the minimum (ties → earliest). Show only if `bestScore < currentScore`:
"Leave at **7:30 PM** instead — dry at all stops, air moderate." with a button **Use this time**. Description is built from the best route: "dry at all stops" if max rain < 50, "air moderate" if max AQI < 100.

### 9.6 Screen spec

1. Top bar: back arrow, title "Trip planner".
2. **Route header** (glass card): "Home → Office", "~10 km · ~60 min", tiny footnote "Sample route: stops and timings are fixed for the demo."
3. **Departure selector:** chips `Now · +30 min · +1 h · 5 PM`, and a stepper `[–15]  4:30 PM  [+15]`. Below: "Arrive ≈ 5:30 PM".
4. **Verdict card:** pill + summary sentence + (P1) "better time" suggestion.
5. **Route map card (schematic, drawn on Canvas, no map SDK):** project each waypoint's lat/lon to canvas — `x = (lon - minLon)*cos(meanLat) / span`, `y = (maxLat - lat) / span` with 24dp padding, uniform scale. Draw: faint dots for the other 11 localities, a 4dp rounded polyline in white 60% alpha, 14dp nodes filled with severity color (green/amber/red), labels beside nodes, an animated dash along the line (P1).
6. **Timeline list:** one row per waypoint: severity dot + vertical connector; place; ETA time; weather glyph + temperature; rain chip (blue) with %; AQI chip (AQI color); visibility if ≤ 5 km; the advisory lines (persona-tagged) under it.
7. **Route exposure strip:** "Average AQI on route: 143 · Stops with rain ≥ 50%: 3 of 4".

Expected results (from the shipped dataset; use as acceptance criteria):

| Sim departure | Rain % at stops (V/G/T/N) | AQI at stops | Verdict | Better time |
|---|---|---|---|---|
| Sep 28 08:00 | 25 / 19 / 26 / 21 | 126 / 159 / 165 / 178 | Alert (AQI climbing along the route) | 10:15 AM |
| Sep 28 16:30 | 90 / 90 / 75 / 90 | 81 / 93 / 92 / 112 | Alert (storm) | between 6:45 PM and 7:30 PM (exact minute depends on which personas are active — with only the base route personas `commuter + health_conscious` it is **6:45 PM**, score 0) |

The stop-by-stop rain %, AQI and verdicts in this table are fixed data facts and must match exactly. Only the "better time" varies with the active persona set, because each persona adds its own rules to the route score.

---

## PART C — UI/UX

## 10. Design principles (inspired by Apple Weather + Google Weather)

**Take from Apple Weather:** the full-bleed gradient that reflects the sky, the huge light-weight temperature, the hero that collapses into a compact header while scrolling, frosted modular cards with tiny ALL-CAPS headers, the horizontal hourly strip, the 10-day list with range bars, the sentence-style rain summary ("Rain expected around 5 PM").

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

Animate each stop with `animateColorAsState(tween(900))` so the sky glides when the simulated hour changes.

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

`Screen` = `Onboarding | Home | TripPlanner | Alerts | Settings`. Sheets (`ModalBottomSheet`): `LocationSheet`, `PersonaSheet`, `EditCardsSheet`, `DemoPanel`. First launch → Onboarding; afterwards → Home.

### 12.2 Onboarding (4 steps, `HorizontalPager` disabled for swipe — use Next/Back buttons; progress dots)

1. **Welcome** — dark night gradient, ☁️ glyph, title "Weather that tells you what to do", one-line sub "Pick who you are — we'll show what matters." Button **Get started**. (P2: language chooser English / தமிழ் here.)
2. **Who are you?** — title "Pick all that apply". 2-column grid of 8 **PersonaTile**s (icon 32 dp, label, one-line description, check badge when selected; selected = brighter glass + 1.5 dp white border). **Continue** enabled when ≥ 1 selected.
   Descriptions: Health-conscious "AQI, pollen, UV"; Outdoor fitness "Best run time, heat, wind"; Beachgoer & surfer "Waves, tides, sea temp"; Traveler "Fog, rain, packing tips"; Parent & family "School run, clothes drying"; Agriculture & gardener "Soil moisture, rain, irrigation"; Commuter "Rain, fog, route weather"; Event planner "Best day, comfort, rain risk".
3. **Where are you?** — searchable list of the 15 localities (name, "14 km · Residential"); selecting highlights. Default `nungambakkam`. Note under the list: "You can switch any time — try a coastal one like Adyar or Mahabalipuram."
4. **Alerts** — explain "We'll alert you when conditions matter to you". Button **Allow notifications** (runtime permission on API 33+, via `rememberLauncherForActivityResult(RequestPermission())`) and **Maybe later**. Finish → 1 s "Preparing your dashboard…" shimmer → Home. Persist everything.

### 12.3 Home screen — section order (single `LazyColumn`, background gradient behind everything)

```
┌───────────────────────────────────────┐
│ ▾ Velachery · 14 km          ⏱ Mon 9:00 AM   ⚙ │  1 top bar (location chip, sim-time chip, settings)
│ [🏃 Fitness] [👪 Family] [🚗 Commuter]  ✎ │  2 persona chips (tap → PersonaSheet)
│                                       │
│               Velachery               │  3 HERO
│                 31°                   │    heroTemp 96sp Light
│           Partly cloudy               │
│          H:33°   L:22°                │
│    ⛈ Rain expected around 5 PM (90%)  │  4 rain sentence pill
│                                       │
│ ┌ ALERT  Rain · Parent & Family ────┐ │  5 advisory banners (max 3 + "See all N")
│ │ Rain expected within 3 hours —    │ │
│ │ bring in any drying clothes.  🔊  │ │
│ └────────────────────────────────────┘ │
│ ┌ NEXT 24 HOURS ────────────────────┐ │  6 hourly strip card
│ │ Now  10AM 11AM 12PM 1PM …          │ │
│ │  ⛅   ☀️   ☀️   🌦️   🌧️           │ │
│ │ 31°  32°  33°  33°  30°           │ │
│ │ 💧   10%  20%  55%  90%           │ │
│ └────────────────────────────────────┘ │
│ FOR YOU                          Edit │  7 persona card grid
│ ┌──────────────────────────────────┐  │
│ │ AIR QUALITY   132  Sensitive     │  │  (wide)
│ │ ▓▓▓▓▓▓░░░░░░  PM2.5 82 · PM10 116│  │
│ └──────────────────────────────────┘  │
│ ┌─────────────┐ ┌──────────────────┐  │
│ │ UV  7       │ │ HUMIDITY 78%     │  │  (two small)
│ └─────────────┘ └──────────────────┘  │
│ …more cards…                          │
│ 7-DAY FORECAST                        │  8 daily list card
│ Today  🌧️ 90%  22° ▬▬▬▬▬▬ 33°        │
│ Tue    ⛅      22° ▬▬▬▬▬▬ 34°        │
│                                       │
│ Demo dataset · modelled on IMD/TNPCB station patterns │  9 footer (micro text)
└───────────────────────────────────────┘
```

**Hero details.** Location name (heroLocation), temperature (heroTemp) with the `°` at 60% size, condition display name + glyph, `H:` / `L:` from today's rows, "Feels like {feels_like}°" in label style. Under it the **rain sentence** in a pill (blue-tinted glass). Under that a one-line **"Today for you"** headline (Section 5.7) with a 🔊 button (P2).
**Collapse (P1):** when the list scrolls past the hero, fade the hero out (`graphicsLayer { alpha = 1 - progress }`) and fade in a compact sticky bar showing `Velachery  31°  Partly cloudy`.

**Advisory banners.** Height wraps content. Left: severity icon (pulsing dot for ALERT); title row `ALERT · Rain` (12 sp caps) + persona tag chip ("Parent & Family"); body = `message` (15 sp). Max 3 visible; if more → a text button "See all N alerts" → Alerts screen. Swipe-to-dismiss hides a banner **for the current sim hour only** (state in ViewModel, not persisted). Field → title: `aqi_value→Air quality, pollen_*→Pollen, uv_index→UV, heat_alert→Heat, wind_speed→Wind, wave_height_m→Sea, visibility_km→Visibility, fog_flag→Fog, rain_chance_percent→Rain, soil_moisture_percent→Soil, rainfall_mm→Rain, comfort_index→Comfort`.

**Hourly strip.** `LazyRow` of 24 items; item width 64 dp; contents top→bottom: time label ("Now", "10 AM"), glyph 28 sp, temperature, and a rain-% line (blue) only if ≥ 20%. Current item has a brighter glass pill behind it. (P1: insert "Sunrise 6:00 AM" / "Sunset 5:55 PM" items.)

**7-day list.** Each row: day label ("Today", "Tue"), glyph, rain % (blue, only if ≥ 30), min temp, a **range bar** (rounded, gradient from 11.4 temp scale, spanning that day's min→max within the global min/max of the 7 days), max temp, and a tiny AQI dot (color of daily avg AQI). Tap a row (P1) → none.

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
| `aqi_card` (wide) | Left: AQI number (cardValue) + category chip in AQI color. Right: "Main pollutant PM2.5", "PM2.5 62 µg/m³ · PM10 88 µg/m³". Below: full-width gradient bar (green→yellow→orange→red→purple, scale 0–300) with a white marker at the value (animated). |
| `pollen_card` (small) | Three rows Grass / Tree / Ragweed, each with a 3-dot meter (dots filled per level) and the label. |
| `uv_card` (small) | UV number + label chip (UV colors), thin gradient bar with marker, footer "Sun protection {first hour with uv≥3}–{last}" for today. |
| `humidity_card` (small) | Humidity % (cardValue), "Dew point 24° · Muggy" using 5.8. |
| `running_window_card` (wide) | Header "BEST TIME TO RUN". Big text "8–10 PM" (or "Not recommended today"). Sub-line "AQI 89 · 27° · UV 0". A 24-cell timeline (hours 5–21) colored green→amber→red by hourly penalty, best window outlined in white. Footer "Sunrise 6:00 AM · Sunset 5:55 PM (approx.)". |
| `heat_card` (small) | Feels-like number, "Hottest around 2 PM (34°)", red "Heat alert" chip if `heat_alert==1`. |
| `marine_card` (wide, coastal) | Wave height big + status chip (Calm/Moderate/Rough/Dangerous), row: period s · direction · water temp °C. Tide curve (Canvas) over 24 h with dots at highs/lows and label "Next high tide 4:00 PM". **Inland:** empty state (12.9). |
| `travel_alerts_card` (wide) | Visibility km, fog chip if flagged, "Rain chance next 6 h: max X%". Disruption risk = High if any ALERT, Medium if any CAUTION, else Low. Packing chips (5.6). Button "Plan a trip →" opens Trip Planner. |
| `family_commute_card` (wide) | Two mini rows: "Morning 7–9 AM: max rain 5% · 27°" and "Pickup 4–6 PM: max rain 35% · 32°" (computed from that day's series). Heat and AQI chips if flagged. |
| `agriculture_card` (wide) | Left: radial soil-moisture gauge (Canvas arc, 270°; ≤25% red, 26–35% amber, >35% green). Right: "Rain next 24 h: 12 mm", irrigation advice: soil ≤25 and rain-24h < 2 mm → "Irrigate this evening"; rain-24h ≥ 5 mm → "Hold off irrigation"; else "Soil is adequate". (Frost row omitted — dormant.) |
| `commute_traffic_card` (small) | "Roads": Clear / Wet / Poor visibility; "Rain in next 2 h: max X%"; small link "Plan a trip →". |
| `event_planning_card` (wide) | Comfort chip (Good/Fair/Poor), rain %, UV, AQI; sentence "Outdoor event conditions are Fair now". |
| `cloth_drying_card` (wide) | Header "CLOTHES DRYING". Status chip (5.2). Big text: window "9 AM – 1 PM". Row of 9 mini bars (08–16) colored by dryness. Switch "Rain alarm" bound to `cloth_alarm_enabled`. |
| `trip_planner_card` (wide) | "Home → Office · ~60 min", verdict pill for **leaving now**, 4 dots colored by severity connected by a line; tap → Trip Planner. |
| `daily_outlook_card` (wide) | Event-oriented 7-day strip: each day a column with comfort dot, rain %, AQI; the best day has a "Best day" ribbon (5.5). |

### 12.5 Location sheet

`ModalBottomSheet`, search field, list of 15 rows: name, "14 km · Residential, IT-adjacent", a coastal `🌊` marker for coastal ones, and a tiny live AQI dot for the current sim hour. Selecting closes the sheet, persists, and the hero/gradient animate to the new place. This is the "Judge test" ("switch to a coastal city and show me marine alerts"): selecting Mahabalipuram with `beach_surfer` active must immediately show the marine card + wave alert (at the storm hour).

### 12.6 Persona sheet

Same tile grid as onboarding, multi-select, **Done** button; applies the rules in 4.2.

### 12.7 Top bar sim-time chip

Always visible (honest: the data is simulated). Text `Mon 28 Sep · 9:00 AM`. Tap → Demo panel. Long-press → nothing.

### 12.8 Edit cards sheet

Opened by "Edit" next to FOR YOU. A list of **all** 15 card definitions: leading up/down arrow buttons (reorder), label, switch (enabled). Cards that need coastal data show a "Coastal only" caption when the current location is inland. Footer buttons: **Reset to persona defaults**, **Done**. Sets `cards_customized = true` on any change.

### 12.9 Empty / loading / error states

- **Loading:** shimmer glass placeholders while the DB copies (first launch only).
- **Marine inland:** icon `Waves` at 40% alpha + "Sea conditions aren't available for inland areas. Try Adyar, Royapuram, Ennore or Mahabalipuram." + button "Switch location".
- **Rules JSON error / DB error:** full-screen gradient with a friendly message and "Retry"; log the exception with tag `PersonaWeather`.
- **No advisories:** a single calm banner in green: "All clear — nothing needs your attention."

### 12.10 Settings

Rows: Your personas → PersonaSheet; Location → LocationSheet; Notifications (switch); Clothes rain alarm (switch); Language (P2); **Demo controls** → Demo panel; About → dialog: "Data: simulated dataset modelled on IMD automatic-weather-station and TNPCB air-quality station patterns for Chennai. The app is offline by design (no internet permission). Live-data connectors are planned."; Reset app (clears prefs → onboarding).

---

## 13. Notifications (notify/NotificationHelper.kt)

- Channels (create at app start, API 26+): `advisories` (IMPORTANCE_HIGH, default sound) and `clothes_alarm` (IMPORTANCE_HIGH, sound = `RingtoneManager.getDefaultUri(TYPE_ALARM)` with `AudioAttributes.USAGE_ALARM`). A channel's sound can't be changed after creation — note in DECISIONS.md.
- Permission: `POST_NOTIFICATIONS` (API 33+) requested in onboarding step 4; if denied, the in-app banners still work.
- Trigger: in the ViewModel, when the sim hour changes (or auto-play advances), for each new `ALERT` advisory whose `ruleId` has not been posted for `(locationId, date)` → post once. `pf_cloth_alert` posts on `clothes_alarm` (only if `cloth_alarm_enabled`), everything else on `advisories`.
- Content: title = `"{Field title} alert · {Location}"`, text = `message`, tap → opens the app on Home.
- The Demo panel has **Send test alert** (posts a sample so judges can see it without waiting).

## 14. Localization & text-to-speech (P2 — design now, build later)

- Do **not** use Android resource switching or AppCompat per-app locales (extra dependencies). Use an in-app `Lang { EN, TA }` and a `Strings` object with two maps and a `t(key)` lookup exposed through a `CompositionLocal`; switching is instant, no restart.
- Advisory text: use `message_ta` from JSON when `language == ta`, fall back to `message`.
- TTS: `TextToSpeech(context, listener)`; language `Locale("ta", "IN")` for Tamil. After init, check `isLanguageAvailable(locale)`; if `LANG_MISSING_DATA`/`LANG_NOT_SUPPORTED`, show a snackbar with an action that launches `TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA`. A 🔊 **Listen** button appears on the "Today for you" headline and on each banner.
- Rural/low-literacy design: large tap targets, icon-first cards, read-aloud advisories. (Speech-to-text via Sarvam AI is **parked**: it needs a network API.)

## 15. Offline, robustness

- No network code anywhere. Verify: airplane mode → everything works. No INTERNET permission in the manifest.
- Never crash on NULL columns (marine) — `fieldValue` returns null and rules skip.
- Out-of-range sim time → clamp.
- Guard divide-by-zero in gauges and range bars (single-value series).
- Wrap DB open + JSON parse in `runCatching` → `AppUiState.Error`.

---

## PART D — DEMO, PHASES, TESTS

## 16. Demo mode

**Demo panel** (bottom sheet from the top-bar time chip or Settings):
1. **Time control:** date stepper (−1 d / +1 d), hour slider 0–23, ⏮ ⏭ hour buttons, **Play/Pause** (+1 h every 2 s).
2. **Scenario shortcuts** (each sets sim time + location + adds the needed persona if missing):

| Scenario | Sim time | Location | Persona(s) | What must appear |
|---|---|---|---|---|
| Polluted morning | Sep 28 09:00 | Nungambakkam | Health-conscious | ALERT air quality (AQI 178, Unhealthy) + CAUTION pollen. Switch to Velachery (AQI 132): only cautions → "location matters". Smog gradient. |
| Storm evening commute | Sep 28 16:30 | Velachery (Trip Planner) | Commuter | Verdict Alert, rain 90/90/75/90%, better time ≈ 6:45–7:30 PM |
| Clothes on the line | Sep 28 14:00 | Velachery | Parent & family | ALERT "Rain expected within 3 hours — bring in any drying clothes" (starts at 13:00) + notification with alarm sound |
| Foggy dawn | Sep 29 05:00 | Nungambakkam | Traveler + Commuter | Fog ALERT, visibility 2.1 km, fog glyph and gradient |
| Surf check | Sep 28 19:00 | Mahabalipuram | Beachgoer & surfer | Wave 2.05 m → ALERT "Rough sea… not advised" + CAUTION strong wind; tide curve |
| Heat spell | Oct 2 14:00 | Sriperumbudur | Outdoor fitness | Heat CAUTION (37.5°C) + UV CAUTION |
| Plan an event | Sep 28 (7-day list) | Velachery | Event planner | Best day **Sat 3 Oct**; **Sun 4 Oct** Poor (66% rain) |
| Wet Sunday | Oct 4 14:00 | Tambaram | Event planner | Comfort ALERT + rain CAUTION |
| Offline proof | any | any | any | Turn on airplane mode; app keeps working |

3. **Send test alert**, **Reset onboarding**, **Language** (P2).

## 17. Build phases (one at a time; each ends with a compiling app)

**Phase 0 — Scaffold & assets.** Gradle project, package layout (Section 2), assets copied, manifest (POST_NOTIFICATIONS only), theme stub. *Accept:* `assembleDebug` succeeds; app launches. *You verify:* screen shows text "rows=7560 locations=15 personas=8".

**Phase 1 — Data layer.** `WeatherDatabase`, `SqliteWeatherDataSource`, models incl. `fieldValue`, `SimClock`, `RulesLoader`, `UserPrefs`, `AppContainer`. *Accept:* unit tests for models/loader pass; series length per location = 504; marine fields null for inland. *You verify:* debug screen prints Velachery row for `2026-09-28T09:00` (AQI 132, temp 29.2).

**Phase 2 — Engine + insights.** `AdvisoryEngine`, `Insights`, `Labels`, `Tuning`. *Accept:* all Section 18 test vectors pass. *You verify:* run `./gradlew test` green.

**Phase 3 — Design system + component gallery.** Theme, gradients, GlassCard, Gauge, Sparkline, WeatherGlyph, SeverityBanner, all 15 card composables rendered from fixtures on a hidden "gallery" screen. *Accept:* every card looks like Section 12.4; gradient switch works. *You verify:* scroll the gallery; compare with the wireframe.

**Phase 4 — Onboarding + persistence.** 4-step onboarding, prefs, default cards. *Accept:* picks survive app restart; ≥1 persona required. *You verify:* pick Fitness + Family → kill app → reopen → lands on Home with those chips.

**Phase 5 — Home.** ViewModel, hero, rain sentence, headline, banners, hourly strip, 7-day list, card grid, location sheet, persona sheet, edit-cards sheet, top bar with sim-time chip. *Accept:* scenario "Polluted morning" and "Surf check" behave exactly as in Section 16; switching location updates everything in < 300 ms. *You verify:* the Judge test in 12.5.

**Phase 6 — Trip Planner.** Route config, ETA matching, per-waypoint advisories, verdict, timeline, schematic map, departure selector, exposure strip; (P1) better-time suggestion. *Accept:* the Section 9.6 expected-results table. *You verify:* set departure 16:30 on Sep 28 → red verdict, and a "better time" suggestion between 6:45 PM and 7:30 PM.

**Phase 7 — Demo panel + notifications.** Time controls, scenarios, auto-play, channels, permission flow, alarm sound for clothes. *Accept:* all scenario shortcuts land on the described result; test alert appears in the shade. *You verify:* Clothes-on-the-line scenario fires a notification at 13:00 sim time.

**Phase 8 — Polish.** Animations (11.7), collapsing hero (P1), 24h chart with tabs (P1), Alerts screen (P1), accessibility pass, empty/error states, app icon (adaptive, sky-gradient + cloud), splash. *Accept:* usable at 130% font scale; TalkBack labels present; no jank on a mid-range phone.

**Phase 9 (P2) — Tamil + TTS + custom glyphs.**

## 17A. Three-person parallel implementation plan

The phases above describe product order. The three contributors work in parallel inside that order, with one integrator responsible for keeping the app buildable.

### Team ownership

| Person | Owns | Primary files | Depends on |
|---|---|---|---|
| A — Integrator / App shell | Project scaffold, shared contracts, `AppContainer`, `AppViewModel`, `UiState`, app root, navigation, integration builds | `PersonaWeatherApp.kt`, `MainActivity.kt`, `data/AppContainer.kt`, `viewmodel/`, `ui/AppRoot.kt`, Gradle and manifest | Decisions from the plan; interfaces from B and C |
| B — Data + pure Kotlin engine | SQLite/JSON loading, preferences, models, SimClock, rules, advisories, insights, route calculations, unit tests | `core/`, `data/`, `src/test/` | Assets and Appendix A; no UI dependency |
| C — Compose experience | Theme, components, onboarding, home, cards, trip/demo/settings screens, notifications UI states, fixture/gallery screen | `ui/`, `notify/`, UI tests | Stable models and `UiState`; fixture data may be used first |

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
| 0 | Scaffold, manifest, build, startup screen | Validate asset names and database schema | Create theme stub and fixture gallery shell |
| 1 | Wire `AppContainer` and debug state | Implement all data layer, models, SimClock, loader, prefs, tests | Build reusable components against fixtures |
| 2 | Expose engine results through `UiState` | Implement and test engine, insights, labels, tuning | Render advisory/banner/card states from fixtures |
| 3 | Integrate gallery into app root | Fix model or fixture gaps found by gallery | Implement design system and all 15 card composables |
| 4 | Connect onboarding state and persistence | Provide default-card and preference APIs | Implement onboarding and edit-card flows |
| 5 | Implement ViewModel and screen integration | Support performance/data bugs found by the Judge test | Implement Home, sheets, hero, hourly strip, daily list, and card grid |
| 6 | Integrate route state and navigation | Implement and test route/ETA calculations | Implement Trip Planner visualizations and controls |
| 7 | Integrate scenarios and notification lifecycle | Validate alert inputs and edge cases | Implement Demo panel, notification UI, and settings |
| 8–9 | Release integration and acceptance matrix | Regression tests and accessibility data checks | Polish, accessibility labels, animations, Tamil/TTS when approved |

### Updating-code workflow

- Use one branch per person: `work/integrator`, `work/core-data`, and `work/compose-ui`.
- Keep commits small and single-purpose, for example `core: add lookahead rule evaluation` or `ui: add rain sentence card`.
- Pull or rebase from the integration branch at least twice per day and before opening a pull request.
- Never edit another person's owned file directly without coordinating first.
- Every pull request must state: files changed, contract changes, validation command, and known follow-up work.
- Person A merges only changes that compile with the current branch. A temporary fixture or adapter is acceptable, but it must be clearly named and removed when the real implementation lands.
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

Own core/, data/, and JVM unit tests. Keep core/ free of android.* and androidx.* imports. Implement only the shipped SQLite schema, persona_rules.json schema, specified algorithms, and test vectors; do not invent fields, thresholds, network calls, Room, Retrofit, or Hilt.

Because other people are editing continuously, inspect the current branch before changing shared models. Prefer additive changes. If a contract must change, announce the change with migration details and update all known consumers. Keep data loading behind WeatherDataSource and keep UI concerns out of core/data.

After every logical slice run the narrowest unit test, then ./gradlew test. Report exact test results, changed contracts, assumptions recorded in DECISIONS.md, and any fixture or API that Person A or C must consume.
```

### Prompt for Person C — Compose experience

```text
You are Person C, owner of PersonaWeather's Compose experience. Read PersonaWeather_Implementation_Plan.md, especially Sections 11–12 and 16.

Own ui/, notification-facing UI, the component gallery, onboarding, Home, Trip Planner visuals, Demo panel, settings, and UI tests. Do not access SQLite or JSON from composables. Consume ViewModel state and stable core models; while those are unavailable, use clearly named fixtures with the same final types.

The app is being edited continuously by Persons A and B. Inspect current contracts before coding and avoid changing shared models from the UI branch. If a needed state is missing, propose the smallest UiState addition to Person A instead of embedding business logic in the screen. Keep thresholds and advisory decisions in core, never in composables.

Work in small screen/component slices. Validate with the gallery or focused UI test, then ./gradlew assembleDebug when integration is affected. Report changed files, required UiState fields, visual states covered, and any unresolved loading/error/empty state. Preserve existing work during merges.
```

### Definition of done for every contribution

The contribution compiles with the current integration branch, has focused tests or a documented manual check, does not add forbidden dependencies or `INTERNET`, does not break the pure-Kotlin boundary, and includes a short handoff note for any contract or fixture change. The phase is complete only after the phase-specific “You verify” check passes on the integrated branch.

## 18. Testing

**Unit tests (core/, JVM):** rule operators; NULL never trips; lookahead aggregates; coastal gating; de-dup per (persona, field); sorting; ETA rounding (16:30→17:00, 16:29→16:00); best-run window; drying window; tide events; rain sentence; daily summaries; route score/verdict.

**Test vectors (from the shipped dataset — assert exactly):**

| # | Location · time · personas | Expected advisories (ruleId : severity : observed) |
|---|---|---|
| T1 | nungambakkam · 2026-09-28T09:00 · health_conscious | `hc_aqi_alert : ALERT : 178`, `hc_pollen_caution : CAUTION : Moderate` (no `hc_aqi_caution`, deduped) |
| T2 | velachery · 2026-09-28T09:00 · health_conscious, outdoor_fitness | `hc_aqi_caution : CAUTION : 132`, `hc_pollen_caution : CAUTION` (fitness: none) |
| T3 | velachery · 2026-09-28T14:00 · parent_family | `pf_cloth_alert : ALERT : 90.0` (lookahead max) |
| T4 | velachery · 2026-09-28T12:00 · parent_family | *no* `pf_cloth_alert` (3-h max is 55) |
| T5 | nungambakkam · 2026-09-29T05:00 · traveler, commuter | `tr_visibility_caution` (2.1), `tr_fog_alert : ALERT`, `cm_fog_caution`, `cm_visibility_caution` |
| T6 | mahabalipuram · 2026-09-28T19:00 · beach_surfer | `bs_wave_alert : ALERT : 2.05`, `bs_wind_caution : CAUTION : 21.8` (no `bs_wave_caution`, deduped) |
| T7 | nungambakkam · 2026-09-28T09:00 · beach_surfer | *empty* (inland → persona skipped) |
| T8 | sriperumbudur · 2026-10-02T14:00 · outdoor_fitness | `of_heat_caution : CAUTION`, `of_uv_caution : CAUTION : 8.7` |
| T9 | velachery · 2026-09-28T17:00 · commuter | `cm_rain_alert : ALERT : 90.0` (+ `cm_rain_soon` deduped away), `cm_visibility_caution : 2.8` |
| T10 | tambaram · 2026-10-04T14:00 · event_planner | `ep_comfort_alert : ALERT`, `ep_rain_caution : CAUTION : 66.0` |

**Insight vectors:** best run window Nungambakkam from 2026-09-28T05:00 → 20:00–22:00 (penalty ≈ 8.4); Mahabalipuram → 05:00–07:00. Tide events Mahabalipuram from 2026-09-28T08:00: LOW 10:00 (0.1), HIGH 16:00 (1.29), LOW 22:00, HIGH 05:00. Drying window Velachery 2026-09-28 (from 08:00): 09:00–13:00; 2026-09-29: none; 2026-09-30: 08:00–14:00. Route (Section 9.6 table) for departures 08:00 and 16:30. **Route unit test (personas = `commuter` + `health_conscious`, `fieldFilter` per Section 9.3, ETAs rounded half-up):** departure 2026-09-28T08:00 → per-stop scores 1/3/3/3 = **10**, best departure **10:15 (score 4)**; departure 2026-09-28T16:30 → per-stop scores 4/4/4/5 = **17**, best departure **18:45 (score 0)**.

**Manual matrix:** every persona × {inland, coastal location} — no crash, sensible cards; airplane mode; font scale 130%; rotate the phone (state must survive); kill/relaunch (prefs restored).

## 19. Data caveats (be honest in the pitch and in the About dialog)

1. The dataset is **simulated**: hourly diurnal curves + injected weather episodes, with per-locality character (traffic hubs and industrial belts carry higher AQI; coastal spots are humid with marine data; Sriperumbudur is hotter/drier). It is modelled on real IMD automatic-weather-station and TNPCB air-quality-station patterns; it is **not** live telemetry.
2. Injected episodes on specific dates (so alerts are demonstrable): Sep 28 polluted morning + west→east evening thunderstorm; Sep 29 fog + lingering pollution; Oct 1 smaller afternoon storm; Oct 2 heat spell; Oct 4 wet day.
3. Soil moisture drifts slowly and is not coupled to rainfall events. Frost never occurs. AQI labels are US-style.
4. Because the app reads through `WeatherDataSource`, the demo→live swap is one class (Open-Meteo/IMD) — this answers the evaluator red flag "hardcoded weather mockups": the location switcher and the route feature already resolve data dynamically by coordinates/location id and time.

## 20. Future-proofing hooks (design now, build later — do NOT implement)

| Parked feature | Hook already in place |
|---|---|
| Calendar events (event management) | `AdvisoryEngine.evaluate(series, nowIndex, …)` takes any index → evaluate at an event's start; reuse the window scoring from the route code over `[start, end]`. |
| Google Maps / Routes API (real route rain) | `RouteWeather` consumes `List<Waypoint(locationId or lat/lon, eta)>`; only the source of waypoints/ETAs changes. |
| Slack "will this event work?" | `core/` is pure Kotlin with no Android imports → move to a small JVM/Ktor service unchanged. |
| Smart-home (Google Home) | Add an outbound `ActionSuggestion` derived from advisories (e.g., "close windows", "switch on air purifier") behind an interface; no code now. |
| Rolling 24h exposure tracker | Needs a **writable** store: create a separate `exposure.db` via `SQLiteOpenHelper`; **never write to the bundled read-only DB**. Exposure = Σ(AQI × hours the user logged outdoors). |
| Live data | Implement `OpenMeteoDataSource : WeatherDataSource`; map its variables onto the `WeatherRow` columns (Appendix A). |
| Sarvam speech-to-text / Tamil voice | Sits behind a `VoiceInput` interface; parked. |

---

## APPENDIX A — `weather_timeseries` columns (40)

| # | Column | Type | Unit / values | Null? |
|---|---|---|---|---|
| 1 | id | INTEGER PK | | no |
| 2 | location_id | TEXT | FK → locations.id | no |
| 3 | timestamp | TEXT | `yyyy-MM-ddTHH:00` | no |
| 4 | date | TEXT | `yyyy-MM-dd` | no |
| 5 | hour | INTEGER | 0–23 | no |
| 6 | temperature | REAL | °C | no |
| 7 | feels_like | REAL | °C | no |
| 8 | humidity | REAL | % | no |
| 9 | wind_speed | REAL | km/h | no |
| 10 | wind_direction | TEXT | SE, S, SSE, E (coastal) / SW, W, NW, N (inland) | no |
| 11 | condition | TEXT | the 6 values in 3.7 | no |
| 12 | pressure_hpa | REAL | hPa | no |
| 13 | dew_point | REAL | °C | no |
| 14 | uv_index | REAL | 0–~10 (0 at night) | no |
| 15 | rain_chance_percent | REAL | 0–95 | no |
| 16 | rainfall_mm | REAL | mm in that hour | no |
| 17 | visibility_km | REAL | km | no |
| 18 | fog_flag | INTEGER | 0/1 | no |
| 19 | aqi_value | INTEGER | ~35–180 | no |
| 20 | aqi_category | TEXT | US-style labels (3.7) | no |
| 21 | dominant_pollutant | TEXT | PM2.5 / PM10 | no |
| 22 | pm2_5 | REAL | µg/m³ | no |
| 23 | pm10 | REAL | µg/m³ | no |
| 24 | no2 | REAL | µg/m³ | no |
| 25 | o3 | REAL | µg/m³ | no |
| 26 | co | REAL | mg/m³ | no |
| 27 | so2 | REAL | µg/m³ | no |
| 28 | pollen_grass | TEXT | Low / Moderate | no |
| 29 | pollen_tree | TEXT | Low | no |
| 30 | pollen_ragweed | TEXT | Low | no |
| 31 | wave_height_m | REAL | m | **yes (inland)** |
| 32 | wave_period_sec | REAL | s | **yes (inland)** |
| 33 | wave_direction | TEXT | SE | **yes (inland)** |
| 34 | water_temperature | REAL | °C | **yes (inland)** |
| 35 | tide_height_m | REAL | m | **yes (inland)** |
| 36 | soil_moisture_percent | REAL | % | no |
| 37 | frost_alert | INTEGER | always 0 | no |
| 38 | heat_alert | INTEGER | 1 if temp ≥ 34 | no |
| 39 | best_running_hour | INTEGER | **ignore** (static) | no |
| 40 | comfort_index | TEXT | Good / Fair / Poor | no |

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

## APPENDIX C — `Tuning.kt` constants (starting values)

```
RUN_HOURS = 5..21          RUN_WINDOW_HOURS = 2      RUN_NOT_RECOMMENDED_PENALTY = 14.0
DRY_HOURS = 8..17          DRY_MAX_RAIN = 25         DRY_MIN_WINDOW_HOURS = 3
RAIN_LIKELY = 50           RAIN_EASING = 35
HOURLY_ITEMS = 24          DAILY_ITEMS = 7           BANNER_MAX_VISIBLE = 3
ROUTE_SEARCH_MINUTES = -60..180 step 15
ROUTE_FIELDS = rain_chance_percent, aqi_value, visibility_km, fog_flag, heat_alert
ROUTE_BASE_PERSONAS = commuter, health_conscious
SUNRISE = 06:00   SUNSET = 17:55   (approximate)
EVENT_HOURS = 8..20        EVENT_WEIGHTS = Good 0, Fair 1, Poor 3
SIM_DEFAULT_START = 2026-09-28T08:00
```

## APPENDIX D — Definition of Done (MVP)

- [ ] `assembleDebug` builds; APK sideloads via adb and launches on a real phone.
- [ ] No INTERNET permission; every feature works in airplane mode.
- [ ] Onboarding multi-select → Home shows only the chosen personas' cards; user can edit/reorder cards; choices persist.
- [ ] Switching among all 15 locations updates hero, gradient, banners, cards; coastal-only content appears/hides correctly.
- [ ] All 10 test vectors (T1–T10) and all scenario shortcuts in Section 16 reproduce exactly.
- [ ] Trip Planner reproduces the Section 9.6 table; per-stop weather is matched to each ETA.
- [ ] Local notifications fire once per alert; clothes alarm uses the alarm sound.
- [ ] UI matches Section 11 (gradient sky, glass cards, 96 sp hero temp, staggered entrance, animated gauges) and is usable at 130% font scale.
- [ ] DECISIONS.md lists every place the spec was silent.
