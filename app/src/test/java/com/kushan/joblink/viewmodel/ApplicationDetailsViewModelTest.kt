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
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class ApplicationDetailsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun applicationDetailsLoadForRouteId() = runTest {
        val application = JobApplication(
            applicationId = "application-1",
            jobId = "job-1",
            applicantId = "applicant-1",
        )
        val repository = FakeApplicationRepository(ApplicationResult.Success(application))

        val viewModel = ApplicationDetailsViewModel("application-1", repository)
        advanceUntilIdle()

        assertEquals("application-1", repository.requestedApplicationId)
        assertEquals(application, viewModel.uiState.value.application)
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun missingApplicationCreatesNotFoundState() = runTest {
        val repository = FakeApplicationRepository(
            ApplicationResult.Failure(ApplicationError.APPLICATION_NOT_FOUND),
        )

        val viewModel = ApplicationDetailsViewModel("missing", repository)
        advanceUntilIdle()

        assertEquals(ApplicationError.APPLICATION_NOT_FOUND, viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.application)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    private class FakeApplicationRepository(
        private val detailResult: ApplicationResult<JobApplication>,
    ) : ApplicationRepository {
        var requestedApplicationId: String? = null

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
            ApplicationResult.Success(emptyList())

        override suspend fun getMyApplication(
            applicationId: String,
        ): ApplicationResult<JobApplication> {
            requestedApplicationId = applicationId
            return detailResult
        }

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
