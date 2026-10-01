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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SavedJobsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun savedJobsLoadInRepositoryOrder() = runTest {
        val jobs = listOf(
            Job(id = "newest", title = "Android Developer"),
            Job(id = "older", title = "Product Designer"),
        )
        val repository = FakeJobRepository(
            mutableListOf(JobResult.Success(jobs)),
        )

        val viewModel = SavedJobsViewModel(repository)
        advanceUntilIdle()

        assertEquals(jobs, viewModel.uiState.value.jobs)
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(1, repository.savedJobsCalls)
    }

    @Test
    fun emptySavedJobsCreatesEmptyReadyState() = runTest {
        val repository = FakeJobRepository(
            mutableListOf(JobResult.Success(emptyList())),
        )

        val viewModel = SavedJobsViewModel(repository)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.jobs.isEmpty())
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(null, viewModel.uiState.value.error)
    }

    @Test
    fun refreshFailureKeepsLoadedSavedJobs() = runTest {
        val jobs = listOf(Job(id = "job-1"))
        val repository = FakeJobRepository(
            mutableListOf(
                JobResult.Success(jobs),
                JobResult.Failure(JobError.NETWORK),
            ),
        )
        val viewModel = SavedJobsViewModel(repository)
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(jobs, viewModel.uiState.value.jobs)
        assertEquals(JobError.NETWORK, viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isRefreshing)
    }

    private class FakeJobRepository(
        private val savedJobResults: MutableList<JobResult<List<Job>>>,
    ) : JobRepository {
        var savedJobsCalls = 0

        override suspend fun getSavedJobs(): JobResult<List<Job>> {
            savedJobsCalls += 1
            return savedJobResults.removeAt(0)
        }

        override suspend fun postJob(job: Job): JobResult<Job> = JobResult.Success(job)

        override suspend fun getActiveJobs(): JobResult<List<Job>> =
            JobResult.Success(emptyList())

        override suspend fun getJob(jobId: String): JobResult<Job> =
            JobResult.Failure(JobError.JOB_NOT_FOUND)

        override suspend fun isJobSaved(jobId: String): JobResult<Boolean> =
            JobResult.Success(false)

        override suspend fun saveJob(jobId: String): JobResult<Unit> = JobResult.Success(Unit)

        override suspend fun unsaveJob(jobId: String): JobResult<Unit> = JobResult.Success(Unit)
    }
}
