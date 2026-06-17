# Territory Run

Territory Run is an Android-native geo game MVP built with Kotlin, Jetpack Compose and Material 3.

Players sign in, choose one of three teams and capture real-world territory by recording closed walking or jogging loops. The MVP runs locally with a fake Play Games login and an in-memory backend so it is playable without a server.

## Getting started

1. Open this folder in Android Studio.
2. Let Gradle sync the project.
3. Run the `app` configuration on an emulator or Android device.

## MVP features

- Splash/login flow with a Play Games Services v2 integration point
- Team selection: Rot, Blau, Grün
- Territory map fallback drawn in Compose
- GPS tracking client using `FusedLocationProviderClient`
- Foreground service declaration for active recording
- Session validation with anti-cheat checks
- Haversine distance, polygon area, bounding box and self-intersection checks
- XP, levels, daily missions, streak, achievements and local leaderboard
- Backend interfaces plus `InMemoryGameApiService`
- DataStore settings
- Room database, entity and DAO contracts for the future local cache

## External integration placeholders

- `app/src/main/res/values/strings.xml`
  - Replace `play_games_app_id` with the real Play Games Services game id.
- `FakePlayGamesAuthService`
  - Replace the fake sign-in with Play Games Services v2 and backend auth-code exchange.
- `GameApiService`
  - Replace `InMemoryGameApiService` with a real server client.
- `TerritoryMap`
  - Replace the Compose fallback renderer with an OSMDroid, MapLibre or Google Maps Compose adapter.

## Project layout

- `app/src/main/java/com/example/areawalker`: Android app source code
- `domain`: game models, geo algorithms and validation rules
- `data`: auth, backend, local cache contracts, location and repositories
- `services`: Android foreground tracking service
- `ui`: Compose screens, components, state and theme
- `app/src/main/res`: Android resources
- `build.gradle.kts`: shared build configuration
- `settings.gradle.kts`: Gradle project settings
