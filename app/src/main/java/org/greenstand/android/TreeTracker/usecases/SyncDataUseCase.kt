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
import kotlinx.coroutines.CancellationException
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

                treeUpload(
                    syncStep = SyncStep.LEGACY_TREES,
                    onGetTreeIds = { dao.getAllTreeCaptureIdsToUpload() },
                    onUpload = { treeUploader.uploadLegacyTrees(it, instanceId) },
                )

                treeUpload(
                    syncStep = SyncStep.TREES,
                    onGetTreeIds = { dao.getAllTreeIdsToUpload() },
                    onUpload = { treeUploader.uploadTrees(it) },
                )

                executeTrackedStep(SyncStep.LOCATIONS, "Location Upload") {
                    uploadLocationDataUseCase.execute(Unit)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Underlying uploaders already recorded the failure type; just log here so we don't
            // double-count the same failure as unknown.
            Timber.tag(TAG).e(e, "Error occurred during syncing data")
            syncProgressTracker.endSync(error = e.localizedMessage)
            return false
        }
        syncProgressTracker.endSync()
        return true
    }

    private suspend fun treeUpload(
        syncStep: SyncStep,
        onGetTreeIds: suspend () -> List<Long>,
        onUpload: suspend (List<Long>) -> Unit,
    ) {
        syncProgressTracker.startStep(syncStep)
        try {
            var treeIds = onGetTreeIds()
            val totalTrees = treeIds.size
            var uploadedSoFar = 0
            syncProgressTracker.updateStepProgress(syncStep, 0, totalTrees)

            while (treeIds.isNotEmpty() && coroutineContext.isActive) {
                executeIfContextActive("Tree Upload") {
                    onUpload(treeIds)
                }
                uploadedSoFar += treeIds.size
                syncProgressTracker.updateStepProgress(syncStep, uploadedSoFar, totalTrees)

                val remainingIds = onGetTreeIds()
                if (!treeIds.containsAll(remainingIds)) {
                    treeIds = remainingIds
                } else {
                    if (remainingIds.isNotEmpty()) {
                        Timber.tag(TAG).e("Remaining trees failed to upload, ending tree sync...")
                    }
                    break
                }
            }
            syncProgressTracker.completeStep(syncStep)
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
            // Underling uploaders set the FAILURE_TYPE already, so we just log here
            Timber.tag(TAG).e(e, "$tag failed")
            throw e
        }
    }
}