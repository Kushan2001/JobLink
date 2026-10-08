package com.kushan.joblink.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kushan.joblink.data.model.CompanyProfile
import com.kushan.joblink.data.model.Job
import com.kushan.joblink.data.model.JobApplication
import com.kushan.joblink.data.repository.ApplicationError
import com.kushan.joblink.data.repository.ApplicationRepository
import com.kushan.joblink.data.repository.ApplicationResult
import com.kushan.joblink.data.repository.EmployerProfileRepository
import com.kushan.joblink.data.repository.JobError
import com.kushan.joblink.data.repository.JobRepository
import com.kushan.joblink.data.repository.JobResult
import com.kushan.joblink.data.repository.ProfileError
import com.kushan.joblink.data.repository.ProfileResult
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EmployerHomeUiState(
    val companyProfile: CompanyProfile? = null,
    val jobs: List<Job> = emptyList(),
    val recentApplicants: List<JobApplication> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val profileError: ProfileError? = null,
    val jobError: JobError? = null,
    val applicationError: ApplicationError? = null,
) {
    val activeJobsCount: Int
        get() = jobs.count(Job::active)

    val applicationsCount: Long
        get() = jobs.sumOf(Job::applicantCount)

    val hasDashboardError: Boolean
        get() = profileError != null || jobError != null

    val hasRecentApplicantsError: Boolean
        get() = applicationError != null

    val hasError: Boolean
        get() = hasDashboardError || hasRecentApplicantsError
}

class EmployerHomeViewModel(
    private val employerProfileRepository: EmployerProfileRepository,
    private val jobRepository: JobRepository,
    private val applicationRepository: ApplicationRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(EmployerHomeUiState())
    val uiState: StateFlow<EmployerHomeUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    fun retry() = loadDashboard()

    fun refresh() = loadDashboard(isRefresh = true)

    private fun loadDashboard(isRefresh: Boolean = false) {
        if (_uiState.value.isRefreshing) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = !isRefresh,
                isRefreshing = isRefresh,
                profileError = null,
                jobError = null,
                applicationError = null,
            )
            val results = coroutineScope {
                val profile = async { employerProfileRepository.getCompanyProfile() }
                val jobs = async { jobRepository.getEmployerJobs() }
                val applicants = async {
                    applicationRepository.getRecentEmployerApplications(RECENT_APPLICANT_LIMIT)
                }
                Triple(profile.await(), jobs.await(), applicants.await())
            }
            val profileResult = results.first
            val jobsResult = results.second
            val applicantResult = results.third
            _uiState.value = EmployerHomeUiState(
                companyProfile = (profileResult as? ProfileResult.Success)?.value
                    ?: _uiState.value.companyProfile,
                jobs = (jobsResult as? JobResult.Success)?.value ?: _uiState.value.jobs,
                recentApplicants = (applicantResult as? ApplicationResult.Success)?.value
                    ?: _uiState.value.recentApplicants,
                isLoading = false,
                isRefreshing = false,
                profileError = (profileResult as? ProfileResult.Failure)?.error,
                jobError = (jobsResult as? JobResult.Failure)?.error,
                applicationError = (applicantResult as? ApplicationResult.Failure)?.error,
            )
        }
    }

    class Factory(
        private val employerProfileRepository: EmployerProfileRepository,
        private val jobRepository: JobRepository,
        private val applicationRepository: ApplicationRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(EmployerHomeViewModel::class.java)) {
                "Unknown ViewModel class: ${modelClass.name}"
            }
            return EmployerHomeViewModel(
                employerProfileRepository,
                jobRepository,
                applicationRepository,
            ) as T
        }
    }

    private companion object {
        const val RECENT_APPLICANT_LIMIT = 5L
    }
}
