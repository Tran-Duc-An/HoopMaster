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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import com.example.hoopmaster.ui.responsive.HoopResponsiveTokens
import com.example.hoopmaster.ui.responsive.HoopWindowInfo
import com.example.hoopmaster.ui.responsive.rememberHoopResponsiveTokens
import com.example.hoopmaster.ui.responsive.rememberHoopWindowInfo
import com.example.hoopmaster.viewmodels.AuthViewModel

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var isSignupMode by rememberSaveable { mutableStateOf(false) }
    val logoId = R.drawable.hoopmaster_logo
    val usernameValue = viewModel.username.value
    val emailValue = viewModel.email.value
    val nameValue = viewModel.name.value
    val passwordValue = viewModel.password.value
    val loadingValue = viewModel.isLoading.value
    val errorValue = viewModel.errorMessage.value

    val windowInfo = rememberHoopWindowInfo()
    val tokens = rememberHoopResponsiveTokens(windowInfo)

    LoginScreenContent(
        isSignupMode = isSignupMode,
        onSignupModeChange = { isSignupMode = it },
        logoId = logoId,
        usernameValue = usernameValue,
        onUsernameChange = { viewModel.username.value = it },
        emailValue = emailValue,
        onEmailChange = { viewModel.email.value = it },
        nameValue = nameValue,
        onNameChange = { viewModel.name.value = it },
        passwordValue = passwordValue,
        onPasswordChange = { viewModel.password.value = it },
        loadingValue = loadingValue,
        errorValue = errorValue,
        onLoginClick = {
            if (isSignupMode) {
                viewModel.signup(onSuccess = onLoginSuccess)
            } else {
                viewModel.login(onSuccess = onLoginSuccess)
            }
        },
        windowInfo = windowInfo,
        tokens = tokens
    )
}

@Composable
private fun LoginScreenContent(
    isSignupMode: Boolean,
    onSignupModeChange: (Boolean) -> Unit,
    logoId: Int,
    usernameValue: String,
    onUsernameChange: (String) -> Unit,
    emailValue: String,
    onEmailChange: (String) -> Unit,
    nameValue: String,
    onNameChange: (String) -> Unit,
    passwordValue: String,
    onPasswordChange: (String) -> Unit,
    loadingValue: Boolean,
    errorValue: String?,
    onLoginClick: () -> Unit,
    windowInfo: HoopWindowInfo,
    tokens: HoopResponsiveTokens
) {
    val compactLayout = windowInfo.isLandscape || windowInfo.isSmallHeight
    val outerVerticalPadding = if (compactLayout) tokens.spacing.contentGap else tokens.spacing.sectionGap

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
            .verticalScroll(rememberScrollState())
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(
                    horizontal = tokens.spacing.screenMargin,
                    vertical = outerVerticalPadding
                )
                .widthIn(max = tokens.sizing.contentMaxWidth),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(tokens.spacing.cardPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(tokens.spacing.sectionGap)
            ) {
                Image(
                    painter = painterResource(id = logoId),
                    contentDescription = "HoopMaster logo",
                    modifier = Modifier.size(tokens.sizing.logoSize),
                    contentScale = ContentScale.Fit
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(tokens.spacing.contentGap)
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
                        onClick = { onSignupModeChange(false) },
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
                        onClick = { onSignupModeChange(true) },
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
                    verticalArrangement = Arrangement.spacedBy(tokens.spacing.contentGap)
                ) {
                    HoopOutlinedTextField(
                        value = usernameValue,
                        onValueChange = onUsernameChange,
                        label = if (isSignupMode) "Username" else "Username or email",
                        placeholder = if (isSignupMode) "curry30" else "curry30 or name@school.com",
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (isSignupMode) {
                        HoopOutlinedTextField(
                            value = emailValue,
                            onValueChange = onEmailChange,
                            label = "Email",
                            placeholder = "name@school.com",
                            modifier = Modifier.fillMaxWidth()
                        )

                        HoopOutlinedTextField(
                            value = nameValue,
                            onValueChange = onNameChange,
                            label = "Name",
                            placeholder = "Stephen Curry",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    HoopOutlinedTextField(
                        value = passwordValue,
                        onValueChange = onPasswordChange,
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
                    onClick = onLoginClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = tokens.spacing.contentGap)
                        .heightIn(min = tokens.sizing.buttonMinHeight),
                    enabled = !loadingValue,
                    compact = compactLayout
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
