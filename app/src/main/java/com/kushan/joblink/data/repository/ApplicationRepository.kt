package com.kushan.joblink.data.repository

import com.kushan.joblink.data.model.ApplicationDraft
import com.kushan.joblink.data.model.JobApplication

enum class ApplicationError {
    NOT_AUTHENTICATED,
    PROFILE_NOT_FOUND,
    WRONG_ROLE,
    JOB_NOT_FOUND,
    CV_REQUIRED,
    ALREADY_APPLIED,
    PERMISSION_DENIED,
    NETWORK,
    UNKNOWN,
}

sealed interface ApplicationResult<out T> {
    data class Success<T>(val value: T) : ApplicationResult<T>
    data class Failure(val error: ApplicationError) : ApplicationResult<Nothing>
}

interface ApplicationRepository {
    suspend fun getApplicationDraft(jobId: String): ApplicationResult<ApplicationDraft>

    suspend fun submitApplication(
        jobId: String,
        coverMessage: String,
    ): ApplicationResult<JobApplication>
}
