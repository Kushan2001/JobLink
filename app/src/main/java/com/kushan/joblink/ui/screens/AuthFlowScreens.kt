package com.kushan.joblink.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kushan.joblink.R

@Composable
fun WelcomeScreen(
    onContinue: () -> Unit,
    onLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DestinationScreen(
        title = R.string.welcome_title,
        primaryActionLabel = R.string.get_started,
        onPrimaryAction = onContinue,
        secondaryActionLabel = R.string.login,
        onSecondaryAction = onLogin,
        modifier = modifier,
    )
}

@Composable
fun RoleSelectionScreen(
    onLogin: () -> Unit,
    onRegister: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DestinationScreen(
        title = R.string.role_selection_title,
        primaryActionLabel = R.string.register,
        onPrimaryAction = onRegister,
        secondaryActionLabel = R.string.login,
        onSecondaryAction = onLogin,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
fun LoginScreen(
    onRegister: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DestinationScreen(
        title = R.string.login_title,
        primaryActionLabel = R.string.register_instead,
        onPrimaryAction = onRegister,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
fun RegisterScreen(
    onLogin: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DestinationScreen(
        title = R.string.register_title,
        primaryActionLabel = R.string.login_instead,
        onPrimaryAction = onLogin,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
private fun DestinationScreen(
    @StringRes title: Int,
    @StringRes primaryActionLabel: Int,
    onPrimaryAction: () -> Unit,
    modifier: Modifier = Modifier,
    @StringRes secondaryActionLabel: Int? = null,
    onSecondaryAction: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onPrimaryAction) {
            Text(text = stringResource(primaryActionLabel))
        }

        if (secondaryActionLabel != null && onSecondaryAction != null) {
            TextButton(onClick = onSecondaryAction) {
                Text(text = stringResource(secondaryActionLabel))
            }
        }

        if (onBack != null) {
            TextButton(onClick = onBack) {
                Text(text = stringResource(R.string.back))
            }
        }
    }
}
