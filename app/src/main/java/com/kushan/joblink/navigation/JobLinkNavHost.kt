package com.kushan.joblink.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
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
                onLogin = { navController.navigate(LoginDestination) },
                onRegister = { navController.navigate(RegisterDestination) },
                onBack = { navController.popBackStack() },
            )
        }

        composable<LoginDestination> {
            LoginScreen(
                onRegister = { navController.navigate(RegisterDestination) },
                onBack = { navController.popBackStack() },
            )
        }

        composable<RegisterDestination> {
            RegisterScreen(
                onLogin = { navController.navigate(LoginDestination) },
                onBack = { navController.popBackStack() },
            )
        }
    }
}
