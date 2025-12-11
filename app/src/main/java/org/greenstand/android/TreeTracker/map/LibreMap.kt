package org.greenstand.android.TreeTracker.map

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.gson.JsonArray
import com.google.gson.JsonObject
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
    selectedMarkerId: String?,
    modifier: Modifier = Modifier,
    styleUrl: String = "https://demotiles.maplibre.org/style.json",
    onMarkerClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val mapView = remember {
        MapView(context).apply {
            getMapAsync { mapLibreMap ->
                mapLibreMap.setStyle(styleUrl) { style ->
                    // Set initial camera position (centered on equator with moderate zoom)
                    val initialPosition = CameraPosition.Builder()
                        .target(LatLng(0.0, 0.0))
                        .zoom(2.0)
                        .build()
                    mapLibreMap.cameraPosition = initialPosition
                    val locationComponent = mapLibreMap.locationComponent
                    locationComponent.activateLocationComponent(
                        LocationComponentActivationOptions
                            .builder(context, style)
                            .locationComponentOptions(
                                LocationComponentOptions.builder(context)
                                    .pulseEnabled(true)
                                    .build()
                            )
                            .build()
                    )
                    locationComponent.isLocationComponentEnabled = true
                }
            }
        }
    }

    // Handle lifecycle events
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
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
                    // Remove existing source and layer if they exist
                    style.getLayer("tree-markers-layer")?.let { style.removeLayer(it) }
                    style.getSource("tree-markers-source")?.let { style.removeSource(it) }

                    // Add markers in bulk as GeoJSON
                    if (markers.isNotEmpty()) {
                        // Create GeoJSON FeatureCollection
                        val features = JsonArray()
                        markers.forEach { marker ->
                            val feature = JsonObject().apply {
                                addProperty("type", "Feature")
                                add("geometry", JsonObject().apply {
                                    addProperty("type", "Point")
                                    add("coordinates", JsonArray().apply {
                                        add(marker.longitude)
                                        add(marker.latitude)
                                    })
                                })
                                add("properties", JsonObject().apply {
                                    addProperty("id", marker.id)
                                })
                            }
                            features.add(feature)
                        }

                        val featureCollection = JsonObject().apply {
                            addProperty("type", "FeatureCollection")
                            add("features", features)
                        }

                        // Add GeoJSON source
                        val source = GeoJsonSource("tree-markers-source", featureCollection.toString())
                        style.addSource(source)

                        // Add circle layer with green dots
                        val radiusExpression = if (selectedMarkerId != null) {
                            Expression.switchCase(
                                Expression.eq(Expression.get("id"), Expression.literal(selectedMarkerId)),
                                Expression.literal(12f), // Selected marker radius
                                Expression.literal(8f)     // Default marker radius
                            )
                        } else {
                            Expression.literal(8f)
                        }

                        val circleLayer = CircleLayer("tree-markers-layer", "tree-markers-source").apply {
                            setProperties(
                                PropertyFactory.circleRadius(radiusExpression),
                                PropertyFactory.circleColor("#4CAF50"),
                                PropertyFactory.circleStrokeWidth(2f),
                                PropertyFactory.circleStrokeColor("#FFFFFF")
                            )
                        }
                        style.addLayer(circleLayer)

                        // Add click listener for markers
                        mapLibreMap.addOnMapClickListener { latLng ->
                            // Convert map coordinates to screen point
                            val screenPoint = mapLibreMap.projection.toScreenLocation(latLng)

                            // Query features at click point from marker layer
                            val features = mapLibreMap.queryRenderedFeatures(screenPoint, "tree-markers-layer")

                            // If marker clicked, extract ID and notify
                            if (features.isNotEmpty()) {
                                val markerId = features.first().getStringProperty("id")
                                if (markerId != null) {
                                    onMarkerClick(markerId)
                                    return@addOnMapClickListener true // Consume event
                                }
                            }
                            false // Don't consume - allow map pan/zoom
                        }

                        // Handle selected marker zoom
                        if (selectedMarkerId != null) {
                            val selectedMarker = markers.find { it.id == selectedMarkerId }
                            selectedMarker?.let { marker ->
                                val position = CameraPosition.Builder()
                                    .target(LatLng(marker.latitude, marker.longitude))
                                    .zoom(14.0)
                                    .build()
                                mapLibreMap.animateCamera(CameraUpdateFactory.newCameraPosition(position))
                            }
                        } else {
                            // Calculate bounds to fit all markers
                            val boundsBuilder = LatLngBounds.Builder()
                            markers.forEach { marker ->
                                boundsBuilder.include(LatLng(marker.latitude, marker.longitude))
                            }

                            val bounds = boundsBuilder.build()
                            val padding = 100 // padding in pixels

                            // Animate camera to show all markers
                            mapLibreMap.animateCamera(
                                CameraUpdateFactory.newLatLngBounds(bounds, padding)
                            )
                        }
                    }
                }
            }
        }
    )
}
