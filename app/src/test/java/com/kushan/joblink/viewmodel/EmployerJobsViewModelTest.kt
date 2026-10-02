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
class EmployerJobsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun employerJobsLoadInRepositoryOrder() = runTest {
        val jobs = listOf(
            Job(id = "newest", title = "Android Developer", applicantCount = 3),
            Job(id = "older", title = "Designer", active = false),
        )
        val repository = FakeJobRepository(
            employerJobResults = mutableListOf(JobResult.Success(jobs)),
        )

        val viewModel = EmployerJobsViewModel(repository)
        advanceUntilIdle()

        assertEquals(jobs, viewModel.uiState.value.jobs)
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun emptyEmployerJobsCreatesEmptyReadyState() = runTest {
        val repository = FakeJobRepository(
            employerJobResults = mutableListOf(JobResult.Success(emptyList())),
        )

        val viewModel = EmployerJobsViewModel(repository)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.jobs.isEmpty())
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun deactivationUpdatesOnlyTheMatchingJob() = runTest {
        val first = Job(id = "job-1", active = true)
        val second = Job(id = "job-2", active = true)
        val repository = FakeJobRepository(
            employerJobResults = mutableListOf(JobResult.Success(listOf(first, second))),
        ).apply {
            activeResult = JobResult.Success(first.copy(active = false))
        }
        val viewModel = EmployerJobsViewModel(repository)
        advanceUntilIdle()

        viewModel.setJobActive("job-1", false)
        advanceUntilIdle()

        assertEquals("job-1", repository.updatedActiveJobId)
        assertEquals(false, repository.requestedActiveState)
        assertFalse(requireNotNull(viewModel.uiState.value.jobs.first()).active)
        assertTrue(viewModel.uiState.value.jobs[1].active)
        assertNull(viewModel.uiState.value.actionError)
    }

    @Test
    fun activationFailureKeepsCurrentJobAndShowsActionError() = runTest {
        val job = Job(id = "job-1", active = false)
        val repository = FakeJobRepository(
            employerJobResults = mutableListOf(JobResult.Success(listOf(job))),
        ).apply {
            activeResult = JobResult.Failure(JobError.PERMISSION_DENIED)
        }
        val viewModel = EmployerJobsViewModel(repository)
        advanceUntilIdle()

        viewModel.setJobActive("job-1", true)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.jobs.single().active)
        assertEquals(JobError.PERMISSION_DENIED, viewModel.uiState.value.actionError)
        assertNull(viewModel.uiState.value.updatingJobId)
    }

    private class FakeJobRepository(
        private val employerJobResults: MutableList<JobResult<List<Job>>>,
    ) : JobRepository {
        var activeResult: JobResult<Job> = JobResult.Failure(JobError.UNKNOWN)
        var updatedActiveJobId: String? = null
        var requestedActiveState: Boolean? = null

        override suspend fun getEmployerJobs(): JobResult<List<Job>> =
            employerJobResults.removeAt(0)

        override suspend fun getEmployerJob(jobId: String): JobResult<Job> =
            JobResult.Failure(JobError.JOB_NOT_FOUND)

        override suspend fun updateEmployerJob(jobId: String, job: Job): JobResult<Job> =
            JobResult.Success(job)

        override suspend fun setEmployerJobActive(
            jobId: String,
            active: Boolean,
        ): JobResult<Job> {
            updatedActiveJobId = jobId
            requestedActiveState = active
            return activeResult
        }

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
