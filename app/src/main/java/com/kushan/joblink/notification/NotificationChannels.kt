package com.kushan.joblink.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.kushan.joblink.R

object NotificationChannels {
    const val JOB_UPDATES = "job_updates"

    fun create(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            JOB_UPDATES,
            context.getString(R.string.notification_channel_job_updates),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notification_channel_job_updates_description)
        }

        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }
}
