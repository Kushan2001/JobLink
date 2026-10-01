package com.kushan.joblink.viewmodel

import com.kushan.joblink.MainDispatcherRule
import com.kushan.joblink.data.model.CvMetadata
import com.kushan.joblink.data.model.CvUploadFile
import com.kushan.joblink.data.model.JobSeekerProfile
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
class JobSeekerProfileViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun profileLoadsIntoViewState() = runTest {
        val profile = completeProfile()
        val repository = FakeProfileRepository(
            loadResult = ProfileResult.Success(profile),
        )
        val viewModel = JobSeekerProfileViewModel(repository)

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(profile, viewModel.uiState.value.savedProfile)
        assertEquals("Kotlin, Android", viewModel.uiState.value.skillsInput)
    }

    @Test
    fun validationPreventsInvalidProfileSave() = runTest {
        val repository = FakeProfileRepository(
            loadResult = ProfileResult.Success(completeProfile()),
        )
        val viewModel = JobSeekerProfileViewModel(repository)

        advanceUntilIdle()
        viewModel.startEditing()
        viewModel.onFullNameChanged("")
        viewModel.onPhoneChanged("12")
        viewModel.onSkillsChanged("")
        viewModel.saveProfile()
        advanceUntilIdle()

        assertEquals(0, repository.saveCalls)
        assertTrue(
            ProfileValidationError.FULL_NAME_REQUIRED in
                viewModel.uiState.value.validationErrors,
        )
        assertTrue(
            ProfileValidationError.PHONE_INVALID in
                viewModel.uiState.value.validationErrors,
        )
        assertTrue(
            ProfileValidationError.SKILLS_REQUIRED in
                viewModel.uiState.value.validationErrors,
        )
    }

    @Test
    fun successfulSaveNormalizesListsAndReturnsToViewMode() = runTest {
        val repository = FakeProfileRepository(
            loadResult = ProfileResult.Success(completeProfile()),
        )
        val viewModel = JobSeekerProfileViewModel(repository)

        advanceUntilIdle()
        viewModel.startEditing()
        viewModel.onSkillsChanged("Kotlin, Compose, kotlin")
        viewModel.onPreferredJobTypesChanged("Full-time, Remote")
        repository.saveHandler = { profile -> ProfileResult.Success(profile) }
        viewModel.saveProfile()
        advanceUntilIdle()

        assertEquals(listOf("Kotlin", "Compose"), repository.savedProfile?.skills)
        assertEquals(
            listOf("Full-time", "Remote"),
            repository.savedProfile?.preferredJobTypes,
        )
        assertFalse(viewModel.uiState.value.isEditing)
        assertTrue(viewModel.uiState.value.saveSucceeded)
    }

    @Test
    fun saveFailureKeepsFormEditableAndExposesError() = runTest {
        val repository = FakeProfileRepository(
            loadResult = ProfileResult.Success(completeProfile()),
        ).apply {
            saveHandler = { ProfileResult.Failure(ProfileError.NETWORK) }
        }
        val viewModel = JobSeekerProfileViewModel(repository)

        advanceUntilIdle()
        viewModel.startEditing()
        viewModel.saveProfile()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isEditing)
        assertFalse(viewModel.uiState.value.isSaving)
        assertEquals(ProfileError.NETWORK, viewModel.uiState.value.error)
    }

    @Test
    fun invalidCvSelectionShowsFileNameAndDoesNotUpload() = runTest {
        val repository = FakeProfileRepository(
            loadResult = ProfileResult.Success(completeProfile()),
        )
        val viewModel = JobSeekerProfileViewModel(repository)
        advanceUntilIdle()

        viewModel.onCvFileSelected(
            CvUploadFile(
                uri = "content://documents/resume.docx",
                fileName = "resume.docx",
                contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            ),
        )
        viewModel.uploadSelectedCv()
        advanceUntilIdle()

        assertEquals("resume.docx", viewModel.uiState.value.selectedCvFile?.fileName)
        assertEquals(ProfileError.INVALID_CV_FILE, viewModel.uiState.value.cvUploadError)
        assertEquals(0, repository.uploadCalls)
    }

    @Test
    fun successfulCvUploadReportsProgressAndUpdatesProfileMetadata() = runTest {
        val repository = FakeProfileRepository(
            loadResult = ProfileResult.Success(completeProfile()),
        )
        val cvMetadata = CvMetadata(
            fileName = "Kushan-CV.pdf",
            storagePath = "users/job-seeker-id/cv/current.pdf",
            sizeBytes = 1_024L,
        )
        repository.uploadHandler = { _, onProgress ->
            onProgress(0.45f)
            ProfileResult.Success(cvMetadata)
        }
        val viewModel = JobSeekerProfileViewModel(repository)
        advanceUntilIdle()

        viewModel.onCvFileSelected(validCvFile())
        viewModel.uploadSelectedCv()
        advanceUntilIdle()

        assertEquals(1, repository.uploadCalls)
        assertEquals(cvMetadata, viewModel.uiState.value.savedProfile?.cv)
        assertEquals(1f, viewModel.uiState.value.cvUploadProgress)
        assertTrue(viewModel.uiState.value.cvUploadSucceeded)
        assertEquals(null, viewModel.uiState.value.selectedCvFile)
    }

    @Test
    fun cvUploadFailureKeepsSelectionAndExposesError() = runTest {
        val repository = FakeProfileRepository(
            loadResult = ProfileResult.Success(completeProfile()),
        ).apply {
            uploadHandler = { _, onProgress ->
                onProgress(0.2f)
                ProfileResult.Failure(ProfileError.NETWORK)
            }
        }
        val viewModel = JobSeekerProfileViewModel(repository)
        advanceUntilIdle()

        viewModel.onCvFileSelected(validCvFile())
        viewModel.uploadSelectedCv()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isUploadingCv)
        assertEquals(ProfileError.NETWORK, viewModel.uiState.value.cvUploadError)
        assertEquals("Kushan-CV.pdf", viewModel.uiState.value.selectedCvFile?.fileName)
        assertEquals(0.2f, viewModel.uiState.value.cvUploadProgress)
    }

    private class FakeProfileRepository(
        private val loadResult: ProfileResult<JobSeekerProfile>,
    ) : JobSeekerProfileRepository {
        var saveCalls = 0
        var savedProfile: JobSeekerProfile? = null
        var saveHandler: (JobSeekerProfile) -> ProfileResult<JobSeekerProfile> = {
            ProfileResult.Success(it)
        }
        var uploadCalls = 0
        var uploadHandler: (
            CvUploadFile,
            (Float) -> Unit,
        ) -> ProfileResult<CvMetadata> = { file, _ ->
            ProfileResult.Success(
                CvMetadata(
                    fileName = file.fileName,
                    storagePath = "users/job-seeker-id/cv/current.pdf",
                ),
            )
        }

        override suspend fun getProfile(): ProfileResult<JobSeekerProfile> = loadResult

        override suspend fun saveProfile(
            profile: JobSeekerProfile,
        ): ProfileResult<JobSeekerProfile> {
            saveCalls += 1
            savedProfile = profile
            return saveHandler(profile)
        }

        override suspend fun uploadCv(
            file: CvUploadFile,
            onProgress: (Float) -> Unit,
        ): ProfileResult<CvMetadata> {
            uploadCalls += 1
            return uploadHandler(file, onProgress)
        }
    }

    private fun validCvFile() = CvUploadFile(
        uri = "content://documents/Kushan-CV.pdf",
        fileName = "Kushan-CV.pdf",
        contentType = "application/pdf",
    )

    private fun completeProfile() = JobSeekerProfile(
        uid = "job-seeker-id",
        fullName = "Job Seeker",
        professionalHeadline = "Junior Android Developer",
        location = "Colombo",
        phone = "+94 77 123 4567",
        bio = "Android developer focused on accessible applications.",
        education = "BSc in Software Engineering",
        experienceSummary = "Built Kotlin and Compose portfolio applications.",
        skills = listOf("Kotlin", "Android"),
        preferredJobTypes = listOf("Full-time", "Remote"),
    )
}
