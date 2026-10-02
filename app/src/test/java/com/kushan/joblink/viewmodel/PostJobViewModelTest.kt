package com.kushan.joblink.viewmodel

import com.kushan.joblink.MainDispatcherRule
import com.google.firebase.Timestamp
import com.kushan.joblink.data.model.Job
import com.kushan.joblink.data.model.JobType
import com.kushan.joblink.data.model.WorkMode
import com.kushan.joblink.data.repository.JobError
import com.kushan.joblink.data.repository.JobRepository
import com.kushan.joblink.data.repository.JobResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
class PostJobViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).run {
        isLenient = false
        requireNotNull(parse("2026-09-29"))
    }

    @Test
    fun requiredFieldValidationPreventsPosting() = runTest {
        val repository = FakeJobRepository()
        val viewModel = PostJobViewModel(repository) { Date(currentDate.time) }

        viewModel.postJob()
        advanceUntilIdle()

        assertEquals(0, repository.postCalls)
        assertTrue(PostJobValidationError.TITLE_REQUIRED in viewModel.uiState.value.validationErrors)
        assertTrue(PostJobValidationError.SKILLS_REQUIRED in viewModel.uiState.value.validationErrors)
        assertTrue(PostJobValidationError.DEADLINE_REQUIRED in viewModel.uiState.value.validationErrors)
    }

    @Test
    fun validFormIsNormalizedAndPostedWithoutEditableEmployerIdentity() = runTest {
        val repository = FakeJobRepository().apply {
            postHandler = { job ->
                JobResult.Success(
                    job.copy(
                        id = "job-id",
                        employerId = "authenticated-employer",
                        companyName = "JobLink Labs",
                    ),
                )
            }
        }
        val viewModel = PostJobViewModel(repository) { Date(currentDate.time) }
        completeForm(viewModel)

        viewModel.postJob()
        advanceUntilIdle()

        val submittedJob = requireNotNull(repository.submittedJob)
        assertEquals("Software Engineer", submittedJob.title)
        assertEquals(JobType.CONTRACT, submittedJob.jobType)
        assertEquals(WorkMode.HYBRID, submittedJob.workMode)
        assertEquals(listOf("Kotlin", "Compose"), submittedJob.requiredSkills)
        assertEquals(100_000L, submittedJob.salaryMin)
        assertEquals(150_000L, submittedJob.salaryMax)
        assertEquals("LKR", submittedJob.currency)
        assertEquals("", submittedJob.employerId)
        assertEquals("", submittedJob.companyName)
        assertEquals("authenticated-employer", viewModel.uiState.value.postedJob?.employerId)
        assertFalse(viewModel.uiState.value.isPosting)
    }

    @Test
    fun invalidSalaryAndPastDeadlineAreReported() = runTest {
        val repository = FakeJobRepository()
        val viewModel = PostJobViewModel(repository) { Date(currentDate.time) }
        completeForm(viewModel)
        viewModel.onSalaryMinChanged("200000")
        viewModel.onSalaryMaxChanged("100000")
        viewModel.onApplicationDeadlineChanged("2026-09-29")

        viewModel.postJob()
        advanceUntilIdle()

        assertEquals(0, repository.postCalls)
        assertTrue(
            PostJobValidationError.SALARY_RANGE_INVALID in
                viewModel.uiState.value.validationErrors,
        )
        assertTrue(
            PostJobValidationError.DEADLINE_NOT_FUTURE in
                viewModel.uiState.value.validationErrors,
        )
    }

    @Test
    fun repositoryFailureStopsLoadingAndExposesFirebaseError() = runTest {
        val repository = FakeJobRepository().apply {
            postHandler = { JobResult.Failure(JobError.PERMISSION_DENIED) }
        }
        val viewModel = PostJobViewModel(repository) { Date(currentDate.time) }
        completeForm(viewModel)

        viewModel.postJob()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isPosting)
        assertEquals(JobError.PERMISSION_DENIED, viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.postedJob)
    }

    @Test
    fun startAnotherJobClearsConfirmationAndForm() = runTest {
        val repository = FakeJobRepository().apply {
            postHandler = { JobResult.Success(it.copy(id = "job-id")) }
        }
        val viewModel = PostJobViewModel(repository) { Date(currentDate.time) }
        completeForm(viewModel)
        viewModel.postJob()
        advanceUntilIdle()

        viewModel.startAnotherJob()

        assertNull(viewModel.uiState.value.postedJob)
        assertEquals("", viewModel.uiState.value.title)
        assertEquals(WorkMode.ONSITE, viewModel.uiState.value.workMode)
    }

    @Test
    fun editModeLoadsExistingJobAndSavesThroughUpdate() = runTest {
        val existingJob = Job(
            id = "job-1",
            employerId = "employer-1",
            companyName = "JobLink Labs",
            title = "Android Developer",
            description = "Build Android applications.",
            category = "Engineering",
            location = "Colombo",
            workMode = WorkMode.HYBRID,
            jobType = JobType.FULL_TIME,
            experienceLevel = "Mid-level",
            requiredSkills = listOf("Kotlin"),
            requirements = listOf("Two years experience"),
            applicationDeadline = Timestamp(
                requireNotNull(
                    SimpleDateFormat("yyyy-MM-dd", Locale.US).parse("2026-10-31"),
                ),
            ),
        )
        val repository = FakeJobRepository().apply {
            employerJobResult = JobResult.Success(existingJob)
            updateHandler = { _, job -> JobResult.Success(job.copy(id = "job-1")) }
        }

        val viewModel = PostJobViewModel(
            jobRepository = repository,
            jobId = "job-1",
            currentDateProvider = { Date(currentDate.time) },
        )
        advanceUntilIdle()

        assertEquals("Android Developer", viewModel.uiState.value.title)
        assertTrue(viewModel.uiState.value.isEditMode)
        assertTrue(viewModel.uiState.value.hasLoadedJob)

        viewModel.onTitleChanged("Senior Android Developer")
        viewModel.postJob()
        advanceUntilIdle()

        assertEquals(0, repository.postCalls)
        assertEquals(1, repository.updateCalls)
        assertEquals("job-1", repository.updatedJobId)
        assertEquals("Senior Android Developer", repository.updatedJob?.title)
        assertEquals("job-1", viewModel.uiState.value.postedJob?.id)
    }

    @Test
    fun editModeLoadFailureShowsRepositoryError() = runTest {
        val repository = FakeJobRepository().apply {
            employerJobResult = JobResult.Failure(JobError.JOB_NOT_FOUND)
        }

        val viewModel = PostJobViewModel(repository, jobId = "missing")
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.isEditMode)
        assertFalse(viewModel.uiState.value.hasLoadedJob)
        assertEquals(JobError.JOB_NOT_FOUND, viewModel.uiState.value.error)
    }

    private fun completeForm(viewModel: PostJobViewModel) {
        viewModel.onTitleChanged("  Software Engineer  ")
        viewModel.onDescriptionChanged("Build reliable Android products.")
        viewModel.onCategoryChanged("Engineering")
        viewModel.onLocationChanged("Colombo")
        viewModel.onWorkModeChanged(WorkMode.HYBRID)
        viewModel.onJobTypeChanged(JobType.CONTRACT)
        viewModel.onSalaryMinChanged("100000")
        viewModel.onSalaryMaxChanged("150000")
        viewModel.onCurrencyChanged("lkr")
        viewModel.onExperienceLevelChanged("Mid-level")
        viewModel.onRequiredSkillsChanged(" Kotlin, Compose, Kotlin ")
        viewModel.onRequirementsChanged("Degree, 2 years experience")
        viewModel.onBenefitsChanged("Flexible hours, Learning budget")
        viewModel.onApplicationDeadlineChanged("2026-10-31")
    }

    private class FakeJobRepository : JobRepository {
        var postCalls = 0
        var updateCalls = 0
        var submittedJob: Job? = null
        var updatedJob: Job? = null
        var updatedJobId: String? = null
        var postHandler: (Job) -> JobResult<Job> = { JobResult.Success(it) }
        var updateHandler: (String, Job) -> JobResult<Job> = { _, job ->
            JobResult.Success(job)
        }
        var employerJobResult: JobResult<Job> = JobResult.Failure(JobError.JOB_NOT_FOUND)

        override suspend fun postJob(job: Job): JobResult<Job> {
            postCalls += 1
            submittedJob = job
            return postHandler(job)
        }

        override suspend fun getEmployerJobs(): JobResult<List<Job>> =
            JobResult.Success(emptyList())

        override suspend fun getEmployerJob(jobId: String): JobResult<Job> = employerJobResult

        override suspend fun updateEmployerJob(jobId: String, job: Job): JobResult<Job> {
            updateCalls += 1
            updatedJobId = jobId
            updatedJob = job
            return updateHandler(jobId, job)
        }

        override suspend fun setEmployerJobActive(
            jobId: String,
            active: Boolean,
        ): JobResult<Job> = JobResult.Failure(JobError.JOB_NOT_FOUND)

        override suspend fun getActiveJobs(): JobResult<List<Job>> =
            JobResult.Success(emptyList())

        override suspend fun getJob(jobId: String): JobResult<Job> =
            JobResult.Failure(JobError.JOB_NOT_FOUND)

        override suspend fun getSavedJobs(): JobResult<List<Job>> =
            JobResult.Success(emptyList())

        override suspend fun isJobSaved(jobId: String): JobResult<Boolean> =
            JobResult.Success(false)

        override suspend fun saveJob(jobId: String): JobResult<Unit> =
            JobResult.Success(Unit)

        override suspend fun unsaveJob(jobId: String): JobResult<Unit> =
            JobResult.Success(Unit)
    }
}
