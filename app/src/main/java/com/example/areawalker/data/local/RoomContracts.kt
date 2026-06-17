package com.example.areawalker.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import com.example.areawalker.domain.model.Team

@Entity(tableName = "territories")
data class TerritoryEntity(
    @PrimaryKey val id: String,
    val team: Team,
    val polygonJson: String,
    val areaSquareMeters: Double,
    val capturedAtMillis: Long
)

@Entity(tableName = "track_sessions")
data class TrackSessionEntity(
    @PrimaryKey val id: String,
    val playerId: String,
    val team: Team,
    val startedAtMillis: Long,
    val endedAtMillis: Long?,
    val pointsJson: String,
    val valid: Boolean
)

@Dao
interface TerritoryRunDao {
    @Query("SELECT * FROM territories")
    suspend fun territories(): List<TerritoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTerritory(entity: TerritoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(entity: TrackSessionEntity)
}
