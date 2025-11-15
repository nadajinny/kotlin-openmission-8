package com.ndjinny.tagmoa.view

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.ndjinny.tagmoa.R
import com.ndjinny.tagmoa.model.Tag

class TagAdapter(
    private val onToggleHidden: (Tag) -> Unit,
    private val onDelete: (Tag) -> Unit,
    private val dragStartListener: OnStartDragListener
) : RecyclerView.Adapter<TagAdapter.TagViewHolder>(), ItemTouchHelperAdapter {

    private val tags = mutableListOf<Tag>()

    fun submitTags(newTags: List<Tag>) {
        tags.clear()
        tags.addAll(newTags.map { it.copy() })
        notifyDataSetChanged()
    }

    fun getCurrentList(): List<Tag> = tags.toList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_tag, parent, false)
        return TagViewHolder(view)
    }

    override fun onBindViewHolder(holder: TagViewHolder, position: Int) {
        val tag = tags[position]
        holder.bind(tag)
        holder.dragHandle.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                dragStartListener.onStartDrag(holder)
            }
            false
        }
    }

    override fun getItemCount(): Int = tags.size

    override fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
        if (fromPosition == toPosition ||
            fromPosition !in tags.indices ||
            toPosition !in tags.indices
        ) return false
        val item = tags.removeAt(fromPosition)
        tags.add(toPosition, item)
        notifyItemMoved(fromPosition, toPosition)
        return true
    }

    inner class TagViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tagName: TextView = itemView.findViewById(R.id.textTagName)
        private val hideButton: TextView = itemView.findViewById(R.id.btnToggleHidden)
        private val deleteButton: TextView = itemView.findViewById(R.id.btnDeleteTag)
        val dragHandle: ImageView = itemView.findViewById(R.id.imageDragHandle)
        private val tagColor: View = itemView.findViewById(R.id.viewTagColor)
        private val defaultHideColor: Int = hideButton.currentTextColor
        private val showColor: Int = ContextCompat.getColor(itemView.context, R.color.gray_500)

        fun bind(tag: Tag) {
            tagName.text = tag.name
            tagName.alpha = if (tag.hidden) 0.5f else 1f
            tagColor.alpha = if (tag.hidden) 0.4f else 1f

            hideButton.text = itemView.context.getString(
                if (tag.hidden) R.string.action_show_tag else R.string.action_hide_tag
            )
            hideButton.setTextColor(if (tag.hidden) showColor else defaultHideColor)

            hideButton.setOnClickListener { onToggleHidden(tag) }
            deleteButton.setOnClickListener { onDelete(tag) }
        }
    }
}
