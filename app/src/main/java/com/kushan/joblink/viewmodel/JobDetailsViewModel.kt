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

data class JobDetailsUiState(
    val job: Job? = null,
    val isLoading: Boolean = true,
    val error: JobError? = null,
)

class JobDetailsViewModel(
    private val jobId: String,
    private val jobRepository: JobRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(JobDetailsUiState())
    val uiState: StateFlow<JobDetailsUiState> = _uiState.asStateFlow()

    init {
        loadJob()
    }

    fun retry() {
        loadJob()
    }

    private fun loadJob() {
        if (_uiState.value.isLoading && _uiState.value.error == null && _uiState.value.job != null) {
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = jobRepository.getJob(jobId)) {
                is JobResult.Success -> {
                    _uiState.value = JobDetailsUiState(job = result.value, isLoading = false)
                }

                is JobResult.Failure -> {
                    _uiState.value = JobDetailsUiState(
                        isLoading = false,
                        error = result.error,
                    )
                }
            }
        }
    }

    class Factory(
        private val jobId: String,
        private val jobRepository: JobRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(JobDetailsViewModel::class.java)) {
                "Unknown ViewModel class: ${modelClass.name}"
            }
            return JobDetailsViewModel(jobId, jobRepository) as T
        }
    }
}
