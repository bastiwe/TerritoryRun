package com.example.areawalker.domain.model

data class GpsPoint(
    val latitude: Double,
    val longitude: Double,
    val timestampMillis: Long,
    val accuracyMeters: Float,
    val speedMetersPerSecond: Float? = null,
    val isMock: Boolean = false
)

