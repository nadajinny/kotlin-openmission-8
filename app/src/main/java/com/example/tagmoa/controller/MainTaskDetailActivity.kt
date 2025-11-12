package com.example.tagmoa.controller

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tagmoa.R
import com.example.tagmoa.model.MainTask
import com.example.tagmoa.model.SubTask
import com.example.tagmoa.model.Tag
import com.example.tagmoa.model.UserDatabase
import com.example.tagmoa.view.SubTaskAdapter
import com.example.tagmoa.view.formatDateRange
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener

class MainTaskDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TASK_ID = "extra_task_id"
    }

    private lateinit var tasksRef: DatabaseReference
    private lateinit var subTasksRef: DatabaseReference
    private lateinit var tagsRef: DatabaseReference
    private lateinit var userId: String

    private lateinit var textTitle: TextView
    private lateinit var textDate: TextView
    private lateinit var textTags: TextView
    private lateinit var textDescription: TextView
    private lateinit var colorView: View
    private lateinit var subTaskEmpty: TextView

    private lateinit var adapter: SubTaskAdapter

    private var taskId: String = ""
    private var currentTask: MainTask? = null
    private var tagsMap: Map<String, Tag> = emptyMap()

    private var taskListener: ValueEventListener? = null
    private var subTaskListener: ValueEventListener? = null
    private var tagListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_task_detail)

        taskId = intent.getStringExtra(EXTRA_TASK_ID).orEmpty()
        if (taskId.isBlank()) {
            finish()
            return
        }

        val uid = requireUserIdOrRedirect() ?: return
        userId = uid
        tasksRef = UserDatabase.tasksRef(userId)
        subTasksRef = UserDatabase.subTasksRef(userId)
        tagsRef = UserDatabase.tagsRef(userId)

        textTitle = findViewById(R.id.textDetailTitle)
        textDate = findViewById(R.id.textDetailDate)
        textTags = findViewById(R.id.textDetailTags)
        textDescription = findViewById(R.id.textDetailDescription)
        colorView = findViewById(R.id.viewDetailColor)
        subTaskEmpty = findViewById(R.id.textSubTaskEmpty)

        val recyclerSubTasks = findViewById<RecyclerView>(R.id.recyclerSubTasks)
        val btnEditTask = findViewById<Button>(R.id.btnEditTask)
        val btnDeleteTask = findViewById<Button>(R.id.btnDeleteTask)
        val btnAddSubTask = findViewById<Button>(R.id.btnAddSubTask)

        adapter = SubTaskAdapter(
            onEdit = { subTask ->
                val intent = Intent(this, AddEditSubTaskActivity::class.java)
                intent.putExtra(AddEditSubTaskActivity.EXTRA_MAIN_TASK_ID, taskId)
                intent.putExtra(AddEditSubTaskActivity.EXTRA_SUB_TASK_ID, subTask.id)
                startActivity(intent)
            },
            onDelete = { subTask -> confirmDeleteSubTask(subTask) }
        )

        recyclerSubTasks.layoutManager = LinearLayoutManager(this)
        recyclerSubTasks.adapter = adapter

        btnEditTask.setOnClickListener {
            val intent = Intent(this, AddEditMainTaskActivity::class.java)
            intent.putExtra(AddEditMainTaskActivity.EXTRA_TASK_ID, taskId)
            startActivity(intent)
        }

        btnDeleteTask.setOnClickListener { confirmDeleteTask() }
        btnAddSubTask.setOnClickListener {
            val intent = Intent(this, AddEditSubTaskActivity::class.java)
            intent.putExtra(AddEditSubTaskActivity.EXTRA_MAIN_TASK_ID, taskId)
            startActivity(intent)
        }

        observeTags()
        observeTask()
        observeSubTasks()
    }

    private fun observeTags() {
        tagListener?.let { tagsRef.removeEventListener(it) }
        tagListener = tagsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                tagsMap = snapshot.children.mapNotNull { child ->
                    val tag = child.getValue(Tag::class.java)
                    tag?.apply { id = id.ifBlank { child.key.orEmpty() } }
                }.associateBy { it.id }
                renderTask()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun observeTask() {
        taskListener?.let { tasksRef.child(taskId).removeEventListener(it) }
        taskListener = tasksRef.child(taskId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                currentTask = snapshot.getValue(MainTask::class.java)
                currentTask?.id = taskId
                renderTask()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun observeSubTasks() {
        subTaskListener?.let { subTasksRef.child(taskId).removeEventListener(it) }
        subTaskListener = subTasksRef.child(taskId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val items = mutableListOf<SubTask>()
                for (child in snapshot.children) {
                    val subTask = child.getValue(SubTask::class.java)
                    if (subTask != null) {
                        subTask.id = subTask.id.ifBlank { child.key.orEmpty() }
                        items.add(subTask)
                    }
                }
                adapter.submitList(items)
                subTaskEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
            }

            override fun onCancelled(error: DatabaseError) {
                subTaskEmpty.visibility = View.VISIBLE
            }
        })
    }

    private fun renderTask() {
        val task = currentTask ?: return
        textTitle.text = task.title.ifBlank { getString(R.string.label_no_title) }
        val dateLabel = formatDateRange(task.startDate, task.endDate, task.dueDate)
        textDate.text = if (dateLabel.isEmpty()) {
            getString(R.string.label_no_date)
        } else {
            getString(R.string.label_with_date, dateLabel)
        }
        val tagNames = task.tagIds.mapNotNull { tagsMap[it]?.name }
        textTags.text = if (tagNames.isEmpty()) getString(R.string.label_no_tags) else tagNames.joinToString(", ")
        textDescription.text = task.description.ifBlank { getString(R.string.label_no_description) }
        val parsedColor = try {
            Color.parseColor(task.mainColor)
        } catch (e: IllegalArgumentException) {
            ContextCompat.getColor(this, R.color.brand_primary)
        }
        colorView.setBackgroundColor(parsedColor)
    }

    private fun confirmDeleteTask() {
        AlertDialog.Builder(this)
            .setTitle(R.string.title_delete_task)
            .setMessage(R.string.message_delete_task)
            .setPositiveButton(R.string.action_delete) { _, _ -> deleteTask() }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun deleteTask() {
        tasksRef.child(taskId).removeValue()
            .addOnSuccessListener {
                subTasksRef.child(taskId).removeValue()
                Toast.makeText(this, R.string.message_task_deleted, Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { error ->
                Toast.makeText(
                    this,
                    error.message ?: getString(R.string.error_generic),
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun confirmDeleteSubTask(subTask: SubTask) {
        AlertDialog.Builder(this)
            .setTitle(R.string.title_delete_subtask)
            .setMessage(R.string.message_delete_subtask)
            .setPositiveButton(R.string.action_delete) { _, _ -> deleteSubTask(subTask) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun deleteSubTask(subTask: SubTask) {
        if (subTask.id.isBlank()) return
        subTasksRef.child(taskId).child(subTask.id).removeValue()
            .addOnSuccessListener {
                Toast.makeText(this, R.string.message_subtask_deleted, Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { error ->
                Toast.makeText(
                    this,
                    error.message ?: getString(R.string.error_generic),
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        taskListener?.let { tasksRef.child(taskId).removeEventListener(it) }
        subTaskListener?.let { subTasksRef.child(taskId).removeEventListener(it) }
        tagListener?.let { tagsRef.removeEventListener(it) }
    }
}
