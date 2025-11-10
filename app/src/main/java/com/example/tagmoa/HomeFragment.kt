package com.example.tagmoa

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import java.util.Calendar

class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var recyclerMainDue: RecyclerView
    private lateinit var recyclerSubDue: RecyclerView
    private lateinit var textMainEmpty: TextView
    private lateinit var textSubEmpty: TextView

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

        recyclerMainDue = view.findViewById(R.id.recyclerHomeMainDue)
        recyclerSubDue = view.findViewById(R.id.recyclerHomeSubDue)
        textMainEmpty = view.findViewById(R.id.textHomeMainDueEmpty)
        textSubEmpty = view.findViewById(R.id.textHomeSubDueEmpty)

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

        observeTasks()
        observeSubTasks()
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
                    task.id = taskId
                    taskMap[taskId] = task
                    if (isDueToday(task.dueDate)) {
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
                        if (isDueToday(subTask.dueDate)) {
                            val parentTitle = taskMap[mainId]?.title ?: getString(R.string.label_no_title)
                            result.add(
                                DueSubTaskItem(
                                    id = subId,
                                    mainTaskId = mainId,
                                    content = subTask.content,
                                    parentTitle = parentTitle,
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

    private fun isDueToday(dueDate: Long?): Boolean {
        if (dueDate == null) return false
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
        return dueDate in startCal.timeInMillis..endCal.timeInMillis
    }

    override fun onDestroyView() {
        super.onDestroyView()
        tasksListener?.let { tasksRef.removeEventListener(it) }
        subTasksListener?.let { subTasksRef.removeEventListener(it) }
    }
}
