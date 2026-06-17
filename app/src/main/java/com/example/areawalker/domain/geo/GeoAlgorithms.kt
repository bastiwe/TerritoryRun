package com.example.areawalker.domain.geo

import com.example.areawalker.domain.model.BoundingBox
import com.example.areawalker.domain.model.GpsPoint
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object GeoAlgorithms {
    private const val EarthRadiusMeters = 6_371_000.0

    fun haversineMeters(a: GpsPoint, b: GpsPoint): Double {
        val dLat = Math.toRadians(b.latitude - a.latitude)
        val dLon = Math.toRadians(b.longitude - a.longitude)
        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)
        val h = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
        return 2 * EarthRadiusMeters * atan2(sqrt(h), sqrt(1 - h))
    }

    fun totalDistanceMeters(points: List<GpsPoint>): Double =
        points.zipWithNext().sumOf { (a, b) -> haversineMeters(a, b) }

    fun speedMetersPerSecond(a: GpsPoint, b: GpsPoint): Double {
        val seconds = (b.timestampMillis - a.timestampMillis).coerceAtLeast(1L) / 1000.0
        return haversineMeters(a, b) / seconds
    }

    fun isClosedLoop(points: List<GpsPoint>, maxDistanceMeters: Double): Boolean {
        if (points.size < 3) return false
        return haversineMeters(points.first(), points.last()) <= maxDistanceMeters
    }

    fun approximatePolygonAreaSquareMeters(points: List<GpsPoint>): Double {
        if (points.size < 3) return 0.0
        val originLat = Math.toRadians(points.map { it.latitude }.average())
        val projected = points.map {
            val x = Math.toRadians(it.longitude) * EarthRadiusMeters * cos(originLat)
            val y = Math.toRadians(it.latitude) * EarthRadiusMeters
            x to y
        }
        val closed = if (projected.first() == projected.last()) projected else projected + projected.first()
        val shoelace = closed.zipWithNext().sumOf { (a, b) -> a.first * b.second - b.first * a.second }
        return abs(shoelace) / 2.0
    }

    fun simplifyRoute(points: List<GpsPoint>, minDistanceMeters: Double = 4.0): List<GpsPoint> {
        if (points.size < 3) return points
        val result = mutableListOf(points.first())
        points.drop(1).dropLast(1).forEach { point ->
            if (haversineMeters(result.last(), point) >= minDistanceMeters) result += point
        }
        result += points.last()
        return result
    }

    fun filterOutliers(
        points: List<GpsPoint>,
        maxSpeedMetersPerSecond: Double,
        maxAccuracyMeters: Float
    ): List<GpsPoint> {
        if (points.isEmpty()) return emptyList()
        val filtered = mutableListOf(points.first())
        points.drop(1).forEach { point ->
            val previous = filtered.last()
            val speed = speedMetersPerSecond(previous, point)
            if (point.accuracyMeters <= maxAccuracyMeters && speed <= maxSpeedMetersPerSecond * 1.6) {
                filtered += point
            }
        }
        return filtered
    }

    fun boundingBox(points: List<GpsPoint>): BoundingBox {
        val lats = points.map { it.latitude }
        val lons = points.map { it.longitude }
        return BoundingBox(lats.min(), lons.min(), lats.max(), lons.max())
    }

    fun hasSelfIntersection(points: List<GpsPoint>): Boolean {
        if (points.size < 5) return false
        val projected = points.map { it.longitude to it.latitude }
        val segments = projected.zipWithNext()
        for (i in segments.indices) {
            for (j in i + 1 until segments.size) {
                if (abs(i - j) <= 1) continue
                if (i == 0 && j == segments.lastIndex) continue
                if (segmentsIntersect(segments[i].first, segments[i].second, segments[j].first, segments[j].second)) {
                    return true
                }
            }
        }
        return false
    }

    private fun segmentsIntersect(a: Pair<Double, Double>, b: Pair<Double, Double>, c: Pair<Double, Double>, d: Pair<Double, Double>): Boolean {
        fun orientation(p: Pair<Double, Double>, q: Pair<Double, Double>, r: Pair<Double, Double>): Double =
            (q.second - p.second) * (r.first - q.first) - (q.first - p.first) * (r.second - q.second)

        val o1 = orientation(a, b, c)
        val o2 = orientation(a, b, d)
        val o3 = orientation(c, d, a)
        val o4 = orientation(c, d, b)
        return (o1 > 0 && o2 < 0 || o1 < 0 && o2 > 0) && (o3 > 0 && o4 < 0 || o3 < 0 && o4 > 0)
    }
}

