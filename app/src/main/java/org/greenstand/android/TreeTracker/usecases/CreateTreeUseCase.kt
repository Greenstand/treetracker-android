package org.greenstand.android.TreeTracker.usecases

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.greenstand.android.TreeTracker.analytics.Analytics
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.database.entity.TreeEntity
import org.greenstand.android.TreeTracker.models.LocationUpdateManager
import org.greenstand.android.TreeTracker.models.Tree
import timber.log.Timber

class CreateTreeUseCase(
    private val locationUpdateManager: LocationUpdateManager,
    private val dao: TreeTrackerDAO,
    private val analytics: Analytics,
    private val gson: Gson,
) : UseCase<Tree, Long>() {

    override suspend fun execute(params: Tree): Long = withContext(Dispatchers.IO) {
        val location = locationUpdateManager.currentLocation
        val time = location?.time ?: System.currentTimeMillis()

        val entity = TreeEntity(
            uuid = params.treeUuid.toString(),
            sessionId = params.sessionId,
            photoPath = params.photoPath,
            photoUrl = null,
            note = params.content,
            longitude = params.meanLongitude,
            latitude = params.meanLatitude,
            createdAt = time,
            extraData = gson.toJson(params.treeCaptureAttributes()),
        )

        Timber.d("Inserting TreeCapture entity $entity")
        analytics.treePlanted()
        dao.insertTree(entity)
    }
}
