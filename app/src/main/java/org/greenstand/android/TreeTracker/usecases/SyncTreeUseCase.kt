package org.greenstand.android.TreeTracker.usecases

import kotlinx.coroutines.coroutineScope
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO

data class SyncTreeParams(val treeId: Long)

class SyncTreeUseCase(private val uploadImageUseCase: UploadImageUseCase,
                      private val uploadTreeUseCase: UploadTreeUseCase,
                      private val removeLocalImagesWithIdsUseCase: RemoveLocalTreeImagesWithIdsUseCase,
                      private val dao: TreeTrackerDAO) : UseCase<SyncTreeParams, Unit>() {

    override suspend fun execute(params: SyncTreeParams) {
        coroutineScope {
            val tree = dao.getTreeCaptureById(params.treeId)

            if (tree.localPhotoPath == null) {
                throw IllegalStateException("Trying to upload tree with no imagePath")
            }

            // Upload photo only if it hasn't been saved in the DB (hasn't been uploaded yet)
            if (tree.photoUrl == null) {
                val imageUrl = uploadImageUseCase.execute(UploadImageParams(imagePath = tree.localPhotoPath!!,
                                                                            lat = tree.latitude,
                                                                            long = tree.longitude))
                    ?: throw IllegalStateException("No imageUrl")

                tree.photoUrl = imageUrl

                dao.updateTreeCapture(tree)
            }

            uploadTreeUseCase.execute(UploadTreeParams(treeId = tree.id, treeImageUrl = tree.photoUrl!!))

            removeLocalImagesWithIdsUseCase.execute(RemoveLocalTreeImagesWithIdsParams(listOf(tree.id)))
        }
    }
}
