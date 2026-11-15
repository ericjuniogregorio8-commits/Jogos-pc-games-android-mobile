package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val ZipPlayDarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = ObsidianBg,
    primaryContainer = NeonCyanDark,
    onPrimaryContainer = NeonCyan,
    secondary = ElectricAmber,
    onSecondary = ObsidianBg,
    secondaryContainer = ElectricAmberDark,
    onSecondaryContainer = ElectricAmber,
    tertiary = ArcadeMagenta,
    onTertiary = ObsidianBg,
    tertiaryContainer = ArcadeMagentaDark,
    onTertiaryContainer = ArcadeMagenta,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = ObsidianBg,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = ObsidianSurfaceVariant,
    surfaceContainerHigh = ObsidianElevated,
    surfaceContainerHighest = ObsidianBorder,
    outline = ObsidianBorder,
    outlineVariant = TextMuted,
    error = ErrorRed,
    onError = ObsidianBg,
    errorContainer = ErrorContainerDark,
    onErrorContainer = ErrorRed
)

val ZipPlayShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun ZipPlayTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ZipPlayDarkColorScheme,
        typography = ZipPlayTypography,
        shapes = ZipPlayShapes,
        content = content
    )
}
