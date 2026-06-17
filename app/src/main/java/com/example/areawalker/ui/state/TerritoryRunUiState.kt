package com.example.areawalker.ui.state

import com.example.areawalker.domain.model.GameStats
import com.example.areawalker.domain.model.GpsPoint
import com.example.areawalker.domain.model.Player
import com.example.areawalker.domain.model.Team
import com.example.areawalker.domain.model.Territory
import com.example.areawalker.domain.model.TrackSession

data class TerritoryRunUiState(
    val screen: AppScreen = AppScreen.SplashLogin,
    val loading: Boolean = false,
    val player: Player? = null,
    val selectedTeam: Team? = null,
    val territories: List<Territory> = emptyList(),
    val currentLocation: GpsPoint? = null,
    val tracking: Boolean = false,
    val currentPoints: List<GpsPoint> = emptyList(),
    val distanceMeters: Double = 0.0,
    val elapsedMillis: Long = 0L,
    val currentSpeedKmh: Double = 0.0,
    val gpsQuality: String = "Warten",
    val trackingError: String? = null,
    val lastSession: TrackSession? = null,
    val lastAwardedXp: Int = 0,
    val lastMessage: String = "",
    val stats: GameStats? = null
)
