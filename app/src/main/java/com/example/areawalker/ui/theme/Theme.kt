package com.example.areawalker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF22C55E),
    onPrimary = Color(0xFF07130C),
    secondary = Color(0xFFFACC15),
    background = Color(0xFF0D1117),
    onBackground = Color.White,
    surface = Color(0xFF121821),
    onSurface = Color.White
)

@Composable
fun AreaWalkerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        content = content
    )
}

