package com.kushan.joblink.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.kushan.joblink.MainActivity

object NotificationIntentFactory {
    fun create(context: Context, notification: JobLinkNotification): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_OPEN_NOTIFICATION
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(JobLinkNotificationParser.EVENT_KEY, notification.event.name)
            notification.applicationId?.let {
                putExtra(JobLinkNotificationParser.APPLICATION_ID_KEY, it)
            }
            notification.jobId?.let {
                putExtra(JobLinkNotificationParser.JOB_ID_KEY, it)
            }
        }

        val requestCode = listOf(
            notification.event.name,
            notification.applicationId,
            notification.jobId,
        ).joinToString(separator = ":").hashCode()

        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private const val ACTION_OPEN_NOTIFICATION =
        "com.kushan.joblink.action.OPEN_NOTIFICATION"
}
