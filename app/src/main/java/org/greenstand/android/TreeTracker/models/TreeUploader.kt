/*
 * Copyright 2023 Treetracker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.greenstand.android.TreeTracker.models

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
import java.io.File
import kotlin.coroutines.coroutineContext

class TreeUploader(
    private val uploadImageUseCase: UploadImageUseCase,
    private val objectStorageClient: ObjectStorageClient,
    private val createTreeRequestUseCase: CreateTreeRequestUseCase,
    private val dao: TreeTrackerDAO,
    private val json: Json,
) {
    fun log(msg: String) = Timber.tag("TreeUploader").d(msg)

    suspend fun uploadLegacyTrees(
        treeIds: List<Long>,
        instanceId: String,
    ) {
        windowedTreeUpload(treeIds) { treeIdBundle ->
            val legacyTrees = dao.getTreeCapturesByIds(treeIdBundle)
            uploadLegacyTreeImages(legacyTrees)

            // Only bundle trees whose image made it to storage. The rest stay pending for the next sync.
            val uploadedTrees = uploadLegacyTreeBundles(legacyTrees.filter { it.photoUrl != null }, instanceId)
            if (uploadedTrees.isEmpty()) return@windowedTreeUpload

            deleteLocalImages(uploadedTrees.map { it.localPhotoPath })
            dao.removeTreeCapturesLocalImagePaths(uploadedTrees.map { it.id })
        }
    }

    suspend fun uploadTrees(treeIds: List<Long>) {
        windowedTreeUpload(treeIds) { treeIdBundle ->
            val trees = dao.getTreesByIds(treeIdBundle)
            uploadTreeImages(trees)

            // Only bundle trees whose image made it to storage. The rest stay pending for the next sync.
            val uploadedTrees = uploadTreeBundles(trees.filter { it.photoUrl != null })
            if (uploadedTrees.isEmpty()) return@windowedTreeUpload

            deleteLocalImages(uploadedTrees.map { it.photoPath })
            dao.removeTreesLocalImagePaths(uploadedTrees.map { it.id })
        }
    }

    private suspend fun windowedTreeUpload(
        treeIds: List<Long>,
        onHandleUpload: suspend (List<Long>) -> Unit,
    ) {
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
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "NewTree upload failed")
            }
        }
        log("Completed upload for ${treeIds.size} trees")
    }

    private suspend fun uploadLegacyTreeImages(trees: List<TreeCaptureEntity>) {
        log("Uploading tree images...")
        coroutineScope {
            trees
                .filter { it.photoUrl == null } // Upload photo only if it hasn't been saved in the DB (hasn't been uploaded yet)
                .map { tree ->
                    async {
                        val imageUrl =
                            uploadTreeImage(tree.id, tree.localPhotoPath, tree.latitude, tree.longitude)
                                ?: return@async

                        // Update local tree data with image Url
                        tree.photoUrl = imageUrl
                        dao.updateTreeCapture(tree)
                    }
                }.awaitAll()
        }
        log("Tree Image Upload Completed")
    }

    private suspend fun uploadTreeImages(trees: List<TreeEntity>) {
        log("Uploading tree images...")
        coroutineScope {
            trees
                .filter { it.photoUrl == null } // Upload photo only if it hasn't been saved in the DB (hasn't been uploaded yet)
                .map { tree ->
                    async {
                        val imageUrl =
                            uploadTreeImage(tree.id, tree.photoPath, tree.latitude, tree.longitude)
                                ?: return@async

                        // Update local tree data with image Url
                        tree.photoUrl = imageUrl
                        dao.updateTree(tree)
                    }
                }.awaitAll()
        }

        log("Tree Image Upload Completed")
    }

    /**
     * Uploads one tree's image and returns its URL, or null if it could not be uploaded.
     * Failures are contained to this tree so a single missing or failed image can't block
     * the rest of its bundle; the tree keeps a null photoUrl and is retried on the next sync.
     */
    private suspend fun uploadTreeImage(
        treeId: Long,
        imagePath: String?,
        lat: Double,
        long: Double,
    ): String? {
        if (imagePath == null) {
            Timber.e("Tree $treeId has no local image to upload")
            return null
        }
        val imageUrl =
            try {
                uploadImageUseCase.execute(UploadImageParams(imagePath = imagePath, lat = lat, long = long))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Image upload failed for tree $treeId")
                return null
            }
        if (imageUrl == null) {
            Timber.e("Image upload failed for tree $treeId")
        }
        return imageUrl
    }

    /** Uploads one bundle for [trees] and returns the trees it contains. */
    private suspend fun uploadLegacyTreeBundles(
        trees: List<TreeCaptureEntity>,
        instanceId: String,
    ): List<TreeCaptureEntity> {
        log("Uploading Tree Bundle...")
        // Create a request object for each tree
        val requests =
            buildTreeRequests(trees, { it.id }) { tree ->
                createTreeRequestUseCase.execute(
                    CreateTreeRequestParams(
                        tree.id,
                        tree.photoUrl!!,
                    ),
                )
            }
        if (requests.isEmpty()) return emptyList()
        val bundledTrees = requests.map { it.first }

        val jsonBundle = json.encodeToString(UploadBundle.createV1(newTreeRequests = requests.map { it.second }, instanceId = instanceId))

        // Create a hash ID to reference this upload bundle later
        val bundleId = jsonBundle.md5()

        // Update the trees in DB with the bundleId
        dao.updateTreeCapturesBundleIds(bundledTrees.map { it.id }, bundleId)
        objectStorageClient.uploadBundle(jsonBundle, bundleId)
        dao.updateTreeCapturesUploadStatus(bundledTrees.map { it.id }, true)
        log("Bundle Tree Upload Completed")
        return bundledTrees
    }

    /** Uploads one bundle for [trees] and returns the trees it contains. */
    private suspend fun uploadTreeBundles(trees: List<TreeEntity>): List<TreeEntity> {
        log("Uploading Tree Bundle...")
        // Create a request object for each tree
        val requests =
            buildTreeRequests(trees, { it.id }) { tree ->
                val sessionUuid = dao.getSessionById(tree.sessionId).uuid
                TreeCaptureRequest(
                    sessionId = sessionUuid,
                    treeId = tree.uuid,
                    lat = tree.latitude,
                    lon = tree.longitude,
                    note = tree.note,
                    imageUrl = tree.photoUrl ?: "",
                    createdAt = tree.createdAt.toString(),
                    stepCount = null,
                    deltaStepCount = null,
                    rotationMatrix = null,
                    extraAttributes = null, // gson.toJson(tree.extraAttributes)  extra attributes disabled
                )
            }
        if (requests.isEmpty()) return emptyList()
        val bundledTrees = requests.map { it.first }

        val jsonBundle = json.encodeToString(UploadBundle.createV2(treeCaptures = requests.map { it.second }))

        // Create a hash ID to reference this upload bundle later
        val bundleId = "${jsonBundle.md5()}_captures"

        // Update the trees in DB with the bundleId
        dao.updateTreesBundleIds(bundledTrees.map { it.id }, bundleId)
        objectStorageClient.uploadBundle(jsonBundle, bundleId)
        dao.updateTreesUploadStatus(bundledTrees.map { it.id }, true)
        log("Bundle Tree Upload Completed")
        return bundledTrees
    }

    /**
     * Builds the upload request for each tree. A tree whose request can't be built (e.g. its
     * planter check-in or session row is missing) is logged and left out, so it can't block the
     * rest of the bundle; it stays pending and is retried on the next sync.
     */
    private suspend fun <T, R> buildTreeRequests(
        trees: List<T>,
        treeId: (T) -> Long,
        buildRequest: suspend (T) -> R,
    ): List<Pair<T, R>> =
        trees.mapNotNull { tree ->
            try {
                tree to buildRequest(tree)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Could not build the upload request for tree ${treeId(tree)}")
                null
            }
        }

    private fun deleteLocalImages(photoPaths: List<String?>) {
        log("Deleting local image files for uploaded...")

        photoPaths
            .mapNotNull { it }
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