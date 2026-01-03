package com.example.wearosdashboard.presentation.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

internal val WearAppColorPalette: Colors = Colors(
    primary = Color(0xFFF7931A), // Bitcoin Orange
    primaryVariant = Color(0xFFC07204),
    secondary = Color(0xFFFFD700), // Gold
    secondaryVariant = Color(0xFFC5A800),
    error = Color(0xFFCF6679),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onError = Color.Black
)

internal val WearAppTypography = Typography(
    body1 = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    )
)

@Composable
fun WearOSDashboardTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colors = WearAppColorPalette,
        typography = WearAppTypography,
        content = content
    )
}
