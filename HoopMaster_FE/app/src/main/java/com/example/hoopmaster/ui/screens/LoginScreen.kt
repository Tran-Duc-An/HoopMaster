package com.example.hoopmaster.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hoopmaster.ui.components.HoopErrorBanner
import com.example.hoopmaster.ui.responsive.HoopResponsiveTokens
import com.example.hoopmaster.ui.responsive.HoopWindowInfo
import com.example.hoopmaster.ui.responsive.rememberHoopResponsiveTokens
import com.example.hoopmaster.ui.responsive.rememberHoopWindowInfo
import com.example.hoopmaster.viewmodels.AuthViewModel

private val AthleticBackground = Color(0xFFFCF9F8)
private val SurfaceLowest = Color(0xFFFFFFFF)
private val SurfaceLow = Color(0xFFF6F3F2)
private val Surface = Color(0xFFF0EDEC)
private val SurfaceHigh = Color(0xFFEBE7E7)
private val SurfaceBright = Color(0xFFFCF9F8)
private val Primary = Color(0xFFB02F00)
private val PrimaryContainer = Color(0xFFFF5722)
private val OnPrimaryContainer = Color(0xFF541200)
private val OnSurface = Color(0xFF1C1B1B)
private val OnSurfaceVariant = Color(0xFF5B4039)
private val Outline = Color(0xFF907067)
private val OutlineVariant = Color(0xFFE4BEB4)

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var isSignupMode by rememberSaveable { mutableStateOf(false) }
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
    val topPadding = if (compactLayout) 20.dp else 64.dp
    val panelPadding = if (compactLayout) 20.dp else 28.dp
    val fieldGap = if (compactLayout) 12.dp else 16.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AthleticBackground)
            .imePadding()
    ) {
        AuthBackground(modifier = Modifier.matchParentSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .padding(top = topPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BrandHeader(compact = compactLayout)

            Spacer(modifier = Modifier.height(if (compactLayout) 20.dp else 36.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = tokens.sizing.contentMaxWidth)
                    .clip(RoundedCornerShape(24.dp))
                    .background(SurfaceLow.copy(alpha = 0.86f))
                    .border(
                        width = 1.dp,
                        color = SurfaceBright.copy(alpha = 0.46f),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(panelPadding),
                verticalArrangement = Arrangement.spacedBy(fieldGap)
            ) {
                AuthModeTabs(
                    isSignupMode = isSignupMode,
                    onSignupModeChange = onSignupModeChange
                )

                AuthTextField(
                    value = usernameValue,
                    onValueChange = onUsernameChange,
                    label = if (isSignupMode) "Username" else "Email or Username",
                    placeholder = if (isSignupMode) "curry30" else "athlete@example.com",
                    icon = Icons.Outlined.AlternateEmail,
                    enabled = !loadingValue
                )

                if (isSignupMode) {
                    AuthTextField(
                        value = emailValue,
                        onValueChange = onEmailChange,
                        label = "Email Address",
                        placeholder = "name@school.com",
                        icon = Icons.Outlined.AlternateEmail,
                        enabled = !loadingValue
                    )

                    AuthTextField(
                        value = nameValue,
                        onValueChange = onNameChange,
                        label = "Athlete Name",
                        placeholder = "Stephen Curry",
                        icon = Icons.Outlined.Person,
                        enabled = !loadingValue
                    )
                }

                AuthTextField(
                    value = passwordValue,
                    onValueChange = onPasswordChange,
                    label = "Password",
                    placeholder = "••••••••",
                    icon = Icons.Outlined.Lock,
                    enabled = !loadingValue,
                    visualTransformation = PasswordVisualTransformation()
                )

                errorValue?.let { error ->
                    HoopErrorBanner(
                        message = error,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                AuthSubmitButton(
                    text = if (isSignupMode) "Create Account" else "Access System",
                    loading = loadingValue,
                    onClick = onLoginClick,
                    compact = compactLayout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = if (compactLayout) 8.dp else 20.dp)
                        .heightIn(min = tokens.sizing.buttonMinHeight + 16.dp)
                )

                if (loadingValue) {
                    Text(
                        text = if (isSignupMode) "Creating account..." else "Signing in...",
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun AuthBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    SurfaceLow,
                    AthleticBackground,
                    SurfaceLowest
                )
            )
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Primary.copy(alpha = 0.12f), Color.Transparent),
                center = Offset(size.width * 0.52f, size.height * 0.10f),
                radius = size.width * 0.70f
            ),
            radius = size.width * 0.70f,
            center = Offset(size.width * 0.52f, size.height * 0.10f)
        )
        drawLine(
            color = OutlineVariant.copy(alpha = 0.24f),
            start = Offset(size.width * 0.08f, size.height * 0.78f),
            end = Offset(size.width * 0.92f, size.height * 0.68f),
            strokeWidth = 2.dp.toPx()
        )
        drawLine(
            color = OutlineVariant.copy(alpha = 0.18f),
            start = Offset(size.width * 0.02f, size.height * 0.90f),
            end = Offset(size.width * 0.98f, size.height * 0.78f),
            strokeWidth = 1.dp.toPx()
        )
    }
}

@Composable
private fun BrandHeader(compact: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 10.dp)
    ) {
        BasketballMark(
            modifier = Modifier.size(if (compact) 44.dp else 56.dp),
            color = Primary
        )
        Text(
            text = "HOOPMASTER",
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
                fontSize = if (compact) 38.sp else 48.sp,
                lineHeight = if (compact) 40.sp else 50.sp
            ),
            color = Primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "Dominate the data. Own the court.",
            style = MaterialTheme.typography.bodyLarge,
            color = OnSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun BasketballMark(
    modifier: Modifier,
    color: Color
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 4.dp.toPx()
        val radius = size.minDimension / 2f - strokeWidth / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(
            color = color,
            radius = radius,
            center = center,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        drawLine(
            color = color,
            start = Offset(center.x, center.y - radius),
            end = Offset(center.x, center.y + radius),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(center.x - radius, center.y),
            end = Offset(center.x + radius, center.y),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(center.x - radius * 1.35f, center.y - radius),
            size = Size(radius * 1.35f, radius * 2f),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        drawArc(
            color = color,
            startAngle = 90f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(center.x, center.y - radius),
            size = Size(radius * 1.35f, radius * 2f),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun AuthModeTabs(
    isSignupMode: Boolean,
    onSignupModeChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceHigh)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        AuthModeTab(
            text = "Log In",
            selected = !isSignupMode,
            onClick = { onSignupModeChange(false) },
            modifier = Modifier.weight(1f)
        )
        AuthModeTab(
            text = "Sign Up",
            selected = isSignupMode,
            onClick = { onSignupModeChange(true) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AuthModeTab(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) PrimaryContainer else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.uppercase(),
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 16.sp,
                letterSpacing = 1.2.sp
            ),
            color = if (selected) OnPrimaryContainer else OnSurfaceVariant
        )
    }
}

@Composable
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    icon: ImageVector,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label.uppercase(),
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                lineHeight = 16.sp,
                letterSpacing = 1.1.sp
            ),
            color = OnSurfaceVariant
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp),
            enabled = enabled,
            singleLine = true,
            placeholder = {
                Text(
                    text = placeholder,
                    color = Outline.copy(alpha = 0.72f)
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = OnSurfaceVariant
                )
            },
            visualTransformation = visualTransformation,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = OnSurface),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = OnSurface,
                unfocusedTextColor = OnSurface,
                disabledTextColor = OnSurface.copy(alpha = 0.46f),
                focusedContainerColor = Surface,
                unfocusedContainerColor = Surface,
                disabledContainerColor = Surface.copy(alpha = 0.72f),
                focusedBorderColor = Primary,
                unfocusedBorderColor = SurfaceBright,
                disabledBorderColor = OutlineVariant,
                cursorColor = Primary,
                focusedLeadingIconColor = Primary,
                unfocusedLeadingIconColor = OnSurfaceVariant,
                disabledLeadingIconColor = OnSurfaceVariant.copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
private fun AuthSubmitButton(
    text: String,
    loading: Boolean,
    onClick: () -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = !loading,
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = PrimaryContainer,
            contentColor = OnPrimaryContainer,
            disabledContainerColor = PrimaryContainer.copy(alpha = 0.46f),
            disabledContentColor = OnPrimaryContainer.copy(alpha = 0.62f)
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.dp,
                color = OnPrimaryContainer
            )
        } else {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = text.uppercase(),
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Black,
                        fontSize = if (compact) 22.sp else 26.sp,
                        lineHeight = if (compact) 26.sp else 30.sp
                    )
                )
                Spacer(modifier = Modifier.size(10.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}
