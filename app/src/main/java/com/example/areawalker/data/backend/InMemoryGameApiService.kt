package com.example.areawalker.data.backend

import com.example.areawalker.domain.geo.GeoAlgorithms
import com.example.areawalker.domain.model.Achievement
import com.example.areawalker.domain.model.GameStats
import com.example.areawalker.domain.model.LeaderboardEntry
import com.example.areawalker.domain.model.Mission
import com.example.areawalker.domain.model.Player
import com.example.areawalker.domain.model.Team
import com.example.areawalker.domain.model.Territory
import com.example.areawalker.domain.model.TrackSession
import com.example.areawalker.domain.rules.BackendValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID
import kotlin.math.sqrt

class InMemoryGameApiService(
    private val backendValidator: BackendValidator = BackendValidator()
) : GameApiService {
    private val territoryState = MutableStateFlow(seedTerritories())
    private val playerState = MutableStateFlow<Player?>(null)
    private val sessions = mutableListOf<TrackSession>()

    override val territories: Flow<List<Territory>> = territoryState
    override val player: Flow<Player?> = playerState

    override suspend fun upsertPlayer(player: Player) {
        playerState.value = playerState.value?.let { current ->
            current.copy(id = player.id, displayName = player.displayName)
        } ?: player
    }

    override suspend fun selectTeam(playerId: String, team: Team): Player {
        val updated = (playerState.value ?: Player(playerId, "Local Runner")).copy(team = team)
        playerState.value = updated
        return updated
    }

    override suspend fun submitSession(session: TrackSession): SubmitSessionResult {
        val serverValidation = backendValidator.validateSubmittedTrack(session.points)
        val validatedSession = session.copy(validation = serverValidation)
        sessions += validatedSession
        if (!serverValidation.isValid) {
            return SubmitSessionResult(false, validatedSession, null, 0, serverValidation.reasons.joinToString())
        }

        val polygon = serverValidation.filteredPoints
        val area = GeoAlgorithms.approximatePolygonAreaSquareMeters(polygon)
        val territory = Territory(
            id = UUID.randomUUID().toString(),
            team = session.team,
            polygon = polygon,
            areaSquareMeters = area,
            capturedAtMillis = System.currentTimeMillis(),
            capturedByPlayerId = session.playerId,
            sourceSessionId = session.id,
            boundingBox = GeoAlgorithms.boundingBox(polygon)
        )
        territoryState.update { existing ->
            // MVP contest rule: new valid territory replaces overlapping territories by bounding-box overlap.
            existing.filterNot { overlaps(it, territory) } + territory
        }
        val xp = xpForArea(area)
        playerState.update { player ->
            player?.copy(
                xp = player.xp + xp,
                level = levelForXp(player.xp + xp),
                personalAreaSquareMeters = player.personalAreaSquareMeters + area,
                sessions = player.sessions + 1,
                streakDays = (player.streakDays + 1).coerceAtMost(365),
                achievements = achievementsFor(player.personalAreaSquareMeters + area, player.sessions + 1)
            )
        }
        return SubmitSessionResult(true, validatedSession, territory, xp, "Gebiet erobert")
    }

    override suspend fun stats(playerId: String): GameStats {
        val player = playerState.value ?: Player(playerId, "Local Runner")
        val teamAreas = Team.playable.associateWith { team ->
            territoryState.value.filter { it.team == team }.sumOf { it.areaSquareMeters }
        }
        return GameStats(
            player = player,
            teamAreas = teamAreas,
            missions = listOf(
                Mission("daily-loop", "Einen Rundgang abschließen", 1, if (player.sessions > 0) 1 else 0, 100, player.sessions > 0),
                Mission("daily-area", "2.000 m² für dein Team sichern", 2_000, player.personalAreaSquareMeters.toInt(), 150, player.personalAreaSquareMeters >= 2_000)
            ),
            leaderboard = leaderboard()
        )
    }

    override suspend fun leaderboard(): List<LeaderboardEntry> {
        val player = playerState.value ?: Player("local-player", "Local Runner", Team.Green)
        return listOf(
            LeaderboardEntry(1, player.displayName, player.team ?: Team.Green, player.personalAreaSquareMeters, player.xp),
            LeaderboardEntry(2, "Mira", Team.Red, 8_800.0, 740),
            LeaderboardEntry(3, "Jonas", Team.Blue, 6_400.0, 620)
        ).sortedByDescending { it.areaSquareMeters }.mapIndexed { index, entry -> entry.copy(rank = index + 1) }
    }

    private fun xpForArea(area: Double): Int = (50 + sqrt(area).toInt()).coerceAtMost(500)
    private fun levelForXp(xp: Int): Int = 1 + xp / 500

    private fun achievementsFor(area: Double, sessions: Int): List<Achievement> = listOf(
        Achievement("first-loop", "Erster Rundgang", "Schließe deine erste gültige Route.", sessions > 0),
        Achievement("one-hectare", "Hektar-Held", "Erobere insgesamt 10.000 m2.", area >= 10_000)
    )

    private fun overlaps(a: Territory, b: Territory): Boolean =
        a.boundingBox.minLat <= b.boundingBox.maxLat &&
            a.boundingBox.maxLat >= b.boundingBox.minLat &&
            a.boundingBox.minLon <= b.boundingBox.maxLon &&
            a.boundingBox.maxLon >= b.boundingBox.minLon

    private fun seedTerritories(): List<Territory> {
        val now = System.currentTimeMillis()
        fun p(lat: Double, lon: Double) = com.example.areawalker.domain.model.GpsPoint(lat, lon, now, 8f)
        val red = listOf(p(52.5200, 13.4040), p(52.5209, 13.4052), p(52.5202, 13.4064), p(52.5195, 13.4051), p(52.5200, 13.4040))
        val blue = listOf(p(52.5185, 13.4020), p(52.5193, 13.4031), p(52.5188, 13.4045), p(52.5178, 13.4034), p(52.5185, 13.4020))
        return listOf(
            Territory("seed-red", Team.Red, red, GeoAlgorithms.approximatePolygonAreaSquareMeters(red), now, "seed", "seed", GeoAlgorithms.boundingBox(red)),
            Territory("seed-blue", Team.Blue, blue, GeoAlgorithms.approximatePolygonAreaSquareMeters(blue), now, "seed", "seed", GeoAlgorithms.boundingBox(blue))
        )
    }
}
