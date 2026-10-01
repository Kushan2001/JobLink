package com.kushan.joblink.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kushan.joblink.data.model.ApplicationDraft
import com.kushan.joblink.data.model.JobApplication
import com.kushan.joblink.data.repository.ApplicationError
import com.kushan.joblink.data.repository.ApplicationRepository
import com.kushan.joblink.data.repository.ApplicationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ApplicationUiState(
    val draft: ApplicationDraft? = null,
    val coverMessage: String = "",
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val submittedApplication: JobApplication? = null,
    val error: ApplicationError? = null,
)

class ApplicationViewModel(
    private val jobId: String,
    private val applicationRepository: ApplicationRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ApplicationUiState())
    val uiState: StateFlow<ApplicationUiState> = _uiState.asStateFlow()

    init {
        loadDraft()
    }

    fun retry() {
        if (_uiState.value.isSubmitting) return
        loadDraft()
    }

    fun onCoverMessageChanged(value: String) {
        _uiState.update {
            it.copy(
                coverMessage = value,
                error = null,
            )
        }
    }

    fun submitApplication() {
        val currentState = _uiState.value
        if (currentState.isSubmitting || currentState.draft == null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            when (
                val result = applicationRepository.submitApplication(
                    jobId = jobId,
                    coverMessage = currentState.coverMessage,
                )
            ) {
                is ApplicationResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            submittedApplication = result.value,
                        )
                    }
                }

                is ApplicationResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            error = result.error,
                        )
                    }
                }
            }
        }
    }

    private fun loadDraft() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                )
            }
            when (val result = applicationRepository.getApplicationDraft(jobId)) {
                is ApplicationResult.Success -> {
                    _uiState.value = ApplicationUiState(
                        draft = result.value,
                        isLoading = false,
                    )
                }

                is ApplicationResult.Failure -> {
                    _uiState.value = ApplicationUiState(
                        isLoading = false,
                        error = result.error,
                    )
                }
            }
        }
    }

    class Factory(
        private val jobId: String,
        private val applicationRepository: ApplicationRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ApplicationViewModel::class.java)) {
                "Unknown ViewModel class: ${modelClass.name}"
            }
            return ApplicationViewModel(jobId, applicationRepository) as T
        }
    }
}
