package com.ndjinny.tagmoa.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ndjinny.tagmoa.R
import com.ndjinny.tagmoa.model.SubTask

class CalendarSubTaskAdapter(
    private val onToggleComplete: (SubTask, Boolean) -> Unit
) : RecyclerView.Adapter<CalendarSubTaskAdapter.CalendarSubTaskViewHolder>() {

    private val items = mutableListOf<SubTask>()

    fun submitList(data: List<SubTask>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarSubTaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_calendar_subtask, parent, false)
        return CalendarSubTaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: CalendarSubTaskViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class CalendarSubTaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val checkbox: CheckBox = itemView.findViewById(R.id.checkboxCalendarSubTask)
        private val contentText: TextView = itemView.findViewById(R.id.textCalendarSubTaskContent)
        private val dateText: TextView = itemView.findViewById(R.id.textCalendarSubTaskDate)

        fun bind(subTask: SubTask) {
            val context = itemView.context
            contentText.text = subTask.content.ifBlank { context.getString(R.string.label_no_description) }
            PriorityFontUtil.apply(contentText, subTask.priority)
            PriorityFontUtil.apply(dateText, subTask.priority)
            val dateLabel = formatDateRange(subTask.startDate, subTask.endDate, subTask.dueDate)
            dateText.text = if (dateLabel.isEmpty()) {
                context.getString(R.string.label_no_date)
            } else {
                context.getString(R.string.label_with_date, dateLabel)
            }

            checkbox.setOnCheckedChangeListener(null)
            checkbox.isChecked = subTask.isCompleted
            checkbox.contentDescription = context.getString(
                R.string.desc_toggle_subtask_complete,
                contentText.text
            )
            checkbox.setOnCheckedChangeListener { _, isChecked ->
                onToggleComplete(subTask, isChecked)
            }

            val alpha = if (subTask.isCompleted) 0.5f else 1f
            contentText.alpha = alpha
            dateText.alpha = alpha
        }
    }
}
