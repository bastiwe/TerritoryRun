package com.example.areawalker.data.auth

import android.app.Activity
import com.example.areawalker.domain.model.Player

interface AuthService {
    suspend fun signIn(activity: Activity): Player
    suspend fun signOut()
}

class FakePlayGamesAuthService : AuthService {
    override suspend fun signIn(activity: Activity): Player {
        // TODO: Replace with Play Games Services v2:
        // PlayGames.getPlayersClient(activity).currentPlayer and server auth code handoff.
        return Player(id = "local-player", displayName = "Local Runner")
    }

    override suspend fun signOut() = Unit
}

