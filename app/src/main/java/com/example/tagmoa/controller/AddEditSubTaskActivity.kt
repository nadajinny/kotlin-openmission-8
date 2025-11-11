package com.example.tagmoa.controller

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.example.tagmoa.R
import com.example.tagmoa.model.MainTask
import com.example.tagmoa.model.SubTask
import com.example.tagmoa.model.UserDatabase
import com.example.tagmoa.view.SimpleItemSelectedListener
import com.example.tagmoa.view.asDateLabel
import com.google.firebase.database.DatabaseReference
import java.util.Calendar

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
    private lateinit var textDate: TextView
    private lateinit var editContent: EditText

    private var selectedDate: Long? = null
    private var selectedMainTaskId: String? = null
    private var subTaskId: String? = null
    private var mainTasks: List<MainTask> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uid = requireUserIdOrRedirect() ?: return
        userId = uid
        tasksRef = UserDatabase.tasksRef(userId)
        subTasksRef = UserDatabase.subTasksRef(userId)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_add_edit_sub_task)
        applyEdgeToEdgeInsets(findViewById(R.id.rootAddEditSubTask))

        spinnerMainTasks = findViewById(R.id.spinnerMainTasks)
        spinnerPriority = findViewById(R.id.spinnerPriority)
        textDate = findViewById(R.id.textSubTaskDate)
        editContent = findViewById(R.id.editSubTaskContent)

        val btnSelectDate = findViewById<Button>(R.id.btnSelectSubTaskDate)
        val btnClearDate = findViewById<Button>(R.id.btnClearSubTaskDate)
        val btnSave = findViewById<Button>(R.id.btnSaveSubTask)

        selectedMainTaskId = intent.getStringExtra(EXTRA_MAIN_TASK_ID)
        subTaskId = intent.getStringExtra(EXTRA_SUB_TASK_ID)

        setupPrioritySpinner()
        btnSelectDate.setOnClickListener { showDatePicker() }
        btnClearDate.setOnClickListener {
            selectedDate = null
            updateDateLabel()
        }
        btnSave.setOnClickListener { saveSubTask() }

        loadMainTasks()
    }

    private fun setupPrioritySpinner() {
        val priorities = resources.getStringArray(R.array.sub_task_priorities)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, priorities)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPriority.adapter = adapter
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

            updateDateLabel()

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
            selectedDate = subTask.dueDate
            val adapterCount = spinnerPriority.adapter?.count ?: 0
            if (adapterCount > 0) {
                val priorityIndex = subTask.priority.coerceIn(0, adapterCount - 1)
                spinnerPriority.setSelection(priorityIndex)
            }
            selectedMainTaskId = subTask.mainTaskId.ifBlank { mainTaskId }
            setMainTaskSelection(selectedMainTaskId)
            updateDateLabel()
        }
    }

    private fun setMainTaskSelection(taskId: String?) {
        if (taskId == null) return
        val index = mainTasks.indexOfFirst { it.id == taskId }
        if (index >= 0) {
            spinnerMainTasks.setSelection(index)
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        selectedDate?.let { calendar.timeInMillis = it }
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth, 0, 0, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                selectedDate = calendar.timeInMillis
                updateDateLabel()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateDateLabel() {
        val label = selectedDate.asDateLabel()
        textDate.text = if (label.isEmpty()) {
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
            dueDate = selectedDate
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

    private fun applyEdgeToEdgeInsets(root: View) {
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = systemBars.top, bottom = systemBars.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(root)
    }
}
