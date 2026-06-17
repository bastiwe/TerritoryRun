package com.example.areawalker.domain.model

data class Player(
    val id: String,
    val displayName: String,
    val team: Team? = null,
    val xp: Int = 0,
    val level: Int = 1,
    val personalAreaSquareMeters: Double = 0.0,
    val sessions: Int = 0,
    val streakDays: Int = 0,
    val lastActiveDayEpoch: Long? = null,
    val achievements: List<Achievement> = emptyList()
)

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val unlocked: Boolean = false
)

data class Mission(
    val id: String,
    val title: String,
    val target: Int,
    val progress: Int,
    val rewardXp: Int,
    val completed: Boolean
)

data class LeaderboardEntry(
    val rank: Int,
    val playerName: String,
    val team: Team,
    val areaSquareMeters: Double,
    val xp: Int
)

