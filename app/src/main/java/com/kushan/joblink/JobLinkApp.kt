package com.kushan.joblink

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.kushan.joblink.navigation.JobLinkNavHost
import com.kushan.joblink.ui.screens.AuthenticationLoadingScreen
import com.kushan.joblink.ui.theme.JobLinkTheme
import com.kushan.joblink.viewmodel.AuthViewModel

@Composable
fun JobLinkApp() {
    JobLinkTheme {
        val application = LocalContext.current.applicationContext as JobLinkApplication
        val authViewModel: AuthViewModel = viewModel(
            factory = AuthViewModel.Factory(application.authRepository),
        )
        val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()
        val navController = rememberNavController()

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            if (authUiState.isInitializing) {
                AuthenticationLoadingScreen()
            } else {
                JobLinkNavHost(
                    navController = navController,
                    authViewModel = authViewModel,
                    applicationRepository = application.applicationRepository,
                    employerProfileRepository = application.employerProfileRepository,
                    jobSeekerProfileRepository = application.jobSeekerProfileRepository,
                    jobRepository = application.jobRepository,
                )
            }
        }
    }
}
