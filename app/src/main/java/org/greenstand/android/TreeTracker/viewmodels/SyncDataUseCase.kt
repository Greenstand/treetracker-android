package org.greenstand.android.TreeTracker.viewmodels

import org.greenstand.android.TreeTracker.api.RetrofitApi
import org.greenstand.android.TreeTracker.database.v2.TreeTrackerDAO
import org.greenstand.android.TreeTracker.usecases.*
import timber.log.Timber

class SyncDataUseCase(private val syncTreeUseCase: SyncTreeUseCase,
                      private val uploadPlanterDetailsUseCase: UploadPlanterUseCase,
                      private val api: RetrofitApi,
                      private val dao: TreeTrackerDAO) : UseCase<Unit, Unit>() {

    override suspend fun execute(params: Unit) {

        if (api.authenticateDevice()) {
            Timber.tag("SyncDataUseCase").w("Device Authentication failed")
            return
        }

        uploadPlanters()

        var treeIdList = dao.getAllTreeCaptureIdsToUpload()

        while(treeIdList.isNotEmpty()) {
            uploadTrees(treeIdList)
            treeIdList = dao.getAllTreeCaptureIdsToUpload()
        }
    }

    private suspend fun uploadPlanters() {
        // Upload all user registration data that hasn't been uploaded yet
        val planterInfoToUploadList = dao.getAllPlanterInfo()

        Timber.tag("SyncDataUseCase").d("Uploading Planter Info for ${planterInfoToUploadList.size} planters")

        planterInfoToUploadList.forEach {
            runCatching {
                uploadPlanterDetailsUseCase.execute(UploadPlanterParams(planterInfoId = it.id))
            }
        }
    }

    private suspend fun uploadTrees(treeIds: List<Long>) {

        Timber.tag("SyncDataUseCase").d("Uploading ${treeIds.size} trees")

        treeIds.onEach {
            try {
                syncTreeUseCase.execute(SyncTreeParams(treeId = it))
            } catch (e: Exception) {
                Timber.e("NewTree upload failed")
            }
        }
    }

}