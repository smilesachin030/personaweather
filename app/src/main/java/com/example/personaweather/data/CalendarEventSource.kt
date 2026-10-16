package com.example.personaweather.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant

private const val CALENDAR_QUERY_WINDOW_DAYS = 30L

data class CalendarEvent(
    val id: Long,
    val title: String,
    val start: Instant,
    val end: Instant?,
    val location: String?,
    val calendarName: String?,
    val isAllDay: Boolean,
) {
    val instanceKey: String
        get() = "${id}_${start.toEpochMilli()}"
}

interface CalendarEventSource {
    fun hasPermission(context: Context): Boolean
    suspend fun upcomingEvents(start: Instant, end: Instant): List<CalendarEvent>
}

class DeviceCalendarEventSource(
    private val context: Context,
) : CalendarEventSource {
    override fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    override suspend fun upcomingEvents(start: Instant, end: Instant): List<CalendarEvent> = withContext(Dispatchers.IO) {
        if (!hasPermission(context)) return@withContext emptyList()
        val startMillis = start.toEpochMilli()
        val endMillis = end.toEpochMilli()
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
            .appendPath(startMillis.toString())
            .appendPath(endMillis.toString())
            .build()

        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.EVENT_LOCATION,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.CALENDAR_DISPLAY_NAME,
        )

        val events = mutableListOf<CalendarEvent>()
        val cursor = context.contentResolver.query(
            uri,
            projection,
            null,
            null,
            "${CalendarContract.Instances.BEGIN} ASC",
        ) ?: return@withContext emptyList()

        try {
            val eventIdColumn = cursor.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_ID)
            val titleColumn = cursor.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
            val beginColumn = cursor.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
            val endColumn = cursor.getColumnIndexOrThrow(CalendarContract.Instances.END)
            val locationColumn = cursor.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_LOCATION)
            val allDayColumn = cursor.getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY)
            val calendarNameColumn = cursor.getColumnIndexOrThrow(CalendarContract.Instances.CALENDAR_DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val startEpoch = cursor.getLong(beginColumn)
                val endEpoch = cursor.getLong(endColumn)
                val title = cursor.getString(titleColumn)?.takeIf { it.isNotBlank() } ?: "Untitled event"
                val location = cursor.getString(locationColumn)?.takeIf { it.isNotBlank() }
                val isAllDay = cursor.getInt(allDayColumn) == 1
                val calendarName = cursor.getString(calendarNameColumn)?.takeIf { it.isNotBlank() }

                events += CalendarEvent(
                    id = cursor.getLong(eventIdColumn),
                    title = title,
                    start = Instant.ofEpochMilli(startEpoch),
                    end = if (endEpoch > 0L) Instant.ofEpochMilli(endEpoch) else null,
                    location = location,
                    calendarName = calendarName,
                    isAllDay = isAllDay,
                )
            }
        } finally {
            cursor.close()
        }
        events
    }

    companion object {
        const val DEFAULT_LOOKAHEAD_DAYS = CALENDAR_QUERY_WINDOW_DAYS
    }
}
