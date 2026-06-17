package com.example.areawalker.ui.components

import java.util.Locale

fun meters(value: Double): String = if (value >= 1000) {
    String.format(Locale.US, "%.2f km", value / 1000.0)
} else {
    String.format(Locale.US, "%.0f m", value)
}

fun area(value: Double): String = if (value >= 10_000) {
    String.format(Locale.US, "%.2f ha", value / 10_000.0)
} else {
    String.format(Locale.US, "%.0f m²", value)
}

fun duration(millis: Long): String {
    val seconds = millis / 1000
    val minutes = seconds / 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds % 60)
}
