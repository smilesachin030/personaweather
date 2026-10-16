package com.example.personaweather

import android.app.Application
import com.example.personaweather.data.AppContainer
import com.example.personaweather.notify.ClothesRainAlarmWorker
import com.example.personaweather.notify.NotificationHelper

class PersonaWeatherApp : Application() {
    lateinit var container: AppContainer
        private set
    lateinit var notificationHelper: NotificationHelper
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        notificationHelper = NotificationHelper(this)
        ClothesRainAlarmWorker.reconcile(
            this,
            enabled = container.userPrefs.onboarded &&
                container.userPrefs.notificationsEnabled &&
                container.userPrefs.clothAlarmEnabled &&
                notificationHelper.hasPermission(),
        )
    }
}