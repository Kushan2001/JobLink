package com.kushan.joblink.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kushan.joblink.R
import com.kushan.joblink.data.model.UserRole
import com.kushan.joblink.ui.components.PasswordVisibilityButton
import com.kushan.joblink.ui.theme.JobLinkSpacing
import com.kushan.joblink.viewmodel.ConfirmPasswordValidationError
import com.kushan.joblink.viewmodel.FullNameValidationError
import com.kushan.joblink.viewmodel.RegisterUiState
import com.kushan.joblink.viewmodel.RegisterViewModel
import com.kushan.joblink.viewmodel.RegistrationEmailValidationError
import com.kushan.joblink.viewmodel.RegistrationPasswordValidationError

@Composable
fun RegisterScreen(
    onLogin: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RegisterViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    RegisterContent(
        uiState = uiState,
        onFullNameChanged = viewModel::onFullNameChanged,
        onEmailChanged = viewModel::onEmailChanged,
        onPasswordChanged = viewModel::onPasswordChanged,
        onConfirmPasswordChanged = viewModel::onConfirmPasswordChanged,
        onPasswordVisibilityChanged = viewModel::onPasswordVisibilityChanged,
        onConfirmPasswordVisibilityChanged = viewModel::onConfirmPasswordVisibilityChanged,
        onRegister = { viewModel.validateRegistrationInput() },
        onLogin = onLogin,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
private fun RegisterContent(
    uiState: RegisterUiState,
    onFullNameChanged: (String) -> Unit,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onConfirmPasswordChanged: (String) -> Unit,
    onPasswordVisibilityChanged: () -> Unit,
    onConfirmPasswordVisibilityChanged: () -> Unit,
    onRegister: () -> Unit,
    onLogin: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val fullNameErrorText = when (uiState.fullNameError) {
        FullNameValidationError.REQUIRED -> stringResource(R.string.full_name_required)
        null -> null
    }
    val emailErrorText = when (uiState.emailError) {
        RegistrationEmailValidationError.REQUIRED -> stringResource(R.string.email_required)
        RegistrationEmailValidationError.INVALID_FORMAT -> stringResource(R.string.email_invalid)
        null -> null
    }
    val passwordErrorText = when (uiState.passwordError) {
        RegistrationPasswordValidationError.REQUIRED -> stringResource(R.string.password_required)
        RegistrationPasswordValidationError.TOO_SHORT -> stringResource(R.string.password_too_short)
        null -> null
    }
    val confirmPasswordErrorText = when (uiState.confirmPasswordError) {
        ConfirmPasswordValidationError.REQUIRED -> stringResource(R.string.confirm_password_required)
        ConfirmPasswordValidationError.DOES_NOT_MATCH -> stringResource(R.string.passwords_do_not_match)
        null -> null
    }
    val passwordToggleDescription = stringResource(
        if (uiState.isPasswordVisible) R.string.hide_password else R.string.show_password,
    )
    val confirmPasswordToggleDescription = stringResource(
        if (uiState.isConfirmPasswordVisible) R.string.hide_password else R.string.show_password,
    )
    val roleLabel = stringResource(
        when (uiState.role) {
            UserRole.JOB_SEEKER -> R.string.job_seeker
            UserRole.EMPLOYER -> R.string.employer
        },
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = JobLinkSpacing.large, vertical = JobLinkSpacing.medium),
        ) {
            TextButton(
                onClick = onBack,
                enabled = !uiState.isLoading,
            ) {
                Text(text = stringResource(R.string.back))
            }
            Spacer(modifier = Modifier.height(JobLinkSpacing.large))
            Text(
                text = stringResource(R.string.register_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(JobLinkSpacing.small))
            Text(
                text = stringResource(R.string.register_subtitle),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(modifier = Modifier.height(JobLinkSpacing.medium))
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(
                    text = stringResource(R.string.registering_as, roleLabel),
                    modifier = Modifier.padding(
                        horizontal = JobLinkSpacing.medium,
                        vertical = JobLinkSpacing.small,
                    ),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Spacer(modifier = Modifier.height(JobLinkSpacing.extraLarge))
            OutlinedTextField(
                value = uiState.fullName,
                onValueChange = onFullNameChanged,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading,
                label = { Text(text = stringResource(R.string.full_name)) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Person, contentDescription = null)
                },
                supportingText = fullNameErrorText?.let { message ->
                    { Text(text = message) }
                },
                isError = fullNameErrorText != null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                ),
            )
            Spacer(modifier = Modifier.height(JobLinkSpacing.medium))
            OutlinedTextField(
                value = uiState.email,
                onValueChange = onEmailChanged,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading,
                label = { Text(text = stringResource(R.string.email)) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Email, contentDescription = null)
                },
                supportingText = emailErrorText?.let { message ->
                    { Text(text = message) }
                },
                isError = emailErrorText != null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                ),
            )
            Spacer(modifier = Modifier.height(JobLinkSpacing.medium))
            OutlinedTextField(
                value = uiState.password,
                onValueChange = onPasswordChanged,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading,
                label = { Text(text = stringResource(R.string.password)) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = null)
                },
                trailingIcon = {
                    PasswordVisibilityButton(
                        passwordVisible = uiState.isPasswordVisible,
                        onClick = onPasswordVisibilityChanged,
                        contentDescription = passwordToggleDescription,
                        enabled = !uiState.isLoading,
                    )
                },
                supportingText = {
                    Text(
                        text = passwordErrorText
                            ?: stringResource(R.string.password_requirement),
                    )
                },
                isError = passwordErrorText != null,
                singleLine = true,
                visualTransformation = if (uiState.isPasswordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                ),
            )
            Spacer(modifier = Modifier.height(JobLinkSpacing.medium))
            OutlinedTextField(
                value = uiState.confirmPassword,
                onValueChange = onConfirmPasswordChanged,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading,
                label = { Text(text = stringResource(R.string.confirm_password)) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = null)
                },
                trailingIcon = {
                    PasswordVisibilityButton(
                        passwordVisible = uiState.isConfirmPasswordVisible,
                        onClick = onConfirmPasswordVisibilityChanged,
                        contentDescription = confirmPasswordToggleDescription,
                        enabled = !uiState.isLoading,
                    )
                },
                supportingText = confirmPasswordErrorText?.let { message ->
                    { Text(text = message) }
                },
                isError = confirmPasswordErrorText != null,
                singleLine = true,
                visualTransformation = if (uiState.isConfirmPasswordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        onRegister()
                    },
                ),
            )
            Spacer(modifier = Modifier.height(JobLinkSpacing.large))
            Button(
                onClick = {
                    focusManager.clearFocus()
                    onRegister()
                },
                enabled = !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(text = stringResource(R.string.create_account))
                }
            }
            Spacer(modifier = Modifier.height(JobLinkSpacing.medium))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.existing_account_prompt),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
                TextButton(
                    onClick = onLogin,
                    enabled = !uiState.isLoading,
                ) {
                    Text(text = stringResource(R.string.login))
                }
            }
        }
    }
}
