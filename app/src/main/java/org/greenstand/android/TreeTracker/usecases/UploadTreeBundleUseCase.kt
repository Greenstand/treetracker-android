package org.greenstand.android.TreeTracker.usecases

import com.google.gson.Gson
import kotlinx.coroutines.coroutineScope
import org.greenstand.android.TreeTracker.api.TreeBundleUploader
import org.greenstand.android.TreeTracker.api.models.requests.NewTreeRequest
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.utilities.md5

data class UploadTreeBundleParams(val treeIds: List<Long>)

class UploadTreeBundleUseCase(private val uploadImageUseCase: UploadImageUseCase,
                              private val uploadTreeUseCase: UploadTreeUseCase,
                              private val treeBundleUploader: TreeBundleUploader,
                              private val createTreeRequestUseCase: CreateTreeRequestUseCase,
                              private val dao: TreeTrackerDAO) : UseCase<UploadTreeBundleParams, Unit>() {

    override suspend fun execute(params: UploadTreeBundleParams) {
        coroutineScope {
            val trees = dao.getTreeCapturesByIds(params.treeIds)

            // Upload the images of each tree
            trees.forEach { tree ->
                if (tree.localPhotoPath == null) {
                    throw IllegalStateException("No imagePath")
                }

                // Upload photo only if it hasn't been saved in the DB (hasn't been uploaded yet)
                if (tree.photoUrl == null) {
                    val imageUrl = uploadImageUseCase.execute(UploadImageParams(imagePath = tree.localPhotoPath))
                        ?: throw IllegalStateException("No imageUrl")

                    // Update local tree data with image Url
                    tree.photoUrl = imageUrl
                    dao.updateTreeCapture(tree)
                }
            }

            // Create a request object for each tree
            val treeRequestList = trees.map { tree -> createTreeRequestUseCase.execute(CreateTreeRequestParams(tree.id, tree.photoUrl!!)) }

            val jsonBundle = Gson().toJson(TreeBundle(treeRequestList))

            // Create a hash ID to reference this upload bundle later
            val bundleId = jsonBundle.md5()

            // Update the trees in DB with the bundleId
            dao.updateTreeCapturesBundleIds(trees.map { it.id }, bundleId)

            treeBundleUploader.uploadTreeJsonBundle(jsonBundle)
        }
    }



}

private data class TreeBundle(val trees: List<NewTreeRequest>)