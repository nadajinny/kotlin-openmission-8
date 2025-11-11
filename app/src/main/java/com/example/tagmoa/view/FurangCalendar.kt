package com.example.tagmoa.view

import java.util.Calendar
import java.util.Date

/**
 * Utility that produces a flattened list of dates for the month view:
 * [prev month tail] + [current month days] + [next month head] = 6 * 7 cells.
 */
class FurangCalendar(date: Date) {

    companion object {
        const val DAYS_OF_WEEK = 7
        const val ROWS_OF_CALENDAR = 6
        const val TOTAL_CELLS = DAYS_OF_WEEK * ROWS_OF_CALENDAR
    }

    private val calendar: Calendar = Calendar.getInstance().apply { time = date }

    var prevTail: Int = 0
        private set
    var nextHead: Int = 0
        private set
    var currentMaxDate: Int = 0
        private set

    val dateList: ArrayList<Int> = arrayListOf()

    fun initBaseCalendar() {
        dateList.clear()
        calendar.set(Calendar.DAY_OF_MONTH, 1)

        currentMaxDate = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        prevTail = calendar.get(Calendar.DAY_OF_WEEK) - 1

        appendPrevTail()
        appendCurrentMonth()

        val usedCells = prevTail + currentMaxDate
        nextHead = TOTAL_CELLS - usedCells
        appendNextHead()
    }

    private fun appendPrevTail() {
        if (prevTail == 0) return
        val tempCal = calendar.clone() as Calendar
        tempCal.add(Calendar.MONTH, -1)
        val maxDate = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val start = maxDate - prevTail + 1
        for (day in start..maxDate) {
            dateList.add(day)
        }
    }

    private fun appendCurrentMonth() {
        for (day in 1..currentMaxDate) {
            dateList.add(day)
        }
    }

    private fun appendNextHead() {
        val tailCount = TOTAL_CELLS - dateList.size
        for (day in 1..tailCount) {
            dateList.add(day)
        }
    }
}
