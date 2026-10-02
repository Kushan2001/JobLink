package com.kushan.joblink.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kushan.joblink.R
import com.kushan.joblink.data.model.Job
import com.kushan.joblink.data.model.JobType
import com.kushan.joblink.data.repository.JobError
import com.kushan.joblink.ui.theme.JobLinkSpacing
import com.kushan.joblink.viewmodel.EmployerJobsUiState
import com.kushan.joblink.viewmodel.EmployerJobsViewModel
import java.text.DateFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployerJobsScreen(
    viewModel: EmployerJobsViewModel,
    onViewJob: (String) -> Unit,
    onEditJob: (String) -> Unit,
    onPostJob: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val error = uiState.error
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        EmployerJobsHeader(onPostJob = onPostJob, onBack = onBack)
        when {
            uiState.isLoading -> EmployerJobsLoading()
            error != null && uiState.jobs.isEmpty() -> EmployerJobsError(
                error = error,
                onRetry = viewModel::retry,
            )

            else -> PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier
                    .widthIn(max = 800.dp)
                    .fillMaxSize(),
            ) {
                EmployerJobsList(
                    uiState = uiState,
                    onViewJob = onViewJob,
                    onEditJob = onEditJob,
                    onSetJobActive = viewModel::setJobActive,
                    onRetry = viewModel::refresh,
                )
            }
        }
    }
}

@Composable
private fun EmployerJobsHeader(
    onPostJob: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .widthIn(max = 800.dp)
            .fillMaxWidth()
            .padding(horizontal = JobLinkSpacing.large),
    ) {
        TextButton(onClick = onBack) {
            Text(text = stringResource(R.string.back))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.my_jobs),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Button(onClick = onPostJob) {
                Text(text = stringResource(R.string.post_job))
            }
        }
        Text(
            text = stringResource(R.string.my_jobs_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(JobLinkSpacing.medium))
    }
}

@Composable
private fun EmployerJobsList(
    uiState: EmployerJobsUiState,
    onViewJob: (String) -> Unit,
    onEditJob: (String) -> Unit,
    onSetJobActive: (String, Boolean) -> Unit,
    onRetry: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(JobLinkSpacing.large),
        verticalArrangement = Arrangement.spacedBy(JobLinkSpacing.medium),
    ) {
        uiState.error?.let { error ->
            item {
                EmployerJobErrorMessage(error)
                TextButton(onClick = onRetry) {
                    Text(text = stringResource(R.string.refresh))
                }
            }
        }
        uiState.actionError?.let { error ->
            item { EmployerJobErrorMessage(error) }
        }
        if (uiState.jobs.isEmpty()) {
            item { EmployerJobsEmpty() }
        } else {
            items(uiState.jobs, key = { it.id }) { job ->
                EmployerJobCard(
                    job = job,
                    isUpdating = uiState.updatingJobId == job.id,
                    actionsEnabled = uiState.updatingJobId == null,
                    onView = { onViewJob(job.id) },
                    onEdit = { onEditJob(job.id) },
                    onSetActive = { onSetJobActive(job.id, !job.active) },
                )
            }
        }
    }
}

@Composable
private fun EmployerJobCard(
    job: Job,
    isUpdating: Boolean,
    actionsEnabled: Boolean,
    onView: () -> Unit,
    onEdit: () -> Unit,
    onSetActive: () -> Unit,
) {
    Card(
        onClick = onView,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(modifier = Modifier.padding(JobLinkSpacing.large)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(JobLinkSpacing.small),
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = job.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = listOf(job.location, job.jobType.displayName())
                            .filter(String::isNotBlank)
                            .joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                color = MaterialTheme.colorScheme.primary,
            )
            job.createdAt?.let { createdAt ->
                Text(
                    text = stringResource(
                        R.string.posted_on,
                        DateFormat.getDateInstance(DateFormat.MEDIUM).format(createdAt.toDate()),
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(JobLinkSpacing.medium))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(JobLinkSpacing.small),
                verticalArrangement = Arrangement.spacedBy(JobLinkSpacing.small),
            ) {
                OutlinedButton(onClick = onView, enabled = actionsEnabled) {
                    Text(text = stringResource(R.string.view))
                }
                OutlinedButton(onClick = onEdit, enabled = actionsEnabled) {
                    Text(text = stringResource(R.string.edit))
                }
                Button(onClick = onSetActive, enabled = actionsEnabled) {
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
        }
    }
}

@Composable
internal fun JobActivityBadge(active: Boolean) {
    Surface(
        color = if (active) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        contentColor = if (active) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = stringResource(if (active) R.string.active else R.string.inactive),
            modifier = Modifier.padding(
                horizontal = JobLinkSpacing.small,
                vertical = JobLinkSpacing.extraSmall,
            ),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun EmployerJobsEmpty() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = JobLinkSpacing.section),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.no_employer_jobs_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(R.string.no_employer_jobs_message),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmployerJobsLoading() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(JobLinkSpacing.medium),
        ) {
            CircularProgressIndicator()
            Text(text = stringResource(R.string.loading_employer_jobs))
        }
    }
}

@Composable
private fun EmployerJobsError(
    error: JobError,
    onRetry: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(JobLinkSpacing.large),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 560.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(JobLinkSpacing.medium),
        ) {
            EmployerJobErrorMessage(error)
            Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.retry))
            }
        }
    }
}

@Composable
internal fun EmployerJobErrorMessage(error: JobError) {
    val message = when (error) {
        JobError.NOT_AUTHENTICATED -> R.string.job_error_not_authenticated
        JobError.ACCOUNT_NOT_FOUND -> R.string.job_error_account_missing
        JobError.WRONG_ROLE -> R.string.job_error_wrong_role
        JobError.PERMISSION_DENIED -> R.string.employer_jobs_error_permission
        JobError.NETWORK -> R.string.employer_jobs_error_network
        JobError.JOB_NOT_FOUND -> R.string.employer_job_not_found
        else -> R.string.employer_jobs_error_unknown
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
            text = stringResource(message),
            modifier = Modifier.padding(JobLinkSpacing.medium),
        )
    }
}

@Composable
private fun JobType.displayName(): String = stringResource(
    when (this) {
        JobType.FULL_TIME -> R.string.job_type_full_time
        JobType.PART_TIME -> R.string.job_type_part_time
        JobType.CONTRACT -> R.string.job_type_contract
        JobType.INTERNSHIP -> R.string.job_type_internship
        JobType.FREELANCE -> R.string.job_type_freelance
        JobType.TEMPORARY -> R.string.job_type_temporary
    },
)
