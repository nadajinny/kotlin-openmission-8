package com.example.tagmoa.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tagmoa.R
import com.example.tagmoa.model.MainTask
import com.example.tagmoa.model.SubTask
import com.example.tagmoa.model.Tag

class MainTaskAdapter(
    private val onMoreClick: (MainTask) -> Unit,
    private val onToggleMainComplete: (MainTask, Boolean) -> Unit,
    private val onToggleSubTaskComplete: (SubTask, Boolean) -> Unit
) : RecyclerView.Adapter<MainTaskAdapter.MainTaskViewHolder>() {

    private val items = mutableListOf<MainTask>()
    private val subTasksByTaskId = mutableMapOf<String, List<SubTask>>()
    private val expandedIds = mutableSetOf<String>()

    fun submitTasks(tasks: List<MainTask>, subTasks: Map<String, List<SubTask>> = emptyMap()) {
        items.clear()
        items.addAll(tasks)
        subTasksByTaskId.clear()
        subTasksByTaskId.putAll(subTasks)
        val validIds = tasks.mapNotNull { it.id.takeIf { id -> id.isNotBlank() } }.toSet()
        expandedIds.retainAll(validIds)
        notifyDataSetChanged()
    }

    fun updateTags(tagMap: Map<String, Tag>) {
        // Reserved for future use
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainTaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_main_task, parent, false)
        return MainTaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: MainTaskViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class MainTaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val mainRow: View = itemView.findViewById(R.id.layoutMainRow)
        private val titleView: TextView = itemView.findViewById(R.id.textTaskTitle)
        private val checkbox: CheckBox = itemView.findViewById(R.id.checkboxMainComplete)
        private val moreButton: ImageButton = itemView.findViewById(R.id.buttonTaskMore)
        private val subTaskContainer: LinearLayout = itemView.findViewById(R.id.layoutSubTasks)

        fun bind(task: MainTask) {
            val context = itemView.context
            val subTasks = subTasksByTaskId[task.id].orEmpty()

            titleView.text = task.title.ifBlank { context.getString(R.string.label_no_title) }

            checkbox.setOnCheckedChangeListener(null)
            checkbox.isChecked = task.isCompleted
            checkbox.setOnCheckedChangeListener { _, isChecked ->
                onToggleMainComplete(task, isChecked)
            }

            val alpha = if (task.isCompleted) 0.4f else 1f
            titleView.alpha = alpha
            checkbox.alpha = alpha

            val expanded = expandedIds.contains(task.id) && subTasks.isNotEmpty()
            bindSubTasks(subTasks, expanded)

            mainRow.setOnClickListener {
                if (subTasks.isEmpty()) return@setOnClickListener
                toggleExpand(task)
            }

            moreButton.setOnClickListener {
                onMoreClick(task)
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
            subTasks.forEach { sub ->
                val child = inflater.inflate(
                    R.layout.item_main_subtask,
                    subTaskContainer,
                    false
                )
                val cb: CheckBox = child.findViewById(R.id.checkboxSubTask)
                val content: TextView = child.findViewById(R.id.textSubTaskContent)

                content.text = sub.content
                PriorityFontUtil.apply(content, sub.priority)

                cb.setOnCheckedChangeListener(null)
                cb.isChecked = sub.isCompleted
                cb.setOnCheckedChangeListener { _, isChecked ->
                    onToggleSubTaskComplete(sub, isChecked)
                }

                val alpha = if (sub.isCompleted) 0.4f else 1f
                content.alpha = alpha

                subTaskContainer.addView(child)
            }
        }

        private fun toggleExpand(task: MainTask) {
            if (task.id.isBlank()) return
            if (expandedIds.contains(task.id)) {
                expandedIds.remove(task.id)
            } else {
                expandedIds.add(task.id)
            }
            val pos = bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                notifyItemChanged(pos)
            }
        }
    }
}
