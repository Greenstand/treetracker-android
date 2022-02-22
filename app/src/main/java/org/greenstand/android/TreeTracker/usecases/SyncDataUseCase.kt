package org.greenstand.android.TreeTracker.usecases

import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.models.PlanterUploader
import org.greenstand.android.TreeTracker.models.SessionUploader
import org.greenstand.android.TreeTracker.models.TreeUploader
import timber.log.Timber

class SyncDataUseCase(
    private val treeUploader: TreeUploader,
    private val uploadLocationDataUseCase: UploadLocationDataUseCase,
    private val dao: TreeTrackerDAO,
    private val planterUploader: PlanterUploader,
    private val sessionUploader: SessionUploader,
) : UseCase<Unit, Boolean>() {

    private val TAG = "SyncDataUseCase"

    override suspend fun execute(params: Unit): Boolean {
        withContext(Dispatchers.IO) {

            safeWork("User Upload") {
                planterUploader.uploadPlanters()
            }

            safeWork("Session Upload") {
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

            safeWork("Location Upload") {
                uploadLocationDataUseCase.execute(Unit)
            }
        }
        return true
    }

    private suspend fun treeUpload(
        onGetTreeIds: suspend () -> List<Long>,
        onUpload: suspend (List<Long>) -> Unit
    ) {
        var treeIds = onGetTreeIds()
        while (treeIds.isNotEmpty() && coroutineContext.isActive) {
            safeWork("Tree Upload") {
                onUpload(treeIds)
            }
            val remainingIds = dao.getAllTreeIdsToUpload()
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

    private suspend fun safeWork(tag: String, action: suspend () -> Unit) {
        try {
            if (coroutineContext.isActive) {
                action()
            } else {
                coroutineContext.cancel()
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e("$tag -> ${e.localizedMessage}")
        }
    }
}
