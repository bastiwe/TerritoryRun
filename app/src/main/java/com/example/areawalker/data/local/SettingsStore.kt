package com.example.areawalker.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.areawalker.domain.model.Team
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore("territory_run_settings")

class SettingsStore(private val context: Context) {
    private val selectedTeam = stringPreferencesKey("selected_team")

    val selectedTeamFlow: Flow<Team?> = context.settingsDataStore.data.map { prefs ->
        prefs[selectedTeam]?.let { Team.valueOf(it) }
    }

    suspend fun setSelectedTeam(team: Team) {
        context.settingsDataStore.edit { it[selectedTeam] = team.name }
    }
}

