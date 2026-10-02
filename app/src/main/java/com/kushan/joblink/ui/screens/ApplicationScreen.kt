package com.kushan.joblink.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.kushan.joblink.data.model.ApplicationDraft
import com.kushan.joblink.data.repository.ApplicationError
import com.kushan.joblink.ui.theme.JobLinkSpacing
import com.kushan.joblink.viewmodel.ApplicationViewModel

@Composable
fun ApplicationScreen(
    viewModel: ApplicationViewModel,
    onBack: () -> Unit,
    onProfile: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val submittedApplication = uiState.submittedApplication
    val draft = uiState.draft
    val error = uiState.error

    when {
        uiState.isLoading -> ApplicationLoading(modifier)
        submittedApplication != null -> ApplicationSuccess(
            onDone = onDone,
            modifier = modifier,
        )

        draft != null -> ApplicationForm(
            draft = draft,
            coverMessage = uiState.coverMessage,
            isSubmitting = uiState.isSubmitting,
            error = error,
            onCoverMessageChanged = viewModel::onCoverMessageChanged,
            onSubmit = viewModel::submitApplication,
            onBack = onBack,
            modifier = modifier,
        )

        else -> ApplicationLoadError(
            error = requireNotNull(error),
            onRetry = viewModel::retry,
            onProfile = onProfile,
            onBack = onBack,
            modifier = modifier,
        )
    }
}

@Composable
private fun ApplicationForm(
    draft: ApplicationDraft,
    coverMessage: String,
    isSubmitting: Boolean,
    error: ApplicationError?,
    onCoverMessageChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ApplicationPage(modifier) {
        TextButton(onClick = onBack, enabled = !isSubmitting) {
            Text(text = stringResource(R.string.back))
        }
        Text(
            text = stringResource(R.string.apply_for_job),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = draft.jobTitle,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = draft.companyName,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.height(JobLinkSpacing.large))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = MaterialTheme.shapes.large,
        ) {
            Column(modifier = Modifier.padding(JobLinkSpacing.large)) {
                Text(
                    text = stringResource(R.string.attached_cv),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = draft.cvFileName.ifBlank {
                        stringResource(R.string.uploaded_cv)
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.height(JobLinkSpacing.large))
        OutlinedTextField(
            value = coverMessage,
            onValueChange = onCoverMessageChanged,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSubmitting,
            label = { Text(text = stringResource(R.string.cover_message)) },
            supportingText = { Text(text = stringResource(R.string.cover_message_optional)) },
            minLines = 5,
            maxLines = 10,
        )

        error?.let {
            Spacer(modifier = Modifier.height(JobLinkSpacing.medium))
            ApplicationErrorMessage(error = it)
        }

        Spacer(modifier = Modifier.height(JobLinkSpacing.large))
        Button(
            onClick = onSubmit,
            enabled = !isSubmitting,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                )
            } else {
                Text(text = stringResource(R.string.submit_application))
            }
        }
    }
}

@Composable
private fun ApplicationSuccess(
    onDone: () -> Unit,
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
            modifier = Modifier.widthIn(max = 560.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = MaterialTheme.shapes.large,
        ) {
            Column(
                modifier = Modifier.padding(JobLinkSpacing.extraLarge),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(JobLinkSpacing.medium),
            ) {
                Text(
                    text = stringResource(R.string.application_submitted_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.application_submitted_message),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                    Text(text = stringResource(R.string.done))
                }
            }
        }
    }
}

@Composable
private fun ApplicationLoading(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(JobLinkSpacing.medium),
        ) {
            CircularProgressIndicator()
            Text(text = stringResource(R.string.preparing_application))
        }
    }
}

@Composable
private fun ApplicationLoadError(
    error: ApplicationError,
    onRetry: () -> Unit,
    onProfile: () -> Unit,
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
            ApplicationErrorMessage(error)
            if (error == ApplicationError.CV_REQUIRED) {
                Button(onClick = onProfile, modifier = Modifier.fillMaxWidth()) {
                    Text(text = stringResource(R.string.upload_cv))
                }
            }
            if (error != ApplicationError.ALREADY_APPLIED) {
                TextButton(onClick = onRetry) {
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
private fun ApplicationErrorMessage(error: ApplicationError) {
    val message = when (error) {
        ApplicationError.NOT_AUTHENTICATED -> R.string.application_error_auth
        ApplicationError.PROFILE_NOT_FOUND -> R.string.application_error_profile
        ApplicationError.WRONG_ROLE -> R.string.application_error_role
        ApplicationError.JOB_NOT_FOUND -> R.string.application_error_job
        ApplicationError.APPLICATION_NOT_FOUND -> R.string.application_details_not_found
        ApplicationError.CV_REQUIRED -> R.string.application_error_cv_required
        ApplicationError.CV_NOT_AVAILABLE,
        ApplicationError.CV_DOWNLOAD_FAILED,
        ApplicationError.CV_VIEWER_UNAVAILABLE,
        ApplicationError.INVALID_STATUS,
        -> R.string.application_error_unknown
        ApplicationError.ALREADY_APPLIED -> R.string.application_error_duplicate
        ApplicationError.PERMISSION_DENIED -> R.string.application_error_permission
        ApplicationError.NETWORK -> R.string.application_error_network
        ApplicationError.UNKNOWN -> R.string.application_error_unknown
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
private fun ApplicationPage(
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
