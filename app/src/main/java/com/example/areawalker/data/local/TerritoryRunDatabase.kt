package com.example.areawalker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.areawalker.domain.model.Team

@Database(
    entities = [
        TerritoryEntity::class,
        TrackSessionEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(TerritoryRunConverters::class)
abstract class TerritoryRunDatabase : RoomDatabase() {
    abstract fun dao(): TerritoryRunDao
}

class TerritoryRunConverters {
    @TypeConverter
    fun teamToString(team: Team): String = team.name

    @TypeConverter
    fun stringToTeam(value: String): Team = Team.valueOf(value)
}

