package com.example.areawalker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import com.example.areawalker.core.TerritoryRunApp
import com.example.areawalker.ui.screens.TerritoryRunRoot
import com.example.areawalker.ui.state.TerritoryRunViewModel
import com.example.areawalker.ui.state.TerritoryRunViewModelFactory
import com.example.areawalker.ui.theme.AreaWalkerTheme

class MainActivity : ComponentActivity() {
    private val viewModel: TerritoryRunViewModel by viewModels {
        TerritoryRunViewModelFactory((application as TerritoryRunApp).container)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        setContent {
            val state by viewModel.state.collectAsState()
            AreaWalkerTheme {
                TerritoryRunRoot(
                    state = state,
                    onSignIn = viewModel::signIn,
                    onTeam = viewModel::selectTeam,
                    onNavigate = viewModel::navigate,
                    onStart = viewModel::startTracking,
                    onStop = viewModel::stopTracking,
                    onDemoRun = viewModel::createDemoRun
                )
            }
        }
    }
}

