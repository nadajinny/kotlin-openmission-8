package com.example.tagmoa

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class DueSubTaskItem(
    val id: String,
    val mainTaskId: String,
    val content: String,
    val parentTitle: String,
    val dueDate: Long?
)

class DueSubTaskAdapter : RecyclerView.Adapter<DueSubTaskAdapter.DueSubTaskViewHolder>() {

    private val items = mutableListOf<DueSubTaskItem>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DueSubTaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_due_subtask, parent, false)
        return DueSubTaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: DueSubTaskViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun submitList(data: List<DueSubTaskItem>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    inner class DueSubTaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val contentText: TextView = itemView.findViewById(R.id.textDueSubTaskContent)
        private val parentText: TextView = itemView.findViewById(R.id.textDueSubTaskParent)
        private val dateText: TextView = itemView.findViewById(R.id.textDueSubTaskDate)

        fun bind(item: DueSubTaskItem) {
            contentText.text = item.content.ifBlank { itemView.context.getString(R.string.label_no_title) }
            parentText.text = itemView.context.getString(R.string.label_main_task) + ": " + item.parentTitle
            val dateLabel = item.dueDate.asDateLabel()
            dateText.text = if (dateLabel.isBlank()) {
                itemView.context.getString(R.string.label_no_date)
            } else {
                itemView.context.getString(R.string.label_with_date, dateLabel)
            }
        }
    }
}
