package org.greenstand.android.TreeTracker.usecases

import com.google.gson.Gson
import kotlinx.coroutines.coroutineScope
import org.greenstand.android.TreeTracker.api.TreeBundleUploader
import org.greenstand.android.TreeTracker.api.models.requests.NewTreeRequest
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.utilities.md5
import timber.log.Timber

data class UploadTreeBundleParams(val treeIds: List<Long>)

class UploadTreeBundleUseCase(private val uploadImageUseCase: UploadImageUseCase,
                              private val treeBundleUploader: TreeBundleUploader,
                              private val createTreeRequestUseCase: CreateTreeRequestUseCase,
                              private val dao: TreeTrackerDAO) : UseCase<UploadTreeBundleParams, Unit>() {

    fun log(msg: String) = Timber.tag("UploadTreeBundleUseCase").d(msg)

    override suspend fun execute(params: UploadTreeBundleParams) {
        coroutineScope {
            val trees = dao.getTreeCapturesByIds(params.treeIds)

            log("Starting image uploads...")
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
            log("Image uploads complete")

            log("Creating tree requests")
            // Create a request object for each tree
            val treeRequestList = trees.map { tree -> createTreeRequestUseCase.execute(CreateTreeRequestParams(tree.id, tree.photoUrl!!)) }

            val jsonBundle = Gson().toJson(TreeBundle(treeRequestList))

            log("Creating MD5 hash")
            // Create a hash ID to reference this upload bundle later
            val bundleId = jsonBundle.md5()

            log("Updating tree DB entries with MD5 hash")
            // Update the trees in DB with the bundleId
            dao.updateTreeCapturesBundleIds(trees.map { it.id }, bundleId)


            log("Uploading Bundle...")
            treeBundleUploader.uploadTreeJsonBundle(jsonBundle, bundleId)

            dao.updateTreeCapturesUploadStatus(trees.map { it.id }, true)

            log("Bundle Upload Completed")
        }
    }



}

private data class TreeBundle(val trees: List<NewTreeRequest>)