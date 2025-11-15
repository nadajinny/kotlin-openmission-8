package com.ndjinny.tagmoa.controller

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.ndjinny.tagmoa.R
import com.ndjinny.tagmoa.model.MainTask
import com.ndjinny.tagmoa.model.Tag
import com.ndjinny.tagmoa.model.UserDatabase
import com.ndjinny.tagmoa.model.ensureManualScheduleFlag
import com.ndjinny.tagmoa.view.TagAdapter
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener

class TagFilterManagementFragment : Fragment(R.layout.fragment_tag_filter_management) {

    private lateinit var tagsRef: DatabaseReference
    private lateinit var tasksRef: DatabaseReference
    private lateinit var adapter: TagAdapter
    private lateinit var emptyState: TextView

    private var tagsListener: ValueEventListener? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val uid = requireUserIdOrRedirect() ?: run {
            parentFragmentManager.popBackStack()
            return
        }
        tagsRef = UserDatabase.tagsRef(uid)
        tasksRef = UserDatabase.tasksRef(uid)

        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbarTagFilter)
        toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        emptyState = view.findViewById(R.id.textTagEmptyState)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerTags)

        adapter = TagAdapter { tag -> confirmDeleteTag(tag) }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        observeTags()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        tagsListener?.let { tagsRef.removeEventListener(it) }
    }

    private fun observeTags() {
        tagsListener?.let { tagsRef.removeEventListener(it) }
        tagsListener = tagsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tags = mutableListOf<Tag>()
                for (child in snapshot.children) {
                    val tag = child.getValue(Tag::class.java) ?: continue
                    val tagId = tag.id.ifBlank { child.key.orEmpty() }
                    tag.id = tagId
                    tags.add(tag)
                }
                adapter.submitTags(tags)
                emptyState.visibility = if (tags.isEmpty()) View.VISIBLE else View.GONE
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun confirmDeleteTag(tag: Tag) {
        if (tag.id.isBlank()) return
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(R.string.title_delete_tag)
            .setMessage(getString(R.string.message_delete_tag, tag.name))
            .setPositiveButton(R.string.action_delete) { _, _ -> deleteTag(tag) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun deleteTag(tag: Tag) {
        if (tag.id.isBlank()) return
        tagsRef.child(tag.id).removeValue()
            .addOnSuccessListener {
                removeTagFromTasks(tag.id)
                context?.let { ctx ->
                    Toast.makeText(ctx, R.string.message_tag_deleted, Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { error ->
                context?.let { ctx ->
                    Toast.makeText(
                        ctx,
                        error.message ?: getString(R.string.error_generic),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun removeTagFromTasks(tagId: String) {
        tasksRef.get().addOnSuccessListener { snapshot ->
            snapshot.children.forEach { child ->
                val taskId = child.key ?: return@forEach
                val task = child.getValue(MainTask::class.java)
                if (task != null) {
                    task.ensureManualScheduleFlag()
                    if (task.tagIds.remove(tagId)) {
                        tasksRef.child(taskId).child("tagIds").setValue(task.tagIds)
                    }
                }
            }
        }
    }
}
