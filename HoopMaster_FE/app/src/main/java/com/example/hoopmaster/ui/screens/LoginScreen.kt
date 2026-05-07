@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.hoopmaster.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hoopmaster.R
import com.example.hoopmaster.ui.components.HoopErrorBanner
import com.example.hoopmaster.ui.components.HoopPrimaryButton
import com.example.hoopmaster.ui.components.HoopOutlinedTextField
import com.example.hoopmaster.ui.theme.HoopSpacing
import com.example.hoopmaster.viewmodels.AuthViewModel

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var isSignupMode by rememberSaveable { mutableStateOf(false) }
    val logoId = R.drawable.hoopmaster_logo
    val emailValue = viewModel.email.value
    val passwordValue = viewModel.password.value
    val loadingValue = viewModel.isLoading.value
    val errorValue = viewModel.errorMessage.value

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(HoopSpacing.Lg)
            ) {
                Image(
                    painter = painterResource(id = logoId),
                    contentDescription = "HoopMaster logo",
                    modifier = Modifier.size(88.dp),
                    contentScale = ContentScale.Fit
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(HoopSpacing.Sm)
                ) {
                    Text(
                        text = "HOOPMASTER",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Basketball training that starts with one clean sign-in.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SegmentedButton(
                        selected = !isSignupMode,
                        onClick = { isSignupMode = false },
                        shape = androidx.compose.material3.SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        icon = {
                            if (!isSignupMode) {
                                androidx.compose.material3.Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = null
                                )
                            }
                        }
                    ) {
                        Text("Log in")
                    }
                    SegmentedButton(
                        selected = isSignupMode,
                        onClick = { isSignupMode = true },
                        shape = androidx.compose.material3.SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        icon = {
                            if (isSignupMode) {
                                androidx.compose.material3.Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = null
                                )
                            }
                        }
                    ) {
                        Text("Sign up")
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(HoopSpacing.Md)
                ) {
                    HoopOutlinedTextField(
                        value = emailValue,
                        onValueChange = { viewModel.email.value = it },
                        label = "Email",
                        placeholder = "name@school.com",
                        modifier = Modifier.fillMaxWidth()
                    )

                    HoopOutlinedTextField(
                        value = passwordValue,
                        onValueChange = { viewModel.password.value = it },
                        label = "Password",
                        placeholder = "Enter password",
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                errorValue?.let { error ->
                    HoopErrorBanner(
                        message = error,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                HoopPrimaryButton(
                    text = if (isSignupMode) "Sign up" else "Log in",
                    onClick = {
                        if (isSignupMode) {
                            viewModel.signup(onSuccess = onLoginSuccess)
                        } else {
                            viewModel.login(onSuccess = onLoginSuccess)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = HoopSpacing.Xs),
                    enabled = !loadingValue
                )

                if (loadingValue) {
                    Text(
                        text = "Signing in...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
