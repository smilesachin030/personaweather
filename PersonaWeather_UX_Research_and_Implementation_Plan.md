# PersonaWeather — UX Research & Implementation Plan

**Purpose:** A product-specific UX plan to complement the existing `PersonaWeather_Implementation_Plan.md`. It keeps the app's core promise—turn local weather and environmental data into useful decisions—while making those decisions easier to notice, understand, trust, and act on.

**Scope:** UX and interaction changes for onboarding, Home, advisories, locations, forecasts, Trip Planner, notifications, and accessibility. This is a plan, not an instruction to replace the existing data engine or redesign the product as a generic weather dashboard. Keep the original implementation plan and its data constraints authoritative.

## 1. Product outcome

Help a person answer, at a glance:

1. **What matters to me right now?**
2. **What should I do, and by when?**
3. **How certain and how current is this information?**

The primary UX success is not time spent in the app. It is a correct, confident decision with minimal effort. The app should reduce uncertainty without manufacturing urgency or implying more precision than the data supports.

### UX principles

- **Decision before detail:** lead with the most useful action; expose supporting measurements when wanted.
- **Personal, not presumptive:** tailor defaults to chosen personas, make them easy to change, and never infer sensitive traits.
- **Calm urgency:** distinguish genuinely time-sensitive alerts from useful but non-urgent insights.
- **Trust is part of the interface:** make source, freshness, mode, and uncertainty understandable at the point of use.
- **Progressive disclosure:** show essentials first and allow a deliberate path to detail; do not hide important settings or safety context.
- **Accessible by default:** preserve meaning with text, icons, and structure as well as color; work with screen readers, large text, and reduced motion.
- **Respect user agency:** no dark patterns, forced notification opt-ins, deceptive scarcity, or manipulative engagement loops.

## 2. Behavioral-design foundations and how to apply them

These are design lenses and hypotheses to validate with users—not guarantees of behavior.

| Foundation | Application in PersonaWeather | Guardrail |
|---|---|---|
| Cognitive load and chunking | Group Home into **Now**, **Next**, and **For you**. Give each card one headline, one useful measure, and at most one next step. | Do not remove useful context merely to make the screen look sparse. |
| Recognition over recall | Use familiar labels, clear selected states, recent locations, and visible source/mode labels. Avoid requiring users to remember what a color or icon means. | Keep labels visible; icons alone are not a substitute for language. |
| Hick–Hyman law / choice architecture | Offer recommended persona and card defaults, with a simple route to customize. Make the first choice set small and scannable. | Recommendations must be reversible, explainable, and not preselect notification consent. |
| Progressive disclosure | Put the actionable summary first; reveal hourly, daily, metric, and rule details on deliberate expansion or navigation. | Keep critical warnings and data caveats visible without expansion. |
| Fitts' law and touch ergonomics | Make primary actions and frequently used controls easy to reach and at least 48 dp in the Android interface. | Do not pack the Home header with small icon-only controls. |
| Risk communication and alarm fatigue | State **condition + impact + action + time window** in advisories. Reserve the strongest treatment for actionable, high-severity cases. | Never escalate tone or severity to increase engagement; avoid repeated notifications for unchanged conditions. |
| Self-determination and user agency | Let users choose personas, locations, cards, and alert preferences; explain the benefit before asking permission. | Skip, defer, and undo paths must be real and easy to find. |
| Trust calibration | Show the provider, freshness/cache state, and when a value is modeled or unavailable. State uncertainty plainly. | Do not imply station-level measurements or exact predictions if the data is modeled or coarser. |
| Peak-end and feedback | Make setup completion and actions (location changed, cards saved, trip pack ready) end with clear confirmation and a useful next step. | Feedback should confirm a real state change, not simulate progress or success. |

**Practice references:** Nielsen Norman Group's usability heuristics (status visibility, real-world language, consistency, error prevention, user control); Hick–Hyman and Fitts' laws; cognitive-load and recognition-over-recall principles; Android Material 3 and accessibility guidance; WCAG 2.2 AA as a contrast and operability baseline. Validate these applications in the actual Android UI, including TalkBack and system font scaling.

Weather-app references reviewed:

- [Google Weather on Google Play](https://play.google.com/store/apps/details?id=com.google.android.apps.weather) for a focused current-conditions and forecast experience.
- [Apple Weather user guide](https://support.apple.com/en-in/guide/iphone/iph1ac0b35f/ios) for the progressive path from current conditions to hourly, 10-day, and condition-specific details, with region-availability caveats.
- [AccuWeather on Google Play](https://play.google.com/store/apps/details?id=com.accuweather.android) for its stated emphasis on near-term precipitation, radar, and severe-weather alerts.

Use these as interaction-pattern references, not as a source for PersonaWeather's forecast values or a reason to add unsupported data. Keep the existing Open-Meteo provider and the app's locality/coverage caveats authoritative.

Platform guidance checked for this plan:

- [Compose accessibility semantics](https://developer.android.com/develop/ui/compose/accessibility/semantics): use component roles and state, descriptive labels, meaningful grouping/headings, and restrained live-region announcements.
- [Android accessibility principles](https://developer.android.com/guide/topics/ui/accessibility/principles): label meaningful controls, use cues beyond color, and prefer built-in accessible component behavior.
- [WCAG 2.2 contrast minimum](https://www.w3.org/WAI/WCAG22/Understanding/contrast-minimum.html): verify text contrast against the actual rendered surface; transparency over a changing background cannot guarantee readable contrast.

## 3. Recommended end-to-end experience

### First run: deliver value before requesting commitment

1. **Welcome:** Explain in one sentence that PersonaWeather turns local forecasts into practical advice. Primary action: **Get started**. Show a small preview only when real forecast data is ready.
2. **Personalization:** Ask “What would you like weather help with?” Use multi-select persona tiles with stock outline icons and short, concrete examples. Mark any suggested default as a suggestion, not a hidden choice. Explain that choices shape advice and alert priority as well as the Home cards.
3. **Location:** Show the current/default locality and searchable Chennai-area locations. Make “Use this location” / “Choose another” clear, retain a sensible default, and explain that location can be changed later. Do not request device location permission if locality selection is sufficient.
4. **Dashboard preview:** Briefly show how the chosen personas change the advice hierarchy, supporting context, accent treatment, and starting cards. Let the user review the choices here; persona changes after setup belong in Settings, while card editing remains available on Home.
5. **Notifications (optional):** First explain the kinds of alert they may receive and let them choose categories or timing where supported. Ask for the OS permission only after the user taps an explicit opt-in. Include **Not now** and leave the app fully useful without permission.
6. **Finish:** Confirm the selected locality/personas and open Home. Avoid a fake wait or an uninterruptible preparation animation; use a real loading indicator only when work is actually pending.

Persist setup as each step is completed so interruption does not erase choices. Back navigation must preserve selections. Errors must identify what failed and give a retry or continue-with-offline option where valid.

### Returning user: orient, don't make them configure again

Open directly to Home with the currently selected location and data mode visible. Keep location and card controls discoverable; put persona changes in Settings rather than adding a persona editor to Home. When changing a setting, confirm the applied value and update affected advice without an unexplained reset or jump in scroll position.

## 4. Screen-by-screen plan

### Home: useful hierarchy instead of a wall of metrics

Recommended information order:

1. **Context bar:** locality, provider and freshness/cache status, and an accessible settings entry. Location selection should be a clear, large target.
2. **Now:** current condition, temperature, feels-like and high/low where available.
3. **Top action:** the single most important current advisory, phrased as an action and time window. If there is no active warning, show a reassuring useful insight (for example, a favorable activity window), not a fabricated alert.
4. **Next:** concise rain outlook and a scannable hourly forecast.
5. **For you:** persona-specific cards, ordered by current relevance rather than by a fixed parade of metrics.
6. **Later:** daily forecast and less urgent supporting detail.

Keep Home scrollable and predictable. Do not let animated entry effects delay reading or move content under a user's finger. Avoid making users dismiss an alert just to reach forecast content. Show at most one expanded urgent advisory and a compact count/summary for additional items; “See all” opens the complete, severity- and time-ordered list. A dismissed banner may be hidden for the current simulated hour as specified, but its alert remains available in Alerts.

Each advisory should answer **what happened, why it matters to this persona, what to do, and when**. Example pattern: “Rain likely after 5 PM. Bring drying clothes indoors before then.” Include a supporting value or source on expansion, not a string of unexplained thresholds. If the engine does not provide enough context for a safe action, show the condition without inventing advice.

### Weather-linked hydration and food ideas (planned)

Extend the existing heat advice with a small, optional food idea when the app has a current, provider-backed heat signal. For example: “It feels hot today. Drink water regularly and carry a bottle; watermelon or cucumber can be a refreshing, water-rich snack.” Keep the water action first; food is an optional example, never a prescription or a substitute for water.

- Reuse the existing heat-alert / feels-like trigger and the existing Weather advice card. Do not add a nutrition persona, health profile, new data provider, or a separate Home card for the first version.
- Apply only when a relevant persona is selected (health-conscious, outdoor fitness, parent & family, or traveler). Keep one combined heat/hydration message and deduplicate against the Traveler packing tip's existing water-bottle suggestion.
- Use forecast evidence already shown by “Why this advice?”; do not infer dehydration, nutritional need, allergies, food preferences, or what foods are available locally. Food examples should be localized and optional.
- Keep this as general weather-linked wellness guidance, not individualized dietary or medical advice. Do not prescribe water quantities, electrolyte/salt intake, dietary restrictions, or claim a food prevents/treats heat illness. Severe-heat safety instructions remain higher priority; food ideas must never dilute or replace them.
- Do not add food-safety/storage recommendations without suitable data. If the heat signal is missing, stale beyond the existing policy's allowed use, or below threshold, show no food recommendation.

### Persona-specific experience: make relevance visible beyond card order

Keep the same navigation, core weather facts, and safety hierarchy for everyone, but make the first screen feel meaningfully tuned to the needs a person selected:

- **Change the emphasis, not the product shell.** Use each selected persona's stock outline icon and a restrained accent/detail treatment in the Home summary and related cards. Keep typography, layout rules, contrast, and navigation consistent so the app stays recognizable and usable.
- **Change the lead-in and supporting context.** When the forecast supports it, give the top relevant persona a concise, time-based framing (for example, a commute window, a run window, or a family drying window) and surface the matching useful measure. Keep severe weather and high-confidence safety advice above persona-specific convenience advice.
- **Layer multiple needs without competing dashboards.** Let the current advisory engine and relevance order decide which selected perspective leads; make the other relevant needs discoverable in “For you.” Avoid repeating the same weather fact as separate persona cards.
- **Preview real differences during onboarding.** Show selected needs and their starting cards, and explain that advice/alert priority also adapts. Do not use fabricated forecast values or pretend a static mock is live data; only render a live preview if forecast data is actually ready.
- **Keep persona editing in Settings after setup.** Home may show passive, concise context such as “For you,” but must not make persona chips or the dashboard a shortcut to changing persona. Keep the Home card editor focused on cards.
- **Respect choice and privacy.** A persona is a chosen lens, not a demographic profile. Do not infer age, health status, family structure, occupation, or identity; use only the selected needs and available weather data.

Validate personalization with the same forecast and locality under two different persona selections. A reviewer should be able to point to a real difference in lead advice/context and a subtle visual cue, while confirming that raw values, provider/freshness labels, severity ranking, and access to safety information remain truthful and consistent.

### Forecasts and cards: progressive detail

- Maintain one consistent card anatomy: topic, plain-language takeaway, key value and unit, relevant time, optional explanation/details.
- Use a tap or an explicitly labelled **Details** action to reach a chart or supporting data; do not make the whole card's behavior ambiguous.
- Label charts and axes in text; give meaningful accessible summaries, units, and selected-range state.
- Show missing or inapplicable measures as **Not available here** / **Not relevant for this location**, not as zero or an empty blank.
- Support edit, reorder, enable, and disable actions with visible saved state and a reset-to-recommended option.
- Explain AQI/UV/risk categories with a word label and suggested action; do not rely on a color gauge alone.

### Location selection: confidence and locality

Use a searchable, keyboard-friendly list of localities with name and helpful distinguishing context. Highlight the selected location with both text and a non-color state. Retain a recent/selected location for quick return. Do not suggest a distance or locality type unless those values exist and are accurate in the bundled data. For coastal-only measures, explain why the card appears or is absent when useful.

### Trip Planner: make the decision path legible

Lead with route, travel date/departure time, and one overall verdict with a concise reason. Follow with the route timeline so people can see conditions **at estimated arrival time per stop**, not just current weather. Make high-risk stops easy to identify in text and on the route; color is supplementary. Let a user inspect the conditions and data time behind the verdict. Clearly label a suggested departure time as a recommendation, explain what it optimizes, and preserve manual departure selection.

For offline preparation, show exactly which route/date/weather/safety context will be saved, the completion state, and whether the pack is still valid. If data is unavailable or stale, say so before the trip starts; never present an empty route as “safe.”

### Alerts and notifications: relevance over volume

- Sort in-app alerts by severity and time relevance; show timestamp, location, reason, and action.
- Ask notification permission in context after explaining the benefit, never as a blocking onboarding requirement.
- Allow categories and quiet periods where the product supports them. Make alert controls easy to revisit.
- Group or suppress repeats for the same continuing event; notify again only for a meaningful change or renewed urgency.
- Notification copy should be short, actionable, non-alarmist, and identify the locality. Opening it should land on the matching detail.
- Distinguish in-app advisories from OS notifications; turning off notifications must not remove Home advice.

### Source status and data limitations

The current app uses provider forecasts with cached/stale status; it has no selectable Demo/Live mode. Keep source and freshness/cache status visible, indicate offline or stale data when cached values are shown, and identify modeled air-quality values and data limitations in relevant detail/About surfaces. If a demo mode is reintroduced, label it explicitly and never merge sample and provider values without a clear transition.

## 5. Visual, interaction, and accessibility refinements

Keep the existing sky-inspired palette and glass-card direction if desired, but treat legibility and hierarchy as constraints, not decoration:

- Verify foreground/background contrast for **every** gradient state, alert color, disabled state, and text size. Add a stable scrim or opaque surface when needed; translucent dark glass over a light sky does not guarantee contrast.
- Use semantic roles consistently. Pair severity colors with the words **Alert**, **Caution**, or **Info** plus an appropriate icon. Do not use red/amber/green as the sole signal.
- Avoid all-caps for long or important messages; reserve it for short section labels. Use body-sized text for actions and risk explanations.
- Support Android font scaling without clipped, overlapping, or fixed-height text. Test at 100%, 130%, and the largest practical accessibility setting.
- Give every icon-only control a useful TalkBack label, hint, and state. Group advisory semantics so the message is read in a meaningful order, without redundantly reading decorative glyphs.
- Maintain at least 48 dp interactive targets, logical focus order, visible keyboard focus, and accessible selected/expanded/checked state.
- Respect system reduced-motion settings. Do not pulse urgent alerts indefinitely; a static severity icon and concise label are clearer and less distracting.
- Keep motion short, interruptible, and tied to an actual state change. Do not animate essential text or make a moving gradient reduce text legibility.
- Ensure touch targets do not overlap and content remains usable in landscape, on small screens, and with display magnification.

## 6. Phased delivery plan

### Phase 0 — UX baseline and decision freeze

- Capture current flows/screens for first run, returning Home, location change, advisory, notification opt-in, Trip Planner, and offline/stale states.
- Map the current implementation to this plan; mark already-satisfied items rather than rebuilding them.
- Confirm product language, mode/source labels, alert severity policy, and event/action copy with available real data.
- Create a compact screen-state inventory: loading, ready, no relevant alert, no data, stale/cached, offline, and recoverable error.

**Exit check:** Every primary screen has a defined user goal, primary action (or explicit read-only purpose), and empty/error/offline behavior.

### Phase 1 — Trustworthy decision hierarchy

- Reorder Home around context → now → top action → next forecast → personalized cards → daily detail.
- Establish the advisory message pattern and limit expanded banner competition.
- Keep provider, freshness/cache, modeled-data, and stale/offline treatments visible at the point of use; only add Demo/Live labels if a demo mode exists.
- Ensure alert details remain available when a Home banner is dismissed.

**Exit check:** In a brief usability test, users can identify the highest-priority advice, its time window, and whether data is Demo or Live without coaching.

### Phase 2 — Low-friction, respectful personalization

- Make onboarding resumable, skippable where safe, and reversible.
- Improve persona and location choice scanning, descriptions, selected states, outline-icon recognition, and back behavior.
- Preview how selected needs change advice priority and the starting cards before setup ends; offer recommended cards with explicit editing.
- Apply restrained persona-specific emphasis and context to Home and related advice, while keeping the shared shell and safety hierarchy stable.
- Keep persona editing in Settings after onboarding; do not expose it from Home.
- Move notification permission to a contextual, voluntary ask and provide usable “Not now” behavior.

**Exit check:** A first-time user can finish setup, change a persona in Settings and a location from its visible control, and reach Home without granting notification permission or losing earlier choices. With the same forecast loaded, two different persona selections produce a clear, truthful difference in lead context or advice and a subtle visual cue—not only a different card list.

### Phase 3 — Forecast and trip comprehension

- Standardize card layout, key-value units, details disclosure, and chart labels.
- Clarify route verdict, per-stop arrival conditions, risk explanations, and offline pack validity.
- Add the heat-linked hydration/optional food idea to the existing persona advice path, reusing its trigger, evidence, priority, and localization; remove duplicate water copy from packing tips where both surfaces are visible.
- Add explicit unavailable/stale states instead of blank or success-shaped fallbacks.

**Exit check:** Users can explain the route's main risk and identify which stop/time causes it; they can distinguish unavailable information from safe conditions. In supported heat conditions, they understand that drinking water is the main action and food examples are optional general ideas, not medical advice.

### Phase 4 — Accessibility, refinement, and release validation

- Verify TalkBack semantics, focus order, touch targets, contrast, font scale, landscape/small displays, and reduced motion.
- Review all English and Tamil strings for truncation, clarity, and consistent units if Tamil UI is active.
- Remove motion or visual treatments that harm comprehension; tune only after usability observation.

**Exit check:** No critical task is blocked by assistive technology, larger text, reduced motion, or common screen sizes; release blockers are fixed and retested.

## 7. Validation plan and acceptance criteria

Use short, moderated task tests with people representing different chosen needs (for example, commuter, family, outdoor fitness, and beach). Do not treat persona labels as demographic facts. Ask participants to think aloud, observe errors and hesitation, and confirm whether the copy is understood. Validate provider, cached/stale, and offline data states.

### Core tasks

1. Identify what to do about today's most important condition and when to do it.
2. Find the hour with the best conditions for a chosen activity.
3. Switch to a coastal locality and explain why a marine card is or is not present.
4. Change a persona from Settings, change a card from Home, and confirm each change updates only its intended personalization.
5. Plan a route and identify the stop/time that drives the route verdict.
6. Tell which provider supplies a value and whether it is current, cached/stale, modeled, or unavailable.
7. Defer notification permission and later locate its settings.
8. Check an upcoming calendar event, confirm indoor/outdoor context and a forecast locality, then explain the event-duration advice or why coverage is unavailable. Confirm that calendar location text remains on-device and indoor AQI is an outdoor estimate.
9. Given a hot-weather advisory, explain the hydration action, identify the food suggestion as optional, and confirm that no individualized diet or medical claim is being made. Verify that no duplicate water tip appears when Traveler packing tips are also shown.

### Release targets

- At least **4 of 5** representative participants complete each core task without moderator help.
- At least **4 of 5** correctly state the primary advisory's action and time window after one glance.
- All tested participants distinguish current provider data from cached/stale and unavailable data; test Demo versus Live only if both modes exist.
- At least **4 of 5** identify whether event advice covers the full event duration and understand the indoor-air-quality caveat.
- At least **4 of 5** understand that water is the primary heat-related action and any food examples are optional; no tested user mistakes the suggestion for personalized medical/nutrition advice.
- No blocking accessibility defects in critical journeys; verify TalkBack, 48 dp targets, contrast, reduced motion, and 130%+ text scaling on-device.
- No notification opt-in is required to complete onboarding or use the core app.
- Validate loading, no-alert, no-data, stale, offline, error, and permission-denied states; no state may silently imply “all clear.”

These are initial product targets. Record task failures and confusion, revise the interface, and retest rather than treating small-sample results as statistical proof. Avoid adding analytics or transmitting personal behavior data just to measure these goals; use moderated sessions and local test builds unless separately consented product analytics are approved.

## 8. Implementation notes for the AI coder

- Treat this file as a UX addendum to `PersonaWeather_Implementation_Plan.md`, not a replacement for its schema, data caveats, architecture, or acceptance tests.
- Before changing a screen, inspect the current UI and ViewModel state and reuse existing composables, tokens, localization, and preferences.
- Implement a phase as a complete slice; preserve current behavior not explicitly changed here.
- Add/adjust UI tests for important semantics and state transitions where existing test infrastructure supports them; test pure decision logic in the core layer.
- Do not fabricate data, confidence, recommendations, distances, or alert causes to make the UX feel complete. Surface missing information honestly.
- Treat hydration/food guidance as deterministic weather-linked wellness copy only; reuse the current heat rule, localize examples, deduplicate with packing tips, and keep medical/nutritional advice out of scope.
- Keep content concise and localize through the existing strings layer; do not hard-code a new parallel copy system.
- After each phase, report changed files, tested user tasks/states, accessibility checks, and any unmet exit criteria.

### Applied refinements

- Onboarding persona choices use distinct stock outline icons, clearer multi-select feedback, and a truthful preview of advice emphasis plus starting cards. Persona editing is available from Settings and no longer from Home.
- Home identifies the provider and, when applicable, cached/stale status; gives current advisories priority over secondary suggestions; and no longer substitutes invented temperature/AQI/clear-sky values when a current observation is unavailable.
- Home's “Now” summary now omits weather values when the current provider row is missing, and an empty forecast no longer receives an “All clear” message. The lead advisory is surfaced ahead of secondary suggestions; additional advisories remain available from Alerts.
- The “For you” section explicitly names the selected personas so the data-insight ordering is understandable, with Tamil and Hindi labels.
- When the current forecast is unavailable, Home offers retry/location actions and hides forecast-derived cards, summaries, and assistant entry points instead of showing fallback measurements or all-clear copy.
- The Trip Planner card is a neutral entry point; it no longer presents a preset route, stop count, distance, or duration as if it were the user's plan.
- WeatherBuddy remains a screen-level draggable control rather than expanding the “Today” forecast card.
- WeatherBuddy opens from a labeled weather-guide pill; the chat keeps suggested prompts directly above the visible question composer, types selected prompts into the composer, and reveals forecast-aware replies progressively.
- A stable app-level background/content-color surface keeps destination text readable in dark mode. Settings headings use the shared typography scale and the source explanation is grouped on a contrasting surface.
- Weather advice has stronger visual hierarchy and larger action text; the supporting evidence is hidden until the user opens the localized, accessible “Why this advice?” control.
- Home's location/settings/card controls meet the 48 dp target and the location control has button semantics.
- Onboarding no longer silently selects a persona; it requires an explicit choice, communicates the selection requirement, exposes persona/location selection state to assistive technology, preserves selections through saved state, and explains that notifications are optional.
- Glass cards and route verdicts use stable dark surfaces for legible text over changing sky colors; card titles are exposed as headings; continuously pulsing severity markers were removed.
- Smart-home suggestions no longer claim to control an unconnected device; the primary action is explicitly a user-confirmed “mark as done” action, with accurate completion copy.
- Tamil and Hindi strings were added for new setup-status and unavailable-current-condition messages.
- HomeViewModel tests now target the current live-forecast behavior, including stale-data disclosure, location recovery, user-customized cards, and honest suggestion acknowledgment.
- The rain chart now uses a labeled 0–100% axis and reports the actual peak in its summary/accessibility description, avoiding misleading first-to-last values such as 0% → 0%.
- Weather advice leads with an action and time window; rain/UV/AQI/wave measurements stay in expandable evidence. Trip Planner’s route headline likewise gives a packing or exposure action while retaining detailed stop metrics.
- Family guidance and the commute card use school drop-off/pickup windows instead of arbitrary dates or overnight rain. Road status uses the same near-term horizon as its rain warning and calls out conflicting rain-condition/0% probability fields instead of presenting contradictory copy.
- UV cards show the next daylight peak even when current UV is low at night. Coastal cards select the nearest available marine forecast when the current row lacks wave data, name the sea state, give safety guidance, and show the roughest forecast time.
- Packing tips are forecast-driven for the next 24 hours, including sunscreen from UV 3 during daylight, suitable rain gear, water in heat, and an optional well-fitting mask for sensitive users. English, Tamil, and Hindi copy and commute-format tests were updated together.
- Pollen requests six species-specific concentration series from Open-Meteo and keeps their `grains/m³` values and hourly peaks separate from AQI. When the selected location has no returned pollen values, the card shows one concise, localized unavailable-forecast message rather than a technical provider/AQI explanation. The air-quality API is allowed to auto-select its regional model so pollen can populate where supported; a live Chennai query currently returns null for every pollen species, which is presented as unavailable rather than fabricated.
- Hourly and daily forecasts are exposed as complete, localized TalkBack summaries rather than fragmented visual-only tiles; the 24-hour chart announces its selected metric and timestamped values. Location choices expose radio-button selection state, settings switches expose their labels and checked state as single controls, and onboarding progress is announced as a determinate step.

**Current build verification and deployment:** `assembleDebug` succeeded after the UX and pollen changes. The generated debug APK (`app/build/outputs/apk/debug/app-debug.apk`, version 1.2 / versionCode 3) was installed via ADB on the Moto G67 Power and explicitly launched; Android reported `com.example.personaweather/.MainActivity` as top-resumed, the app process remained alive, and no `FATAL EXCEPTION` appeared in the post-launch log check. `testDebugUnitTest` remains blocked before executing tests because the existing `HomeViewModelTest.kt` references removed Demo-mode APIs (`ModeAwareWeatherDataSource`, `DemoScenarios`, `weatherMode`, and `simTime`) and removed `WeatherRow` fields (`frostAlert`, `bestRunningHour`). These test-source errors are outside this deploy request and were not rewritten. This was a debug-device deployment only—not a Play Store, signed production, or hosted-backend release. TalkBack focus/navigation, contrast with Android's contrast tools, large font and display scaling, reduced-motion behavior, and the full offline/permission states remain release-device checks.
