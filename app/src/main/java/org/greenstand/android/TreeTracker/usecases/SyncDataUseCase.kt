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
package org.greenstand.android.TreeTracker.usecases

import com.google.firebase.installations.FirebaseInstallations
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.models.DeviceConfigUploader
import org.greenstand.android.TreeTracker.models.PlanterUploader
import org.greenstand.android.TreeTracker.models.SessionUploader
import org.greenstand.android.TreeTracker.models.TreeUploader
import org.greenstand.android.TreeTracker.models.messages.MessagesRepo
import org.greenstand.android.TreeTracker.overlay.SyncProgressTracker
import org.greenstand.android.TreeTracker.overlay.SyncStep
import timber.log.Timber
import kotlin.coroutines.coroutineContext

class SyncDataUseCase(
    private val treeUploader: TreeUploader,
    private val uploadLocationDataUseCase: UploadLocationDataUseCase,
    private val dao: TreeTrackerDAO,
    private val planterUploader: PlanterUploader,
    private val sessionUploader: SessionUploader,
    private val deviceConfigUploader: DeviceConfigUploader,
    private val messagesRepo: MessagesRepo,
    private val syncProgressTracker: SyncProgressTracker,
) : UseCase<Unit, Boolean>() {
    private val TAG = "SyncDataUseCase"

    override suspend fun execute(params: Unit): Boolean {
        syncProgressTracker.startSync()
        val allTreesUploaded =
            try {
                withContext(Dispatchers.IO) {
                    val instanceId =
                        try {
                            FirebaseInstallations.getInstance().id.await()
                        } catch (e: Exception) {
                            ""
                        }

                    executeTrackedStep(SyncStep.MESSAGES, "Message Sync") {
                        messagesRepo.syncMessages()
                    }

                    executeTrackedStep(SyncStep.DEVICE_CONFIG, "Device Config Upload") {
                        deviceConfigUploader.upload(instanceId)
                    }

                    executeTrackedStep(SyncStep.USERS, "User Upload") {
                        planterUploader.upload(instanceId)
                    }

                    executeTrackedStep(SyncStep.SESSIONS, "Session Upload") {
                        sessionUploader.upload()
                    }

                    val legacyTreesUploaded =
                        treeUpload(
                            syncStep = SyncStep.LEGACY_TREES,
                            onGetTreeIds = { dao.getAllTreeCaptureIdsToUpload() },
                            onUpload = { treeUploader.uploadLegacyTrees(it, instanceId) },
                        )

                    val treesUploaded =
                        treeUpload(
                            syncStep = SyncStep.TREES,
                            onGetTreeIds = { dao.getAllTreeIdsToUpload() },
                            onUpload = { treeUploader.uploadTrees(it) },
                        )

                    executeTrackedStep(SyncStep.LOCATIONS, "Location Upload") {
                        uploadLocationDataUseCase.execute(Unit)
                    }

                    legacyTreesUploaded && treesUploaded
                }
            } catch (e: Exception) {
                Timber.e("Error occurred during syncing data. ${e.localizedMessage}")
                syncProgressTracker.endSync(error = e.localizedMessage)
                return false
            }
        if (!allTreesUploaded) {
            // Report partial tree failures instead of success so the stranded trees aren't hidden.
            syncProgressTracker.endSync(error = "Some trees failed to upload")
            return false
        }
        syncProgressTracker.endSync()
        return true
    }

    /** Uploads trees until none remain or a pass finds no new trees. Returns true if every tree was uploaded. */
    private suspend fun treeUpload(
        syncStep: SyncStep,
        onGetTreeIds: suspend () -> List<Long>,
        onUpload: suspend (List<Long>) -> Unit,
    ): Boolean {
        syncProgressTracker.startStep(syncStep)
        try {
            var treeIds = onGetTreeIds()
            var remainingIds = treeIds
            val totalTrees = treeIds.size
            syncProgressTracker.updateStepProgress(syncStep, 0, totalTrees)

            while (treeIds.isNotEmpty() && coroutineContext.isActive) {
                executeIfContextActive("Tree Upload") {
                    onUpload(treeIds)
                }

                remainingIds = onGetTreeIds()
                syncProgressTracker.updateStepProgress(syncStep, (totalTrees - remainingIds.size).coerceAtLeast(0), totalTrees)
                if (!treeIds.containsAll(remainingIds)) {
                    treeIds = remainingIds
                } else {
                    break
                }
            }

            if (remainingIds.isNotEmpty()) {
                Timber.tag(TAG).e("${remainingIds.size} trees failed to upload, ending tree sync...")
                syncProgressTracker.failStep(syncStep, "${remainingIds.size} trees failed to upload")
                return false
            }
            syncProgressTracker.completeStep(syncStep)
            return true
        } catch (e: Exception) {
            syncProgressTracker.failStep(syncStep, e.localizedMessage)
            throw e
        }
    }

    private suspend fun executeTrackedStep(
        syncStep: SyncStep,
        tag: String,
        action: suspend () -> Unit,
    ) {
        syncProgressTracker.startStep(syncStep)
        try {
            executeIfContextActive(tag, action)
            syncProgressTracker.completeStep(syncStep)
        } catch (e: Exception) {
            syncProgressTracker.failStep(syncStep, e.localizedMessage)
            throw e
        }
    }

    private suspend fun executeIfContextActive(
        tag: String,
        action: suspend () -> Unit,
    ) {
        try {
            if (coroutineContext.isActive) {
                action()
            } else {
                coroutineContext.cancel()
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e("$tag -> ${e.localizedMessage}")
            throw e
        }
    }
}