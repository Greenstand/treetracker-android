package org.greenstand.android.TreeTracker.usecases

import com.google.gson.Gson
import kotlinx.coroutines.coroutineScope
import org.greenstand.android.TreeTracker.api.TreeBundleUploader
import org.greenstand.android.TreeTracker.api.models.requests.NewTreeRequest
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.utilities.md5
import timber.log.Timber
import java.io.File

data class UploadTreeBundleParams(val treeIds: List<Long>)

class UploadTreeBundleUseCase(private val uploadImageUseCase: UploadImageUseCase,
                              private val treeBundleUploader: TreeBundleUploader,
                              private val createTreeRequestUseCase: CreateTreeRequestUseCase,
                              private val dao: TreeTrackerDAO) : UseCase<UploadTreeBundleParams, Unit>() {

    fun log(msg: String) = Timber.tag("UploadTreeBundleUseCase").d(msg)

    override suspend fun execute(params: UploadTreeBundleParams) {
        coroutineScope {

            log("Starting bulk upload for ${params.treeIds.size} trees")

            val trees = dao.getTreeCapturesByIds(params.treeIds)

            log("Starting image uploads...")
            // Upload the images of each tree
            trees
                .filter { it.photoUrl == null } // Upload photo only if it hasn't been saved in the DB (hasn't been uploaded yet)
                .forEach { tree ->

                    val imageUrl = uploadImageUseCase.execute(UploadImageParams(imagePath = tree.localPhotoPath!!))
                        ?: throw IllegalStateException("No imageUrl")

                    // Update local tree data with image Url
                    tree.photoUrl = imageUrl
                    dao.updateTreeCapture(tree)
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
            log("Bundle Upload Completed")

            log("Deleting all local image files for uploaded trees...")
            trees.forEach { File(it.localPhotoPath).delete() }

            log("Updating tree capture DB status to uploaded = true")
            dao.updateTreeCapturesUploadStatus(trees.map { it.id }, true)

        }
    }



}

private data class TreeBundle(val trees: List<NewTreeRequest>)