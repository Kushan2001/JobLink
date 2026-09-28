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
import com.kushan.joblink.data.model.JobSeekerProfile
import com.kushan.joblink.data.repository.ProfileError
import com.kushan.joblink.ui.theme.JobLinkSpacing
import com.kushan.joblink.viewmodel.JobSeekerProfileUiState
import com.kushan.joblink.viewmodel.JobSeekerProfileViewModel
import com.kushan.joblink.viewmodel.ProfileValidationError

@Composable
fun JobSeekerProfileScreen(
    viewModel: JobSeekerProfileViewModel,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val savedProfile = uiState.savedProfile

    when {
        uiState.isLoading -> ProfileLoadingContent(modifier = modifier)
        savedProfile == null -> ProfileLoadErrorContent(
            error = uiState.error,
            onRetry = viewModel::loadProfile,
            onLogout = onLogout,
            modifier = modifier,
        )

        uiState.isEditing -> ProfileEditContent(
            uiState = uiState,
            onFullNameChanged = viewModel::onFullNameChanged,
            onProfessionalHeadlineChanged = viewModel::onProfessionalHeadlineChanged,
            onLocationChanged = viewModel::onLocationChanged,
            onPhoneChanged = viewModel::onPhoneChanged,
            onBioChanged = viewModel::onBioChanged,
            onEducationChanged = viewModel::onEducationChanged,
            onExperienceSummaryChanged = viewModel::onExperienceSummaryChanged,
            onSkillsChanged = viewModel::onSkillsChanged,
            onPreferredJobTypesChanged = viewModel::onPreferredJobTypesChanged,
            onSave = viewModel::saveProfile,
            onCancel = viewModel::cancelEditing,
            modifier = modifier,
        )

        else -> ProfileViewContent(
            profile = savedProfile,
            saveSucceeded = uiState.saveSucceeded,
            onEdit = viewModel::startEditing,
            onLogout = onLogout,
            modifier = modifier,
        )
    }
}

@Composable
private fun ProfileViewContent(
    profile: JobSeekerProfile,
    saveSucceeded: Boolean,
    onEdit: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ProfilePage(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.my_profile),
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
                    text = stringResource(R.string.profile_saved),
                    modifier = Modifier.padding(JobLinkSpacing.medium),
                )
            }
        }

        Spacer(modifier = Modifier.height(JobLinkSpacing.large))
        Text(
            text = profile.fullName,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = profile.professionalHeadline.valueOrNotProvided(),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(JobLinkSpacing.large))
        Button(
            onClick = onEdit,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.edit_profile))
        }
        Spacer(modifier = Modifier.height(JobLinkSpacing.extraLarge))

        ProfileSection(
            title = R.string.profile_contact,
            value = listOf(profile.location, profile.phone)
                .filter(String::isNotBlank)
                .joinToString("\n")
                .valueOrNotProvided(),
        )
        ProfileSection(title = R.string.bio, value = profile.bio.valueOrNotProvided())
        ProfileSection(title = R.string.education, value = profile.education.valueOrNotProvided())
        ProfileSection(
            title = R.string.experience_summary,
            value = profile.experienceSummary.valueOrNotProvided(),
        )
        ProfileSection(
            title = R.string.skills,
            value = profile.skills.joinToString(", ").valueOrNotProvided(),
        )
        ProfileSection(
            title = R.string.preferred_job_types,
            value = profile.preferredJobTypes.joinToString(", ").valueOrNotProvided(),
        )
    }
}

@Composable
private fun ProfileEditContent(
    uiState: JobSeekerProfileUiState,
    onFullNameChanged: (String) -> Unit,
    onProfessionalHeadlineChanged: (String) -> Unit,
    onLocationChanged: (String) -> Unit,
    onPhoneChanged: (String) -> Unit,
    onBioChanged: (String) -> Unit,
    onEducationChanged: (String) -> Unit,
    onExperienceSummaryChanged: (String) -> Unit,
    onSkillsChanged: (String) -> Unit,
    onPreferredJobTypesChanged: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ProfilePage(modifier = modifier) {
        Text(
            text = stringResource(R.string.edit_profile),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(JobLinkSpacing.small))
        Text(
            text = stringResource(R.string.profile_edit_subtitle),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
        )

        if (uiState.error != null) {
            Spacer(modifier = Modifier.height(JobLinkSpacing.medium))
            ProfileErrorMessage(error = uiState.error)
        }

        Spacer(modifier = Modifier.height(JobLinkSpacing.extraLarge))
        ProfileTextField(
            value = uiState.fullName,
            onValueChange = onFullNameChanged,
            label = R.string.full_name,
            error = if (ProfileValidationError.FULL_NAME_REQUIRED in uiState.validationErrors) {
                R.string.full_name_required
            } else {
                null
            },
            enabled = !uiState.isSaving,
        )
        ProfileTextField(
            value = uiState.professionalHeadline,
            onValueChange = onProfessionalHeadlineChanged,
            label = R.string.professional_headline,
            error = if (ProfileValidationError.HEADLINE_REQUIRED in uiState.validationErrors) {
                R.string.headline_required
            } else {
                null
            },
            enabled = !uiState.isSaving,
        )
        ProfileTextField(
            value = uiState.location,
            onValueChange = onLocationChanged,
            label = R.string.location,
            error = if (ProfileValidationError.LOCATION_REQUIRED in uiState.validationErrors) {
                R.string.location_required
            } else {
                null
            },
            enabled = !uiState.isSaving,
        )
        ProfileTextField(
            value = uiState.phone,
            onValueChange = onPhoneChanged,
            label = R.string.phone,
            error = if (ProfileValidationError.PHONE_INVALID in uiState.validationErrors) {
                R.string.phone_invalid
            } else {
                null
            },
            enabled = !uiState.isSaving,
            keyboardType = KeyboardType.Phone,
        )
        ProfileTextField(
            value = uiState.bio,
            onValueChange = onBioChanged,
            label = R.string.bio,
            enabled = !uiState.isSaving,
            singleLine = false,
            minLines = 3,
        )
        ProfileTextField(
            value = uiState.education,
            onValueChange = onEducationChanged,
            label = R.string.education,
            enabled = !uiState.isSaving,
            singleLine = false,
            minLines = 2,
        )
        ProfileTextField(
            value = uiState.experienceSummary,
            onValueChange = onExperienceSummaryChanged,
            label = R.string.experience_summary,
            enabled = !uiState.isSaving,
            singleLine = false,
            minLines = 3,
        )
        ProfileTextField(
            value = uiState.skillsInput,
            onValueChange = onSkillsChanged,
            label = R.string.skills,
            error = if (ProfileValidationError.SKILLS_REQUIRED in uiState.validationErrors) {
                R.string.skills_required
            } else {
                null
            },
            supportingText = R.string.comma_separated_hint,
            enabled = !uiState.isSaving,
        )
        ProfileTextField(
            value = uiState.preferredJobTypesInput,
            onValueChange = onPreferredJobTypesChanged,
            label = R.string.preferred_job_types,
            error = if (ProfileValidationError.JOB_TYPES_REQUIRED in uiState.validationErrors) {
                R.string.job_types_required
            } else {
                null
            },
            supportingText = R.string.job_types_hint,
            enabled = !uiState.isSaving,
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
                Text(text = stringResource(R.string.save_profile))
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
private fun ProfilePage(
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
private fun ProfileTextField(
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
private fun ProfileSection(
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
private fun ProfileLoadingContent(
    modifier: Modifier = Modifier,
) {
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
            Text(text = stringResource(R.string.loading_profile))
        }
    }
}

@Composable
private fun ProfileLoadErrorContent(
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
            ProfileErrorMessage(error = error ?: ProfileError.UNKNOWN)
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
private fun ProfileErrorMessage(
    error: ProfileError,
    modifier: Modifier = Modifier,
) {
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
private fun ProfileError.messageResource(): Int = when (this) {
    ProfileError.NOT_AUTHENTICATED -> R.string.profile_error_not_authenticated
    ProfileError.PROFILE_NOT_FOUND -> R.string.profile_error_not_found
    ProfileError.WRONG_ROLE -> R.string.profile_error_wrong_role
    ProfileError.PERMISSION_DENIED -> R.string.profile_error_permission_denied
    ProfileError.NETWORK -> R.string.profile_error_network
    ProfileError.UNKNOWN -> R.string.profile_error_unknown
}

@Composable
private fun String.valueOrNotProvided(): String =
    ifBlank { stringResource(R.string.not_provided) }
