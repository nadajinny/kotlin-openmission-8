package com.ndjinny.tagmoa.controller

import android.Manifest
import android.app.Activity
import android.os.Build
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.ndjinny.tagmoa.R
import com.ndjinny.tagmoa.model.AlarmPreferences

object AlarmPermissionManager {

    private const val REQ_NOTIFICATION_PERMISSION = 1001
    private var promptedNotification = false
    private var promptedExactAlarm = false

    fun ensurePermissions(activity: Activity) {
        val notificationGranted = ensureNotificationPermission(activity)
        val exactGranted = ensureExactAlarmPermission(activity)
        syncAlarmPreferences(activity, enabled = notificationGranted && exactGranted)
    }

    private fun ensureNotificationPermission(activity: Activity): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        val granted = ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.POST_NOTIFICATIONS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!granted && !promptedNotification) {
            promptedNotification = true
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQ_NOTIFICATION_PERMISSION
            )
        }
        return granted
    }

    private fun ensureExactAlarmPermission(activity: Activity): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val hasPermission = ExactAlarmPermissionHelper.hasExactAlarmPermission(activity)
        if (!hasPermission && !promptedExactAlarm) {
            promptedExactAlarm = true
            activity.window?.decorView?.post {
                Toast.makeText(activity, R.string.alarm_exact_permission_required, Toast.LENGTH_SHORT).show()
                ExactAlarmPermissionHelper.requestExactAlarmPermission(activity)
            }
        }
        if (hasPermission) {
            promptedExactAlarm = false
        }
        return hasPermission
    }

    private fun syncAlarmPreferences(activity: Activity, enabled: Boolean) {
        if (enabled) {
            if (!AlarmPreferences.isMajorAlarmEnabled(activity)) {
                AlarmPreferences.setMajorAlarmEnabled(activity, true)
                TaskReminderScheduler.rescheduleStoredMainReminders(activity)
            }
            if (!AlarmPreferences.isSubAlarmEnabled(activity)) {
                AlarmPreferences.setSubAlarmEnabled(activity, true)
                TaskReminderScheduler.rescheduleStoredSubReminders(activity)
            }
        } else {
            if (AlarmPreferences.isMajorAlarmEnabled(activity)) {
                AlarmPreferences.setMajorAlarmEnabled(activity, false)
                TaskReminderScheduler.cancelAllStoredMainReminders(activity)
            }
            if (AlarmPreferences.isSubAlarmEnabled(activity)) {
                AlarmPreferences.setSubAlarmEnabled(activity, false)
                TaskReminderScheduler.cancelAllStoredSubReminders(activity)
            }
        }
    }
}
