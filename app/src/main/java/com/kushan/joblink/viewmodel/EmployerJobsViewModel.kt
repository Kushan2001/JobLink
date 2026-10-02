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

data class EmployerJobsUiState(
    val jobs: List<Job> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val updatingJobId: String? = null,
    val error: JobError? = null,
    val actionError: JobError? = null,
)

class EmployerJobsViewModel(
    private val jobRepository: JobRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(EmployerJobsUiState())
    val uiState: StateFlow<EmployerJobsUiState> = _uiState.asStateFlow()

    init {
        loadJobs()
    }

    fun retry() {
        loadJobs()
    }

    fun refresh() {
        loadJobs(isRefresh = true)
    }

    fun setJobActive(jobId: String, active: Boolean) {
        if (_uiState.value.updatingJobId != null) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    updatingJobId = jobId,
                    actionError = null,
                )
            }
            when (val result = jobRepository.setEmployerJobActive(jobId, active)) {
                is JobResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            jobs = state.jobs.map { job ->
                                if (job.id == jobId) result.value else job
                            },
                            updatingJobId = null,
                        )
                    }
                }

                is JobResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            updatingJobId = null,
                            actionError = result.error,
                        )
                    }
                }
            }
        }
    }

    private fun loadJobs(isRefresh: Boolean = false) {
        val currentState = _uiState.value
        if ((currentState.isLoading && isRefresh) || currentState.isRefreshing) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = !isRefresh,
                    isRefreshing = isRefresh,
                    error = null,
                    actionError = null,
                )
            }
            when (val result = jobRepository.getEmployerJobs()) {
                is JobResult.Success -> {
                    _uiState.update {
                        it.copy(
                            jobs = result.value,
                            isLoading = false,
                            isRefreshing = false,
                        )
                    }
                }

                is JobResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = result.error,
                        )
                    }
                }
            }
        }
    }

    class Factory(
        private val jobRepository: JobRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(EmployerJobsViewModel::class.java)) {
                "Unknown ViewModel class: ${modelClass.name}"
            }
            return EmployerJobsViewModel(jobRepository) as T
        }
    }
}
