package com.kushan.joblink.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kushan.joblink.data.model.ApplicationStatus
import com.kushan.joblink.data.model.JobApplication
import com.kushan.joblink.data.repository.ApplicationError
import com.kushan.joblink.data.repository.ApplicationRepository
import com.kushan.joblink.data.repository.ApplicationResult
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ApplicantDetailsUiState(
    val application: JobApplication? = null,
    val isLoading: Boolean = true,
    val isUpdatingStatus: Boolean = false,
    val isDownloadingCv: Boolean = false,
    val cvDownloadProgress: Float = 0f,
    val downloadedCv: File? = null,
    val error: ApplicationError? = null,
    val actionError: ApplicationError? = null,
)

class ApplicantDetailsViewModel(
    private val applicationId: String,
    private val applicationRepository: ApplicationRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ApplicantDetailsUiState())
    val uiState: StateFlow<ApplicantDetailsUiState> = _uiState.asStateFlow()

    init {
        loadApplication()
    }

    fun retry() = loadApplication()

    fun updateStatus(status: ApplicationStatus) {
        if (_uiState.value.isUpdatingStatus || _uiState.value.application == null) return
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingStatus = true, actionError = null) }
            when (
                val result = applicationRepository.updateEmployerApplicationStatus(
                    applicationId,
                    status,
                )
            ) {
                is ApplicationResult.Success -> _uiState.update {
                    it.copy(application = result.value, isUpdatingStatus = false)
                }

                is ApplicationResult.Failure -> _uiState.update {
                    it.copy(isUpdatingStatus = false, actionError = result.error)
                }
            }
        }
    }

    fun downloadCv() {
        if (_uiState.value.isDownloadingCv || _uiState.value.application == null) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isDownloadingCv = true,
                    cvDownloadProgress = 0f,
                    downloadedCv = null,
                    actionError = null,
                )
            }
            when (
                val result = applicationRepository.downloadApplicantCv(applicationId) { progress ->
                    _uiState.update { it.copy(cvDownloadProgress = progress) }
                }
            ) {
                is ApplicationResult.Success -> _uiState.update {
                    it.copy(
                        isDownloadingCv = false,
                        cvDownloadProgress = 1f,
                        downloadedCv = result.value,
                    )
                }

                is ApplicationResult.Failure -> _uiState.update {
                    it.copy(isDownloadingCv = false, actionError = result.error)
                }
            }
        }
    }

    fun consumeDownloadedCv() {
        _uiState.update { it.copy(downloadedCv = null) }
    }

    fun onCvViewerUnavailable() {
        _uiState.update {
            it.copy(downloadedCv = null, actionError = ApplicationError.CV_VIEWER_UNAVAILABLE)
        }
    }

    private fun loadApplication() {
        viewModelScope.launch {
            _uiState.value = ApplicantDetailsUiState(isLoading = true)
            _uiState.value = when (
                val result = applicationRepository.getEmployerApplication(applicationId)
            ) {
                is ApplicationResult.Success -> ApplicantDetailsUiState(
                    application = result.value,
                    isLoading = false,
                )

                is ApplicationResult.Failure -> ApplicantDetailsUiState(
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
            require(modelClass.isAssignableFrom(ApplicantDetailsViewModel::class.java)) {
                "Unknown ViewModel class: ${modelClass.name}"
            }
            return ApplicantDetailsViewModel(applicationId, applicationRepository) as T
        }
    }
}
