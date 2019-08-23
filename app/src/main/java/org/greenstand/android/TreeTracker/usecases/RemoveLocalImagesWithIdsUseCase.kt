package org.greenstand.android.TreeTracker.usecases

import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import java.io.File

data class RemoveLocalImagesWithIdsParams(val treeCaptureIds: List<Long>)

class RemoveLocalImagesWithIdsUseCase(private val dao: TreeTrackerDAO) : UseCase<RemoveLocalImagesWithIdsParams, Unit>() {

    override suspend fun execute(params: RemoveLocalImagesWithIdsParams) {

        val trees = dao.getTreeCapturesByIds(params.treeCaptureIds)

        trees.forEach {
            val photoFile = File(it.localPhotoPath)
            if (photoFile.exists()) {
                photoFile.delete()
            }
        }

        dao.removeTreeCapturesLocalImagePaths(params.treeCaptureIds)
    }

}