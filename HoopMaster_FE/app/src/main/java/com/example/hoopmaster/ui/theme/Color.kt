package com.example.hoopmaster.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
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

// Athletic Tech dark design tokens.
val DarkAthleticBackground = Color(0xFF1E100C)
val DarkSurfaceLowest = Color(0xFF180B07)
val DarkSurfaceLow = Color(0xFF271813)
val DarkSurface = Color(0xFF2C1C17)
val DarkSurfaceHigh = Color(0xFF372621)
val DarkSurfaceHighest = Color(0xFF43302B)
val DarkSurfaceDim = Color(0xFF1E100C)
val DarkSurfaceBright = Color(0xFF483530)
val DarkPrimary = Color(0xFFFFB5A0)
val DarkOnPrimary = Color(0xFF5F1500)
val DarkPrimaryContainer = Color(0xFFFF5722)
val DarkOnPrimaryContainer = Color(0xFF541200)
val DarkSecondary = Color(0xFF78DC77)
val DarkOnSecondary = Color(0xFF00390A)
val DarkSecondaryContainer = Color(0xFF00761F)
val DarkOnSecondaryContainer = Color(0xFF95FB92)
val DarkTertiary = Color(0xFF86CFFF)
val DarkOnTertiary = Color(0xFF00344C)
val DarkTertiaryContainer = Color(0xFF019AD8)
val DarkOnTertiaryContainer = Color(0xFF002D43)
val DarkOnSurface = Color(0xFFFADCD4)
val DarkOnSurfaceVariant = Color(0xFFE4BEB4)
val DarkInverseSurface = Color(0xFFFADCD4)
val DarkInverseOnSurface = Color(0xFF3E2C27)
val DarkOutline = Color(0xFFAB8980)
val DarkOutlineVariant = Color(0xFF5B4039)
val DarkSurfaceVariant = Color(0xFF43302B)
val DarkSurfaceTint = DarkPrimary
val DarkError = Color(0xFFFFB4AB)
val DarkOnError = Color(0xFF690005)
val DarkErrorContainer = Color(0xFF93000A)
val DarkOnErrorContainer = Color(0xFFFFDAD6)

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

val HoopMasterDarkColorScheme: ColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    inversePrimary = Primary,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,
    background = DarkAthleticBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurfaceLowest,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    inverseSurface = DarkInverseSurface,
    inverseOnSurface = DarkInverseOnSurface,
    surfaceTint = DarkSurfaceTint
)

val HoopMasterColorScheme: ColorScheme = HoopMasterLightColorScheme
