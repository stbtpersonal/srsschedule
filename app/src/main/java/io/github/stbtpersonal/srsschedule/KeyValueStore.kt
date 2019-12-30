package io.github.stbtpersonal.srsschedule

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration

class KeyValueStore(private val context: Context) {
    private val sharedPreferences =
        this.context.getSharedPreferences("key-value-store", Context.MODE_PRIVATE)
    private val json = Json(JsonConfiguration.Stable)


    private val accountNameKey = "account-name"
    var accountName: String?
        get() = this.sharedPreferences.getString(accountNameKey, null)
        set(value) = with(this.sharedPreferences.edit()) {
            putString(accountNameKey, value)
            apply()
        }

    private val accountTypeKey = "account-type"
    var accountType: String?
        get() = this.sharedPreferences.getString(accountTypeKey, null)
        set(value) = with(this.sharedPreferences.edit()) {
            putString(accountTypeKey, value)
            apply()
        }

    @Serializable
    data class SerializedLevelsAndTimes(val levelsAndTimes: Collection<Pair<Int, Long>>)

    private val levelsAndTimesKey = "levels-and-times"
    var levelsAndTimes: Collection<Pair<Int, Long>>?
        get() {
            val serialized =
                this.sharedPreferences.getString(levelsAndTimesKey, null) ?: return null
            val wrapped = this.json.parse(SerializedLevelsAndTimes.serializer(), serialized)
            return wrapped.levelsAndTimes
        }
        set(value) = with(this.sharedPreferences.edit()) {
            val wrapped = SerializedLevelsAndTimes(value!!)
            val serialized =
                this@KeyValueStore.json.stringify(SerializedLevelsAndTimes.serializer(), wrapped)
            putString(levelsAndTimesKey, serialized)
            apply()
        }

    fun containsLevelsAndTimes(): Boolean {
        return this.sharedPreferences.contains(levelsAndTimesKey)
    }
}