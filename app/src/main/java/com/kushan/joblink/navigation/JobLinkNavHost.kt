package com.kushan.joblink.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.kushan.joblink.data.model.UserRole
import com.kushan.joblink.ui.screens.AuthenticatedHomeScreen
import com.kushan.joblink.ui.screens.AuthenticationLoadingScreen
import com.kushan.joblink.ui.screens.LoginScreen
import com.kushan.joblink.ui.screens.RegisterScreen
import com.kushan.joblink.ui.screens.RoleSelectionScreen
import com.kushan.joblink.ui.screens.WelcomeScreen
import com.kushan.joblink.viewmodel.AuthViewModel

@Composable
fun JobLinkNavHost(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    modifier: Modifier = Modifier,
) {
    val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(authUiState.currentUser) {
        when (authUiState.currentUser?.role) {
            UserRole.JOB_SEEKER -> {
                navController.navigate(JobSeekerHomeDestination) {
                    popUpTo<WelcomeDestination> { inclusive = true }
                    launchSingleTop = true
                }
            }

            UserRole.EMPLOYER -> {
                navController.navigate(EmployerHomeDestination) {
                    popUpTo<WelcomeDestination> { inclusive = true }
                    launchSingleTop = true
                }
            }

            null -> Unit
        }
    }

    NavHost(
        navController = navController,
        startDestination = WelcomeDestination,
        modifier = modifier,
    ) {
        composable<WelcomeDestination> {
            WelcomeScreen(
                onContinue = { navController.navigate(RoleSelectionDestination) },
                onLogin = { navController.navigate(LoginDestination) },
            )
        }

        composable<RoleSelectionDestination> {
            RoleSelectionScreen(
                onContinue = { role ->
                    navController.navigate(RegisterDestination(role = role))
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable<LoginDestination> {
            LoginScreen(
                authViewModel = authViewModel,
                onForgotPassword = {},
                onRegister = {
                    authViewModel.clearError()
                    navController.navigate(RoleSelectionDestination)
                },
                onBack = {
                    authViewModel.clearError()
                    navController.popBackStack()
                },
            )
        }

        composable<RegisterDestination> {
            RegisterScreen(
                authViewModel = authViewModel,
                onLogin = {
                    authViewModel.clearError()
                    navController.navigate(LoginDestination)
                },
                onBack = {
                    authViewModel.clearError()
                    navController.popBackStack()
                },
            )
        }

        composable<JobSeekerHomeDestination> {
            val profile = authUiState.currentUser
            if (profile == null) {
                AuthenticationLoadingScreen()
            } else {
                AuthenticatedHomeScreen(
                    profile = profile,
                    onLogout = {
                        authViewModel.logout()
                        navController.navigate(WelcomeDestination) {
                            popUpTo<JobSeekerHomeDestination> { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
            }
        }

        composable<EmployerHomeDestination> {
            val profile = authUiState.currentUser
            if (profile == null) {
                AuthenticationLoadingScreen()
            } else {
                AuthenticatedHomeScreen(
                    profile = profile,
                    onLogout = {
                        authViewModel.logout()
                        navController.navigate(WelcomeDestination) {
                            popUpTo<EmployerHomeDestination> { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
            }
        }
    }
}
