package com.ndjinny.tagmoa.controller

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ndjinny.tagmoa.R
import com.ndjinny.tagmoa.view.CalendarAdapter
import com.ndjinny.tagmoa.view.FurangCalendar
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MonthlyCalendarFragment : Fragment(R.layout.fragment_calendar_page) {

    private var pagePosition: Int = START_POSITION

    private lateinit var yearMonthText: TextView
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pagePosition = arguments?.getInt(ARG_POSITION, START_POSITION) ?: START_POSITION
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        yearMonthText = view.findViewById(R.id.textCalendarYearMonth)
        recyclerView = view.findViewById(R.id.recyclerCalendar)

        recyclerView.setHasFixedSize(true)
        recyclerView.itemAnimator = null
        recyclerView.isNestedScrollingEnabled = false
        val gridLayoutManager = object : GridLayoutManager(requireContext(), FurangCalendar.DAYS_OF_WEEK) {
            override fun canScrollVertically(): Boolean = false
        }
        recyclerView.layoutManager = gridLayoutManager

        val targetDate = resolveTargetDate()
        renderYearMonth(targetDate)

        recyclerView.doOnPreDraw {
            if (recyclerView.adapter == null) {
                val availableHeight = recyclerView.height
                val defaultHeight = resources.getDimensionPixelSize(R.dimen.calendar_min_cell_height)
                val cellHeight = (availableHeight / FurangCalendar.ROWS_OF_CALENDAR).takeIf { it > 0 } ?: defaultHeight
                recyclerView.adapter = CalendarAdapter(requireContext(), cellHeight, targetDate)
            }
        }
    }

    private fun resolveTargetDate(): Date {
        val offset = pagePosition - START_POSITION
        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, offset)
        }
        return calendar.time
    }

    private fun renderYearMonth(date: Date) {
        val formatter = SimpleDateFormat(getString(R.string.calendar_year_month_format), Locale.KOREA)
        yearMonthText.text = formatter.format(date)
    }

    companion object {
        private const val ARG_POSITION = "arg_position"
        const val START_POSITION: Int = Int.MAX_VALUE / 2

        fun newInstance(position: Int): MonthlyCalendarFragment {
            return MonthlyCalendarFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_POSITION, position)
                }
            }
        }
    }
}
