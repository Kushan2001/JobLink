package com.kushan.joblink.viewmodel

import com.kushan.joblink.MainDispatcherRule
import com.kushan.joblink.data.model.Job
import com.kushan.joblink.data.model.CvMetadata
import com.kushan.joblink.data.model.CvUploadFile
import com.kushan.joblink.data.model.JobSeekerProfile
import com.kushan.joblink.data.model.JobType
import com.kushan.joblink.data.model.WorkMode
import com.kushan.joblink.data.repository.JobError
import com.kushan.joblink.data.repository.JobRepository
import com.kushan.joblink.data.repository.JobResult
import com.kushan.joblink.data.repository.JobSeekerProfileRepository
import com.kushan.joblink.data.repository.ProfileError
import com.kushan.joblink.data.repository.ProfileResult
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

        val viewModel = HomeViewModel(repository, FakeJobSeekerProfileRepository())
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

        val viewModel = HomeViewModel(repository, FakeJobSeekerProfileRepository())
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

        val viewModel = HomeViewModel(repository, FakeJobSeekerProfileRepository())
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
        val viewModel = HomeViewModel(repository, FakeJobSeekerProfileRepository())
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(jobs, viewModel.uiState.value.jobs)
        assertEquals(JobError.NETWORK, viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isRefreshing)
    }

    @Test
    fun searchMatchesTitleAndCompanyWithoutAnotherRepositoryRead() = runTest {
        val repository = FakeJobRepository(
            mutableListOf(JobResult.Success(filterableJobs())),
        )
        val viewModel = HomeViewModel(repository, FakeJobSeekerProfileRepository())
        advanceUntilIdle()

        viewModel.onSearchQueryChanged("android acme")

        assertEquals(listOf("android"), viewModel.uiState.value.jobs.map(Job::id))
        assertEquals(1, repository.activeJobCalls)

        viewModel.onSearchQueryChanged("northstar")

        assertEquals(listOf("designer"), viewModel.uiState.value.jobs.map(Job::id))
        assertEquals(1, repository.activeJobCalls)
    }

    @Test
    fun savedJobIdsAreExposedForFeedCards() = runTest {
        val jobs = filterableJobs()
        val repository = FakeJobRepository(
            activeJobResults = mutableListOf(JobResult.Success(jobs)),
            savedJobsResult = JobResult.Success(listOf(jobs.first())),
        )

        val viewModel = HomeViewModel(repository, FakeJobSeekerProfileRepository())
        advanceUntilIdle()

        assertEquals(setOf("android"), viewModel.uiState.value.savedJobIds)
        assertEquals(1, repository.savedJobCalls)
    }

    @Test
    fun savedJobsFailureDoesNotHideSuccessfullyLoadedJobFeed() = runTest {
        val jobs = filterableJobs()
        val repository = FakeJobRepository(
            activeJobResults = mutableListOf(JobResult.Success(jobs)),
            savedJobsResult = JobResult.Failure(JobError.PERMISSION_DENIED),
        )

        val viewModel = HomeViewModel(repository, FakeJobSeekerProfileRepository())
        advanceUntilIdle()

        assertEquals(jobs.map(Job::id), viewModel.uiState.value.jobs.map(Job::id))
        assertEquals(null, viewModel.uiState.value.error)
        assertEquals(JobError.PERMISSION_DENIED, viewModel.uiState.value.savedJobsError)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun profileFailureDoesNotHideSuccessfullyLoadedJobFeed() = runTest {
        val jobs = filterableJobs()
        val repository = FakeJobRepository(
            activeJobResults = mutableListOf(JobResult.Success(jobs)),
        )
        val profileRepository = FakeJobSeekerProfileRepository(
            result = ProfileResult.Failure(ProfileError.NETWORK),
        )

        val viewModel = HomeViewModel(
            jobRepository = repository,
            jobSeekerProfileRepository = profileRepository,
            logError = { _, _ -> },
            logWarning = {},
        )
        advanceUntilIdle()

        assertEquals(jobs.map(Job::id), viewModel.uiState.value.jobs.map(Job::id))
        assertEquals(null, viewModel.uiState.value.error)
        assertEquals(ProfileError.NETWORK, viewModel.uiState.value.profileError)
        assertTrue(viewModel.uiState.value.recommendations.isEmpty())
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun categoriesComeFromLoadedJobsAndCanToggleTheExistingFilter() = runTest {
        val repository = FakeJobRepository(
            mutableListOf(JobResult.Success(filterableJobs())),
        )
        val viewModel = HomeViewModel(repository, FakeJobSeekerProfileRepository())
        advanceUntilIdle()

        assertEquals(listOf("Engineering", "Design"), viewModel.uiState.value.categories)

        viewModel.onCategorySelected("Engineering")
        assertEquals(listOf("android"), viewModel.uiState.value.jobs.map(Job::id))

        viewModel.onCategorySelected("Engineering")
        assertEquals("", viewModel.uiState.value.categoryFilter)
        assertEquals(2, viewModel.uiState.value.jobs.size)
        assertEquals(1, repository.activeJobCalls)
    }

    @Test
    fun textAndEnumFiltersCanBeCombinedAndCleared() = runTest {
        val jobs = filterableJobs()
        val repository = FakeJobRepository(
            mutableListOf(JobResult.Success(jobs)),
        )
        val viewModel = HomeViewModel(repository, FakeJobSeekerProfileRepository())
        advanceUntilIdle()

        viewModel.onCategoryFilterChanged("engineering")
        viewModel.onLocationFilterChanged("colombo")
        viewModel.onExperienceLevelFilterChanged("mid")
        viewModel.onJobTypeFilterChanged(JobType.FULL_TIME)
        viewModel.onWorkModeFilterChanged(WorkMode.HYBRID)

        val filteredState = viewModel.uiState.value
        assertEquals(listOf("android"), filteredState.jobs.map(Job::id))
        assertEquals(5, filteredState.activeFilterCount)
        assertTrue(filteredState.hasActiveFilters)
        assertEquals(1, repository.activeJobCalls)

        viewModel.clearFilters()

        assertEquals(jobs, viewModel.uiState.value.jobs)
        assertFalse(viewModel.uiState.value.hasActiveFilters)
    }

    @Test
    fun selectedFiltersRemainAppliedAfterRefresh() = runTest {
        val initialJobs = filterableJobs()
        val refreshedJobs = initialJobs + Job(
            id = "ios",
            companyName = "Acme Labs",
            title = "iOS Developer",
            category = "Engineering",
            location = "Kandy",
            experienceLevel = "Mid level",
            jobType = JobType.CONTRACT,
            workMode = WorkMode.REMOTE,
        )
        val repository = FakeJobRepository(
            mutableListOf(
                JobResult.Success(initialJobs),
                JobResult.Success(refreshedJobs),
            ),
        )
        val viewModel = HomeViewModel(repository, FakeJobSeekerProfileRepository())
        advanceUntilIdle()
        viewModel.onCategoryFilterChanged("engineering")
        viewModel.onWorkModeFilterChanged(WorkMode.REMOTE)

        viewModel.refresh()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("engineering", state.categoryFilter)
        assertEquals(WorkMode.REMOTE, state.workModeFilter)
        assertEquals(listOf("ios"), state.jobs.map(Job::id))
        assertEquals(2, repository.activeJobCalls)
    }

    private fun filterableJobs(): List<Job> = listOf(
        Job(
            id = "android",
            companyName = "Acme Labs",
            title = "Android Developer",
            category = "Engineering",
            location = "Colombo",
            experienceLevel = "Mid level",
            jobType = JobType.FULL_TIME,
            workMode = WorkMode.HYBRID,
        ),
        Job(
            id = "designer",
            companyName = "Northstar Studio",
            title = "Product Designer",
            category = "Design",
            location = "Galle",
            experienceLevel = "Senior",
            jobType = JobType.CONTRACT,
            workMode = WorkMode.REMOTE,
        ),
    )

    private class FakeJobRepository(
        private val activeJobResults: MutableList<JobResult<List<Job>>>,
        private val savedJobsResult: JobResult<List<Job>> = JobResult.Success(emptyList()),
    ) : JobRepository {
        var activeJobCalls = 0
        var savedJobCalls = 0

        override suspend fun getEmployerJobs(): JobResult<List<Job>> =
            JobResult.Success(emptyList())

        override suspend fun getEmployerJob(jobId: String): JobResult<Job> =
            JobResult.Failure(JobError.JOB_NOT_FOUND)

        override suspend fun updateEmployerJob(jobId: String, job: Job): JobResult<Job> =
            JobResult.Success(job)

        override suspend fun setEmployerJobActive(
            jobId: String,
            active: Boolean,
        ): JobResult<Job> = JobResult.Failure(JobError.JOB_NOT_FOUND)

        override suspend fun getActiveJobs(): JobResult<List<Job>> {
            activeJobCalls += 1
            return activeJobResults.removeAt(0)
        }

        override suspend fun getJob(jobId: String): JobResult<Job> =
            JobResult.Failure(JobError.JOB_NOT_FOUND)

        override suspend fun getSavedJobs(): JobResult<List<Job>> {
            savedJobCalls += 1
            return savedJobsResult
        }

        override suspend fun isJobSaved(jobId: String): JobResult<Boolean> =
            JobResult.Success(false)

        override suspend fun postJob(job: Job): JobResult<Job> = JobResult.Success(job)

        override suspend fun saveJob(jobId: String): JobResult<Unit> =
            JobResult.Success(Unit)

        override suspend fun unsaveJob(jobId: String): JobResult<Unit> =
            JobResult.Success(Unit)
    }

    private class FakeJobSeekerProfileRepository(
        private val result: ProfileResult<JobSeekerProfile> = ProfileResult.Success(
            recommendationProfile(),
        ),
    ) : JobSeekerProfileRepository {
        override suspend fun getProfile() = result

        override suspend fun saveProfile(profile: JobSeekerProfile) =
            ProfileResult.Success(profile)

        override suspend fun uploadCv(
            file: CvUploadFile,
            onProgress: (Float) -> Unit,
        ): ProfileResult<CvMetadata> = ProfileResult.Success(CvMetadata())
    }

    private companion object {
        fun recommendationProfile() = JobSeekerProfile(
            uid = "applicant-1",
            fullName = "Alex Silva",
            professionalHeadline = "Mid level Android developer",
            location = "Colombo",
            phone = "",
            bio = "",
            education = "",
            experienceSummary = "Mid level Android developer",
            skills = listOf("Kotlin"),
            preferredJobTypes = listOf("Full time"),
        )
    }
}
