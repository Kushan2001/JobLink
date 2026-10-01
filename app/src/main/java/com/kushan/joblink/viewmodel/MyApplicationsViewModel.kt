package com.kushan.joblink.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kushan.joblink.data.model.ApplicationStatus
import com.kushan.joblink.data.model.JobApplication
import com.kushan.joblink.data.repository.ApplicationError
import com.kushan.joblink.data.repository.ApplicationRepository
import com.kushan.joblink.data.repository.ApplicationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MyApplicationsUiState(
    val applications: List<JobApplication> = emptyList(),
    val selectedStatus: ApplicationStatus? = null,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: ApplicationError? = null,
) {
    val filteredApplications: List<JobApplication>
        get() = selectedStatus?.let { status ->
            applications.filter { it.status == status }
        } ?: applications
}

class MyApplicationsViewModel(
    private val applicationRepository: ApplicationRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MyApplicationsUiState())
    val uiState: StateFlow<MyApplicationsUiState> = _uiState.asStateFlow()

    init {
        loadApplications()
    }

    fun onStatusSelected(status: ApplicationStatus?) {
        _uiState.update { it.copy(selectedStatus = status) }
    }

    fun retry() {
        loadApplications()
    }

    fun refresh() {
        loadApplications(isRefresh = true)
    }

    private fun loadApplications(isRefresh: Boolean = false) {
        val currentState = _uiState.value
        if ((currentState.isLoading && isRefresh) || currentState.isRefreshing) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = !isRefresh,
                    isRefreshing = isRefresh,
                    error = null,
                )
            }
            when (val result = applicationRepository.getMyApplications()) {
                is ApplicationResult.Success -> {
                    _uiState.update {
                        it.copy(
                            applications = result.value,
                            isLoading = false,
                            isRefreshing = false,
                            error = null,
                        )
                    }
                }

                is ApplicationResult.Failure -> {
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
        private val applicationRepository: ApplicationRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(MyApplicationsViewModel::class.java)) {
                "Unknown ViewModel class: ${modelClass.name}"
            }
            return MyApplicationsViewModel(applicationRepository) as T
        }
    }
}
