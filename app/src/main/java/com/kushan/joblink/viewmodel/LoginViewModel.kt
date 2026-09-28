package com.kushan.joblink.viewmodel

import androidx.core.util.PatternsCompat
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val emailError: EmailValidationError? = null,
    val passwordError: PasswordValidationError? = null,
    val isLoading: Boolean = false,
)

enum class EmailValidationError {
    REQUIRED,
    INVALID_FORMAT,
}

enum class PasswordValidationError {
    REQUIRED,
}

class LoginViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChanged(email: String) {
        _uiState.update { currentState ->
            currentState.copy(
                email = email,
                emailError = null,
            )
        }
    }

    fun onPasswordChanged(password: String) {
        _uiState.update { currentState ->
            currentState.copy(
                password = password,
                passwordError = null,
            )
        }
    }

    fun onPasswordVisibilityChanged() {
        _uiState.update { currentState ->
            currentState.copy(isPasswordVisible = !currentState.isPasswordVisible)
        }
    }

    fun validateLoginInput(): Boolean {
        val currentState = _uiState.value
        val normalizedEmail = currentState.email.trim()
        val emailError = when {
            normalizedEmail.isEmpty() -> EmailValidationError.REQUIRED
            !PatternsCompat.EMAIL_ADDRESS.matcher(normalizedEmail).matches() -> {
                EmailValidationError.INVALID_FORMAT
            }

            else -> null
        }
        val passwordError = if (currentState.password.isBlank()) {
            PasswordValidationError.REQUIRED
        } else {
            null
        }

        _uiState.update {
            it.copy(
                email = normalizedEmail,
                emailError = emailError,
                passwordError = passwordError,
            )
        }

        return emailError == null && passwordError == null
    }
}
