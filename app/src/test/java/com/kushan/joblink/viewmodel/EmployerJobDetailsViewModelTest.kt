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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EmployerJobDetailsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun employerJobLoadsForRouteId() = runTest {
        val job = Job(id = "job-1", employerId = "employer-1")
        val repository = FakeJobRepository(JobResult.Success(job))

        val viewModel = EmployerJobDetailsViewModel("job-1", repository)
        advanceUntilIdle()

        assertEquals("job-1", repository.requestedJobId)
        assertEquals(job, viewModel.uiState.value.job)
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun reactivationUpdatesLoadedJob() = runTest {
        val job = Job(id = "job-1", active = false)
        val repository = FakeJobRepository(JobResult.Success(job)).apply {
            activeResult = JobResult.Success(job.copy(active = true))
        }
        val viewModel = EmployerJobDetailsViewModel("job-1", repository)
        advanceUntilIdle()

        viewModel.setActive(true)
        advanceUntilIdle()

        assertTrue(requireNotNull(viewModel.uiState.value.job).active)
        assertFalse(viewModel.uiState.value.isUpdating)
        assertNull(viewModel.uiState.value.actionError)
    }

    @Test
    fun missingEmployerJobCreatesNotFoundState() = runTest {
        val repository = FakeJobRepository(JobResult.Failure(JobError.JOB_NOT_FOUND))

        val viewModel = EmployerJobDetailsViewModel("missing", repository)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.job)
        assertEquals(JobError.JOB_NOT_FOUND, viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    private class FakeJobRepository(
        private val employerJobResult: JobResult<Job>,
    ) : JobRepository {
        var requestedJobId: String? = null
        var activeResult: JobResult<Job> = JobResult.Failure(JobError.UNKNOWN)

        override suspend fun getEmployerJobs(): JobResult<List<Job>> =
            JobResult.Success(emptyList())

        override suspend fun getEmployerJob(jobId: String): JobResult<Job> {
            requestedJobId = jobId
            return employerJobResult
        }

        override suspend fun updateEmployerJob(jobId: String, job: Job): JobResult<Job> =
            JobResult.Success(job)

        override suspend fun setEmployerJobActive(
            jobId: String,
            active: Boolean,
        ): JobResult<Job> = activeResult

        override suspend fun postJob(job: Job): JobResult<Job> = JobResult.Success(job)
        override suspend fun getActiveJobs(): JobResult<List<Job>> =
            JobResult.Success(emptyList())
        override suspend fun getJob(jobId: String): JobResult<Job> =
            JobResult.Failure(JobError.JOB_NOT_FOUND)
        override suspend fun getSavedJobs(): JobResult<List<Job>> =
            JobResult.Success(emptyList())
        override suspend fun isJobSaved(jobId: String): JobResult<Boolean> =
            JobResult.Success(false)
        override suspend fun saveJob(jobId: String): JobResult<Unit> = JobResult.Success(Unit)
        override suspend fun unsaveJob(jobId: String): JobResult<Unit> = JobResult.Success(Unit)
    }
}
