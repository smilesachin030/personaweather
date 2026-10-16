package com.example.personaweather.widget

import android.content.Context
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import androidx.glance.appwidget.updateAll
import java.util.concurrent.TimeUnit

class WidgetRefreshWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = runCatching {
        PersonaWeatherWidget.updateAll(applicationContext)
        Result.success()
    }.getOrElse { Result.retry() }
}

object WidgetRefreshScheduler {
    private const val PERIODIC_WORK_NAME = "persona_weather_widget_periodic_refresh"
    private const val DEBOUNCED_WORK_NAME = "persona_weather_widget_debounced_refresh"

    fun schedulePeriodic(context: Context) {
        if (!hasWidgets(context)) return
        val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(30, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    fun cancelPeriodic(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK_NAME)
        WorkManager.getInstance(context).cancelUniqueWork(DEBOUNCED_WORK_NAME)
    }

    fun enqueueDebounced(context: Context) {
        if (!hasWidgets(context)) return
        val request = OneTimeWorkRequestBuilder<WidgetRefreshWorker>()
            .setInitialDelay(12, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            DEBOUNCED_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    private fun hasWidgets(context: Context): Boolean = AppWidgetManager.getInstance(context)
        .getAppWidgetIds(ComponentName(context, WeatherWidgetReceiver::class.java))
        .isNotEmpty()
}