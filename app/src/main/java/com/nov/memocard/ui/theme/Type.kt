package com.nov.memocard.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily

private val Base = Typography()

private fun serif(style: TextStyle): TextStyle = style.copy(fontFamily = FontFamily.Serif)

val Typography = Typography(
    displayLarge = serif(Base.displayLarge),
    displayMedium = serif(Base.displayMedium),
    displaySmall = serif(Base.displaySmall),
    headlineLarge = serif(Base.headlineLarge),
    headlineMedium = serif(Base.headlineMedium),
    headlineSmall = serif(Base.headlineSmall),
    titleLarge = serif(Base.titleLarge),
    titleMedium = serif(Base.titleMedium),
    titleSmall = serif(Base.titleSmall),
    bodyLarge = serif(Base.bodyLarge),
    bodyMedium = serif(Base.bodyMedium),
    bodySmall = serif(Base.bodySmall),
    labelLarge = serif(Base.labelLarge),
    labelMedium = serif(Base.labelMedium),
    labelSmall = serif(Base.labelSmall),
)

/** Screen-size based font scale: small phones shrink a bit, tablets grow, clamped. */
fun responsiveFontScale(widthDp: Int, heightDp: Int): Float {
    val shortSide = minOf(widthDp, heightDp).coerceAtLeast(1)
    return (shortSide / 400f).coerceIn(0.85f, 1.45f)
}