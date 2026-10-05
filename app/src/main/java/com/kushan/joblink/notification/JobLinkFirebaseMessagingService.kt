package com.kushan.joblink.notification

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.kushan.joblink.JobLinkApplication
import com.kushan.joblink.R

// The current FCM API replaces the legacy onNewToken callback with onRegistered.
@SuppressLint("MissingFirebaseInstanceTokenRefresh")
class JobLinkFirebaseMessagingService : FirebaseMessagingService() {
    override fun onRegistered(installationId: String) {
        Log.d(TAG, "FCM app instance registered; syncing its installation ID.")
        (application as JobLinkApplication).saveMessagingInstallationId(installationId)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val notification = JobLinkNotificationParser.parse(
            data = message.data,
            fallbackTitle = message.notification?.title,
            fallbackBody = message.notification?.body,
        )

        if (notification == null) {
            Log.w(TAG, "Ignoring FCM message with an unknown or missing event type.")
            return
        }

        showNotification(notification, message.messageId)
    }

    private fun showNotification(notification: JobLinkNotification, messageId: String?) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.i(TAG, "Notification received, but notification permission is not granted.")
            return
        }

        val title = notification.title ?: defaultTitle(notification.event)
        val body = notification.body ?: defaultBody(notification.event)
        val systemNotification = NotificationCompat.Builder(this, NotificationChannels.JOB_UPDATES)
            .setSmallIcon(R.drawable.ic_joblink_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(NotificationIntentFactory.create(this, notification))
            .build()

        NotificationManagerCompat.from(this).notify(
            messageId?.hashCode() ?: notification.hashCode(),
            systemNotification,
        )
    }

    private fun defaultTitle(event: JobLinkNotificationEvent): String = getString(
        when (event) {
            JobLinkNotificationEvent.APPLICATION_REVIEWED -> R.string.notification_application_reviewed_title
            JobLinkNotificationEvent.SHORTLISTED -> R.string.notification_shortlisted_title
            JobLinkNotificationEvent.INTERVIEW -> R.string.notification_interview_title
            JobLinkNotificationEvent.JOB_OFFER -> R.string.notification_job_offer_title
            JobLinkNotificationEvent.NEW_MATCHING_JOB -> R.string.notification_new_matching_job_title
        },
    )

    private fun defaultBody(event: JobLinkNotificationEvent): String = getString(
        when (event) {
            JobLinkNotificationEvent.APPLICATION_REVIEWED -> R.string.notification_application_reviewed_body
            JobLinkNotificationEvent.SHORTLISTED -> R.string.notification_shortlisted_body
            JobLinkNotificationEvent.INTERVIEW -> R.string.notification_interview_body
            JobLinkNotificationEvent.JOB_OFFER -> R.string.notification_job_offer_body
            JobLinkNotificationEvent.NEW_MATCHING_JOB -> R.string.notification_new_matching_job_body
        },
    )

    private companion object {
        const val TAG = "JobLinkMessaging"
    }
}
