package org.greenstand.android.TreeTracker.usecases

import org.greenstand.android.TreeTracker.api.RetrofitApi
import org.greenstand.android.TreeTracker.api.models.requests.AttributeRequest
import org.greenstand.android.TreeTracker.api.models.requests.NewTreeRequest
import org.greenstand.android.TreeTracker.database.v2.TreeTrackerDAO

data class UploadTreeParams(val treeId: Long,
                            val treeImageUrl: String)

class UploadTreeUseCase(private val api: RetrofitApi,
                        private val dao: TreeTrackerDAO) : UseCase<UploadTreeParams, Unit>() {

    override suspend fun execute(params: UploadTreeParams) {

        val treeCapture = dao.getTreeCaptureById(params.treeId)
        val planterCheckIn = dao.getPlanterCheckInById(treeCapture.planterCheckInId)
        val planterInfo = dao.getPlanterInfoById(planterCheckIn.planterInfoId) ?: throw IllegalStateException("No Planter Info")

        val attributesList = dao.getTreeAttributeByTreeCaptureId(treeCapture.id)
        val attributesRequest =  mutableListOf<AttributeRequest>()
        for (attribute in attributesList){
            attributesRequest.add(AttributeRequest(key=attribute.key, value=attribute.value))
        }

        val newTreeRequest = NewTreeRequest(
            uuid = treeCapture.uuid,
            imageUrl = params.treeImageUrl,
            userId = planterCheckIn.id.toInt(),
            sequenceId = treeCapture.id,
            lat = treeCapture.latitude,
            lon = treeCapture.longitude,
            gpsAccuracy = treeCapture.accuracy.toInt(),
            planterIdentifier = planterInfo.identifier,
            planterPhotoUrl = planterCheckIn.photoUrl,
            timestamp = treeCapture.createAt,
            note = treeCapture.noteContent,
            attributes = attributesRequest
        )

        api.createTree(newTreeRequest)

        // Update tree db object with update status
        treeCapture.uploaded = true
        dao.updateTreeCapture(treeCapture)

    }
}
