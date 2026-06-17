package com.example.areawalker.data.location

import com.example.areawalker.domain.model.GpsPoint
import kotlinx.coroutines.flow.Flow

interface LocationClient {
    fun locationUpdates(): Flow<GpsPoint>
}

