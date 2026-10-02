package com.kushan.joblink.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.kushan.joblink.data.model.Job
import com.kushan.joblink.data.model.JobType
import com.kushan.joblink.data.model.WorkMode
import com.kushan.joblink.data.repository.JobError
import com.kushan.joblink.data.repository.JobRepository
import com.kushan.joblink.data.repository.JobResult
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PostJobUiState(
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val location: String = "",
    val workMode: WorkMode = WorkMode.ONSITE,
    val jobType: JobType = JobType.FULL_TIME,
    val salaryMin: String = "",
    val salaryMax: String = "",
    val currency: String = "",
    val experienceLevel: String = "",
    val requiredSkills: String = "",
    val requirements: String = "",
    val benefits: String = "",
    val applicationDeadline: String = "",
    val validationErrors: Set<PostJobValidationError> = emptySet(),
    val isLoading: Boolean = false,
    val isEditMode: Boolean = false,
    val hasLoadedJob: Boolean = false,
    val isPosting: Boolean = false,
    val postedJob: Job? = null,
    val error: JobError? = null,
)

enum class PostJobValidationError {
    TITLE_REQUIRED,
    DESCRIPTION_REQUIRED,
    CATEGORY_REQUIRED,
    LOCATION_REQUIRED,
    EXPERIENCE_LEVEL_REQUIRED,
    SKILLS_REQUIRED,
    REQUIREMENTS_REQUIRED,
    DEADLINE_REQUIRED,
    DEADLINE_INVALID,
    DEADLINE_NOT_FUTURE,
    SALARY_MIN_INVALID,
    SALARY_MAX_INVALID,
    SALARY_RANGE_INVALID,
    CURRENCY_REQUIRED,
}

class PostJobViewModel(
    private val jobRepository: JobRepository,
    private val jobId: String? = null,
    private val currentDateProvider: () -> Date = { Date() },
) : ViewModel() {
    private val _uiState = MutableStateFlow(PostJobUiState())
    val uiState: StateFlow<PostJobUiState> = _uiState.asStateFlow()

    init {
        if (jobId != null) loadJobForEditing()
    }

    fun onTitleChanged(value: String) = updateField(PostJobValidationError.TITLE_REQUIRED) {
        copy(title = value)
    }

    fun onDescriptionChanged(value: String) =
        updateField(PostJobValidationError.DESCRIPTION_REQUIRED) { copy(description = value) }

    fun onCategoryChanged(value: String) = updateField(PostJobValidationError.CATEGORY_REQUIRED) {
        copy(category = value)
    }

    fun onLocationChanged(value: String) = updateField(PostJobValidationError.LOCATION_REQUIRED) {
        copy(location = value)
    }

    fun onWorkModeChanged(value: WorkMode) = update { copy(workMode = value) }

    fun onJobTypeChanged(value: JobType) = update { copy(jobType = value) }

    fun onSalaryMinChanged(value: String) {
        updateField(
            PostJobValidationError.SALARY_MIN_INVALID,
            PostJobValidationError.SALARY_RANGE_INVALID,
        ) { copy(salaryMin = value) }
    }

    fun onSalaryMaxChanged(value: String) {
        updateField(
            PostJobValidationError.SALARY_MAX_INVALID,
            PostJobValidationError.SALARY_RANGE_INVALID,
        ) { copy(salaryMax = value) }
    }

    fun onCurrencyChanged(value: String) = updateField(PostJobValidationError.CURRENCY_REQUIRED) {
        copy(currency = value)
    }

    fun onExperienceLevelChanged(value: String) =
        updateField(PostJobValidationError.EXPERIENCE_LEVEL_REQUIRED) {
            copy(experienceLevel = value)
        }

    fun onRequiredSkillsChanged(value: String) =
        updateField(PostJobValidationError.SKILLS_REQUIRED) { copy(requiredSkills = value) }

    fun onRequirementsChanged(value: String) =
        updateField(PostJobValidationError.REQUIREMENTS_REQUIRED) { copy(requirements = value) }

    fun onBenefitsChanged(value: String) = update { copy(benefits = value) }

    fun onApplicationDeadlineChanged(value: String) {
        updateField(
            PostJobValidationError.DEADLINE_REQUIRED,
            PostJobValidationError.DEADLINE_INVALID,
            PostJobValidationError.DEADLINE_NOT_FUTURE,
        ) { copy(applicationDeadline = value) }
    }

    fun postJob() {
        val currentState = _uiState.value
        if (currentState.isPosting) return

        val salaryMin = currentState.salaryMin.trim().toOptionalLong()
        val salaryMax = currentState.salaryMax.trim().toOptionalLong()
        val deadline = currentState.applicationDeadline.trim().toDateOrNull()
        val errors = buildSet {
            if (currentState.title.isBlank()) add(PostJobValidationError.TITLE_REQUIRED)
            if (currentState.description.isBlank()) add(PostJobValidationError.DESCRIPTION_REQUIRED)
            if (currentState.category.isBlank()) add(PostJobValidationError.CATEGORY_REQUIRED)
            if (currentState.location.isBlank()) add(PostJobValidationError.LOCATION_REQUIRED)
            if (currentState.experienceLevel.isBlank()) {
                add(PostJobValidationError.EXPERIENCE_LEVEL_REQUIRED)
            }
            if (currentState.requiredSkills.toEntries().isEmpty()) {
                add(PostJobValidationError.SKILLS_REQUIRED)
            }
            if (currentState.requirements.toEntries().isEmpty()) {
                add(PostJobValidationError.REQUIREMENTS_REQUIRED)
            }
            when {
                currentState.applicationDeadline.isBlank() -> {
                    add(PostJobValidationError.DEADLINE_REQUIRED)
                }

                deadline == null -> add(PostJobValidationError.DEADLINE_INVALID)
                !deadline.after(currentDateProvider().startOfDay()) -> {
                    add(PostJobValidationError.DEADLINE_NOT_FUTURE)
                }
            }
            if (currentState.salaryMin.isNotBlank() && salaryMin == null) {
                add(PostJobValidationError.SALARY_MIN_INVALID)
            }
            if (currentState.salaryMax.isNotBlank() && salaryMax == null) {
                add(PostJobValidationError.SALARY_MAX_INVALID)
            }
            if (salaryMin != null && salaryMax != null && salaryMax < salaryMin) {
                add(PostJobValidationError.SALARY_RANGE_INVALID)
            }
            if ((salaryMin != null || salaryMax != null) && currentState.currency.isBlank()) {
                add(PostJobValidationError.CURRENCY_REQUIRED)
            }
        }

        if (errors.isNotEmpty() || deadline == null) {
            _uiState.update { it.copy(validationErrors = errors, error = null) }
            return
        }

        val job = Job(
            title = currentState.title.trim(),
            description = currentState.description.trim(),
            category = currentState.category.trim(),
            location = currentState.location.trim(),
            workMode = currentState.workMode,
            jobType = currentState.jobType,
            salaryMin = salaryMin,
            salaryMax = salaryMax,
            currency = currentState.currency.trim().uppercase(),
            experienceLevel = currentState.experienceLevel.trim(),
            requiredSkills = currentState.requiredSkills.toEntries(),
            requirements = currentState.requirements.toEntries(),
            benefits = currentState.benefits.toEntries(),
            applicationDeadline = Timestamp(deadline),
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isPosting = true, error = null) }
            val result = if (jobId == null) {
                jobRepository.postJob(job)
            } else {
                jobRepository.updateEmployerJob(jobId, job)
            }
            when (result) {
                is JobResult.Success -> {
                    _uiState.update {
                        it.copy(isPosting = false, postedJob = result.value, error = null)
                    }
                }

                is JobResult.Failure -> {
                    _uiState.update { it.copy(isPosting = false, error = result.error) }
                }
            }
        }
    }

    fun startAnotherJob() {
        _uiState.value = PostJobUiState()
    }

    fun retryLoading() {
        if (jobId != null) loadJobForEditing()
    }

    private fun loadJobForEditing() {
        val currentJobId = jobId ?: return
        viewModelScope.launch {
            _uiState.value = PostJobUiState(
                isLoading = true,
                isEditMode = true,
            )
            _uiState.value = when (val result = jobRepository.getEmployerJob(currentJobId)) {
                is JobResult.Success -> result.value.toEditUiState()
                is JobResult.Failure -> PostJobUiState(
                    isLoading = false,
                    isEditMode = true,
                    error = result.error,
                )
            }
        }
    }

    private fun Job.toEditUiState() = PostJobUiState(
        title = title,
        description = description,
        category = category,
        location = location,
        workMode = workMode,
        jobType = jobType,
        salaryMin = salaryMin?.toString().orEmpty(),
        salaryMax = salaryMax?.toString().orEmpty(),
        currency = currency,
        experienceLevel = experienceLevel,
        requiredSkills = requiredSkills.joinToString(", "),
        requirements = requirements.joinToString(", "),
        benefits = benefits.joinToString(", "),
        applicationDeadline = applicationDeadline?.toDate()?.let {
            deadlineDateFormat().format(it)
        }.orEmpty(),
        isEditMode = true,
        hasLoadedJob = true,
    )

    private fun updateField(
        vararg validationErrors: PostJobValidationError,
        update: PostJobUiState.() -> PostJobUiState,
    ) {
        val errorsToClear = validationErrors.toSet()
        _uiState.update { currentState ->
            currentState.update().copy(
                validationErrors = currentState.validationErrors - errorsToClear,
                error = null,
            )
        }
    }

    private fun update(update: PostJobUiState.() -> PostJobUiState) {
        _uiState.update { it.update().copy(error = null) }
    }

    private fun String.toOptionalLong(): Long? = when {
        isBlank() -> null
        else -> toLongOrNull()?.takeIf { it >= 0 }
    }

    private fun String.toDateOrNull(): Date? {
        if (!matches(DEADLINE_DATE_REGEX)) return null
        return try {
            deadlineDateFormat().parse(this)
        } catch (_: ParseException) {
            null
        }
    }

    private fun Date.startOfDay(): Date = Calendar.getInstance().run {
        time = this@startOfDay
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        time
    }

    private fun deadlineDateFormat() = SimpleDateFormat(DEADLINE_DATE_FORMAT, Locale.US).apply {
        isLenient = false
    }

    private fun String.toEntries(): List<String> =
        split(',', '\n')
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinct()

    class Factory(
        private val jobRepository: JobRepository,
        private val jobId: String? = null,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(PostJobViewModel::class.java)) {
                "Unknown ViewModel class: ${modelClass.name}"
            }
            return PostJobViewModel(jobRepository, jobId) as T
        }
    }

    private companion object {
        const val DEADLINE_DATE_FORMAT = "yyyy-MM-dd"
        val DEADLINE_DATE_REGEX = Regex("\\d{4}-\\d{2}-\\d{2}")
    }
}
