package com.kushan.joblink.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kushan.joblink.data.model.Job
import com.kushan.joblink.data.model.JobSeekerProfile
import com.kushan.joblink.data.model.JobType
import com.kushan.joblink.data.model.WorkMode
import com.kushan.joblink.data.repository.JobError
import com.kushan.joblink.data.repository.JobRepository
import com.kushan.joblink.data.repository.JobResult
import com.kushan.joblink.data.repository.JobSeekerProfileRepository
import com.kushan.joblink.data.repository.ProfileError
import com.kushan.joblink.data.repository.ProfileResult
import com.kushan.joblink.recommendation.JobRecommendation
import com.kushan.joblink.recommendation.JobRecommendationScorer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val allJobs: List<Job> = emptyList(),
    val jobs: List<Job> = emptyList(),
    val savedJobIds: Set<String> = emptySet(),
    val profile: JobSeekerProfile? = null,
    val recommendations: Map<String, JobRecommendation> = emptyMap(),
    val searchQuery: String = "",
    val categoryFilter: String = "",
    val locationFilter: String = "",
    val experienceLevelFilter: String = "",
    val jobTypeFilter: JobType? = null,
    val workModeFilter: WorkMode? = null,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: JobError? = null,
    val savedJobsError: JobError? = null,
    val profileError: ProfileError? = null,
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
    private val jobSeekerProfileRepository: JobSeekerProfileRepository,
    private val logError: (String, Throwable) -> Unit = { message, throwable ->
        Log.e(TAG, message, throwable)
    },
    private val logWarning: (String) -> Unit = { message -> Log.w(TAG, message) },
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
                    savedJobsError = null,
                    profileError = null,
                )
            }
            val jobsResult = try {
                jobRepository.getActiveJobs()
            } catch (exception: Exception) {
                logError("Unexpected exception while loading jobs", exception)
                JobResult.Failure(JobError.UNKNOWN)
            }
            when (jobsResult) {
                is JobResult.Success -> {
                    _uiState.update {
                        it.copy(
                            allJobs = jobsResult.value,
                            recommendations = recommendationsFor(it.profile, jobsResult.value),
                            isLoading = false,
                            isRefreshing = false,
                        ).withFilteredJobs()
                    }

                    loadSavedJobsWithoutBlockingFeed()
                    loadProfileWithoutBlockingFeed(jobsResult.value)
                }

                is JobResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = jobsResult.error,
                        )
                    }
                }
            }
        }
    }

    private suspend fun loadSavedJobsWithoutBlockingFeed() {
        val savedJobsResult = try {
            jobRepository.getSavedJobs()
        } catch (exception: Exception) {
            logError("Saved jobs failed without blocking the job feed", exception)
            JobResult.Failure(JobError.UNKNOWN)
        }
        _uiState.update { currentState ->
            when (savedJobsResult) {
                is JobResult.Success -> currentState.copy(
                    savedJobIds = savedJobsResult.value.mapTo(mutableSetOf(), Job::id),
                    savedJobsError = null,
                )

                is JobResult.Failure -> currentState.copy(savedJobsError = savedJobsResult.error)
            }
        }
    }

    private suspend fun loadProfileWithoutBlockingFeed(jobs: List<Job>) {
        val profileResult = try {
            jobSeekerProfileRepository.getProfile()
        } catch (exception: Exception) {
            logError("Profile loading failed without blocking the job feed", exception)
            ProfileResult.Failure(ProfileError.UNKNOWN)
        }
        _uiState.update { currentState ->
            when (profileResult) {
                is ProfileResult.Success -> currentState.copy(
                    profile = profileResult.value,
                    recommendations = recommendationsFor(profileResult.value, jobs),
                    profileError = null,
                ).withFilteredJobs()

                is ProfileResult.Failure -> {
                    logWarning("Recommendations unavailable: ${profileResult.error}")
                    currentState.copy(profileError = profileResult.error)
                }
            }
        }
    }

    private fun recommendationsFor(
        profile: JobSeekerProfile?,
        jobs: List<Job>,
    ): Map<String, JobRecommendation> {
        if (profile == null) return emptyMap()
        return jobs.mapNotNull { job ->
            runCatching { JobRecommendationScorer.score(profile, job) }
                .onFailure { exception ->
                    logError(
                        "Recommendation scoring failed for job ${job.id}; showing it unscored",
                        exception,
                    )
                }
                .getOrNull()
                ?.let { recommendation -> job.id to recommendation }
        }.toMap()
    }

    private fun updateFilters(update: HomeUiState.() -> HomeUiState) {
        _uiState.update { currentState ->
            currentState.update().withFilteredJobs()
        }
    }

    class Factory(
        private val jobRepository: JobRepository,
        private val jobSeekerProfileRepository: JobSeekerProfileRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                "Unknown ViewModel class: ${modelClass.name}"
            }
            return HomeViewModel(jobRepository, jobSeekerProfileRepository) as T
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
    return copy(
        jobs = filteredJobs.sortedByDescending { job ->
            recommendations[job.id]?.score ?: 0
        },
    )
}

private fun String.normalizedTerms(): List<String> =
    normalizedValue().split(" ").filter(String::isNotBlank)

private fun String.normalizedValue(): String =
    trim().lowercase().replace(Regex("\\s+"), " ")

private const val TAG = "JobLinkHome"
