package com.example.tagmoa

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import java.util.Locale

class MainTaskListFragment : Fragment(R.layout.fragment_main_task_list) {

    private lateinit var searchInput: TextInputEditText
    private lateinit var btnSearch: MaterialButton
    private lateinit var chipGroup: ChipGroup
    private lateinit var btnAddTask: MaterialButton
    private lateinit var btnManageTags: MaterialButton
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyState: TextView

    private lateinit var tasksRef: DatabaseReference
    private lateinit var tagsRef: DatabaseReference

    private val adapter = MainTaskAdapter { task ->
        if (task.id.isBlank()) return@MainTaskAdapter
        val intent = Intent(requireContext(), MainTaskDetailActivity::class.java).apply {
            putExtra(MainTaskDetailActivity.EXTRA_TASK_ID, task.id)
        }
        startActivity(intent)
    }

    private val allTasks = mutableListOf<MainTask>()
    private val tagMap = mutableMapOf<String, Tag>()
    private val selectedTagIds = mutableSetOf<String>()

    private var tasksListener: ValueEventListener? = null
    private var tagsListener: ValueEventListener? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val uid = requireUserIdOrRedirect() ?: return
        tasksRef = UserDatabase.tasksRef(uid)
        tagsRef = UserDatabase.tagsRef(uid)

        searchInput = view.findViewById(R.id.editSearchMainTask)
        btnSearch = view.findViewById(R.id.btnSearchMainTask)
        chipGroup = view.findViewById(R.id.chipGroupTags)
        btnAddTask = view.findViewById(R.id.btnAddMainTask)
        btnManageTags = view.findViewById(R.id.btnManageTags)
        recyclerView = view.findViewById(R.id.recyclerMainTaskList)
        progressBar = view.findViewById(R.id.progressMainList)
        emptyState = view.findViewById(R.id.textMainListEmpty)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        btnAddTask.setOnClickListener {
            startActivity(Intent(requireContext(), AddEditMainTaskActivity::class.java))
        }

        btnManageTags.setOnClickListener {
            startActivity(Intent(requireContext(), TagManagementActivity::class.java))
        }

        btnSearch.setOnClickListener { filterTasks() }
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterTasks()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        observeTags()
        observeTasks()
    }

    private fun observeTags() {
        tagsListener?.let { tagsRef.removeEventListener(it) }
        tagsListener = tagsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tags = mutableMapOf<String, Tag>()
                for (child in snapshot.children) {
                    val tag = child.getValue(Tag::class.java) ?: continue
                    val tagId = tag.id.ifBlank { child.key.orEmpty() }
                    tag.id = tagId
                    tags[tagId] = tag
                }
                tagMap.clear()
                tagMap.putAll(tags)
                adapter.updateTags(tagMap)
                buildTagChips()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun buildTagChips() {
        chipGroup.removeAllViews()
        selectedTagIds.clear()
        if (tagMap.isEmpty()) {
            val chip = Chip(requireContext()).apply {
                text = getString(R.string.message_no_tags)
                isEnabled = false
            }
            chipGroup.addView(chip)
            filterTasks()
            return
        }
        tagMap.values.forEach { tag ->
            val chip = Chip(requireContext()).apply {
                text = tag.name
                isCheckable = true
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedTagIds.add(tag.id)
                    } else {
                        selectedTagIds.remove(tag.id)
                    }
                    filterTasks()
                }
            }
            chipGroup.addView(chip)
        }
        filterTasks()
    }

    private fun observeTasks() {
        tasksListener?.let { tasksRef.removeEventListener(it) }
        progressBar.visibility = View.VISIBLE
        tasksListener = tasksRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                progressBar.visibility = View.GONE
                allTasks.clear()
                for (child in snapshot.children) {
                    val task = child.getValue(MainTask::class.java) ?: continue
                    val taskId = task.id.ifBlank { child.key.orEmpty() }
                    task.id = taskId
                    allTasks.add(task)
                }
                filterTasks()
            }

            override fun onCancelled(error: DatabaseError) {
                progressBar.visibility = View.GONE
                emptyState.visibility = View.VISIBLE
            }
        })
    }

    private fun filterTasks() {
        val query = searchInput.text?.toString().orEmpty().trim().lowercase(Locale.getDefault())
        val filtered = allTasks.filter { task ->
            val matchesQuery = if (query.isEmpty()) {
                true
            } else {
                task.title.lowercase(Locale.getDefault()).contains(query) ||
                    task.description.lowercase(Locale.getDefault()).contains(query)
            }
            val matchesTags = if (selectedTagIds.isEmpty()) {
                true
            } else {
                selectedTagIds.all { task.tagIds.contains(it) }
            }
            matchesQuery && matchesTags
        }
        adapter.submitTasks(filtered)
        emptyState.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        tasksListener?.let { tasksRef.removeEventListener(it) }
        tagsListener?.let { tagsRef.removeEventListener(it) }
    }
}
