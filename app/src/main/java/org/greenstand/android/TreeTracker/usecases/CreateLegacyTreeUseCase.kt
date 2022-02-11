package org.greenstand.android.TreeTracker.usecases

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.database.legacy.entity.TreeAttributeEntity
import org.greenstand.android.TreeTracker.database.legacy.entity.TreeCaptureEntity
import org.greenstand.android.TreeTracker.models.LocationUpdateManager
import org.greenstand.android.TreeTracker.models.Tree
import timber.log.Timber

data class CreateLegacyTreeParams(
    val planterCheckInId: Long,
    val tree: Tree,
)

class CreateLegacyTreeUseCase(
    private val locationUpdateManager: LocationUpdateManager,
    private val dao: TreeTrackerDAO,
) : UseCase<CreateLegacyTreeParams, Long>() {

    override suspend fun execute(params: CreateLegacyTreeParams): Long = withContext(Dispatchers.IO) {
        val location = locationUpdateManager.currentLocation
        val time = location?.time ?: System.currentTimeMillis()
        val timeInSeconds = time / 1000


        val entity = TreeCaptureEntity(
            uuid = params.tree.treeUuid.toString(),
            planterCheckInId = params.planterCheckInId,
            localPhotoPath = params.tree.photoPath,
            photoUrl = null,
            noteContent = params.tree.content,
            longitude = params.tree.meanLongitude,
            latitude = params.tree.meanLatitude,
            accuracy = 0.0, // accuracy is a legacy remnant and not used. Pending table cleanup
            createAt = timeInSeconds,
        )
        val attributeEntitites = params.tree.treeCaptureAttributes().map {
            TreeAttributeEntity(it.key, it.value, -1)
        }.toList()
        Timber.d("Inserting TreeCapture entity $entity")
        dao.insertTreeWithAttributes(entity, attributeEntitites)

    }
}