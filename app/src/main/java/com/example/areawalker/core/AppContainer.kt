package com.example.areawalker.core

import android.content.Context
import com.example.areawalker.data.auth.FakePlayGamesAuthService
import com.example.areawalker.data.backend.InMemoryGameApiService
import com.example.areawalker.data.local.SettingsStore
import com.example.areawalker.data.location.FusedLocationClient
import com.example.areawalker.data.location.TrackingSessionManager
import com.example.areawalker.data.repository.GameRepository
import com.example.areawalker.data.routing.BackendRoutingService
import com.example.areawalker.data.routing.FakeRoutingService
import com.example.areawalker.BuildConfig
import com.example.areawalker.domain.rules.RouteValidator

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val settingsStore = SettingsStore(appContext)
    private val api = InMemoryGameApiService()

    val gameRepository = GameRepository(
        authService = FakePlayGamesAuthService(),
        api = api,
        settingsStore = settingsStore
    )
    val locationClient = FusedLocationClient(appContext)
    val trackingSessionManager = TrackingSessionManager(locationClient)
    val routingService = if (BuildConfig.ROUTING_BASE_URL.isBlank()) {
        FakeRoutingService()
    } else {
        BackendRoutingService(BuildConfig.ROUTING_BASE_URL)
    }
    val routeValidator = RouteValidator()
}
