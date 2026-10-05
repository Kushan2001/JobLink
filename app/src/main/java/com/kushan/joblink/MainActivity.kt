package com.kushan.joblink

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kushan.joblink.notification.JobLinkNotificationParser
import com.kushan.joblink.notification.NotificationNavigationCoordinator

class MainActivity : ComponentActivity() {
    private val notificationNavigationCoordinator = NotificationNavigationCoordinator()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notificationNavigationCoordinator.handleIntent(intent)
        enableEdgeToEdge()
        setContent {
            val notificationNavigation by notificationNavigationCoordinator.navigation
                .collectAsStateWithLifecycle()
            JobLinkApp(
                notificationNavigation = notificationNavigation,
                onNotificationNavigationHandled = ::markNotificationNavigationHandled,
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        notificationNavigationCoordinator.handleIntent(intent)
    }

    private fun markNotificationNavigationHandled() {
        notificationNavigationCoordinator.markHandled()
        intent?.apply {
            removeExtra(JobLinkNotificationParser.EVENT_KEY)
            removeExtra(JobLinkNotificationParser.LEGACY_EVENT_KEY)
            removeExtra(JobLinkNotificationParser.APPLICATION_ID_KEY)
            removeExtra(JobLinkNotificationParser.JOB_ID_KEY)
        }
    }
}
