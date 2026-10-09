package com.kushan.joblink.ui.screens

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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kushan.joblink.R
import com.kushan.joblink.data.model.JobApplication
import com.kushan.joblink.data.repository.ApplicationError
import com.kushan.joblink.ui.components.ApplicationStatusBadge
import com.kushan.joblink.ui.theme.JobLinkSpacing
import com.kushan.joblink.viewmodel.ApplicationDetailsViewModel
import java.text.DateFormat

@Composable
fun ApplicationDetailsScreen(
    viewModel: ApplicationDetailsViewModel,
    onViewJob: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val application = uiState.application
    val error = uiState.error

    when {
        uiState.isLoading -> ApplicationDetailsLoading(modifier)
        application != null -> ApplicationDetailsContent(
            application = application,
            onViewJob = { onViewJob(application.jobId) },
            onBack = onBack,
            modifier = modifier,
        )

        else -> ApplicationDetailsError(
            error = requireNotNull(error),
            onRetry = viewModel::retry,
            onBack = onBack,
            modifier = modifier,
        )
    }
}

@Composable
private fun ApplicationDetailsContent(
    application: JobApplication,
    onViewJob: () -> Unit,
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
                .widthIn(max = 680.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(JobLinkSpacing.large),
            verticalArrangement = Arrangement.spacedBy(JobLinkSpacing.medium),
        ) {
            TextButton(onClick = onBack) {
                Text(text = stringResource(R.string.back))
            }
            Text(
                text = stringResource(R.string.application_details),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(JobLinkSpacing.small),
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = application.jobTitle.ifBlank {
                            stringResource(R.string.job_title_unavailable)
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = application.companyName.ifBlank {
                            stringResource(R.string.company_not_specified)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                ApplicationStatusBadge(application.status)
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.large,
            ) {
                Column(
                    modifier = Modifier.padding(JobLinkSpacing.large),
                    verticalArrangement = Arrangement.spacedBy(JobLinkSpacing.medium),
                ) {
                    ApplicationDetailRow(
                        label = stringResource(R.string.submitted_date),
                        value = application.submittedAt?.let { timestamp ->
                            DateFormat.getDateTimeInstance(
                                DateFormat.MEDIUM,
                                DateFormat.SHORT,
                            ).format(timestamp.toDate())
                        } ?: stringResource(R.string.submitted_date_pending),
                    )
                    ApplicationDetailRow(
                        label = stringResource(R.string.application_reference),
                        value = application.applicationId,
                    )
                    ApplicationDetailRow(
                        label = stringResource(R.string.cv),
                        value = if (application.cvReference.isBlank()) {
                            stringResource(R.string.cv_reference_unavailable)
                        } else {
                            stringResource(R.string.cv_attached)
                        },
                    )
                }
            }

            Text(
                text = stringResource(R.string.cover_message),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = application.coverMessage?.takeIf(String::isNotBlank)
                    ?: stringResource(R.string.no_cover_message),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(JobLinkSpacing.small))
            OutlinedButton(
                onClick = onViewJob,
                enabled = application.jobId.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(R.string.view_job))
            }
        }
    }
}

@Composable
private fun ApplicationDetailRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(JobLinkSpacing.extraSmall)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ApplicationDetailsLoading(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(JobLinkSpacing.medium),
        ) {
            CircularProgressIndicator()
            Text(text = stringResource(R.string.loading_application_details))
        }
    }
}

@Composable
private fun ApplicationDetailsError(
    error: ApplicationError,
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
            ApplicationListErrorMessage(error)
            if (error != ApplicationError.APPLICATION_NOT_FOUND) {
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
