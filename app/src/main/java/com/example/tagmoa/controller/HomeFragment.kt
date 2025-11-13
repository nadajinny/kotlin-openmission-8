package com.example.tagmoa.controller

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tagmoa.R
import com.example.tagmoa.model.MainTask
import com.example.tagmoa.model.SubTask
import com.example.tagmoa.model.UserDatabase
import com.example.tagmoa.model.ensureManualScheduleFlag
import com.example.tagmoa.view.DueMainTaskAdapter
import com.example.tagmoa.view.DueSubTaskAdapter
import com.example.tagmoa.view.DueSubTaskItem
import com.example.tagmoa.view.overlapsWithRange
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HomeFragment : Fragment(R.layout.fragment_home) {

    private var backgroundView: ImageView? = null
    private lateinit var recyclerMainDue: RecyclerView
    private lateinit var recyclerSubDue: RecyclerView
    private lateinit var textMainEmpty: TextView
    private lateinit var textSubEmpty: TextView
    private lateinit var textTodayDate: TextView
    private lateinit var textTodayTime: TextView

    private lateinit var mainAdapter: DueMainTaskAdapter
    private lateinit var subAdapter: DueSubTaskAdapter

    private lateinit var tasksRef: DatabaseReference
    private lateinit var subTasksRef: DatabaseReference

    private var tasksListener: ValueEventListener? = null
    private var subTasksListener: ValueEventListener? = null

    private val taskMap = mutableMapOf<String, MainTask>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val uid = requireUserIdOrRedirect() ?: return
        tasksRef = UserDatabase.tasksRef(uid)
        subTasksRef = UserDatabase.subTasksRef(uid)

        backgroundView = view.findViewById(R.id.imageHomeBackground)
        recyclerMainDue = view.findViewById(R.id.recyclerHomeMainDue)
        recyclerSubDue = view.findViewById(R.id.recyclerHomeSubDue)
        textMainEmpty = view.findViewById(R.id.textHomeMainDueEmpty)
        textSubEmpty = view.findViewById(R.id.textHomeSubDueEmpty)
        textTodayDate = view.findViewById(R.id.textHomeTodayDate)
        textTodayTime = view.findViewById(R.id.textHomeTodayTime)

        mainAdapter = DueMainTaskAdapter { mainTask ->
            if (mainTask.id.isBlank()) return@DueMainTaskAdapter
            val intent = Intent(requireContext(), MainTaskDetailActivity::class.java).apply {
                putExtra(MainTaskDetailActivity.EXTRA_TASK_ID, mainTask.id)
            }
            startActivity(intent)
        }
        subAdapter = DueSubTaskAdapter()

        recyclerMainDue.layoutManager = LinearLayoutManager(requireContext())
        recyclerMainDue.adapter = mainAdapter

        recyclerSubDue.layoutManager = LinearLayoutManager(requireContext())
        recyclerSubDue.adapter = subAdapter

        updateTodayInfo()
        applySavedBackground()
        observeTasks()
        observeSubTasks()
    }

    private fun updateTodayInfo() {
        val now = Calendar.getInstance().time
        val dateFormat = SimpleDateFormat("yyyy년 MM월 dd일 (E)", Locale.KOREA)
        val timeFormat = SimpleDateFormat("HH:mm", Locale.KOREA)
        textTodayDate.text = dateFormat.format(now)
        textTodayTime.text = timeFormat.format(now)
    }

    private fun observeTasks() {
        tasksListener?.let { tasksRef.removeEventListener(it) }
        tasksListener = tasksRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val result = mutableListOf<MainTask>()
                taskMap.clear()
                for (child in snapshot.children) {
                    val task = child.getValue(MainTask::class.java) ?: continue
                    val taskId = task.id.ifBlank { child.key.orEmpty() }
                    task.ensureManualScheduleFlag()
                    task.id = taskId
                    taskMap[taskId] = task
                    if (isTaskDueToday(task)) {
                        result.add(task)
                    }
                }
                mainAdapter.submitList(result)
                textMainEmpty.visibility = if (result.isEmpty()) View.VISIBLE else View.GONE
            }

            override fun onCancelled(error: DatabaseError) {
                textMainEmpty.visibility = View.VISIBLE
            }
        })
    }

    private fun observeSubTasks() {
        subTasksListener?.let { subTasksRef.removeEventListener(it) }
        subTasksListener = subTasksRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val result = mutableListOf<DueSubTaskItem>()
                for (mainSnapshot in snapshot.children) {
                    val mainId = mainSnapshot.key.orEmpty()
                    for (child in mainSnapshot.children) {
                        val subTask = child.getValue(SubTask::class.java) ?: continue
                        val subId = subTask.id.ifBlank { child.key.orEmpty() }
                        if (subTask.isCompleted) continue
                        if (isRangeDueToday(subTask.startDate, subTask.endDate, subTask.dueDate)) {
                            val parentTitle = taskMap[mainId]?.title ?: getString(R.string.label_no_title)
                            result.add(
                                DueSubTaskItem(
                                    id = subId,
                                    mainTaskId = mainId,
                                    content = subTask.content,
                                    parentTitle = parentTitle,
                                    startDate = subTask.startDate,
                                    endDate = subTask.endDate,
                                    dueDate = subTask.dueDate
                                )
                            )
                        }
                    }
                }
                subAdapter.submitList(result)
                textSubEmpty.visibility = if (result.isEmpty()) View.VISIBLE else View.GONE
            }

            override fun onCancelled(error: DatabaseError) {
                textSubEmpty.visibility = View.VISIBLE
            }
        })
    }

    private fun isTaskDueToday(task: MainTask): Boolean {
        if (task.isCompleted) return false
        if (!task.manualSchedule && task.dueDate == null) return false
        return isRangeDueToday(task.startDate, task.endDate, task.dueDate)
    }

    private fun isRangeDueToday(startDate: Long?, endDate: Long?, fallbackDate: Long?): Boolean {
        val startCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return overlapsWithRange(
            startDate,
            endDate,
            startCal.timeInMillis,
            endCal.timeInMillis,
            fallbackDate
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        tasksListener?.let { tasksRef.removeEventListener(it) }
        subTasksListener?.let { subTasksRef.removeEventListener(it) }
        backgroundView = null
    }

    private fun applySavedBackground() {
        if (backgroundView == null) return
        val uri = HomeBackgroundManager.getBackgroundUri(requireContext())
        if (uri != null) {
            applyBackground(uri)
        } else {
            resetToDefaultBackground()
        }
    }

    private fun applyBackground(uri: Uri) {
        val resolver = requireContext().contentResolver
        try {
            resolver.openInputStream(uri)?.use { input ->
                val bitmap = BitmapFactory.decodeStream(input)
                if (bitmap != null) {
                    backgroundView?.setImageBitmap(bitmap)
                } else {
                    throw IllegalArgumentException("Bitmap decode failed")
                }
            } ?: throw IllegalArgumentException("Unable to open stream")
        } catch (e: Exception) {
            Toast.makeText(requireContext(), getString(R.string.home_background_load_failed), Toast.LENGTH_SHORT).show()
            HomeBackgroundManager.clearBackgroundUri(requireContext())
            resetToDefaultBackground()
        }
    }

    private fun resetToDefaultBackground() {
        backgroundView?.setImageResource(R.color.color_f2f6f6)
    }

}
