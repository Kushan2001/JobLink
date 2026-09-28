package com.kushan.joblink.viewmodel

import com.kushan.joblink.MainDispatcherRule
import com.kushan.joblink.data.model.CompanyProfile
import com.kushan.joblink.data.repository.EmployerProfileRepository
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
class EmployerProfileViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun companyProfileLoadsIntoViewState() = runTest {
        val profile = completeProfile()
        val repository = FakeEmployerProfileRepository(
            loadResult = ProfileResult.Success(profile),
        )
        val viewModel = EmployerProfileViewModel(repository)

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(profile, viewModel.uiState.value.savedProfile)
        assertEquals(profile.companyName, viewModel.uiState.value.companyName)
    }

    @Test
    fun validationPreventsInvalidCompanySave() = runTest {
        val repository = FakeEmployerProfileRepository(
            loadResult = ProfileResult.Success(completeProfile()),
        )
        val viewModel = EmployerProfileViewModel(repository)

        advanceUntilIdle()
        viewModel.startEditing()
        viewModel.onCompanyNameChanged("")
        viewModel.onWebsiteChanged("not a website")
        viewModel.onContactEmailChanged("invalid-email")
        viewModel.saveProfile()
        advanceUntilIdle()

        assertEquals(0, repository.saveCalls)
        assertTrue(
            EmployerProfileValidationError.COMPANY_NAME_REQUIRED in
                viewModel.uiState.value.validationErrors,
        )
        assertTrue(
            EmployerProfileValidationError.WEBSITE_INVALID in
                viewModel.uiState.value.validationErrors,
        )
        assertTrue(
            EmployerProfileValidationError.CONTACT_EMAIL_INVALID in
                viewModel.uiState.value.validationErrors,
        )
    }

    @Test
    fun successfulSaveTrimsFieldsAndReturnsToViewMode() = runTest {
        val repository = FakeEmployerProfileRepository(
            loadResult = ProfileResult.Success(completeProfile()),
        )
        val viewModel = EmployerProfileViewModel(repository)

        advanceUntilIdle()
        viewModel.startEditing()
        viewModel.onCompanyNameChanged("  JobLink Labs  ")
        repository.saveHandler = { profile -> ProfileResult.Success(profile) }
        viewModel.saveProfile()
        advanceUntilIdle()

        assertEquals("JobLink Labs", repository.savedProfile?.companyName)
        assertFalse(viewModel.uiState.value.isEditing)
        assertTrue(viewModel.uiState.value.saveSucceeded)
    }

    @Test
    fun saveFailureKeepsFormEditableAndExposesError() = runTest {
        val repository = FakeEmployerProfileRepository(
            loadResult = ProfileResult.Success(completeProfile()),
        ).apply {
            saveHandler = { ProfileResult.Failure(ProfileError.PERMISSION_DENIED) }
        }
        val viewModel = EmployerProfileViewModel(repository)

        advanceUntilIdle()
        viewModel.startEditing()
        viewModel.saveProfile()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isEditing)
        assertFalse(viewModel.uiState.value.isSaving)
        assertEquals(ProfileError.PERMISSION_DENIED, viewModel.uiState.value.error)
    }

    private class FakeEmployerProfileRepository(
        private val loadResult: ProfileResult<CompanyProfile>,
    ) : EmployerProfileRepository {
        var saveCalls = 0
        var savedProfile: CompanyProfile? = null
        var saveHandler: (CompanyProfile) -> ProfileResult<CompanyProfile> = {
            ProfileResult.Success(it)
        }

        override suspend fun getCompanyProfile(): ProfileResult<CompanyProfile> = loadResult

        override suspend fun saveCompanyProfile(
            profile: CompanyProfile,
        ): ProfileResult<CompanyProfile> {
            saveCalls += 1
            savedProfile = profile
            return saveHandler(profile)
        }
    }

    private fun completeProfile() = CompanyProfile(
        ownerUid = "employer-id",
        companyName = "JobLink Labs",
        companyDescription = "A product company building employment tools.",
        industry = "Software",
        companySize = "11–50 employees",
        location = "Colombo",
        website = "https://example.com",
        contactEmail = "careers@example.com",
    )
}
