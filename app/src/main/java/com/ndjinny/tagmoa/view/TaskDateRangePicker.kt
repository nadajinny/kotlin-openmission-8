package com.ndjinny.tagmoa.view

import android.content.Context
import android.view.LayoutInflater
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.ndjinny.tagmoa.R
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import com.prolificinteractive.materialcalendarview.MaterialCalendarView
import com.prolificinteractive.materialcalendarview.format.ArrayWeekDayFormatter
import java.util.Calendar

object TaskDateRangePicker {

    fun show(
        context: Context,
        initialStartDateMillis: Long?,
        initialEndDateMillis: Long?,
        onRangeSelected: (Long?, Long?) -> Unit
    ) {
        val dialogView = LayoutInflater.from(context)
            .inflate(R.layout.dialog_task_date_range_picker, null, false)
        val calendarView: MaterialCalendarView = dialogView.findViewById(R.id.dialogCalendarView)
        val selectionLabel: TextView = dialogView.findViewById(R.id.textCalendarSelectedRange)
        val btnConfirm: Button = dialogView.findViewById(R.id.btnCalendarConfirm)
        val btnCancel: Button = dialogView.findViewById(R.id.btnCalendarCancel)

        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .create()

        var selectedStartDay = initialStartDateMillis?.toCalendarDay()
        var selectedEndDay = initialEndDateMillis?.toCalendarDay()

        calendarView.setWeekDayFormatter(
            ArrayWeekDayFormatter(context.resources.getTextArray(R.array.custom_weekdays))
        )
        calendarView.setHeaderTextAppearance(R.style.CalendarWidgetHeader)
        calendarView.setTitleFormatter { day ->
            context.getString(
                R.string.calendar_header_format,
                day.year,
                day.month
            )
        }
        applyDecorators(calendarView, context, CalendarDay.today().month)

        if (selectedStartDay != null && selectedEndDay != null) {
            calendarView.selectRange(selectedStartDay, selectedEndDay)
        } else if (selectedStartDay != null) {
            calendarView.setDateSelected(selectedStartDay, true)
        }

        fun updateSelectionLabel() {
            val startMillis = selectedStartDay?.toEpochMillis()
            val endMillis = selectedEndDay?.toEpochMillis()
            val label = formatDateRange(startMillis, endMillis)
            selectionLabel.text = if (label.isEmpty()) {
                context.getString(R.string.label_select_period_placeholder)
            } else {
                label
            }
            btnConfirm.isEnabled = selectedStartDay != null
        }

        calendarView.setOnDateChangedListener { _, date, selected ->
            if (selected) {
                selectedStartDay = date
                selectedEndDay = null
            } else {
                selectedStartDay = null
                selectedEndDay = null
            }
            updateSelectionLabel()
        }

        calendarView.setOnRangeSelectedListener { _, dates ->
            if (dates.isNotEmpty()) {
                selectedStartDay = dates.first()
                selectedEndDay = dates.last()
            } else {
                selectedEndDay = null
            }
            updateSelectionLabel()
        }

        calendarView.setOnMonthChangedListener { _, date ->
            applyDecorators(calendarView, context, date.month)
        }

        updateSelectionLabel()

        btnCancel.setOnClickListener { dialog.dismiss() }
        btnConfirm.setOnClickListener {
            onRangeSelected(
                selectedStartDay?.toEpochMillis(),
                selectedEndDay?.toEpochMillis()
            )
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun applyDecorators(
        calendarView: MaterialCalendarView,
        context: Context,
        selectedMonth: Int
    ) {
        calendarView.removeDecorators()
        val decorators = listOf(
            DaySelectorDecorator(context),
            TodayDecorator(context),
            SundayDecorator(context),
            SaturdayDecorator(context),
            SelectedMonthDecorator(context, selectedMonth)
        )
        calendarView.addDecorators(decorators)
        calendarView.invalidateDecorators()
    }
}

private class DaySelectorDecorator(context: Context) : DayViewDecorator {
    private val drawable = ContextCompat.getDrawable(context, R.drawable.calendar_selector)

    override fun shouldDecorate(day: CalendarDay): Boolean = true

    override fun decorate(view: DayViewFacade) {
        drawable?.let { view.setSelectionDrawable(it) }
    }
}

private class TodayDecorator(context: Context) : DayViewDecorator {
    private val drawable = ContextCompat.getDrawable(context, R.drawable.calendar_circle_gray)
    private val today = CalendarDay.today()

    override fun shouldDecorate(day: CalendarDay): Boolean = day == today

    override fun decorate(view: DayViewFacade) {
        drawable?.let { view.setBackgroundDrawable(it) }
    }
}

private class SelectedMonthDecorator(
    context: Context,
    private val selectedMonth: Int
) : DayViewDecorator {
    private val inactiveColor =
        ContextCompat.getColor(context, R.color.calendar_color_inactive)

    override fun shouldDecorate(day: CalendarDay): Boolean = day.month != selectedMonth

    override fun decorate(view: DayViewFacade) {
        view.addSpan(android.text.style.ForegroundColorSpan(inactiveColor))
    }
}

private class SundayDecorator(context: Context) : DayViewDecorator {
    private val sundayColor =
        ContextCompat.getColor(context, R.color.calendar_color_sunday)

    override fun shouldDecorate(day: CalendarDay): Boolean {
        return day.dayOfWeek() == Calendar.SUNDAY
    }

    override fun decorate(view: DayViewFacade) {
        view.addSpan(android.text.style.ForegroundColorSpan(sundayColor))
    }
}

private class SaturdayDecorator(context: Context) : DayViewDecorator {
    private val saturdayColor =
        ContextCompat.getColor(context, R.color.calendar_color_saturday)

    override fun shouldDecorate(day: CalendarDay): Boolean {
        return day.dayOfWeek() == Calendar.SATURDAY
    }

    override fun decorate(view: DayViewFacade) {
        view.addSpan(android.text.style.ForegroundColorSpan(saturdayColor))
    }
}

private fun CalendarDay.toEpochMillis(): Long {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month - 1)
        set(Calendar.DAY_OF_MONTH, day)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return calendar.timeInMillis
}

private fun CalendarDay.dayOfWeek(): Int {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month - 1)
        set(Calendar.DAY_OF_MONTH, day)
    }
    return calendar.get(Calendar.DAY_OF_WEEK)
}

private fun Long.toCalendarDay(): CalendarDay {
    val calendar = Calendar.getInstance().apply { timeInMillis = this@toCalendarDay }
    return CalendarDay.from(
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH) + 1,
        calendar.get(Calendar.DAY_OF_MONTH)
    )
}
