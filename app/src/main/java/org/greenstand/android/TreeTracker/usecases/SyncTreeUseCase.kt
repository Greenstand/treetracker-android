package org.greenstand.android.TreeTracker.usecases

import kotlinx.coroutines.coroutineScope
import org.greenstand.android.TreeTracker.managers.TreeManager

data class SyncTreeParams(val treeId: Long)

class SyncTreeUseCase(private val uploadImageUseCase: UploadImageUseCase,
                      private val uploadTreeUseCase: UploadTreeUseCase,
                      private val treeManager: TreeManager) : UseCase<SyncTreeParams, Unit>() {

    override suspend fun execute(params: SyncTreeParams) {
        coroutineScope {
            val tree = treeManager.getTree(params.treeId)

            if (tree.name == null) {
                throw IllegalStateException("No imagePath")
            }

            val imageUrl = uploadImageUseCase.execute(UploadImageParams(imagePath = tree.name!!)) ?: throw IllegalStateException("No imageUrl")

            uploadTreeUseCase.execute(UploadTreeParams(treeId = tree.tree_id, treeImageUrl = imageUrl))
        }
    }
}