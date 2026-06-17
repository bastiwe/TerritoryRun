package com.example.areawalker.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import com.example.areawalker.domain.model.GpsPoint
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class FusedLocationClient(
    context: Context
) : LocationClient {
    private val client: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    override suspend fun currentLocation(): GpsPoint? =
        client.lastLocation.await()?.toGpsPoint()
            ?: client.getCurrentLocation(
                LocationRequest.PRIORITY_HIGH_ACCURACY,
                CancellationTokenSource().token
            ).await()?.toGpsPoint()

    @SuppressLint("MissingPermission")
    override fun locationUpdates(): Flow<GpsPoint> = callbackFlow {
        val request = LocationRequest.create()
            .setInterval(2_000L)
            .setFastestInterval(1_000L)
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.forEach { location ->
                    trySend(location.toGpsPoint())
                }
            }
        }

        client.requestLocationUpdates(request, callback, Looper.getMainLooper())
        awaitClose { client.removeLocationUpdates(callback) }
    }

    private fun android.location.Location.toGpsPoint(): GpsPoint =
        GpsPoint(
            latitude = latitude,
            longitude = longitude,
            timestampMillis = time,
            accuracyMeters = accuracy,
            speedMetersPerSecond = if (hasSpeed()) speed else null,
            isMock = isFromMockProvider
        )
}
