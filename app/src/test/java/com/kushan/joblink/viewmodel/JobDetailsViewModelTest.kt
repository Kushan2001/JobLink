package com.kushan.joblink.viewmodel

import com.kushan.joblink.MainDispatcherRule
import com.kushan.joblink.data.model.Job
import com.kushan.joblink.data.repository.JobError
import com.kushan.joblink.data.repository.JobRepository
import com.kushan.joblink.data.repository.JobResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class JobDetailsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun requestedJobLoadsById() = runTest {
        val job = Job(id = "job-1", title = "Android Developer")
        val repository = FakeJobRepository(JobResult.Success(job))

        val viewModel = JobDetailsViewModel("job-1", repository)
        advanceUntilIdle()

        assertEquals("job-1", repository.requestedJobId)
        assertEquals(job, viewModel.uiState.value.job)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun missingJobExposesNotFoundError() = runTest {
        val repository = FakeJobRepository(JobResult.Failure(JobError.JOB_NOT_FOUND))

        val viewModel = JobDetailsViewModel("missing", repository)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.job)
        assertEquals(JobError.JOB_NOT_FOUND, viewModel.uiState.value.error)
    }

    private class FakeJobRepository(
        private val jobResult: JobResult<Job>,
    ) : JobRepository {
        var requestedJobId: String? = null

        override suspend fun getJob(jobId: String): JobResult<Job> {
            requestedJobId = jobId
            return jobResult
        }

        override suspend fun getActiveJobs(): JobResult<List<Job>> =
            JobResult.Success(emptyList())

        override suspend fun postJob(job: Job): JobResult<Job> = JobResult.Success(job)
    }
}
