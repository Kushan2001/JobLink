package com.kushan.joblink.ui.screens

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kushan.joblink.R
import com.kushan.joblink.data.model.ApplicationStatus
import com.kushan.joblink.data.model.JobApplication
import com.kushan.joblink.data.repository.ApplicationError
import com.kushan.joblink.ui.components.ApplicationStatusBadge
import com.kushan.joblink.ui.components.ListLoadingState
import com.kushan.joblink.ui.components.labelResource
import com.kushan.joblink.ui.theme.JobLinkSpacing
import com.kushan.joblink.viewmodel.MyApplicationsUiState
import com.kushan.joblink.viewmodel.MyApplicationsViewModel
import java.text.DateFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyApplicationsScreen(
    viewModel: MyApplicationsViewModel,
    onApplicationClick: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val error = uiState.error

    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MyApplicationsHeader(onBack = onBack)
        ApplicationStatusFilters(
            selectedStatus = uiState.selectedStatus,
            onStatusSelected = viewModel::onStatusSelected,
        )
        when {
            uiState.isLoading -> MyApplicationsLoading()
            error != null && uiState.applications.isEmpty() -> MyApplicationsError(
                error = error,
                onRetry = viewModel::retry,
            )

            else -> PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier
                    .widthIn(max = 760.dp)
                    .fillMaxSize(),
            ) {
                MyApplicationsList(
                    uiState = uiState,
                    onApplicationClick = onApplicationClick,
                    onRetry = viewModel::refresh,
                )
            }
        }
    }
}

@Composable
private fun MyApplicationsHeader(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .widthIn(max = 760.dp)
            .fillMaxWidth()
            .padding(horizontal = JobLinkSpacing.large),
    ) {
        TextButton(onClick = onBack) {
            Text(text = stringResource(R.string.back))
        }
        Text(
            text = stringResource(R.string.my_applications),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(R.string.my_applications_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(JobLinkSpacing.medium))
    }
}

@Composable
private fun ApplicationStatusFilters(
    selectedStatus: ApplicationStatus?,
    onStatusSelected: (ApplicationStatus?) -> Unit,
) {
    Row(
        modifier = Modifier
            .widthIn(max = 760.dp)
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = JobLinkSpacing.large),
        horizontalArrangement = Arrangement.spacedBy(JobLinkSpacing.small),
    ) {
        FilterChip(
            selected = selectedStatus == null,
            onClick = { onStatusSelected(null) },
            label = { Text(text = stringResource(R.string.all_statuses)) },
        )
        ApplicationStatus.entries.forEach { status ->
            FilterChip(
                selected = selectedStatus == status,
                onClick = { onStatusSelected(status) },
                label = { Text(text = stringResource(status.labelResource())) },
            )
        }
    }
    Spacer(modifier = Modifier.height(JobLinkSpacing.small))
}

@Composable
private fun MyApplicationsList(
    uiState: MyApplicationsUiState,
    onApplicationClick: (String) -> Unit,
    onRetry: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(JobLinkSpacing.large),
        verticalArrangement = Arrangement.spacedBy(JobLinkSpacing.medium),
    ) {
        uiState.error?.let { error ->
            item {
                ApplicationListErrorMessage(error)
                TextButton(onClick = onRetry) {
                    Text(text = stringResource(R.string.refresh))
                }
            }
        }
        if (uiState.filteredApplications.isEmpty()) {
            item {
                MyApplicationsEmpty(isFiltered = uiState.selectedStatus != null)
            }
        } else {
            items(
                items = uiState.filteredApplications,
                key = { it.applicationId },
            ) { application ->
                ApplicationCard(
                    application = application,
                    onClick = { onApplicationClick(application.applicationId) },
                )
            }
        }
    }
}

@Composable
private fun ApplicationCard(
    application: JobApplication,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
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
                Text(
                    text = application.jobTitle.ifBlank {
                        stringResource(R.string.job_title_unavailable)
                    },
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                ApplicationStatusBadge(status = application.status)
            }
            Spacer(modifier = Modifier.height(JobLinkSpacing.extraSmall))
            Text(
                text = application.companyName.ifBlank {
                    stringResource(R.string.company_not_specified)
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(JobLinkSpacing.medium))
            Text(
                text = application.submittedAt?.let { timestamp ->
                    stringResource(
                        R.string.submitted_on,
                        DateFormat.getDateInstance(DateFormat.MEDIUM).format(timestamp.toDate()),
                    )
                } ?: stringResource(R.string.submitted_date_pending),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MyApplicationsEmpty(isFiltered: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = JobLinkSpacing.section),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(
                if (isFiltered) {
                    R.string.no_applications_for_status
                } else {
                    R.string.no_applications_title
                },
            ),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(
                if (isFiltered) {
                    R.string.no_applications_for_status_message
                } else {
                    R.string.no_applications_message
                },
            ),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MyApplicationsLoading() {
    ListLoadingState(
        label = stringResource(R.string.loading_applications),
        modifier = Modifier.widthIn(max = 760.dp),
    )
}

@Composable
private fun MyApplicationsError(
    error: ApplicationError,
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
        ) {
            ApplicationListErrorMessage(error)
            Spacer(modifier = Modifier.height(JobLinkSpacing.medium))
            Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.retry))
            }
        }
    }
}

@Composable
internal fun ApplicationListErrorMessage(error: ApplicationError) {
    val message = when (error) {
        ApplicationError.NOT_AUTHENTICATED -> R.string.application_error_auth
        ApplicationError.PROFILE_NOT_FOUND -> R.string.application_error_profile
        ApplicationError.WRONG_ROLE -> R.string.application_error_role
        ApplicationError.APPLICATION_NOT_FOUND -> R.string.application_details_not_found
        ApplicationError.PERMISSION_DENIED -> R.string.application_list_error_permission
        ApplicationError.NETWORK -> R.string.application_list_error_network
        else -> R.string.application_list_error_unknown
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
