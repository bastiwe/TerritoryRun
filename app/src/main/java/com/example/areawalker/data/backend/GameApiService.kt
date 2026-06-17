package com.example.areawalker.data.backend

import com.example.areawalker.domain.model.GameStats
import com.example.areawalker.domain.model.LeaderboardEntry
import com.example.areawalker.domain.model.Player
import com.example.areawalker.domain.model.Team
import com.example.areawalker.domain.model.Territory
import com.example.areawalker.domain.model.TrackSession
import kotlinx.coroutines.flow.Flow

interface GameApiService {
    val territories: Flow<List<Territory>>
    val player: Flow<Player?>

    suspend fun upsertPlayer(player: Player)
    suspend fun selectTeam(playerId: String, team: Team): Player
    suspend fun submitSession(session: TrackSession): SubmitSessionResult
    suspend fun stats(playerId: String): GameStats
    suspend fun leaderboard(): List<LeaderboardEntry>
}

data class SubmitSessionResult(
    val accepted: Boolean,
    val session: TrackSession,
    val territory: Territory?,
    val awardedXp: Int,
    val message: String
)

