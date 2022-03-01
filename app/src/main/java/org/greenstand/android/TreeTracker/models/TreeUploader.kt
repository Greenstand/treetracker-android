package org.greenstand.android.TreeTracker.models

import com.google.gson.Gson
import java.io.File
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import org.greenstand.android.TreeTracker.api.ObjectStorageClient
import org.greenstand.android.TreeTracker.api.models.requests.TreeCaptureRequest
import org.greenstand.android.TreeTracker.api.models.requests.UploadBundle
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.database.entity.TreeEntity
import org.greenstand.android.TreeTracker.database.legacy.entity.TreeCaptureEntity
import org.greenstand.android.TreeTracker.usecases.CreateTreeRequestParams
import org.greenstand.android.TreeTracker.usecases.CreateTreeRequestUseCase
import org.greenstand.android.TreeTracker.usecases.UploadImageParams
import org.greenstand.android.TreeTracker.usecases.UploadImageUseCase
import org.greenstand.android.TreeTracker.utilities.md5
import timber.log.Timber

class TreeUploader(
    private val uploadImageUseCase: UploadImageUseCase,
    private val objectStorageClient: ObjectStorageClient,
    private val createTreeRequestUseCase: CreateTreeRequestUseCase,
    private val dao: TreeTrackerDAO,
    private val gson: Gson
) {

    fun log(msg: String) = Timber.tag("TreeUploader").d(msg)

    suspend fun uploadLegacyTrees(treeIds: List<Long>) {
        windowedTreeUpload(treeIds) { treeIdBundle ->
            val legacyTrees = dao.getTreeCapturesByIds(treeIdBundle)
            uploadLegacyTreeImages(legacyTrees)
            uploadLegacyTreeBundles(legacyTrees)

            deleteLocalImages(legacyTrees.map { it.localPhotoPath })
            dao.removeTreeCapturesLocalImagePaths(legacyTrees.map { it.id })
        }
    }

    suspend fun uploadTrees(treeIds: List<Long>) {
        windowedTreeUpload(treeIds) { treeIdBundle ->
            val trees = dao.getTreesByIds(treeIdBundle)
            uploadTreeImages(trees)
            uploadTreeBundles(trees)

            deleteLocalImages(trees.map { it.photoPath })
            dao.removeTreesLocalImagePaths(trees.map { it.id })
        }
    }

    private suspend fun windowedTreeUpload(treeIds: List<Long>, onHandleUpload: suspend (List<Long>) -> Unit) {
        log("Uploading ${treeIds.size} trees")
        treeIds.windowed(size = TREE_BUNDLE_SIZE, step = TREE_BUNDLE_SIZE, partialWindows = true).onEach { treeIdBundle ->
            try {
                if (coroutineContext.isActive) {
                    coroutineScope {
                        log("Starting bulk upload for ${treeIdBundle.size} trees")
                        onHandleUpload(treeIdBundle)
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

    private suspend fun uploadLegacyTreeImages(trees: List<TreeCaptureEntity>) {
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

    private suspend fun uploadTreeImages(trees: List<TreeEntity>) {
        log("Uploading tree images...")
        trees
            .filter { it.photoUrl == null } // Upload photo only if it hasn't been saved in the DB (hasn't been uploaded yet)
            .forEach { tree ->
                val imageUrl = uploadImageUseCase.execute(
                    UploadImageParams(
                        imagePath = tree.photoPath!!,
                        lat = tree.latitude,
                        long = tree.longitude
                    )
                ) ?: throw IllegalStateException("No imageUrl")

                // Update local tree data with image Url
                tree.photoUrl = imageUrl
                dao.updateTree(tree)
            }
        log("Tree Image Upload Completed")
    }

    private suspend fun uploadLegacyTreeBundles(trees: List<TreeCaptureEntity>) {
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

        val jsonBundle = gson.toJson(UploadBundle.createV1(newTreeRequests = treeRequestList))

        // Create a hash ID to reference this upload bundle later
        val bundleId = jsonBundle.md5()

        // Update the trees in DB with the bundleId
        dao.updateTreeCapturesBundleIds(trees.map { it.id }, bundleId)
        objectStorageClient.uploadBundle(jsonBundle, bundleId)
        dao.updateTreeCapturesUploadStatus(trees.map { it.id }, true)
        log("Bundle Tree Upload Completed")
    }

    private suspend fun uploadTreeBundles(trees: List<TreeEntity>) {
        log("Uploading Tree Bundle...")
        // Create a request object for each tree
        val treeRequestList = trees.map { tree ->
            val sessionUuid = dao.getSessionById(tree.sessionId).uuid
            TreeCaptureRequest(
                sessionId = sessionUuid,
                treeId = tree.uuid,
                lat = tree.latitude,
                lon = tree.longitude,
                note = tree.note,
                imageUrl = tree.photoUrl ?: "",
                createdAt = tree.createdAt,
                stepCount = null,
                deltaStepCount = null,
                rotationMatrix = null,
                extraAttributes = null // gson.toJson(tree.extraAttributes)  extra attributes disabled
            )
        }

        val jsonBundle = gson.toJson(UploadBundle.createV2(treeCaptures = treeRequestList))

        // Create a hash ID to reference this upload bundle later
        val bundleId = "${jsonBundle.md5()}_captures"

        // Update the trees in DB with the bundleId
        dao.updateTreesBundleIds(trees.map { it.id }, bundleId)
        objectStorageClient.uploadBundle(jsonBundle, bundleId)
        dao.updateTreesUploadStatus(trees.map { it.id }, true)
        log("Bundle Tree Upload Completed")
    }

    private fun deleteLocalImages(photoPaths: List<String?>) {
        log("Deleting local image files for uploaded...")

        photoPaths.mapNotNull { it }
            .forEach { photoPath ->
                val photoFile = File(photoPath)
                if (photoFile.exists()) {
                    photoFile.delete()
                }
            }

        log("Local image files deleted")
    }

    companion object {
        private const val TREE_BUNDLE_SIZE = 50
    }
}