package com.ndjinny.tagmoa.controller

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ndjinny.tagmoa.R
import com.ndjinny.tagmoa.model.MainTask
import com.ndjinny.tagmoa.model.SubTask
import com.ndjinny.tagmoa.model.UserDatabase
import com.ndjinny.tagmoa.view.SimpleItemSelectedListener
import com.ndjinny.tagmoa.view.TaskDateRangePicker
import com.ndjinny.tagmoa.view.formatDateRange
import com.google.firebase.database.DatabaseReference

class AddEditSubTaskActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_MAIN_TASK_ID = "extra_main_task_id"
        const val EXTRA_SUB_TASK_ID = "extra_sub_task_id"
    }

    private lateinit var tasksRef: DatabaseReference
    private lateinit var subTasksRef: DatabaseReference
    private lateinit var userId: String

    private lateinit var spinnerMainTasks: Spinner
    private lateinit var spinnerPriority: Spinner
    private lateinit var textDateRange: TextView
    private lateinit var editContent: EditText

    private var selectedStartDate: Long? = null
    private var selectedEndDate: Long? = null
    private var selectedMainTaskId: String? = null
    private var subTaskId: String? = null
    private var mainTasks: List<MainTask> = emptyList()
    private var isCompleted: Boolean = false
    private var completedAt: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uid = requireUserIdOrRedirect() ?: return
        userId = uid
        tasksRef = UserDatabase.tasksRef(userId)
        subTasksRef = UserDatabase.subTasksRef(userId)
        setContentView(R.layout.activity_add_edit_sub_task)

        spinnerMainTasks = findViewById(R.id.spinnerMainTasks)
        spinnerPriority = findViewById(R.id.spinnerPriority)
        textDateRange = findViewById(R.id.textSubTaskDateRange)
        editContent = findViewById(R.id.editSubTaskContent)

        val btnSelectDateRange = findViewById<Button>(R.id.btnSelectSubTaskDateRange)
        val btnSave = findViewById<Button>(R.id.btnSaveSubTask)

        selectedMainTaskId = intent.getStringExtra(EXTRA_MAIN_TASK_ID)
        subTaskId = intent.getStringExtra(EXTRA_SUB_TASK_ID)

        setupPrioritySpinner()
        btnSelectDateRange.setOnClickListener { showDateRangePicker() }
        btnSave.setOnClickListener { saveSubTask() }

        loadMainTasks()
    }

    private fun setupPrioritySpinner() {
        val priorities = resources.getStringArray(R.array.sub_task_priorities)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, priorities)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPriority.adapter = adapter
        spinnerPriority.setSelection(0, false) // 기본값: 낮음
    }

    private fun loadMainTasks() {
        tasksRef.get().addOnSuccessListener { snapshot ->
            mainTasks = snapshot.children.mapNotNull { child ->
                val task = child.getValue(MainTask::class.java)
                task?.apply { id = id.ifBlank { child.key.orEmpty() } }
            }
            if (mainTasks.isEmpty()) {
                Toast.makeText(this, R.string.error_no_main_tasks, Toast.LENGTH_SHORT).show()
                finish()
                return@addOnSuccessListener
            }
            val names = mainTasks.map { it.title.ifBlank { getString(R.string.label_no_title) } }
            val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, names)
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerMainTasks.adapter = spinnerAdapter

            spinnerMainTasks.setOnItemSelectedListener(SimpleItemSelectedListener { index ->
                if (index in mainTasks.indices) {
                    selectedMainTaskId = mainTasks[index].id
                }
            })

            val defaultIndex = selectedMainTaskId?.let { id ->
                mainTasks.indexOfFirst { it.id == id }
            } ?: 0
            if (defaultIndex >= 0 && mainTasks.isNotEmpty()) {
                spinnerMainTasks.setSelection(defaultIndex)
                selectedMainTaskId = mainTasks.getOrNull(defaultIndex)?.id
            }

            updateDateRangeLabel()

            if (!subTaskId.isNullOrBlank()) {
                loadSubTask()
            }
        }
    }

    private fun loadSubTask() {
        val mainTaskId = selectedMainTaskId ?: return
        val subId = subTaskId ?: return
        subTasksRef.child(mainTaskId).child(subId).get().addOnSuccessListener { snapshot ->
            val subTask = snapshot.getValue(SubTask::class.java) ?: return@addOnSuccessListener
            editContent.setText(subTask.content)
            selectedStartDate = subTask.startDate ?: subTask.dueDate
            selectedEndDate = subTask.endDate ?: subTask.dueDate
            isCompleted = subTask.isCompleted
            completedAt = subTask.completedAt
            val adapterCount = spinnerPriority.adapter?.count ?: 0
            if (adapterCount > 0) {
                val priorityIndex = subTask.priority.coerceIn(0, adapterCount - 1)
                spinnerPriority.setSelection(priorityIndex)
            }
            selectedMainTaskId = subTask.mainTaskId.ifBlank { mainTaskId }
            setMainTaskSelection(selectedMainTaskId)
            updateDateRangeLabel()
        }
    }

    private fun setMainTaskSelection(taskId: String?) {
        if (taskId == null) return
        val index = mainTasks.indexOfFirst { it.id == taskId }
        if (index >= 0) {
            spinnerMainTasks.setSelection(index)
        }
    }

    private fun showDateRangePicker() {
        TaskDateRangePicker.show(
            context = this,
            initialStartDateMillis = selectedStartDate,
            initialEndDateMillis = selectedEndDate
        ) { start, end ->
            selectedStartDate = start
            selectedEndDate = end
            updateDateRangeLabel()
        }
    }

    private fun updateDateRangeLabel() {
        val label = formatDateRange(selectedStartDate, selectedEndDate)
        textDateRange.text = if (label.isEmpty()) {
            getString(R.string.label_no_date)
        } else {
            getString(R.string.label_with_date, label)
        }
    }

    private fun saveSubTask() {
        val mainTaskId = selectedMainTaskId
        if (mainTaskId.isNullOrBlank()) {
            Toast.makeText(this, R.string.error_select_main_task, Toast.LENGTH_SHORT).show()
            return
        }
        val content = editContent.text.toString().trim()
        if (content.isEmpty()) {
            Toast.makeText(this, R.string.error_empty_subtask_content, Toast.LENGTH_SHORT).show()
            return
        }
        val priority = spinnerPriority.selectedItemPosition
        val subTaskKey = subTaskId ?: subTasksRef.child(mainTaskId).push().key
        if (subTaskKey.isNullOrBlank()) {
            Toast.makeText(this, R.string.error_generic, Toast.LENGTH_SHORT).show()
            return
        }
        val subTask = SubTask(
            id = subTaskKey,
            mainTaskId = mainTaskId,
            content = content,
            priority = priority,
            startDate = selectedStartDate,
            endDate = selectedEndDate,
            dueDate = selectedEndDate ?: selectedStartDate,
            isCompleted = isCompleted,
            completedAt = completedAt
        )
        subTasksRef.child(mainTaskId).child(subTaskKey).setValue(subTask)
            .addOnSuccessListener {
                Toast.makeText(this, R.string.message_subtask_saved, Toast.LENGTH_SHORT).show()
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

}
