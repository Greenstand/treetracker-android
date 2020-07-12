package org.greenstand.android.TreeTracker.usecases

import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import java.io.File

data class RemoveLocalTreeImagesWithIdsParams(val treeCaptureIds: List<Long>)

class RemoveLocalTreeImagesWithIdsUseCase(private val dao: TreeTrackerDAO) : UseCase<RemoveLocalTreeImagesWithIdsParams, Unit>() {

    override suspend fun execute(params: RemoveLocalTreeImagesWithIdsParams) {

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