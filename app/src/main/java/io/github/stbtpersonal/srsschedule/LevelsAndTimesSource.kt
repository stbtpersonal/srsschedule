package io.github.stbtpersonal.srsschedule

import android.content.Context
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.sheets.v4.Sheets
import java.util.concurrent.CompletableFuture

object LevelsAndTimesSource {
    private const val SHEET_NAME = "SRS"

    private const val COLUMN_INDEX_LEVEL = "F"
    private const val COLUMN_INDEX_TIME = "G"
    private const val START_ROW_INDEX = 2

    fun refresh(context: Context, credential: GoogleAccountCredential): CompletableFuture<*> {
        return CompletableFuture.runAsync {
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

            val keyValueStore = KeyValueStore(context)
            keyValueStore.levelsAndTimes = levelsAndTimes
        }
    }

    fun get(context: Context): Collection<Pair<Int, Long>>? {
        val keyValueStore = KeyValueStore(context)
        return keyValueStore.levelsAndTimes
    }
}