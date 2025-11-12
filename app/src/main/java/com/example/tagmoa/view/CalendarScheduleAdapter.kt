package com.example.tagmoa.view
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.tagmoa.R
import com.example.tagmoa.model.MainTask
import com.example.tagmoa.model.SubTask
import com.google.android.material.card.MaterialCardView

class CalendarScheduleAdapter(
    private val onItemLongClick: (MainTask) -> Unit,
    private val onToggleComplete: (MainTask, Boolean) -> Unit,
    private val onToggleSubTaskComplete: (SubTask, Boolean) -> Unit
) : RecyclerView.Adapter<CalendarScheduleAdapter.ScheduleViewHolder>() {

    private val items = mutableListOf<MainTask>()
    private val subTasksByTaskId = mutableMapOf<String, List<SubTask>>()
    private val expandedTaskIds = mutableSetOf<String>()

    fun submitList(data: List<MainTask>, subTasks: Map<String, List<SubTask>>) {
        items.clear()
        items.addAll(data)
        subTasksByTaskId.clear()
        subTasksByTaskId.putAll(subTasks)
        val validIds = data.mapNotNull { it.id.takeIf(String::isNotBlank) }.toSet()
        expandedTaskIds.retainAll(validIds)
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
        private val subTaskCount: TextView = itemView.findViewById(R.id.textScheduleSubTaskCount)
        private val checkbox: CheckBox = itemView.findViewById(R.id.checkboxScheduleComplete)
        private val card: MaterialCardView = itemView as MaterialCardView
        private val subTaskContainer: LinearLayout =
            itemView.findViewById(R.id.layoutScheduleSubTasks)

        fun bind(item: MainTask) {
            val context = itemView.context
            val title = item.title.ifBlank { context.getString(R.string.label_no_title) }
            val dateRange = formatDateRange(item.startDate, item.endDate, item.dueDate)
                .ifBlank { context.getString(R.string.label_no_date) }
            val subTasks = subTasksByTaskId[item.id].orEmpty()
            val total = subTasks.size
            val incomplete = subTasks.count { !it.isCompleted }

            titleView.text = title
            dateView.text = dateRange

            if (total > 0) {
                subTaskCount.visibility = View.VISIBLE
                subTaskCount.text =
                    context.getString(R.string.calendar_subtask_count, total, incomplete)
            } else {
                subTaskCount.visibility = View.GONE
            }

            checkbox.setOnCheckedChangeListener(null)
            checkbox.isChecked = item.isCompleted
            checkbox.setOnCheckedChangeListener { _, isChecked ->
                onToggleComplete(item, isChecked)
            }

            val alpha = if (item.isCompleted) 0.5f else 1f
            titleView.alpha = alpha
            dateView.alpha = alpha
            subTaskCount.alpha = alpha

            val highlightColor = ContextCompat.getColor(context, R.color.color_aad6eb)
            val defaultColor = ContextCompat.getColor(context, R.color.gray_200)
            val isExpanded = expandedTaskIds.contains(item.id) && subTasks.isNotEmpty()
            card.strokeColor = if (isExpanded) highlightColor else defaultColor

            bindSubTasks(subTasks, isExpanded)

            itemView.setOnClickListener {
                if (subTasks.isEmpty()) return@setOnClickListener
                toggleExpansion(item)
            }
            itemView.setOnLongClickListener {
                onItemLongClick(item)
                true
            }
        }

        private fun bindSubTasks(subTasks: List<SubTask>, expanded: Boolean) {
            subTaskContainer.removeAllViews()
            if (!expanded || subTasks.isEmpty()) {
                subTaskContainer.visibility = View.GONE
                return
            }
            subTaskContainer.visibility = View.VISIBLE
            val inflater = LayoutInflater.from(subTaskContainer.context)
            subTasks.forEach { subTask ->
                val child = inflater.inflate(
                    R.layout.item_calendar_subtask,
                    subTaskContainer,
                    false
                )
                val checkbox: CheckBox = child.findViewById(R.id.checkboxCalendarSubTask)
                val content: TextView = child.findViewById(R.id.textCalendarSubTaskContent)
                val date: TextView = child.findViewById(R.id.textCalendarSubTaskDate)
                val context = child.context
                content.text =
                    subTask.content.ifBlank { context.getString(R.string.label_no_description) }
                val dateLabel = formatDateRange(subTask.startDate, subTask.endDate, subTask.dueDate)
                date.text = if (dateLabel.isEmpty()) {
                    context.getString(R.string.label_no_date)
                } else {
                    context.getString(R.string.label_with_date, dateLabel)
                }

                checkbox.setOnCheckedChangeListener(null)
                checkbox.isChecked = subTask.isCompleted
                checkbox.contentDescription = context.getString(
                    R.string.desc_toggle_subtask_complete,
                    content.text
                )
                checkbox.setOnCheckedChangeListener { _, isChecked ->
                    onToggleSubTaskComplete(subTask, isChecked)
                }

                val alpha = if (subTask.isCompleted) 0.5f else 1f
                content.alpha = alpha
                date.alpha = alpha

                subTaskContainer.addView(child)
            }
        }

        private fun toggleExpansion(item: MainTask) {
            val id = item.id
            if (id.isBlank()) return
            if (expandedTaskIds.contains(id)) {
                expandedTaskIds.remove(id)
            } else {
                expandedTaskIds.add(id)
            }
            val position = bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) {
                notifyItemChanged(position)
            }
        }
    }
}
