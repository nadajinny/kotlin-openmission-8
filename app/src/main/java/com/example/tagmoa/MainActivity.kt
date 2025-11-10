package com.example.tagmoa

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity() {

    private lateinit var tasksRef: DatabaseReference
    private lateinit var tagsRef: DatabaseReference
    private lateinit var adapter: MainTaskAdapter
    private lateinit var emptyState: TextView
    private lateinit var progressBar: ProgressBar

    private var tasksListener: ValueEventListener? = null
    private var tagsListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerMainTasks)
        val btnAddMainTask = findViewById<Button>(R.id.btnAddMainTask)
        val btnManageTags = findViewById<Button>(R.id.btnManageTags)
        emptyState = findViewById(R.id.textEmptyState)
        progressBar = findViewById(R.id.progressMain)

        adapter = MainTaskAdapter { mainTask ->
            if (mainTask.id.isNotBlank()) {
                val intent = Intent(this, MainTaskDetailActivity::class.java)
                intent.putExtra(MainTaskDetailActivity.EXTRA_TASK_ID, mainTask.id)
                startActivity(intent)
            }
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        tasksRef = FirebaseDatabase.getInstance().getReference("mainTasks")
        tagsRef = FirebaseDatabase.getInstance().getReference("tags")

        btnAddMainTask.setOnClickListener {
            startActivity(Intent(this, AddEditMainTaskActivity::class.java))
        }

        btnManageTags.setOnClickListener {
            startActivity(Intent(this, TagManagementActivity::class.java))
        }

        observeTags()
        observeTasks()
    }

    private fun observeTags() {
        tagsListener?.let { tagsRef.removeEventListener(it) }
        tagsListener = tagsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val newTags = mutableMapOf<String, Tag>()
                for (child in snapshot.children) {
                    val tag = child.getValue(Tag::class.java)
                    if (tag != null) {
                        val tagId = tag.id.ifBlank { child.key.orEmpty() }
                        tag.id = tagId
                        newTags[tagId] = tag
                    }
                }
                adapter.updateTags(newTags)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun observeTasks() {
        tasksListener?.let { tasksRef.removeEventListener(it) }
        progressBar.visibility = View.VISIBLE
        tasksListener = tasksRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tasks = mutableListOf<MainTask>()
                for (child in snapshot.children) {
                    val task = child.getValue(MainTask::class.java)
                    if (task != null) {
                        val taskId = task.id.ifBlank { child.key.orEmpty() }
                        task.id = taskId
                        tasks.add(task)
                    }
                }
                adapter.submitTasks(tasks)
                updateEmptyState(tasks.isEmpty())
                progressBar.visibility = View.GONE
            }

            override fun onCancelled(error: DatabaseError) {
                progressBar.visibility = View.GONE
                updateEmptyState(true)
            }
        })
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        emptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        tasksListener?.let { tasksRef.removeEventListener(it) }
        tagsListener?.let { tagsRef.removeEventListener(it) }
    }
}
