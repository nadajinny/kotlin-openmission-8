package com.ndjinny.tagmoa.controller

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import com.ndjinny.tagmoa.R

class TaskDeadlineReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
        val taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE).orEmpty()
        val isSubTask = intent.getBooleanExtra(EXTRA_IS_SUBTASK, false)

        val placeholderRes = if (isSubTask) {
            R.string.notification_sub_deadline_title_placeholder
        } else {
            R.string.notification_deadline_title_placeholder
        }
        val scheduleName = taskTitle.takeIf { it.isNotBlank() }
            ?: context.getString(placeholderRes)

        val titleRes = if (isSubTask) {
            R.string.notification_sub_deadline_title
        } else {
            R.string.notification_deadline_title
        }
        val bodyRes = if (isSubTask) {
            R.string.notification_sub_deadline_body
        } else {
            R.string.notification_deadline_body
        }

        val notificationTitle = context.getString(titleRes, scheduleName)
        val contentText = context.getString(bodyRes, scheduleName)
        val launchIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(launchIntent)
            getPendingIntent(taskId.hashCode(), PENDING_INTENT_FLAGS)
        }

        TaskReminderScheduler.ensureChannel(context)

        val notification = NotificationCompat.Builder(context, TaskReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_nav_tasks)
            .setContentTitle(notificationTitle)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(taskId.hashCode(), notification)
    }

    companion object {
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_TITLE = "extra_task_title"
        const val EXTRA_IS_SUBTASK = "extra_is_subtask"

        private const val PENDING_INTENT_FLAGS =
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    }
}
