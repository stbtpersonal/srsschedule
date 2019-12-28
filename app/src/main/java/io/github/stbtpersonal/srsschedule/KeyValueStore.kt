package io.github.stbtpersonal.srsschedule

import android.content.Context

class KeyValueStore(private val context: Context) {
    private val sharedPreferences = this.context.getSharedPreferences("key-value-store", Context.MODE_PRIVATE)

    var accountName: String?
        get() = this.sharedPreferences.getString("account-name", null)
        set(value) = with(this.sharedPreferences.edit()) {
            putString("account-name", value)
            apply()
        }

    var accountType: String?
        get() = this.sharedPreferences.getString("account-type", null)
        set(value) = with(this.sharedPreferences.edit()) {
            putString("account-type", value)
            apply()
        }
}