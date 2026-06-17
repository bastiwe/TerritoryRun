package com.example.areawalker.ui.screens

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.areawalker.domain.model.Team
import com.example.areawalker.ui.components.StatTile
import com.example.areawalker.ui.components.TeamPill
import com.example.areawalker.ui.components.TerritoryMap
import com.example.areawalker.ui.components.area
import com.example.areawalker.ui.components.duration
import com.example.areawalker.ui.components.meters
import com.example.areawalker.ui.components.teamColor
import com.example.areawalker.ui.state.AppScreen
import com.example.areawalker.ui.state.TerritoryRunUiState

@Composable
fun TerritoryRunRoot(
    state: TerritoryRunUiState,
    onSignIn: (Activity) -> Unit,
    onTeam: (Team) -> Unit,
    onNavigate: (AppScreen) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onDemoRun: () -> Unit
) {
    val context = LocalContext.current
    GameScaffold(state, onNavigate) {
        when (state.screen) {
            AppScreen.SplashLogin -> LoginScreen(state) { onSignIn(context as Activity) }
            AppScreen.TeamSelect -> TeamSelectScreen(onTeam)
            AppScreen.Map -> MapScreen(state, onNavigate, onStart, onDemoRun)
            AppScreen.Tracking -> TrackingScreen(state, onStop, onDemoRun)
            AppScreen.SessionResult -> SessionResultScreen(state, onNavigate)
            AppScreen.Profile -> ProfileScreen(state)
            AppScreen.Leaderboard -> LeaderboardScreen(state)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GameScaffold(
    state: TerritoryRunUiState,
    onNavigate: (AppScreen) -> Unit,
    content: @Composable () -> Unit
) {
    val showNav = state.player?.team != null
    Scaffold(
        containerColor = Color(0xFF0D1117),
        bottomBar = {
            if (showNav) {
                NavigationBar(containerColor = Color(0xFF121821)) {
                    listOf(AppScreen.Map, AppScreen.Tracking, AppScreen.Profile, AppScreen.Leaderboard).forEach { screen ->
                        NavigationBarItem(
                            selected = state.screen == screen,
                            onClick = { onNavigate(screen) },
                            icon = {},
                            label = { Text(screen.label()) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(gameBackground()),
            color = Color.Transparent
        ) {
            content()
        }
    }
}

@Composable
private fun LoginScreen(state: TerritoryRunUiState, onSignIn: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Territory Run", fontSize = 42.sp, fontWeight = FontWeight.Black, color = Color.White)
        Text("Laufe echte Routen. Schließe Gebiete. Gewinne die Stadt für dein Team.", color = Color(0xFFB8C2D6), fontSize = 18.sp)
        Spacer(Modifier.height(28.dp))
        Button(onClick = onSignIn, enabled = !state.loading, colors = primaryButtonColors()) {
            Text(if (state.loading) "Verbinde..." else "Mit Play Games anmelden")
        }
        Spacer(Modifier.height(12.dp))
        Text("MVP nutzt lokal einen Fake-Login. TODO: Play Games Services v2 Server-Auth-Code anbinden.", color = Color(0xFF7F8EA3), fontSize = 13.sp)
    }
}

@Composable
private fun TeamSelectScreen(onTeam: (Team) -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("Wähle dein Team", fontSize = 34.sp, fontWeight = FontWeight.Black, color = Color.White)
        Text("Diese Wahl wird serverseitig fixiert, sobald ein echtes Backend aktiv ist.", color = Color(0xFFB8C2D6))
        Spacer(Modifier.height(22.dp))
        Team.playable.forEach { team ->
            Button(
                modifier = Modifier.fillMaxWidth().height(56.dp),
                onClick = { onTeam(team) },
                colors = ButtonDefaults.buttonColors(containerColor = teamColor(team))
            ) {
                Text("Team ${team.displayName}", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun MapScreen(
    state: TerritoryRunUiState,
    onNavigate: (AppScreen) -> Unit,
    onStart: () -> Unit,
    onDemoRun: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Header(state)
        Spacer(Modifier.height(12.dp))
        TerritoryMap(state.territories, state.currentPoints, Modifier.weight(1f).fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        Row {
            Button(modifier = Modifier.weight(1f), onClick = onStart, colors = primaryButtonColors()) { Text("GPS-Run starten") }
            Spacer(Modifier.width(10.dp))
            Button(modifier = Modifier.weight(1f), onClick = onDemoRun, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFACC15), contentColor = Color(0xFF121212))) {
                Text("Demo-Run")
            }
        }
        Spacer(Modifier.height(10.dp))
        Button(modifier = Modifier.fillMaxWidth(), onClick = { onNavigate(AppScreen.Profile) }) { Text("Profil und Team-Stats") }
    }
}

@Composable
private fun TrackingScreen(state: TerritoryRunUiState, onStop: () -> Unit, onDemoRun: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Live Tracking", fontSize = 30.sp, fontWeight = FontWeight.Black, color = Color.White)
        Spacer(Modifier.height(12.dp))
        TerritoryMap(state.territories, state.currentPoints, Modifier.weight(1f).fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("Distanz", meters(state.distanceMeters), Modifier.weight(1f))
            StatTile("Zeit", duration(state.elapsedMillis), Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("Speed", "${state.currentSpeedKmh.toInt()} km/h", Modifier.weight(1f))
            StatTile("GPS", state.gpsQuality, Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Button(modifier = Modifier.fillMaxWidth().height(54.dp), onClick = onStop, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE5484D))) { Text("Stop & auswerten") }
        Spacer(Modifier.height(8.dp))
        Button(modifier = Modifier.fillMaxWidth(), onClick = onDemoRun) { Text("Gültige Demo-Session erzeugen") }
    }
}

@Composable
private fun SessionResultScreen(state: TerritoryRunUiState, onNavigate: (AppScreen) -> Unit) {
    val session = state.lastSession
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.Center) {
        Text(if (session?.validation?.isValid == true) "Gebiet erobert" else "Route abgelehnt", fontSize = 34.sp, fontWeight = FontWeight.Black, color = Color.White)
        Spacer(Modifier.height(12.dp))
        Text(state.lastMessage, color = Color(0xFFB8C2D6))
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("Fläche", area(session?.areaSquareMeters ?: 0.0), Modifier.weight(1f))
            StatTile("Distanz", meters(session?.distanceMeters ?: 0.0), Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("Punkte", "${session?.points?.size ?: 0}", Modifier.weight(1f))
            StatTile("XP", "+${state.lastAwardedXp}", Modifier.weight(1f))
        }
        if (session?.validation?.reasons?.isNotEmpty() == true) {
            Spacer(Modifier.height(14.dp))
            Text(session.validation.reasons.joinToString("\n"), color = Color(0xFFFFB4AB))
        }
        Spacer(Modifier.height(20.dp))
        Button(modifier = Modifier.fillMaxWidth(), onClick = { onNavigate(AppScreen.Map) }, colors = primaryButtonColors()) { Text("Zur Karte") }
    }
}

@Composable
private fun ProfileScreen(state: TerritoryRunUiState) {
    val player = state.stats?.player ?: state.player
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Profil", fontSize = 32.sp, fontWeight = FontWeight.Black, color = Color.White)
            Spacer(Modifier.height(8.dp))
            player?.team?.let { TeamPill("Team ${it.displayName}", teamColor(it)) }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("Level", "${player?.level ?: 1}", Modifier.weight(1f))
                StatTile("XP", "${player?.xp ?: 0}", Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("Eigene Fläche", area(player?.personalAreaSquareMeters ?: 0.0), Modifier.weight(1f))
                StatTile("Streak", "${player?.streakDays ?: 0} Tage", Modifier.weight(1f))
            }
        }
        item { Text("Team-Flächen", color = Color.White, fontWeight = FontWeight.Bold) }
        items(state.stats?.teamAreas?.toList() ?: emptyList()) { (team, value) ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TeamPill(team.displayName, teamColor(team))
                Text(area(value), color = Color.White)
            }
        }
        item { Text("Missionen", color = Color.White, fontWeight = FontWeight.Bold) }
        items(state.stats?.missions ?: emptyList()) { mission ->
            Text("${mission.title}: ${mission.progress}/${mission.target} (+${mission.rewardXp} XP)", color = if (mission.completed) Color(0xFF86EFAC) else Color(0xFFB8C2D6))
        }
        item { Text("Badges", color = Color.White, fontWeight = FontWeight.Bold) }
        items(player?.achievements ?: emptyList()) { achievement ->
            Text("${if (achievement.unlocked) "Unlocked" else "Locked"} - ${achievement.title}", color = Color(0xFFB8C2D6))
        }
    }
}

@Composable
private fun LeaderboardScreen(state: TerritoryRunUiState) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("Leaderboard", fontSize = 32.sp, fontWeight = FontWeight.Black, color = Color.White) }
        items(state.stats?.leaderboard ?: emptyList()) { entry ->
            Surface(color = Color.White.copy(alpha = 0.08f), shape = MaterialTheme.shapes.small) {
                Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("#${entry.rank} ${entry.playerName}", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("${area(entry.areaSquareMeters)} / ${entry.xp} XP", color = teamColor(entry.team))
                }
            }
        }
    }
}

@Composable
private fun Header(state: TerritoryRunUiState) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column {
            Text("Territory Run", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black)
            Text("Kontrolliere die Karte durch Rundgänge", color = Color(0xFFB8C2D6))
        }
        state.selectedTeam?.let { TeamPill(it.displayName, teamColor(it)) }
    }
}

private fun AppScreen.label(): String = when (this) {
    AppScreen.Map -> "Karte"
    AppScreen.Tracking -> "Run"
    AppScreen.Profile -> "Profil"
    AppScreen.Leaderboard -> "Rang"
    else -> name
}

@Composable
private fun primaryButtonColors() = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E), contentColor = Color(0xFF07130C))

private fun gameBackground(): Brush = Brush.verticalGradient(listOf(Color(0xFF0D1117), Color(0xFF111827), Color(0xFF0B1412)))
