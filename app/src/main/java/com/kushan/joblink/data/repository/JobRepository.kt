package com.kushan.joblink.data.repository

import com.kushan.joblink.data.model.Job

enum class JobError {
    NOT_AUTHENTICATED,
    ACCOUNT_NOT_FOUND,
    WRONG_ROLE,
    COMPANY_PROFILE_REQUIRED,
    PERMISSION_DENIED,
    NETWORK,
    JOB_NOT_FOUND,
    UNKNOWN,
}

sealed interface JobResult<out T> {
    data class Success<T>(val value: T) : JobResult<T>
    data class Failure(val error: JobError) : JobResult<Nothing>
}

interface JobRepository {
    suspend fun postJob(job: Job): JobResult<Job>

    suspend fun getActiveJobs(): JobResult<List<Job>>

    suspend fun getJob(jobId: String): JobResult<Job>
}
