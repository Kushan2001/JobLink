package com.kushan.joblink.data.repository

import com.kushan.joblink.data.model.UserProfile
import com.kushan.joblink.data.model.UserRole
import kotlinx.coroutines.flow.Flow

sealed interface AuthenticationState {
    data object Loading : AuthenticationState
    data object Unauthenticated : AuthenticationState

    data class Authenticated(
        val uid: String,
        val email: String,
    ) : AuthenticationState
}

enum class AuthError {
    INVALID_CREDENTIALS,
    EMAIL_ALREADY_IN_USE,
    WEAK_PASSWORD,
    USER_DISABLED,
    TOO_MANY_REQUESTS,
    NETWORK,
    PROFILE_NOT_FOUND,
    PROFILE_INVALID,
    PROFILE_SAVE_FAILED,
    PERMISSION_DENIED,
    UNKNOWN,
}

sealed interface AuthResult<out T> {
    data class Success<T>(val value: T) : AuthResult<T>
    data class Failure(val error: AuthError) : AuthResult<Nothing>
}

interface AuthRepository {
    val authenticationState: Flow<AuthenticationState>

    suspend fun register(
        fullName: String,
        email: String,
        password: String,
        role: UserRole,
    ): AuthResult<UserProfile>

    suspend fun login(email: String, password: String): AuthResult<UserProfile>

    suspend fun getCurrentUserProfile(): AuthResult<UserProfile>

    fun logout()
}
