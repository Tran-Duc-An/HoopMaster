package com.example.hoopmaster.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val BrandSans = FontFamily.SansSerif

val HoopMasterTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = BrandSans,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 48.sp,
        lineHeight = 52.8.sp,
        letterSpacing = (-0.96).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = BrandSans,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 38.4.sp,
        letterSpacing = (-0.32).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = BrandSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 28.8.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = BrandSans,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 27.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = BrandSans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    labelLarge = TextStyle(
        fontFamily = BrandSans,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 16.8.sp,
        letterSpacing = 0.7.sp
    )
)

