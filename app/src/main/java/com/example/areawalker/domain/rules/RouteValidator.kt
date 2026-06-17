package com.example.areawalker.domain.rules

import com.example.areawalker.domain.geo.GeoAlgorithms
import com.example.areawalker.domain.model.GpsPoint
import com.example.areawalker.domain.model.ValidationResult

class RouteValidator {
    fun validate(points: List<GpsPoint>): ValidationResult {
        val reasons = mutableListOf<String>()
        if (points.any { it.isMock }) reasons += "Mock Location erkannt"

        val filtered = GeoAlgorithms
            .filterOutliers(points, GameRules.MaxSpeedMetersPerSecond, GameRules.MaxAcceptedAccuracyMeters)
            .let { GeoAlgorithms.simplifyRoute(it) }

        if (filtered.size < GameRules.MinimumGpsPoints) reasons += "Zu wenige GPS-Punkte"
        if (!GeoAlgorithms.isClosedLoop(filtered, GameRules.MaxClosingDistanceMeters)) reasons += "Start und Ende liegen nicht nah genug beieinander"

        val duration = (filtered.lastOrNull()?.timestampMillis ?: 0L) - (filtered.firstOrNull()?.timestampMillis ?: 0L)
        if (duration < GameRules.MinimumDurationMillis) reasons += "Rundgang ist zu kurz"

        val distance = GeoAlgorithms.totalDistanceMeters(filtered)
        if (distance < GameRules.MinimumDistanceMeters) reasons += "Distanz unter 500 m"

        val area = GeoAlgorithms.approximatePolygonAreaSquareMeters(filtered)
        if (area < GameRules.MinimumAreaSquareMeters) reasons += "Fläche ist zu klein"

        if (GeoAlgorithms.hasSelfIntersection(filtered)) reasons += "Route überschneidet sich selbst"

        filtered.zipWithNext().forEach { (a, b) ->
            val jump = GeoAlgorithms.haversineMeters(a, b)
            val speed = GeoAlgorithms.speedMetersPerSecond(a, b)
            if (jump > GameRules.LargeJumpMeters && speed > GameRules.MaxSpeedMetersPerSecond) {
                reasons += "Unrealistischer GPS-Sprung erkannt"
                return@forEach
            }
            if (speed > GameRules.MaxSpeedMetersPerSecond) {
                reasons += "Maximalgeschwindigkeit überschritten"
                return@forEach
            }
        }

        return ValidationResult(
            isValid = reasons.isEmpty(),
            reasons = reasons.distinct(),
            filteredPoints = filtered
        )
    }
}
