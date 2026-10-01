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

    @Test
    fun saveAndUnsaveTogglePersistedState() = runTest {
        val repository = FakeJobRepository(JobResult.Success(Job(id = "job-1")))
        val viewModel = JobDetailsViewModel("job-1", repository)
        advanceUntilIdle()

        viewModel.onSavedStateToggle()
        advanceUntilIdle()
        assertEquals("job-1", repository.savedJobId)
        assertEquals(true, viewModel.uiState.value.isSaved)
        assertEquals(
            JobDetailsActionMessage.JOB_SAVED,
            viewModel.uiState.value.actionMessage,
        )

        viewModel.onSavedStateToggle()
        advanceUntilIdle()
        assertEquals("job-1", repository.unsavedJobId)
        assertEquals(false, viewModel.uiState.value.isSaved)
        assertEquals(
            JobDetailsActionMessage.JOB_UNSAVED,
            viewModel.uiState.value.actionMessage,
        )
    }

    @Test
    fun existingSavedStateLoadsWithJob() = runTest {
        val repository = FakeJobRepository(
            jobResult = JobResult.Success(Job(id = "job-1")),
            initiallySaved = true,
        )

        val viewModel = JobDetailsViewModel("job-1", repository)
        advanceUntilIdle()

        assertEquals(true, viewModel.uiState.value.isSaved)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    private class FakeJobRepository(
        private val jobResult: JobResult<Job>,
        private val initiallySaved: Boolean = false,
    ) : JobRepository {
        var requestedJobId: String? = null
        var savedJobId: String? = null
        var unsavedJobId: String? = null

        override suspend fun getJob(jobId: String): JobResult<Job> {
            requestedJobId = jobId
            return jobResult
        }

        override suspend fun getActiveJobs(): JobResult<List<Job>> =
            JobResult.Success(emptyList())

        override suspend fun getSavedJobs(): JobResult<List<Job>> =
            JobResult.Success(emptyList())

        override suspend fun isJobSaved(jobId: String): JobResult<Boolean> =
            JobResult.Success(initiallySaved)

        override suspend fun postJob(job: Job): JobResult<Job> = JobResult.Success(job)

        override suspend fun saveJob(jobId: String): JobResult<Unit> {
            savedJobId = jobId
            return JobResult.Success(Unit)
        }

        override suspend fun unsaveJob(jobId: String): JobResult<Unit> {
            unsavedJobId = jobId
            return JobResult.Success(Unit)
        }
    }
}
