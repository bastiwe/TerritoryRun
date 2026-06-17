package com.example.areawalker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.example.areawalker.core.TerritoryRunApp
import com.example.areawalker.services.TrackingForegroundService
import com.example.areawalker.ui.screens.TerritoryRunRoot
import com.example.areawalker.ui.state.AppScreen
import com.example.areawalker.ui.state.TerritoryRunViewModel
import com.example.areawalker.ui.state.TerritoryRunViewModelFactory
import com.example.areawalker.ui.theme.AreaWalkerTheme

class MainActivity : ComponentActivity() {
    private val viewModel: TerritoryRunViewModel by viewModels {
        TerritoryRunViewModelFactory((application as TerritoryRunApp).container)
    }
    private var initialMapLocationRequested = false

    private val mapLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val locationGranted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
            hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (locationGranted) viewModel.refreshCurrentLocation()
    }

    private val trackingPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val locationGranted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
            hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        val notificationGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            grants[Manifest.permission.POST_NOTIFICATIONS] == true ||
            hasPermission(Manifest.permission.POST_NOTIFICATIONS)

        when {
            locationGranted && notificationGranted -> startTrackingWithService()
            !locationGranted -> viewModel.reportTrackingPermissionDenied("Bitte Standortberechtigung erlauben, damit Territory Run deine Route aufzeichnen kann.")
            else -> viewModel.reportTrackingPermissionDenied("Bitte Benachrichtigungen erlauben, damit die aktive GPS-Aufzeichnung als Foreground-Service laufen kann.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        setContent {
            val state by viewModel.state.collectAsState()
            LaunchedEffect(state.screen, state.selectedTeam) {
                if (state.screen == AppScreen.Map && state.selectedTeam != null && !initialMapLocationRequested) {
                    initialMapLocationRequested = true
                    requestInitialMapLocation()
                }
            }
            AreaWalkerTheme {
                TerritoryRunRoot(
                    state = state,
                    onSignIn = viewModel::signIn,
                    onTeam = viewModel::selectTeam,
                    onNavigate = viewModel::navigate,
                    onStart = ::requestTrackingStart,
                    onStop = ::stopTrackingWithService,
                    onDemoRun = viewModel::createDemoRun
                )
            }
        }
    }

    private fun requestTrackingStart() {
        val missingPermissions = requiredTrackingPermissions().filterNot(::hasPermission)
        if (missingPermissions.isEmpty()) {
            startTrackingWithService()
        } else {
            trackingPermissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }

    private fun requestInitialMapLocation() {
        val missingPermissions = requiredLocationPermissions().filterNot(::hasPermission)
        if (missingPermissions.isEmpty()) {
            viewModel.refreshCurrentLocation()
        } else {
            mapLocationPermissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }

    private fun startTrackingWithService() {
        ContextCompat.startForegroundService(
            this,
            Intent(this, TrackingForegroundService::class.java)
        )
        viewModel.startTracking()
    }

    private fun stopTrackingWithService() {
        viewModel.stopTracking()
        stopService(Intent(this, TrackingForegroundService::class.java))
    }

    private fun requiredTrackingPermissions(): List<String> = buildList {
        addAll(requiredLocationPermissions())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun requiredLocationPermissions(): List<String> = buildList {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}
