package io.github.stbtpersonal.srsschedule

import android.accounts.Account
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.services.drive.DriveScopes
import com.google.api.services.sheets.v4.SheetsScopes
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : Activity() {
    companion object {
        private const val GOOGLE_SIGN_IN_REQUEST = 1
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

        this.refreshButton.setOnClickListener { this.refresh() }
        this.launchSrsButton.setOnClickListener { this.launchSrs() }

        NotificationScheduler.scheduleNotifications(this)

        val keyValueStore = KeyValueStore(this)
        if (!keyValueStore.containsLevelsAndTimes()) {
            this.refresh()
            return
        }
        this.populateSchedule()
    }

    private fun refresh() {
        this.hideSchedule()
        this.signInAndPopulateSchedule()
    }

    private fun signInAndPopulateSchedule() {
        val keyValueStore = KeyValueStore(this)
        val accountName = keyValueStore.accountName
        val accountType = keyValueStore.accountType
        if (accountName == null || accountType == null) {
            this.requestSignInAndPopulateSchedule()
            return
        }

        val account = Account(accountName, accountType)
        val credential = this.buildCredential(this, account)
        LevelsAndTimesSource.refresh(this, credential)
            .thenRun { this.runOnUiThread(this::populateSchedule) }
    }

    private fun buildCredential(context: Context, account: Account): GoogleAccountCredential {
        val scopes = listOf(
            SheetsScopes.SPREADSHEETS,
            DriveScopes.DRIVE_METADATA_READONLY
        )
        val credential = GoogleAccountCredential.usingOAuth2(context, scopes)
        credential.selectedAccount = account
        return credential
    }

    private fun requestSignInAndPopulateSchedule() {
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
                    .addOnSuccessListener { result ->
                        val account = result.account!!
                        val credential = this.buildCredential(this, account)

                        val keyValueStore = KeyValueStore(this)
                        keyValueStore.accountName = account.name
                        keyValueStore.accountType = account.type

                        LevelsAndTimesSource.refresh(this, credential)
                            .thenRun { this.runOnUiThread(this::populateSchedule) }
                    }
                    .addOnFailureListener { e ->
                        throw Exception(e)
                    }
            }
        }
    }

    private fun populateSchedule() {
        val levelsAndTimes = LevelsAndTimesSource.get(this)!!
        val scheduleItems = ScheduleItemBuilder.build(levelsAndTimes)
        this.scheduleRecyclerViewAdapter.setScheduleItems(scheduleItems)

        NotificationScheduler.notifyIfRequired(this)

        this.showSchedule()
    }

    private fun hideSchedule() {
        this.spinner.visibility = View.VISIBLE
        this.scheduleContainer.visibility = View.GONE
    }

    private fun showSchedule() {
        this.spinner.visibility = View.GONE
        this.scheduleContainer.visibility = View.VISIBLE
    }

    private fun launchSrs() {
        val url = "https://stbtpersonal.github.io/srs/"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        this.startActivity(intent)
    }
}
