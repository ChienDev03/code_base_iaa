package com.your.sourbase.ui.theme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
/** Single replacement surface for brand mapping. Dynamic color is intentionally off. */
object BrandPalette {
    val light = lightColorScheme(
        primary = ColorPrimitives.Blue, onPrimary = ColorPrimitives.White,
        primaryContainer = ColorPrimitives.PaleBlue, onPrimaryContainer = ColorPrimitives.Navy,
        secondary = ColorPrimitives.Gray, onSecondary = ColorPrimitives.White,
        secondaryContainer = ColorPrimitives.Light, onSecondaryContainer = ColorPrimitives.Black,
        tertiary = ColorPrimitives.Navy, onTertiary = ColorPrimitives.White,
        tertiaryContainer = ColorPrimitives.PaleBlue, onTertiaryContainer = ColorPrimitives.Navy,
        surface = ColorPrimitives.White, onSurface = ColorPrimitives.Black,
        background = ColorPrimitives.Light, onBackground = ColorPrimitives.Black,
        surfaceVariant = ColorPrimitives.Light, onSurfaceVariant = ColorPrimitives.Gray,
        outline = ColorPrimitives.Outline, outlineVariant = ColorPrimitives.Outline,
        error = ColorPrimitives.Error, onError = ColorPrimitives.White,
        errorContainer = ColorPrimitives.ErrorContainer, onErrorContainer = ColorPrimitives.Error,
    )
    val dark = darkColorScheme(
        primary = ColorPrimitives.PaleBlue, onPrimary = ColorPrimitives.Navy,
        primaryContainer = ColorPrimitives.Navy, onPrimaryContainer = ColorPrimitives.PaleBlue,
        secondary = ColorPrimitives.Light, onSecondary = ColorPrimitives.Black,
        secondaryContainer = ColorPrimitives.DarkSurface, onSecondaryContainer = ColorPrimitives.White,
        tertiary = ColorPrimitives.PaleBlue, onTertiary = ColorPrimitives.Navy,
        tertiaryContainer = ColorPrimitives.Navy, onTertiaryContainer = ColorPrimitives.PaleBlue,
        surface = ColorPrimitives.DarkSurface, onSurface = ColorPrimitives.White,
        background = ColorPrimitives.Black, onBackground = ColorPrimitives.White,
        surfaceVariant = ColorPrimitives.DarkSurface, onSurfaceVariant = ColorPrimitives.Light,
        outline = ColorPrimitives.Light, outlineVariant = ColorPrimitives.Outline,
        error = ColorPrimitives.ErrorContainer, onError = ColorPrimitives.Error,
        errorContainer = ColorPrimitives.Error, onErrorContainer = ColorPrimitives.ErrorContainer,
    )
}
/** App typography uses the platform font, without brand font licensing dependencies. */
data class AppTypography(val title: TextStyle = TextStyle(fontSize = 24.sp), val body: TextStyle = TextStyle(fontSize = 16.sp))
val LocalAppTypography = staticCompositionLocalOf { AppTypography() }
/** Preserves density and fixes app-owned text scale, including dialog descendants. */
@Composable
fun AppTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val density = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1f)) {
        MaterialTheme(colorScheme = if (dark) BrandPalette.dark else BrandPalette.light, content = content)
    }
}
