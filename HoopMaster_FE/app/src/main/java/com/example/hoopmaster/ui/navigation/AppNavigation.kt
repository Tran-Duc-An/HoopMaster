package com.example.hoopmaster.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.hoopmaster.core.di.AppContainer
import com.example.hoopmaster.ui.screens.ExerciseDetailScreen
import com.example.hoopmaster.ui.screens.HomeScreen
import com.example.hoopmaster.ui.screens.LoginScreen
import com.example.hoopmaster.ui.screens.PlanningChatScreen
import com.example.hoopmaster.ui.screens.ProfileScreen
import com.example.hoopmaster.ui.screens.SessionSummaryScreen
import com.example.hoopmaster.ui.screens.TrackingScreen

@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val container = remember(context) {
        AppContainer(context.applicationContext)
    }
    val navController = rememberNavController()
    val startDestination = if (container.sessionStore.getUserId() != null) {
        Routes.Home
    } else {
        Routes.Login
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.Login) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.Home) {
                        popUpTo(Routes.Login) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Routes.Planning) {
            PlanningChatScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.Home) {
            HomeScreen(
                onPersonalizePlan = { navController.navigate(Routes.Planning) },
                onStartShooting = { navController.navigate(Routes.Tracking) },
                onOpenExercise = { exerciseId ->
                    navController.navigate(Routes.exerciseDetail(exerciseId))
                },
                onOpenProfile = { navController.navigate(Routes.Profile) }
            )
        }

        composable(
            route = Routes.ExerciseDetail,
            arguments = listOf(navArgument(Routes.ExerciseDetailArg) { type = NavType.IntType })
        ) { entry ->
            val exerciseId = entry.arguments?.getInt(Routes.ExerciseDetailArg) ?: return@composable
            ExerciseDetailScreen(
                exerciseId = exerciseId,
                onBack = { navController.popBackStack() },
                onStartTracking = { navController.navigate(Routes.Tracking) }
            )
        }

        composable(Routes.Tracking) {
            TrackingScreen(
                onEndSession = {
                    navController.navigate(Routes.Summary) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Routes.Summary) {
            SessionSummaryScreen(
                onBackHome = {
                    navController.navigate(Routes.Home) {
                        popUpTo(Routes.Home) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Routes.Profile) {
            ProfileScreen(
                onBack = { navController.popBackStack() },
                onLogout = {
                    navController.navigate(Routes.Login) {
                        popUpTo(Routes.Home) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}
