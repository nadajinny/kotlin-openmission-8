package com.example.tagmoa.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tagmoa.R
import com.example.tagmoa.model.MainTask

class DueMainTaskAdapter(
    private val onClick: (MainTask) -> Unit
) : RecyclerView.Adapter<DueMainTaskAdapter.DueMainTaskViewHolder>() {

    private val items = mutableListOf<MainTask>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DueMainTaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_due_task, parent, false)
        return DueMainTaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: DueMainTaskViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun submitList(data: List<MainTask>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    inner class DueMainTaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleText: TextView = itemView.findViewById(R.id.textDueTaskTitle)
        private val dateText: TextView = itemView.findViewById(R.id.textDueTaskDate)

        fun bind(task: MainTask) {
            titleText.text = task.title.ifBlank { itemView.context.getString(R.string.label_no_title) }
            dateText.text = task.buildScheduleLabel(itemView.context)
            val alpha = if (task.isCompleted) 0.5f else 1f
            titleText.alpha = alpha
            dateText.alpha = alpha
            itemView.setOnClickListener { onClick(task) }
        }
    }
}
