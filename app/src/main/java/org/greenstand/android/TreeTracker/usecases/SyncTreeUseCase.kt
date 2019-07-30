package org.greenstand.android.TreeTracker.usecases

import kotlinx.coroutines.coroutineScope
import org.greenstand.android.TreeTracker.database.v2.TreeTrackerDAO

data class SyncTreeParams(val treeId: Long)

class SyncTreeUseCase(private val uploadImageUseCase: UploadImageUseCase,
                      private val uploadTreeUseCase: UploadTreeUseCase,
                      private val dao: TreeTrackerDAO) : UseCase<SyncTreeParams, Unit>() {

    override suspend fun execute(params: SyncTreeParams) {
        coroutineScope {
            val tree = dao.getTreeCaptureById(params.treeId)

            if (tree.localPhotoPath == null) {
                throw IllegalStateException("No imagePath")
            }

            val imageUrl = uploadImageUseCase.execute(UploadImageParams(imagePath = tree.localPhotoPath)) ?: throw IllegalStateException("No imageUrl")

            tree.photoUrl = imageUrl

            dao.updateTreeCapture(tree)

            uploadTreeUseCase.execute(UploadTreeParams(treeId = tree.id, treeImageUrl = imageUrl))
        }
    }
}