package com.example.areawalker.data.repository

import android.app.Activity
import com.example.areawalker.data.auth.AuthService
import com.example.areawalker.data.backend.GameApiService
import com.example.areawalker.data.backend.SubmitSessionResult
import com.example.areawalker.data.local.SettingsStore
import com.example.areawalker.domain.model.GameStats
import com.example.areawalker.domain.model.Player
import com.example.areawalker.domain.model.Team
import com.example.areawalker.domain.model.Territory
import com.example.areawalker.domain.model.TrackSession
import kotlinx.coroutines.flow.Flow

class GameRepository(
    private val authService: AuthService,
    private val api: GameApiService,
    private val settingsStore: SettingsStore
) {
    val player: Flow<Player?> = api.player
    val territories: Flow<List<Territory>> = api.territories

    suspend fun signIn(activity: Activity): Player {
        val player = authService.signIn(activity)
        api.upsertPlayer(player)
        return player
    }

    suspend fun selectTeam(playerId: String, team: Team): Player {
        settingsStore.setSelectedTeam(team)
        return api.selectTeam(playerId, team)
    }

    suspend fun submitSession(session: TrackSession): SubmitSessionResult = api.submitSession(session)
    suspend fun stats(playerId: String): GameStats = api.stats(playerId)
}

