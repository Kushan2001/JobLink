package com.kushan.joblink.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kushan.joblink.data.model.Job
import com.kushan.joblink.data.model.JobType
import com.kushan.joblink.data.model.WorkMode
import com.kushan.joblink.data.repository.JobError
import com.kushan.joblink.data.repository.JobRepository
import com.kushan.joblink.data.repository.JobResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val allJobs: List<Job> = emptyList(),
    val jobs: List<Job> = emptyList(),
    val savedJobIds: Set<String> = emptySet(),
    val searchQuery: String = "",
    val categoryFilter: String = "",
    val locationFilter: String = "",
    val experienceLevelFilter: String = "",
    val jobTypeFilter: JobType? = null,
    val workModeFilter: WorkMode? = null,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: JobError? = null,
) {
    val categories: List<String>
        get() = allJobs
            .map(Job::category)
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinctBy { it.lowercase() }
            .take(8)

    val hasActiveFilters: Boolean
        get() = searchQuery.isNotBlank() ||
            categoryFilter.isNotBlank() ||
            locationFilter.isNotBlank() ||
            experienceLevelFilter.isNotBlank() ||
            jobTypeFilter != null ||
            workModeFilter != null

    val activeFilterCount: Int
        get() = listOf(
            categoryFilter.isNotBlank(),
            locationFilter.isNotBlank(),
            experienceLevelFilter.isNotBlank(),
            jobTypeFilter != null,
            workModeFilter != null,
        ).count { it }
}

class HomeViewModel(
    private val jobRepository: JobRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadJobs()
    }

    fun retry() {
        loadJobs()
    }

    fun refresh() {
        loadJobs(isRefresh = true)
    }

    fun onSearchQueryChanged(query: String) {
        updateFilters { copy(searchQuery = query) }
    }

    fun onCategoryFilterChanged(category: String) {
        updateFilters { copy(categoryFilter = category) }
    }

    fun onCategorySelected(category: String) {
        updateFilters {
            copy(categoryFilter = category.takeUnless { it == categoryFilter }.orEmpty())
        }
    }

    fun onLocationFilterChanged(location: String) {
        updateFilters { copy(locationFilter = location) }
    }

    fun onExperienceLevelFilterChanged(experienceLevel: String) {
        updateFilters { copy(experienceLevelFilter = experienceLevel) }
    }

    fun onJobTypeFilterChanged(jobType: JobType) {
        updateFilters {
            copy(jobTypeFilter = jobType.takeUnless { it == jobTypeFilter })
        }
    }

    fun onWorkModeFilterChanged(workMode: WorkMode) {
        updateFilters {
            copy(workModeFilter = workMode.takeUnless { it == workModeFilter })
        }
    }

    fun clearFilters() {
        updateFilters {
            copy(
                searchQuery = "",
                categoryFilter = "",
                locationFilter = "",
                experienceLevelFilter = "",
                jobTypeFilter = null,
                workModeFilter = null,
            )
        }
    }

    private fun loadJobs(isRefresh: Boolean = false) {
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
            val jobsResult = jobRepository.getActiveJobs()
            val savedJobsResult = jobRepository.getSavedJobs()
            when {
                jobsResult is JobResult.Success && savedJobsResult is JobResult.Success -> {
                    _uiState.update {
                        it.copy(
                            allJobs = jobsResult.value,
                            savedJobIds = savedJobsResult.value.mapTo(mutableSetOf(), Job::id),
                            isLoading = false,
                            isRefreshing = false,
                        ).withFilteredJobs()
                    }
                }

                else -> {
                    val error = (jobsResult as? JobResult.Failure)?.error
                        ?: (savedJobsResult as JobResult.Failure).error
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = error,
                        )
                    }
                }
            }
        }
    }

    private fun updateFilters(update: HomeUiState.() -> HomeUiState) {
        _uiState.update { currentState ->
            currentState.update().withFilteredJobs()
        }
    }

    class Factory(
        private val jobRepository: JobRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                "Unknown ViewModel class: ${modelClass.name}"
            }
            return HomeViewModel(jobRepository) as T
        }
    }
}

private fun HomeUiState.withFilteredJobs(): HomeUiState {
    val searchTerms = searchQuery.normalizedTerms()
    val normalizedCategory = categoryFilter.normalizedValue()
    val normalizedLocation = locationFilter.normalizedValue()
    val normalizedExperience = experienceLevelFilter.normalizedValue()

    val filteredJobs = allJobs.filter { job ->
        val searchableText = "${job.title} ${job.companyName}".normalizedValue()
        searchTerms.all(searchableText::contains) &&
            job.category.normalizedValue().contains(normalizedCategory) &&
            job.location.normalizedValue().contains(normalizedLocation) &&
            job.experienceLevel.normalizedValue().contains(normalizedExperience) &&
            (jobTypeFilter == null || job.jobType == jobTypeFilter) &&
            (workModeFilter == null || job.workMode == workModeFilter)
    }
    return copy(jobs = filteredJobs)
}

private fun String.normalizedTerms(): List<String> =
    normalizedValue().split(" ").filter(String::isNotBlank)

private fun String.normalizedValue(): String =
    trim().lowercase().replace(Regex("\\s+"), " ")
