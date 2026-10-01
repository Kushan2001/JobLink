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
    val isUpdatingSavedState: Boolean = false,
    val isSaved: Boolean = false,
    val error: JobError? = null,
    val actionError: JobError? = null,
    val actionMessage: JobDetailsActionMessage? = null,
)

enum class JobDetailsActionMessage {
    JOB_SAVED,
    JOB_UNSAVED,
}

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

    fun onSavedStateToggle() {
        val currentState = _uiState.value
        if (currentState.isUpdatingSavedState) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isUpdatingSavedState = true,
                    actionError = null,
                    actionMessage = null,
                )
            }
            val result = if (currentState.isSaved) {
                jobRepository.unsaveJob(jobId)
            } else {
                jobRepository.saveJob(jobId)
            }
            when (result) {
                is JobResult.Success -> {
                    val isSaved = !currentState.isSaved
                    _uiState.update {
                        it.copy(
                            isUpdatingSavedState = false,
                            isSaved = isSaved,
                            actionMessage = if (isSaved) {
                                JobDetailsActionMessage.JOB_SAVED
                            } else {
                                JobDetailsActionMessage.JOB_UNSAVED
                            },
                        )
                    }
                }

                is JobResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isUpdatingSavedState = false,
                            actionError = result.error,
                        )
                    }
                }
            }
        }
    }

    private fun loadJob() {
        if (_uiState.value.isLoading && _uiState.value.error == null && _uiState.value.job != null) {
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = jobRepository.getJob(jobId)) {
                is JobResult.Success -> {
                    when (val savedResult = jobRepository.isJobSaved(jobId)) {
                        is JobResult.Success -> {
                            _uiState.value = JobDetailsUiState(
                                job = result.value,
                                isLoading = false,
                                isSaved = savedResult.value,
                            )
                        }

                        is JobResult.Failure -> {
                            _uiState.value = JobDetailsUiState(
                                isLoading = false,
                                error = savedResult.error,
                            )
                        }
                    }
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
