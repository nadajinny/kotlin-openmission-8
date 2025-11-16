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
import android.util.Log
import androidx.core.content.ContextCompat
import com.ndjinny.tagmoa.R
import com.ndjinny.tagmoa.model.AlarmPreferences
import com.ndjinny.tagmoa.model.MainTask
import com.ndjinny.tagmoa.model.SubTask
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

object TaskReminderScheduler {

    const val CHANNEL_ID = "task_deadline_channel"
    private const val TAG = "TaskReminderScheduler"
    private const val PREFS_NAME = "task_reminder_cache"
    private const val KEY_MAIN_REMINDERS = "main_task_reminders"
    private const val KEY_SUB_REMINDERS = "sub_task_reminders"

    private enum class ReminderType { MAIN, SUB }

    private data class ScheduledReminder(
        val id: String,
        val title: String,
        val triggerAt: Long
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
        Log.d(TAG, "syncMainTaskReminders: tasks=${tasks.size}")
        val timePreference = AlarmPreferences.getMajorAlarmTime(context)
        val reminders = tasks.asSequence()
            .filter { !it.isCompleted }
            .mapNotNull { task ->
                val dueDate = task.dueDate ?: return@mapNotNull null
                val triggerAt = buildTriggerTime(dueDate, timePreference)
                val title = task.title.ifBlank {
                    context.getString(R.string.notification_deadline_title_placeholder)
                }
                ScheduledReminder(task.id, title, triggerAt)
            }
            .toList()
        syncReminders(
            context = context,
            type = ReminderType.MAIN,
            storageKey = KEY_MAIN_REMINDERS,
            isEnabled = AlarmPreferences.isMajorAlarmEnabled(context),
            reminders = reminders
        )
    }

    fun syncSubTaskReminders(context: Context, tasks: List<SubTask>) {
        Log.d(TAG, "syncSubTaskReminders: tasks=${tasks.size}")
        val timePreference = AlarmPreferences.getSubAlarmTime(context)
        val reminders = tasks.asSequence()
            .filter { !it.isCompleted && it.alarmEnabled }
            .mapNotNull { task ->
                val title = task.content.ifBlank {
                    context.getString(R.string.notification_sub_deadline_title_placeholder)
                }
                val triggerAt = task.alarmTimeMillis ?: run {
                    val dueDate = task.dueDate ?: return@mapNotNull null
                    buildTriggerTime(dueDate, timePreference)
                }
                ScheduledReminder(task.id, title, triggerAt)
            }
            .toList()
        syncReminders(
            context = context,
            type = ReminderType.SUB,
            storageKey = KEY_SUB_REMINDERS,
            isEnabled = AlarmPreferences.isSubAlarmEnabled(context),
            reminders = reminders
        )
    }

    fun rescheduleStoredMainReminders(context: Context) {
        rescheduleStoredReminders(
            context = context,
            type = ReminderType.MAIN,
            storageKey = KEY_MAIN_REMINDERS,
            isEnabled = AlarmPreferences.isMajorAlarmEnabled(context)
        )
    }

    fun rescheduleStoredSubReminders(context: Context) {
        rescheduleStoredReminders(
            context = context,
            type = ReminderType.SUB,
            storageKey = KEY_SUB_REMINDERS,
            isEnabled = AlarmPreferences.isSubAlarmEnabled(context)
        )
    }

    fun cancelAllStoredMainReminders(context: Context) {
        cancelAllStoredReminders(context, ReminderType.MAIN, KEY_MAIN_REMINDERS)
    }

    fun cancelAllStoredSubReminders(context: Context) {
        cancelAllStoredReminders(context, ReminderType.SUB, KEY_SUB_REMINDERS)
    }

    private fun syncReminders(
        context: Context,
        type: ReminderType,
        storageKey: String,
        isEnabled: Boolean,
        reminders: List<ScheduledReminder>
    ) {
        ensureChannel(context)
        val hasNotifyPermission = hasNotificationPermission(context)
        val hasExactPermission = ExactAlarmPermissionHelper.hasExactAlarmPermission(context)
        if (!isEnabled || !hasNotifyPermission) {
            Log.w(
                TAG,
                "syncReminders skipped type=$type enabled=$isEnabled notifyPerm=$hasNotifyPermission exactPerm=$hasExactPermission"
            )
            cancelAllStoredReminders(context, type, storageKey)
            return
        }

        val now = System.currentTimeMillis()
        val filtered = reminders.filter { it.triggerAt > now }
        Log.d(
            TAG,
            "syncReminders type=$type total=${reminders.size} filtered=${filtered.size} exactPerm=${ExactAlarmPermissionHelper.hasExactAlarmPermission(context)}"
        )

        val stored = loadStoredReminders(context, storageKey)
        val storedMap = stored.associateBy { it.id }
        val desiredIds = filtered.map { it.id }.toSet()

        stored.forEach { scheduled ->
            if (!desiredIds.contains(scheduled.id)) {
                Log.d(TAG, "Removing reminder id=${scheduled.id}")
                cancelReminder(context, type, scheduled.id, scheduled.title)
            }
        }

        filtered.forEach { scheduled ->
            val existing = storedMap[scheduled.id]
            if (existing == null ||
                existing.triggerAt != scheduled.triggerAt ||
                existing.title != scheduled.title
            ) {
                Log.d(TAG, "Scheduling reminder id=${scheduled.id} triggerAt=${scheduled.triggerAt}")
                scheduleReminder(context, type, scheduled.id, scheduled.title, scheduled.triggerAt)
            } else {
                Log.d(TAG, "Keeping existing reminder id=${scheduled.id}")
            }
        }

        saveStoredReminders(context, storageKey, filtered)
    }

    private fun rescheduleStoredReminders(
        context: Context,
        type: ReminderType,
        storageKey: String,
        isEnabled: Boolean
    ) {
        if (!isEnabled || !hasNotificationPermission(context)) {
            Log.w(TAG, "rescheduleStoredReminders skipped type=$type enabled=$isEnabled")
            return
        }
        ensureChannel(context)
        val stored = loadStoredReminders(context, storageKey)
        stored.forEach {
            Log.d(TAG, "Rescheduling stored reminder id=${it.id}")
            scheduleReminder(context, type, it.id, it.title, it.triggerAt)
        }
    }

    private fun cancelAllStoredReminders(
        context: Context,
        type: ReminderType,
        storageKey: String
    ) {
        val stored = loadStoredReminders(context, storageKey)
        Log.d(TAG, "cancelAllStoredReminders type=$type count=${stored.size}")
        stored.forEach { cancelReminder(context, type, it.id, it.title) }
        saveStoredReminders(context, storageKey, emptyList())
    }

    private fun scheduleReminder(
        context: Context,
        type: ReminderType,
        taskId: String,
        title: String,
        triggerAtMillis: Long
    ) {
        if (taskId.isBlank()) return
        val hasExactPermission = ExactAlarmPermissionHelper.hasExactAlarmPermission(context)
        if (triggerAtMillis <= System.currentTimeMillis()) {
            Log.w(TAG, "Trigger time already passed for $taskId")
            cancelReminder(context, type, taskId)
            return
        }
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = buildPendingIntent(context, type, taskId, title)
        if (hasExactPermission) {
            Log.d(TAG, "Scheduling exact alarm id=$taskId at=$triggerAtMillis title=$title type=$type")
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            Log.w(TAG, "Exact alarm permission missing. Using inexact fallback for $taskId")
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    private fun cancelReminder(
        context: Context,
        type: ReminderType,
        taskId: String,
        title: String? = null
    ) {
        if (taskId.isBlank()) return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = buildPendingIntent(context, type, taskId, title.orEmpty())
        Log.d(TAG, "Cancelling alarm id=$taskId type=$type")
        alarmManager.cancel(pendingIntent)
    }

    private fun buildPendingIntent(
        context: Context,
        type: ReminderType,
        taskId: String,
        title: String
    ): PendingIntent {
        val intent = Intent(context, TaskDeadlineReceiver::class.java).apply {
            putExtra(TaskDeadlineReceiver.EXTRA_TASK_ID, taskId)
            putExtra(TaskDeadlineReceiver.EXTRA_TASK_TITLE, title)
            putExtra(TaskDeadlineReceiver.EXTRA_IS_SUBTASK, type == ReminderType.SUB)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val requestCode = "${type.name}-$taskId".hashCode()
        return PendingIntent.getBroadcast(
            context,
            requestCode,
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

    private fun loadStoredReminders(context: Context, key: String): List<ScheduledReminder> {
        val json = prefs(context).getString(key, "[]") ?: "[]"
        val array = JSONArray(json)
        val list = mutableListOf<ScheduledReminder>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val id = obj.optString("id")
            val title = obj.optString("title")
            val triggerAt = obj.optLong("triggerAt", -1)
            if (id.isNotBlank() && triggerAt > 0) {
                list.add(ScheduledReminder(id, title, triggerAt))
            }
        }
        return list
    }

    private fun saveStoredReminders(
        context: Context,
        key: String,
        reminders: List<ScheduledReminder>
    ) {
        val array = JSONArray()
        reminders.forEach { reminder ->
            val obj = JSONObject().apply {
                put("id", reminder.id)
                put("title", reminder.title)
                put("triggerAt", reminder.triggerAt)
            }
            array.put(obj)
        }
        prefs(context).edit().putString(key, array.toString()).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun hasNotificationPermission(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }
}
