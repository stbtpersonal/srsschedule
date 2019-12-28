package io.github.stbtpersonal.srsschedule

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.*

class ScheduleRecyclerViewAdapter : RecyclerView.Adapter<ScheduleRecyclerViewAdapter.ViewHolder>() {
    class ViewHolder(itemView: ViewGroup) : RecyclerView.ViewHolder(itemView)

    private val scheduleItems = mutableListOf<ScheduleItem>()

    fun setScheduleItems(scheduleItems: Collection<ScheduleItem>) {
        this.scheduleItems.clear()
        this.scheduleItems.addAll(scheduleItems.toList())
        this.notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val scheduleItemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.view_schedule_item, parent, false) as ViewGroup
        return ViewHolder(scheduleItemView)
    }

    override fun getItemCount(): Int {
        return this.scheduleItems.size
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val scheduleItemView = holder.itemView
        val scheduleItem = this.scheduleItems.get(position)

        val scheduleItemHour = scheduleItemView.findViewById<TextView>(R.id.scheduleItemHour)
        if (scheduleItem.date == DateUtils.epoch) {
            scheduleItemHour.text = "Now"
        } else {
            val calendar = Calendar.getInstance()
            calendar.time = scheduleItem.date
            scheduleItemHour.text = "${calendar.get(Calendar.HOUR_OF_DAY)}:00"
        }

        val scheduleItemAmount = scheduleItemView.findViewById<TextView>(R.id.scheduleItemAmount)
        scheduleItemAmount.text = "${scheduleItem.amount}"
    }
}