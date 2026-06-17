package com.example.areawalker.data.routing

import com.example.areawalker.domain.model.GpsPoint
import kotlin.math.cos

class FakeRoutingService : RoutingService {
    override suspend fun createDemoLoop(
        start: GpsPoint,
        targetDistanceMeters: Double,
        targetDurationMillis: Long
    ): List<GpsPoint> {
        // TODO: Replace with backend-backed pedestrian routing via GraphHopper, Valhalla or OSRM.
        val sideMeters = targetDistanceMeters / 4.0
        val latOffset = sideMeters / 111_320.0
        val lonOffset = sideMeters / (111_320.0 * cos(Math.toRadians(start.latitude))).coerceAtLeast(0.1)
        val corners = listOf(
            start.latitude to start.longitude,
            start.latitude + latOffset to start.longitude,
            start.latitude + latOffset to start.longitude + lonOffset,
            start.latitude to start.longitude + lonOffset,
            start.latitude to start.longitude
        )

        val points = mutableListOf<GpsPoint>()
        val segments = corners.zipWithNext()
        val pointsPerSegment = 10
        val intervalMillis = targetDurationMillis / (segments.size * pointsPerSegment)

        segments.forEachIndexed { segmentIndex, (a, b) ->
            repeat(pointsPerSegment) { step ->
                val ratio = step / pointsPerSegment.toDouble()
                points += GpsPoint(
                    latitude = a.first + (b.first - a.first) * ratio,
                    longitude = a.second + (b.second - a.second) * ratio,
                    timestampMillis = start.timestampMillis + (segmentIndex * pointsPerSegment + step) * intervalMillis,
                    accuracyMeters = 8f,
                    speedMetersPerSecond = null,
                    isMock = false
                )
            }
        }
        points += GpsPoint(
            latitude = start.latitude,
            longitude = start.longitude,
            timestampMillis = start.timestampMillis + targetDurationMillis,
            accuracyMeters = 8f
        )
        return points
    }

    override suspend fun matchRoute(points: List<GpsPoint>): List<GpsPoint> = points
}
