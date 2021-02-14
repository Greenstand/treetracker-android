package org.greenstand.android.TreeTracker.models

import com.google.gson.Gson
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import org.greenstand.android.TreeTracker.api.ObjectStorageClient
import org.greenstand.android.TreeTracker.api.models.requests.UploadBundle
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.database.entity.TreeCaptureEntity
import org.greenstand.android.TreeTracker.usecases.*
import org.greenstand.android.TreeTracker.utilities.md5
import timber.log.Timber
import java.io.File
import kotlin.coroutines.coroutineContext

class TreeUploader(
    private val uploadImageUseCase: UploadImageUseCase,
    private val objectStorageClient: ObjectStorageClient,
    private val createTreeRequestUseCase: CreateTreeRequestUseCase,
    private val dao: TreeTrackerDAO,
    private val gson: Gson
) {

    fun log(msg: String) = Timber.tag("TreeUploader").d(msg)

    suspend fun uploadTrees(treeIds: List<Long>) {
        log("Uploading ${treeIds.size} trees")

        treeIds.windowed(size = TREE_BUNDLE_SIZE, step = TREE_BUNDLE_SIZE, partialWindows = true).onEach { treeIdBundle ->
            try {
                if (coroutineContext.isActive) {
                    coroutineScope {
                        log("Starting bulk upload for ${treeIdBundle.size} trees")

                        val trees = dao.getTreeCapturesByIds(treeIdBundle)

                        uploadTreeImages(trees)
                        uploadTreeBundles(trees)
                        deleteLocalTreeImages(trees)

                        dao.updateTreeCapturesUploadStatus(trees.map { it.id }, true)

                        log("Completed bulk upload for ${treeIdBundle.size} trees")
                    }
                } else {
                    coroutineContext.cancel()
                }
            } catch (e: Exception) {
                Timber.e("NewTree upload failed")
            }
        }

        log("Completed upload for ${treeIds.size} trees")
    }

    private suspend fun uploadTreeImages(trees: List<TreeCaptureEntity>) {
        log("Uploading tree images...")
        trees
            .filter { it.photoUrl == null } // Upload photo only if it hasn't been saved in the DB (hasn't been uploaded yet)
            .forEach { tree ->
                val imageUrl = uploadImageUseCase.execute(
                    UploadImageParams(
                        imagePath = tree.localPhotoPath!!,
                        lat = tree.latitude,
                        long = tree.longitude
                    )
                ) ?: throw IllegalStateException("No imageUrl")

                // Update local tree data with image Url
                tree.photoUrl = imageUrl
                dao.updateTreeCapture(tree)
            }
        log("Tree Image Upload Completed")
    }

    private suspend fun uploadTreeBundles(trees: List<TreeCaptureEntity>) {
        log("Uploading Tree Bundle...")
        // Create a request object for each tree
        val treeRequestList = trees.map { tree ->
            createTreeRequestUseCase.execute(
                CreateTreeRequestParams(
                    tree.id,
                    tree.photoUrl!!
                )
            )
        }

        val jsonBundle = gson.toJson(UploadBundle(trees = treeRequestList))

        // Create a hash ID to reference this upload bundle later
        val bundleId = jsonBundle.md5()

        // Update the trees in DB with the bundleId
        dao.updateTreeCapturesBundleIds(trees.map { it.id }, bundleId)

        objectStorageClient.uploadBundle(jsonBundle, bundleId)
        log("Bundle Tree Upload Completed")
    }

    private suspend fun deleteLocalTreeImages(trees: List<TreeCaptureEntity>) {
        log("Deleting local image files for uploaded trees...")

        trees.mapNotNull { it.localPhotoPath }
            .forEach { photoPath ->
            val photoFile = File(photoPath)
            if (photoFile.exists()) {
                photoFile.delete()
            }
        }

        dao.removeTreeCapturesLocalImagePaths(trees.map { it.id })

        log("Local tree image files deleted")
    }

    companion object {
        private const val TREE_BUNDLE_SIZE = 50
    }
}