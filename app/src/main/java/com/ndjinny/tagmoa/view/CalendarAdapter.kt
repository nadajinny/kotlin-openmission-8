package com.ndjinny.tagmoa.view

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.ndjinny.tagmoa.R
import java.util.Calendar
import java.util.Date

class CalendarAdapter(
    private val context: Context,
    private val cellHeightPx: Int,
    date: Date
) : RecyclerView.Adapter<CalendarAdapter.CalendarItemHolder>() {

    private val furangCalendar = FurangCalendar(date).apply { initBaseCalendar() }
    private val dataList = ArrayList(furangCalendar.dateList)
    private val displayedMonthCalendar = Calendar.getInstance().apply { time = date }
    private val todayCalendar = Calendar.getInstance()
    private val defaultTextColor = ContextCompat.getColor(context, android.R.color.black)
    private val sundayColor = ContextCompat.getColor(context, R.color.calendar_color_sunday)
    private val saturdayColor = ContextCompat.getColor(context, R.color.calendar_color_saturday)
    private val inactiveColor = ContextCompat.getColor(context, R.color.calendar_color_inactive)

    private val todayDay = todayCalendar.get(Calendar.DAY_OF_MONTH)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarItemHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_calendar_day, parent, false)
        return CalendarItemHolder(view)
    }

    override fun onBindViewHolder(holder: CalendarItemHolder, position: Int) {
        val params = holder.itemView.layoutParams
        val desiredHeight = cellHeightPx
        if (desiredHeight > 0 && params != null && params.height != desiredHeight) {
            params.height = desiredHeight
            holder.itemView.layoutParams = params
        }
        holder.bind(dataList[position], position)
    }

    override fun getItemCount(): Int = dataList.size

    inner class CalendarItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val dayText: TextView = itemView.findViewById(R.id.textCalendarDay)

        fun bind(dayValue: Int, position: Int) {
            dayText.text = dayValue.toString()

            val firstDateIndex = furangCalendar.prevTail
            val lastDateIndex = dataList.size - furangCalendar.nextHead - 1
            val isInactive = position < firstDateIndex || position > lastDateIndex
            val isSameMonthAsToday = displayedMonthCalendar.get(Calendar.YEAR) == todayCalendar.get(Calendar.YEAR) &&
                displayedMonthCalendar.get(Calendar.MONTH) == todayCalendar.get(Calendar.MONTH)
            val isTodayCell = !isInactive && isSameMonthAsToday && dayValue == todayDay

            val dayOfWeek = position % FurangCalendar.DAYS_OF_WEEK
            val textColor = when {
                isInactive -> inactiveColor
                dayOfWeek == 0 -> sundayColor
                dayOfWeek == FurangCalendar.DAYS_OF_WEEK - 1 -> saturdayColor
                else -> defaultTextColor
            }
            dayText.setTextColor(textColor)
            dayText.setTypeface(dayText.typeface, if (isTodayCell) Typeface.BOLD else Typeface.NORMAL)

        }
    }
}
