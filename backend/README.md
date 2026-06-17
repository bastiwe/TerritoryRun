# Territory Run Backend

Small JVM backend for Territory Run routing experiments.

## Local run

```powershell
.\gradlew.bat :backend:run
```

Health check:

```http
GET http://localhost:8080/health
```

Demo loop:

```http
POST http://localhost:8080/routing/demo-loop
Content-Type: application/json

{
  "start": {
    "latitude": 52.52,
    "longitude": 13.405,
    "timestampMillis": 1781690000000,
    "accuracyMeters": 8
  },
  "targetDistanceMeters": 800,
  "targetDurationMillis": 720000
}
```

## Valhalla plan

Keep this backend API stable and replace `FakeRoutingProvider` with a `ValhallaRoutingProvider`.

Target architecture:

```text
Android app -> Territory Run backend -> Valhalla -> OpenStreetMap tiles
```

Do not call Valhalla directly from the Android app in production.

## Windows hosting

For local Windows hosting with Android emulator or a real phone in the same WLAN, see:

```text
docs/windows-local-hosting.md
```

Docker Desktop can be used with `docker-compose.local.yml` after running:

```powershell
.\gradlew.bat :backend:installDist
docker compose -f docker-compose.local.yml up --build
```
