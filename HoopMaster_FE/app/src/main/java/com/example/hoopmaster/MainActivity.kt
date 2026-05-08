package com.example.hoopmaster

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.hoopmaster.ui.navigation.AppNavigation
import com.example.hoopmaster.ui.theme.HoopMasterTheme

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
