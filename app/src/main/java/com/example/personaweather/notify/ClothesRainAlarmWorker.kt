package com.example.personaweather.notify

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.personaweather.PersonaWeatherApp
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

class ClothesRainAlarmWorker(context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val app = applicationContext as? PersonaWeatherApp
        if (app == null) {
            Log.e(TAG, "Application is not PersonaWeatherApp; cannot check rain alarm.")
            return Result.failure()
        }
        val prefs = app.container.userPrefs
        if (!prefs.onboarded || !prefs.notificationsEnabled || !prefs.clothAlarmEnabled) return Result.success()
        if (!app.notificationHelper.hasPermission()) return Result.success()

        return try {
            val locations = (app.container.weatherDataSource.locations() + prefs.rememberedLocations)
                .distinctBy { it.id }
            val location = locations.firstOrNull { it.id == prefs.locationId }
                ?: prefs.rememberedLocation(prefs.locationId)
                ?: error("Selected location is unavailable for the clothes rain alarm.")
            val series = app.container.weatherDataSource.series(location)
            if (series.isEmpty()) return Result.retry()

            val zone = location.timeZone?.let(ZoneId::of) ?: ZoneId.of("Asia/Kolkata")
            val now = LocalDateTime.now(zone).withMinute(0).withSecond(0).withNano(0)
            val nowIndex = series.indices.minByOrNull { index ->
                Duration.between(series[index].time, now).abs()
            } ?: return Result.retry()
            val alarm = ClothesRainAlarm.evaluate(
                series = series,
                nowIndex = nowIndex,
                catalog = app.container.rulesCatalog,
                isCoastal = location.isCoastal,
            )
            app.notificationHelper.notifyAlerts(location, now, alarm, prefs)
            Result.success()
        } catch (cancellation: kotlinx.coroutines.CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            Log.e(TAG, "Unable to check the clothes rain alarm.", failure)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "ClothesRainAlarm"
        private const val PERIODIC_WORK_NAME = "persona_weather_clothes_rain_alarm"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ClothesRainAlarmWorker>(30, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK_NAME)
        }

        fun reconcile(context: Context, enabled: Boolean) {
            if (enabled) schedule(context) else cancel(context)
        }
    }
}
