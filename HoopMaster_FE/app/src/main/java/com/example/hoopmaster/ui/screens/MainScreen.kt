package com.example.hoopmaster.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Đảm bảo tên biến ở đây là onNavigateToTracking
@Composable
fun MainScreen(onNavigateToTracking: () -> Unit) {
    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🏀 HoopMaster", fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Chế độ Testing - Bỏ qua Login")
            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = onNavigateToTracking, // Gọi đúng tên biến ở đây
                modifier = Modifier.fillMaxWidth(0.7f).height(56.dp)
            ) {
                Text("Bắt đầu tập luyện (Test)", fontSize = 18.sp)
            }
        }
    }
}