package com.example.tagmoa.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tagmoa.R
import com.example.tagmoa.model.SubTask

class SubTaskAdapter(
    private val onEdit: (SubTask) -> Unit,
    private val onDelete: (SubTask) -> Unit
) : RecyclerView.Adapter<SubTaskAdapter.SubTaskViewHolder>() {

    private val subTasks = mutableListOf<SubTask>()

    fun submitList(newItems: List<SubTask>) {
        subTasks.clear()
        subTasks.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubTaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_sub_task, parent, false)
        return SubTaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: SubTaskViewHolder, position: Int) {
        holder.bind(subTasks[position])
    }

    override fun getItemCount(): Int = subTasks.size

    inner class SubTaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val contentText: TextView = itemView.findViewById(R.id.textSubTaskContent)
        private val dateText: TextView = itemView.findViewById(R.id.textSubTaskDate)
        private val priorityText: TextView = itemView.findViewById(R.id.textSubTaskPriority)
        private val editButton: Button = itemView.findViewById(R.id.btnEditSubTask)
        private val deleteButton: Button = itemView.findViewById(R.id.btnDeleteSubTask)

        fun bind(subTask: SubTask) {
            val context = itemView.context
            contentText.text = subTask.content.ifBlank {
                context.getString(R.string.label_no_description)
            }
            val dateLabel = formatDateRange(subTask.startDate, subTask.endDate, subTask.dueDate)
            dateText.text = if (dateLabel.isEmpty()) {
                context.getString(R.string.label_no_date)
            } else {
                context.getString(R.string.label_with_date, dateLabel)
            }
            priorityText.text = context.getString(
                R.string.label_with_priority,
                when (subTask.priority) {
                    2 -> context.getString(R.string.priority_high)
                    1 -> context.getString(R.string.priority_medium)
                    else -> context.getString(R.string.priority_low)
                }
            )
            editButton.setOnClickListener { onEdit(subTask) }
            deleteButton.setOnClickListener { onDelete(subTask) }
        }
    }
}
