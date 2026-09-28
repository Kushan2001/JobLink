package com.kushan.joblink.viewmodel

import androidx.core.util.PatternsCompat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.navigation.toRoute
import com.kushan.joblink.data.model.UserRole
import com.kushan.joblink.navigation.RegisterDestination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

private const val MINIMUM_PASSWORD_LENGTH = 8

data class RegisterUiState(
    val role: UserRole,
    val fullName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val fullNameError: FullNameValidationError? = null,
    val emailError: RegistrationEmailValidationError? = null,
    val passwordError: RegistrationPasswordValidationError? = null,
    val confirmPasswordError: ConfirmPasswordValidationError? = null,
)

enum class FullNameValidationError {
    REQUIRED,
}

enum class RegistrationEmailValidationError {
    REQUIRED,
    INVALID_FORMAT,
}

enum class RegistrationPasswordValidationError {
    REQUIRED,
    TOO_SHORT,
}

enum class ConfirmPasswordValidationError {
    REQUIRED,
    DOES_NOT_MATCH,
}

class RegisterViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    private val selectedRole = savedStateHandle.toRoute<RegisterDestination>().role

    private val _uiState = MutableStateFlow(RegisterUiState(role = selectedRole))
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onFullNameChanged(fullName: String) {
        _uiState.update { it.copy(fullName = fullName, fullNameError = null) }
    }

    fun onEmailChanged(email: String) {
        _uiState.update { it.copy(email = email, emailError = null) }
    }

    fun onPasswordChanged(password: String) {
        _uiState.update {
            it.copy(
                password = password,
                passwordError = null,
                confirmPasswordError = null,
            )
        }
    }

    fun onConfirmPasswordChanged(confirmPassword: String) {
        _uiState.update {
            it.copy(
                confirmPassword = confirmPassword,
                confirmPasswordError = null,
            )
        }
    }

    fun onPasswordVisibilityChanged() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun onConfirmPasswordVisibilityChanged() {
        _uiState.update { it.copy(isConfirmPasswordVisible = !it.isConfirmPasswordVisible) }
    }

    fun validateRegistrationInput(): Boolean {
        val currentState = _uiState.value
        val normalizedFullName = currentState.fullName.trim()
        val normalizedEmail = currentState.email.trim()

        val fullNameError = if (normalizedFullName.isEmpty()) {
            FullNameValidationError.REQUIRED
        } else {
            null
        }
        val emailError = when {
            normalizedEmail.isEmpty() -> RegistrationEmailValidationError.REQUIRED
            !PatternsCompat.EMAIL_ADDRESS.matcher(normalizedEmail).matches() -> {
                RegistrationEmailValidationError.INVALID_FORMAT
            }

            else -> null
        }
        val passwordError = when {
            currentState.password.isEmpty() -> RegistrationPasswordValidationError.REQUIRED
            currentState.password.length < MINIMUM_PASSWORD_LENGTH -> {
                RegistrationPasswordValidationError.TOO_SHORT
            }

            else -> null
        }
        val confirmPasswordError = when {
            currentState.confirmPassword.isEmpty() -> ConfirmPasswordValidationError.REQUIRED
            currentState.password != currentState.confirmPassword -> {
                ConfirmPasswordValidationError.DOES_NOT_MATCH
            }

            else -> null
        }

        _uiState.update {
            it.copy(
                fullName = normalizedFullName,
                email = normalizedEmail,
                fullNameError = fullNameError,
                emailError = emailError,
                passwordError = passwordError,
                confirmPasswordError = confirmPasswordError,
            )
        }

        return fullNameError == null &&
            emailError == null &&
            passwordError == null &&
            confirmPasswordError == null
    }
}
