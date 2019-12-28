package io.github.stbtpersonal.srsschedule

import android.accounts.Account
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.util.*

object NotificationScheduler {
    const val HOUR_PASSED_ACTION = "io.github.stbtpersonal.srsschedule.intent.action.HOUR_PASSED"
    const val CHANNEL_ID = "srsschedule"

    fun scheduleNotifications(context: Context) {
        val applicationContext = context.applicationContext

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = System.currentTimeMillis()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        calendar.set(Calendar.HOUR_OF_DAY, currentHour + 1)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 30)

        val intent = Intent(applicationContext, NotificationBroadcastReceiver::class.java)
        intent.action = this.HOUR_PASSED_ACTION
        val pendingIntent = PendingIntent.getBroadcast(applicationContext, 0, intent, 0)

        val alarmManager =
            applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            1000 * 60 * 60,
            pendingIntent
        )
    }

    fun notifyIfRequired(context: Context) {
        val keyValueStore = KeyValueStore(context)
        val accountName = keyValueStore.accountName
        val accountType = keyValueStore.accountType
        if (accountName == null || accountType == null) {
            return
        }

        object : Thread() {
            override fun run() {
                val account = Account(accountName, accountType)
                val credential = Google.buildCredential(context, account)
                val scheduleItems = ScheduleItemBuilder.build(context, credential)
                val nowItem = scheduleItems.find { it.time == DateUtils.epoch }
                if (nowItem != null) {
                    notify(context, nowItem.amount)
                }
            }
        }.start()
    }

    private fun notify(context: Context, reviewsCount: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(this.CHANNEL_ID, this.CHANNEL_ID, importance)

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val applicationContext = context.applicationContext

        val intent = Intent(applicationContext, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent = PendingIntent.getActivity(applicationContext, 0, intent, 0)

        val notification = NotificationCompat.Builder(applicationContext, this.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_round)
            .setContentTitle("SRS")
            .setContentText("$reviewsCount reviews are available")
            .setNumber(reviewsCount)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(0, notification)
    }
}