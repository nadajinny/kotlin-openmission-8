package com.example.tagmoa.controller

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class CalendarPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    val firstFragmentPosition: Int = MonthlyCalendarFragment.START_POSITION

    override fun getItemCount(): Int = Int.MAX_VALUE

    override fun createFragment(position: Int): Fragment {
        return MonthlyCalendarFragment.newInstance(position)
    }
}
