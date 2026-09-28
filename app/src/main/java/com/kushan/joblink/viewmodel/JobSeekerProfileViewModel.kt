package com.kushan.joblink.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kushan.joblink.data.model.JobSeekerProfile
import com.kushan.joblink.data.repository.JobSeekerProfileRepository
import com.kushan.joblink.data.repository.ProfileError
import com.kushan.joblink.data.repository.ProfileResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class JobSeekerProfileUiState(
    val uid: String = "",
    val fullName: String = "",
    val professionalHeadline: String = "",
    val location: String = "",
    val phone: String = "",
    val bio: String = "",
    val education: String = "",
    val experienceSummary: String = "",
    val skillsInput: String = "",
    val preferredJobTypesInput: String = "",
    val savedProfile: JobSeekerProfile? = null,
    val validationErrors: Set<ProfileValidationError> = emptySet(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isEditing: Boolean = false,
    val saveSucceeded: Boolean = false,
    val error: ProfileError? = null,
)

enum class ProfileValidationError {
    FULL_NAME_REQUIRED,
    HEADLINE_REQUIRED,
    LOCATION_REQUIRED,
    PHONE_INVALID,
    SKILLS_REQUIRED,
    JOB_TYPES_REQUIRED,
}

class JobSeekerProfileViewModel(
    private val profileRepository: JobSeekerProfileRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(JobSeekerProfileUiState())
    val uiState: StateFlow<JobSeekerProfileUiState> = _uiState.asStateFlow()

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

            when (val result = profileRepository.getProfile()) {
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

    fun onFullNameChanged(value: String) = updateField(ProfileValidationError.FULL_NAME_REQUIRED) {
        copy(fullName = value)
    }

    fun onProfessionalHeadlineChanged(value: String) =
        updateField(ProfileValidationError.HEADLINE_REQUIRED) {
            copy(professionalHeadline = value)
        }

    fun onLocationChanged(value: String) = updateField(ProfileValidationError.LOCATION_REQUIRED) {
        copy(location = value)
    }

    fun onPhoneChanged(value: String) = updateField(ProfileValidationError.PHONE_INVALID) {
        copy(phone = value)
    }

    fun onBioChanged(value: String) = updateField { copy(bio = value) }

    fun onEducationChanged(value: String) = updateField { copy(education = value) }

    fun onExperienceSummaryChanged(value: String) = updateField {
        copy(experienceSummary = value)
    }

    fun onSkillsChanged(value: String) = updateField(ProfileValidationError.SKILLS_REQUIRED) {
        copy(skillsInput = value)
    }

    fun onPreferredJobTypesChanged(value: String) =
        updateField(ProfileValidationError.JOB_TYPES_REQUIRED) {
            copy(preferredJobTypesInput = value)
        }

    fun saveProfile() {
        val currentState = _uiState.value
        if (currentState.isSaving) return

        val skills = currentState.skillsInput.toEntryList()
        val preferredJobTypes = currentState.preferredJobTypesInput.toEntryList()
        val errors = buildSet {
            if (currentState.fullName.isBlank()) add(ProfileValidationError.FULL_NAME_REQUIRED)
            if (currentState.professionalHeadline.isBlank()) {
                add(ProfileValidationError.HEADLINE_REQUIRED)
            }
            if (currentState.location.isBlank()) add(ProfileValidationError.LOCATION_REQUIRED)
            if (currentState.phone.isNotBlank() && !currentState.phone.isValidPhone()) {
                add(ProfileValidationError.PHONE_INVALID)
            }
            if (skills.isEmpty()) add(ProfileValidationError.SKILLS_REQUIRED)
            if (preferredJobTypes.isEmpty()) add(ProfileValidationError.JOB_TYPES_REQUIRED)
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

        val profile = JobSeekerProfile(
            uid = currentState.uid,
            fullName = currentState.fullName.trim(),
            professionalHeadline = currentState.professionalHeadline.trim(),
            location = currentState.location.trim(),
            phone = currentState.phone.trim(),
            bio = currentState.bio.trim(),
            education = currentState.education.trim(),
            experienceSummary = currentState.experienceSummary.trim(),
            skills = skills,
            preferredJobTypes = preferredJobTypes,
        )

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSaving = true,
                    error = null,
                    saveSucceeded = false,
                )
            }

            when (val result = profileRepository.saveProfile(profile)) {
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
        validationError: ProfileValidationError? = null,
        update: JobSeekerProfileUiState.() -> JobSeekerProfileUiState,
    ) {
        _uiState.update { currentState ->
            currentState.update().copy(
                validationErrors = validationError?.let {
                    currentState.validationErrors - it
                } ?: currentState.validationErrors,
                error = null,
                saveSucceeded = false,
            )
        }
    }

    private fun JobSeekerProfile.toUiState(
        isEditing: Boolean = false,
        saveSucceeded: Boolean = false,
    ) = JobSeekerProfileUiState(
        uid = uid,
        fullName = fullName,
        professionalHeadline = professionalHeadline,
        location = location,
        phone = phone,
        bio = bio,
        education = education,
        experienceSummary = experienceSummary,
        skillsInput = skills.joinToString(", "),
        preferredJobTypesInput = preferredJobTypes.joinToString(", "),
        savedProfile = this,
        isLoading = false,
        isEditing = isEditing,
        saveSucceeded = saveSucceeded,
    )

    private fun String.toEntryList(): List<String> = split(',')
        .map(String::trim)
        .filter(String::isNotEmpty)
        .distinctBy { it.lowercase() }

    private fun String.isValidPhone(): Boolean {
        val allowedCharacters = Regex("^\\+?[0-9 ()-]+$")
        val digitCount = count(Char::isDigit)
        return allowedCharacters.matches(this) && digitCount in 7..15
    }

    class Factory(
        private val profileRepository: JobSeekerProfileRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(JobSeekerProfileViewModel::class.java)) {
                "Unknown ViewModel class: ${modelClass.name}"
            }
            return JobSeekerProfileViewModel(profileRepository) as T
        }
    }
}
