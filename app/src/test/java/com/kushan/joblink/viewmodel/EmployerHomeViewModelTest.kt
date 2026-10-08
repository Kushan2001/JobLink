package com.kushan.joblink.viewmodel

import com.kushan.joblink.MainDispatcherRule
import com.kushan.joblink.data.model.CompanyProfile
import com.kushan.joblink.data.model.Job
import com.kushan.joblink.data.model.JobApplication
import com.kushan.joblink.data.repository.ApplicationError
import com.kushan.joblink.data.repository.ApplicationResult
import com.kushan.joblink.data.repository.EmployerProfileRepository
import com.kushan.joblink.data.repository.JobError
import com.kushan.joblink.data.repository.JobRepository
import com.kushan.joblink.data.repository.JobResult
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
class EmployerHomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun dashboardCombinesProfileJobMetricsAndRecentApplicants() = runTest {
        val jobs = listOf(
            Job(id = "active", active = true, applicantCount = 3),
            Job(id = "inactive", active = false, applicantCount = 2),
        )
        val applicants = listOf(JobApplication(applicationId = "application-1"))
        val applicationRepository = DashboardApplicationRepository(
            ApplicationResult.Success(applicants),
        )

        val viewModel = EmployerHomeViewModel(
            employerProfileRepository = DashboardProfileRepository(
                ProfileResult.Success(companyProfile()),
            ),
            jobRepository = DashboardJobRepository(JobResult.Success(jobs)),
            applicationRepository = applicationRepository,
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("JobLink Labs", state.companyProfile?.companyName)
        assertEquals(1, state.activeJobsCount)
        assertEquals(5L, state.applicationsCount)
        assertEquals(applicants, state.recentApplicants)
        assertEquals(5L, applicationRepository.requestedLimit)
        assertFalse(state.isLoading)
        assertFalse(state.hasError)
    }

    @Test
    fun emptyRecentApplicantsIsAValidDashboardResult() = runTest {
        val viewModel = EmployerHomeViewModel(
            employerProfileRepository = DashboardProfileRepository(
                ProfileResult.Success(companyProfile()),
            ),
            jobRepository = DashboardJobRepository(JobResult.Success(emptyList())),
            applicationRepository = DashboardApplicationRepository(
                ApplicationResult.Success(emptyList()),
            ),
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.recentApplicants.isEmpty())
        assertFalse(state.hasRecentApplicantsError)
        assertFalse(state.hasError)
        assertFalse(state.isLoading)
    }

    @Test
    fun partialRefreshFailureKeepsPreviouslyLoadedDashboardData() = runTest {
        val profileRepository = DashboardProfileRepository(
            ProfileResult.Success(companyProfile()),
        )
        val jobRepository = DashboardJobRepository(
            JobResult.Success(listOf(Job(id = "job-1", active = true))),
        )
        val applicationRepository = DashboardApplicationRepository(
            ApplicationResult.Success(emptyList()),
        )
        val viewModel = EmployerHomeViewModel(
            profileRepository,
            jobRepository,
            applicationRepository,
        )
        advanceUntilIdle()
        applicationRepository.result = ApplicationResult.Failure(ApplicationError.NETWORK)

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals("JobLink Labs", viewModel.uiState.value.companyProfile?.companyName)
        assertEquals(1, viewModel.uiState.value.activeJobsCount)
        assertEquals(ApplicationError.NETWORK, viewModel.uiState.value.applicationError)
        assertTrue(viewModel.uiState.value.hasError)
        assertFalse(viewModel.uiState.value.hasDashboardError)
        assertTrue(viewModel.uiState.value.hasRecentApplicantsError)
        assertFalse(viewModel.uiState.value.isRefreshing)
    }

    @Test
    fun recentApplicantFailureDoesNotHideSuccessfulDashboardSections() = runTest {
        val jobs = listOf(Job(id = "job-1", active = true, applicantCount = 4))
        val viewModel = EmployerHomeViewModel(
            employerProfileRepository = DashboardProfileRepository(
                ProfileResult.Success(companyProfile()),
            ),
            jobRepository = DashboardJobRepository(JobResult.Success(jobs)),
            applicationRepository = DashboardApplicationRepository(
                ApplicationResult.Failure(ApplicationError.UNKNOWN),
            ),
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("JobLink Labs", state.companyProfile?.companyName)
        assertEquals(jobs, state.jobs)
        assertEquals(1, state.activeJobsCount)
        assertEquals(4L, state.applicationsCount)
        assertFalse(state.hasDashboardError)
        assertTrue(state.hasRecentApplicantsError)
        assertFalse(state.isLoading)
    }

    @Test
    fun initialProfileFailureCreatesLoadErrorState() = runTest {
        val viewModel = EmployerHomeViewModel(
            employerProfileRepository = DashboardProfileRepository(
                ProfileResult.Failure(ProfileError.NETWORK),
            ),
            jobRepository = DashboardJobRepository(JobResult.Failure(JobError.NETWORK)),
            applicationRepository = DashboardApplicationRepository(
                ApplicationResult.Failure(ApplicationError.NETWORK),
            ),
        )
        advanceUntilIdle()

        assertEquals(null, viewModel.uiState.value.companyProfile)
        assertEquals(ProfileError.NETWORK, viewModel.uiState.value.profileError)
        assertTrue(viewModel.uiState.value.hasError)
        assertTrue(viewModel.uiState.value.hasDashboardError)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    private class DashboardProfileRepository(
        var result: ProfileResult<CompanyProfile>,
    ) : EmployerProfileRepository {
        override suspend fun getCompanyProfile() = result
        override suspend fun saveCompanyProfile(profile: CompanyProfile) =
            ProfileResult.Success(profile)
    }

    private class DashboardJobRepository(
        var result: JobResult<List<Job>>,
    ) : JobRepository {
        override suspend fun getEmployerJobs() = result
        override suspend fun getEmployerJob(jobId: String) =
            JobResult.Failure(JobError.JOB_NOT_FOUND)
        override suspend fun updateEmployerJob(jobId: String, job: Job) =
            JobResult.Success(job)
        override suspend fun setEmployerJobActive(jobId: String, active: Boolean) =
            JobResult.Failure(JobError.JOB_NOT_FOUND)
        override suspend fun getActiveJobs() = JobResult.Success(emptyList<Job>())
        override suspend fun getJob(jobId: String) =
            JobResult.Failure(JobError.JOB_NOT_FOUND)
        override suspend fun getSavedJobs() = JobResult.Success(emptyList<Job>())
        override suspend fun isJobSaved(jobId: String) = JobResult.Success(false)
        override suspend fun postJob(job: Job) = JobResult.Success(job)
        override suspend fun saveJob(jobId: String) = JobResult.Success(Unit)
        override suspend fun unsaveJob(jobId: String) = JobResult.Success(Unit)
    }

    private class DashboardApplicationRepository(
        var result: ApplicationResult<List<JobApplication>>,
    ) : EmptyApplicationRepository() {
        var requestedLimit: Long? = null

        override suspend fun getRecentEmployerApplications(
            limit: Long,
        ): ApplicationResult<List<JobApplication>> {
            requestedLimit = limit
            return result
        }
    }

    private fun companyProfile() = CompanyProfile(
        ownerUid = "employer-1",
        companyName = "JobLink Labs",
        companyDescription = "Employment tools.",
        industry = "Software",
        companySize = "11–50 employees",
        location = "Colombo",
        website = "",
        contactEmail = "careers@example.com",
    )
}
