package com.nov.memocard.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    primaryContainer = TealSoftLight,
    onPrimaryContainer = Teal,
    secondary = Teal,
    background = BgLight,
    surface = SurfaceWhite,
    surfaceVariant = BgLight,
    onBackground = InkLight2,
    onSurface = InkLight2,
    onSurfaceVariant = MutedLight2,
    outline = DividerLight,
    outlineVariant = DividerLight,
    error = BadgeOrange,
)

private val DarkColors = darkColorScheme(
    primary = TealDark,
    onPrimary = Color(0xFF00332E),
    primaryContainer = TealSoftDark,
    onPrimaryContainer = TealDark,
    secondary = TealDark,
    background = Color.Black,
    surface = Color.Black,
    surfaceVariant = Color(0xFF151515),
    onBackground = InkDark2,
    onSurface = InkDark2,
    onSurfaceVariant = MutedDark2,
    outline = Color(0xFF242424),
    outlineVariant = Color(0xFF242424),
    error = BadgeOrangeDark,
)

private val MemoShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp),
)

@Composable
fun MemoCardTheme(
    themeMode: Int = 0,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = Typography,
        shapes = MemoShapes,
        content = content,
    )
}