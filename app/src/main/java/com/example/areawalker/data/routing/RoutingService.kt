package com.example.areawalker.data.routing

import com.example.areawalker.domain.model.GpsPoint

interface RoutingService {
    suspend fun createDemoLoop(
        start: GpsPoint,
        targetDistanceMeters: Double,
        targetDurationMillis: Long
    ): List<GpsPoint>

    suspend fun matchRoute(points: List<GpsPoint>): List<GpsPoint> = points
}
