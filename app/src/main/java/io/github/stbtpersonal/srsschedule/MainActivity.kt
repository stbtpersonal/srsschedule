package io.github.stbtpersonal.srsschedule

import android.app.Activity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : Activity() {
    private val scheduleRecyclerViewAdapter = ScheduleRecyclerViewAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.setContentView(R.layout.activity_main)

        this.scheduleRecyclerView.layoutManager =
            LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        this.scheduleRecyclerView.adapter = this.scheduleRecyclerViewAdapter

        NotificationScheduler.scheduleNotifications(this)

        this.refresh()
    }

    private fun refresh() {

    }
}
