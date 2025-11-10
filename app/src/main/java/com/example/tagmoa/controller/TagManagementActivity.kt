package com.example.tagmoa.controller

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tagmoa.R
import com.example.tagmoa.model.MainTask
import com.example.tagmoa.model.Tag
import com.example.tagmoa.model.UserDatabase
import com.example.tagmoa.view.TagAdapter
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener

class TagManagementActivity : AppCompatActivity() {

    private lateinit var tagsRef: DatabaseReference
    private lateinit var tasksRef: DatabaseReference
    private lateinit var userId: String
    private lateinit var adapter: TagAdapter
    private lateinit var emptyState: TextView
    private lateinit var inputTagName: EditText

    private var tagsListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uid = requireUserIdOrRedirect() ?: return
        userId = uid
        tagsRef = UserDatabase.tagsRef(userId)
        tasksRef = UserDatabase.tasksRef(userId)
        setContentView(R.layout.activity_tag_management)

        emptyState = findViewById(R.id.textTagEmptyState)
        inputTagName = findViewById(R.id.editTagName)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerTags)
        val btnAddTag = findViewById<Button>(R.id.btnAddTag)

        adapter = TagAdapter { tag -> confirmDeleteTag(tag) }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        btnAddTag.setOnClickListener { handleCreateTag() }

        observeTags()
    }

    private fun observeTags() {
        tagsListener?.let { tagsRef.removeEventListener(it) }
        tagsListener = tagsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tags = mutableListOf<Tag>()
                for (child in snapshot.children) {
                    val tag = child.getValue(Tag::class.java)
                    if (tag != null) {
                        val tagId = tag.id.ifBlank { child.key.orEmpty() }
                        tag.id = tagId
                        tags.add(tag)
                    }
                }
                adapter.submitTags(tags)
                emptyState.visibility = if (tags.isEmpty()) View.VISIBLE else View.GONE
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun handleCreateTag() {
        val name = inputTagName.text.toString().trim()
        if (name.isEmpty()) {
            Toast.makeText(this, R.string.error_empty_tag_name, Toast.LENGTH_SHORT).show()
            return
        }
        val newId = tagsRef.push().key ?: return
        val newTag = Tag(id = newId, name = name)
        tagsRef.child(newId).setValue(newTag)
            .addOnSuccessListener {
                inputTagName.text.clear()
                Toast.makeText(this, R.string.message_tag_created, Toast.LENGTH_SHORT).show()
                promptAssignTagToTasks(newTag)
            }
            .addOnFailureListener { error ->
                Toast.makeText(
                    this,
                    error.message ?: getString(R.string.error_generic),
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun promptAssignTagToTasks(tag: Tag) {
        tasksRef.get().addOnSuccessListener { snapshot ->
            val tasks = snapshot.children.mapNotNull { child ->
                val task = child.getValue(MainTask::class.java)
                task?.apply { id = id.ifBlank { child.key.orEmpty() } }
            }
            if (tasks.isEmpty()) {
                Toast.makeText(this, R.string.message_no_tasks_for_tag, Toast.LENGTH_SHORT).show()
                return@addOnSuccessListener
            }

            val taskNames = tasks.map { it.title.ifBlank { getString(R.string.label_no_title) } }.toTypedArray()
            val checked = BooleanArray(tasks.size)

            AlertDialog.Builder(this)
                .setTitle(getString(R.string.title_assign_tag, tag.name))
                .setMultiChoiceItems(taskNames, checked) { _, index, isChecked ->
                    checked[index] = isChecked
                }
                .setPositiveButton(R.string.action_apply) { _, _ ->
                    val selected = tasks.filterIndexed { index, _ -> checked[index] }
                    if (selected.isNotEmpty()) {
                        attachTagToTasks(tag.id, selected)
                    }
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    private fun attachTagToTasks(tagId: String, selectedTasks: List<MainTask>) {
        selectedTasks.forEach { task ->
            if (task.id.isNotBlank()) {
                val updatedTags = task.tagIds.toMutableSet()
                updatedTags.add(tagId)
                tasksRef.child(task.id).child("tagIds").setValue(updatedTags.toList())
            }
        }
        Toast.makeText(this, R.string.message_tag_applied, Toast.LENGTH_SHORT).show()
    }

    private fun confirmDeleteTag(tag: Tag) {
        AlertDialog.Builder(this)
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
                Toast.makeText(this, R.string.message_tag_deleted, Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { error ->
                Toast.makeText(
                    this,
                    error.message ?: getString(R.string.error_generic),
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun removeTagFromTasks(tagId: String) {
        tasksRef.get().addOnSuccessListener { snapshot ->
            snapshot.children.forEach { child ->
                val taskId = child.key ?: return@forEach
                val task = child.getValue(MainTask::class.java)
                if (task != null && task.tagIds.remove(tagId)) {
                    tasksRef.child(taskId).child("tagIds").setValue(task.tagIds)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tagsListener?.let { tagsRef.removeEventListener(it) }
    }
}
