package com.example.areawalker.data.location

import com.example.areawalker.domain.model.GpsPoint
import com.example.areawalker.domain.geo.GeoAlgorithms
import com.example.areawalker.domain.rules.GameRules
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TrackingLocationState(
    val isTracking: Boolean = false,
    val startedAtMillis: Long = 0L,
    val points: List<GpsPoint> = emptyList(),
    val error: String? = null
)

class TrackingSessionManager(
    private val locationClient: LocationClient
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutableState = MutableStateFlow(TrackingLocationState())
    val state: StateFlow<TrackingLocationState> = mutableState.asStateFlow()

    private var trackingJob: Job? = null

    fun startTracking() {
        if (trackingJob != null) return
        mutableState.value = TrackingLocationState(
            isTracking = true,
            startedAtMillis = System.currentTimeMillis()
        )
        trackingJob = scope.launch {
            runCatching {
                locationClient.locationUpdates().collect { point ->
                    mutableState.update { current ->
                        current.copy(points = appendIfUseful(current.points, point), error = null)
                    }
                }
            }.onFailure { error ->
                mutableState.update {
                    it.copy(
                        isTracking = false,
                        error = error.message ?: "Standort konnte nicht gestartet werden"
                    )
                }
                trackingJob = null
            }
        }
    }

    fun stopTracking() {
        trackingJob?.cancel()
        trackingJob = null
        mutableState.update { it.copy(isTracking = false) }
    }

    private fun appendIfUseful(points: List<GpsPoint>, point: GpsPoint): List<GpsPoint> {
        if (point.accuracyMeters > GameRules.MaxAcceptedAccuracyMeters) return points
        val previous = points.lastOrNull() ?: return listOf(point)
        val distanceMeters = GeoAlgorithms.haversineMeters(previous, point)
        val seconds = ((point.timestampMillis - previous.timestampMillis).coerceAtLeast(1L)) / 1000.0
        val speedMetersPerSecond = distanceMeters / seconds
        val jitterThresholdMeters = maxOf(8.0, previous.accuracyMeters.toDouble(), point.accuracyMeters.toDouble())

        if (distanceMeters < jitterThresholdMeters) return points
        if (speedMetersPerSecond > GameRules.MaxSpeedMetersPerSecond * 1.6) return points

        return points + point
    }
}
