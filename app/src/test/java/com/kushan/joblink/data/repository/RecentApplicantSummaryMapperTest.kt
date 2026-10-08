package com.kushan.joblink.data.repository

import com.google.firebase.Timestamp
import com.kushan.joblink.data.model.ApplicationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecentApplicantSummaryMapperTest {
    @Test
    fun mapsOnlySafeDashboardSummaryFields() {
        val submittedAt = Timestamp(1_700_000_000, 0)
        val summary = RecentApplicantSummaryMapper.from(
            documentId = "job-1_applicant-1",
            data = mapOf(
                "applicationId" to "untrusted-application-id",
                "jobId" to "job-1",
                "employerId" to "employer-1",
                "applicantId" to "applicant-1",
                "jobTitle" to "Android Developer",
                "applicantFullName" to "A. Candidate",
                "submittedAt" to submittedAt,
                "status" to "SHORTLISTED",
                "applicantPhone" to "+94 00 000 0000",
                "applicantBio" to "Private profile detail",
            ),
        )

        assertEquals("job-1_applicant-1", summary.applicationId)
        assertEquals("A. Candidate", summary.applicantFullName)
        assertEquals(ApplicationStatus.SHORTLISTED, summary.status)
        assertEquals(submittedAt, summary.submittedAt)
        assertEquals("", summary.applicantPhone)
        assertEquals("", summary.applicantBio)
    }

    @Test
    fun malformedOrMissingOptionalFieldsUseSafeDefaults() {
        val summary = RecentApplicantSummaryMapper.from(
            documentId = "legacy-application",
            data = mapOf(
                "jobId" to "job-1",
                "applicantFullName" to listOf("wrong type"),
                "submittedAt" to "not a timestamp",
                "status" to "UNKNOWN_STATUS",
            ),
        )

        assertEquals("legacy-application", summary.applicationId)
        assertEquals("", summary.applicantFullName)
        assertNull(summary.submittedAt)
        assertEquals(ApplicationStatus.SUBMITTED, summary.status)
    }
}
