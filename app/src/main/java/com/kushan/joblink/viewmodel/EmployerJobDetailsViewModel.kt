package com.kushan.joblink.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kushan.joblink.data.model.Job
import com.kushan.joblink.data.repository.JobError
import com.kushan.joblink.data.repository.JobRepository
import com.kushan.joblink.data.repository.JobResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EmployerJobDetailsUiState(
    val job: Job? = null,
    val isLoading: Boolean = true,
    val isUpdating: Boolean = false,
    val error: JobError? = null,
    val actionError: JobError? = null,
)

class EmployerJobDetailsViewModel(
    private val jobId: String,
    private val jobRepository: JobRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(EmployerJobDetailsUiState())
    val uiState: StateFlow<EmployerJobDetailsUiState> = _uiState.asStateFlow()

    init {
        loadJob()
    }

    fun retry() {
        loadJob()
    }

    fun setActive(active: Boolean) {
        if (_uiState.value.isUpdating || _uiState.value.job == null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isUpdating = true, actionError = null) }
            when (val result = jobRepository.setEmployerJobActive(jobId, active)) {
                is JobResult.Success -> {
                    _uiState.update {
                        it.copy(
                            job = result.value,
                            isUpdating = false,
                        )
                    }
                }

                is JobResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isUpdating = false,
                            actionError = result.error,
                        )
                    }
                }
            }
        }
    }

    private fun loadJob() {
        viewModelScope.launch {
            _uiState.value = EmployerJobDetailsUiState(isLoading = true)
            _uiState.value = when (val result = jobRepository.getEmployerJob(jobId)) {
                is JobResult.Success -> EmployerJobDetailsUiState(
                    job = result.value,
                    isLoading = false,
                )

                is JobResult.Failure -> EmployerJobDetailsUiState(
                    isLoading = false,
                    error = result.error,
                )
            }
        }
    }

    class Factory(
        private val jobId: String,
        private val jobRepository: JobRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(EmployerJobDetailsViewModel::class.java)) {
                "Unknown ViewModel class: ${modelClass.name}"
            }
            return EmployerJobDetailsViewModel(jobId, jobRepository) as T
        }
    }
}
