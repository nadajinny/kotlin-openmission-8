package com.example.tagmoa.controller

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.example.tagmoa.R

class CalendarFragment : Fragment(R.layout.fragment_calendar) {

    private lateinit var calendarViewPager: ViewPager2

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        calendarViewPager = view.findViewById(R.id.calendarViewPager)
        val adapter = CalendarPagerAdapter(this)
        calendarViewPager.adapter = adapter
        calendarViewPager.orientation = ViewPager2.ORIENTATION_VERTICAL
        calendarViewPager.setCurrentItem(adapter.firstFragmentPosition, false)
    }
}
