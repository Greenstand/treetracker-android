package org.greenstand.android.TreeTracker.usecases

import org.greenstand.android.TreeTracker.api.RetrofitApi
import org.greenstand.android.TreeTracker.api.models.requests.AttributeRequest
import org.greenstand.android.TreeTracker.api.models.requests.NewTreeRequest
import org.greenstand.android.TreeTracker.database.AppDatabase
import org.greenstand.android.TreeTracker.database.v2.TreeTrackerDAO
import org.greenstand.android.TreeTracker.managers.TreeManager
import org.greenstand.android.TreeTracker.managers.UserManager
import org.greenstand.android.TreeTracker.utilities.Utils
import timber.log.Timber

data class UploadTreeParams(val treeId: Long,
                            val treeImageUrl: String)

class UploadTreeUseCase(private val userManager: UserManager,
                        private val api: RetrofitApi,
                        private val dao: TreeTrackerDAO) : UseCase<UploadTreeParams, Unit>() {

    override suspend fun execute(params: UploadTreeParams) {

        val treeUploadData = dao.getTreeUploadDataById(params.treeId)

        val attributesList = dao.getTreeAttributeByTreeCaptureId(treeUploadData.treeCaptureId)
        val attributesRequest =  mutableListOf<AttributeRequest>()
        for (attribute in attributesList){
            attributesRequest.add(AttributeRequest(key=attribute.key, value=attribute.value))
        }

        val newTreeRequest = NewTreeRequest(
            uuid = treeUploadData.uuid,
            imageUrl = params.treeImageUrl,
            userId = userManager.userId.toInt(),
            sequenceId = treeUploadData.treeCaptureId,
            lat = treeUploadData.latitude,
            lon = treeUploadData.longitude,
            gpsAccuracy = treeUploadData.accuracy.toInt(),
            planterIdentifier = treeUploadData.identifier,
            planterPhotoUrl = treeUploadData.planterPhotoUrl,
            timestamp = treeUploadData.createAt,
            note = treeUploadData.noteContent,
            attributes = attributesRequest
        )

        api.createTree(newTreeRequest)

        // Update tree db object with update status
        val treeEntity = dao.getTreeCaptureById(treeUploadData.treeCaptureId)
        treeEntity.uploaded = true
        dao.updateTreeCapture(treeEntity)

    }
}
