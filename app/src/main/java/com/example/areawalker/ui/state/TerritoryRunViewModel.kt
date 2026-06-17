package com.example.areawalker.ui.state

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.areawalker.data.location.LocationClient
import com.example.areawalker.data.repository.GameRepository
import com.example.areawalker.domain.geo.GeoAlgorithms
import com.example.areawalker.domain.model.GpsPoint
import com.example.areawalker.domain.model.Team
import com.example.areawalker.domain.model.TrackSession
import com.example.areawalker.domain.rules.RouteValidator
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class TerritoryRunViewModel(
    private val repository: GameRepository,
    private val locationClient: LocationClient,
    private val validator: RouteValidator
) : ViewModel() {
    private val mutableState = MutableStateFlow(TerritoryRunUiState())
    val state: StateFlow<TerritoryRunUiState> = mutableState.asStateFlow()

    private var trackingJob: Job? = null
    private var startedAtMillis: Long = 0L

    init {
        viewModelScope.launch {
            repository.player.collect { player ->
                mutableState.update {
                    it.copy(
                        player = player,
                        selectedTeam = player?.team,
                        screen = when {
                            player == null -> it.screen
                            player.team == null -> AppScreen.TeamSelect
                            it.screen == AppScreen.SplashLogin || it.screen == AppScreen.TeamSelect -> AppScreen.Map
                            else -> it.screen
                        }
                    )
                }
                refreshStats()
            }
        }
        viewModelScope.launch {
            repository.territories.collect { territories ->
                mutableState.update { it.copy(territories = territories) }
            }
        }
    }

    fun signIn(activity: Activity) {
        viewModelScope.launch {
            mutableState.update { it.copy(loading = true) }
            repository.signIn(activity)
            mutableState.update { it.copy(loading = false) }
        }
    }

    fun selectTeam(team: Team) {
        val player = state.value.player ?: return
        viewModelScope.launch {
            repository.selectTeam(player.id, team)
            refreshStats()
        }
    }

    fun navigate(screen: AppScreen) {
        mutableState.update { it.copy(screen = screen) }
        if (screen == AppScreen.Profile || screen == AppScreen.Leaderboard) refreshStats()
    }

    fun startTracking() {
        if (trackingJob != null) return
        startedAtMillis = System.currentTimeMillis()
        mutableState.update {
            it.copy(
                screen = AppScreen.Tracking,
                tracking = true,
                currentPoints = emptyList(),
                distanceMeters = 0.0,
                elapsedMillis = 0L,
                gpsQuality = "Aktiv"
            )
        }
        trackingJob = viewModelScope.launch {
            locationClient.locationUpdates().collect { point ->
                appendPoint(point)
            }
        }
    }

    fun stopTracking() {
        trackingJob?.cancel()
        trackingJob = null
        finishSession(state.value.currentPoints)
    }

    fun createDemoRun() {
        val now = System.currentTimeMillis() - 6 * 60 * 1000L
        val centerLat = 52.5206
        val centerLon = 13.4098
        val points = mutableListOf<GpsPoint>()
        val shape = listOf(
            centerLat - 0.0030 to centerLon - 0.0040,
            centerLat - 0.0028 to centerLon + 0.0038,
            centerLat + 0.0028 to centerLon + 0.0040,
            centerLat + 0.0030 to centerLon - 0.0038,
            centerLat - 0.0030 to centerLon - 0.0040
        )
        shape.zipWithNext().forEachIndexed { segmentIndex, (a, b) ->
            repeat(8) { step ->
                val ratio = step / 8.0
                points += GpsPoint(
                    latitude = a.first + (b.first - a.first) * ratio,
                    longitude = a.second + (b.second - a.second) * ratio,
                    timestampMillis = now + (segmentIndex * 8 + step) * 12_000L,
                    accuracyMeters = 8f
                )
            }
        }
        points += GpsPoint(shape.last().first, shape.last().second, now + 6 * 60 * 1000L, 8f)
        mutableState.update {
            it.copy(
                screen = AppScreen.Tracking,
                currentPoints = points,
                distanceMeters = GeoAlgorithms.totalDistanceMeters(points),
                elapsedMillis = 6 * 60 * 1000L,
                gpsQuality = "Demo"
            )
        }
        finishSession(points)
    }

    private fun appendPoint(point: GpsPoint) {
        mutableState.update { state ->
            val points = state.currentPoints + point
            val speed = points.takeLast(2).takeIf { it.size == 2 }
                ?.let { (a, b) -> GeoAlgorithms.speedMetersPerSecond(a, b) * 3.6 }
                ?: 0.0
            state.copy(
                currentPoints = points,
                distanceMeters = GeoAlgorithms.totalDistanceMeters(points),
                elapsedMillis = System.currentTimeMillis() - startedAtMillis,
                currentSpeedKmh = speed,
                gpsQuality = when {
                    point.isMock -> "Mock"
                    point.accuracyMeters <= 12f -> "Sehr gut"
                    point.accuracyMeters <= 30f -> "Gut"
                    else -> "Schwach"
                }
            )
        }
    }

    private fun finishSession(points: List<GpsPoint>) {
        val player = state.value.player ?: return
        val team = player.team ?: return
        val validation = validator.validate(points)
        val filtered = validation.filteredPoints.ifEmpty { points }
        val session = TrackSession(
            id = UUID.randomUUID().toString(),
            playerId = player.id,
            team = team,
            startedAtMillis = filtered.firstOrNull()?.timestampMillis ?: System.currentTimeMillis(),
            endedAtMillis = filtered.lastOrNull()?.timestampMillis,
            points = points,
            distanceMeters = GeoAlgorithms.totalDistanceMeters(filtered),
            areaSquareMeters = GeoAlgorithms.approximatePolygonAreaSquareMeters(filtered),
            validation = validation
        )
        viewModelScope.launch {
            val result = repository.submitSession(session)
            mutableState.update {
                it.copy(
                    tracking = false,
                    lastSession = result.session,
                    lastAwardedXp = result.awardedXp,
                    lastMessage = result.message,
                    screen = AppScreen.SessionResult
                )
            }
            refreshStats()
        }
    }

    private fun refreshStats() {
        val playerId = state.value.player?.id ?: return
        viewModelScope.launch {
            mutableState.update { it.copy(stats = repository.stats(playerId)) }
        }
    }
}

