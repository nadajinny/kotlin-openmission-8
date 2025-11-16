package com.ndjinny.tagmoa.controller

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.ndjinny.tagmoa.R
import com.ndjinny.tagmoa.model.AlarmPreferences
import com.ndjinny.tagmoa.model.MainTask
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

object TaskReminderScheduler {

    const val CHANNEL_ID = "task_deadline_channel"
    private const val PREFS_NAME = "task_reminder_cache"
    private const val KEY_MAIN_REMINDERS = "main_task_reminders"

    private data class ScheduledMainTask(
        val id: String,
        val title: String,
        val dueDate: Long
    )

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channelName = context.getString(R.string.notification_channel_task_deadline)
        val channelDescription = context.getString(R.string.notification_channel_task_deadline_desc)
        val channel = NotificationChannel(
            CHANNEL_ID,
            channelName,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = channelDescription
        }
        manager.createNotificationChannel(channel)
    }

    fun syncMainTaskReminders(context: Context, tasks: List<MainTask>) {
        ensureChannel(context)
        if (!AlarmPreferences.isMajorAlarmEnabled(context) || !hasPermission(context)) {
            cancelAllStoredReminders(context)
            return
        }

        val timePreference = AlarmPreferences.getMajorAlarmTime(context)
        val now = System.currentTimeMillis()
        val desired = tasks.asSequence()
            .filter { !it.isCompleted }
            .mapNotNull { task ->
                val dueDate = task.dueDate ?: return@mapNotNull null
                val triggerAt = buildTriggerTime(dueDate, timePreference)
                if (triggerAt <= now) return@mapNotNull null
                val title = task.title.ifBlank {
                    context.getString(R.string.notification_deadline_title_placeholder)
                }
                ScheduledMainTask(task.id, title, dueDate)
            }
            .toList()

        val stored = loadStoredReminders(context)
        val storedMap = stored.associateBy { it.id }
        val desiredIds = desired.map { it.id }.toSet()

        stored.forEach { scheduled ->
            if (!desiredIds.contains(scheduled.id)) {
                cancelMainTaskReminder(context, scheduled.id, scheduled.title)
            }
        }

        desired.forEach { scheduled ->
            val existing = storedMap[scheduled.id]
            if (existing == null || existing.dueDate != scheduled.dueDate || existing.title != scheduled.title) {
                scheduleReminder(context, scheduled.id, scheduled.title, scheduled.dueDate, timePreference)
            }
        }

        saveStoredReminders(context, desired)
    }

    fun rescheduleStoredReminders(context: Context) {
        if (!AlarmPreferences.isMajorAlarmEnabled(context) || !hasPermission(context)) return
        ensureChannel(context)
        val stored = loadStoredReminders(context)
        stored.forEach { scheduleReminder(context, it.id, it.title, it.dueDate) }
    }

    fun cancelAllStoredReminders(context: Context) {
        val stored = loadStoredReminders(context)
        stored.forEach { cancelMainTaskReminder(context, it.id, it.title) }
        saveStoredReminders(context, emptyList())
    }

    private fun scheduleReminder(
        context: Context,
        taskId: String,
        title: String,
        dueDateMillis: Long,
        timeValue: String = AlarmPreferences.getMajorAlarmTime(context)
    ) {
        if (taskId.isBlank()) return
        val triggerAt = buildTriggerTime(dueDateMillis, timeValue)
        if (triggerAt <= System.currentTimeMillis()) {
            cancelMainTaskReminder(context, taskId)
            return
        }
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = buildPendingIntent(context, taskId, title)
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
    }

    private fun cancelMainTaskReminder(context: Context, taskId: String, title: String? = null) {
        if (taskId.isBlank()) return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = buildPendingIntent(context, taskId, title.orEmpty())
        alarmManager.cancel(pendingIntent)
    }

    private fun buildPendingIntent(context: Context, taskId: String, title: String): PendingIntent {
        val intent = Intent(context, TaskDeadlineReceiver::class.java).apply {
            putExtra(TaskDeadlineReceiver.EXTRA_TASK_ID, taskId)
            putExtra(TaskDeadlineReceiver.EXTRA_TASK_TITLE, title)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(
            context,
            taskId.hashCode(),
            intent,
            flags
        )
    }

    private fun buildTriggerTime(dateMillis: Long, timeValue: String): Long {
        val parts = timeValue.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 8
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val calendar = Calendar.getInstance().apply {
            timeInMillis = dateMillis
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    private fun loadStoredReminders(context: Context): List<ScheduledMainTask> {
        val json = prefs(context).getString(KEY_MAIN_REMINDERS, "[]") ?: "[]"
        val array = JSONArray(json)
        val list = mutableListOf<ScheduledMainTask>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val id = obj.optString("id")
            val title = obj.optString("title")
            val dueDate = obj.optLong("dueDate", -1)
            if (id.isNotBlank() && dueDate > 0) {
                list.add(ScheduledMainTask(id, title, dueDate))
            }
        }
        return list
    }

    private fun saveStoredReminders(context: Context, reminders: List<ScheduledMainTask>) {
        val array = JSONArray()
        reminders.forEach { reminder ->
            val obj = JSONObject().apply {
                put("id", reminder.id)
                put("title", reminder.title)
                put("dueDate", reminder.dueDate)
            }
            array.put(obj)
        }
        prefs(context).edit().putString(KEY_MAIN_REMINDERS, array.toString()).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun hasPermission(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }
}
