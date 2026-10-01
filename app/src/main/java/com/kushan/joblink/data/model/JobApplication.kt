package com.kushan.joblink.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp

@Keep
data class JobApplication(
    val applicationId: String = "",
    val jobId: String = "",
    val employerId: String = "",
    val applicantId: String = "",
    val submittedAt: Timestamp? = null,
    val status: ApplicationStatus = ApplicationStatus.SUBMITTED,
    val coverMessage: String? = null,
    val cvReference: String = "",
)

@Keep
enum class ApplicationStatus {
    SUBMITTED,
}

data class ApplicationDraft(
    val jobId: String,
    val jobTitle: String,
    val companyName: String,
    val cvFileName: String,
)
