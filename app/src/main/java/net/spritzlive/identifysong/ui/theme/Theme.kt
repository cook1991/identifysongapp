package net.spritzlive.identifysong.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Accent = Color(0xFF9C7BFF)
private val AccentDark = Color(0xFF5A32D8)
private val BackgroundDark = Color(0xFF1B1033)
private val BackgroundLight = Color(0xFFF7F4FF)

private val DarkColors = darkColorScheme(
    primary = Accent,
    secondary = AccentDark,
    background = BackgroundDark,
    surface = BackgroundDark
)

private val LightColors = lightColorScheme(
    primary = AccentDark,
    secondary = Accent,
    background = BackgroundLight,
    surface = BackgroundLight
)

@Composable
fun IdentifySongTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
