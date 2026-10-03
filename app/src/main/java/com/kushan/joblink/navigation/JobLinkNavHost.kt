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
import com.kushan.joblink.ui.screens.ApplicationDetailsScreen
import com.kushan.joblink.ui.screens.ApplicationScreen
import com.kushan.joblink.ui.screens.ApplicantDetailsScreen
import com.kushan.joblink.ui.screens.EmployerApplicationsScreen
import com.kushan.joblink.ui.screens.EmployerJobDetailsScreen
import com.kushan.joblink.ui.screens.EmployerHomeScreen
import com.kushan.joblink.ui.screens.EmployerJobsScreen
import com.kushan.joblink.ui.screens.EmployerProfileScreen
import com.kushan.joblink.ui.screens.HomeScreen
import com.kushan.joblink.ui.screens.JobDetailsScreen
import com.kushan.joblink.ui.screens.LoginScreen
import com.kushan.joblink.ui.screens.MyApplicationsScreen
import com.kushan.joblink.ui.screens.PostJobScreen
import com.kushan.joblink.ui.screens.JobSeekerProfileScreen
import com.kushan.joblink.ui.screens.RegisterScreen
import com.kushan.joblink.ui.screens.RoleSelectionScreen
import com.kushan.joblink.ui.screens.SavedJobsScreen
import com.kushan.joblink.ui.screens.WelcomeScreen
import com.kushan.joblink.viewmodel.AuthViewModel
import com.kushan.joblink.viewmodel.ApplicationDetailsViewModel
import com.kushan.joblink.viewmodel.ApplicationViewModel
import com.kushan.joblink.viewmodel.ApplicantDetailsViewModel
import com.kushan.joblink.viewmodel.EmployerApplicationsViewModel
import com.kushan.joblink.viewmodel.EmployerJobDetailsViewModel
import com.kushan.joblink.viewmodel.EmployerHomeViewModel
import com.kushan.joblink.viewmodel.EmployerJobsViewModel
import com.kushan.joblink.viewmodel.EmployerProfileViewModel
import com.kushan.joblink.viewmodel.HomeViewModel
import com.kushan.joblink.viewmodel.JobDetailsViewModel
import com.kushan.joblink.viewmodel.JobSeekerProfileViewModel
import com.kushan.joblink.viewmodel.MyApplicationsViewModel
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
                        factory = HomeViewModel.Factory(
                            jobRepository = jobRepository,
                            jobSeekerProfileRepository = jobSeekerProfileRepository,
                        ),
                    )
                    HomeScreen(
                        viewModel = homeViewModel,
                        userName = profile.fullName,
                        onJobClick = { jobId ->
                            navController.navigate(JobDetailsDestination(jobId))
                        },
                        onApplications = {
                            navController.navigate(MyApplicationsDestination)
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

        composable<MyApplicationsDestination> {
            if (authUiState.currentUser?.role == UserRole.JOB_SEEKER) {
                val myApplicationsViewModel: MyApplicationsViewModel = viewModel(
                    factory = MyApplicationsViewModel.Factory(applicationRepository),
                )
                MyApplicationsScreen(
                    viewModel = myApplicationsViewModel,
                    onApplicationClick = { applicationId ->
                        navController.navigate(ApplicationDetailsDestination(applicationId))
                    },
                    onBack = { navController.popBackStack() },
                )
            } else {
                AuthenticationLoadingScreen()
            }
        }

        composable<ApplicationDetailsDestination> { backStackEntry ->
            if (authUiState.currentUser?.role == UserRole.JOB_SEEKER) {
                val destination = backStackEntry.toRoute<ApplicationDetailsDestination>()
                val detailsViewModel: ApplicationDetailsViewModel = viewModel(
                    factory = ApplicationDetailsViewModel.Factory(
                        applicationId = destination.applicationId,
                        applicationRepository = applicationRepository,
                    ),
                )
                ApplicationDetailsScreen(
                    viewModel = detailsViewModel,
                    onViewJob = { jobId ->
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
            if (profile?.role != UserRole.EMPLOYER) {
                AuthenticationLoadingScreen()
            } else {
                val employerHomeViewModel: EmployerHomeViewModel = viewModel(
                    factory = EmployerHomeViewModel.Factory(
                        employerProfileRepository = employerProfileRepository,
                        jobRepository = jobRepository,
                        applicationRepository = applicationRepository,
                    ),
                )
                EmployerHomeScreen(
                    viewModel = employerHomeViewModel,
                    onPostJob = { navController.navigate(PostJobDestination) },
                    onMyJobs = { navController.navigate(EmployerJobsDestination) },
                    onApplicantClick = { applicationId ->
                        navController.navigate(ApplicantDetailsDestination(applicationId))
                    },
                    onCompanyProfile = {
                        navController.navigate(EmployerProfileDestination)
                    },
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

        composable<EmployerProfileDestination> {
            if (authUiState.currentUser?.role == UserRole.EMPLOYER) {
                val profileViewModel: EmployerProfileViewModel = viewModel(
                    factory = EmployerProfileViewModel.Factory(employerProfileRepository),
                )
                EmployerProfileScreen(
                    viewModel = profileViewModel,
                    onPostJob = { navController.navigate(PostJobDestination) },
                    onMyJobs = { navController.navigate(EmployerJobsDestination) },
                    onBack = { navController.popBackStack() },
                    onLogout = {
                        authViewModel.logout()
                        navController.navigate(WelcomeDestination) {
                            popUpTo<EmployerHomeDestination> { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
            } else {
                AuthenticationLoadingScreen()
            }
        }

        composable<EmployerJobsDestination> {
            if (authUiState.currentUser?.role == UserRole.EMPLOYER) {
                val employerJobsViewModel: EmployerJobsViewModel = viewModel(
                    factory = EmployerJobsViewModel.Factory(jobRepository),
                )
                EmployerJobsScreen(
                    viewModel = employerJobsViewModel,
                    onViewJob = { jobId ->
                        navController.navigate(EmployerJobDetailsDestination(jobId))
                    },
                    onEditJob = { jobId ->
                        navController.navigate(EditJobDestination(jobId))
                    },
                    onPostJob = { navController.navigate(PostJobDestination) },
                    onBack = { navController.popBackStack() },
                )
            } else {
                AuthenticationLoadingScreen()
            }
        }

        composable<EmployerJobDetailsDestination> { backStackEntry ->
            if (authUiState.currentUser?.role == UserRole.EMPLOYER) {
                val destination = backStackEntry.toRoute<EmployerJobDetailsDestination>()
                val detailsViewModel: EmployerJobDetailsViewModel = viewModel(
                    factory = EmployerJobDetailsViewModel.Factory(
                        jobId = destination.jobId,
                        jobRepository = jobRepository,
                    ),
                )
                EmployerJobDetailsScreen(
                    viewModel = detailsViewModel,
                    onEdit = { jobId -> navController.navigate(EditJobDestination(jobId)) },
                    onViewApplicants = { jobId ->
                        navController.navigate(EmployerApplicationsDestination(jobId))
                    },
                    onBack = { navController.popBackStack() },
                )
            } else {
                AuthenticationLoadingScreen()
            }
        }

        composable<EmployerApplicationsDestination> { backStackEntry ->
            if (authUiState.currentUser?.role == UserRole.EMPLOYER) {
                val destination = backStackEntry.toRoute<EmployerApplicationsDestination>()
                val applicationsViewModel: EmployerApplicationsViewModel = viewModel(
                    factory = EmployerApplicationsViewModel.Factory(
                        jobId = destination.jobId,
                        applicationRepository = applicationRepository,
                    ),
                )
                EmployerApplicationsScreen(
                    viewModel = applicationsViewModel,
                    onApplicantClick = { applicationId ->
                        navController.navigate(ApplicantDetailsDestination(applicationId))
                    },
                    onBack = { navController.popBackStack() },
                )
            } else {
                AuthenticationLoadingScreen()
            }
        }

        composable<ApplicantDetailsDestination> { backStackEntry ->
            if (authUiState.currentUser?.role == UserRole.EMPLOYER) {
                val destination = backStackEntry.toRoute<ApplicantDetailsDestination>()
                val applicantDetailsViewModel: ApplicantDetailsViewModel = viewModel(
                    factory = ApplicantDetailsViewModel.Factory(
                        applicationId = destination.applicationId,
                        applicationRepository = applicationRepository,
                    ),
                )
                ApplicantDetailsScreen(
                    viewModel = applicantDetailsViewModel,
                    onBack = { navController.popBackStack() },
                )
            } else {
                AuthenticationLoadingScreen()
            }
        }

        composable<EditJobDestination> { backStackEntry ->
            if (authUiState.currentUser?.role == UserRole.EMPLOYER) {
                val destination = backStackEntry.toRoute<EditJobDestination>()
                val editJobViewModel: PostJobViewModel = viewModel(
                    factory = PostJobViewModel.Factory(
                        jobRepository = jobRepository,
                        jobId = destination.jobId,
                    ),
                )
                PostJobScreen(
                    viewModel = editJobViewModel,
                    onBack = {
                        navController.navigate(EmployerJobsDestination) {
                            popUpTo<EmployerJobsDestination> { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                )
            } else {
                AuthenticationLoadingScreen()
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
