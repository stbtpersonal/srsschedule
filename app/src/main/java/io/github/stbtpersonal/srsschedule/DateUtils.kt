package io.github.stbtpersonal.srsschedule

import java.util.*

object DateUtils {
    val epoch = Calendar.getInstance()
    val later = Calendar.getInstance()

    init {
        this.epoch.timeInMillis = 0
        this.later.timeInMillis = Long.MAX_VALUE
    }

    fun thisHour(): Calendar {
        val now = Calendar.getInstance()
        now.set(Calendar.MINUTE, 0)
        now.set(Calendar.SECOND, 0)
        now.set(Calendar.MILLISECOND, 0)
        return now
    }

    fun toHour(millis: Long): Calendar {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = millis
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar
    }
}