package com.example.tagmoa

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity() {

    private lateinit var tasksRef: DatabaseReference
    private lateinit var tagsRef: DatabaseReference
    private lateinit var adapter: MainTaskAdapter
    private lateinit var emptyState: TextView
    private lateinit var progressBar: ProgressBar

    private var tasksListener: ValueEventListener? = null
    private var tagsListener: ValueEventListener? = null

    private val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var userId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!ensureAuthenticated()) {
            return
        }

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

        googleSignInClient = GoogleSignIn.getClient(this, buildSignInOptions())

        btnAddMainTask.setOnClickListener {
            startActivity(Intent(this, AddEditMainTaskActivity::class.java))
        }

        btnManageTags.setOnClickListener {
            startActivity(Intent(this, TagManagementActivity::class.java))
        }

        observeTags()
        observeTasks()
    }

    override fun onStart() {
        super.onStart()
        ensureAuthenticated()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                performLogout()
                true
            }
            R.id.action_disconnect -> {
                performAccountDisconnect()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
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

    private fun ensureAuthenticated(): Boolean {
        val currentUser = firebaseAuth.currentUser ?: run {
            redirectToLogin()
            return false
        }

        val shouldRefreshRefs = !::userId.isInitialized || userId != currentUser.uid
        if (shouldRefreshRefs) {
            userId = currentUser.uid
            tasksRef = UserDatabase.tasksRef(userId)
            tagsRef = UserDatabase.tagsRef(userId)
        }
        return true
    }

    private fun redirectToLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun performLogout() {
        firebaseAuth.signOut()
        googleSignInClient.signOut().addOnCompleteListener {
            Toast.makeText(this, getString(R.string.message_signed_out), Toast.LENGTH_SHORT).show()
            redirectToLogin()
        }
    }

    private fun performAccountDisconnect() {
        val currentUser = firebaseAuth.currentUser
        if (currentUser == null) {
            redirectToLogin()
            return
        }

        currentUser.delete().addOnCompleteListener { deleteTask ->
            if (deleteTask.isSuccessful) {
                firebaseAuth.signOut()
                googleSignInClient.revokeAccess().addOnCompleteListener {
                    Toast.makeText(
                        this,
                        getString(R.string.message_account_deleted),
                        Toast.LENGTH_SHORT
                    ).show()
                    redirectToLogin()
                }
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.message_account_delete_failed),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun buildSignInOptions(): GoogleSignInOptions {
        return GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
    }
}
