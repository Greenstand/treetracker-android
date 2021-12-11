package org.greenstand.android.TreeTracker.usecases

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.greenstand.android.TreeTracker.analytics.Analytics
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.database.entity.TreeAttributeEntity
import org.greenstand.android.TreeTracker.database.entity.TreeCaptureEntity
import org.greenstand.android.TreeTracker.models.LocationUpdateManager
import org.greenstand.android.TreeTracker.models.SessionTracker
import org.greenstand.android.TreeTracker.models.Tree
import timber.log.Timber

class CreateTreeUseCase(
    private val locationUpdateManager: LocationUpdateManager,
    private val dao: TreeTrackerDAO,
    private val analytics: Analytics,
    private val sessionTracker: SessionTracker,
) : UseCase<Tree, Long>() {

    override suspend fun execute(params: Tree): Long = withContext(Dispatchers.IO) {
        val location = locationUpdateManager.currentLocation
        val time = location?.time ?: System.currentTimeMillis()
        val timeInSeconds = time / 1000

        val entity = TreeCaptureEntity(
            uuid = params.treeUuid.toString(),
            planterCheckInId = -1,
            sessionId = params.sessionId,
            localPhotoPath = params.photoPath,
            photoUrl = null,
            noteContent = params.content,
            longitude = params.meanLongitude,
            latitude = params.meanLatitude,
            accuracy = 0.0, // accuracy is a legacy remnant and not used. Pending table cleanup
            createAt = timeInSeconds,
            wallet = sessionTracker.currentUser?.wallet ?: ""
        )
        analytics.treePlanted()
        val attributeEntitites = params.treeCaptureAttributes().map {
            TreeAttributeEntity(it.key, it.value, -1)
        }.toList()
        Timber.d("Inserting TreeCapture entity $entity")
        dao.insertTreeWithAttributes(entity, attributeEntitites)

    }
}
