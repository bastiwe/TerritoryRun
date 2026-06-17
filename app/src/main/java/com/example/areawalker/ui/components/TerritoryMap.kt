package com.example.areawalker.ui.components

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.areawalker.BuildConfig
import com.example.areawalker.domain.model.GpsPoint
import com.example.areawalker.domain.model.Team
import com.example.areawalker.domain.model.Territory
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.PolygonOptions
import org.maplibre.android.annotations.PolylineOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun TerritoryMap(
    territories: List<Territory>,
    route: List<GpsPoint>,
    currentLocation: GpsPoint? = null,
    activeTeam: Team? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapTilerApiKey = BuildConfig.MAPTILER_API_KEY
    val styleUrl = remember(mapTilerApiKey) { mapStyleUrl(mapTilerApiKey) }
    val lastOverlaySignature = remember { mutableStateOf("") }
    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context).apply { onCreate(null) }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF17211F))
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { mapView },
            update = {
                mapView.getMapAsync { map ->
                    val overlaySignature = overlaySignature(territories, route, currentLocation, activeTeam)
                    if (mapView.tag != styleUrl) {
                        map.setStyle(styleUrl) {
                            mapView.tag = styleUrl
                            lastOverlaySignature.value = overlaySignature
                            redrawMap(map, territories, route, currentLocation, activeTeam, moveCamera = true)
                        }
                    } else if (lastOverlaySignature.value != overlaySignature) {
                        lastOverlaySignature.value = overlaySignature
                        redrawMap(map, territories, route, currentLocation, activeTeam, moveCamera = route.size <= 1)
                    }
                }
            }
        )
        Text(
            if (mapTilerApiKey.isBlank()) {
                "MapLibre aktiv - MapTiler API-Key fehlt, Demo-Style wird genutzt"
            } else {
                "MapLibre + MapTiler"
            },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
                .background(Color(0xCC0D1117), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            color = Color.White.copy(alpha = 0.86f)
        )
    }
}

fun teamColor(team: Team): Color = Color(team.color)

private fun redrawMap(
    map: org.maplibre.android.maps.MapLibreMap,
    territories: List<Territory>,
    route: List<GpsPoint>,
    currentLocation: GpsPoint?,
    activeTeam: Team?,
    moveCamera: Boolean
) {
    map.clear()
    drawTerritories(map, territories)
    drawLiveCapturePreview(map, route, activeTeam)
    drawRoute(map, route)
    drawCurrentPosition(map, route.lastOrNull() ?: currentLocation, activeTeam)
    if (moveCamera) cameraTarget(territories, route, currentLocation)?.let { target ->
        map.cameraPosition = CameraPosition.Builder()
            .target(target)
            .zoom(if (route.isNotEmpty()) 14.0 else 13.0)
            .build()
    }
}

private fun mapStyleUrl(mapTilerApiKey: String): String =
    if (mapTilerApiKey.isBlank()) {
        "https://demotiles.maplibre.org/style.json"
    } else {
        "https://api.maptiler.com/maps/streets-v2/style.json?key=$mapTilerApiKey"
    }

private fun overlaySignature(
    territories: List<Territory>,
    route: List<GpsPoint>,
    currentLocation: GpsPoint?,
    activeTeam: Team?
): String =
    "${activeTeam?.name}|${territories.joinToString { "${it.id}:${it.team}:${it.polygon.size}" }}|route:${route.size}:${route.lastOrNull()?.latitude}:${route.lastOrNull()?.longitude}|current:${currentLocation?.latitude}:${currentLocation?.longitude}"

private fun drawTerritories(map: org.maplibre.android.maps.MapLibreMap, territories: List<Territory>) {
    territories.forEach { territory ->
        val latLngs = territory.polygon.map { it.toLatLng() }
        if (latLngs.size >= 3) {
            val color = territory.team.mapColor(alpha = 0x62)
            map.addPolygon(
                PolygonOptions()
                    .addAll(latLngs)
                    .fillColor(color)
                    .strokeColor(territory.team.mapColor(alpha = 0xDD))
            )
        }
    }
}

private fun drawRoute(map: org.maplibre.android.maps.MapLibreMap, route: List<GpsPoint>) {
    if (route.size < 2) return
    map.addPolyline(
        PolylineOptions()
            .addAll(route.map { it.toLatLng() })
            .color(AndroidColor.rgb(255, 214, 10))
            .width(7f)
    )
}

private fun drawLiveCapturePreview(
    map: org.maplibre.android.maps.MapLibreMap,
    route: List<GpsPoint>,
    activeTeam: Team?
) {
    if (route.size < 3) return
    val previewPolygon = route.map { it.toLatLng() } + route.first().toLatLng()
    val previewTeam = activeTeam ?: Team.Neutral
    map.addPolygon(
        PolygonOptions()
            .addAll(previewPolygon)
            .fillColor(previewTeam.mapColor(alpha = 0x35))
            .strokeColor(previewTeam.mapColor(alpha = 0xB8))
    )
}

private fun drawCurrentPosition(
    map: org.maplibre.android.maps.MapLibreMap,
    point: GpsPoint?,
    activeTeam: Team?
) {
    if (point == null) return
    val team = activeTeam ?: Team.Neutral
    map.addPolygon(
        PolygonOptions()
            .addAll(circleAround(point, radiusMeters = 8.0))
            .fillColor(team.mapColor(alpha = 0xF0))
            .strokeColor(AndroidColor.WHITE)
    )
}

private fun cameraTarget(territories: List<Territory>, route: List<GpsPoint>, currentLocation: GpsPoint?): LatLng? {
    if (route.isEmpty() && currentLocation != null) return currentLocation.toLatLng()
    val points = route.ifEmpty { territories.flatMap { it.polygon } }
    if (points.isEmpty()) return LatLng(52.5200, 13.4050)
    return LatLng(
        points.map { it.latitude }.average(),
        points.map { it.longitude }.average()
    )
}

private fun GpsPoint.toLatLng(): LatLng = LatLng(latitude, longitude)

private fun circleAround(center: GpsPoint, radiusMeters: Double): List<LatLng> {
    val latRadius = radiusMeters / 111_320.0
    val lonRadius = radiusMeters / (111_320.0 * cos(Math.toRadians(center.latitude))).coerceAtLeast(0.1)
    return (0..20).map { index ->
        val angle = 2.0 * PI * index / 20.0
        LatLng(
            center.latitude + latRadius * sin(angle),
            center.longitude + lonRadius * cos(angle)
        )
    }
}

private fun Team.mapColor(alpha: Int): Int {
    val color = color.toInt()
    return AndroidColor.argb(
        alpha,
        AndroidColor.red(color),
        AndroidColor.green(color),
        AndroidColor.blue(color)
    )
}
