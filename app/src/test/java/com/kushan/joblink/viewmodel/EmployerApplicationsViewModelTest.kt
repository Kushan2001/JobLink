package com.kushan.joblink.viewmodel

import com.kushan.joblink.MainDispatcherRule
import com.kushan.joblink.data.model.ApplicationDraft
import com.kushan.joblink.data.model.ApplicationStatus
import com.kushan.joblink.data.model.EmployerApplicationsData
import com.kushan.joblink.data.model.JobApplication
import com.kushan.joblink.data.repository.ApplicationError
import com.kushan.joblink.data.repository.ApplicationRepository
import com.kushan.joblink.data.repository.ApplicationResult
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EmployerApplicationsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun applicationsLoadForRouteJob() = runTest {
        val data = EmployerApplicationsData(
            jobId = "job-1",
            jobTitle = "Android Developer",
            applications = listOf(application()),
        )
        val repository = FakeApplicationRepository(
            mutableListOf(ApplicationResult.Success(data)),
        )

        val viewModel = EmployerApplicationsViewModel("job-1", repository)
        advanceUntilIdle()

        assertEquals("job-1", repository.requestedJobId)
        assertEquals(data, viewModel.uiState.value.data)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun refreshFailureKeepsPreviouslyLoadedApplicants() = runTest {
        val data = EmployerApplicationsData("job-1", "Android Developer", listOf(application()))
        val repository = FakeApplicationRepository(
            mutableListOf(
                ApplicationResult.Success(data),
                ApplicationResult.Failure(ApplicationError.NETWORK),
            ),
        )
        val viewModel = EmployerApplicationsViewModel("job-1", repository)
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(data, viewModel.uiState.value.data)
        assertEquals(ApplicationError.NETWORK, viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isRefreshing)
    }

    private class FakeApplicationRepository(
        private val results: MutableList<ApplicationResult<EmployerApplicationsData>>,
    ) : EmptyApplicationRepository() {
        var requestedJobId: String? = null

        override suspend fun getEmployerApplications(jobId: String): ApplicationResult<EmployerApplicationsData> {
            requestedJobId = jobId
            return results.removeAt(0)
        }
    }
}

private fun application() = JobApplication(
    applicationId = "application-1",
    jobId = "job-1",
    employerId = "employer-1",
    applicantId = "applicant-1",
    applicantFullName = "Alex Silva",
)

internal open class EmptyApplicationRepository : ApplicationRepository {
    override suspend fun getEmployerApplications(
        jobId: String,
    ): ApplicationResult<EmployerApplicationsData> =
        ApplicationResult.Failure(ApplicationError.UNKNOWN)
    override suspend fun getEmployerApplication(
        applicationId: String,
    ): ApplicationResult<JobApplication> =
        ApplicationResult.Failure(ApplicationError.UNKNOWN)
    override suspend fun updateEmployerApplicationStatus(
        applicationId: String,
        status: ApplicationStatus,
    ): ApplicationResult<JobApplication> = ApplicationResult.Failure(ApplicationError.UNKNOWN)
    override suspend fun downloadApplicantCv(
        applicationId: String,
        onProgress: (Float) -> Unit,
    ): ApplicationResult<File> = ApplicationResult.Failure(ApplicationError.UNKNOWN)
    override suspend fun getMyApplications(): ApplicationResult<List<JobApplication>> =
        ApplicationResult.Success(emptyList<JobApplication>())
    override suspend fun getMyApplication(
        applicationId: String,
    ): ApplicationResult<JobApplication> =
        ApplicationResult.Failure(ApplicationError.UNKNOWN)
    override suspend fun getApplicationDraft(
        jobId: String,
    ): ApplicationResult<ApplicationDraft> =
        ApplicationResult.Failure(ApplicationError.UNKNOWN)
    override suspend fun submitApplication(
        jobId: String,
        coverMessage: String,
    ): ApplicationResult<JobApplication> =
        ApplicationResult.Failure(ApplicationError.UNKNOWN)
}
