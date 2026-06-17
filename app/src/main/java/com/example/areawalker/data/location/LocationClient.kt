package com.example.areawalker.data.location

import com.example.areawalker.domain.model.GpsPoint
import kotlinx.coroutines.flow.Flow

interface LocationClient {
    suspend fun currentLocation(): GpsPoint?
    fun locationUpdates(): Flow<GpsPoint>
}
