package com.kushan.joblink.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kushan.joblink.data.model.JobApplication
import com.kushan.joblink.data.repository.ApplicationError
import com.kushan.joblink.data.repository.ApplicationRepository
import com.kushan.joblink.data.repository.ApplicationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ApplicationDetailsUiState(
    val application: JobApplication? = null,
    val isLoading: Boolean = true,
    val error: ApplicationError? = null,
)

class ApplicationDetailsViewModel(
    private val applicationId: String,
    private val applicationRepository: ApplicationRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ApplicationDetailsUiState())
    val uiState: StateFlow<ApplicationDetailsUiState> = _uiState.asStateFlow()

    init {
        loadApplication()
    }

    fun retry() {
        loadApplication()
    }

    private fun loadApplication() {
        viewModelScope.launch {
            _uiState.value = ApplicationDetailsUiState(isLoading = true)
            _uiState.value = when (
                val result = applicationRepository.getMyApplication(applicationId)
            ) {
                is ApplicationResult.Success -> ApplicationDetailsUiState(
                    application = result.value,
                    isLoading = false,
                )

                is ApplicationResult.Failure -> ApplicationDetailsUiState(
                    isLoading = false,
                    error = result.error,
                )
            }
        }
    }

    class Factory(
        private val applicationId: String,
        private val applicationRepository: ApplicationRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ApplicationDetailsViewModel::class.java)) {
                "Unknown ViewModel class: ${modelClass.name}"
            }
            return ApplicationDetailsViewModel(applicationId, applicationRepository) as T
        }
    }
}
