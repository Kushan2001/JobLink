package com.kushan.joblink.viewmodel

import com.kushan.joblink.MainDispatcherRule
import com.kushan.joblink.data.model.ApplicationStatus
import com.kushan.joblink.data.model.JobApplication
import com.kushan.joblink.data.repository.ApplicationError
import com.kushan.joblink.data.repository.ApplicationResult
import java.io.File
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
class ApplicantDetailsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun detailsLoadAndAllowedStatusCanBeUpdated() = runTest {
        val submitted = application()
        val reviewed = submitted.copy(status = ApplicationStatus.REVIEWED)
        val repository = FakeApplicationRepository(
            detailResult = ApplicationResult.Success(submitted),
            updateResult = ApplicationResult.Success(reviewed),
        )
        val viewModel = ApplicantDetailsViewModel("application-1", repository)
        advanceUntilIdle()

        viewModel.updateStatus(ApplicationStatus.REVIEWED)
        advanceUntilIdle()

        assertEquals("application-1", repository.requestedApplicationId)
        assertEquals(ApplicationStatus.REVIEWED, repository.requestedStatus)
        assertEquals(reviewed, viewModel.uiState.value.application)
        assertFalse(viewModel.uiState.value.isUpdatingStatus)
    }

    @Test
    fun statusFailureIsShownWithoutDiscardingApplicant() = runTest {
        val submitted = application()
        val repository = FakeApplicationRepository(
            detailResult = ApplicationResult.Success(submitted),
            updateResult = ApplicationResult.Failure(ApplicationError.PERMISSION_DENIED),
        )
        val viewModel = ApplicantDetailsViewModel("application-1", repository)
        advanceUntilIdle()

        viewModel.updateStatus(ApplicationStatus.SHORTLISTED)
        advanceUntilIdle()

        assertEquals(submitted, viewModel.uiState.value.application)
        assertEquals(ApplicationError.PERMISSION_DENIED, viewModel.uiState.value.actionError)
    }

    @Test
    fun cvDownloadReportsProgressAndProducesPrivateFile() = runTest {
        val file = File("candidate.pdf")
        val repository = FakeApplicationRepository(
            detailResult = ApplicationResult.Success(application()),
            downloadResult = ApplicationResult.Success(file),
        )
        val viewModel = ApplicantDetailsViewModel("application-1", repository)
        advanceUntilIdle()

        viewModel.downloadCv()
        advanceUntilIdle()

        assertEquals(1f, viewModel.uiState.value.cvDownloadProgress)
        assertEquals(file, viewModel.uiState.value.downloadedCv)
        assertFalse(viewModel.uiState.value.isDownloadingCv)

        viewModel.consumeDownloadedCv()
        assertNull(viewModel.uiState.value.downloadedCv)
    }

    @Test
    fun viewerFailureCreatesUsefulActionError() = runTest {
        val repository = FakeApplicationRepository(
            detailResult = ApplicationResult.Success(application()),
        )
        val viewModel = ApplicantDetailsViewModel("application-1", repository)
        advanceUntilIdle()

        viewModel.onCvViewerUnavailable()

        assertEquals(ApplicationError.CV_VIEWER_UNAVAILABLE, viewModel.uiState.value.actionError)
        assertTrue(viewModel.uiState.value.application != null)
    }

    private class FakeApplicationRepository(
        private val detailResult: ApplicationResult<JobApplication>,
        private val updateResult: ApplicationResult<JobApplication> =
            ApplicationResult.Failure(ApplicationError.UNKNOWN),
        private val downloadResult: ApplicationResult<File> =
            ApplicationResult.Failure(ApplicationError.UNKNOWN),
    ) : EmptyApplicationRepository() {
        var requestedApplicationId: String? = null
        var requestedStatus: ApplicationStatus? = null

        override suspend fun getEmployerApplication(applicationId: String): ApplicationResult<JobApplication> {
            requestedApplicationId = applicationId
            return detailResult
        }

        override suspend fun updateEmployerApplicationStatus(
            applicationId: String,
            status: ApplicationStatus,
        ): ApplicationResult<JobApplication> {
            requestedStatus = status
            return updateResult
        }

        override suspend fun downloadApplicantCv(
            applicationId: String,
            onProgress: (Float) -> Unit,
        ): ApplicationResult<File> {
            onProgress(0.5f)
            return downloadResult
        }
    }
}

private fun application() = JobApplication(
    applicationId = "application-1",
    jobId = "job-1",
    employerId = "employer-1",
    applicantId = "applicant-1",
    applicantFullName = "Alex Silva",
    status = ApplicationStatus.SUBMITTED,
    cvReference = "users/applicant-1/cv/current.pdf",
)
