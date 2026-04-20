package com.kanban.mobile.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kanban.mobile.core.session.SessionState
import com.kanban.mobile.feature.auth.LoginScreen
import com.kanban.mobile.feature.auth.LoginViewModel
import com.kanban.mobile.feature.auth.MainPlaceholderScreen
import com.kanban.mobile.feature.auth.MainPlaceholderViewModel
import com.kanban.mobile.feature.auth.RegisterScreen
import com.kanban.mobile.feature.auth.RegisterViewModel
import com.kanban.mobile.feature.auth.SplashDestination
import com.kanban.mobile.feature.auth.SplashScreen
import com.kanban.mobile.feature.auth.SplashViewModel
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val mainNavViewModel: MainNavViewModel = hiltViewModel()
    val sessionState by mainNavViewModel.sessionState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        mainNavViewModel.sessionInvalidated.collect {
            navController.navigate(AppRoutes.Login) {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        }
    }

    LaunchedEffect(sessionState, navController.currentBackStackEntry) {
        val route = navController.currentBackStackEntry?.destination?.route ?: return@LaunchedEffect
        if (sessionState is SessionState.Authenticated && route == AppRoutes.Login) {
            navController.navigate(AppRoutes.Main) {
                popUpTo(AppRoutes.Login) { inclusive = true }
            }
        }
    }

    KanbanNavHost(
        navController = navController,
        sessionState = sessionState,
    )
}

@Composable
private fun KanbanNavHost(
    navController: NavHostController,
    sessionState: SessionState,
) {
    NavHost(
        navController = navController,
        startDestination = AppRoutes.Splash,
    ) {
        composable(AppRoutes.Splash) {
            val vm: SplashViewModel = hiltViewModel()
            SplashScreen(
                viewModel = vm,
                onNavigate = { dest ->
                    when (dest) {
                        SplashDestination.Main -> navController.navigate(AppRoutes.Main) {
                            popUpTo(AppRoutes.Splash) { inclusive = true }
                        }
                        SplashDestination.Login -> navController.navigate(AppRoutes.Login) {
                            popUpTo(AppRoutes.Splash) { inclusive = true }
                        }
                    }
                },
            )
        }
        composable(AppRoutes.Login) {
            val vm: LoginViewModel = hiltViewModel()
            LoginScreen(
                viewModel = vm,
                onRegisterClick = { navController.navigate(AppRoutes.Register) },
                onAuthenticated = {
                    navController.navigate(AppRoutes.Main) {
                        popUpTo(AppRoutes.Login) { inclusive = true }
                    }
                },
            )
        }
        composable(AppRoutes.Register) {
            val vm: RegisterViewModel = hiltViewModel()
            RegisterScreen(
                viewModel = vm,
                onLoginClick = { navController.popBackStack() },
                onRegistered = {
                    navController.navigate(AppRoutes.Main) {
                        popUpTo(AppRoutes.Login) { inclusive = true }
                    }
                },
            )
        }
        composable(AppRoutes.Main) {
            val vm: MainPlaceholderViewModel = hiltViewModel()
            val label = (sessionState as? SessionState.Authenticated)?.email
            MainPlaceholderScreen(
                viewModel = vm,
                userLabel = label,
                onLogout = {
                    navController.navigate(AppRoutes.Login) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                },
            )
        }
    }
}
