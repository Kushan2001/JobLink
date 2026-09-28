package com.kushan.joblink.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.kushan.joblink.ui.screens.LoginScreen
import com.kushan.joblink.ui.screens.RegisterScreen
import com.kushan.joblink.ui.screens.RoleSelectionScreen
import com.kushan.joblink.ui.screens.WelcomeScreen

@Composable
fun JobLinkNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
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
                onForgotPassword = {},
                onRegister = { navController.navigate(RegisterDestination()) },
                onBack = { navController.popBackStack() },
            )
        }

        composable<RegisterDestination> { backStackEntry ->
            val destination = backStackEntry.toRoute<RegisterDestination>()
            RegisterScreen(
                selectedRole = destination.role,
                onLogin = { navController.navigate(LoginDestination) },
                onBack = { navController.popBackStack() },
            )
        }
    }
}
