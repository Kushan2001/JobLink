package com.kushan.joblink.viewmodel

import com.kushan.joblink.MainDispatcherRule
import com.kushan.joblink.data.model.UserProfile
import com.kushan.joblink.data.model.UserRole
import com.kushan.joblink.data.repository.AuthError
import com.kushan.joblink.data.repository.AuthRepository
import com.kushan.joblink.data.repository.AuthResult
import com.kushan.joblink.data.repository.AuthenticationState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun loginSuccessExposesAuthenticatedProfile() = runTest {
        val profile = jobSeekerProfile()
        val repository = FakeAuthRepository().apply {
            loginResult = AuthResult.Success(profile)
        }
        val viewModel = AuthViewModel(repository)

        advanceUntilIdle()
        viewModel.login("person@example.com", "password123")
        advanceUntilIdle()

        assertEquals(profile, viewModel.uiState.value.currentUser)
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun registrationPreservesSelectedRole() = runTest {
        val profile = employerProfile()
        val repository = FakeAuthRepository().apply {
            registerResult = AuthResult.Success(profile)
        }
        val viewModel = AuthViewModel(repository)

        advanceUntilIdle()
        viewModel.register(
            fullName = profile.fullName,
            email = profile.email,
            password = "password123",
            role = UserRole.EMPLOYER,
        )
        advanceUntilIdle()

        assertEquals(UserRole.EMPLOYER, repository.registeredRole)
        assertEquals(profile, viewModel.uiState.value.currentUser)
    }

    @Test
    fun loginFailureStopsLoadingAndExposesUsefulError() = runTest {
        val repository = FakeAuthRepository().apply {
            loginResult = AuthResult.Failure(AuthError.INVALID_CREDENTIALS)
        }
        val viewModel = AuthViewModel(repository)

        advanceUntilIdle()
        viewModel.login("person@example.com", "wrong-password")
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.currentUser)
        assertEquals(AuthError.INVALID_CREDENTIALS, viewModel.uiState.value.error)
    }

    @Test
    fun existingFirebaseSessionRestoresApplicationProfile() = runTest {
        val profile = jobSeekerProfile()
        val repository = FakeAuthRepository(
            initialAuthenticationState = AuthenticationState.Authenticated(
                uid = profile.uid,
                email = profile.email,
            ),
        ).apply {
            currentProfileResult = AuthResult.Success(profile)
        }
        val viewModel = AuthViewModel(repository)

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isInitializing)
        assertEquals(profile, viewModel.uiState.value.currentUser)
    }

    @Test
    fun logoutClearsCurrentUser() = runTest {
        val profile = employerProfile()
        val repository = FakeAuthRepository().apply {
            loginResult = AuthResult.Success(profile)
        }
        val viewModel = AuthViewModel(repository)

        advanceUntilIdle()
        viewModel.login(profile.email, "password123")
        advanceUntilIdle()
        viewModel.logout()

        assertEquals(1, repository.logoutCalls)
        assertNull(viewModel.uiState.value.currentUser)
        assertFalse(viewModel.uiState.value.isInitializing)
    }

    private class FakeAuthRepository(
        initialAuthenticationState: AuthenticationState = AuthenticationState.Unauthenticated,
    ) : AuthRepository {
        private val authState = MutableStateFlow(initialAuthenticationState)
        override val authenticationState: Flow<AuthenticationState> = authState

        var loginResult: AuthResult<UserProfile> = AuthResult.Failure(AuthError.UNKNOWN)
        var registerResult: AuthResult<UserProfile> = AuthResult.Failure(AuthError.UNKNOWN)
        var currentProfileResult: AuthResult<UserProfile> = AuthResult.Failure(AuthError.UNKNOWN)
        var registeredRole: UserRole? = null
        var logoutCalls: Int = 0

        override suspend fun register(
            fullName: String,
            email: String,
            password: String,
            role: UserRole,
        ): AuthResult<UserProfile> {
            registeredRole = role
            return registerResult
        }

        override suspend fun login(
            email: String,
            password: String,
        ): AuthResult<UserProfile> = loginResult

        override suspend fun getCurrentUserProfile(): AuthResult<UserProfile> = currentProfileResult

        override fun logout() {
            logoutCalls += 1
            authState.value = AuthenticationState.Unauthenticated
        }
    }

    private fun jobSeekerProfile() = UserProfile(
        uid = "job-seeker-id",
        fullName = "Job Seeker",
        email = "person@example.com",
        role = UserRole.JOB_SEEKER,
    )

    private fun employerProfile() = UserProfile(
        uid = "employer-id",
        fullName = "Employer",
        email = "employer@example.com",
        role = UserRole.EMPLOYER,
    )
}
