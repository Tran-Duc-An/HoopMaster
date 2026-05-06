package com.example.hoopmaster.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import com.example.hoopmaster.R

// Đảm bảo tên biến ở đây là onNavigateToTracking
@Composable
fun MainScreen(onNavigateToTracking: () -> Unit) {
    val context = LocalContext.current
    val logoId = remember(context) {
        val drawableId = context.resources.getIdentifier("hoopmaster_logo", "drawable", context.packageName)
        if (drawableId != 0) {
            drawableId
        } else {
            context.resources.getIdentifier("hoopmaster_logo_foreground", "mipmap", context.packageName)
        }
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (logoId != 0) {
                Image(
                    painter = painterResource(id = logoId),
                    contentDescription = "HoopMaster logo",
                    modifier = Modifier.size(140.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            Text(
                text = "HOOPMASTER",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Chế độ Testing - Bỏ qua Login",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = onNavigateToTracking, // Gọi đúng tên biến ở đây
                modifier = Modifier.fillMaxWidth(0.8f).height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Text(
                    "Bắt đầu tập luyện (Test)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}