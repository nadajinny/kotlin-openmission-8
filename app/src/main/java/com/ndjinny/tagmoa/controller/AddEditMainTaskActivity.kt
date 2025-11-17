package com.ndjinny.tagmoa.controller

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.ndjinny.tagmoa.R
import com.ndjinny.tagmoa.model.MainTask
import com.ndjinny.tagmoa.model.Tag
import com.ndjinny.tagmoa.model.UserDatabase
import com.ndjinny.tagmoa.model.ensureManualScheduleFlag
import com.ndjinny.tagmoa.view.TaskDateRangePicker
import com.ndjinny.tagmoa.view.formatDateRange
import com.google.firebase.database.DatabaseReference
import com.google.android.material.switchmaterial.SwitchMaterial
import java.util.Locale

class AddEditMainTaskActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TASK_ID = "extra_task_id"
    }

    private lateinit var tasksRef: DatabaseReference
    private lateinit var tagsRef: DatabaseReference
    private lateinit var userId: String

    private lateinit var editTitle: EditText
    private lateinit var editDescription: EditText
    private lateinit var textDateRange: TextView
    private lateinit var textSelectedTags: TextView
    private lateinit var switchAlarm: SwitchMaterial
    //private lateinit var spinnerColor: Spinner

    private var selectedStartDate: Long? = null
    private var selectedEndDate: Long? = null
    private var selectedColor: String = "#A0A7B3" // default gray tone
    private val selectedTagIds = mutableSetOf<String>()
    private var taskId: String? = null
    private var hasManualSchedule: Boolean = false
    private var isTaskCompleted: Boolean = false
    private var taskCompletedAt: Long? = null
    private var isAlarmEnabled: Boolean = true
    private var allTags: List<Tag> = emptyList()
    //private lateinit var colorValues: Array<String>
    private var loadedTask: MainTask? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uid = requireUserIdOrRedirect() ?: return
        userId = uid
        tasksRef = UserDatabase.tasksRef(userId)
        tagsRef = UserDatabase.tagsRef(userId)
        setContentView(R.layout.activity_add_edit_main_task)

        editTitle = findViewById(R.id.editTaskTitle)
        editDescription = findViewById(R.id.editTaskDescription)
        textDateRange = findViewById(R.id.textTaskDateRange)
        textSelectedTags = findViewById(R.id.textSelectedTags)
        switchAlarm = findViewById(R.id.switchMainTaskAlarm)
        //spinnerColor = findViewById(R.id.spinnerTaskColor)
        switchAlarm.setOnCheckedChangeListener { _, isChecked ->
            isAlarmEnabled = isChecked
        }
        switchAlarm.isChecked = isAlarmEnabled

        val btnSelectDateRange = findViewById<Button>(R.id.btnSelectDateRange)
        val btnSelectTags = findViewById<Button>(R.id.btnSelectTags)
        val btnSaveTask = findViewById<Button>(R.id.btnSaveTask)

        //setupColorSpinner()
        loadTags()

        btnSelectDateRange.setOnClickListener { showDateRangePicker() }
        btnSelectTags.setOnClickListener { showTagSelector() }
        btnSaveTask.setOnClickListener { saveTask() }

        taskId = intent.getStringExtra(EXTRA_TASK_ID)
        if (taskId != null) {
            loadTask(taskId!!)
        } else {
            updateDateRangeLabel()
            updateTagLabel()
            switchAlarm.isChecked = isAlarmEnabled
        }
    }

//    private fun setupColorSpinner() {
//        val colorNames = resources.getStringArray(R.array.main_task_color_names)
//        colorValues = resources.getStringArray(R.array.main_task_color_values)
//        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, colorNames)
//        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
//        spinnerColor.adapter = adapter
//        spinnerColor.setSelection(0)
//        selectedColor = colorValues.getOrNull(0) ?: selectedColor
//        spinnerColor.onItemSelectedListener = SimpleItemSelectedListener { index ->
//            if (index in colorValues.indices) {
//                selectedColor = colorValues[index]
//            }
//        }
//    }

    private fun loadTags() {
        tagsRef.get().addOnSuccessListener { snapshot ->
            val tags = snapshot.children.mapNotNull { child ->
                val tag = child.getValue(Tag::class.java)
                tag?.apply { id = id.ifBlank { child.key.orEmpty() } }
            }
            allTags = tags.sortedWith(
                compareBy<Tag> { it.hidden }
                    .thenBy { it.order }
                    .thenBy { it.name.lowercase(Locale.getDefault()) }
            )
            updateTagLabel()
        }
    }

    private fun showTagSelector() {
        val selectableTags = allTags.filter { !it.hidden }
        if (selectableTags.isEmpty()) {
            Toast.makeText(this, R.string.message_no_tags_available, Toast.LENGTH_SHORT).show()
            return
        }
        val tagNames = selectableTags.map { it.name }.toTypedArray()
        val checked = BooleanArray(selectableTags.size) { index ->
            selectableTags[index].id in selectedTagIds
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.title_select_tags)
            .setMultiChoiceItems(tagNames, checked) { _, which, isChecked ->
                val tagId = selectableTags[which].id
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
        TaskDateRangePicker.show(
            context = this,
            initialStartDateMillis = selectedStartDate,
            initialEndDateMillis = selectedEndDate
        ) { start, end ->
            selectedStartDate = start
            selectedEndDate = end
            hasManualSchedule = true
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
            task.id = task.id.ifBlank { id }
            task.ensureManualScheduleFlag()
            loadedTask = task
            editTitle.setText(task.title)
            editDescription.setText(task.description)
            hasManualSchedule = task.manualSchedule
            isTaskCompleted = task.isCompleted
            taskCompletedAt = task.completedAt
            isAlarmEnabled = task.alarmEnabled
            switchAlarm.isChecked = isAlarmEnabled
            if (task.manualSchedule) {
                selectedStartDate = task.startDate ?: task.dueDate
                selectedEndDate = task.endDate ?: task.dueDate
            } else {
                selectedStartDate = null
                selectedEndDate = null
            }
            selectedColor = task.mainColor
            selectedTagIds.clear()
            selectedTagIds.addAll(task.tagIds)
            //setColorSelection(selectedColor)
            updateDateRangeLabel()
            updateTagLabel()
        }
    }

//    private fun setColorSelection(colorHex: String) {
//        val index = colorValues.indexOfFirst { it.equals(colorHex, ignoreCase = true) }
//        if (index >= 0) {
//            spinnerColor.setSelection(index)
//        }
//    }

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

        val manualStart = selectedStartDate ?: selectedEndDate
        val finalStartDate = if (hasManualSchedule) {
            manualStart
        } else {
            loadedTask
                ?.takeIf { !it.manualSchedule }
                ?.startDate
        }
        val finalEndDate = if (hasManualSchedule) {
            selectedEndDate
        } else {
            loadedTask
                ?.takeIf { !it.manualSchedule }
                ?.endDate
        }
        val finalDueDate = if (hasManualSchedule) {
            selectedEndDate ?: selectedStartDate
        } else {
            loadedTask
                ?.takeIf { !it.manualSchedule }
                ?.dueDate
        }
        if (hasManualSchedule && finalStartDate == null) {
            Toast.makeText(this, R.string.label_select_period_placeholder, Toast.LENGTH_SHORT).show()
            return
        }

        val finalIsCompleted = isTaskCompleted && taskId != null
        val finalCompletedAt = if (finalIsCompleted) taskCompletedAt else null

        val task = MainTask(
            id = taskIdValue,
            title = title,
            description = editDescription.text.toString().trim(),
            startDate = finalStartDate,
            endDate = finalEndDate,
            dueDate = finalDueDate,
            isCompleted = finalIsCompleted,
            completedAt = finalCompletedAt,
            manualSchedule = hasManualSchedule,
            mainColor = selectedColor,
            tagIds = selectedTagIds.toMutableList(),
            alarmEnabled = switchAlarm.isChecked
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

}
