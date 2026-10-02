package com.kushan.joblink.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kushan.joblink.data.model.EmployerApplicationsData
import com.kushan.joblink.data.repository.ApplicationError
import com.kushan.joblink.data.repository.ApplicationRepository
import com.kushan.joblink.data.repository.ApplicationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EmployerApplicationsUiState(
    val data: EmployerApplicationsData? = null,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: ApplicationError? = null,
)

class EmployerApplicationsViewModel(
    private val jobId: String,
    private val applicationRepository: ApplicationRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(EmployerApplicationsUiState())
    val uiState: StateFlow<EmployerApplicationsUiState> = _uiState.asStateFlow()

    init {
        loadApplications()
    }

    fun retry() = loadApplications()

    fun refresh() = loadApplications(isRefresh = true)

    private fun loadApplications(isRefresh: Boolean = false) {
        if (_uiState.value.isRefreshing) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = !isRefresh,
                    isRefreshing = isRefresh,
                    error = null,
                )
            }
            when (val result = applicationRepository.getEmployerApplications(jobId)) {
                is ApplicationResult.Success -> _uiState.update {
                    it.copy(
                        data = result.value,
                        isLoading = false,
                        isRefreshing = false,
                    )
                }

                is ApplicationResult.Failure -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
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
            require(modelClass.isAssignableFrom(EmployerApplicationsViewModel::class.java)) {
                "Unknown ViewModel class: ${modelClass.name}"
            }
            return EmployerApplicationsViewModel(jobId, applicationRepository) as T
        }
    }
}
