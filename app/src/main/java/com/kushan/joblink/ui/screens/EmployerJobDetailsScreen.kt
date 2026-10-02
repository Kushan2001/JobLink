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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kushan.joblink.R
import com.kushan.joblink.data.model.Job
import com.kushan.joblink.data.model.JobType
import com.kushan.joblink.data.model.WorkMode
import com.kushan.joblink.data.repository.JobError
import com.kushan.joblink.ui.theme.JobLinkSpacing
import com.kushan.joblink.viewmodel.EmployerJobDetailsViewModel
import java.text.DateFormat
import java.text.NumberFormat

@Composable
fun EmployerJobDetailsScreen(
    viewModel: EmployerJobDetailsViewModel,
    onEdit: (String) -> Unit,
    onViewApplicants: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val job = uiState.job
    val error = uiState.error

    when {
        uiState.isLoading -> EmployerJobDetailsLoading(modifier)
        job != null -> EmployerJobDetailsContent(
            job = job,
            isUpdating = uiState.isUpdating,
            actionError = uiState.actionError,
            onEdit = { onEdit(job.id) },
            onViewApplicants = { onViewApplicants(job.id) },
            onSetActive = { viewModel.setActive(!job.active) },
            onBack = onBack,
            modifier = modifier,
        )

        else -> EmployerJobDetailsError(
            error = requireNotNull(error),
            onRetry = viewModel::retry,
            onBack = onBack,
            modifier = modifier,
        )
    }
}

@Composable
private fun EmployerJobDetailsContent(
    job: Job,
    isUpdating: Boolean,
    actionError: JobError?,
    onEdit: () -> Unit,
    onViewApplicants: () -> Unit,
    onSetActive: () -> Unit,
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
            TextButton(onClick = onBack, enabled = !isUpdating) {
                Text(text = stringResource(R.string.back))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(JobLinkSpacing.small),
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = job.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = job.companyName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                JobActivityBadge(active = job.active)
            }

            Spacer(modifier = Modifier.height(JobLinkSpacing.medium))
            Text(
                text = pluralStringResource(
                    R.plurals.applicant_count,
                    job.applicantCount.toInt(),
                    job.applicantCount,
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )
            job.createdAt?.let { timestamp ->
                Text(
                    text = stringResource(
                        R.string.posted_on,
                        DateFormat.getDateInstance(DateFormat.MEDIUM).format(timestamp.toDate()),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (actionError != null) {
                Spacer(modifier = Modifier.height(JobLinkSpacing.medium))
                EmployerJobErrorMessage(actionError)
            }

            Spacer(modifier = Modifier.height(JobLinkSpacing.large))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(JobLinkSpacing.small),
                verticalArrangement = Arrangement.spacedBy(JobLinkSpacing.small),
            ) {
                OutlinedButton(onClick = onViewApplicants, enabled = !isUpdating) {
                    Text(text = stringResource(R.string.view_applicants))
                }
                OutlinedButton(onClick = onEdit, enabled = !isUpdating) {
                    Text(text = stringResource(R.string.edit_job))
                }
                Button(onClick = onSetActive, enabled = !isUpdating) {
                    if (isUpdating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(
                            text = stringResource(
                                if (job.active) R.string.deactivate else R.string.reactivate,
                            ),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(JobLinkSpacing.extraLarge))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.large,
            ) {
                Column(
                    modifier = Modifier.padding(JobLinkSpacing.large),
                    verticalArrangement = Arrangement.spacedBy(JobLinkSpacing.medium),
                ) {
                    EmployerJobDetailRow(R.string.location, job.location)
                    EmployerJobDetailRow(R.string.job_category, job.category)
                    EmployerJobDetailRow(R.string.job_type, stringResource(job.jobType.label()))
                    EmployerJobDetailRow(R.string.work_mode, stringResource(job.workMode.label()))
                    EmployerJobDetailRow(R.string.experience_level, job.experienceLevel)
                    EmployerJobDetailRow(
                        R.string.compensation,
                        job.formattedEmployerSalary()
                            ?: stringResource(R.string.salary_not_specified),
                    )
                    EmployerJobDetailRow(
                        R.string.application_deadline,
                        job.applicationDeadline?.let { deadline ->
                            DateFormat.getDateInstance(DateFormat.MEDIUM).format(deadline.toDate())
                        } ?: stringResource(R.string.deadline_not_specified),
                    )
                }
            }

            EmployerJobTextSection(R.string.about_role, job.description)
            EmployerJobListSection(R.string.required_skills, job.requiredSkills)
            EmployerJobListSection(R.string.requirements, job.requirements)
            EmployerJobListSection(R.string.benefits, job.benefits)
        }
    }
}

@Composable
private fun EmployerJobDetailRow(@StringRes label: Int, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(JobLinkSpacing.extraSmall)) {
        Text(
            text = stringResource(label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value.ifBlank { stringResource(R.string.not_provided) })
    }
}

@Composable
private fun EmployerJobTextSection(@StringRes title: Int, value: String) {
    Spacer(modifier = Modifier.height(JobLinkSpacing.extraLarge))
    Text(
        text = stringResource(title),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
    )
    Spacer(modifier = Modifier.height(JobLinkSpacing.small))
    Text(
        text = value.ifBlank { stringResource(R.string.not_provided) },
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun EmployerJobListSection(@StringRes title: Int, values: List<String>) {
    EmployerJobTextSection(
        title = title,
        value = values.takeIf { it.isNotEmpty() }?.joinToString("\n• ", prefix = "• ")
            ?: stringResource(R.string.not_provided),
    )
}

@Composable
private fun EmployerJobDetailsLoading(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(JobLinkSpacing.medium),
        ) {
            CircularProgressIndicator()
            Text(text = stringResource(R.string.loading_job_details))
        }
    }
}

@Composable
private fun EmployerJobDetailsError(
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
            EmployerJobErrorMessage(error)
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
private fun Job.formattedEmployerSalary(): String? {
    if (salaryMin == null && salaryMax == null) return null
    val numberFormat = NumberFormat.getNumberInstance()
    return when {
        salaryMin != null && salaryMax != null -> stringResource(
            R.string.salary_range,
            currency,
            numberFormat.format(salaryMin),
            numberFormat.format(salaryMax),
        )

        salaryMin != null -> stringResource(
            R.string.salary_from,
            currency,
            numberFormat.format(salaryMin),
        )

        else -> stringResource(
            R.string.salary_up_to,
            currency,
            numberFormat.format(salaryMax),
        )
    }
}

@StringRes
private fun JobType.label(): Int = when (this) {
    JobType.FULL_TIME -> R.string.job_type_full_time
    JobType.PART_TIME -> R.string.job_type_part_time
    JobType.CONTRACT -> R.string.job_type_contract
    JobType.INTERNSHIP -> R.string.job_type_internship
    JobType.FREELANCE -> R.string.job_type_freelance
    JobType.TEMPORARY -> R.string.job_type_temporary
}

@StringRes
private fun WorkMode.label(): Int = when (this) {
    WorkMode.ONSITE -> R.string.work_mode_onsite
    WorkMode.REMOTE -> R.string.work_mode_remote
    WorkMode.HYBRID -> R.string.work_mode_hybrid
}
