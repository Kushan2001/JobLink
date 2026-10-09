package com.kushan.joblink.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kushan.joblink.R
import com.kushan.joblink.data.model.JobApplication
import com.kushan.joblink.data.model.employerApplicationStatuses
import com.kushan.joblink.data.repository.ApplicationError
import com.kushan.joblink.ui.components.ApplicationStatusBadge
import com.kushan.joblink.ui.components.labelResource
import com.kushan.joblink.ui.theme.JobLinkSpacing
import com.kushan.joblink.viewmodel.ApplicantDetailsViewModel
import java.text.DateFormat

@Composable
fun ApplicantDetailsScreen(
    viewModel: ApplicantDetailsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val openCvLabel = stringResource(R.string.open_cv)

    LaunchedEffect(uiState.downloadedCv, openCvLabel) {
        val file = uiState.downloadedCv ?: return@LaunchedEffect
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, openCvLabel))
            viewModel.consumeDownloadedCv()
        } catch (_: ActivityNotFoundException) {
            viewModel.onCvViewerUnavailable()
        } catch (_: IllegalArgumentException) {
            viewModel.onCvViewerUnavailable()
        }
    }

    when {
        uiState.isLoading -> ApplicantListLoading()
        uiState.application != null -> ApplicantDetailsContent(
            application = uiState.application!!,
            isUpdatingStatus = uiState.isUpdatingStatus,
            isDownloadingCv = uiState.isDownloadingCv,
            downloadProgress = uiState.cvDownloadProgress,
            actionError = uiState.actionError,
            onStatusSelected = viewModel::updateStatus,
            onOpenCv = viewModel::downloadCv,
            onBack = onBack,
            modifier = modifier,
        )
        else -> ApplicantDetailsError(
            error = uiState.error ?: ApplicationError.UNKNOWN,
            onRetry = viewModel::retry,
            onBack = onBack,
            modifier = modifier,
        )
    }
}

@Composable
private fun ApplicantDetailsContent(
    application: JobApplication,
    isUpdatingStatus: Boolean,
    isDownloadingCv: Boolean,
    downloadProgress: Float,
    actionError: ApplicationError?,
    onStatusSelected: (com.kushan.joblink.data.model.ApplicationStatus) -> Unit,
    onOpenCv: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize().safeDrawingPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 760.dp).fillMaxWidth()
                .verticalScroll(rememberScrollState()).padding(JobLinkSpacing.large),
        ) {
            TextButton(onClick = onBack, enabled = !isUpdatingStatus && !isDownloadingCv) {
                Text(stringResource(R.string.back))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(JobLinkSpacing.medium),
                verticalAlignment = Alignment.Top,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        application.applicantFullName.ifBlank {
                            stringResource(R.string.applicant_name_unavailable)
                        },
                        modifier = Modifier.semantics { heading() },
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        application.jobTitle.ifBlank {
                            stringResource(R.string.job_title_unavailable)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                ApplicationStatusBadge(application.status)
            }

            Spacer(Modifier.height(JobLinkSpacing.large))
            Text(
                stringResource(R.string.update_application_status),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(JobLinkSpacing.small),
                verticalArrangement = Arrangement.spacedBy(JobLinkSpacing.small),
            ) {
                employerApplicationStatuses.forEach { status ->
                    FilterChip(
                        selected = application.status == status,
                        onClick = { onStatusSelected(status) },
                        enabled = !isUpdatingStatus,
                        label = { Text(stringResource(status.labelResource())) },
                    )
                }
            }
            if (isUpdatingStatus) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            actionError?.let {
                Spacer(Modifier.height(JobLinkSpacing.medium))
                EmployerApplicationErrorMessage(it)
            }

            ApplicantSection(R.string.contact_information) {
                ApplicantDetailRow(R.string.email, application.applicantEmail)
                ApplicantDetailRow(R.string.phone, application.applicantPhone)
                ApplicantDetailRow(R.string.location, application.applicantLocation)
            }
            ApplicantSection(R.string.candidate_profile) {
                ApplicantDetailRow(R.string.professional_headline, application.applicantHeadline)
                ApplicantDetailRow(R.string.bio, application.applicantBio)
                ApplicantDetailRow(R.string.education, application.applicantEducation)
                ApplicantDetailRow(
                    R.string.experience_summary,
                    application.applicantExperienceSummary,
                )
                ApplicantDetailRow(
                    R.string.skills,
                    application.applicantSkills.joinToString(", "),
                )
            }
            ApplicantSection(R.string.application_details) {
                ApplicantDetailRow(
                    R.string.submitted_date,
                    application.submittedAt?.let {
                        DateFormat.getDateInstance(DateFormat.MEDIUM).format(it.toDate())
                    }.orEmpty(),
                )
                ApplicantDetailRow(
                    R.string.cover_message,
                    application.coverMessage.orEmpty(),
                )
                Spacer(Modifier.height(JobLinkSpacing.small))
                Button(
                    onClick = onOpenCv,
                    enabled = !isDownloadingCv && application.cvReference.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (isDownloadingCv) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                        )
                        Text(
                            text = stringResource(
                                R.string.downloading_cv_progress,
                                (downloadProgress * 100).toInt(),
                            ),
                            modifier = Modifier.padding(start = JobLinkSpacing.small),
                        )
                    } else {
                        Text(stringResource(R.string.open_cv))
                    }
                }
            }
        }
    }
}

@Composable
private fun ApplicantSection(@StringRes title: Int, content: @Composable () -> Unit) {
    Spacer(Modifier.height(JobLinkSpacing.extraLarge))
    Text(
        stringResource(title),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
    )
    Spacer(Modifier.height(JobLinkSpacing.small))
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(JobLinkSpacing.large),
            verticalArrangement = Arrangement.spacedBy(JobLinkSpacing.medium),
            content = { content() },
        )
    }
}

@Composable
private fun ApplicantDetailRow(@StringRes label: Int, value: String) {
    Column {
        Text(
            stringResource(label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value.ifBlank { stringResource(R.string.not_provided) })
    }
}

@Composable
private fun ApplicantDetailsError(
    error: ApplicationError,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) = Box(
    modifier = modifier.fillMaxSize().safeDrawingPadding().padding(JobLinkSpacing.large),
    contentAlignment = Alignment.Center,
) {
    Column(
        modifier = Modifier.widthIn(max = 560.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        EmployerApplicationErrorMessage(error)
        Spacer(Modifier.height(JobLinkSpacing.medium))
        if (error != ApplicationError.APPLICATION_NOT_FOUND) {
            Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.retry))
            }
        }
        TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
    }
}

@Composable
internal fun EmployerApplicationErrorMessage(error: ApplicationError) {
    val message = when (error) {
        ApplicationError.NOT_AUTHENTICATED -> R.string.application_error_auth
        ApplicationError.PROFILE_NOT_FOUND -> R.string.employer_application_error_profile
        ApplicationError.WRONG_ROLE -> R.string.employer_application_error_role
        ApplicationError.JOB_NOT_FOUND,
        ApplicationError.APPLICATION_NOT_FOUND -> R.string.employer_application_not_found
        ApplicationError.CV_NOT_AVAILABLE -> R.string.cv_not_available
        ApplicationError.CV_DOWNLOAD_FAILED -> R.string.cv_download_failed
        ApplicationError.CV_VIEWER_UNAVAILABLE -> R.string.cv_viewer_unavailable
        ApplicationError.INVALID_STATUS -> R.string.application_status_invalid
        ApplicationError.PERMISSION_DENIED -> R.string.employer_application_error_permission
        ApplicationError.NETWORK -> R.string.employer_application_error_network
        else -> R.string.employer_application_error_unknown
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.medium,
    ) {
        Text(stringResource(message), Modifier.padding(JobLinkSpacing.medium))
    }
}
