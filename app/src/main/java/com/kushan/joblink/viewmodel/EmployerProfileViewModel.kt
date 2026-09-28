package com.kushan.joblink.viewmodel

import androidx.core.util.PatternsCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kushan.joblink.data.model.CompanyProfile
import com.kushan.joblink.data.repository.EmployerProfileRepository
import com.kushan.joblink.data.repository.ProfileError
import com.kushan.joblink.data.repository.ProfileResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EmployerProfileUiState(
    val ownerUid: String = "",
    val companyName: String = "",
    val companyDescription: String = "",
    val industry: String = "",
    val companySize: String = "",
    val location: String = "",
    val website: String = "",
    val contactEmail: String = "",
    val savedProfile: CompanyProfile? = null,
    val validationErrors: Set<EmployerProfileValidationError> = emptySet(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isEditing: Boolean = false,
    val saveSucceeded: Boolean = false,
    val error: ProfileError? = null,
)

enum class EmployerProfileValidationError {
    COMPANY_NAME_REQUIRED,
    DESCRIPTION_REQUIRED,
    INDUSTRY_REQUIRED,
    COMPANY_SIZE_REQUIRED,
    LOCATION_REQUIRED,
    WEBSITE_INVALID,
    CONTACT_EMAIL_REQUIRED,
    CONTACT_EMAIL_INVALID,
}

class EmployerProfileViewModel(
    private val profileRepository: EmployerProfileRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(EmployerProfileUiState())
    val uiState: StateFlow<EmployerProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        if (_uiState.value.isSaving) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    saveSucceeded = false,
                )
            }

            when (val result = profileRepository.getCompanyProfile()) {
                is ProfileResult.Success -> {
                    _uiState.value = result.value.toUiState()
                }

                is ProfileResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.error,
                        )
                    }
                }
            }
        }
    }

    fun startEditing() {
        val profile = _uiState.value.savedProfile ?: return
        _uiState.value = profile.toUiState(isEditing = true)
    }

    fun cancelEditing() {
        val profile = _uiState.value.savedProfile ?: return
        _uiState.value = profile.toUiState()
    }

    fun onCompanyNameChanged(value: String) =
        updateField(EmployerProfileValidationError.COMPANY_NAME_REQUIRED) {
            copy(companyName = value)
        }

    fun onCompanyDescriptionChanged(value: String) =
        updateField(EmployerProfileValidationError.DESCRIPTION_REQUIRED) {
            copy(companyDescription = value)
        }

    fun onIndustryChanged(value: String) =
        updateField(EmployerProfileValidationError.INDUSTRY_REQUIRED) {
            copy(industry = value)
        }

    fun onCompanySizeChanged(value: String) =
        updateField(EmployerProfileValidationError.COMPANY_SIZE_REQUIRED) {
            copy(companySize = value)
        }

    fun onLocationChanged(value: String) =
        updateField(EmployerProfileValidationError.LOCATION_REQUIRED) {
            copy(location = value)
        }

    fun onWebsiteChanged(value: String) =
        updateField(EmployerProfileValidationError.WEBSITE_INVALID) {
            copy(website = value)
        }

    fun onContactEmailChanged(value: String) {
        _uiState.update { currentState ->
            currentState.copy(
                contactEmail = value,
                validationErrors = currentState.validationErrors - setOf(
                    EmployerProfileValidationError.CONTACT_EMAIL_REQUIRED,
                    EmployerProfileValidationError.CONTACT_EMAIL_INVALID,
                ),
                error = null,
                saveSucceeded = false,
            )
        }
    }

    fun saveProfile() {
        val currentState = _uiState.value
        if (currentState.isSaving) return

        val normalizedWebsite = currentState.website.trim()
        val normalizedEmail = currentState.contactEmail.trim()
        val errors = buildSet {
            if (currentState.companyName.isBlank()) {
                add(EmployerProfileValidationError.COMPANY_NAME_REQUIRED)
            }
            if (currentState.companyDescription.isBlank()) {
                add(EmployerProfileValidationError.DESCRIPTION_REQUIRED)
            }
            if (currentState.industry.isBlank()) {
                add(EmployerProfileValidationError.INDUSTRY_REQUIRED)
            }
            if (currentState.companySize.isBlank()) {
                add(EmployerProfileValidationError.COMPANY_SIZE_REQUIRED)
            }
            if (currentState.location.isBlank()) {
                add(EmployerProfileValidationError.LOCATION_REQUIRED)
            }
            if (
                normalizedWebsite.isNotEmpty() &&
                !PatternsCompat.WEB_URL.matcher(normalizedWebsite).matches()
            ) {
                add(EmployerProfileValidationError.WEBSITE_INVALID)
            }
            when {
                normalizedEmail.isEmpty() -> {
                    add(EmployerProfileValidationError.CONTACT_EMAIL_REQUIRED)
                }

                !PatternsCompat.EMAIL_ADDRESS.matcher(normalizedEmail).matches() -> {
                    add(EmployerProfileValidationError.CONTACT_EMAIL_INVALID)
                }
            }
        }

        if (errors.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    validationErrors = errors,
                    saveSucceeded = false,
                )
            }
            return
        }

        val profile = CompanyProfile(
            ownerUid = currentState.ownerUid,
            companyName = currentState.companyName.trim(),
            companyDescription = currentState.companyDescription.trim(),
            industry = currentState.industry.trim(),
            companySize = currentState.companySize.trim(),
            location = currentState.location.trim(),
            website = normalizedWebsite,
            contactEmail = normalizedEmail,
        )

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSaving = true,
                    error = null,
                    saveSucceeded = false,
                )
            }

            when (val result = profileRepository.saveCompanyProfile(profile)) {
                is ProfileResult.Success -> {
                    _uiState.value = result.value.toUiState(saveSucceeded = true)
                }

                is ProfileResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = result.error,
                        )
                    }
                }
            }
        }
    }

    private fun updateField(
        validationError: EmployerProfileValidationError,
        update: EmployerProfileUiState.() -> EmployerProfileUiState,
    ) {
        _uiState.update { currentState ->
            currentState.update().copy(
                validationErrors = currentState.validationErrors - validationError,
                error = null,
                saveSucceeded = false,
            )
        }
    }

    private fun CompanyProfile.toUiState(
        isEditing: Boolean = false,
        saveSucceeded: Boolean = false,
    ) = EmployerProfileUiState(
        ownerUid = ownerUid,
        companyName = companyName,
        companyDescription = companyDescription,
        industry = industry,
        companySize = companySize,
        location = location,
        website = website,
        contactEmail = contactEmail,
        savedProfile = this,
        isLoading = false,
        isEditing = isEditing,
        saveSucceeded = saveSucceeded,
    )

    class Factory(
        private val profileRepository: EmployerProfileRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(EmployerProfileViewModel::class.java)) {
                "Unknown ViewModel class: ${modelClass.name}"
            }
            return EmployerProfileViewModel(profileRepository) as T
        }
    }
}
