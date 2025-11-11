package com.example.tagmoa.controller

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.example.tagmoa.R
import com.example.tagmoa.model.MainTask
import com.example.tagmoa.model.Tag
import com.example.tagmoa.model.UserDatabase
import com.example.tagmoa.view.DateRangePickerModal
import com.example.tagmoa.view.SimpleItemSelectedListener
import com.example.tagmoa.view.formatDateRange
import com.google.firebase.database.DatabaseReference

class AddEditMainTaskActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TASK_ID = "extra_task_id"
    }

    private lateinit var tasksRef: DatabaseReference
    private lateinit var tagsRef: DatabaseReference
    private lateinit var userId: String

    private lateinit var editTitle: EditText
    private lateinit var editDescription: EditText
    private lateinit var editDuration: EditText
    private lateinit var textDateRange: TextView
    private lateinit var textSelectedTags: TextView
    private lateinit var spinnerColor: Spinner
    private lateinit var composeDatePickerHost: ComposeView

    private var selectedStartDate: Long? = null
    private var selectedEndDate: Long? = null
    private var selectedColor: String = "#559999"
    private val selectedTagIds = mutableSetOf<String>()
    private var taskId: String? = null
    private var allTags: List<Tag> = emptyList()
    private lateinit var colorValues: Array<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uid = requireUserIdOrRedirect() ?: return
        userId = uid
        tasksRef = UserDatabase.tasksRef(userId)
        tagsRef = UserDatabase.tagsRef(userId)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_add_edit_main_task)
        applyEdgeToEdgeInsets(findViewById(R.id.rootAddEditMainTask))

        editTitle = findViewById(R.id.editTaskTitle)
        editDescription = findViewById(R.id.editTaskDescription)
        editDuration = findViewById(R.id.editTaskDuration)
        textDateRange = findViewById(R.id.textTaskDateRange)
        textSelectedTags = findViewById(R.id.textSelectedTags)
        spinnerColor = findViewById(R.id.spinnerTaskColor)
        composeDatePickerHost = findViewById<ComposeView>(R.id.dateRangePickerHost).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        }

        val btnSelectDateRange = findViewById<Button>(R.id.btnSelectDateRange)
        val btnClearDateRange = findViewById<Button>(R.id.btnClearDateRange)
        val btnSelectTags = findViewById<Button>(R.id.btnSelectTags)
        val btnSaveTask = findViewById<Button>(R.id.btnSaveTask)

        setupColorSpinner()
        loadTags()

        btnSelectDateRange.setOnClickListener { showDateRangePicker() }
        btnClearDateRange.setOnClickListener {
            selectedStartDate = null
            selectedEndDate = null
            updateDateRangeLabel()
        }
        btnSelectTags.setOnClickListener { showTagSelector() }
        btnSaveTask.setOnClickListener { saveTask() }

        taskId = intent.getStringExtra(EXTRA_TASK_ID)
        if (taskId != null) {
            loadTask(taskId!!)
        } else {
            updateDateRangeLabel()
            updateTagLabel()
        }
    }

    private fun setupColorSpinner() {
        val colorNames = resources.getStringArray(R.array.main_task_color_names)
        colorValues = resources.getStringArray(R.array.main_task_color_values)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, colorNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerColor.adapter = adapter
        spinnerColor.setSelection(0)
        selectedColor = colorValues.getOrNull(0) ?: selectedColor
        spinnerColor.onItemSelectedListener = SimpleItemSelectedListener { index ->
            if (index in colorValues.indices) {
                selectedColor = colorValues[index]
            }
        }
    }

    private fun loadTags() {
        tagsRef.get().addOnSuccessListener { snapshot ->
            allTags = snapshot.children.mapNotNull { child ->
                val tag = child.getValue(Tag::class.java)
                tag?.apply { id = id.ifBlank { child.key.orEmpty() } }
            }
            updateTagLabel()
        }
    }

    private fun showTagSelector() {
        if (allTags.isEmpty()) {
            Toast.makeText(this, R.string.message_no_tags_available, Toast.LENGTH_SHORT).show()
            return
        }
        val tagNames = allTags.map { it.name }.toTypedArray()
        val checked = BooleanArray(allTags.size) { index ->
            allTags[index].id in selectedTagIds
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.title_select_tags)
            .setMultiChoiceItems(tagNames, checked) { _, which, isChecked ->
                val tagId = allTags[which].id
                if (isChecked) {
                    selectedTagIds.add(tagId)
                } else {
                    selectedTagIds.remove(tagId)
                }
            }
            .setPositiveButton(android.R.string.ok) { _, _ -> updateTagLabel() }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showDateRangePicker() {
        composeDatePickerHost.setContent {
            DateRangePickerModal(
                initialStartDateMillis = selectedStartDate,
                initialEndDateMillis = selectedEndDate,
                onDateRangeSelected = { (start, end) ->
                    selectedStartDate = start
                    selectedEndDate = end
                    updateDateRangeLabel()
                },
                onDismiss = { clearDateRangePickerHost() }
            )
        }
    }

    private fun clearDateRangePickerHost() {
        composeDatePickerHost.setContent { }
    }

    private fun updateDateRangeLabel() {
        val label = formatDateRange(selectedStartDate, selectedEndDate)
        textDateRange.text = if (label.isEmpty()) {
            getString(R.string.label_no_date)
        } else {
            getString(R.string.label_with_date, label)
        }
    }

    private fun updateTagLabel() {
        textSelectedTags.text = if (selectedTagIds.isEmpty()) {
            getString(R.string.label_no_tags)
        } else {
            val names = allTags.filter { it.id in selectedTagIds }.map { it.name }
            if (names.isEmpty()) getString(R.string.label_no_tags) else names.joinToString(", ")
        }
    }

    private fun loadTask(id: String) {
        tasksRef.child(id).get().addOnSuccessListener { snapshot ->
            val task = snapshot.getValue(MainTask::class.java) ?: return@addOnSuccessListener
            editTitle.setText(task.title)
            editDescription.setText(task.description)
            editDuration.setText(task.duration)
            selectedStartDate = task.startDate ?: task.dueDate
            selectedEndDate = task.endDate ?: task.dueDate
            selectedColor = task.mainColor
            selectedTagIds.clear()
            selectedTagIds.addAll(task.tagIds)
            setColorSelection(selectedColor)
            updateDateRangeLabel()
            updateTagLabel()
        }
    }

    private fun setColorSelection(colorHex: String) {
        val index = colorValues.indexOfFirst { it.equals(colorHex, ignoreCase = true) }
        if (index >= 0) {
            spinnerColor.setSelection(index)
        }
    }

    private fun saveTask() {
        val title = editTitle.text.toString().trim()
        if (title.isEmpty()) {
            Toast.makeText(this, R.string.error_empty_title, Toast.LENGTH_SHORT).show()
            return
        }

        val taskIdValue = taskId ?: tasksRef.push().key
        if (taskIdValue.isNullOrBlank()) {
            Toast.makeText(this, R.string.error_generic, Toast.LENGTH_SHORT).show()
            return
        }

        val task = MainTask(
            id = taskIdValue,
            title = title,
            description = editDescription.text.toString().trim(),
            startDate = selectedStartDate,
            endDate = selectedEndDate,
            dueDate = selectedEndDate ?: selectedStartDate,
            duration = editDuration.text.toString().trim(),
            mainColor = selectedColor,
            tagIds = selectedTagIds.toMutableList()
        )

        tasksRef.child(task.id).setValue(task)
            .addOnSuccessListener {
                Toast.makeText(this, R.string.message_task_saved, Toast.LENGTH_SHORT).show()
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
