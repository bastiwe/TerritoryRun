package com.example.areawalker.core

import android.app.Application

class TerritoryRunApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

