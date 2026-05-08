package com.example.hoopmaster.ui.navigation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.hoopmaster.network.SessionManager
import com.example.hoopmaster.ui.screens.LoginScreen
import com.example.hoopmaster.ui.screens.MainScreen
import com.example.hoopmaster.ui.screens.TrackingScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "main") {

        composable("main") {
            // Sửa lại thành onNavigateToTracking, không dùng onStartNewSession nữa
            MainScreen(
                onNavigateToTracking = {
                    navController.navigate("tracking")
                }
            )
        }

        composable("tracking") {
            TrackingScreen(
                onEndSession = {
                    navController.popBackStack()
                }
            )
        }
    }
}