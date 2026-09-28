package com.kushan.joblink.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kushan.joblink.R
import com.kushan.joblink.data.model.CompanyProfile
import com.kushan.joblink.data.repository.ProfileError
import com.kushan.joblink.ui.theme.JobLinkSpacing
import com.kushan.joblink.viewmodel.EmployerProfileUiState
import com.kushan.joblink.viewmodel.EmployerProfileValidationError
import com.kushan.joblink.viewmodel.EmployerProfileViewModel

@Composable
fun EmployerProfileScreen(
    viewModel: EmployerProfileViewModel,
    onPostJob: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val savedProfile = uiState.savedProfile

    when {
        uiState.isLoading -> CompanyProfileLoadingContent(modifier = modifier)
        savedProfile == null -> CompanyProfileLoadErrorContent(
            error = uiState.error,
            onRetry = viewModel::loadProfile,
            onLogout = onLogout,
            modifier = modifier,
        )

        uiState.isEditing -> CompanyProfileEditContent(
            uiState = uiState,
            onCompanyNameChanged = viewModel::onCompanyNameChanged,
            onCompanyDescriptionChanged = viewModel::onCompanyDescriptionChanged,
            onIndustryChanged = viewModel::onIndustryChanged,
            onCompanySizeChanged = viewModel::onCompanySizeChanged,
            onLocationChanged = viewModel::onLocationChanged,
            onWebsiteChanged = viewModel::onWebsiteChanged,
            onContactEmailChanged = viewModel::onContactEmailChanged,
            onSave = viewModel::saveProfile,
            onCancel = viewModel::cancelEditing,
            modifier = modifier,
        )

        else -> CompanyProfileViewContent(
            profile = savedProfile,
            saveSucceeded = uiState.saveSucceeded,
            onEdit = viewModel::startEditing,
            onPostJob = onPostJob,
            onLogout = onLogout,
            modifier = modifier,
        )
    }
}

@Composable
private fun CompanyProfileViewContent(
    profile: CompanyProfile,
    saveSucceeded: Boolean,
    onEdit: () -> Unit,
    onPostJob: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CompanyProfilePage(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.company_profile),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
            TextButton(onClick = onLogout) {
                Text(text = stringResource(R.string.logout))
            }
        }

        if (saveSucceeded) {
            Spacer(modifier = Modifier.height(JobLinkSpacing.medium))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { liveRegion = LiveRegionMode.Polite },
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(
                    text = stringResource(R.string.company_profile_saved),
                    modifier = Modifier.padding(JobLinkSpacing.medium),
                )
            }
        }

        Spacer(modifier = Modifier.height(JobLinkSpacing.large))
        Text(
            text = profile.companyName.companyValueOrNotProvided(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = profile.industry.companyValueOrNotProvided(),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(JobLinkSpacing.large))
        Button(
            onClick = onPostJob,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.post_job))
        }
        Spacer(modifier = Modifier.height(JobLinkSpacing.small))
        OutlinedButton(
            onClick = onEdit,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.edit_company_profile))
        }
        Spacer(modifier = Modifier.height(JobLinkSpacing.extraLarge))

        CompanyProfileSection(
            title = R.string.company_description,
            value = profile.companyDescription.companyValueOrNotProvided(),
        )
        CompanyProfileSection(
            title = R.string.company_details,
            value = listOf(profile.companySize, profile.location)
                .filter(String::isNotBlank)
                .joinToString("\n")
                .companyValueOrNotProvided(),
        )
        CompanyProfileSection(
            title = R.string.website,
            value = profile.website.companyValueOrNotProvided(),
        )
        CompanyProfileSection(
            title = R.string.contact_email,
            value = profile.contactEmail.companyValueOrNotProvided(),
        )
    }
}

@Composable
private fun CompanyProfileEditContent(
    uiState: EmployerProfileUiState,
    onCompanyNameChanged: (String) -> Unit,
    onCompanyDescriptionChanged: (String) -> Unit,
    onIndustryChanged: (String) -> Unit,
    onCompanySizeChanged: (String) -> Unit,
    onLocationChanged: (String) -> Unit,
    onWebsiteChanged: (String) -> Unit,
    onContactEmailChanged: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CompanyProfilePage(modifier = modifier) {
        Text(
            text = stringResource(R.string.edit_company_profile),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(JobLinkSpacing.small))
        Text(
            text = stringResource(R.string.company_profile_edit_subtitle),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
        )

        if (uiState.error != null) {
            Spacer(modifier = Modifier.height(JobLinkSpacing.medium))
            CompanyProfileErrorMessage(error = uiState.error)
        }

        Spacer(modifier = Modifier.height(JobLinkSpacing.extraLarge))
        CompanyProfileTextField(
            value = uiState.companyName,
            onValueChange = onCompanyNameChanged,
            label = R.string.company_name,
            error = if (
                EmployerProfileValidationError.COMPANY_NAME_REQUIRED in
                uiState.validationErrors
            ) {
                R.string.company_name_required
            } else {
                null
            },
            enabled = !uiState.isSaving,
        )
        CompanyProfileTextField(
            value = uiState.companyDescription,
            onValueChange = onCompanyDescriptionChanged,
            label = R.string.company_description,
            error = if (
                EmployerProfileValidationError.DESCRIPTION_REQUIRED in
                uiState.validationErrors
            ) {
                R.string.company_description_required
            } else {
                null
            },
            enabled = !uiState.isSaving,
            singleLine = false,
            minLines = 4,
        )
        CompanyProfileTextField(
            value = uiState.industry,
            onValueChange = onIndustryChanged,
            label = R.string.industry,
            error = if (
                EmployerProfileValidationError.INDUSTRY_REQUIRED in uiState.validationErrors
            ) {
                R.string.industry_required
            } else {
                null
            },
            enabled = !uiState.isSaving,
        )
        CompanyProfileTextField(
            value = uiState.companySize,
            onValueChange = onCompanySizeChanged,
            label = R.string.company_size,
            error = if (
                EmployerProfileValidationError.COMPANY_SIZE_REQUIRED in
                uiState.validationErrors
            ) {
                R.string.company_size_required
            } else {
                null
            },
            supportingText = R.string.company_size_hint,
            enabled = !uiState.isSaving,
        )
        CompanyProfileTextField(
            value = uiState.location,
            onValueChange = onLocationChanged,
            label = R.string.location,
            error = if (
                EmployerProfileValidationError.LOCATION_REQUIRED in uiState.validationErrors
            ) {
                R.string.location_required
            } else {
                null
            },
            enabled = !uiState.isSaving,
        )
        CompanyProfileTextField(
            value = uiState.website,
            onValueChange = onWebsiteChanged,
            label = R.string.website,
            error = if (
                EmployerProfileValidationError.WEBSITE_INVALID in uiState.validationErrors
            ) {
                R.string.website_invalid
            } else {
                null
            },
            supportingText = R.string.website_optional,
            enabled = !uiState.isSaving,
            keyboardType = KeyboardType.Uri,
        )
        CompanyProfileTextField(
            value = uiState.contactEmail,
            onValueChange = onContactEmailChanged,
            label = R.string.contact_email,
            error = when {
                EmployerProfileValidationError.CONTACT_EMAIL_REQUIRED in
                    uiState.validationErrors -> R.string.contact_email_required

                EmployerProfileValidationError.CONTACT_EMAIL_INVALID in
                    uiState.validationErrors -> R.string.email_invalid

                else -> null
            },
            enabled = !uiState.isSaving,
            keyboardType = KeyboardType.Email,
        )

        Spacer(modifier = Modifier.height(JobLinkSpacing.small))
        Button(
            onClick = onSave,
            enabled = !uiState.isSaving,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
        ) {
            if (uiState.isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                )
            } else {
                Text(text = stringResource(R.string.save_company_profile))
            }
        }
        Spacer(modifier = Modifier.height(JobLinkSpacing.small))
        OutlinedButton(
            onClick = onCancel,
            enabled = !uiState.isSaving,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.cancel))
        }
    }
}

@Composable
private fun CompanyProfilePage(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
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
            content = content,
        )
    }
}

@Composable
private fun CompanyProfileTextField(
    value: String,
    onValueChange: (String) -> Unit,
    @StringRes label: Int,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    @StringRes error: Int? = null,
    @StringRes supportingText: Int? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    minLines: Int = 1,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        label = { Text(text = stringResource(label)) },
        supportingText = when {
            error != null -> {
                { Text(text = stringResource(error)) }
            }

            supportingText != null -> {
                { Text(text = stringResource(supportingText)) }
            }

            else -> null
        },
        isError = error != null,
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
    )
    Spacer(modifier = Modifier.height(JobLinkSpacing.medium))
}

@Composable
private fun CompanyProfileSection(
    @StringRes title: Int,
    value: String,
) {
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
            Text(
                text = value,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
    Spacer(modifier = Modifier.height(JobLinkSpacing.medium))
}

@Composable
private fun CompanyProfileLoadingContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(JobLinkSpacing.medium),
        ) {
            CircularProgressIndicator()
            Text(text = stringResource(R.string.loading_company_profile))
        }
    }
}

@Composable
private fun CompanyProfileLoadErrorContent(
    error: ProfileError?,
    onRetry: () -> Unit,
    onLogout: () -> Unit,
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
            CompanyProfileErrorMessage(error = error ?: ProfileError.UNKNOWN)
            Spacer(modifier = Modifier.height(JobLinkSpacing.large))
            Button(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(R.string.retry))
            }
            TextButton(onClick = onLogout) {
                Text(text = stringResource(R.string.logout))
            }
        }
    }
}

@Composable
private fun CompanyProfileErrorMessage(
    error: ProfileError,
    modifier: Modifier = Modifier,
) {
    val message = when (error) {
        ProfileError.NOT_AUTHENTICATED -> R.string.profile_error_not_authenticated
        ProfileError.PROFILE_NOT_FOUND -> R.string.company_profile_error_account_missing
        ProfileError.WRONG_ROLE -> R.string.company_profile_error_wrong_role
        ProfileError.PERMISSION_DENIED -> R.string.company_profile_error_permission_denied
        ProfileError.NETWORK -> R.string.company_profile_error_network
        ProfileError.UNKNOWN -> R.string.company_profile_error_unknown
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Assertive },
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.medium,
    ) {
        Text(
            text = stringResource(message),
            modifier = Modifier.padding(JobLinkSpacing.medium),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun String.companyValueOrNotProvided(): String =
    ifBlank { stringResource(R.string.not_provided) }
