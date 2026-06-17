package com.example.areawalker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.areawalker.domain.model.GpsPoint
import com.example.areawalker.domain.model.Team
import com.example.areawalker.domain.model.Territory

@Composable
fun TerritoryMap(
    territories: List<Territory>,
    route: List<GpsPoint>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color(0xFF17211F), RoundedCornerShape(8.dp))
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val allPoints = territories.flatMap { it.polygon } + route
            val minLat = allPoints.minOfOrNull { it.latitude } ?: 52.518
            val maxLat = allPoints.maxOfOrNull { it.latitude } ?: 52.522
            val minLon = allPoints.minOfOrNull { it.longitude } ?: 13.402
            val maxLon = allPoints.maxOfOrNull { it.longitude } ?: 13.410
            val latSpan = (maxLat - minLat).takeIf { it > 0.00001 } ?: 0.01
            val lonSpan = (maxLon - minLon).takeIf { it > 0.00001 } ?: 0.01

            fun project(point: GpsPoint): Offset {
                val x = ((point.longitude - minLon) / lonSpan).toFloat() * size.width
                val y = size.height - ((point.latitude - minLat) / latSpan).toFloat() * size.height
                return Offset(x.coerceIn(10f, size.width - 10f), y.coerceIn(10f, size.height - 10f))
            }

            val gridColor = Color.White.copy(alpha = 0.06f)
            repeat(7) { index ->
                val x = size.width * index / 6f
                val y = size.height * index / 6f
                drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), 1f)
                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), 1f)
            }

            territories.forEach { territory ->
                val path = Path()
                territory.polygon.forEachIndexed { index, point ->
                    val offset = project(point)
                    if (index == 0) path.moveTo(offset.x, offset.y) else path.lineTo(offset.x, offset.y)
                }
                path.close()
                val color = teamColor(territory.team)
                drawPath(path, color.copy(alpha = 0.26f))
                drawPath(path, color.copy(alpha = 0.8f), style = Stroke(width = 3f))
            }

            if (route.size > 1) {
                route.zipWithNext().forEach { (a, b) ->
                    drawLine(
                        Color(0xFFFACC15),
                        project(a),
                        project(b),
                        strokeWidth = 5f,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
        Text(
            "OSM/MapLibre Adapter vorbereitet - MVP Fallback Map",
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp),
            color = Color.White.copy(alpha = 0.62f)
        )
    }
}

fun teamColor(team: Team): Color = Color(team.color)

