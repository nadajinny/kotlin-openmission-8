package com.example.tagmoa.view

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.tagmoa.R
import com.example.tagmoa.model.MainTask
import com.example.tagmoa.model.Tag

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

            dateText.text = task.buildScheduleLabel(itemView.context)

            descriptionText.text = task.description.ifBlank {
                itemView.context.getString(R.string.label_no_description)
            }
            val contentAlpha = if (task.isCompleted) 0.5f else 1f
            title.alpha = contentAlpha
            tagsText.alpha = contentAlpha
            dateText.alpha = contentAlpha
            descriptionText.alpha = contentAlpha

            val parsedColor = try {
                Color.parseColor(task.mainColor)
            } catch (e: IllegalArgumentException) {
                ContextCompat.getColor(itemView.context, R.color.brand_primary)
            }
            colorStripe.setBackgroundColor(parsedColor)

            itemView.setOnClickListener { onTaskClick(task) }
        }
    }
}
