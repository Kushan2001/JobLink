package com.kushan.joblink.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JobLinkNotificationParserTest {
    @Test
    fun `application events route to application details`() {
        val events = listOf(
            "APPLICATION_REVIEWED",
            "SHORTLISTED",
            "INTERVIEW",
            "JOB_OFFER",
        )

        events.forEach { event ->
            val notification = JobLinkNotificationParser.parse(
                mapOf(
                    "event" to event,
                    "applicationId" to "application-123",
                ),
            )

            assertEquals(
                NotificationNavigation.ApplicationDetails("application-123"),
                notification?.navigation,
            )
        }
    }

    @Test
    fun `matching job routes to job details`() {
        val notification = JobLinkNotificationParser.parse(
            mapOf(
                "event" to "NEW_MATCHING_JOB",
                "jobId" to "job-456",
            ),
        )

        assertEquals(
            NotificationNavigation.JobDetails("job-456"),
            notification?.navigation,
        )
    }

    @Test
    fun `missing destination identifier falls back to home`() {
        val notification = JobLinkNotificationParser.parse(
            mapOf("event" to "APPLICATION_REVIEWED"),
        )

        assertEquals(NotificationNavigation.Home, notification?.navigation)
    }

    @Test
    fun `legacy status aliases remain safe to parse`() {
        val notification = JobLinkNotificationParser.parse(
            mapOf(
                "type" to "OFFERED",
                "applicationId" to "application-123",
            ),
        )

        assertEquals(JobLinkNotificationEvent.JOB_OFFER, notification?.event)
    }

    @Test
    fun `data text overrides notification fallback text`() {
        val notification = JobLinkNotificationParser.parse(
            data = mapOf(
                "event" to "INTERVIEW",
                "title" to "Interview scheduled",
                "body" to "Open JobLink for the details.",
            ),
            fallbackTitle = "Fallback title",
            fallbackBody = "Fallback body",
        )

        assertEquals("Interview scheduled", notification?.title)
        assertEquals("Open JobLink for the details.", notification?.body)
    }

    @Test
    fun `unknown event is ignored`() {
        assertNull(
            JobLinkNotificationParser.parse(
                mapOf("event" to "UNSUPPORTED_EVENT"),
            ),
        )
    }
}
