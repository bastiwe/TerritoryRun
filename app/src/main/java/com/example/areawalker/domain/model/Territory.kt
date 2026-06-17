package com.example.areawalker.domain.model

data class Territory(
    val id: String,
    val team: Team,
    val polygon: List<GpsPoint>,
    val areaSquareMeters: Double,
    val capturedAtMillis: Long,
    val capturedByPlayerId: String,
    val sourceSessionId: String,
    val boundingBox: BoundingBox
)

