package com.example.tagmoa.view

import android.content.Context
import android.text.style.ForegroundColorSpan
import androidx.core.content.ContextCompat
import com.example.tagmoa.R
import com.example.tagmoa.model.MainTask
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import com.prolificinteractive.materialcalendarview.spans.DotSpan
import java.util.Calendar

object CalendarDecorators {

    fun dayDecorator(context: Context): DayViewDecorator {
        return object : DayViewDecorator {
            private val drawable = ContextCompat.getDrawable(context, R.drawable.calendar_selector)
            override fun shouldDecorate(day: CalendarDay): Boolean = true
            override fun decorate(view: DayViewFacade) {
                drawable?.let { view.setSelectionDrawable(it) }
            }
        }
    }

    fun todayDecorator(context: Context): DayViewDecorator {
        return object : DayViewDecorator {
            private val drawable = ContextCompat.getDrawable(context, R.drawable.calendar_circle_today)
            private val today = CalendarDay.today()

            override fun shouldDecorate(day: CalendarDay): Boolean = day == today

            override fun decorate(view: DayViewFacade) {
                drawable?.let { view.setBackgroundDrawable(it) }
                view.addSpan(ForegroundColorSpan(ContextCompat.getColor(context, android.R.color.white)))
            }
        }
    }

    fun selectedMonthDecorator(context: Context, selectedMonth: Int): DayViewDecorator {
        return object : DayViewDecorator {
            private val disabledColor = ContextCompat.getColor(context, R.color.enabled_date_color)
            override fun shouldDecorate(day: CalendarDay): Boolean = day.month != selectedMonth
            override fun decorate(view: DayViewFacade) {
                view.addSpan(ForegroundColorSpan(disabledColor))
            }
        }
    }

    fun sundayDecorator(context: Context): DayViewDecorator {
        return dayOfWeekDecorator(context, Calendar.SUNDAY, R.color.calendar_color_sunday)
    }

    fun saturdayDecorator(context: Context): DayViewDecorator {
        return dayOfWeekDecorator(context, Calendar.SATURDAY, R.color.calendar_color_saturday)
    }

    private fun dayOfWeekDecorator(
        context: Context,
        targetDayOfWeek: Int,
        colorRes: Int
    ): DayViewDecorator {
        return object : DayViewDecorator {
            private val color = ContextCompat.getColor(context, colorRes)
            override fun shouldDecorate(day: CalendarDay): Boolean {
                val calendar = Calendar.getInstance()
                calendar.set(day.year, day.month - 1, day.day)
                return calendar.get(Calendar.DAY_OF_WEEK) == targetDayOfWeek
            }

            override fun decorate(view: DayViewFacade) {
                view.addSpan(ForegroundColorSpan(color))
            }
        }
    }

    fun eventDecorator(context: Context, tasks: List<MainTask>): DayViewDecorator {
        return object : DayViewDecorator {
            private val eventDates = buildEventDates(tasks)
            private val dotColor = ContextCompat.getColor(context, R.color.main_color)

            override fun shouldDecorate(day: CalendarDay): Boolean = eventDates.contains(day)

            override fun decorate(view: DayViewFacade) {
                view.addSpan(DotSpan(6f, dotColor))
            }

            private fun buildEventDates(tasks: List<MainTask>): HashSet<CalendarDay> {
                val dates = hashSetOf<CalendarDay>()
                tasks.forEach { task ->
                    val (startMillis, endMillis) = normalizeDateRange(task.startDate, task.endDate, task.dueDate)
                    val start = startMillis ?: return@forEach
                    val end = endMillis ?: start
                    val cursor = start.asStartOfDay()
                    val endCal = end.asStartOfDay()
                    while (!cursor.after(endCal)) {
                        dates.add(
                            CalendarDay.from(
                                cursor.get(Calendar.YEAR),
                                cursor.get(Calendar.MONTH) + 1,
                                cursor.get(Calendar.DAY_OF_MONTH)
                            )
                        )
                        cursor.add(Calendar.DAY_OF_MONTH, 1)
                    }
                }
                return dates
            }

            private fun Long.asStartOfDay(): Calendar {
                return Calendar.getInstance().apply {
                    timeInMillis = this@asStartOfDay
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
            }
        }
    }
}
