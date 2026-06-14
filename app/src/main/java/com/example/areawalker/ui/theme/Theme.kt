package com.example.areawalker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF0F766E),
    onPrimary = Color.White,
    secondary = Color(0xFFF59E0B),
    background = Color(0xFFFAFAF7),
    onBackground = Color(0xFF12322E),
    surface = Color.White,
    onSurface = Color(0xFF12322E)
)

@Composable
fun AreaWalkerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content
    )
}

