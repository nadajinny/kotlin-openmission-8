package com.ndjinny.tagmoa.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ndjinny.tagmoa.R
import com.ndjinny.tagmoa.model.QnaEntry

class QnaAdapter(
    private val onItemClick: (QnaEntry) -> Unit
) : ListAdapter<QnaEntry, QnaAdapter.QnaViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QnaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_qna_entry, parent, false)
        return QnaViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: QnaViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class QnaViewHolder(
        itemView: View,
        private val onItemClick: (QnaEntry) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.textQnaTitle)
        private val content: TextView = itemView.findViewById(R.id.textQnaContent)
        private val meta: TextView = itemView.findViewById(R.id.textQnaMeta)
        private val badge: TextView = itemView.findViewById(R.id.textQnaVisibilityBadge)
        private val commentCount: TextView = itemView.findViewById(R.id.textQnaCommentCount)
        private var currentEntry: QnaEntry? = null

        init {
            itemView.setOnClickListener {
                currentEntry?.let(onItemClick)
            }
        }

        fun bind(entry: QnaEntry) {
            currentEntry = entry
            val context = itemView.context
            title.text = entry.title.orEmpty()
            content.text = entry.content.orEmpty()

            val authorName = entry.authorName?.takeIf { it.isNotBlank() }
                ?: context.getString(R.string.qna_meta_unknown_author)
            val dateLabel = entry.createdAt.takeIf { it > 0 }?.asDateLabel().orEmpty()
            meta.text = if (dateLabel.isNotBlank()) {
                context.getString(R.string.qna_meta_format, authorName, dateLabel)
            } else {
                authorName
            }

            badge.text = if (entry.isPublic) {
                context.getString(R.string.qna_visibility_public_badge)
            } else {
                context.getString(R.string.qna_visibility_private_badge)
            }
            badge.isVisible = true

            val comments = entry.comments?.values ?: emptyList()
            commentCount.isVisible = comments.isNotEmpty()
            if (comments.isNotEmpty()) {
                commentCount.text = context.getString(R.string.qna_comment_count, comments.size)
            } else {
                commentCount.text = ""
            }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<QnaEntry>() {
            override fun areItemsTheSame(oldItem: QnaEntry, newItem: QnaEntry): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: QnaEntry, newItem: QnaEntry): Boolean {
                return oldItem == newItem
            }
        }
    }
}
