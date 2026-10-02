package com.kushan.joblink.data.repository

import com.kushan.joblink.data.model.ApplicationDraft
import com.kushan.joblink.data.model.ApplicationStatus
import com.kushan.joblink.data.model.EmployerApplicationsData
import com.kushan.joblink.data.model.JobApplication
import java.io.File

enum class ApplicationError {
    NOT_AUTHENTICATED,
    PROFILE_NOT_FOUND,
    WRONG_ROLE,
    JOB_NOT_FOUND,
    APPLICATION_NOT_FOUND,
    CV_REQUIRED,
    CV_NOT_AVAILABLE,
    CV_DOWNLOAD_FAILED,
    CV_VIEWER_UNAVAILABLE,
    ALREADY_APPLIED,
    INVALID_STATUS,
    PERMISSION_DENIED,
    NETWORK,
    UNKNOWN,
}

sealed interface ApplicationResult<out T> {
    data class Success<T>(val value: T) : ApplicationResult<T>
    data class Failure(val error: ApplicationError) : ApplicationResult<Nothing>
}

interface ApplicationRepository {
    suspend fun getEmployerApplications(jobId: String): ApplicationResult<EmployerApplicationsData>

    suspend fun getEmployerApplication(applicationId: String): ApplicationResult<JobApplication>

    suspend fun updateEmployerApplicationStatus(
        applicationId: String,
        status: ApplicationStatus,
    ): ApplicationResult<JobApplication>

    suspend fun downloadApplicantCv(
        applicationId: String,
        onProgress: (Float) -> Unit,
    ): ApplicationResult<File>

    suspend fun getMyApplications(): ApplicationResult<List<JobApplication>>

    suspend fun getMyApplication(applicationId: String): ApplicationResult<JobApplication>

    suspend fun getApplicationDraft(jobId: String): ApplicationResult<ApplicationDraft>

    suspend fun submitApplication(
        jobId: String,
        coverMessage: String,
    ): ApplicationResult<JobApplication>
}
