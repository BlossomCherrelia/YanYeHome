package com.yanye.home.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yanye.home.R

private val YanYeLightColorScheme = lightColorScheme(
    primary = YanYeColors.Rose,
    onPrimary = Color.White,
    primaryContainer = YanYeColors.RoseSoft,
    onPrimaryContainer = YanYeColors.Ink,
    secondary = YanYeColors.Green,
    onSecondary = Color.White,
    secondaryContainer = YanYeColors.GreenSoft,
    onSecondaryContainer = YanYeColors.Ink,
    tertiary = YanYeColors.Blue,
    onTertiary = Color.White,
    tertiaryContainer = YanYeColors.BlueSoft,
    onTertiaryContainer = YanYeColors.Ink,
    background = YanYeColors.Paper,
    onBackground = YanYeColors.Ink,
    surface = YanYeColors.Paper,
    onSurface = YanYeColors.Ink,
    surfaceVariant = YanYeColors.Soft,
    onSurfaceVariant = YanYeColors.Muted,
    outline = YanYeColors.Line
)

private val YanYeShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
)

private val MiSansFontFamily = FontFamily(
    Font(R.font.misans_regular, FontWeight.Normal),
    Font(R.font.misans_medium, FontWeight.Medium),
    Font(R.font.misans_semibold, FontWeight.SemiBold),
    Font(R.font.misans_semibold, FontWeight.Bold)
)

private val BaseTypography = Typography()

private val YanYeTypography = Typography(
    displayLarge = BaseTypography.displayLarge.copy(fontFamily = MiSansFontFamily),
    displayMedium = BaseTypography.displayMedium.copy(fontFamily = MiSansFontFamily),
    displaySmall = BaseTypography.displaySmall.copy(fontFamily = MiSansFontFamily),
    headlineLarge = BaseTypography.headlineLarge.copy(fontFamily = MiSansFontFamily),
    headlineMedium = BaseTypography.headlineMedium.copy(fontFamily = MiSansFontFamily),
    headlineSmall = BaseTypography.headlineSmall.copy(fontFamily = MiSansFontFamily),
    titleLarge = BaseTypography.titleLarge.copy(fontFamily = MiSansFontFamily),
    titleMedium = BaseTypography.titleMedium.copy(fontFamily = MiSansFontFamily),
    titleSmall = BaseTypography.titleSmall.copy(fontFamily = MiSansFontFamily),
    bodyLarge = BaseTypography.bodyLarge.copy(fontFamily = MiSansFontFamily),
    bodyMedium = BaseTypography.bodyMedium.copy(fontFamily = MiSansFontFamily),
    bodySmall = BaseTypography.bodySmall.copy(fontFamily = MiSansFontFamily),
    labelLarge = BaseTypography.labelLarge.copy(fontFamily = MiSansFontFamily),
    labelMedium = BaseTypography.labelMedium.copy(fontFamily = MiSansFontFamily),
    labelSmall = BaseTypography.labelSmall.copy(fontFamily = MiSansFontFamily)
)

@Composable
fun YanYeHomeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = YanYeLightColorScheme,
        typography = YanYeTypography,
        shapes = YanYeShapes,
        content = content
    )
}
