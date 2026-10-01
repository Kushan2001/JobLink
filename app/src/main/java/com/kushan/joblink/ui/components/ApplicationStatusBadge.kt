package com.kushan.joblink.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.kushan.joblink.R
import com.kushan.joblink.data.model.ApplicationStatus
import com.kushan.joblink.ui.theme.JobLinkSpacing

@Composable
fun ApplicationStatusBadge(
    status: ApplicationStatus,
    modifier: Modifier = Modifier,
) {
    val colors = status.statusColors()
    Surface(
        modifier = modifier,
        color = colors.first,
        contentColor = colors.second,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = stringResource(status.labelResource()),
            modifier = Modifier.padding(
                horizontal = JobLinkSpacing.small,
                vertical = JobLinkSpacing.extraSmall,
            ),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@StringRes
fun ApplicationStatus.labelResource(): Int = when (this) {
    ApplicationStatus.SUBMITTED -> R.string.application_status_submitted
    ApplicationStatus.REVIEWED -> R.string.application_status_reviewed
    ApplicationStatus.SHORTLISTED -> R.string.application_status_shortlisted
    ApplicationStatus.INTERVIEW -> R.string.application_status_interview
    ApplicationStatus.OFFERED -> R.string.application_status_offered
    ApplicationStatus.REJECTED -> R.string.application_status_rejected
    ApplicationStatus.WITHDRAWN -> R.string.application_status_withdrawn
}

@Composable
private fun ApplicationStatus.statusColors(): Pair<Color, Color> = when (this) {
    ApplicationStatus.SUBMITTED,
    ApplicationStatus.REVIEWED,
    -> MaterialTheme.colorScheme.secondaryContainer to
        MaterialTheme.colorScheme.onSecondaryContainer

    ApplicationStatus.SHORTLISTED,
    ApplicationStatus.INTERVIEW,
    ApplicationStatus.OFFERED,
    -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer

    ApplicationStatus.REJECTED -> {
        MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
    }

    ApplicationStatus.WITHDRAWN -> {
        MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
}
