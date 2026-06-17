# Territory Run lokal auf Windows hosten

Diese Variante ist fuer den MVP-Test gedacht. Spaeter kann derselbe Aufbau auf einen VPS migriert werden.

## Empfehlung

Starte zuerst nur das Territory-Run-Backend lokal. Valhalla kommt danach als eigener Docker-Container dazu, sobald der echte `ValhallaRoutingProvider` im Backend aktiviert ist.

```text
Android App -> Windows-PC Backend -> spaeter Valhalla Container
```

## Variante A: Backend direkt per Gradle starten

Im Projektordner:

```powershell
.\gradlew.bat :backend:run
```

Health-Check:

```powershell
Invoke-RestMethod http://localhost:8080/health
```

Android-Emulator:

```properties
ROUTING_BASE_URL=http://10.0.2.2:8080
```

Echtes Smartphone im gleichen WLAN:

```properties
ROUTING_BASE_URL=http://DEINE-PC-IP:8080
```

Die PC-IP findest du mit:

```powershell
ipconfig
```

Nimm die IPv4-Adresse deines WLAN- oder LAN-Adapters, zum Beispiel `192.168.178.42`.

## Variante B: Backend mit Docker Desktop starten

Voraussetzungen:

- Docker Desktop fuer Windows
- WSL2 aktiviert
- Projekt liegt in einem Windows-Ordner, auf den Docker zugreifen darf
- Java 17 oder neuer in der PowerShell aktiv

Erst das Backend-Paket bauen:

```powershell
$env:JAVA_HOME='C:\Users\paule\.jdks\jbr-17.0.14'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat :backend:installDist
```

Dann den Container starten:

```powershell
docker compose -f docker-compose.local.yml up --build
```

Health-Check:

```powershell
Invoke-RestMethod http://localhost:8080/health
```

Stoppen:

```powershell
docker compose -f docker-compose.local.yml down
```

## Fehler: Gradle nutzt Java 8

Wenn diese Meldung erscheint:

```text
Gradle requires JVM 17 or later to run. Your build is currently configured to use JVM 8.
```

dann nutzt deine PowerShell noch das alte Oracle Java 8. Setze fuer die aktuelle PowerShell-Session das JDK von Android Studio:

```powershell
$env:JAVA_HOME='C:\Users\paule\.jdks\jbr-17.0.14'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
java -version
```

Danach sollte `java -version` Java 17 anzeigen und Gradle laeuft:

```powershell
.\gradlew.bat :backend:installDist
```

## Windows-Firewall

Wenn du mit einem echten Smartphone testest, muss dein Handy den Port `8080` auf deinem PC erreichen koennen.

Erlaube eingehende Verbindungen fuer:

```text
TCP 8080
```

Nur im privaten Heimnetz freigeben, nicht oeffentlich.

## Spaetere VPS-Migration

Auf dem VPS bleibt die Idee gleich:

```text
Android App -> HTTPS -> Territory Run Backend -> internes Valhalla
```

Unterschiede auf dem VPS:

- Domain statt lokaler IP
- HTTPS ueber Caddy oder Traefik
- Firewall nur fuer `22`, `80`, `443`
- Valhalla-Port bleibt intern und wird nicht direkt ins Internet geoeffnet

## Valhalla mit Münster-Testregion

Der lokale Docker-Compose startet auch Valhalla mit dem Münster-Extract von BBBike. Details stehen hier:

```text
docs/valhalla-muenster.md
```
