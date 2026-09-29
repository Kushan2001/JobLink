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
class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun activeJobsLoadIntoFeed() = runTest {
        val jobs = listOf(Job(id = "job-1", title = "Android Developer"))
        val repository = FakeJobRepository(mutableListOf(JobResult.Success(jobs)))

        val viewModel = HomeViewModel(repository)
        advanceUntilIdle()

        assertEquals(jobs, viewModel.uiState.value.jobs)
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(1, repository.activeJobCalls)
    }

    @Test
    fun emptyResultCreatesEmptyReadyState() = runTest {
        val repository = FakeJobRepository(
            mutableListOf(JobResult.Success(emptyList())),
        )

        val viewModel = HomeViewModel(repository)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.jobs.isEmpty())
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(null, viewModel.uiState.value.error)
    }

    @Test
    fun initialFailureExposesError() = runTest {
        val repository = FakeJobRepository(
            mutableListOf(JobResult.Failure(JobError.NETWORK)),
        )

        val viewModel = HomeViewModel(repository)
        advanceUntilIdle()

        assertEquals(JobError.NETWORK, viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun refreshFailureKeepsPreviouslyLoadedJobs() = runTest {
        val jobs = listOf(Job(id = "job-1", title = "Android Developer"))
        val repository = FakeJobRepository(
            mutableListOf(
                JobResult.Success(jobs),
                JobResult.Failure(JobError.NETWORK),
            ),
        )
        val viewModel = HomeViewModel(repository)
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(jobs, viewModel.uiState.value.jobs)
        assertEquals(JobError.NETWORK, viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isRefreshing)
    }

    private class FakeJobRepository(
        private val activeJobResults: MutableList<JobResult<List<Job>>>,
    ) : JobRepository {
        var activeJobCalls = 0

        override suspend fun getActiveJobs(): JobResult<List<Job>> {
            activeJobCalls += 1
            return activeJobResults.removeAt(0)
        }

        override suspend fun getJob(jobId: String): JobResult<Job> =
            JobResult.Failure(JobError.JOB_NOT_FOUND)

        override suspend fun postJob(job: Job): JobResult<Job> = JobResult.Success(job)
    }
}
