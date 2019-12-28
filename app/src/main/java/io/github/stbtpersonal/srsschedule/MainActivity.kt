package io.github.stbtpersonal.srsschedule

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*


class MainActivity : Activity() {
    companion object {
        private const val GOOGLE_SIGN_IN_REQUEST = 1
        private const val SHEET_NAME = "SRS"

        private const val COLUMN_INDEX_LEVEL = "F"
        private const val COLUMN_INDEX_TIME = "G"
        private const val START_ROW_INDEX = 2
    }

    private val scheduleRecyclerViewAdapter = ScheduleRecyclerViewAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.setContentView(R.layout.activity_main)

        this.scheduleRecyclerView.layoutManager = LinearLayoutManager(
            this,
            RecyclerView.VERTICAL,
            false
        )
        this.scheduleRecyclerView.adapter = this.scheduleRecyclerViewAdapter

        NotificationScheduler.scheduleNotifications(this)

        this.refresh()
    }

    private fun refresh() {
        this.hideSchedule()
        this.requestSignIn()
    }

    private fun requestSignIn() {
        val signInOptions = GoogleSignInOptions
            .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(
                Scope(SheetsScopes.SPREADSHEETS),
                Scope(DriveScopes.DRIVE_METADATA_READONLY)
            )
            .build()
        val client = GoogleSignIn.getClient(this, signInOptions)

        this.startActivityForResult(client.signInIntent, GOOGLE_SIGN_IN_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == GOOGLE_SIGN_IN_REQUEST) {
            if (resultCode == RESULT_OK) {
                GoogleSignIn
                    .getSignedInAccountFromIntent(data)
                    .addOnSuccessListener { account ->
                        val scopes = listOf(
                            SheetsScopes.SPREADSHEETS,
                            DriveScopes.DRIVE_METADATA_READONLY
                        )
                        val credential = GoogleAccountCredential.usingOAuth2(this, scopes)
                        credential.selectedAccount = account.account

                        object : Thread() {
                            override fun run() {
                                this@MainActivity.populateSchedule(credential)
                            }
                        }.start()
                    }
                    .addOnFailureListener { e ->
                        throw Exception(e)
                    }
            }
        }
    }

    private fun populateSchedule(credential: GoogleAccountCredential) {
        val levelsAndTimes = this.fetchLevelsAndTimes(credential)
        val scheduleItems = this.buildScheduleItems(levelsAndTimes)
        this.scheduleRecyclerViewAdapter.setScheduleItems(scheduleItems)

        this.runOnUiThread { this.showSchedule() }
    }

    private fun fetchLevelsAndTimes(credential: GoogleAccountCredential): Collection<Pair<Int, Long>> {
        val levelsAndTimes = mutableListOf<Pair<Int, Long>>()

        val jsonFactory = JacksonFactory.getDefaultInstance()
        val httpTransport = AndroidHttp.newCompatibleTransport()

        val drive = Drive.Builder(httpTransport, jsonFactory, credential)
            .setApplicationName(getString(R.string.app_name))
            .build()
        val sheets = Sheets.Builder(httpTransport, jsonFactory, credential)
            .setApplicationName(getString(R.string.app_name))
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

    private fun buildScheduleItems(levelsAndTimes: Collection<Pair<Int, Long>>): Collection<ScheduleItem> {
        val scheduledItems = listOf(ScheduleItem(Date(), 100))

        return scheduledItems
    }

    private fun hideSchedule() {
        this.spinner.visibility = View.VISIBLE
        this.scheduleContainer.visibility = View.GONE
    }

    private fun showSchedule() {
        this.spinner.visibility = View.GONE
        this.scheduleContainer.visibility = View.VISIBLE
    }
}
