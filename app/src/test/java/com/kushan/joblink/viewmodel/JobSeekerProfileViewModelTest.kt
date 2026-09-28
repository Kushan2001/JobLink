package com.kushan.joblink.viewmodel

import com.kushan.joblink.MainDispatcherRule
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

    private class FakeProfileRepository(
        private val loadResult: ProfileResult<JobSeekerProfile>,
    ) : JobSeekerProfileRepository {
        var saveCalls = 0
        var savedProfile: JobSeekerProfile? = null
        var saveHandler: (JobSeekerProfile) -> ProfileResult<JobSeekerProfile> = {
            ProfileResult.Success(it)
        }

        override suspend fun getProfile(): ProfileResult<JobSeekerProfile> = loadResult

        override suspend fun saveProfile(
            profile: JobSeekerProfile,
        ): ProfileResult<JobSeekerProfile> {
            saveCalls += 1
            savedProfile = profile
            return saveHandler(profile)
        }
    }

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
