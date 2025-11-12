package com.example.tagmoa.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tagmoa.R
import com.example.tagmoa.model.MainTask

class CalendarScheduleAdapter(
    private val onItemClick: (MainTask) -> Unit
) : RecyclerView.Adapter<CalendarScheduleAdapter.ScheduleViewHolder>() {

    private val items = mutableListOf<MainTask>()

    fun submitList(data: List<MainTask>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_calendar_schedule, parent, false)
        return ScheduleViewHolder(view)
    }

    override fun onBindViewHolder(holder: ScheduleViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ScheduleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val titleView: TextView = itemView.findViewById(R.id.textScheduleTitle)
        private val dateView: TextView = itemView.findViewById(R.id.textScheduleDate)

        fun bind(item: MainTask) {
            val context = itemView.context
            val title = item.title.ifBlank { context.getString(R.string.label_no_title) }
            val dateRange = formatDateRange(item.startDate, item.endDate, item.dueDate)
                .ifBlank { context.getString(R.string.label_no_date) }

            titleView.text = title
            dateView.text = dateRange

            itemView.setOnClickListener { onItemClick(item) }
        }
    }
}
