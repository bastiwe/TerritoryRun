package com.example.areawalker.domain.model

enum class Team(val displayName: String, val color: Long) {
    Neutral("Neutral", 0xFF8A8F98),
    Red("Rot", 0xFFE5484D),
    Blue("Blau", 0xFF3B82F6),
    Green("Grün", 0xFF22C55E);

    companion object {
        val playable = listOf(Red, Blue, Green)
    }
}
