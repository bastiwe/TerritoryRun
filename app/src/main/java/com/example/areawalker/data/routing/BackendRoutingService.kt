package com.example.areawalker.data.routing

import com.example.areawalker.domain.model.GpsPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class BackendRoutingService(
    private val baseUrl: String
) : RoutingService {
    override suspend fun createDemoLoop(
        start: GpsPoint,
        targetDistanceMeters: Double,
        targetDurationMillis: Long
    ): List<GpsPoint> = withContext(Dispatchers.IO) {
        val connection = (URL("${baseUrl.trimEnd('/')}/routing/demo-loop").openConnection() as HttpURLConnection)
        connection.requestMethod = "POST"
        connection.connectTimeout = 8_000
        connection.readTimeout = 12_000
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        connection.doOutput = true
        connection.outputStream.use { output ->
            output.write(requestJson(start, targetDistanceMeters, targetDurationMillis).toByteArray(Charsets.UTF_8))
        }

        val status = connection.responseCode
        val body = if (status in 200..299) {
            connection.inputStream.bufferedReader().use { it.readText() }
        } else {
            connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
        }
        connection.disconnect()

        if (status !in 200..299) error("Routing backend returned HTTP $status: $body")
        parsePoints(body)
    }

    override suspend fun matchRoute(points: List<GpsPoint>): List<GpsPoint> = withContext(Dispatchers.IO) {
        if (points.size < 2) return@withContext points
        val connection = (URL("${baseUrl.trimEnd('/')}/routing/match").openConnection() as HttpURLConnection)
        connection.requestMethod = "POST"
        connection.connectTimeout = 8_000
        connection.readTimeout = 20_000
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        connection.doOutput = true
        connection.outputStream.use { output ->
            output.write(pointsRequestJson(points).toByteArray(Charsets.UTF_8))
        }

        val status = connection.responseCode
        val body = if (status in 200..299) {
            connection.inputStream.bufferedReader().use { it.readText() }
        } else {
            connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
        }
        connection.disconnect()

        if (status !in 200..299) return@withContext points
        runCatching { parsePoints(body) }.getOrElse { points }
    }

    private fun requestJson(start: GpsPoint, targetDistanceMeters: Double, targetDurationMillis: Long): String =
        """
        {
          "start": {
            "latitude": ${start.latitude},
            "longitude": ${start.longitude},
            "timestampMillis": ${start.timestampMillis},
            "accuracyMeters": ${start.accuracyMeters}
          },
          "targetDistanceMeters": $targetDistanceMeters,
          "targetDurationMillis": $targetDurationMillis
        }
        """.trimIndent()

    private fun pointsRequestJson(points: List<GpsPoint>): String =
        points.joinToString(prefix = """{"points":[""", postfix = "]}") { point ->
            """
            {
              "latitude": ${point.latitude},
              "longitude": ${point.longitude},
              "timestampMillis": ${point.timestampMillis},
              "accuracyMeters": ${point.accuracyMeters}
            }
            """.trimIndent()
        }

    private fun parsePoints(body: String): List<GpsPoint> {
        val array = JSONObject(body).getJSONArray("points")
        return List(array.length()) { index ->
            val item = array.getJSONObject(index)
            GpsPoint(
                latitude = item.getDouble("latitude"),
                longitude = item.getDouble("longitude"),
                timestampMillis = item.getLong("timestampMillis"),
                accuracyMeters = item.optDouble("accuracyMeters", 8.0).toFloat()
            )
        }
    }
}
