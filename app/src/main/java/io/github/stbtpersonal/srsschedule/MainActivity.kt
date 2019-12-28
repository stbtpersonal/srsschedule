package io.github.stbtpersonal.srsschedule

import android.accounts.Account
import android.app.Activity
import android.content.Intent
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

    private lateinit var keyValueStore: KeyValueStore

    private val scheduleRecyclerViewAdapter = ScheduleRecyclerViewAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.keyValueStore = KeyValueStore(this)

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
        this.signInAndPopulateSchedule()
    }

    private fun signInAndPopulateSchedule() {
        val accountName = this.keyValueStore.accountName
        val accountType = this.keyValueStore.accountType
        if (accountName == null || accountType == null) {
            this.requestSignInAndPopulateSchedule()
            return
        }

        val account = Account(accountName, accountType)
        val credential = Google.buildCredential(this, account)
        this.populateSchedule(credential)
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
                        val credential = Google.buildCredential(this, account)

                        this.keyValueStore.accountName = account.name
                        this.keyValueStore.accountType = account.type

                        this.populateSchedule(credential)
                    }
                    .addOnFailureListener { e ->
                        throw Exception(e)
                    }
            }
        }
    }

    private fun populateSchedule(credential: GoogleAccountCredential) {
        val self = this
        object : Thread() {
            override fun run() {
                val scheduleItems = ScheduleItemBuilder.build(self, credential)
                self.scheduleRecyclerViewAdapter.setScheduleItems(scheduleItems)

                self.runOnUiThread { self.showSchedule() }
            }
        }.start()
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
