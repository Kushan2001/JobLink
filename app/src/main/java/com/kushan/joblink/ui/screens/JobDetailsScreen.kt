package com.kushan.joblink.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kushan.joblink.R
import com.kushan.joblink.data.model.Job
import com.kushan.joblink.data.model.JobType
import com.kushan.joblink.data.model.WorkMode
import com.kushan.joblink.ui.theme.JobLinkSpacing
import com.kushan.joblink.viewmodel.JobDetailsActionMessage
import com.kushan.joblink.viewmodel.JobDetailsViewModel
import java.text.DateFormat
import java.text.NumberFormat

@Composable
fun JobDetailsScreen(
    viewModel: JobDetailsViewModel,
    onApplyNow: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val job = uiState.job

    when {
        uiState.isLoading -> JobDetailsLoading(modifier)
        job != null -> JobDetailsContent(
            job = job,
            isUpdatingSavedState = uiState.isUpdatingSavedState,
            isSaved = uiState.isSaved,
            actionError = uiState.actionError,
            actionMessage = uiState.actionMessage,
            onSavedStateToggle = viewModel::onSavedStateToggle,
            onApplyNow = { onApplyNow(job.id) },
            onBack = onBack,
            modifier = modifier,
        )

        else -> JobDetailsError(
            error = requireNotNull(uiState.error),
            onRetry = viewModel::retry,
            onBack = onBack,
            modifier = modifier,
        )
    }
}

@Composable
private fun JobDetailsContent(
    job: Job,
    isUpdatingSavedState: Boolean,
    isSaved: Boolean,
    actionError: com.kushan.joblink.data.repository.JobError?,
    actionMessage: JobDetailsActionMessage?,
    onSavedStateToggle: () -> Unit,
    onApplyNow: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(JobLinkSpacing.large),
        ) {
            TextButton(onClick = onBack) {
                Text(text = stringResource(R.string.back))
            }
            Text(
                text = job.title.ifBlank { stringResource(R.string.job_details) },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = job.companyName.ifBlank {
                    stringResource(R.string.company_not_specified)
                },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(JobLinkSpacing.medium))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(JobLinkSpacing.small),
            ) {
                Text(
                    text = job.location.ifBlank {
                        stringResource(R.string.location_not_specified)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(text = "•", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = stringResource(job.workMode.labelResource()),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            Text(
                text = stringResource(job.jobType.labelResource()),
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(modifier = Modifier.height(JobLinkSpacing.small))
            Text(
                text = job.formattedSalary()
                    ?: stringResource(R.string.salary_not_specified),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )
            job.createdAt?.let { timestamp ->
                Spacer(modifier = Modifier.height(JobLinkSpacing.small))
                Text(
                    text = stringResource(
                        R.string.posted_on,
                        DateFormat.getDateInstance(DateFormat.MEDIUM).format(timestamp.toDate()),
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            Spacer(modifier = Modifier.height(JobLinkSpacing.large))
            OutlinedButton(
                onClick = onSavedStateToggle,
                enabled = !isUpdatingSavedState,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(
                        when {
                            isUpdatingSavedState && isSaved -> R.string.removing_saved_job
                            isUpdatingSavedState -> R.string.saving_job
                            isSaved -> R.string.unsave_job
                            else -> R.string.save_job
                        },
                    ),
                )
            }
            Button(
                onClick = onApplyNow,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(R.string.apply_now))
            }

            actionMessage?.let { message ->
                Spacer(modifier = Modifier.height(JobLinkSpacing.small))
                JobActionMessage(message = message)
            }
            actionError?.let { error ->
                Spacer(modifier = Modifier.height(JobLinkSpacing.small))
                JobActionErrorMessage(error = error)
            }

            DetailSection(
                R.string.about_role,
                job.description.ifBlank { stringResource(R.string.description_not_provided) },
            )
            DetailSection(
                R.string.job_category,
                job.category.ifBlank { stringResource(R.string.not_provided) },
            )
            DetailSection(
                R.string.experience,
                job.experienceLevel.ifBlank { stringResource(R.string.not_provided) },
            )
            DetailSection(
                R.string.required_skills,
                job.requiredSkills.toInlineListOr(
                    fallback = stringResource(R.string.skills_not_specified),
                ),
            )
            DetailSection(
                R.string.requirements,
                job.requirements.toBulletListOr(
                    fallback = stringResource(R.string.requirements_not_specified),
                ),
            )
            DetailSection(
                R.string.benefits,
                job.benefits.toBulletListOr(
                    fallback = stringResource(R.string.benefits_not_specified),
                ),
            )
            DetailSection(
                R.string.application_deadline,
                job.applicationDeadline?.let { timestamp ->
                    stringResource(
                        R.string.application_closes,
                        DateFormat.getDateInstance(DateFormat.MEDIUM).format(timestamp.toDate()),
                    )
                } ?: stringResource(R.string.deadline_not_specified),
            )
        }
    }
}

@Composable
private fun JobActionMessage(message: JobDetailsActionMessage) {
    val text = when (message) {
        JobDetailsActionMessage.JOB_SAVED -> R.string.job_saved_confirmation
        JobDetailsActionMessage.JOB_UNSAVED -> R.string.job_unsaved_confirmation
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.medium,
    ) {
        Text(
            text = stringResource(text),
            modifier = Modifier.padding(JobLinkSpacing.medium),
        )
    }
}

@Composable
private fun JobActionErrorMessage(
    error: com.kushan.joblink.data.repository.JobError,
) {
    val text = when (error) {
        com.kushan.joblink.data.repository.JobError.NOT_AUTHENTICATED -> {
            R.string.save_job_error_auth
        }

        com.kushan.joblink.data.repository.JobError.PERMISSION_DENIED -> {
            R.string.save_job_error_permission
        }

        com.kushan.joblink.data.repository.JobError.NETWORK -> R.string.save_job_error_network
        else -> R.string.save_job_error_unknown
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Assertive },
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.medium,
    ) {
        Text(
            text = stringResource(text),
            modifier = Modifier.padding(JobLinkSpacing.medium),
        )
    }
}

@Composable
private fun DetailSection(
    @StringRes title: Int,
    value: String,
) {
    Spacer(modifier = Modifier.height(JobLinkSpacing.large))
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(modifier = Modifier.padding(JobLinkSpacing.large)) {
            Text(
                text = stringResource(title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(JobLinkSpacing.small))
            Text(text = value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun JobDetailsLoading(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun JobDetailsError(
    error: com.kushan.joblink.data.repository.JobError,
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
        ) {
            JobFeedErrorMessage(error = error)
            Spacer(modifier = Modifier.height(JobLinkSpacing.medium))
            Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.retry))
            }
            TextButton(onClick = onBack) {
                Text(text = stringResource(R.string.back))
            }
        }
    }
}

@Composable
private fun Job.formattedSalary(): String? {
    if (salaryMin == null && salaryMax == null) return null
    val numberFormat = NumberFormat.getNumberInstance()
    return when {
        salaryMin != null && salaryMax != null -> stringResource(
            R.string.salary_range,
            currency,
            numberFormat.format(salaryMin),
            numberFormat.format(salaryMax),
        ).trim()

        salaryMin != null -> stringResource(
            R.string.salary_from,
            currency,
            numberFormat.format(salaryMin),
        ).trim()

        else -> stringResource(
            R.string.salary_up_to,
            currency,
            numberFormat.format(salaryMax),
        ).trim()
    }
}

private fun List<String>.toInlineListOr(fallback: String): String =
    filter(String::isNotBlank).joinToString(" • ").ifBlank { fallback }

private fun List<String>.toBulletListOr(fallback: String): String {
    val entries = filter(String::isNotBlank)
    return if (entries.isEmpty()) fallback else entries.joinToString("\n• ", prefix = "• ")
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
private fun WorkMode.labelResource(): Int = when (this) {
    WorkMode.ONSITE -> R.string.work_mode_onsite
    WorkMode.REMOTE -> R.string.work_mode_remote
    WorkMode.HYBRID -> R.string.work_mode_hybrid
}
