package com.kushan.joblink.notification

import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NotificationNavigationCoordinator {
    private val _navigation = MutableStateFlow<NotificationNavigation?>(null)
    val navigation: StateFlow<NotificationNavigation?> = _navigation.asStateFlow()

    fun handleIntent(intent: Intent?) {
        val data = intent?.extras?.keySet()?.associateWith { key ->
            intent.extras?.getString(key).orEmpty()
        }.orEmpty()

        JobLinkNotificationParser.parse(data)?.let { notification ->
            _navigation.value = notification.navigation
        }
    }

    fun markHandled() {
        _navigation.value = null
    }
}
