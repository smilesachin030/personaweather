# HISTORICAL / SUPERSEDED: Person B Implementation Decisions

This nested file records decisions for an earlier synthetic-data implementation and is not authoritative. Use `../DECISIONS.md` and `../PersonaWeather_Implementation_Plan.md` for current provider-only, offline-cache behavior.

- The workspace contains the specification and source data only; no Gradle Android project or Person A shared contract existed. Person B files therefore use the package and contracts specified in the plan, but Android adapters cannot be integrated or compiled in this workspace yet.
- Android-bound code stays in `data/`; `core/` uses only Kotlin/JVM and `kotlinx.coroutines` for the specified `SimClock` StateFlow.
- Persona and card ordering is explicitly fixed to the order listed in the implementation plan because `JSONObject` iteration order is not a portable contract.
- ETA lookup uses the first hourly row at or after the rounded ETA when no exact row exists, and clamps before/after the loaded series. The shipped hourly series should normally provide exact rows.
- Drying-window ties use lowest average humidity, then earliest start as a stable final tie-break.
- No additional weather fields, thresholds, or network behavior were introduced.

## Handoff

- Person A: wire `SqliteWeatherDataSource`, `RulesLoader`, `UserPrefs`, and `SimClock` from the app container. Add Android/Compose/Gradle dependencies from the plan and keep `weather_advisory.db` plus `persona_rules.json` under `app/src/main/assets/`.
- Person C: use `Insights`, `AdvisoryEngine`, `RouteWeather`, and their result types from the core package. `RouteWeather.ROUTE` is the fixed route; pass loaded per-location series and the coastal map to `evaluate`.
- Validation in this workspace was limited to source diagnostics, JSON parsing, pure-Kotlin import-boundary checking, and direct read-only SQLite queries because Java/Kotlin/Gradle are unavailable and no Android project scaffold is present.