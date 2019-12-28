package io.github.stbtpersonal.srsschedule

import android.content.Context
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.sheets.v4.Sheets
import java.util.*

object ScheduleItemBuilder {
    private const val SHEET_NAME = "SRS"

    private const val COLUMN_INDEX_LEVEL = "F"
    private const val COLUMN_INDEX_TIME = "G"
    private const val START_ROW_INDEX = 2

    private val LEVEL_DURATIONS_IN_HOURS = listOf(0, 4, 8, 23, 47, 167, 335, 719, 2879)

    fun build(
        context: Context,
        credential: GoogleAccountCredential
    ): Collection<ScheduleItem> {
        val levelsAndTimes = this.fetchLevelsAndTimes(context, credential)
        val now = DateUtils.thisHour()

        val in24Hours = DateUtils.thisHour()
        in24Hours.add(Calendar.HOUR_OF_DAY, 24)

        val scheduleItems = mutableMapOf<Calendar, Int>()
        for ((level, time) in levelsAndTimes) {
            if (level >= LEVEL_DURATIONS_IN_HOURS.size) {
                continue
            }

            var reviewTime = DateUtils.toHour(time)
            reviewTime.add(Calendar.HOUR_OF_DAY, LEVEL_DURATIONS_IN_HOURS[level])

            if (reviewTime.before(now)) {
                reviewTime = DateUtils.epoch
            }
            if (reviewTime.after(in24Hours)) {
                reviewTime = DateUtils.later
            }

            if (!scheduleItems.containsKey(reviewTime)) {
                scheduleItems[reviewTime] = 0
            }
            scheduleItems[reviewTime] = scheduleItems[reviewTime]!! + 1
        }

        return scheduleItems
            .map { (time, amount) -> ScheduleItem(time, amount) }
            .sortedBy { it.time }
    }

    fun fetchLevelsAndTimes(
        context: Context,
        credential: GoogleAccountCredential
    ): Collection<Pair<Int, Long>> {
        val levelsAndTimes = mutableListOf<Pair<Int, Long>>()

        val jsonFactory = JacksonFactory.getDefaultInstance()
        val httpTransport = AndroidHttp.newCompatibleTransport()

        val drive = Drive.Builder(httpTransport, jsonFactory, credential)
            .setApplicationName(context.getString(R.string.app_name))
            .build()
        val sheets = Sheets.Builder(httpTransport, jsonFactory, credential)
            .setApplicationName(context.getString(R.string.app_name))
            .build()

        val filesResult = drive
            .files()
            .list()
            .setFields("files(id, name, webViewLink)")
            .setQ("mimeType = 'application/vnd.google-apps.spreadsheet' and name contains '[\"$SHEET_NAME\"]'")
            .execute()
        val files = filesResult.files
        for (file in files) {
            val sheetsResult = sheets
                .spreadsheets()
                .values()
                .get(
                    file.id,
                    "$SHEET_NAME!$COLUMN_INDEX_LEVEL$START_ROW_INDEX:$COLUMN_INDEX_TIME"
                )
                .execute()
            val values = sheetsResult.getValues()
            for (row in values) {
                if (row.isNotEmpty()) {
                    val level = (row[0] as String).toInt()
                    val time = (row[1] as String).toLong()
                    levelsAndTimes.add(Pair(level, time))
                }
            }
        }

        return levelsAndTimes
    }
}