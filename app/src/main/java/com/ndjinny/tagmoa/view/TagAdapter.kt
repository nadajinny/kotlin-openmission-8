package com.ndjinny.tagmoa.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ndjinny.tagmoa.R
import com.ndjinny.tagmoa.model.Tag
import androidx.core.content.ContextCompat

class TagAdapter(
    private val onToggleHidden: (Tag) -> Unit,
    private val onDelete: (Tag) -> Unit
) : RecyclerView.Adapter<TagAdapter.TagViewHolder>() {

    private val tags = mutableListOf<Tag>()

    fun submitTags(newTags: List<Tag>) {
        tags.clear()
        tags.addAll(newTags)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_tag, parent, false)
        return TagViewHolder(view)
    }

    override fun onBindViewHolder(holder: TagViewHolder, position: Int) {
        holder.bind(tags[position])
    }

    override fun getItemCount(): Int = tags.size

    inner class TagViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tagName: TextView = itemView.findViewById(R.id.textTagName)
        private val hideButton: TextView = itemView.findViewById(R.id.btnToggleHidden)
        private val deleteButton: TextView = itemView.findViewById(R.id.btnDeleteTag)
        private val defaultHideColor: Int = hideButton.currentTextColor

        fun bind(tag: Tag) {
            tagName.text = tag.name
            tagName.alpha = if (tag.hidden) 0.5f else 1f
            hideButton.text = itemView.context.getString(
                if (tag.hidden) R.string.action_show_tag else R.string.action_hide_tag
            )
            val showColor = ContextCompat.getColor(itemView.context, R.color.gray_500)
            hideButton.setTextColor(if (tag.hidden) showColor else defaultHideColor)
            hideButton.setOnClickListener { onToggleHidden(tag) }
            deleteButton.setOnClickListener { onDelete(tag) }
        }
    }
}
