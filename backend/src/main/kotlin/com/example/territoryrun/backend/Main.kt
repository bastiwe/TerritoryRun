package com.example.territoryrun.backend

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.URL
import java.nio.charset.StandardCharsets
import kotlin.math.cos

data class GpsPointDto(
    val latitude: Double,
    val longitude: Double,
    val timestampMillis: Long,
    val accuracyMeters: Float = 8f
)

data class DemoLoopRequest(
    val start: GpsPointDto,
    val targetDistanceMeters: Double,
    val targetDurationMillis: Long
)

interface RoutingProvider {
    fun createDemoLoop(request: DemoLoopRequest): List<GpsPointDto>
    fun matchRoute(points: List<GpsPointDto>): List<GpsPointDto> = points
}

class FakeRoutingProvider : RoutingProvider {
    override fun createDemoLoop(request: DemoLoopRequest): List<GpsPointDto> {
        // TODO: Replace with Valhalla pedestrian routing. Keep this contract stable for the Android app.
        val sideMeters = request.targetDistanceMeters / 4.0
        val latOffset = sideMeters / 111_320.0
        val lonOffset = sideMeters / (111_320.0 * cos(Math.toRadians(request.start.latitude))).coerceAtLeast(0.1)
        val corners = listOf(
            request.start.latitude to request.start.longitude,
            request.start.latitude + latOffset to request.start.longitude,
            request.start.latitude + latOffset to request.start.longitude + lonOffset,
            request.start.latitude to request.start.longitude + lonOffset,
            request.start.latitude to request.start.longitude
        )
        val points = mutableListOf<GpsPointDto>()
        val segments = corners.zipWithNext()
        val pointsPerSegment = 10
        val intervalMillis = request.targetDurationMillis / (segments.size * pointsPerSegment)
        segments.forEachIndexed { segmentIndex, (a, b) ->
            repeat(pointsPerSegment) { step ->
                val ratio = step / pointsPerSegment.toDouble()
                points += GpsPointDto(
                    latitude = a.first + (b.first - a.first) * ratio,
                    longitude = a.second + (b.second - a.second) * ratio,
                    timestampMillis = request.start.timestampMillis + (segmentIndex * pointsPerSegment + step) * intervalMillis
                )
            }
        }
        points += request.start.copy(timestampMillis = request.start.timestampMillis + request.targetDurationMillis)
        return points
    }
}

class ValhallaRoutingProvider(
    private val baseUrl: String,
    private val fallback: RoutingProvider = FakeRoutingProvider()
) : RoutingProvider {
    override fun createDemoLoop(request: DemoLoopRequest): List<GpsPointDto> =
        runCatching {
            val routeRequest = routeRequestJson(request)
            val response = postJson("$baseUrl/route", routeRequest)
            val coordinates = response.extractGeoJsonCoordinates()
            require(coordinates.size >= 2) { "Valhalla returned no route geometry" }
            coordinates.distributeTimestamps(request)
        }.getOrElse { error ->
            println("Valhalla routing failed, falling back to fake route: ${error.message}")
            fallback.createDemoLoop(request)
        }

    override fun matchRoute(points: List<GpsPointDto>): List<GpsPointDto> {
        if (points.size < 2) return points
        return runCatching {
            val request = mapMatchingRequestJson(points)
            val response = postJson("$baseUrl/trace_route", request)
            val coordinates = response.extractGeoJsonCoordinates()
            require(coordinates.size >= 2) { "Valhalla returned no matched geometry" }
            coordinates.distributeTimestamps(points.first().timestampMillis, points.last().timestampMillis, points.first().accuracyMeters)
        }.getOrElse { error ->
            println("Valhalla map matching failed, returning raw GPS route: ${error.message}")
            points
        }
    }

    private fun routeRequestJson(request: DemoLoopRequest): String {
        val sideMeters = request.targetDistanceMeters / 4.0
        val latOffset = sideMeters / 111_320.0
        val lonOffset = sideMeters / (111_320.0 * cos(Math.toRadians(request.start.latitude))).coerceAtLeast(0.1)
        val points = listOf(
            request.start.latitude to request.start.longitude,
            request.start.latitude + latOffset to request.start.longitude,
            request.start.latitude + latOffset to request.start.longitude + lonOffset,
            request.start.latitude to request.start.longitude + lonOffset,
            request.start.latitude to request.start.longitude
        )
        val locations = points.mapIndexed { index, (lat, lon) ->
            val type = if (index == 0 || index == points.lastIndex) "break" else "through"
            """{"lat":$lat,"lon":$lon,"type":"$type","radius":120}"""
        }.joinToString(",")

        return """
            {
              "locations": [$locations],
              "costing": "pedestrian",
              "costing_options": {
                "pedestrian": {
                  "shortest": true,
                  "walking_speed": 5.5
                }
              },
              "directions_type": "none",
              "format": "osrm",
              "shape_format": "geojson",
              "units": "kilometers",
              "language": "de-DE"
            }
        """.trimIndent()
    }

    private fun mapMatchingRequestJson(points: List<GpsPointDto>): String {
        val shape = points.mapIndexed { index, point ->
            val type = if (index == 0 || index == points.lastIndex) ""","type":"break"""" else ""
            """{"lat":${point.latitude},"lon":${point.longitude},"time":${point.timestampMillis / 1000},"radius":${point.accuracyMeters.coerceIn(8f, 60f)}$type}"""
        }.joinToString(",")

        return """
            {
              "shape": [$shape],
              "shape_match": "map_snap",
              "costing": "pedestrian",
              "costing_options": {
                "pedestrian": {
                  "walking_speed": 5.5
                }
              },
              "directions_type": "none",
              "format": "osrm",
              "shape_format": "geojson",
              "use_timestamps": true,
              "trace_options": {
                "gps_accuracy": 25,
                "search_radius": 60,
                "breakage_distance": 200,
                "interpolation_distance": 10
              },
              "units": "kilometers",
              "language": "de-DE"
            }
        """.trimIndent()
    }

    private fun postJson(url: String, body: String): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 5_000
            readTimeout = 20_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
        }

        OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8).use { it.write(body) }

        val status = connection.responseCode
        val response = if (status in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream ?: connection.inputStream
        }.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }

        require(status in 200..299) { "Valhalla HTTP $status: $response" }
        return response
    }

    private fun String.extractGeoJsonCoordinates(): List<Pair<Double, Double>> {
        val geometryIndex = indexOf(""""geometry"""")
        require(geometryIndex >= 0) { "Missing OSRM geometry" }
        val coordinatesIndex = indexOf(""""coordinates"""", startIndex = geometryIndex)
        require(coordinatesIndex >= 0) { "Missing OSRM coordinates" }
        val start = indexOf('[', startIndex = coordinatesIndex)
        require(start >= 0) { "Missing coordinates array" }

        var depth = 0
        var end = -1
        for (index in start until length) {
            when (this[index]) {
                '[' -> depth++
                ']' -> {
                    depth--
                    if (depth == 0) {
                        end = index
                        break
                    }
                }
            }
        }
        require(end > start) { "Unclosed coordinates array" }

        val coordinatesJson = substring(start, end + 1)
        val pairRegex = Regex("""\[\s*(-?\d+(?:\.\d+)?)\s*,\s*(-?\d+(?:\.\d+)?)\s*]""")
        return pairRegex.findAll(coordinatesJson).map { match ->
            val lon = match.groupValues[1].toDouble()
            val lat = match.groupValues[2].toDouble()
            lat to lon
        }.toList()
    }

    private fun List<Pair<Double, Double>>.distributeTimestamps(request: DemoLoopRequest): List<GpsPointDto> {
        val intervalMillis = if (size <= 1) 0 else request.targetDurationMillis / (size - 1)
        return mapIndexed { index, (lat, lon) ->
            GpsPointDto(
                latitude = lat,
                longitude = lon,
                timestampMillis = request.start.timestampMillis + index * intervalMillis,
                accuracyMeters = request.start.accuracyMeters
            )
        }
    }

    private fun List<Pair<Double, Double>>.distributeTimestamps(
        startMillis: Long,
        endMillis: Long,
        accuracyMeters: Float
    ): List<GpsPointDto> {
        val duration = (endMillis - startMillis).coerceAtLeast(0L)
        val intervalMillis = if (size <= 1) 0 else duration / (size - 1)
        return mapIndexed { index, (lat, lon) ->
            GpsPointDto(
                latitude = lat,
                longitude = lon,
                timestampMillis = startMillis + index * intervalMillis,
                accuracyMeters = accuracyMeters
            )
        }
    }
}

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    val valhallaUrl = System.getenv("VALHALLA_URL")?.trim()?.trimEnd('/')
    val provider = if (valhallaUrl.isNullOrBlank()) {
        FakeRoutingProvider()
    } else {
        ValhallaRoutingProvider(valhallaUrl)
    }
    val server = HttpServer.create(InetSocketAddress("0.0.0.0", port), 0)

    server.createContext("/health") { exchange ->
        val routingProvider = if (valhallaUrl.isNullOrBlank()) "fake" else "valhalla"
        exchange.respond(200, """{"status":"ok","routingProvider":"$routingProvider"}""")
    }

    server.createContext("/routing/demo-loop") { exchange ->
        if (exchange.requestMethod != "POST") {
            exchange.respond(405, """{"error":"Method not allowed"}""")
            return@createContext
        }
        runCatching {
            val body = exchange.requestBody.readBytes().toString(StandardCharsets.UTF_8)
            provider.createDemoLoop(parseDemoLoopRequest(body))
        }.onSuccess { points ->
            exchange.respond(200, pointsToJson(points))
        }.onFailure { error ->
            exchange.respond(400, """{"error":"${error.message?.escapeJson() ?: "Invalid request"}"}""")
        }
    }

    server.createContext("/routing/match") { exchange ->
        if (exchange.requestMethod != "POST") {
            exchange.respond(405, """{"error":"Method not allowed"}""")
            return@createContext
        }
        runCatching {
            val body = exchange.requestBody.readBytes().toString(StandardCharsets.UTF_8)
            provider.matchRoute(parsePointsRequest(body))
        }.onSuccess { points ->
            exchange.respond(200, pointsToJson(points))
        }.onFailure { error ->
            exchange.respond(400, """{"error":"${error.message?.escapeJson() ?: "Invalid request"}"}""")
        }
    }

    server.executor = null
    server.start()
    println("Territory Run backend listening on http://0.0.0.0:$port")
}

private fun parseDemoLoopRequest(json: String): DemoLoopRequest =
    DemoLoopRequest(
        start = GpsPointDto(
            latitude = json.doubleValue("latitude"),
            longitude = json.doubleValue("longitude"),
            timestampMillis = json.longValue("timestampMillis"),
            accuracyMeters = json.doubleValue("accuracyMeters", default = 8.0).toFloat()
        ),
        targetDistanceMeters = json.doubleValue("targetDistanceMeters"),
        targetDurationMillis = json.longValue("targetDurationMillis")
    )

private fun parsePointsRequest(json: String): List<GpsPointDto> {
    val objectRegex = Regex("""\{[^{}]*"latitude"\s*:\s*-?\d+(?:\.\d+)?[^{}]*}""")
    return objectRegex.findAll(json).map { match ->
        val item = match.value
        GpsPointDto(
            latitude = item.doubleValue("latitude"),
            longitude = item.doubleValue("longitude"),
            timestampMillis = item.longValue("timestampMillis"),
            accuracyMeters = item.doubleValue("accuracyMeters", default = 8.0).toFloat()
        )
    }.toList().also { points ->
        require(points.isNotEmpty()) { "Missing points" }
    }
}

private fun pointsToJson(points: List<GpsPointDto>): String =
    points.joinToString(prefix = """{"points":[""", postfix = "]}") { point ->
        """{"latitude":${point.latitude},"longitude":${point.longitude},"timestampMillis":${point.timestampMillis},"accuracyMeters":${point.accuracyMeters}}"""
    }

private fun HttpExchange.respond(status: Int, body: String) {
    responseHeaders.add("Content-Type", "application/json; charset=utf-8")
    responseHeaders.add("Access-Control-Allow-Origin", "*")
    val bytes = body.toByteArray(StandardCharsets.UTF_8)
    sendResponseHeaders(status, bytes.size.toLong())
    responseBody.use { it.write(bytes) }
}

private fun String.doubleValue(name: String, default: Double? = null): Double {
    val match = Regex(""""$name"\s*:\s*(-?\d+(?:\.\d+)?)""").find(this)
    return match?.groupValues?.get(1)?.toDouble()
        ?: default
        ?: error("Missing numeric field '$name'")
}

private fun String.longValue(name: String): Long {
    val match = Regex(""""$name"\s*:\s*(-?\d+)""").find(this)
    return match?.groupValues?.get(1)?.toLong()
        ?: error("Missing long field '$name'")
}

private fun String.escapeJson(): String =
    replace("\\", "\\\\").replace("\"", "\\\"")
