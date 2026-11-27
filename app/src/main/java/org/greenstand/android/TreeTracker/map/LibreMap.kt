/*
 * Copyright 2026 Treetracker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.greenstand.android.TreeTracker.map

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.LocationComponentOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource

@androidx.annotation.RequiresPermission(allOf = [android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION])
@Composable
fun LibreMap(
    markers: List<MapMarker>,
    stepPoints: List<StepPoint> = emptyList(),
    selectedMarkerId: String?,
    selectedSessionId: Long? = null,
    modifier: Modifier = Modifier,
    styleUrl: String = "https://demotiles.maplibre.org/style.json",
    onMarkerClick: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val mapView =
        remember {
            MapView(context).apply {
                getMapAsync { mapLibreMap ->
                    mapLibreMap.setStyle(styleUrl) { style ->
                        val initialPosition =
                            CameraPosition
                                .Builder()
                                .target(LatLng(0.0, 0.0))
                                .zoom(2.0)
                                .build()
                        mapLibreMap.cameraPosition = initialPosition
                        val locationComponent = mapLibreMap.locationComponent
                        locationComponent.activateLocationComponent(
                            LocationComponentActivationOptions
                                .builder(context, style)
                                .locationComponentOptions(
                                    LocationComponentOptions
                                        .builder(context)
                                        .pulseEnabled(true)
                                        .build(),
                                ).build(),
                        )
                        locationComponent.isLocationComponentEnabled = true
                    }
                }
            }
        }

    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> mapView.onStart()
                    Lifecycle.Event.ON_RESUME -> mapView.onResume()
                    Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                    Lifecycle.Event.ON_STOP -> mapView.onStop()
                    Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                    else -> {}
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { mapView },
        update = { view ->
            view.getMapAsync { mapLibreMap ->
                mapLibreMap.getStyle { style ->
                    style.getLayer("step-points-layer")?.let { style.removeLayer(it) }
                    style.getSource("step-points-source")?.let { style.removeSource(it) }
                    style.getLayer("tree-markers-layer")?.let { style.removeLayer(it) }
                    style.getSource("tree-markers-source")?.let { style.removeSource(it) }

                    if (stepPoints.isNotEmpty()) {
                        val stepCollection =
                            buildJsonObject {
                                put("type", "FeatureCollection")
                                putJsonArray("features") {
                                    stepPoints.forEach { point ->
                                        addJsonObject {
                                            put("type", "Feature")
                                            putJsonObject("geometry") {
                                                put("type", "Point")
                                                putJsonArray("coordinates") {
                                                    add(kotlinx.serialization.json.JsonPrimitive(point.longitude))
                                                    add(kotlinx.serialization.json.JsonPrimitive(point.latitude))
                                                }
                                            }
                                            putJsonObject("properties") {
                                                put("sessionId", point.sessionId.toString())
                                            }
                                        }
                                    }
                                }
                            }

                        style.addSource(GeoJsonSource("step-points-source", stepCollection.toString()))

                        val selectedSessionStr = selectedSessionId?.toString()
                        val stepColorExpression =
                            if (selectedSessionStr != null) {
                                Expression.switchCase(
                                    Expression.eq(Expression.get("sessionId"), Expression.literal(selectedSessionStr)),
                                    Expression.literal("#42A5F5"),
                                    Expression.literal("#9E9E9E"),
                                )
                            } else {
                                Expression.literal("#9E9E9E")
                            }

                        val stepOpacityExpression =
                            if (selectedSessionStr != null) {
                                Expression.switchCase(
                                    Expression.eq(Expression.get("sessionId"), Expression.literal(selectedSessionStr)),
                                    Expression.literal(0.9f),
                                    Expression.literal(0.3f),
                                )
                            } else {
                                Expression.literal(0.5f)
                            }

                        val stepLayer =
                            CircleLayer("step-points-layer", "step-points-source").apply {
                                setProperties(
                                    PropertyFactory.circleRadius(3.5f),
                                    PropertyFactory.circleColor(stepColorExpression),
                                    PropertyFactory.circleOpacity(stepOpacityExpression),
                                )
                            }
                        style.addLayer(stepLayer)
                    }

                    if (markers.isNotEmpty()) {
                        val featureCollection =
                            buildJsonObject {
                                put("type", "FeatureCollection")
                                putJsonArray("features") {
                                    markers.forEach { marker ->
                                        addJsonObject {
                                            put("type", "Feature")
                                            putJsonObject("geometry") {
                                                put("type", "Point")
                                                putJsonArray("coordinates") {
                                                    add(kotlinx.serialization.json.JsonPrimitive(marker.longitude))
                                                    add(kotlinx.serialization.json.JsonPrimitive(marker.latitude))
                                                }
                                            }
                                            putJsonObject("properties") {
                                                put("id", marker.id)
                                            }
                                        }
                                    }
                                }
                            }

                        val source = GeoJsonSource("tree-markers-source", featureCollection.toString())
                        style.addSource(source)

                        val radiusExpression =
                            if (selectedMarkerId != null) {
                                Expression.switchCase(
                                    Expression.eq(Expression.get("id"), Expression.literal(selectedMarkerId)),
                                    Expression.literal(12f),
                                    Expression.literal(8f),
                                )
                            } else {
                                Expression.literal(8f)
                            }

                        val circleLayer =
                            CircleLayer("tree-markers-layer", "tree-markers-source").apply {
                                setProperties(
                                    PropertyFactory.circleRadius(radiusExpression),
                                    PropertyFactory.circleColor("#4CAF50"),
                                    PropertyFactory.circleStrokeWidth(2f),
                                    PropertyFactory.circleStrokeColor("#FFFFFF"),
                                )
                            }
                        style.addLayer(circleLayer)

                        mapLibreMap.addOnMapClickListener { latLng ->
                            val screenPoint = mapLibreMap.projection.toScreenLocation(latLng)

                            val features = mapLibreMap.queryRenderedFeatures(screenPoint, "tree-markers-layer")

                            if (features.isNotEmpty()) {
                                val markerId = features.first().getStringProperty("id")
                                if (markerId != null) {
                                    onMarkerClick(markerId)
                                    return@addOnMapClickListener true
                                }
                            }
                            false
                        }

                        if (selectedMarkerId != null) {
                            val selectedMarker = markers.find { it.id == selectedMarkerId }
                            selectedMarker?.let { marker ->
                                val position =
                                    CameraPosition
                                        .Builder()
                                        .target(LatLng(marker.latitude, marker.longitude))
                                        .zoom(14.0)
                                        .build()
                                mapLibreMap.animateCamera(CameraUpdateFactory.newCameraPosition(position))
                            }
                        } else {
                            val single = singleMapPoint(markers.map { it.latitude to it.longitude })
                            if (single != null) {
                                val position =
                                    CameraPosition
                                        .Builder()
                                        .target(LatLng(single.first, single.second))
                                        .zoom(14.0)
                                        .build()
                                mapLibreMap.animateCamera(CameraUpdateFactory.newCameraPosition(position))
                            } else {
                                val boundsBuilder = LatLngBounds.Builder()
                                markers.forEach { marker ->
                                    boundsBuilder.include(LatLng(marker.latitude, marker.longitude))
                                }

                                val bounds = boundsBuilder.build()
                                val padding = 100

                                mapLibreMap.animateCamera(
                                    CameraUpdateFactory.newLatLngBounds(bounds, padding),
                                )
                            }
                        }
                    }
                }
            }
        },
    )
}

internal fun singleMapPoint(points: List<Pair<Double, Double>>): Pair<Double, Double>? = points.distinct().singleOrNull()