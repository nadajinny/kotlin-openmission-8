package com.example.tagmoa

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class MainTaskAdapter(
    private val onTaskClick: (MainTask) -> Unit
) : RecyclerView.Adapter<MainTaskAdapter.MainTaskViewHolder>() {

    private val tasks = mutableListOf<MainTask>()
    private var tags = emptyMap<String, Tag>()

    fun submitTasks(newTasks: List<MainTask>) {
        tasks.clear()
        tasks.addAll(newTasks)
        notifyDataSetChanged()
    }

    fun updateTags(newTags: Map<String, Tag>) {
        tags = newTags
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainTaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_main_task, parent, false)
        return MainTaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: MainTaskViewHolder, position: Int) {
        holder.bind(tasks[position], tags)
    }

    override fun getItemCount(): Int = tasks.size

    inner class MainTaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.textTaskTitle)
        private val tagsText: TextView = itemView.findViewById(R.id.textTaskTags)
        private val dateText: TextView = itemView.findViewById(R.id.textTaskDate)
        private val durationText: TextView = itemView.findViewById(R.id.textTaskDuration)
        private val descriptionText: TextView = itemView.findViewById(R.id.textTaskDescription)
        private val colorStripe: View = itemView.findViewById(R.id.viewTaskColor)

        fun bind(task: MainTask, tagMap: Map<String, Tag>) {
            title.text = task.title.ifBlank { itemView.context.getString(R.string.label_no_title) }
            tagsText.text = if (task.tagIds.isEmpty()) {
                itemView.context.getString(R.string.label_no_tags)
            } else {
                task.tagIds.mapNotNull { tagMap[it]?.name }.takeIf { it.isNotEmpty() }
                    ?.joinToString(separator = ", ")
                    ?: itemView.context.getString(R.string.label_no_tags)
            }

            val dateLabel = task.dueDate.asDateLabel()
            dateText.text = if (dateLabel.isEmpty()) {
                itemView.context.getString(R.string.label_no_date)
            } else {
                itemView.context.getString(R.string.label_with_date, dateLabel)
            }

            val durationLabel = if (task.duration.isBlank()) {
                itemView.context.getString(R.string.label_no_duration)
            } else {
                itemView.context.getString(R.string.label_with_duration, task.duration)
            }
            durationText.text = durationLabel

            descriptionText.text = task.description.ifBlank {
                itemView.context.getString(R.string.label_no_description)
            }

            val parsedColor = try {
                Color.parseColor(task.mainColor)
            } catch (e: IllegalArgumentException) {
                ContextCompat.getColor(itemView.context, R.color.purple_500)
            }
            colorStripe.setBackgroundColor(parsedColor)

            itemView.setOnClickListener { onTaskClick(task) }
        }
    }
}
