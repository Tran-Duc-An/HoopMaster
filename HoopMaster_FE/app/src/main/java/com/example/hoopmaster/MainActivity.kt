package com.example.hoopmaster

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.hoopmaster.ui.theme.HoopMasterTheme
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.hoopmaster.ui.screens.MainScreen
import com.example.hoopmaster.ui.screens.TrackingScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HoopMasterTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "main") {
        composable("main") {
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