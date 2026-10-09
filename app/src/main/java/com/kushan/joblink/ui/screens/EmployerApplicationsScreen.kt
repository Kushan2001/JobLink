package com.kushan.joblink.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import com.kushan.joblink.data.repository.ApplicationError
import com.kushan.joblink.ui.components.ApplicationStatusBadge
import com.kushan.joblink.ui.components.ListLoadingState
import com.kushan.joblink.ui.theme.JobLinkSpacing
import com.kushan.joblink.viewmodel.EmployerApplicationsViewModel
import java.text.DateFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployerApplicationsScreen(
    viewModel: EmployerApplicationsViewModel,
    onApplicantClick: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Column(
        modifier = modifier.fillMaxSize().safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 760.dp).fillMaxWidth()
                .padding(horizontal = JobLinkSpacing.large),
        ) {
            TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
            Text(
                text = stringResource(R.string.applicants),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
            uiState.data?.jobTitle?.takeIf(String::isNotBlank)?.let { title ->
                Text(
                    text = stringResource(R.string.applicants_for_job, title),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(JobLinkSpacing.medium))
        }

        when {
            uiState.isLoading -> ApplicantListLoading()
            uiState.error != null && uiState.data == null -> ApplicantListError(
                error = uiState.error!!,
                onRetry = viewModel::retry,
            )
            else -> PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.widthIn(max = 760.dp).fillMaxSize(),
            ) {
                ApplicantList(
                    applications = uiState.data?.applications.orEmpty(),
                    refreshError = uiState.error,
                    onApplicantClick = onApplicantClick,
                    onRetry = viewModel::refresh,
                )
            }
        }
    }
}

@Composable
private fun ApplicantList(
    applications: List<JobApplication>,
    refreshError: ApplicationError?,
    onApplicantClick: (String) -> Unit,
    onRetry: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(JobLinkSpacing.large),
        verticalArrangement = Arrangement.spacedBy(JobLinkSpacing.medium),
    ) {
        refreshError?.let { error ->
            item {
                EmployerApplicationErrorMessage(error)
                TextButton(onClick = onRetry) {
                    Text(stringResource(R.string.refresh))
                }
            }
        }
        if (applications.isEmpty()) {
            item { ApplicantListEmpty() }
        } else {
            items(applications, key = { it.applicationId }) { application ->
                ApplicantCard(
                    application = application,
                    onClick = { onApplicantClick(application.applicationId) },
                )
            }
        }
    }
}

@Composable
private fun ApplicantCard(application: JobApplication, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(Modifier.padding(JobLinkSpacing.large)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(JobLinkSpacing.small),
                verticalAlignment = Alignment.Top,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = application.applicantFullName.ifBlank {
                            stringResource(R.string.applicant_name_unavailable)
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    application.applicantHeadline.takeIf(String::isNotBlank)?.let {
                        Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                ApplicationStatusBadge(application.status)
            }
            Spacer(Modifier.height(JobLinkSpacing.small))
            val summary = listOf(
                application.applicantLocation,
                application.applicantEmail,
            ).filter(String::isNotBlank).joinToString(" • ")
            if (summary.isNotBlank()) Text(summary, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = application.submittedAt?.let {
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
internal fun ApplicantListLoading() {
    ListLoadingState(
        label = stringResource(R.string.loading_applicants),
        modifier = Modifier.widthIn(max = 760.dp),
    )
}

@Composable
private fun ApplicantListEmpty() = Box(
    modifier = Modifier.fillMaxSize().padding(JobLinkSpacing.large),
    contentAlignment = Alignment.Center,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            stringResource(R.string.no_applicants_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            stringResource(R.string.no_applicants_message),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ApplicantListError(error: ApplicationError, onRetry: () -> Unit) = Box(
    modifier = Modifier.fillMaxSize().padding(JobLinkSpacing.large),
    contentAlignment = Alignment.Center,
) {
    Column(
        modifier = Modifier.widthIn(max = 560.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        EmployerApplicationErrorMessage(error)
        Spacer(Modifier.height(JobLinkSpacing.medium))
        Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.retry))
        }
    }
}
