package com.example.personaweather.core.engine

import com.example.personaweather.core.model.DailySummary
import com.example.personaweather.core.model.WeatherRow
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

sealed interface RunningWindow {
    data class Recommended(
        val start: LocalDateTime,
        val end: LocalDateTime,
        val aqi: Int,
        val temperature: Double,
        val uv: Double,
        val penalty: Double,
    ) : RunningWindow

    data object NotRecommended : RunningWindow
    data object NoClearBestWindow : RunningWindow
}

enum class RainGearGuidance { NONE, RAINCOAT, UMBRELLA }

data class CommuteOutlook(
    val peak: WeatherRow,
    val peakTime: LocalDateTime?,
    val rainGearGuidance: RainGearGuidance,
    val hasRainProbabilityConflict: Boolean,
)

data class DryingWindow(
    val start: LocalDateTime,
    val end: LocalDateTime,
    val averageHumidity: Double,
)

data class RoadOutlook(
    val rainIndicated: Boolean,
    val rainChancePercent: Double,
    val hasConflictingRainChance: Boolean,
    val minimumVisibilityKm: Double,
    val fogExpected: Boolean,
)

enum class TideType { HIGH, LOW }

data class TideEvent(val type: TideType, val time: LocalDateTime, val height: Double)

object Insights {
    fun indicatesRain(row: WeatherRow): Boolean =
        row.rainChancePercent >= Tuning.RAINCOAT_CHANCE ||
            row.rainfallMm > 0.0 ||
            row.condition.contains("rain", ignoreCase = true) ||
            row.condition.contains("shower", ignoreCase = true) ||
            row.condition.contains("storm", ignoreCase = true)

    fun roadOutlook(series: List<WeatherRow>, nowIndex: Int): RoadOutlook? {
        val rows = series.drop(nowIndex.coerceAtLeast(0)).take(Tuning.COMMUTE_LOOKAHEAD_HOURS + 1)
        if (rows.isEmpty()) return null
        val peakRainChance = rows.maxOf { it.rainChancePercent }
        val rainIndicated = rows.any(::indicatesRain)
        return RoadOutlook(
            rainIndicated = rainIndicated,
            rainChancePercent = peakRainChance,
            hasConflictingRainChance = rainIndicated && peakRainChance <= 0.0,
            minimumVisibilityKm = rows.minOf { it.visibilityKm },
            fogExpected = rows.any { it.fogFlag == 1 },
        )
    }

    fun bestRunningWindow(series: List<WeatherRow>, nowIndex: Int): RunningWindow? {
        val start = nowIndex.coerceAtLeast(0)
        val end = minOf(series.size, start + Tuning.HOURLY_ITEMS)
        val candidates = (start until end).filter { index ->
            val windowEnd = index + Tuning.RUN_WINDOW_HOURS
            if (series[index].hour !in Tuning.RUN_HOURS || windowEnd > end) {
                false
            } else {
                (0 until Tuning.RUN_WINDOW_HOURS).all { offset ->
                    series.getOrNull(index + offset)?.let { row ->
                        row.time == series[index].time.plusHours(offset.toLong()) &&
                            row.hour in Tuning.RUN_HOURS
                    } == true
                }
            }
        }
        if (candidates.isEmpty()) return null
        val suitable = candidates.filter { index ->
            (0 until Tuning.RUN_WINDOW_HOURS).none { offset ->
                val row = series[index + offset]
                row.heatAlert == 1 ||
                    row.feelsLike >= Tuning.HIGH_FEELS_LIKE ||
                    row.aqiValue >= Tuning.UNHEALTHY_AQI ||
                    row.uvIndex >= Tuning.VERY_HIGH_UV ||
                    indicatesRain(row)
            }
        }
        if (suitable.isEmpty()) return RunningWindow.NotRecommended
        val ranked = suitable.map { index ->
            index to (0 until Tuning.RUN_WINDOW_HOURS).sumOf { offset -> penalty(series[index + offset]) }
        }
            .sortedBy { it.second }
        val best = ranked.first().first
        val penalty = ranked.first().second
        if (penalty >= Tuning.RUN_NOT_RECOMMENDED_PENALTY) return RunningWindow.NotRecommended
        if (ranked.count { it.second == penalty } > 1) return RunningWindow.NoClearBestWindow
        val first = series[best]
        return RunningWindow.Recommended(
            start = first.time,
            end = series[best + Tuning.RUN_WINDOW_HOURS - 1].time.plusHours(1),
            aqi = first.aqiValue,
            temperature = first.temperature,
            uv = first.uvIndex,
            penalty = penalty,
        )
    }

    fun commuteOutlook(
        series: List<WeatherRow>,
        date: LocalDate,
        startHour: Int,
        endHour: Int,
    ): CommuteOutlook? {
        val rows = series.filter {
            it.time.toLocalDate() == date && it.hour in startHour until endHour
        }
        val peak = rows.maxByOrNull { it.rainChancePercent } ?: return null
        val peakTime = rows.filter { it.rainChancePercent == peak.rainChancePercent }
            .singleOrNull()
            ?.time
        val rainIndicated = rows.any(::indicatesRain)
        val guidance = when {
            peak.rainChancePercent >= Tuning.UMBRELLA_CHANCE -> RainGearGuidance.UMBRELLA
            peak.rainChancePercent >= Tuning.RAINCOAT_CHANCE || rainIndicated -> RainGearGuidance.RAINCOAT
            else -> RainGearGuidance.NONE
        }
        return CommuteOutlook(
            peak = peak,
            peakTime = peakTime,
            rainGearGuidance = guidance,
            hasRainProbabilityConflict = rainIndicated && peak.rainChancePercent <= 0.0,
        )
    }

    private fun penalty(row: WeatherRow): Double =
        0.5 * maxOf(0.0, row.temperature - 28.0) +
            0.05 * maxOf(0, row.aqiValue - 50) +
            0.5 * row.uvIndex +
            0.05 * row.rainChancePercent +
            0.1 * maxOf(0.0, row.windSpeed - 15.0)

    fun dryingWindow(series: List<WeatherRow>, nowIndex: Int): DryingWindow? {
        val now = series.getOrNull(nowIndex)?.time ?: return null

        fun bestFor(date: LocalDate, earliestHour: Int): DryingWindow? {
            val rows = series.filter {
                it.time.toLocalDate() == date && it.hour in Tuning.DRY_HOURS && it.hour >= earliestHour
            }
            val windows = mutableListOf<DryingWindow>()
            var run = mutableListOf<WeatherRow>()

            fun finishRun() {
                if (run.size >= Tuning.DRY_MIN_WINDOW_HOURS) {
                    windows += DryingWindow(
                        run.first().time,
                        run.last().time.plusHours(1),
                        run.map { it.humidity }.average(),
                    )
                }
                run = mutableListOf()
            }

            for (row in rows) {
                val consecutive = run.lastOrNull()?.time?.plusHours(1) == row.time
                if (row.rainChancePercent < Tuning.DRY_MAX_RAIN) {
                    if (run.isNotEmpty() && !consecutive) finishRun()
                    run += row
                } else {
                    finishRun()
                }
            }
            finishRun()
            return windows.minWithOrNull(
                compareByDescending<DryingWindow> { ChronoUnit.HOURS.between(it.start, it.end) }
                    .thenBy { it.averageHumidity }
                    .thenBy { it.start },
            )
        }

        val firstHour = maxOf(now.hour, Tuning.DRY_HOURS.first)
        return bestFor(now.toLocalDate(), firstHour)
            ?: bestFor(now.toLocalDate().plusDays(1), Tuning.DRY_HOURS.first)
    }

    fun dryingStatus(window: DryingWindow?, now: LocalDateTime, rainAlert: Boolean): String = when {
        rainAlert -> "Rain soon — bring in"
        window == null -> "No good drying window — dry indoors"
        !now.isBefore(window.start) && now.isBefore(window.end) -> "Good to dry"
        else -> "Wait until ${formatHour(window.start)}"
    }

    fun tideEvents(series: List<WeatherRow>, nowIndex: Int): List<TideEvent> {
        val start = nowIndex.coerceAtLeast(0)
        val end = minOf(series.size, start + Tuning.HOURLY_ITEMS)
        if (end - start < 3) return emptyList()
        return (maxOf(1, start + 1) until end - 1).mapNotNull { index ->
            val previous = series[index - 1].tideHeightM ?: return@mapNotNull null
            val current = series[index].tideHeightM ?: return@mapNotNull null
            val next = series[index + 1].tideHeightM ?: return@mapNotNull null
            when {
                current > previous && current >= next -> TideEvent(TideType.HIGH, series[index].time, current)
                current < previous && current <= next -> TideEvent(TideType.LOW, series[index].time, current)
                else -> null
            }
        }
    }

    fun rainSentence(series: List<WeatherRow>, nowIndex: Int): String {
        val rows = series.drop(nowIndex.coerceAtLeast(0)).take(Tuning.HOURLY_ITEMS)
        if (rows.isEmpty()) return "No rain expected in the next 24 hours"
        if (rows.first().rainChancePercent >= Tuning.RAIN_LIKELY) {
            val easing = rows.drop(1).firstOrNull { it.rainChancePercent < Tuning.RAIN_EASING }
            return if (easing == null) "Rain now" else "Rain now, easing around ${formatHour(easing.time)}"
        }
        val expected = rows.firstOrNull { it.rainChancePercent >= Tuning.RAIN_LIKELY }
            ?: return "No rain expected in the next 24 hours"
        return "Rain expected around ${formatHour(expected.time)} (${expected.rainChancePercent.toInt()}%)"
    }

    fun dailySummaries(
        series: List<WeatherRow>,
        startDate: LocalDate,
        dailyRainChanceByDate: Map<LocalDate, Double> = emptyMap(),
        days: Int = Tuning.DAILY_ITEMS,
    ): List<DailySummary> = (0 until days).mapNotNull { offset ->
        val date = startDate.plusDays(offset.toLong())
        val rows = series.filter { it.time.toLocalDate() == date }
        if (rows.isEmpty()) return@mapNotNull null
        val maxRain = dailyRainChanceByDate[date] ?: rows.maxOf { it.rainChancePercent }
        val dominantCondition = when {
            rows.any { it.condition == "Thunderstorm" } -> "Thunderstorm"
            maxRain >= 60.0 -> "Showers"
            maxRain >= 35.0 -> "Scattered clouds, chance of rain"
            else -> "Sunny"
        }
        val eventScore = rows.filter { it.hour in Tuning.EVENT_HOURS }.fold(0) { score, row ->
            score + when (row.comfortIndex) {
                "Poor" -> 3
                "Fair" -> 1
                else -> 0
            }
        }
        DailySummary(
            date = date.toString(),
            minTemp = rows.minOf { it.temperature },
            maxTemp = rows.maxOf { it.temperature },
            maxRain = maxRain,
            avgAqi = rows.map { it.aqiValue }.average(),
            maxAqi = rows.maxOf { it.aqiValue },
            totalRainMm = rows.sumOf { it.rainfallMm },
            dominantCondition = dominantCondition,
            eventScore = eventScore,
        )
    }

    fun packingTips(series: List<WeatherRow>, nowIndex: Int): List<String> {
        val rows = series.drop(nowIndex.coerceAtLeast(0)).take(Tuning.HOURLY_ITEMS)
        if (rows.isEmpty()) return emptyList()
        return buildList {
            if (rows.any { it.hour in Tuning.DAYLIGHT_HOURS && it.uvIndex >= 3.0 }) {
                add("sun_protection")
            }
            val peakRainChance = rows.maxOf { it.rainChancePercent }
            if (peakRainChance >= Tuning.UMBRELLA_CHANCE) {
                add("umbrella")
            } else if (peakRainChance >= Tuning.RAINCOAT_CHANCE) {
                add("raincoat")
            }
            if (rows.any { it.feelsLike >= Tuning.HIGH_FEELS_LIKE }) {
                add("water")
            }
            if (rows.any { it.aqiValue >= Tuning.SENSITIVE_AQI }) {
                add("air_quality")
            }
        }
    }

    fun formatHour(time: LocalDateTime): String {
        val hour = time.hour % 12
        val displayHour = if (hour == 0) 12 else hour
        val suffix = if (time.hour < 12) "AM" else "PM"
        return if (time.minute == 0) "$displayHour $suffix"
        else "$displayHour:${time.minute.toString().padStart(2, '0')} $suffix"
    }
}