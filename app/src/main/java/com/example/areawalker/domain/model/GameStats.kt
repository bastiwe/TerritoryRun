package com.example.areawalker.domain.model

data class GameStats(
    val player: Player,
    val teamAreas: Map<Team, Double>,
    val missions: List<Mission>,
    val leaderboard: List<LeaderboardEntry>
)

