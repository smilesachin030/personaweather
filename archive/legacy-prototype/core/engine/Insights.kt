package com.example.personaweather.core.engine

import com.example.personaweather.core.model.DailySummary
import com.example.personaweather.core.model.WeatherRow
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

sealed interface RunningWindow {
    data class Recommended(val start: LocalDateTime, val end: LocalDateTime, val aqi: Int, val temperature: Double, val uv: Double, val penalty: Double) : RunningWindow
    data object NotRecommended : RunningWindow
}

data class DryingWindow(val start: LocalDateTime, val end: LocalDateTime, val averageHumidity: Double)
enum class TideType { HIGH, LOW }
data class TideEvent(val type: TideType, val time: LocalDateTime, val height: Double)

object Insights {
    fun bestRunningWindow(series: List<WeatherRow>, nowIndex: Int): RunningWindow? {
        val endExclusive = minOf(series.size, nowIndex + Tuning.HOURLY_ITEMS)
        val candidates = (nowIndex until endExclusive).filter { index ->
            series[index].hour in Tuning.RUN_HOURS && index + Tuning.RUN_WINDOW_HOURS <= endExclusive
        }.filter { index ->
            (0 until Tuning.RUN_WINDOW_HOURS).all { offset ->
                series[index + offset].time == series[index].time.plusHours(offset.toLong())
            }
        }
        val best = candidates.minWithOrNull(
            compareBy<Int> { index -> (0 until Tuning.RUN_WINDOW_HOURS).sumOf { penalty(series[index + it]) } }
                .thenBy { series[it].time }
        ) ?: return null
        val penalty = (0 until Tuning.RUN_WINDOW_HOURS).sumOf { penalty(series[best + it]) }
        if (penalty >= Tuning.RUN_NOT_RECOMMENDED_PENALTY) return RunningWindow.NotRecommended
        val first = series[best]
        return RunningWindow.Recommended(
            first.time, series[best + Tuning.RUN_WINDOW_HOURS - 1].time.plusHours(1),
            first.aqiValue, first.temperature, first.uvIndex, penalty
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
        fun bestFor(date: LocalDate, firstAllowedHour: Int): DryingWindow? {
            val hours = series.filter { it.time.toLocalDate() == date && it.hour in Tuning.DRY_HOURS && it.hour >= firstAllowedHour }
            val windows = mutableListOf<DryingWindow>()
            var run = mutableListOf<WeatherRow>()
            fun finishRun() {
                if (run.size >= Tuning.DRY_MIN_WINDOW_HOURS) {
                    windows += DryingWindow(run.first().time, run.last().time.plusHours(1), run.map { it.humidity }.average())
                }
                run = mutableListOf()
            }
            for (row in hours) {
                val consecutive = run.lastOrNull()?.time?.plusHours(1) == row.time
                if (row.rainChancePercent < Tuning.DRY_MAX_RAIN) {
                    if (run.isNotEmpty() && !consecutive) finishRun()
                    run += row
                } else finishRun()
            }
            finishRun()
            return windows.minWithOrNull(
                compareByDescending<DryingWindow> { ChronoUnit.HOURS.between(it.start, it.end) }
                    .thenBy { it.averageHumidity }
                    .thenBy { it.start }
            )
        }

        val todayHour = maxOf(now.hour, Tuning.DRY_HOURS.first)
        return bestFor(now.toLocalDate(), todayHour) ?: bestFor(now.toLocalDate().plusDays(1), Tuning.DRY_HOURS.first)
    }

    fun dryingStatus(window: DryingWindow?, now: LocalDateTime, rainAlert: Boolean): String = when {
        rainAlert -> "Rain soon — bring in"
        window == null -> "No good drying window — dry indoors"
        !now.isBefore(window.start) && now.isBefore(window.end) -> "Good to dry"
        else -> "Wait until ${formatHour(window.start)}"
    }

    fun tideEvents(series: List<WeatherRow>, nowIndex: Int): List<TideEvent> {
        val end = minOf(series.size, nowIndex + Tuning.HOURLY_ITEMS)
        if (end - nowIndex < 3) return emptyList()
        return (maxOf(1, nowIndex + 1) until end - 1).mapNotNull { index ->
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

    fun dailySummaries(series: List<WeatherRow>, startDate: LocalDate, days: Int = Tuning.DAILY_ITEMS): List<DailySummary> =
        (0 until days).mapNotNull { offset ->
            val date = startDate.plusDays(offset.toLong())
            val rows = series.filter { it.time.toLocalDate() == date }
            if (rows.isEmpty()) return@mapNotNull null
            val maxRain = rows.maxOf { it.rainChancePercent }
            val condition = when {
                rows.any { it.condition == "Thunderstorm" } -> "Thunderstorm"
                maxRain >= 60.0 -> "Showers"
                maxRain >= 35.0 -> "Scattered clouds, chance of rain"
                else -> "Sunny"
            }
            val score = rows.filter { it.hour in Tuning.EVENT_HOURS }.sumOf {
                when (it.comfortIndex) { "Poor" -> 3; "Fair" -> 1; else -> 0 }
            }
            DailySummary(
                date.toString(), rows.minOf { it.temperature }, rows.maxOf { it.temperature }, maxRain,
                rows.map { it.aqiValue }.average(), rows.maxOf { it.aqiValue }, rows.sumOf { it.rainfallMm },
                condition, score
            )
        }

    fun packingTips(series: List<WeatherRow>, nowIndex: Int): List<String> {
        val rows = series.drop(nowIndex.coerceAtLeast(0)).take(13)
        if (rows.isEmpty()) return listOf("Pack light — nothing special")
        return buildList {
            if (rows.any { it.uvIndex >= 8.0 }) add("Sunscreen & cap")
            if (rows.maxOf { it.rainChancePercent } >= 40.0) add("Umbrella")
            if (rows.any { it.temperature >= 33.0 }) add("Water bottle, light cottons")
            if (rows.maxOf { it.aqiValue } >= 100) add("N95 mask")
            if (isEmpty()) add("Pack light — nothing special")
        }
    }

    fun formatHour(time: LocalDateTime): String {
        val value = time.toLocalTime()
        val hour = value.hour % 12
        val displayHour = if (hour == 0) 12 else hour
        val suffix = if (value.hour < 12) "AM" else "PM"
        return if (value.minute == 0) "$displayHour $suffix" else "$displayHour:${value.minute.toString().padStart(2, '0')} $suffix"
    }
}