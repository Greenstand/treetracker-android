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
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapView

@Composable
fun LibreMap(
    markers: List<MapMarker>,
    selectedMarkerId: String?,
    modifier: Modifier = Modifier,
    styleUrl: String = "https://demotiles.maplibre.org/style.json"
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val mapView = remember {
        MapView(context).apply {
            getMapAsync { mapLibreMap ->
                mapLibreMap.setStyle(styleUrl) {
                    // Set initial camera position (centered on equator with moderate zoom)
                    val initialPosition = CameraPosition.Builder()
                        .target(LatLng(0.0, 0.0))
                        .zoom(2.0)
                        .build()
                    mapLibreMap.cameraPosition = initialPosition
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
                    // Clear existing markers
                    mapLibreMap.clear()

                    // Add markers in bulk
                    if (markers.isNotEmpty()) {
                        val markerOptions = markers.map { marker ->
                            MarkerOptions()
                                .position(LatLng(marker.latitude, marker.longitude))
                        }

                        // Add all markers at once
                        mapLibreMap.addMarkers(markerOptions)

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
