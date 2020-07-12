package org.greenstand.android.TreeTracker.usecases

import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import timber.log.Timber

class SyncDataUseCase(
    private val treeLoadStrategy: TreeUploadStrategy,
    private val uploadPlanterDetailsUseCase: UploadPlanterUseCase,
    private val uploadLocationDataUseCase: UploadLocationDataUseCase,
    private val dao: TreeTrackerDAO
) : UseCase<Unit, Boolean>() {

    private val TAG = "SyncDataUseCase"

    override suspend fun execute(params: Unit): Boolean {
        withContext(Dispatchers.IO) {

            uploadPlanters()

            var treeIdList = dao.getAllTreeCaptureIdsToUpload()

            while (treeIdList.isNotEmpty() && coroutineContext.isActive) {
                uploadTrees(treeIdList)
                val remainingIds = dao.getAllTreeCaptureIdsToUpload()
                if (!treeIdList.containsAll(remainingIds)) {
                    treeIdList = remainingIds
                } else {
                    if (remainingIds.isNotEmpty()) {
                        Timber.tag(TAG)
                            .e("Remaining trees failed to upload, ending sync...")
                    }
                    break
                }
            }
            uploadTreeLocationData()
        }
        return true
    }

    private suspend fun uploadPlanters() {
        // Upload all user registration data that hasn't been uploaded yet
        val planterInfoToUploadList = dao.getAllPlanterInfoToUpload()

        Timber.tag(TAG)
            .d("Uploading Planter Info for ${planterInfoToUploadList.size} planters")

        val planterIdsToUpload = planterInfoToUploadList.map { it.id }

        if (coroutineContext.isActive) {
            runCatching {
                uploadPlanterDetailsUseCase.execute(
                    UploadPlanterParams(planterInfoIds = planterIdsToUpload))
            }
        } else {
            coroutineContext.cancel()
        }
    }

    private suspend fun uploadTrees(treeIds: List<Long>) {
        Timber.tag(TAG).d("Uploading ${treeIds.size} trees")
        try {
            if (coroutineContext.isActive) {
                treeLoadStrategy.uploadTrees(treeIds)
            } else {
                coroutineContext.cancel()
            }
        } catch (e: Exception) {
            Timber.e("NewTree upload failed")
        }
    }

    private suspend fun uploadTreeLocationData() {
        Timber.tag(TAG).d("Processing tree location data")
        kotlin.runCatching {
            uploadLocationDataUseCase.execute(Unit)
            Timber.tag(TAG).d("Uploading tree location data complete")
        }
    }
}
