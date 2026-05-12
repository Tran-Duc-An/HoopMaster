package com.example.hoopmaster.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Athletic Tech Light design tokens.
val AthleticBackground = Color(0xFFFCF9F8)
val SurfaceLowest = Color(0xFFFFFFFF)
val SurfaceLow = Color(0xFFF6F3F2)
val Surface = Color(0xFFF0EDEC)
val SurfaceHigh = Color(0xFFEBE7E7)
val SurfaceHighest = Color(0xFFE5E2E1)
val SurfaceDim = Color(0xFFDCD9D9)
val SurfaceBright = Color(0xFFFCF9F8)
val Primary = Color(0xFFB02F00)
val OnPrimary = Color(0xFFFFFFFF)
val PrimaryContainer = Color(0xFFFF5722)
val OnPrimaryContainer = Color(0xFF541200)
val Secondary = Color(0xFF1B6D24)
val OnSecondary = Color(0xFFFFFFFF)
val SecondaryContainer = Color(0xFFA0F399)
val OnSecondaryContainer = Color(0xFF217128)
val Tertiary = Color(0xFF006096)
val OnTertiary = Color(0xFFFFFFFF)
val TertiaryContainer = Color(0xFF007ABC)
val OnTertiaryContainer = Color(0xFFFDFCFF)
val OnSurface = Color(0xFF1C1B1B)
val OnSurfaceVariant = Color(0xFF5B4039)
val InverseSurface = Color(0xFF313030)
val InverseOnSurface = Color(0xFFF3F0EF)
val Outline = Color(0xFF907067)
val OutlineVariant = Color(0xFFE4BEB4)
val SurfaceVariant = Color(0xFFE5E2E1)
val SurfaceTint = Primary
val Transparent = Color.Transparent

val Success = Secondary
val OnSuccess = OnSecondary
val SuccessContainer = SecondaryContainer
val ActiveOrange = PrimaryContainer
val NavyShadow = OnSurface
val Error = Color(0xFFBA1A1A)
val OnError = Color(0xFFFFFFFF)
val ErrorContainer = Color(0xFFFFDAD6)
val OnErrorContainer = Color(0xFF93000A)

val HoopMasterLightColorScheme: ColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    inversePrimary = Color(0xFFFFB5A0),
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    tertiary = Tertiary,
    onTertiary = OnTertiary,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = OnTertiaryContainer,
    error = Error,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
    background = AthleticBackground,
    onBackground = OnSurface,
    surface = SurfaceLowest,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    outline = Outline,
    outlineVariant = OutlineVariant,
    inverseSurface = InverseSurface,
    inverseOnSurface = InverseOnSurface,
    surfaceTint = SurfaceTint
)

val HoopMasterColorScheme: ColorScheme = HoopMasterLightColorScheme
