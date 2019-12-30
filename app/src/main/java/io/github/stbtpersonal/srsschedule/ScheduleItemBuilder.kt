package io.github.stbtpersonal.srsschedule

import java.util.*

object ScheduleItemBuilder {
    private val LEVEL_DURATIONS_IN_HOURS = listOf(0, 4, 8, 23, 47, 167, 335, 719, 2879)

    fun build(levelsAndTimes: Collection<Pair<Int, Long>>): Collection<ScheduleItem> {
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

            if (reviewTime.before(now) || reviewTime.equals(now)) {
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
}