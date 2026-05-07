package com.example.hoopmaster.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hoopmaster.R
import com.example.hoopmaster.viewmodels.AuthUiState
import com.example.hoopmaster.viewmodels.AuthViewModel

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    demoState: AuthUiState? = null,
    demoSignupMode: Boolean? = null,
    viewModel: AuthViewModel = viewModel()
) {
    val isDemo = demoState != null
    var isSignupMode by rememberSaveable { mutableStateOf(demoSignupMode ?: false) }
    var demoEmail by rememberSaveable { mutableStateOf(demoState?.email ?: "") }
    var demoPassword by rememberSaveable { mutableStateOf(demoState?.password ?: "") }
    val logoId = R.drawable.hoopmaster_logo
    val emailValue = if (isDemo) demoEmail else viewModel.email.value
    val passwordValue = if (isDemo) demoPassword else viewModel.password.value
    val loadingValue = if (isDemo) demoState?.isLoading == true else viewModel.isLoading.value
    val errorValue = if (isDemo) demoState?.errorMessage else viewModel.errorMessage.value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = logoId),
            contentDescription = "HoopMaster logo",
            modifier = Modifier.size(120.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "HOOPMASTER",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            "Your AI Basketball Coach",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = { isSignupMode = false }) {
                Text("Log in", fontWeight = if (!isSignupMode) FontWeight.Bold else FontWeight.Normal)
            }
            TextButton(onClick = { isSignupMode = true }) {
                Text("Sign up", fontWeight = if (isSignupMode) FontWeight.Bold else FontWeight.Normal)
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        OutlinedTextField(
            value = emailValue,
            onValueChange = {
                if (isDemo) {
                    demoEmail = it
                } else {
                    viewModel.email.value = it
                }
            },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                cursorColor = MaterialTheme.colorScheme.primary
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = passwordValue,
            onValueChange = {
                if (isDemo) {
                    demoPassword = it
                } else {
                    viewModel.password.value = it
                }
            },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                cursorColor = MaterialTheme.colorScheme.primary
            )
        )

        errorValue?.let { error ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = error, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (loadingValue) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    if (isDemo) {
                        onLoginSuccess()
                    } else if (isSignupMode) {
                        viewModel.signup(onSuccess = onLoginSuccess)
                    } else {
                        viewModel.login(onSuccess = onLoginSuccess)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Text(
                    if (isSignupMode) "SIGN UP" else "LOG IN",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
