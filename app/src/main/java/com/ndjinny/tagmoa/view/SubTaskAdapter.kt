package com.ndjinny.tagmoa.view

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ndjinny.tagmoa.R
import com.ndjinny.tagmoa.model.SubTask

class SubTaskAdapter(
    private val onEdit: (SubTask) -> Unit,
    private val onDelete: (SubTask) -> Unit,
    private val onToggleComplete: (SubTask, Boolean) -> Unit
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
        private val checkbox: CheckBox = itemView.findViewById(R.id.checkboxSubTaskComplete)
        private val contentText: TextView = itemView.findViewById(R.id.textSubTaskContent)
        private val dateText: TextView = itemView.findViewById(R.id.textSubTaskDate)
        private val moreButton: ImageView = itemView.findViewById(R.id.buttonSubTaskMore)

        fun bind(subTask: SubTask) {
            val context = itemView.context
            contentText.text = subTask.content.ifBlank {
                context.getString(R.string.label_no_description)
            }
            PriorityFontUtil.apply(contentText, subTask.priority)
            PriorityFontUtil.apply(dateText, subTask.priority)

            checkbox.setOnCheckedChangeListener(null)
            checkbox.isChecked = subTask.isCompleted
            checkbox.contentDescription = context.getString(
                R.string.desc_toggle_subtask_complete,
                contentText.text
            )
            checkbox.setOnCheckedChangeListener { _, isChecked ->
                onToggleComplete(subTask, isChecked)
            }

            val alpha = if (subTask.isCompleted) 0.4f else 1f
            contentText.alpha = alpha
            dateText.alpha = alpha

            val dateLabel = formatDateRange(subTask.startDate, subTask.endDate, subTask.dueDate)
            if (dateLabel.isEmpty()) {
                dateText.visibility = View.GONE
            } else {
                dateText.visibility = View.VISIBLE
                dateText.text = context.getString(R.string.label_with_date, dateLabel)
            }

            moreButton.setOnClickListener {
                showSubTaskMenu(context, subTask)
            }
        }

        private fun showSubTaskMenu(context: Context, subTask: SubTask) {
            PopupMenu(context, moreButton).apply {
                menuInflater.inflate(R.menu.menu_subtask_item, menu)
                setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.action_edit_subtask -> {
                            onEdit(subTask)
                            true
                        }
                        R.id.action_delete_subtask -> {
                            onDelete(subTask)
                            true
                        }
                        else -> false
                    }
                }
            }.show()
        }
    }
}
