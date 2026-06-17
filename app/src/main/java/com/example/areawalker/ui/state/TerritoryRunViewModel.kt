package com.example.areawalker.ui.state

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.areawalker.data.location.LocationClient
import com.example.areawalker.data.location.TrackingSessionManager
import com.example.areawalker.data.repository.GameRepository
import com.example.areawalker.data.routing.RoutingService
import com.example.areawalker.domain.geo.GeoAlgorithms
import com.example.areawalker.domain.model.GpsPoint
import com.example.areawalker.domain.model.Team
import com.example.areawalker.domain.model.TrackSession
import com.example.areawalker.domain.rules.RouteValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

class TerritoryRunViewModel(
    private val repository: GameRepository,
    private val locationClient: LocationClient,
    private val trackingSessionManager: TrackingSessionManager,
    private val routingService: RoutingService,
    private val validator: RouteValidator
) : ViewModel() {
    private val mutableState = MutableStateFlow(TerritoryRunUiState())
    val state: StateFlow<TerritoryRunUiState> = mutableState.asStateFlow()
    private var elapsedTickerJob: Job? = null

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
        viewModelScope.launch {
            trackingSessionManager.state.collect { trackingState ->
                updateElapsedTicker(trackingState.isTracking, trackingState.startedAtMillis)
                val points = trackingState.points
                val latest = points.lastOrNull()
                val speed = points.takeLast(2).takeIf { it.size == 2 }
                    ?.let { (a, b) -> GeoAlgorithms.speedMetersPerSecond(a, b) * 3.6 }
                    ?: 0.0
                mutableState.update { state ->
                    state.copy(
                        tracking = trackingState.isTracking,
                        currentPoints = points,
                        distanceMeters = GeoAlgorithms.totalDistanceMeters(points),
                        elapsedMillis = if (trackingState.startedAtMillis > 0L) {
                            System.currentTimeMillis() - trackingState.startedAtMillis
                        } else {
                            state.elapsedMillis
                        },
                        currentSpeedKmh = speed,
                        gpsQuality = when {
                            trackingState.error != null -> "Fehler"
                            latest == null && trackingState.isTracking -> "Suche GPS"
                            latest?.isMock == true -> "Mock"
                            latest != null && latest.accuracyMeters <= 12f -> "Sehr gut"
                            latest != null && latest.accuracyMeters <= 30f -> "Gut"
                            latest != null -> "Schwach"
                            else -> state.gpsQuality
                        },
                        trackingError = trackingState.error
                    )
                }
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
        mutableState.update {
            it.copy(
                screen = AppScreen.Tracking,
                tracking = true,
                currentPoints = emptyList(),
                distanceMeters = 0.0,
                elapsedMillis = 0L,
                gpsQuality = "Aktiv",
                trackingError = null
            )
        }
    }

    fun stopTracking() {
        elapsedTickerJob?.cancel()
        elapsedTickerJob = null
        val rawPoints = state.value.currentPoints
        mutableState.update { it.copy(gpsQuality = "Route wird angepasst") }
        viewModelScope.launch {
            val matchedPoints = runCatching { routingService.matchRoute(rawPoints) }
                .getOrElse { rawPoints }
                .ifEmpty { rawPoints }
            mutableState.update {
                it.copy(
                    currentPoints = matchedPoints,
                    distanceMeters = GeoAlgorithms.totalDistanceMeters(matchedPoints),
                    gpsQuality = if (matchedPoints != rawPoints) "Auf Wege angepasst" else "GPS"
                )
            }
            finishSession(matchedPoints, rawPoints)
        }
    }

    fun reportTrackingPermissionDenied(message: String) {
        mutableState.update {
            it.copy(
                tracking = false,
                gpsQuality = "Keine Berechtigung",
                trackingError = message
            )
        }
    }

    fun refreshCurrentLocation() {
        viewModelScope.launch {
            runCatching { locationClient.currentLocation() }
                .onSuccess { location ->
                    if (location != null) {
                        mutableState.update {
                            it.copy(
                                currentLocation = location,
                                gpsQuality = when {
                                    location.isMock -> "Mock"
                                    location.accuracyMeters <= 12f -> "Sehr gut"
                                    location.accuracyMeters <= 30f -> "Gut"
                                    else -> "Schwach"
                                },
                                trackingError = null
                            )
                        }
                    }
                }
                .onFailure { error ->
                    mutableState.update {
                        it.copy(trackingError = error.message ?: "Aktuelle Position konnte nicht geladen werden")
                    }
                }
        }
    }

    fun createDemoRun() {
        viewModelScope.launch {
            val demoDurationMillis = 12 * 60 * 1000L
            val start = currentOrFallbackStart(System.currentTimeMillis() - demoDurationMillis)
            val points = routingService.createDemoLoop(
                start = start,
                targetDistanceMeters = 800.0,
                targetDurationMillis = demoDurationMillis
            )
            mutableState.update {
                it.copy(
                    screen = AppScreen.Tracking,
                    currentPoints = points,
                    distanceMeters = GeoAlgorithms.totalDistanceMeters(points),
                    elapsedMillis = demoDurationMillis,
                    gpsQuality = if (trackingSessionManager.state.value.points.isNotEmpty()) "Demo ab GPS" else "Demo"
                )
            }
            finishSession(points, points)
        }
    }

    private fun currentOrFallbackStart(timestampMillis: Long): GpsPoint {
        val current = trackingSessionManager.state.value.points.lastOrNull() ?: state.value.currentLocation
        return current?.copy(timestampMillis = timestampMillis, accuracyMeters = 8f)
            ?: GpsPoint(
                latitude = 52.5206,
                longitude = 13.4098,
                timestampMillis = timestampMillis,
                accuracyMeters = 8f
            )
    }

    private fun finishSession(points: List<GpsPoint>, rawPoints: List<GpsPoint> = points) {
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
            rawPoints = rawPoints,
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

    private fun updateElapsedTicker(isTracking: Boolean, startedAtMillis: Long) {
        if (!isTracking || startedAtMillis <= 0L) {
            elapsedTickerJob?.cancel()
            elapsedTickerJob = null
            return
        }
        if (elapsedTickerJob != null) return
        elapsedTickerJob = viewModelScope.launch {
            while (true) {
                mutableState.update {
                    it.copy(elapsedMillis = System.currentTimeMillis() - startedAtMillis)
                }
                delay(1_000L)
            }
        }
    }
}
