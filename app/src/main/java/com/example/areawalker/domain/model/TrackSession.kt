package com.example.areawalker.domain.model

data class TrackSession(
    val id: String,
    val playerId: String,
    val team: Team,
    val startedAtMillis: Long,
    val endedAtMillis: Long?,
    val points: List<GpsPoint>,
    val distanceMeters: Double,
    val areaSquareMeters: Double,
    val validation: ValidationResult
)

data class ValidationResult(
    val isValid: Boolean,
    val reasons: List<String>,
    val filteredPoints: List<GpsPoint> = emptyList()
)

