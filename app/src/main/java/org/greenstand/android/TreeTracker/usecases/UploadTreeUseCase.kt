package org.greenstand.android.TreeTracker.usecases

import org.greenstand.android.TreeTracker.api.RetrofitApi
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO

data class UploadTreeParams(val treeId: Long,
                            val treeImageUrl: String)

class UploadTreeUseCase(private val api: RetrofitApi,
                        private val dao: TreeTrackerDAO,
                        private val createTreeRequestUseCase: CreateTreeRequestUseCase) : UseCase<UploadTreeParams, Unit>() {

    override suspend fun execute(params: UploadTreeParams) {

        val newTreeRequest = createTreeRequestUseCase.execute(CreateTreeRequestParams(params.treeId, params.treeImageUrl))

        api.createTree(newTreeRequest)

        // Update tree db object with update status
        val treeCapture = dao.getTreeCaptureById(params.treeId)
        treeCapture.uploaded = true
        dao.updateTreeCapture(treeCapture)
    }
}