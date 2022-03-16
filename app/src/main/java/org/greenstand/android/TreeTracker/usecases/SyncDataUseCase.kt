package org.greenstand.android.TreeTracker.usecases

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.models.DeviceConfigUploader
import org.greenstand.android.TreeTracker.models.FeatureFlags
import org.greenstand.android.TreeTracker.models.PlanterUploader
import org.greenstand.android.TreeTracker.models.SessionUploader
import org.greenstand.android.TreeTracker.models.TreeUploader
import org.greenstand.android.TreeTracker.models.messages.MessagesRepo
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
) : UseCase<Unit, Boolean>() {

    private val TAG = "SyncDataUseCase"

    override suspend fun execute(params: Unit): Boolean {
        try {
            withContext(Dispatchers.IO) {

                if (FeatureFlags.DEBUG_ENABLED) {
                    executeIfContextActive("Message Sync") {
                        messagesRepo.syncMessages()
                    }
                }

                executeIfContextActive("Device Config Upload") {
                    deviceConfigUploader.upload()
                }

                executeIfContextActive("User Upload") {
                    planterUploader.upload()
                }

                executeIfContextActive("Session Upload") {
                    sessionUploader.upload()
                }

                treeUpload(
                    onGetTreeIds = { dao.getAllTreeCaptureIdsToUpload() },
                    onUpload = { treeUploader.uploadLegacyTrees(it) }
                )

                treeUpload(
                    onGetTreeIds = { dao.getAllTreeIdsToUpload() },
                    onUpload = { treeUploader.uploadTrees(it) }
                )

                executeIfContextActive("Location Upload") {
                    uploadLocationDataUseCase.execute(Unit)
                }

            }
        } catch(e: Exception) {
            Timber.e("Error occured during syncing data. ${e.localizedMessage}")
            return false
        }
        return true
    }

    private suspend fun treeUpload(
        onGetTreeIds: suspend () -> List<Long>,
        onUpload: suspend (List<Long>) -> Unit
    ) {
        var treeIds = onGetTreeIds()
        while (treeIds.isNotEmpty() && coroutineContext.isActive) {
            executeIfContextActive("Tree Upload") {
                onUpload(treeIds)
            }
            val remainingIds = onGetTreeIds()
            if (!treeIds.containsAll(remainingIds)) {
                treeIds = remainingIds
            } else {
                if (remainingIds.isNotEmpty()) {
                    Timber.tag(TAG)
                        .e("Remaining trees failed to upload, ending tree sync...")
                }
                break
            }
        }
    }

    private suspend fun executeIfContextActive(tag: String, action: suspend () -> Unit) {
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
