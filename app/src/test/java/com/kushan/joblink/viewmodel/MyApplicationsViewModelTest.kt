package com.kushan.joblink.viewmodel

import com.kushan.joblink.MainDispatcherRule
import com.kushan.joblink.data.model.ApplicationDraft
import com.kushan.joblink.data.model.ApplicationStatus
import com.kushan.joblink.data.model.EmployerApplicationsData
import com.kushan.joblink.data.model.JobApplication
import com.kushan.joblink.data.repository.ApplicationError
import com.kushan.joblink.data.repository.ApplicationRepository
import com.kushan.joblink.data.repository.ApplicationResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class MyApplicationsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun applicationsLoadAndCanBeFilteredByStatus() = runTest {
        val submitted = application("submitted", ApplicationStatus.SUBMITTED)
        val shortlisted = application("shortlisted", ApplicationStatus.SHORTLISTED)
        val repository = FakeApplicationRepository(
            mutableListOf(ApplicationResult.Success(listOf(submitted, shortlisted))),
        )

        val viewModel = MyApplicationsViewModel(repository)
        advanceUntilIdle()

        assertEquals(listOf(submitted, shortlisted), viewModel.uiState.value.filteredApplications)
        assertFalse(viewModel.uiState.value.isLoading)

        viewModel.onStatusSelected(ApplicationStatus.SHORTLISTED)

        assertEquals(listOf(shortlisted), viewModel.uiState.value.filteredApplications)
        assertEquals(ApplicationStatus.SHORTLISTED, viewModel.uiState.value.selectedStatus)
    }

    @Test
    fun emptyApplicationsCreateEmptyReadyState() = runTest {
        val repository = FakeApplicationRepository(
            mutableListOf(ApplicationResult.Success(emptyList())),
        )

        val viewModel = MyApplicationsViewModel(repository)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.applications.isEmpty())
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(null, viewModel.uiState.value.error)
    }

    @Test
    fun refreshFailureKeepsApplicationsAndSelectedFilter() = runTest {
        val applications = listOf(application("reviewed", ApplicationStatus.REVIEWED))
        val repository = FakeApplicationRepository(
            mutableListOf(
                ApplicationResult.Success(applications),
                ApplicationResult.Failure(ApplicationError.NETWORK),
            ),
        )
        val viewModel = MyApplicationsViewModel(repository)
        advanceUntilIdle()
        viewModel.onStatusSelected(ApplicationStatus.REVIEWED)

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(applications, viewModel.uiState.value.applications)
        assertEquals(ApplicationStatus.REVIEWED, viewModel.uiState.value.selectedStatus)
        assertEquals(ApplicationError.NETWORK, viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isRefreshing)
    }

    @Test
    fun initialFailureCreatesErrorState() = runTest {
        val repository = FakeApplicationRepository(
            mutableListOf(ApplicationResult.Failure(ApplicationError.PERMISSION_DENIED)),
        )

        val viewModel = MyApplicationsViewModel(repository)
        advanceUntilIdle()

        assertEquals(ApplicationError.PERMISSION_DENIED, viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    private fun application(id: String, status: ApplicationStatus) = JobApplication(
        applicationId = id,
        jobId = "job-$id",
        employerId = "employer-1",
        applicantId = "applicant-1",
        jobTitle = "Android Developer",
        companyName = "JobLink Labs",
        status = status,
        cvReference = "users/applicant-1/cv/current.pdf",
    )

    private class FakeApplicationRepository(
        private val applicationResults: MutableList<ApplicationResult<List<JobApplication>>>,
    ) : ApplicationRepository {
        override suspend fun getRecentEmployerApplications(
            limit: Long,
        ): ApplicationResult<List<JobApplication>> = ApplicationResult.Success(emptyList())

        override suspend fun getEmployerApplications(jobId: String) =
            ApplicationResult.Success(EmployerApplicationsData(jobId, "", emptyList()))

        override suspend fun getEmployerApplication(applicationId: String) =
            ApplicationResult.Failure(ApplicationError.APPLICATION_NOT_FOUND)

        override suspend fun updateEmployerApplicationStatus(
            applicationId: String,
            status: ApplicationStatus,
        ) = ApplicationResult.Failure(ApplicationError.APPLICATION_NOT_FOUND)

        override suspend fun downloadApplicantCv(
            applicationId: String,
            onProgress: (Float) -> Unit,
        ): ApplicationResult<File> = ApplicationResult.Failure(ApplicationError.CV_NOT_AVAILABLE)

        override suspend fun getMyApplications(): ApplicationResult<List<JobApplication>> =
            applicationResults.removeAt(0)

        override suspend fun getMyApplication(
            applicationId: String,
        ): ApplicationResult<JobApplication> =
            ApplicationResult.Failure(ApplicationError.APPLICATION_NOT_FOUND)

        override suspend fun getApplicationDraft(
            jobId: String,
        ): ApplicationResult<ApplicationDraft> =
            ApplicationResult.Failure(ApplicationError.JOB_NOT_FOUND)

        override suspend fun submitApplication(
            jobId: String,
            coverMessage: String,
        ): ApplicationResult<JobApplication> =
            ApplicationResult.Failure(ApplicationError.UNKNOWN)
    }
}
