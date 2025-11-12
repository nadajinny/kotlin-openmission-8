package com.example.tagmoa.model

/**
 * Ensures legacy tasks (saved before manualSchedule flag was introduced)
 * keep being treated as manually scheduled when an explicit end date exists.
 */
fun MainTask.ensureManualScheduleFlag() {
    if (!manualSchedule && endDate != null) {
        manualSchedule = true
    }
}
