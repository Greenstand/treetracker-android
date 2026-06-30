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
import kotlinx.coroutines.ensureActive
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
        var overallSuccess = true

        try {
            withContext(Dispatchers.IO) {
                val instanceId =
                    try {
                        FirebaseInstallations.getInstance().id.await()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Timber.tag(TAG).e(e, "Failed to get Firebase instance ID")
                        ""
                    }

                if (!executeTrackedStep(SyncStep.MESSAGES) {
                        messagesRepo.syncMessages()
                    }
                ) {
                    overallSuccess = false
                }

                if (!executeTrackedStep(SyncStep.DEVICE_CONFIG) {
                        deviceConfigUploader.upload(instanceId)
                    }
                ) {
                    overallSuccess = false
                }

                if (!executeTrackedStep(SyncStep.USERS) {
                        planterUploader.upload(instanceId)
                    }
                ) {
                    overallSuccess = false
                }

                if (!executeTrackedStep(SyncStep.SESSIONS) {
                        sessionUploader.upload()
                    }
                ) {
                    overallSuccess = false
                }

                if (!treeUpload(
                        syncStep = SyncStep.LEGACY_TREES,
                        onGetTreeIds = { dao.getAllTreeCaptureIdsToUpload() },
                        onUpload = { treeUploader.uploadLegacyTrees(it, instanceId) },
                    )
                ) {
                    overallSuccess = false
                }

                if (!treeUpload(
                        syncStep = SyncStep.TREES,
                        onGetTreeIds = { dao.getAllTreeIdsToUpload() },
                        onUpload = { treeUploader.uploadTrees(it) },
                    )
                ) {
                    overallSuccess = false
                }

                if (!executeTrackedStep(SyncStep.LOCATIONS) {
                        uploadLocationDataUseCase.execute(Unit)
                    }
                ) {
                    overallSuccess = false
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error occurred during syncing data")
            syncProgressTracker.endSync(error = e.localizedMessage)
            return false
        }

        if (overallSuccess) {
            syncProgressTracker.endSync()
        } else {
            syncProgressTracker.endSync(error = "One or more sync steps failed")
        }
        return overallSuccess
    }

    private suspend fun treeUpload(
        syncStep: SyncStep,
        onGetTreeIds: suspend () -> List<Long>,
        onUpload: suspend (List<Long>) -> Unit,
    ): Boolean {
        syncProgressTracker.startStep(syncStep)
        try {
            var treeIds = onGetTreeIds()
            val totalTrees = treeIds.size
            var uploadedSoFar = 0
            syncProgressTracker.updateStepProgress(syncStep, 0, totalTrees)

            while (treeIds.isNotEmpty()) {
                coroutineContext.ensureActive()
                try {
                    executeIfContextActive {
                        onUpload(treeIds)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // Log and let the loop handle it or break
                    Timber.tag(TAG).e(e, "Tree upload batch failed")
                    // We don't throw here to allow progress tracker to mark failure and continue if needed,
                    // but since this is a while loop on IDs, we might get stuck if we don't break.
                    syncProgressTracker.failStep(syncStep, e.localizedMessage)
                    return false
                }

                uploadedSoFar += treeIds.size
                syncProgressTracker.updateStepProgress(syncStep, uploadedSoFar, totalTrees)

                val remainingIds = onGetTreeIds()
                if (remainingIds.isNotEmpty() && treeIds.containsAll(remainingIds)) {
                    Timber.tag(TAG).e("Remaining trees failed to upload, ending tree sync...")
                    syncProgressTracker.failStep(syncStep, "Remaining trees failed to upload")
                    return false
                }
                treeIds = remainingIds
            }
            syncProgressTracker.completeStep(syncStep)
            return true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            syncProgressTracker.failStep(syncStep, e.localizedMessage)
            return false
        }
    }

    private suspend fun executeTrackedStep(
        syncStep: SyncStep,
        action: suspend () -> Unit,
    ): Boolean {
        syncProgressTracker.startStep(syncStep)
        return try {
            executeIfContextActive(action)
            syncProgressTracker.completeStep(syncStep)
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            syncProgressTracker.failStep(syncStep, e.localizedMessage)
            false
        }
    }

    private suspend fun executeIfContextActive(
        action: suspend () -> Unit,
    ) {
        coroutineContext.ensureActive()
        action()
    }
}