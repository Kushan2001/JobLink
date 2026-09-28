package com.kushan.joblink.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kushan.joblink.data.model.UserProfile
import com.kushan.joblink.data.model.UserRole
import com.kushan.joblink.data.repository.AuthError
import com.kushan.joblink.data.repository.AuthRepository
import com.kushan.joblink.data.repository.AuthResult
import com.kushan.joblink.data.repository.AuthenticationState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val isInitializing: Boolean = true,
    val isLoading: Boolean = false,
    val currentUser: UserProfile? = null,
    val error: AuthError? = null,
)

class AuthViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        observeAuthenticationState()
    }

    fun register(
        fullName: String,
        email: String,
        password: String,
        role: UserRole,
    ) {
        performAuthentication {
            authRepository.register(
                fullName = fullName,
                email = email,
                password = password,
                role = role,
            )
        }
    }

    fun login(email: String, password: String) {
        performAuthentication { authRepository.login(email, password) }
    }

    fun logout() {
        authRepository.logout()
        _uiState.value = AuthUiState(isInitializing = false)
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun observeAuthenticationState() {
        viewModelScope.launch {
            authRepository.authenticationState.collectLatest { authState ->
                when (authState) {
                    AuthenticationState.Loading -> {
                        if (!_uiState.value.isLoading && _uiState.value.currentUser == null) {
                            _uiState.update { it.copy(isInitializing = true) }
                        }
                    }

                    AuthenticationState.Unauthenticated -> {
                        if (!_uiState.value.isLoading) {
                            _uiState.update {
                                it.copy(
                                    isInitializing = false,
                                    currentUser = null,
                                )
                            }
                        }
                    }

                    is AuthenticationState.Authenticated -> {
                        val needsProfile = _uiState.value.currentUser?.uid != authState.uid
                        if (!_uiState.value.isLoading && needsProfile) {
                            restoreCurrentUserProfile()
                        }
                    }
                }
            }
        }
    }

    private suspend fun restoreCurrentUserProfile() {
        _uiState.update { it.copy(isInitializing = true, error = null) }

        when (val result = authRepository.getCurrentUserProfile()) {
            is AuthResult.Success -> {
                _uiState.update {
                    it.copy(
                        isInitializing = false,
                        currentUser = result.value,
                    )
                }
            }

            is AuthResult.Failure -> {
                authRepository.logout()
                _uiState.update {
                    it.copy(
                        isInitializing = false,
                        currentUser = null,
                        error = result.error,
                    )
                }
            }
        }
    }

    private fun performAuthentication(
        operation: suspend () -> AuthResult<UserProfile>,
    ) {
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isInitializing = false,
                    isLoading = true,
                    error = null,
                )
            }

            when (val result = operation()) {
                is AuthResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            currentUser = result.value,
                        )
                    }
                }

                is AuthResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            currentUser = null,
                            error = result.error,
                        )
                    }
                }
            }
        }
    }

    class Factory(
        private val authRepository: AuthRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(AuthViewModel::class.java)) {
                "Unknown ViewModel class: ${modelClass.name}"
            }
            return AuthViewModel(authRepository) as T
        }
    }
}
