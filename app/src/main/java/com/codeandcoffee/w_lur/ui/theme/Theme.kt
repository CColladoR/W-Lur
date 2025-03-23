package com.codeandcoffee.w_lur.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF6200EE),
    secondary = Color(0xFF03DAC5),
    tertiary = Color(0xFF03DAC5),
    background = Color(0xFFF5F5F5) // Fondo claro
)

private val DarkColors = darkColorScheme(
    primary = Color(0xffe1888c),
    secondary = Color(0xff91bceb),
    tertiary = Color(0xff91bceb),
    background = Color(0xFF121212) // Fondo oscuro
)

@Composable
fun WLurTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (!useDarkTheme) {
        LightColors
    } else {
        DarkColors
    }

    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}