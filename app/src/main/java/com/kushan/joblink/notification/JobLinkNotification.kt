package com.kushan.joblink.notification

enum class JobLinkNotificationEvent {
    APPLICATION_REVIEWED,
    SHORTLISTED,
    INTERVIEW,
    JOB_OFFER,
    NEW_MATCHING_JOB,
    ;

    companion object {
        fun fromValue(value: String?): JobLinkNotificationEvent? {
            val normalized = value
                ?.trim()
                ?.uppercase()
                ?.replace('-', '_')
                ?: return null

            return when (normalized) {
                "APPLICATION_REVIEWED", "REVIEWED" -> APPLICATION_REVIEWED
                "APPLICATION_SHORTLISTED", "SHORTLISTED" -> SHORTLISTED
                "APPLICATION_INTERVIEW", "INTERVIEW" -> INTERVIEW
                "APPLICATION_OFFERED", "OFFERED", "JOB_OFFER" -> JOB_OFFER
                "NEW_MATCHING_JOB" -> NEW_MATCHING_JOB
                else -> null
            }
        }
    }
}

sealed interface NotificationNavigation {
    data class ApplicationDetails(val applicationId: String) : NotificationNavigation
    data class JobDetails(val jobId: String) : NotificationNavigation
    data object Home : NotificationNavigation
}

data class JobLinkNotification(
    val event: JobLinkNotificationEvent,
    val title: String?,
    val body: String?,
    val applicationId: String?,
    val jobId: String?,
) {
    val navigation: NotificationNavigation
        get() = when (event) {
            JobLinkNotificationEvent.NEW_MATCHING_JOB -> jobId
                ?.takeIf(String::isNotBlank)
                ?.let(NotificationNavigation::JobDetails)
                ?: NotificationNavigation.Home

            else -> applicationId
                ?.takeIf(String::isNotBlank)
                ?.let(NotificationNavigation::ApplicationDetails)
                ?: NotificationNavigation.Home
        }
}

object JobLinkNotificationParser {
    const val EVENT_KEY = "event"
    const val LEGACY_EVENT_KEY = "type"
    const val APPLICATION_ID_KEY = "applicationId"
    const val JOB_ID_KEY = "jobId"
    const val TITLE_KEY = "title"
    const val BODY_KEY = "body"

    fun parse(
        data: Map<String, String>,
        fallbackTitle: String? = null,
        fallbackBody: String? = null,
    ): JobLinkNotification? {
        val event = JobLinkNotificationEvent.fromValue(
            data[EVENT_KEY] ?: data[LEGACY_EVENT_KEY],
        ) ?: return null

        return JobLinkNotification(
            event = event,
            title = data[TITLE_KEY].orNullIfBlank() ?: fallbackTitle.orNullIfBlank(),
            body = data[BODY_KEY].orNullIfBlank() ?: fallbackBody.orNullIfBlank(),
            applicationId = data[APPLICATION_ID_KEY].orNullIfBlank(),
            jobId = data[JOB_ID_KEY].orNullIfBlank(),
        )
    }

    private fun String?.orNullIfBlank(): String? = this?.trim()?.takeIf(String::isNotEmpty)
}
