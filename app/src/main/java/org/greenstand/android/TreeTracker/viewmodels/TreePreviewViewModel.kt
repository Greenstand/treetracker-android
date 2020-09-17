package org.greenstand.android.TreeTracker.viewmodels

import android.location.Location
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.models.LocationUpdateManager

class TreePreviewViewModel(
    private val dao: TreeTrackerDAO,
    private val locationUpdateManager: LocationUpdateManager
) : ViewModel() {

    suspend fun loadTree(treeId: Long): TreePreviewData {

        val tree = withContext(Dispatchers.IO) { dao.getTreeCaptureById(treeId) }

        val results = floatArrayOf(0f, 0f, 0f)

        locationUpdateManager.currentLocation?.let {
            Location.distanceBetween(
                it.latitude,
                it.longitude,
                tree.latitude,
                tree.longitude,
                results
            )
        }

        return TreePreviewData(
            localPhotoPath = tree.localPhotoPath,
            imageUrl = tree.photoUrl,
            distance = results.first(),
            accuracy = tree.accuracy.toFloat(),
            createdAt = tree.createAt,
            note = tree.noteContent
        )
    }
}

data class TreePreviewData(
    val localPhotoPath: String?,
    val imageUrl: String?,
    val distance: Float,
    val accuracy: Float,
    val createdAt: Long,
    val note: String
)
