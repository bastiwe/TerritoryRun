# Online-Multiplayer-Fahrplan

Territory Run soll schrittweise vom lokalen MVP zu einem serverautoritativen Online-Spiel werden.

## Grundsatz

Die Android-App sammelt Daten und zeigt den Spielzustand an. Das Backend entscheidet serverseitig, ob eine Session gültig ist, wem ein Gebiet gehört und wie viele XP vergeben werden.

```text
Android App
  -> HTTPS API
      -> Auth / Player / Sessions / Territories / Leaderboard
      -> Server-Validator
      -> Territory-Engine
      -> PostgreSQL + PostGIS
      -> Valhalla
```

## Backend

Das aktuelle kleine JVM-Backend wird später zu einem echten API-Backend ausgebaut. Favorit für das Projekt ist Ktor, weil es Kotlin-nativ und schlank ist.

Geplante Bereiche:

- Auth: Google Play Games Token prüfen
- Players: Profil, Team, XP, Level
- Sessions: GPS-Rohpunkte hochladen
- Validation: Anti-Cheat und Valhalla Map-Matching
- Territories: Gebiete speichern, überschreiben und contesten
- Leaderboard: Spieler- und Team-Rankings
- Missions: tägliche Missionen und Streaks

## Datenbank

Für Multiplayer wird PostgreSQL mit PostGIS benötigt.

Geplante Tabellen:

- players
- teams
- track_sessions
- gps_points
- territories
- territory_events
- achievements
- missions
- leaderboard_snapshots

PostGIS wird benötigt für Polygone, Überschneidungen, Flächenberechnung, Gebietszuschnitt und Team-Flächen.

## Online-Session-Ablauf

```text
App startet Session lokal
-> sammelt GPS-Rohpunkte
-> Stop
-> App sendet Rohpunkte ans Backend
-> Backend map-matched mit Valhalla
-> Backend validiert Anti-Cheat
-> Backend bildet Polygon
-> Backend prüft Überschneidungen
-> Backend schreibt Territory-Event
-> Backend aktualisiert Player/Team-Stats
-> App bekommt Ergebnis zurück
```

## Gebietslogik MVP

Für den ersten Online-Multiplayer-Stand:

- neue gültige Fläche überschreibt bestehende Fläche
- Überschneidungen mit anderen Teams werden dem neuen Team zugeschrieben
- eigene Teamflächen werden zusammengeführt
- neutrale Fläche wird normal übernommen

Spätere Erweiterungen:

- umkämpfte Gebiete
- mehrere Runs zum Erobern
- Verteidigungsbonus
- Cooldowns gegen Spam
- Team-Events und Push-Nachrichten

## Synchronisation

Start einfach:

- Territories beim App-Start laden
- Territories alle 30 bis 60 Sekunden aktualisieren
- Leaderboard beim Öffnen laden

Später:

- WebSockets oder Server-Sent Events
- Push für wichtige Team-Ereignisse

## Hosting-Ziel

```text
Android App
  -> HTTPS
      -> Territory Run Backend
          -> PostgreSQL/PostGIS
          -> internes Valhalla
```

Docker-Services auf VPS:

- backend
- postgres/postgis
- valhalla
- caddy oder traefik für HTTPS

## Nächste Umsetzungsschritte

1. Docker Compose um PostgreSQL/PostGIS erweitern.
2. Backend von Mini-HttpServer auf Ktor migrieren.
3. API-Endpunkte anlegen:
   - GET /health
   - POST /auth/play-games
   - GET /player/me
   - POST /player/team
   - POST /sessions
   - GET /territories
   - GET /leaderboard
4. Android-Repositories onlinefähig machen.
5. Erste serverseitige Session-Validierung einbauen.

