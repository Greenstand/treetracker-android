package org.greenstand.android.TreeTracker.usecases

import android.content.SharedPreferences
import kotlinx.coroutines.coroutineScope
import org.greenstand.android.TreeTracker.managers.TreeManager

data class SyncTreeParams(val treeId: Long)

class SyncTreeUseCase(private val uploadImageUseCase: UploadImageUseCase,
                      private val uploadTreeUseCase: UploadTreeUseCase,
                      private val treeManager: TreeManager,
                      private val sharedPreferences: SharedPreferences) : UseCase<SyncTreeParams, Unit>() {

    companion object {
        private const val IMAGE_URL_PATH_KEY = "IMAGE_URL_PATH_KEY"
    }

    override suspend fun execute(params: SyncTreeParams) {
        coroutineScope {
            val tree = treeManager.getTree(params.treeId)

            if (tree.name == null) {
                throw IllegalStateException("No imagePath")
            }

            val imageUrl = uploadImageUseCase.execute(UploadImageParams(imagePath = tree.name!!)) ?: throw IllegalStateException("No imageUrl")

            sharedPreferences.edit()
                .putString(IMAGE_URL_PATH_KEY, imageUrl)
                .apply()


            uploadTreeUseCase.execute(UploadTreeParams(treeId = tree.tree_id, treeImageUrl = imageUrl))
        }
    }
}