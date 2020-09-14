package org.greenstand.android.TreeTracker.usecases

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.greenstand.android.TreeTracker.analytics.Analytics
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.database.entity.TreeAttributeEntity
import org.greenstand.android.TreeTracker.database.entity.TreeCaptureEntity
import org.greenstand.android.TreeTracker.models.LocationUpdateManager
import org.greenstand.android.TreeTracker.models.Tree
import timber.log.Timber

class CreateTreeUseCase(
    private val locationUpdateManager: LocationUpdateManager,
    private val dao: TreeTrackerDAO,
    private val analytics: Analytics
) : UseCase<Tree, Long>() {

    override suspend fun execute(tree: Tree): Long = withContext(Dispatchers.IO) {
        val location = locationUpdateManager.currentLocation
        val time = location?.time ?: System.currentTimeMillis()
        val timeInSeconds = time / 1000

        val entity = TreeCaptureEntity(
            uuid = tree.treeUuid.toString(),
            planterCheckInId = tree.planterCheckInId,
            localPhotoPath = tree.photoPath,
            photoUrl = null,
            noteContent = tree.content,
            longitude = location?.longitude ?: 0.0,
            latitude = location?.latitude ?: 0.0,
            accuracy = location?.accuracy?.toDouble() ?: 10000.0,
            createAt = timeInSeconds
        )
        analytics.treePlanted()
        val attributeEntitites = tree.treeCaptureAttributes().map {
            TreeAttributeEntity(it.key, it.value, -1)
        }.toList()
        Timber.d("Inserting TreeCapture entity $entity")
        dao.insertTreeWithAttributes(entity, attributeEntitites)
    }
}
