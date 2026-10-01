package com.kushan.joblink.viewmodel

import com.kushan.joblink.MainDispatcherRule
import com.kushan.joblink.data.model.ApplicationDraft
import com.kushan.joblink.data.model.ApplicationStatus
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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ApplicationViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun applicationDraftLoadsJobAndCvDetails() = runTest {
        val draft = applicationDraft()
        val repository = FakeApplicationRepository(
            draftResult = ApplicationResult.Success(draft),
        )

        val viewModel = ApplicationViewModel("job-1", repository)
        advanceUntilIdle()

        assertEquals("job-1", repository.requestedDraftJobId)
        assertEquals(draft, viewModel.uiState.value.draft)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun submissionUsesRouteJobAndOptionalCoverMessage() = runTest {
        val repository = FakeApplicationRepository(
            draftResult = ApplicationResult.Success(applicationDraft()),
        )
        val submittedApplication = JobApplication(
            applicationId = "job-1_applicant-1",
            jobId = "job-1",
            employerId = "employer-1",
            applicantId = "applicant-1",
            status = ApplicationStatus.SUBMITTED,
            coverMessage = "I am a strong fit.",
            cvReference = "users/applicant-1/cv/current.pdf",
        )
        repository.submitResult = ApplicationResult.Success(submittedApplication)
        val viewModel = ApplicationViewModel("job-1", repository)
        advanceUntilIdle()

        viewModel.onCoverMessageChanged("I am a strong fit.")
        viewModel.submitApplication()
        advanceUntilIdle()

        assertEquals("job-1", repository.submittedJobId)
        assertEquals("I am a strong fit.", repository.submittedCoverMessage)
        assertEquals(submittedApplication, viewModel.uiState.value.submittedApplication)
        assertFalse(viewModel.uiState.value.isSubmitting)
    }

    @Test
    fun cvRequiredPreventsDraftFormFromOpening() = runTest {
        val repository = FakeApplicationRepository(
            draftResult = ApplicationResult.Failure(ApplicationError.CV_REQUIRED),
        )

        val viewModel = ApplicationViewModel("job-1", repository)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.draft)
        assertEquals(ApplicationError.CV_REQUIRED, viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun duplicateSubmissionFailureKeepsDraftAvailable() = runTest {
        val repository = FakeApplicationRepository(
            draftResult = ApplicationResult.Success(applicationDraft()),
        ).apply {
            submitResult = ApplicationResult.Failure(ApplicationError.ALREADY_APPLIED)
        }
        val viewModel = ApplicationViewModel("job-1", repository)
        advanceUntilIdle()

        viewModel.submitApplication()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.draft != null)
        assertEquals(ApplicationError.ALREADY_APPLIED, viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isSubmitting)
    }

    private class FakeApplicationRepository(
        private val draftResult: ApplicationResult<ApplicationDraft>,
    ) : ApplicationRepository {
        var requestedDraftJobId: String? = null
        var submittedJobId: String? = null
        var submittedCoverMessage: String? = null
        var submitResult: ApplicationResult<JobApplication> =
            ApplicationResult.Failure(ApplicationError.UNKNOWN)

        override suspend fun getMyApplications(): ApplicationResult<List<JobApplication>> =
            ApplicationResult.Success(emptyList())

        override suspend fun getMyApplication(
            applicationId: String,
        ): ApplicationResult<JobApplication> =
            ApplicationResult.Failure(ApplicationError.APPLICATION_NOT_FOUND)

        override suspend fun getApplicationDraft(
            jobId: String,
        ): ApplicationResult<ApplicationDraft> {
            requestedDraftJobId = jobId
            return draftResult
        }

        override suspend fun submitApplication(
            jobId: String,
            coverMessage: String,
        ): ApplicationResult<JobApplication> {
            submittedJobId = jobId
            submittedCoverMessage = coverMessage
            return submitResult
        }
    }

    private fun applicationDraft() = ApplicationDraft(
        jobId = "job-1",
        jobTitle = "Android Developer",
        companyName = "Acme Labs",
        cvFileName = "Candidate-CV.pdf",
    )
}
