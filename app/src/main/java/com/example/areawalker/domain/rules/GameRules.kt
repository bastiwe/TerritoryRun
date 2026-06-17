package com.example.areawalker.domain.rules

object GameRules {
    const val MaxSpeedKmh = 15.0
    const val MaxSpeedMetersPerSecond = MaxSpeedKmh / 3.6
    const val MaxClosingDistanceMeters = 50.0
    const val MinimumDurationMillis = 5 * 60 * 1000L
    const val MinimumDistanceMeters = 500.0
    const val MinimumAreaSquareMeters = 4_000.0
    const val MinimumGpsPoints = 24
    const val MaxAcceptedAccuracyMeters = 45f
    const val LargeJumpMeters = 100.0
}

