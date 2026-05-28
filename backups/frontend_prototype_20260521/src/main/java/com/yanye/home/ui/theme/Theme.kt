package com.yanye.home.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

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

@Composable
fun YanYeHomeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = YanYeLightColorScheme,
        typography = Typography(),
        shapes = YanYeShapes,
        content = content
    )
}
