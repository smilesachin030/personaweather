package com.example.personaweather.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.personaweather.MainActivity
import com.example.personaweather.core.model.Advisory
import com.example.personaweather.core.model.Location
import com.example.personaweather.core.model.Severity
import com.example.personaweather.data.UserPrefs
import com.example.personaweather.ui.localization.AppStrings
import java.time.LocalDateTime

class NotificationHelper(context: Context) : AlertNotifier {
    private val appContext = context.applicationContext
    private val history = appContext.getSharedPreferences(HISTORY_PREFS, Context.MODE_PRIVATE)

    init {
        createChannels()
    }

    fun hasPermission(): Boolean = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    override fun notifyAlerts(
        location: Location,
        simTime: LocalDateTime,
        advisories: List<Advisory>,
        prefs: UserPrefs,
    ) {
        if (!prefs.notificationsEnabled || !hasPermission()) return
        createChannels(prefs.language)
        advisories.filter { it.severity == Severity.ALERT }.forEach { advisory ->
            val isClothesAlert = advisory.ruleId == CLOTHES_ALERT_RULE
            if (isClothesAlert && !prefs.clothAlarmEnabled) return@forEach
            val key = "${location.id}|${simTime.toLocalDate()}|${advisory.ruleId}"
            if (history.getBoolean(key, false)) return@forEach
            val localizedMessage = when (prefs.language) {
                "ta" -> advisory.messageTa ?: advisory.message
                "hi" -> advisory.messageHi ?: advisory.message
                else -> advisory.message
            }
            val localizedFieldTitle = when (prefs.language) {
                "ta" -> fieldTitleTa(advisory.field)
                "hi" -> fieldTitleHi(advisory.field)
                else -> fieldTitle(advisory.field)
            }
            val posted = post(
                id = key.hashCode() and Int.MAX_VALUE,
                channelId = if (isClothesAlert) CHANNEL_CLOTHES else CHANNEL_ADVISORIES,
                title = "$localizedFieldTitle ${AppStrings.forCode(prefs.language).text("alert", "alert")} · ${AppStrings.forCode(prefs.language).locationName(location.name)}",
                message = localizedMessage,
            )
            if (posted) history.edit().putBoolean(key, true).apply()
        }
    }

    private fun post(id: Int, channelId: String, title: String, message: String): Boolean {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            appContext,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(appContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        return try {
            NotificationManagerCompat.from(appContext).notify(id, notification)
            true
        } catch (failure: SecurityException) {
            Log.w(TAG, "Notification permission was revoked before the alert could be posted.", failure)
            false
        }
    }

    private fun createChannels(language: String = "en") {
        val strings = AppStrings.forCode(language)
        val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ADVISORIES,
                strings.text("notification_channel_alerts", "Weather alerts"),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = strings.text(
                    "notification_channel_alerts_description",
                    "Important weather advisories for your selected location and personas.",
                )
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_CLOTHES,
                strings.text("notification_channel_clothes", "Clothes drying alarm"),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = strings.text(
                    "notification_channel_clothes_description",
                    "Alarm sound when rain may reach clothes left outside.",
                )
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
            },
        )
    }

    private fun fieldTitle(field: String): String = when (field) {
        "aqi_value" -> "Air quality"
        "rain_chance_percent" -> "Rain"
        "visibility_km", "fog_flag" -> "Visibility"
        "heat_alert" -> "Heat"
        else -> field.replace('_', ' ').replaceFirstChar { it.uppercase() }
    }

    private fun fieldTitleHi(field: String): String = when (field) {
        "aqi_value" -> "वायु गुणवत्ता"
        "rain_chance_percent" -> "बारिश"
        "visibility_km", "fog_flag" -> "दृश्यता"
        "heat_alert" -> "गर्मी"
        "wave_height_m" -> "समुद्र की स्थिति"
        "uv_index" -> "UV सूचकांक"
        "wind_speed" -> "हवा"
        "soil_moisture_percent" -> "मिट्टी की नमी"
        "pollen_grass", "pollen_tree", "pollen_ragweed" -> "पराग"
        else -> field.replace('_', ' ').replaceFirstChar { it.uppercase() }
    }

    private fun fieldTitleTa(field: String): String = when (field) {
        "aqi_value" -> "காற்றின் தரம்"
        "rain_chance_percent" -> "மழை"
        "visibility_km", "fog_flag" -> "பார்வை"
        "heat_alert" -> "வெப்பம்"
        else -> field.replace('_', ' ').replaceFirstChar { it.uppercase() }
    }

    companion object {
        const val CHANNEL_ADVISORIES = "advisories"
        const val CHANNEL_CLOTHES = "clothes_alarm"
        const val CLOTHES_ALERT_RULE = "pf_cloth_alert"
        private const val TAG = "NotificationHelper"
        private const val HISTORY_PREFS = "persona_weather_notification_history"
    }
}