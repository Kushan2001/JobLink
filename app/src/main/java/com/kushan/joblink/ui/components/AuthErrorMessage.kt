package com.kushan.joblink.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.kushan.joblink.R
import com.kushan.joblink.data.repository.AuthError
import com.kushan.joblink.ui.theme.JobLinkSpacing

@Composable
fun AuthErrorMessage(
    error: AuthError?,
    modifier: Modifier = Modifier,
) {
    if (error == null) return

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Assertive },
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.medium,
    ) {
        Text(
            text = stringResource(error.messageResource()),
            modifier = Modifier.padding(JobLinkSpacing.medium),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@StringRes
private fun AuthError.messageResource(): Int = when (this) {
    AuthError.INVALID_CREDENTIALS -> R.string.auth_error_invalid_credentials
    AuthError.EMAIL_ALREADY_IN_USE -> R.string.auth_error_email_in_use
    AuthError.WEAK_PASSWORD -> R.string.auth_error_weak_password
    AuthError.USER_DISABLED -> R.string.auth_error_user_disabled
    AuthError.TOO_MANY_REQUESTS -> R.string.auth_error_too_many_requests
    AuthError.NETWORK -> R.string.auth_error_network
    AuthError.PROFILE_NOT_FOUND -> R.string.auth_error_profile_not_found
    AuthError.PROFILE_INVALID -> R.string.auth_error_profile_invalid
    AuthError.PROFILE_SAVE_FAILED -> R.string.auth_error_profile_save
    AuthError.PERMISSION_DENIED -> R.string.auth_error_permission_denied
    AuthError.UNKNOWN -> R.string.auth_error_unknown
}
