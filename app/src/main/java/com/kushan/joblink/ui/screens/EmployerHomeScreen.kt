package com.kushan.joblink.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kushan.joblink.R
import com.kushan.joblink.data.model.JobApplication
import com.kushan.joblink.ui.components.ApplicationStatusBadge
import com.kushan.joblink.ui.components.DashboardMetricCard
import com.kushan.joblink.ui.components.DashboardShortcutCard
import com.kushan.joblink.ui.theme.JobLinkSpacing
import com.kushan.joblink.viewmodel.EmployerHomeUiState
import com.kushan.joblink.viewmodel.EmployerHomeViewModel
import java.text.DateFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployerHomeScreen(
    viewModel: EmployerHomeViewModel,
    onPostJob: () -> Unit,
    onMyJobs: () -> Unit,
    onApplicantClick: (String) -> Unit,
    onCompanyProfile: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when {
        uiState.isLoading -> EmployerHomeLoading(modifier)
        uiState.companyProfile == null && uiState.profileError != null -> EmployerHomeLoadError(
            onRetry = viewModel::retry,
            onLogout = onLogout,
            modifier = modifier,
        )
        else -> PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = modifier.fillMaxSize().safeDrawingPadding(),
        ) {
            EmployerDashboard(
                uiState = uiState,
                onPostJob = onPostJob,
                onMyJobs = onMyJobs,
                onApplicantClick = onApplicantClick,
                onCompanyProfile = onCompanyProfile,
                onLogout = onLogout,
                onRetry = viewModel::refresh,
            )
        }
    }
}

@Composable
private fun EmployerDashboard(
    uiState: EmployerHomeUiState,
    onPostJob: () -> Unit,
    onMyJobs: () -> Unit,
    onApplicantClick: (String) -> Unit,
    onCompanyProfile: () -> Unit,
    onLogout: () -> Unit,
    onRetry: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(JobLinkSpacing.large),
        verticalArrangement = Arrangement.spacedBy(JobLinkSpacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Column(Modifier.widthIn(max = 760.dp).fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onCompanyProfile) {
                        Text(stringResource(R.string.company_profile))
                    }
                    TextButton(onClick = onLogout) {
                        Text(stringResource(R.string.logout))
                    }
                }
                Text(
                    text = stringResource(
                        R.string.employer_home_greeting,
                        uiState.companyProfile?.companyName.orEmpty().ifBlank {
                            stringResource(R.string.your_company)
                        },
                    ),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.employer_home_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (uiState.hasDashboardError) {
            item {
                Surface(
                    modifier = Modifier.widthIn(max = 760.dp).fillMaxWidth(),
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Column(Modifier.padding(JobLinkSpacing.medium)) {
                        Text(stringResource(R.string.employer_dashboard_partial_error))
                        TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
                    }
                }
            }
        }
        item {
            Button(
                onClick = onPostJob,
                modifier = Modifier.widthIn(max = 760.dp).fillMaxWidth(),
            ) {
                Text(stringResource(R.string.post_job))
            }
        }
        item {
            Row(
                modifier = Modifier.widthIn(max = 760.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(JobLinkSpacing.medium),
            ) {
                DashboardMetricCard(
                    value = uiState.activeJobsCount.toString(),
                    label = stringResource(R.string.active_jobs_count),
                    modifier = Modifier.weight(1f),
                )
                DashboardMetricCard(
                    value = uiState.applicationsCount.toString(),
                    label = stringResource(R.string.total_applications),
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item {
            Row(
                modifier = Modifier.widthIn(max = 760.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(JobLinkSpacing.medium),
            ) {
                DashboardShortcutCard(
                    title = stringResource(R.string.my_jobs),
                    supportingText = stringResource(R.string.manage_job_postings),
                    onClick = onMyJobs,
                    modifier = Modifier.weight(1f),
                )
                DashboardShortcutCard(
                    title = stringResource(R.string.company_profile),
                    supportingText = stringResource(R.string.manage_company_profile),
                    onClick = onCompanyProfile,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item {
            Text(
                text = stringResource(R.string.recent_applicants),
                modifier = Modifier.widthIn(max = 760.dp).fillMaxWidth(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (uiState.hasRecentApplicantsError) {
            item {
                Surface(
                    modifier = Modifier.widthIn(max = 760.dp).fillMaxWidth(),
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    shape = MaterialTheme.shapes.large,
                ) {
                    Column(Modifier.padding(JobLinkSpacing.medium)) {
                        Text(stringResource(R.string.recent_applicants_load_error))
                        TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
                    }
                }
            }
        } else if (uiState.recentApplicants.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.widthIn(max = 760.dp).fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = MaterialTheme.shapes.large,
                ) {
                    Text(
                        text = stringResource(R.string.no_recent_applicants),
                        modifier = Modifier.padding(JobLinkSpacing.large),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            items(uiState.recentApplicants, key = JobApplication::applicationId) { application ->
                RecentApplicantCard(
                    application = application,
                    onClick = { onApplicantClick(application.applicationId) },
                    modifier = Modifier.widthIn(max = 760.dp),
                )
            }
        }
    }
}

@Composable
private fun RecentApplicantCard(
    application: JobApplication,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(Modifier.padding(JobLinkSpacing.medium)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(JobLinkSpacing.small),
                verticalAlignment = Alignment.Top,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        application.applicantFullName.ifBlank {
                            stringResource(R.string.applicant_name_unavailable)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        application.jobTitle.ifBlank {
                            stringResource(R.string.job_title_unavailable)
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                ApplicationStatusBadge(application.status)
            }
            Spacer(Modifier.height(JobLinkSpacing.small))
            Text(
                application.submittedAt?.let {
                    stringResource(
                        R.string.submitted_on,
                        DateFormat.getDateInstance(DateFormat.MEDIUM).format(it.toDate()),
                    )
                } ?: stringResource(R.string.submitted_date_pending),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmployerHomeLoading(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(JobLinkSpacing.medium))
        Text(stringResource(R.string.loading_employer_dashboard))
    }
}

@Composable
private fun EmployerHomeLoadError(
    onRetry: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().safeDrawingPadding().padding(JobLinkSpacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            stringResource(R.string.employer_dashboard_load_error),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(JobLinkSpacing.medium))
        Button(onClick = onRetry, modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth()) {
            Text(stringResource(R.string.retry))
        }
        TextButton(onClick = onLogout) { Text(stringResource(R.string.logout)) }
    }
}
