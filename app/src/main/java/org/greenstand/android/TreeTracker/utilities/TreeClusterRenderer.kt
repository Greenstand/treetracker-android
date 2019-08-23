package org.greenstand.android.TreeTracker.utilities

import android.content.Context
import com.google.android.gms.maps.GoogleMap
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.DefaultClusterRenderer
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.map.TreeMapMarker

class TreeClusterRenderer(private val context: Context,
                          map: GoogleMap,
                          clusterManager: ClusterManager<TreeMapMarker>) : DefaultClusterRenderer<TreeMapMarker>(context, map, clusterManager) {

    init {
        minClusterSize = 5
    }

    override fun getColor(clusterSize: Int): Int {
        return context.resources.getColor(R.color.cautionOrange)
    }

}