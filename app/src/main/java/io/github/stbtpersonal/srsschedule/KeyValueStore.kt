package io.github.stbtpersonal.srsschedule

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration

class KeyValueStore(private val context: Context) {
    private val sharedPreferences =
        this.context.getSharedPreferences("key-value-store", Context.MODE_PRIVATE)
    private val json = Json(JsonConfiguration.Stable)

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

    @Serializable
    data class SerializedLevelsAndTimes(val levelsAndTimes: Collection<Pair<Int, Long>>)

    var levelsAndTimes: Collection<Pair<Int, Long>>?
        get() {
            val serialized =
                this.sharedPreferences.getString("levels-and-times", null) ?: return null
            val wrapped = this.json.parse(SerializedLevelsAndTimes.serializer(), serialized)
            return wrapped.levelsAndTimes
        }
        set(value) = with(this.sharedPreferences.edit()) {
            val wrapped = SerializedLevelsAndTimes(value!!)
            val serialized =
                this@KeyValueStore.json.stringify(SerializedLevelsAndTimes.serializer(), wrapped)
            putString("levels-and-times", serialized)
            apply()
        }
}