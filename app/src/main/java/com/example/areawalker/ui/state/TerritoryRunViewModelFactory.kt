package com.example.areawalker.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.areawalker.core.AppContainer

class TerritoryRunViewModelFactory(
    private val container: AppContainer
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return TerritoryRunViewModel(
            repository = container.gameRepository,
            locationClient = container.locationClient,
            validator = container.routeValidator
        ) as T
    }
}

