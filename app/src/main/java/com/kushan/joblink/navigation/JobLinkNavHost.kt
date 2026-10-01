package com.kushan.joblink.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.kushan.joblink.data.model.UserRole
import com.kushan.joblink.data.repository.ApplicationRepository
import com.kushan.joblink.data.repository.EmployerProfileRepository
import com.kushan.joblink.data.repository.JobSeekerProfileRepository
import com.kushan.joblink.data.repository.JobRepository
import com.kushan.joblink.ui.screens.AuthenticationLoadingScreen
import com.kushan.joblink.ui.screens.ApplicationScreen
import com.kushan.joblink.ui.screens.EmployerProfileScreen
import com.kushan.joblink.ui.screens.HomeScreen
import com.kushan.joblink.ui.screens.JobDetailsScreen
import com.kushan.joblink.ui.screens.LoginScreen
import com.kushan.joblink.ui.screens.PostJobScreen
import com.kushan.joblink.ui.screens.JobSeekerProfileScreen
import com.kushan.joblink.ui.screens.RegisterScreen
import com.kushan.joblink.ui.screens.RoleSelectionScreen
import com.kushan.joblink.ui.screens.SavedJobsScreen
import com.kushan.joblink.ui.screens.WelcomeScreen
import com.kushan.joblink.viewmodel.AuthViewModel
import com.kushan.joblink.viewmodel.ApplicationViewModel
import com.kushan.joblink.viewmodel.EmployerProfileViewModel
import com.kushan.joblink.viewmodel.HomeViewModel
import com.kushan.joblink.viewmodel.JobDetailsViewModel
import com.kushan.joblink.viewmodel.JobSeekerProfileViewModel
import com.kushan.joblink.viewmodel.PostJobViewModel
import com.kushan.joblink.viewmodel.SavedJobsViewModel

@Composable
fun JobLinkNavHost(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    applicationRepository: ApplicationRepository,
    employerProfileRepository: EmployerProfileRepository,
    jobSeekerProfileRepository: JobSeekerProfileRepository,
    jobRepository: JobRepository,
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
            when (profile?.role) {
                UserRole.JOB_SEEKER -> {
                    val homeViewModel: HomeViewModel = viewModel(
                        factory = HomeViewModel.Factory(jobRepository),
                    )
                    HomeScreen(
                        viewModel = homeViewModel,
                        onJobClick = { jobId ->
                            navController.navigate(JobDetailsDestination(jobId))
                        },
                        onSavedJobs = { navController.navigate(SavedJobsDestination) },
                        onProfile = { navController.navigate(JobSeekerProfileDestination) },
                        onLogout = {
                            authViewModel.logout()
                            navController.navigate(WelcomeDestination) {
                                popUpTo<JobSeekerHomeDestination> { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                    )
                }

                else -> AuthenticationLoadingScreen()
            }
        }

        composable<SavedJobsDestination> {
            if (authUiState.currentUser?.role == UserRole.JOB_SEEKER) {
                val savedJobsViewModel: SavedJobsViewModel = viewModel(
                    factory = SavedJobsViewModel.Factory(jobRepository),
                )
                SavedJobsScreen(
                    viewModel = savedJobsViewModel,
                    onJobClick = { jobId ->
                        navController.navigate(JobDetailsDestination(jobId))
                    },
                    onBack = { navController.popBackStack() },
                )
            } else {
                AuthenticationLoadingScreen()
            }
        }

        composable<JobSeekerProfileDestination> {
            if (authUiState.currentUser?.role == UserRole.JOB_SEEKER) {
                val profileViewModel: JobSeekerProfileViewModel = viewModel(
                    factory = JobSeekerProfileViewModel.Factory(jobSeekerProfileRepository),
                )
                JobSeekerProfileScreen(
                    viewModel = profileViewModel,
                    onLogout = {
                        authViewModel.logout()
                        navController.navigate(WelcomeDestination) {
                            popUpTo<JobSeekerHomeDestination> { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
            } else {
                AuthenticationLoadingScreen()
            }
        }

        composable<JobDetailsDestination> { backStackEntry ->
            if (authUiState.currentUser?.role == UserRole.JOB_SEEKER) {
                val destination = backStackEntry.toRoute<JobDetailsDestination>()
                val detailsViewModel: JobDetailsViewModel = viewModel(
                    factory = JobDetailsViewModel.Factory(
                        jobId = destination.jobId,
                        jobRepository = jobRepository,
                    ),
                )
                JobDetailsScreen(
                    viewModel = detailsViewModel,
                    onApplyNow = { jobId ->
                        navController.navigate(ApplicationDestination(jobId))
                    },
                    onBack = { navController.popBackStack() },
                )
            } else {
                AuthenticationLoadingScreen()
            }
        }

        composable<ApplicationDestination> { backStackEntry ->
            if (authUiState.currentUser?.role == UserRole.JOB_SEEKER) {
                val destination = backStackEntry.toRoute<ApplicationDestination>()
                val applicationViewModel: ApplicationViewModel = viewModel(
                    factory = ApplicationViewModel.Factory(
                        jobId = destination.jobId,
                        applicationRepository = applicationRepository,
                    ),
                )
                ApplicationScreen(
                    viewModel = applicationViewModel,
                    onBack = { navController.popBackStack() },
                    onProfile = { navController.navigate(JobSeekerProfileDestination) },
                    onDone = { navController.popBackStack() },
                )
            } else {
                AuthenticationLoadingScreen()
            }
        }

        composable<EmployerHomeDestination> {
            val profile = authUiState.currentUser
            if (profile == null) {
                AuthenticationLoadingScreen()
            } else {
                val profileViewModel: EmployerProfileViewModel = viewModel(
                    factory = EmployerProfileViewModel.Factory(employerProfileRepository),
                )
                EmployerProfileScreen(
                    viewModel = profileViewModel,
                    onPostJob = { navController.navigate(PostJobDestination) },
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

        composable<PostJobDestination> {
            when (authUiState.currentUser?.role) {
                UserRole.EMPLOYER -> {
                    val postJobViewModel: PostJobViewModel = viewModel(
                        factory = PostJobViewModel.Factory(jobRepository),
                    )
                    PostJobScreen(
                        viewModel = postJobViewModel,
                        onBack = { navController.popBackStack() },
                    )
                }

                UserRole.JOB_SEEKER -> {
                    LaunchedEffect(Unit) {
                        navController.navigate(JobSeekerHomeDestination) {
                            popUpTo<PostJobDestination> { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                    AuthenticationLoadingScreen()
                }

                null -> {
                    LaunchedEffect(Unit) {
                        navController.navigate(WelcomeDestination) {
                            popUpTo<PostJobDestination> { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                    AuthenticationLoadingScreen()
                }
            }
        }
    }
}
