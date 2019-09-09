package org.greenstand.android.TreeTracker.usecases

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.greenstand.android.TreeTracker.api.RetrofitApi
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import timber.log.Timber
import kotlin.coroutines.coroutineContext

class SyncDataUseCase(private val treeLoadStrategy: TreeUploadStrategy,
                      private val uploadPlanterDetailsUseCase: UploadPlanterUseCase,
                      private val api: RetrofitApi,
                      private val dao: TreeTrackerDAO
) : UseCase<Unit, Boolean>() {

    override suspend fun execute(params: Unit): Boolean {
        withContext(Dispatchers.IO) {
            if (!api.authenticateDevice()) {
                Timber.tag("SyncDataUseCase").w("Device Authentication failed")
                return@withContext false
            }

            uploadPlanters()

            var treeIdList = dao.getAllTreeCaptureIdsToUpload()

            while (treeIdList.isNotEmpty() && coroutineContext.isActive) {
                    uploadTrees(treeIdList)
                    val remainingIds = dao.getAllTreeCaptureIdsToUpload()
                    if (!treeIdList.containsAll(remainingIds)) {
                        treeIdList = remainingIds
                    } else {
                        if (remainingIds.isNotEmpty()) {
                            Timber.tag("SyncDataUseCase").e("Remaining trees failed to upload, ending sync...")
                        }
                        break
                    }
            }
        }
        return true
    }

    private suspend fun uploadPlanters() {
        // Upload all user registration data that hasn't been uploaded yet
        val planterInfoToUploadList = dao.getAllPlanterInfo()

        Timber.tag("SyncDataUseCase").d("Uploading Planter Info for ${planterInfoToUploadList.size} planters")

        planterInfoToUploadList.forEach {
            if (coroutineContext.isActive) {
                runCatching {
                    uploadPlanterDetailsUseCase.execute(UploadPlanterParams(planterInfoId = it.id))
                }
            } else {
                coroutineContext.cancel()
            }

        }
    }

    private suspend fun uploadTrees(treeIds: List<Long>) {
        Timber.tag("SyncDataUseCase").d("Uploading ${treeIds.size} trees")

        treeIds.onEach {
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
    }

    companion object {
        const val CONTINUOUS_UPLOAD = "continuous_upload"
        const val BUNDLE_UPLOAD = "bundle_upload"
    }
}