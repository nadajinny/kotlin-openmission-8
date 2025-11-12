package com.example.tagmoa.controller

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tagmoa.R
import com.example.tagmoa.model.MainTask
import com.example.tagmoa.model.UserDatabase
import com.example.tagmoa.view.CalendarDecorators
import com.example.tagmoa.view.CalendarScheduleAdapter
import com.example.tagmoa.view.FurangCalendar
import com.example.tagmoa.view.overlapsWithRange
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.MaterialCalendarView
import com.prolificinteractive.materialcalendarview.format.ArrayWeekDayFormatter
import java.util.Calendar

class CalendarFragment : Fragment(R.layout.fragment_calendar) {

    private lateinit var calendarView: MaterialCalendarView
    private lateinit var recyclerSchedule: RecyclerView
    private lateinit var emptyState: TextView
    private lateinit var dateText: TextView

    private val scheduleAdapter = CalendarScheduleAdapter { task ->
        if (task.id.isBlank()) return@CalendarScheduleAdapter
        val intent = Intent(requireContext(), MainTaskDetailActivity::class.java).apply {
            putExtra(MainTaskDetailActivity.EXTRA_TASK_ID, task.id)
        }
        startActivity(intent)
    }

    private lateinit var tasksRef: DatabaseReference
    private var tasksListener: ValueEventListener? = null

    private lateinit var dayDecorator: DayViewDecorator
    private lateinit var todayDecorator: DayViewDecorator
    private lateinit var sundayDecorator: DayViewDecorator
    private lateinit var saturdayDecorator: DayViewDecorator
    private lateinit var selectedMonthDecorator: DayViewDecorator
    private var eventDecorator: DayViewDecorator? = null

    private val allTasks = mutableListOf<MainTask>()
    private var selectedDay: CalendarDay = CalendarDay.today()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val uid = requireUserIdOrRedirect() ?: return
        tasksRef = UserDatabase.tasksRef(uid)

        calendarView = view.findViewById(R.id.calendarView)
        recyclerSchedule = view.findViewById(R.id.recyclerCalendarSchedule)
        emptyState = view.findViewById(R.id.textCalendarEmpty)
        dateText = view.findViewById(R.id.textCalendarSelectedDate)

        recyclerSchedule.layoutManager = LinearLayoutManager(requireContext())
        recyclerSchedule.adapter = scheduleAdapter
        recyclerSchedule.isNestedScrollingEnabled = false

        setupCalendarView()
        observeTasks()
    }

    private fun setupCalendarView() {
        dayDecorator = CalendarDecorators.dayDecorator(requireContext())
        todayDecorator = CalendarDecorators.todayDecorator(requireContext())
        sundayDecorator = CalendarDecorators.sundayDecorator(requireContext())
        saturdayDecorator = CalendarDecorators.saturdayDecorator(requireContext())
        selectedMonthDecorator = CalendarDecorators.selectedMonthDecorator(requireContext(), selectedDay.month)

        calendarView.addDecorators(
            dayDecorator,
            todayDecorator,
            sundayDecorator,
            saturdayDecorator,
            selectedMonthDecorator
        )
        calendarView.setWeekDayFormatter(ArrayWeekDayFormatter(resources.getTextArray(R.array.custom_weekdays)))
        calendarView.setHeaderTextAppearance(R.style.CalendarWidgetHeader)
        calendarView.setDateSelected(selectedDay, true)
        updateSelectedDate(selectedDay)
        calendarView.doOnLayout {
            val availableWidth = it.width - it.paddingLeft - it.paddingRight
            if (availableWidth > 0) {
                val tileWidth = (availableWidth / com.example.tagmoa.view.FurangCalendar.DAYS_OF_WEEK)
                calendarView.setTileWidth(tileWidth)
                calendarView.setTileHeight((tileWidth * 1.1f).toInt())
            }
        }

        calendarView.setOnMonthChangedListener { widget, date ->
            updateSelectedMonthDecorator(date.month)
            widget.clearSelection()
            val firstDay = CalendarDay.from(date.year, date.month, 1)
            widget.setDateSelected(firstDay, true)
            updateSelectedDate(firstDay)
        }
        calendarView.setOnDateChangedListener { _, date, selected ->
            if (selected) {
                updateSelectedDate(date)
            }
        }
    }

    private fun updateSelectedMonthDecorator(month: Int) {
        calendarView.removeDecorator(selectedMonthDecorator)
        selectedMonthDecorator = CalendarDecorators.selectedMonthDecorator(requireContext(), month)
        calendarView.addDecorator(selectedMonthDecorator)
    }

    private fun updateSelectedDate(date: CalendarDay) {
        selectedDay = date
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, date.year)
            set(Calendar.MONTH, date.month - 1)
            set(Calendar.DAY_OF_MONTH, date.day)
        }
        val dayLabel = when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> getString(R.string.calendar_week_mon)
            Calendar.TUESDAY -> getString(R.string.calendar_week_tue)
            Calendar.WEDNESDAY -> getString(R.string.calendar_week_wed)
            Calendar.THURSDAY -> getString(R.string.calendar_week_thu)
            Calendar.FRIDAY -> getString(R.string.calendar_week_fri)
            Calendar.SATURDAY -> getString(R.string.calendar_week_sat)
            Calendar.SUNDAY -> getString(R.string.calendar_week_sun)
            else -> ""
        }
        dateText.text = getString(R.string.calendar_selected_date_format, date.month, date.day, dayLabel)
        filterSchedulesForSelectedDate()
    }

    private fun observeTasks() {
        tasksListener?.let { tasksRef.removeEventListener(it) }
        tasksListener = tasksRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tasks = mutableListOf<MainTask>()
                for (child in snapshot.children) {
                    val task = child.getValue(MainTask::class.java) ?: continue
                    val taskId = task.id.ifBlank { child.key.orEmpty() }
                    task.id = taskId
                    tasks.add(task)
                }
                allTasks.clear()
                allTasks.addAll(tasks)
                refreshEventDecorator()
                filterSchedulesForSelectedDate()
            }

            override fun onCancelled(error: DatabaseError) {
                allTasks.clear()
                scheduleAdapter.submitList(emptyList())
                emptyState.visibility = View.VISIBLE
            }
        })
    }

    private fun refreshEventDecorator() {
        if (!this::calendarView.isInitialized) return
        eventDecorator?.let { calendarView.removeDecorator(it) }
        if (allTasks.isEmpty()) {
            eventDecorator = null
            calendarView.invalidateDecorators()
            return
        }
        eventDecorator = CalendarDecorators.eventDecorator(requireContext(), allTasks)
        calendarView.addDecorator(eventDecorator)
        calendarView.invalidateDecorators()
    }

    private fun filterSchedulesForSelectedDate() {
        val targetStart = selectedDayAtStartMillis()
        val targetEnd = selectedDayAtEndMillis()
        val filtered = allTasks.filter { task ->
            overlapsWithRange(task.startDate, task.endDate, targetStart, targetEnd, task.dueDate)
        }
        scheduleAdapter.submitList(filtered)
        if (filtered.isEmpty()) {
            emptyState.visibility = View.VISIBLE
            recyclerSchedule.visibility = View.GONE
        } else {
            emptyState.visibility = View.GONE
            recyclerSchedule.visibility = View.VISIBLE
        }
    }

    private fun selectedDayAtStartMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(selectedDay.year, selectedDay.month - 1, selectedDay.day, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun selectedDayAtEndMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(selectedDay.year, selectedDay.month - 1, selectedDay.day, 23, 59, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }

    override fun onDestroyView() {
        super.onDestroyView()
        tasksListener?.let { tasksRef.removeEventListener(it) }
    }
}
