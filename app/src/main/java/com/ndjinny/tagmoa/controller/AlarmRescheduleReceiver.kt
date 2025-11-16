package com.ndjinny.tagmoa.controller

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class AlarmRescheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        Log.d("AlarmRescheduleReceiver", "Received ${intent?.action}, rescheduling reminders")
        TaskReminderScheduler.rescheduleStoredMainReminders(context)
        TaskReminderScheduler.rescheduleStoredSubReminders(context)
    }
}
