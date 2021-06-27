package org.greenstand.android.TreeTracker.map

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem

class TreeMapMarker(
    lat: Double,
    lng: Double,
    private val _title: String = "",
    private val _snippet: String = ""
) : ClusterItem {

    private val _position: LatLng = LatLng(lat, lng)

    override fun getPosition(): LatLng {
        return _position
    }

    override fun getTitle(): String {
        return _title
    }

    override fun getSnippet(): String {
        return _snippet
    }
}
