package com.linknest.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LinkNestColorScheme = darkColorScheme(
    primary = LinkNestPrimary,
    onPrimary = Color.White,
    secondary = LinkNestSecondary,
    onSecondary = Color.Black,
    tertiary = LinkNestTertiary,
    onTertiary = Color.Black,
    background = LinkNestBackground,
    onBackground = LinkNestTextPrimary,
    surface = LinkNestSurface,
    onSurface = LinkNestTextPrimary,
    surfaceVariant = LinkNestSurfaceVariant,
    onSurfaceVariant = LinkNestTextSecondary,
    outline = LinkNestOutline,
    error = LinkNestError,
)

@Composable
fun LinkNestTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = LinkNestColorScheme,
        content = content,
    )
}
