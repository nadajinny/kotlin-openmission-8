package com.ndjinny.tagmoa.controller

import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.database.DatabaseReference
import com.ndjinny.tagmoa.R
import com.ndjinny.tagmoa.model.AlarmPreferences
import com.ndjinny.tagmoa.model.MainTask
import com.ndjinny.tagmoa.model.SubTask
import com.ndjinny.tagmoa.model.UserDatabase
import com.ndjinny.tagmoa.view.SimpleItemSelectedListener
import com.ndjinny.tagmoa.view.TaskDateRangePicker
import com.ndjinny.tagmoa.view.formatDateRange
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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
    private lateinit var textStartTime: TextView
    private lateinit var textEndTime: TextView
    private lateinit var switchAlarm: SwitchMaterial
    private lateinit var alarmOptionsContainer: View
    private lateinit var spinnerAlarmOffset: Spinner
    private lateinit var editContent: EditText
    private lateinit var alarmOffsetValues: IntArray
    private var suppressAlarmSwitchCallback = false

    private var selectedStartDate: Long? = null
    private var selectedEndDate: Long? = null
    private var selectedMainTaskId: String? = null
    private var subTaskId: String? = null
    private var mainTasks: List<MainTask> = emptyList()
    private var isCompleted: Boolean = false
    private var completedAt: Long? = null
    private var alarmLeadMinutes: Int = 0

    private val timeFormatter by lazy {
        SimpleDateFormat("a hh:mm", Locale.getDefault())
    }

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
        textStartTime = findViewById(R.id.textSubTaskStartTime)
        textEndTime = findViewById(R.id.textSubTaskEndTime)
        switchAlarm = findViewById(R.id.switchSubTaskAlarm)
        alarmOptionsContainer = findViewById(R.id.layoutSubTaskAlarmOptions)
        spinnerAlarmOffset = findViewById(R.id.spinnerSubTaskAlarmOffset)
        editContent = findViewById(R.id.editSubTaskContent)

        val btnSelectDateRange = findViewById<Button>(R.id.btnSelectSubTaskDateRange)
        val btnSelectStartTime = findViewById<Button>(R.id.btnSelectSubTaskStartTime)
        val btnSelectEndTime = findViewById<Button>(R.id.btnSelectSubTaskEndTime)
        val btnSave = findViewById<Button>(R.id.btnSaveSubTask)

        selectedMainTaskId = intent.getStringExtra(EXTRA_MAIN_TASK_ID)
        subTaskId = intent.getStringExtra(EXTRA_SUB_TASK_ID)

        setupPrioritySpinner()
        setupAlarmOffsetSpinner()
        btnSelectDateRange.setOnClickListener { showDateRangePicker() }
        btnSelectStartTime.setOnClickListener { showTimePicker(isStart = true) }
        btnSelectEndTime.setOnClickListener { showTimePicker(isStart = false) }
        switchAlarm.setOnCheckedChangeListener { _, isChecked ->
            if (suppressAlarmSwitchCallback) return@setOnCheckedChangeListener
            if (isChecked && !ExactAlarmPermissionHelper.hasExactAlarmPermission(this)) {
                Toast.makeText(this, R.string.alarm_exact_permission_required, Toast.LENGTH_SHORT).show()
                ExactAlarmPermissionHelper.requestExactAlarmPermission(this)
                suppressAlarmSwitchCallback = true
                switchAlarm.isChecked = false
                suppressAlarmSwitchCallback = false
                return@setOnCheckedChangeListener
            }
            alarmOptionsContainer.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
        alarmOptionsContainer.visibility = if (switchAlarm.isChecked) View.VISIBLE else View.GONE
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
            val allTasks = snapshot.children.mapNotNull { child ->
                val task = child.getValue(MainTask::class.java)
                task?.apply { id = id.ifBlank { child.key.orEmpty() } }
            }
            val previouslySelected = selectedMainTaskId?.let { id ->
                allTasks.firstOrNull { it.id == id }
            }
            val incompleteTasks = allTasks.filter { !it.isCompleted }
            mainTasks = if (previouslySelected != null && previouslySelected.isCompleted) {
                listOf(previouslySelected) + incompleteTasks.filter { it.id != previouslySelected.id }
            } else {
                incompleteTasks
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
            alarmLeadMinutes = subTask.alarmLeadMinutes
            switchAlarm.isChecked = subTask.alarmEnabled
            setAlarmOffsetSelection(alarmLeadMinutes)
            alarmOptionsContainer.visibility = if (subTask.alarmEnabled) View.VISIBLE else View.GONE
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

    private fun setAlarmOffsetSelection(minutes: Int) {
        val index = alarmOffsetValues.indexOfFirst { it == minutes }.takeIf { it >= 0 } ?: 0
        spinnerAlarmOffset.setSelection(index)
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
            selectedStartDate = mergeDateAndTime(start, selectedStartDate)
            selectedEndDate = mergeDateAndTime(end, selectedEndDate)
            updateDateRangeLabel()
            ensureChronology()
            updateTimeLabels()
        }
    }

    private fun updateDateRangeLabel() {
        val label = formatDateRange(selectedStartDate, selectedEndDate)
        textDateRange.text = if (label.isEmpty()) {
            getString(R.string.label_no_date)
        } else {
            getString(R.string.label_with_date, label)
        }
        updateTimeLabels()
    }

    private fun updateTimeLabels() {
        textStartTime.text = formatTime(selectedStartDate)
        textEndTime.text = formatTime(selectedEndDate)
    }

    private fun formatTime(timeMillis: Long?): String {
        return timeMillis?.let { timeFormatter.format(it) } ?: getString(R.string.label_time_placeholder)
    }

    private fun showTimePicker(isStart: Boolean) {
        val base = if (isStart) selectedStartDate else selectedEndDate
        if (base == null) {
            Toast.makeText(this, R.string.message_select_date_first, Toast.LENGTH_SHORT).show()
            return
        }
        val calendar = Calendar.getInstance().apply { timeInMillis = base }
        var pendingHour = calendar.get(Calendar.HOUR_OF_DAY)
        var pendingMinute = calendar.get(Calendar.MINUTE)
        var appliedViaButton = false
        var userAdjusted = false
        val dialog = TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                appliedViaButton = true
                applySelectedTime(isStart, hourOfDay, minute)
            },
            pendingHour,
            pendingMinute,
            android.text.format.DateFormat.is24HourFormat(this)
        )
        dialog.setTitle(
            if (isStart) getString(R.string.label_subtask_start_time)
            else getString(R.string.label_subtask_end_time)
        )
        dialog.setCanceledOnTouchOutside(true)
        dialog.setOnShowListener {
            dialog.getButton(TimePickerDialog.BUTTON_POSITIVE)?.text = getString(R.string.action_save)
            dialog.getButton(TimePickerDialog.BUTTON_NEGATIVE)?.text = getString(R.string.action_cancel)
            val pickerId = resources.getIdentifier("timePicker", "id", "android")
            val picker = dialog.findViewById<TimePicker>(pickerId)
            picker?.setOnTimeChangedListener { _: TimePicker, hour: Int, minute: Int ->
                userAdjusted = true
                pendingHour = hour
                pendingMinute = minute
            }
        }
        dialog.setOnDismissListener {
            if (!appliedViaButton && userAdjusted) {
                applySelectedTime(isStart, pendingHour, pendingMinute)
            }
        }
        dialog.show()
    }

    private fun applySelectedTime(isStart: Boolean, hour: Int, minute: Int) {
        val target = if (isStart) selectedStartDate else selectedEndDate
        if (target == null) {
            Toast.makeText(this, R.string.message_select_date_first, Toast.LENGTH_SHORT).show()
            return
        }
        val calendar = Calendar.getInstance().apply { timeInMillis = target }
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        if (isStart) {
            selectedStartDate = calendar.timeInMillis
        } else {
            selectedEndDate = calendar.timeInMillis
        }
        ensureChronology()
        updateTimeLabels()
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
        val alarmEnabled = switchAlarm.isChecked
        val alarmOffset = if (alarmEnabled) {
            alarmOffsetValues.getOrNull(spinnerAlarmOffset.selectedItemPosition) ?: 0
        } else {
            0
        }
        val targetEnd = selectedEndDate ?: selectedStartDate
        val computedAlarmTime = if (alarmEnabled && targetEnd != null) {
            targetEnd - alarmOffset * 60_000L
        } else {
            null
        }

        if (alarmEnabled && !AlarmPreferences.isSubAlarmEnabled(this)) {
            AlarmPreferences.setSubAlarmEnabled(this, true)
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
            completedAt = completedAt,
            alarmEnabled = alarmEnabled,
            alarmLeadMinutes = alarmOffset,
            alarmTimeMillis = computedAlarmTime
        )
        subTasksRef.child(mainTaskId).child(subTaskKey).setValue(subTask)
            .addOnSuccessListener {
                refreshSubTaskReminders()
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

    private fun mergeDateAndTime(newDate: Long?, previous: Long?): Long? {
        if (newDate == null) return null
        if (previous == null) return newDate
        val dateCal = Calendar.getInstance().apply { timeInMillis = newDate }
        val prevCal = Calendar.getInstance().apply { timeInMillis = previous }
        dateCal.set(Calendar.HOUR_OF_DAY, prevCal.get(Calendar.HOUR_OF_DAY))
        dateCal.set(Calendar.MINUTE, prevCal.get(Calendar.MINUTE))
        return dateCal.timeInMillis
    }

    private fun ensureChronology() {
        if (selectedStartDate != null && selectedEndDate != null &&
            selectedEndDate!! < selectedStartDate!!
        ) {
            selectedEndDate = selectedStartDate
        }
    }

    private fun setupAlarmOffsetSpinner() {
        alarmOffsetValues = resources.getIntArray(R.array.subtask_alarm_offset_values)
        val labels = resources.getStringArray(R.array.subtask_alarm_offset_labels)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, labels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerAlarmOffset.adapter = adapter
        spinnerAlarmOffset.setSelection(0, false)
    }

    private fun refreshSubTaskReminders() {
        val appContext = applicationContext
        subTasksRef.get().addOnSuccessListener { snapshot ->
            val allSubTasks = mutableListOf<SubTask>()
            for (mainSnapshot in snapshot.children) {
                val mainId = mainSnapshot.key.orEmpty()
                for (child in mainSnapshot.children) {
                    val subTask = child.getValue(SubTask::class.java) ?: continue
                    subTask.id = subTask.id.ifBlank { child.key.orEmpty() }
                    subTask.mainTaskId = subTask.mainTaskId.ifBlank { mainId }
                    allSubTasks.add(subTask)
                }
            }
            TaskReminderScheduler.syncSubTaskReminders(appContext, allSubTasks)
        }.addOnFailureListener { error ->
            Log.e("AddEditSubTask", "Failed to refresh subtask reminders", error)
        }
    }

}
