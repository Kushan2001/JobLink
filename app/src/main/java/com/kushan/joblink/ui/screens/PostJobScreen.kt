package com.kushan.joblink.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kushan.joblink.R
import com.kushan.joblink.data.model.Job
import com.kushan.joblink.data.model.JobType
import com.kushan.joblink.data.model.WorkMode
import com.kushan.joblink.data.repository.JobError
import com.kushan.joblink.ui.theme.JobLinkSpacing
import com.kushan.joblink.viewmodel.PostJobUiState
import com.kushan.joblink.viewmodel.PostJobValidationError
import com.kushan.joblink.viewmodel.PostJobViewModel

@Composable
fun PostJobScreen(
    viewModel: PostJobViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val postedJob = uiState.postedJob
    val loadError = uiState.error

    when {
        uiState.isLoading -> JobFormLoading(modifier)
        uiState.isEditMode && !uiState.hasLoadedJob && loadError != null -> {
            JobFormLoadError(
                error = loadError,
                onRetry = viewModel::retryLoading,
                onBack = onBack,
                modifier = modifier,
            )
        }

        postedJob != null -> JobPostedConfirmation(
            job = postedJob,
            isEditMode = uiState.isEditMode,
            onPostAnother = viewModel::startAnotherJob,
            onBack = onBack,
            modifier = modifier,
        )

        else -> PostJobForm(
            uiState = uiState,
            onTitleChanged = viewModel::onTitleChanged,
            onDescriptionChanged = viewModel::onDescriptionChanged,
            onCategoryChanged = viewModel::onCategoryChanged,
            onLocationChanged = viewModel::onLocationChanged,
            onWorkModeChanged = viewModel::onWorkModeChanged,
            onJobTypeChanged = viewModel::onJobTypeChanged,
            onSalaryMinChanged = viewModel::onSalaryMinChanged,
            onSalaryMaxChanged = viewModel::onSalaryMaxChanged,
            onCurrencyChanged = viewModel::onCurrencyChanged,
            onExperienceLevelChanged = viewModel::onExperienceLevelChanged,
            onRequiredSkillsChanged = viewModel::onRequiredSkillsChanged,
            onRequirementsChanged = viewModel::onRequirementsChanged,
            onBenefitsChanged = viewModel::onBenefitsChanged,
            onApplicationDeadlineChanged = viewModel::onApplicationDeadlineChanged,
            onPost = viewModel::postJob,
            onBack = onBack,
            modifier = modifier,
        )
    }
}

@Composable
private fun PostJobForm(
    uiState: PostJobUiState,
    onTitleChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onCategoryChanged: (String) -> Unit,
    onLocationChanged: (String) -> Unit,
    onWorkModeChanged: (WorkMode) -> Unit,
    onJobTypeChanged: (JobType) -> Unit,
    onSalaryMinChanged: (String) -> Unit,
    onSalaryMaxChanged: (String) -> Unit,
    onCurrencyChanged: (String) -> Unit,
    onExperienceLevelChanged: (String) -> Unit,
    onRequiredSkillsChanged: (String) -> Unit,
    onRequirementsChanged: (String) -> Unit,
    onBenefitsChanged: (String) -> Unit,
    onApplicationDeadlineChanged: (String) -> Unit,
    onPost: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PostJobPage(modifier = modifier) {
        TextButton(onClick = onBack, enabled = !uiState.isPosting) {
            Text(text = stringResource(R.string.back))
        }
        Text(
            text = stringResource(
                if (uiState.isEditMode) R.string.edit_job else R.string.post_job_title,
            ),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(JobLinkSpacing.small))
        Text(
            text = stringResource(
                if (uiState.isEditMode) R.string.edit_job_subtitle else R.string.post_job_subtitle,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
        )

        if (uiState.error != null) {
            Spacer(modifier = Modifier.height(JobLinkSpacing.medium))
            JobPostingErrorMessage(error = uiState.error)
        }

        FormSectionTitle(R.string.job_details)
        JobTextField(
            value = uiState.title,
            onValueChange = onTitleChanged,
            label = R.string.job_title,
            error = uiState.errorFor(
                PostJobValidationError.TITLE_REQUIRED,
                R.string.job_title_required,
            ),
            enabled = !uiState.isPosting,
        )
        JobTextField(
            value = uiState.description,
            onValueChange = onDescriptionChanged,
            label = R.string.job_description,
            error = uiState.errorFor(
                PostJobValidationError.DESCRIPTION_REQUIRED,
                R.string.job_description_required,
            ),
            enabled = !uiState.isPosting,
            singleLine = false,
            minLines = 5,
        )
        JobTextField(
            value = uiState.category,
            onValueChange = onCategoryChanged,
            label = R.string.job_category,
            error = uiState.errorFor(
                PostJobValidationError.CATEGORY_REQUIRED,
                R.string.job_category_required,
            ),
            enabled = !uiState.isPosting,
        )
        JobTextField(
            value = uiState.location,
            onValueChange = onLocationChanged,
            label = R.string.location,
            error = uiState.errorFor(
                PostJobValidationError.LOCATION_REQUIRED,
                R.string.location_required,
            ),
            enabled = !uiState.isPosting,
        )

        ChoiceLabel(R.string.work_mode)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(JobLinkSpacing.small),
        ) {
            WorkMode.entries.forEach { workMode ->
                FilterChip(
                    selected = uiState.workMode == workMode,
                    onClick = { onWorkModeChanged(workMode) },
                    enabled = !uiState.isPosting,
                    label = { Text(text = stringResource(workMode.labelResource())) },
                )
            }
        }

        ChoiceLabel(R.string.job_type)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(JobLinkSpacing.small),
        ) {
            JobType.entries.forEach { jobType ->
                FilterChip(
                    selected = uiState.jobType == jobType,
                    onClick = { onJobTypeChanged(jobType) },
                    enabled = !uiState.isPosting,
                    label = { Text(text = stringResource(jobType.labelResource())) },
                )
            }
        }
        Spacer(modifier = Modifier.height(JobLinkSpacing.medium))
        JobTextField(
            value = uiState.experienceLevel,
            onValueChange = onExperienceLevelChanged,
            label = R.string.experience_level,
            error = uiState.errorFor(
                PostJobValidationError.EXPERIENCE_LEVEL_REQUIRED,
                R.string.experience_level_required,
            ),
            enabled = !uiState.isPosting,
        )

        FormSectionTitle(R.string.compensation)
        JobTextField(
            value = uiState.salaryMin,
            onValueChange = onSalaryMinChanged,
            label = R.string.salary_min,
            error = when {
                PostJobValidationError.SALARY_MIN_INVALID in uiState.validationErrors -> {
                    R.string.salary_invalid
                }

                PostJobValidationError.SALARY_RANGE_INVALID in uiState.validationErrors -> {
                    R.string.salary_range_invalid
                }

                else -> null
            },
            enabled = !uiState.isPosting,
            keyboardType = KeyboardType.Number,
        )
        JobTextField(
            value = uiState.salaryMax,
            onValueChange = onSalaryMaxChanged,
            label = R.string.salary_max,
            error = uiState.errorFor(
                PostJobValidationError.SALARY_MAX_INVALID,
                R.string.salary_invalid,
            ),
            enabled = !uiState.isPosting,
            keyboardType = KeyboardType.Number,
        )
        JobTextField(
            value = uiState.currency,
            onValueChange = onCurrencyChanged,
            label = R.string.currency,
            error = uiState.errorFor(
                PostJobValidationError.CURRENCY_REQUIRED,
                R.string.currency_required,
            ),
            supportingText = R.string.currency_hint,
            enabled = !uiState.isPosting,
        )

        FormSectionTitle(R.string.candidate_requirements)
        JobTextField(
            value = uiState.requiredSkills,
            onValueChange = onRequiredSkillsChanged,
            label = R.string.required_skills,
            error = uiState.errorFor(
                PostJobValidationError.SKILLS_REQUIRED,
                R.string.required_skills_required,
            ),
            supportingText = R.string.requirements_hint,
            enabled = !uiState.isPosting,
            singleLine = false,
            minLines = 2,
        )
        JobTextField(
            value = uiState.requirements,
            onValueChange = onRequirementsChanged,
            label = R.string.requirements,
            error = uiState.errorFor(
                PostJobValidationError.REQUIREMENTS_REQUIRED,
                R.string.requirements_required,
            ),
            supportingText = R.string.requirements_hint,
            enabled = !uiState.isPosting,
            singleLine = false,
            minLines = 3,
        )
        JobTextField(
            value = uiState.benefits,
            onValueChange = onBenefitsChanged,
            label = R.string.benefits,
            supportingText = R.string.benefits_optional,
            enabled = !uiState.isPosting,
            singleLine = false,
            minLines = 2,
        )
        JobTextField(
            value = uiState.applicationDeadline,
            onValueChange = onApplicationDeadlineChanged,
            label = R.string.application_deadline,
            error = when {
                PostJobValidationError.DEADLINE_REQUIRED in uiState.validationErrors -> {
                    R.string.deadline_required
                }

                PostJobValidationError.DEADLINE_INVALID in uiState.validationErrors -> {
                    R.string.deadline_invalid
                }

                PostJobValidationError.DEADLINE_NOT_FUTURE in uiState.validationErrors -> {
                    R.string.deadline_not_future
                }

                else -> null
            },
            supportingText = R.string.deadline_hint,
            enabled = !uiState.isPosting,
        )

        Spacer(modifier = Modifier.height(JobLinkSpacing.small))
        Button(
            onClick = onPost,
            enabled = !uiState.isPosting,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
        ) {
            if (uiState.isPosting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                )
            } else {
                Text(
                    text = stringResource(
                        if (uiState.isEditMode) R.string.save_job_changes else R.string.publish_job,
                    ),
                )
            }
        }
    }
}

@Composable
private fun JobPostedConfirmation(
    job: Job,
    isEditMode: Boolean,
    onPostAnother: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(JobLinkSpacing.large),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .semantics { liveRegion = LiveRegionMode.Polite },
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = MaterialTheme.shapes.large,
        ) {
            Column(
                modifier = Modifier.padding(JobLinkSpacing.extraLarge),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(
                        if (isEditMode) R.string.job_updated_title else R.string.job_posted_title,
                    ),
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(JobLinkSpacing.medium))
                Text(
                    text = stringResource(
                        if (isEditMode) {
                            R.string.job_updated_message
                        } else {
                            R.string.job_posted_message
                        },
                        job.title,
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(modifier = Modifier.height(JobLinkSpacing.extraLarge))
                if (!isEditMode) {
                    Button(
                        onClick = onPostAnother,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text = stringResource(R.string.post_another_job))
                    }
                }
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(
                            if (isEditMode) {
                                R.string.return_to_my_jobs
                            } else {
                                R.string.return_to_company_profile
                            },
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun JobFormLoading(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(JobLinkSpacing.medium),
        ) {
            CircularProgressIndicator()
            Text(text = stringResource(R.string.loading_job_for_editing))
        }
    }
}

@Composable
private fun JobFormLoadError(
    error: JobError,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(JobLinkSpacing.large),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 560.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(JobLinkSpacing.medium),
        ) {
            JobPostingErrorMessage(error)
            if (error != JobError.JOB_NOT_FOUND) {
                Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
                    Text(text = stringResource(R.string.retry))
                }
            }
            TextButton(onClick = onBack) {
                Text(text = stringResource(R.string.back))
            }
        }
    }
}

@Composable
private fun PostJobPage(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(JobLinkSpacing.large),
        ) {
            content()
        }
    }
}

@Composable
private fun FormSectionTitle(@StringRes title: Int) {
    Spacer(modifier = Modifier.height(JobLinkSpacing.extraLarge))
    Text(
        text = stringResource(title),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
    )
    Spacer(modifier = Modifier.height(JobLinkSpacing.medium))
}

@Composable
private fun ChoiceLabel(@StringRes label: Int) {
    Text(
        text = stringResource(label),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun JobTextField(
    value: String,
    onValueChange: (String) -> Unit,
    @StringRes label: Int,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    @StringRes error: Int? = null,
    @StringRes supportingText: Int? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    minLines: Int = 1,
) {
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        label = { Text(text = stringResource(label)) },
        supportingText = when {
            error != null -> { { Text(text = stringResource(error)) } }
            supportingText != null -> { { Text(text = stringResource(supportingText)) } }
            else -> null
        },
        isError = error != null,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = if (singleLine) ImeAction.Next else ImeAction.Default,
        ),
        keyboardActions = KeyboardActions(
            onNext = { focusManager.moveFocus(FocusDirection.Down) },
        ),
        singleLine = singleLine,
        minLines = minLines,
    )
    Spacer(modifier = Modifier.height(JobLinkSpacing.medium))
}

@Composable
private fun JobPostingErrorMessage(
    error: JobError,
    modifier: Modifier = Modifier,
) {
    val message = when (error) {
        JobError.NOT_AUTHENTICATED -> R.string.job_error_not_authenticated
        JobError.ACCOUNT_NOT_FOUND -> R.string.job_error_account_missing
        JobError.WRONG_ROLE -> R.string.job_error_wrong_role
        JobError.COMPANY_PROFILE_REQUIRED -> R.string.job_error_company_profile_required
        JobError.PERMISSION_DENIED -> R.string.job_error_permission_denied
        JobError.NETWORK -> R.string.job_error_network
        JobError.JOB_NOT_FOUND -> R.string.job_error_unknown
        JobError.UNKNOWN -> R.string.job_error_unknown
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Assertive },
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.medium,
    ) {
        Text(
            text = stringResource(message),
            modifier = Modifier.padding(JobLinkSpacing.medium),
        )
    }
}

@StringRes
private fun WorkMode.labelResource(): Int = when (this) {
    WorkMode.ONSITE -> R.string.work_mode_onsite
    WorkMode.REMOTE -> R.string.work_mode_remote
    WorkMode.HYBRID -> R.string.work_mode_hybrid
}

@StringRes
private fun JobType.labelResource(): Int = when (this) {
    JobType.FULL_TIME -> R.string.job_type_full_time
    JobType.PART_TIME -> R.string.job_type_part_time
    JobType.CONTRACT -> R.string.job_type_contract
    JobType.INTERNSHIP -> R.string.job_type_internship
    JobType.FREELANCE -> R.string.job_type_freelance
    JobType.TEMPORARY -> R.string.job_type_temporary
}

@StringRes
private fun PostJobUiState.errorFor(
    validationError: PostJobValidationError,
    @StringRes message: Int,
): Int? = message.takeIf { validationError in validationErrors }
