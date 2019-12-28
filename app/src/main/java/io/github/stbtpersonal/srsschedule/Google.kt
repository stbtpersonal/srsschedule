package io.github.stbtpersonal.srsschedule

import android.accounts.Account
import android.content.Context
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.services.drive.DriveScopes
import com.google.api.services.sheets.v4.SheetsScopes

object Google {
    fun buildCredential(context: Context, account: Account): GoogleAccountCredential {
        val scopes = listOf(
            SheetsScopes.SPREADSHEETS,
            DriveScopes.DRIVE_METADATA_READONLY
        )
        val credential = GoogleAccountCredential.usingOAuth2(context, scopes)
        credential.selectedAccount = account
        return credential
    }
}