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
import kotlinx.coroutines.ensureActive
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
        val messagesSynced =
            try {
                withContext(Dispatchers.IO) {
                    val instanceId =
                        try {
                            FirebaseInstallations.getInstance().id.await()
                        } catch (e: Exception) {
                            ""
                        }

                    // Messages must not block tree data: a failure here is reported, not thrown.
                    val messagesUploaded = executeNonFatalStep(SyncStep.MESSAGES, "Message Sync") { messagesRepo.syncMessages() }

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

                    messagesUploaded
                }
            } catch (e: Exception) {
                Timber.e("Error occurred during syncing data. ${e.localizedMessage}")
                syncProgressTracker.endSync(error = e.localizedMessage)
                return false
            }
        if (!messagesSynced) {
            // Report the failed message upload instead of success; the messages stay queued.
            syncProgressTracker.endSync(error = "Messages failed to upload")
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

    /** Like [executeTrackedStep], but a failure (exception or false) is reported and returned instead of thrown. */
    private suspend fun executeNonFatalStep(
        syncStep: SyncStep,
        tag: String,
        action: suspend () -> Boolean,
    ): Boolean {
        syncProgressTracker.startStep(syncStep)
        try {
            var result = false
            executeIfContextActive(tag) { result = action() }
            // executeIfContextActive skips the action and cancels when the sync was stopped
            coroutineContext.ensureActive()
            if (result) {
                syncProgressTracker.completeStep(syncStep)
            } else {
                syncProgressTracker.failStep(syncStep, "$tag failed")
            }
            return result
        } catch (e: CancellationException) {
            syncProgressTracker.failStep(syncStep, "$tag cancelled")
            throw e
        } catch (e: Exception) {
            syncProgressTracker.failStep(syncStep, "$tag failed: ${e.localizedMessage}")
            return false
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