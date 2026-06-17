# Valhalla-Testregion Münster

Für lokale Tests nutzt Territory Run den freien Münster-Extract von BBBike:

```text
https://download.bbbike.org/osm/bbbike/Muenster/Muenster.osm.pbf
```

Der Extract ist klein genug für Docker Desktop auf einem Windows-PC und wird von Valhalla beim ersten Start in Routing-Tiles umgewandelt.

## Start

Im Projektordner:

```powershell
$env:JAVA_HOME='C:\Users\paule\.jdks\jbr-17.0.14'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat :backend:installDist
docker compose -f docker-compose.local.yml up --build
```

Beim ersten Start lädt Valhalla den Münster-PBF-Extract herunter und baut daraus Routing-Daten. Das kann einige Minuten dauern.

## Prüfen

Backend:

```powershell
Invoke-RestMethod http://localhost:8080/health
```

Erwartung:

```json
{
  "status": "ok",
  "routingProvider": "valhalla"
}
```

Valhalla direkt:

```powershell
Invoke-RestMethod http://localhost:8002/status
```

Falls der Status-Endpunkt je nach Valhalla-Version nicht antwortet, sind die Logs aussagekräftiger:

```powershell
docker logs -f territory-run-valhalla
```

## Wichtig für Tests

Die App kann nur echte Valhalla-Routen erzeugen, wenn dein Startpunkt innerhalb des Münster-Datensatzes liegt. Wenn du außerhalb von Münster testest, fällt das Backend bewusst auf den lokalen Fake-Provider zurück.

Für echte Tests in Münster:

```properties
ROUTING_BASE_URL=http://192.168.178.32:8080
```

bleibt in `local.properties` korrekt, solange dein PC diese IP im WLAN hat.

## Map-Matching

Das Backend bietet zusätzlich:

```http
POST /routing/match
```

Die Android-App sendet nach dem Stoppen einer echten Tracking-Session die aufgenommenen GPS-Rohpunkte an diesen Endpunkt. Valhalla nutzt `trace_route` mit `shape_match: map_snap` und `costing: pedestrian`, um die Route auf laufbare OpenStreetMap-Wege zu legen.

Wichtig:

- Die Rohpunkte bleiben in der App als `rawPoints` erhalten.
- Die gesnappte Route wird für Anzeige, Distanz, Fläche und Session-Ergebnis verwendet.
- Wenn Valhalla nicht erreichbar ist oder die Route außerhalb von Münster liegt, verwendet die App automatisch die Roh-GPS-Route.
- Für Anti-Cheat sollten später serverseitig immer Rohpunkte und gesnappte Route gemeinsam bewertet werden.

Nach Code-Änderungen am Backend musst du den Container neu bauen:

```powershell
$env:JAVA_HOME='C:\Users\paule\.jdks\jbr-17.0.14'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat :backend:installDist
docker compose -f docker-compose.local.yml up --build
```

## Daten aktualisieren

Valhalla speichert die gebauten Daten lokal unter:

```text
valhalla-data/muenster
```

Der Ordner ist absichtlich nicht im Git-Repo. Wenn du den Graph komplett neu bauen willst:

```powershell
docker compose -f docker-compose.local.yml down
Remove-Item -Recurse -Force .\valhalla-data\muenster
docker compose -f docker-compose.local.yml up --build
```

Vorsicht: Das löscht nur die lokalen Routing-Daten, nicht den App-Code.
