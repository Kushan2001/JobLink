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

data class SavedJobsUiState(
    val jobs: List<Job> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: JobError? = null,
)

class SavedJobsViewModel(
    private val jobRepository: JobRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SavedJobsUiState())
    val uiState: StateFlow<SavedJobsUiState> = _uiState.asStateFlow()

    init {
        loadSavedJobs()
    }

    fun retry() {
        loadSavedJobs()
    }

    fun refresh() {
        loadSavedJobs(isRefresh = true)
    }

    private fun loadSavedJobs(isRefresh: Boolean = false) {
        val currentState = _uiState.value
        if (
            currentState.isRefreshing ||
            (currentState.isLoading && isRefresh) ||
            (!currentState.isLoading && isRefresh.not() && currentState.error == null)
        ) {
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = !isRefresh,
                    isRefreshing = isRefresh,
                    error = null,
                )
            }
            when (val result = jobRepository.getSavedJobs()) {
                is JobResult.Success -> {
                    _uiState.value = SavedJobsUiState(
                        jobs = result.value,
                        isLoading = false,
                    )
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
            require(modelClass.isAssignableFrom(SavedJobsViewModel::class.java)) {
                "Unknown ViewModel class: ${modelClass.name}"
            }
            return SavedJobsViewModel(jobRepository) as T
        }
    }
}
