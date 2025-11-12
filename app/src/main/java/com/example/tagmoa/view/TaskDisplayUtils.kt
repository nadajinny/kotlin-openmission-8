package com.example.tagmoa.view

import android.content.Context
import com.example.tagmoa.R
import com.example.tagmoa.model.MainTask

fun MainTask.buildScheduleLabel(context: Context): String {
    val dateLabel = formatDateRange(startDate, endDate, dueDate)
    val statusLabel = when {
        !manualSchedule && dueDate == null -> context.getString(R.string.status_in_progress)
        !manualSchedule && dueDate != null -> context.getString(R.string.status_completed)
        else -> null
    }
    return when {
        dateLabel.isNotEmpty() && statusLabel != null ->
            context.getString(R.string.label_with_date_and_status, dateLabel, statusLabel)
        dateLabel.isNotEmpty() -> context.getString(R.string.label_with_date, dateLabel)
        statusLabel != null -> context.getString(R.string.label_status_only, statusLabel)
        else -> context.getString(R.string.label_no_date)
    }
}
