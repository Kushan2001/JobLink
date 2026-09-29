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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kushan.joblink.R
import com.kushan.joblink.data.model.Job
import com.kushan.joblink.data.model.JobType
import com.kushan.joblink.data.model.WorkMode
import com.kushan.joblink.ui.theme.JobLinkSpacing
import com.kushan.joblink.viewmodel.JobDetailsViewModel
import java.text.DateFormat
import java.text.NumberFormat

@Composable
fun JobDetailsScreen(
    viewModel: JobDetailsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val job = uiState.job

    when {
        uiState.isLoading -> JobDetailsLoading(modifier)
        job != null -> JobDetailsContent(
            job = job,
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
                text = job.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = job.companyName,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(JobLinkSpacing.medium))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(JobLinkSpacing.small),
            ) {
                Text(text = job.location, style = MaterialTheme.typography.bodyLarge)
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
            job.formattedSalary()?.let { salary ->
                Spacer(modifier = Modifier.height(JobLinkSpacing.small))
                Text(
                    text = salary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                )
            }
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

            DetailSection(R.string.about_role, job.description)
            DetailSection(R.string.job_category, job.category)
            DetailSection(R.string.experience, job.experienceLevel)
            DetailSection(R.string.required_skills, job.requiredSkills.joinToString(" • "))
            DetailSection(R.string.requirements, job.requirements.joinToString("\n• ", prefix = "• "))
            if (job.benefits.isNotEmpty()) {
                DetailSection(R.string.benefits, job.benefits.joinToString("\n• ", prefix = "• "))
            }
            job.applicationDeadline?.let { timestamp ->
                DetailSection(
                    R.string.application_deadline,
                    stringResource(
                        R.string.application_closes,
                        DateFormat.getDateInstance(DateFormat.MEDIUM).format(timestamp.toDate()),
                    ),
                )
            }
        }
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
