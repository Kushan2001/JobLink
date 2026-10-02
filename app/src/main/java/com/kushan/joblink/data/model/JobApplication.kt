package com.kushan.joblink.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp

@Keep
data class JobApplication(
    val applicationId: String = "",
    val jobId: String = "",
    val employerId: String = "",
    val applicantId: String = "",
    val jobTitle: String = "",
    val companyName: String = "",
    val applicantFullName: String = "",
    val applicantEmail: String = "",
    val applicantHeadline: String = "",
    val applicantLocation: String = "",
    val applicantPhone: String = "",
    val applicantBio: String = "",
    val applicantEducation: String = "",
    val applicantExperienceSummary: String = "",
    val applicantSkills: List<String> = emptyList(),
    val submittedAt: Timestamp? = null,
    val status: ApplicationStatus = ApplicationStatus.SUBMITTED,
    val coverMessage: String? = null,
    val cvReference: String = "",
)

@Keep
enum class ApplicationStatus {
    SUBMITTED,
    REVIEWED,
    SHORTLISTED,
    INTERVIEW,
    OFFERED,
    REJECTED,
    WITHDRAWN,
}

val employerApplicationStatuses = listOf(
    ApplicationStatus.REVIEWED,
    ApplicationStatus.SHORTLISTED,
    ApplicationStatus.INTERVIEW,
    ApplicationStatus.OFFERED,
    ApplicationStatus.REJECTED,
)

data class ApplicationDraft(
    val jobId: String,
    val jobTitle: String,
    val companyName: String,
    val cvFileName: String,
)

data class EmployerApplicationsData(
    val jobId: String,
    val jobTitle: String,
    val applications: List<JobApplication>,
)
