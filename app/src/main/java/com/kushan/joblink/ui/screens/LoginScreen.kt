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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
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
import com.kushan.joblink.data.repository.AuthError
import com.kushan.joblink.ui.components.AuthErrorMessage
import com.kushan.joblink.ui.components.PasswordVisibilityButton
import com.kushan.joblink.ui.theme.JobLinkSpacing
import com.kushan.joblink.viewmodel.AuthViewModel
import com.kushan.joblink.viewmodel.EmailValidationError
import com.kushan.joblink.viewmodel.LoginUiState
import com.kushan.joblink.viewmodel.LoginViewModel
import com.kushan.joblink.viewmodel.PasswordValidationError

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onForgotPassword: () -> Unit,
    onRegister: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()

    LoginContent(
        uiState = uiState,
        isLoading = authUiState.isLoading,
        authError = authUiState.error,
        onEmailChanged = { email ->
            authViewModel.clearError()
            viewModel.onEmailChanged(email)
        },
        onPasswordChanged = { password ->
            authViewModel.clearError()
            viewModel.onPasswordChanged(password)
        },
        onPasswordVisibilityChanged = viewModel::onPasswordVisibilityChanged,
        onLogin = {
            if (viewModel.validateLoginInput()) {
                authViewModel.login(
                    email = uiState.email.trim(),
                    password = uiState.password,
                )
            }
        },
        onForgotPassword = onForgotPassword,
        onRegister = onRegister,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
private fun LoginContent(
    uiState: LoginUiState,
    isLoading: Boolean,
    authError: AuthError?,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onPasswordVisibilityChanged: () -> Unit,
    onLogin: () -> Unit,
    onForgotPassword: () -> Unit,
    onRegister: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val emailErrorText = when (uiState.emailError) {
        EmailValidationError.REQUIRED -> stringResource(R.string.email_required)
        EmailValidationError.INVALID_FORMAT -> stringResource(R.string.email_invalid)
        null -> null
    }
    val passwordErrorText = when (uiState.passwordError) {
        PasswordValidationError.REQUIRED -> stringResource(R.string.password_required)
        null -> null
    }
    val passwordToggleDescription = stringResource(
        if (uiState.isPasswordVisible) R.string.hide_password else R.string.show_password,
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
                enabled = !isLoading,
            ) {
                Text(text = stringResource(R.string.back))
            }
            Spacer(modifier = Modifier.height(JobLinkSpacing.large))
            Text(
                text = stringResource(R.string.login_title),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(JobLinkSpacing.small))
            Text(
                text = stringResource(R.string.login_subtitle),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
            )
            if (authError != null) {
                Spacer(modifier = Modifier.height(JobLinkSpacing.medium))
                AuthErrorMessage(error = authError)
            }
            Spacer(modifier = Modifier.height(JobLinkSpacing.extraLarge))
            OutlinedTextField(
                value = uiState.email,
                onValueChange = onEmailChanged,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                label = { Text(text = stringResource(R.string.email)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                    )
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
                enabled = !isLoading,
                label = { Text(text = stringResource(R.string.password)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                    )
                },
                trailingIcon = {
                    PasswordVisibilityButton(
                        passwordVisible = uiState.isPasswordVisible,
                        onClick = onPasswordVisibilityChanged,
                        contentDescription = passwordToggleDescription,
                        enabled = !isLoading,
                    )
                },
                supportingText = passwordErrorText?.let { message ->
                    { Text(text = message) }
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
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        onLogin()
                    },
                ),
            )
            TextButton(
                onClick = onForgotPassword,
                enabled = !isLoading,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(text = stringResource(R.string.forgot_password))
            }
            Spacer(modifier = Modifier.height(JobLinkSpacing.large))
            Button(
                onClick = {
                    focusManager.clearFocus()
                    onLogin()
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(text = stringResource(R.string.login))
                }
            }
            Spacer(modifier = Modifier.height(JobLinkSpacing.medium))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.create_account_prompt),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
                TextButton(
                    onClick = onRegister,
                    enabled = !isLoading,
                ) {
                    Text(text = stringResource(R.string.create_account))
                }
            }
        }
    }
}
