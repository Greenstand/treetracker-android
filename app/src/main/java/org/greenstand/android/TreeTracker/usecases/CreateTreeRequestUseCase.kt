package org.greenstand.android.TreeTracker.usecases

import org.greenstand.android.TreeTracker.api.models.requests.AttributeRequest
import org.greenstand.android.TreeTracker.api.models.requests.NewTreeRequest
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.utilities.DeviceUtils

data class CreateTreeRequestParams(
    val treeId: Long,
    val treeImageUrl: String
)

class CreateTreeRequestUseCase(private val dao: TreeTrackerDAO) :
    UseCase<CreateTreeRequestParams, NewTreeRequest>() {

    override suspend fun execute(params: CreateTreeRequestParams): NewTreeRequest {

        val treeCapture = dao.getTreeCaptureById(params.treeId)
        val planterCheckIn = dao.getPlanterCheckInById(treeCapture.planterCheckInId)
        val planterInfo = dao.getPlanterInfoById(planterCheckIn.planterInfoId)
            ?: throw IllegalStateException("No Planter Info")

        val attributesList = dao.getTreeAttributeByTreeCaptureId(treeCapture.id)
        val attributesRequest = mutableListOf<AttributeRequest>()
        for (attribute in attributesList) {
            attributesRequest.add(AttributeRequest(key = attribute.key, value = attribute.value))
        }

        return NewTreeRequest(
            uuid = treeCapture.uuid,
            imageUrl = params.treeImageUrl,
            userId = planterCheckIn.id.toInt(),
            sequenceId = treeCapture.id,
            deviceIdentifier = DeviceUtils.deviceId,
            lat = treeCapture.latitude,
            lon = treeCapture.longitude,
            gpsAccuracy = treeCapture.accuracy.toInt(),
            planterIdentifier = planterInfo.identifier,
            planterPhotoUrl = planterCheckIn.photoUrl,
            timestamp = treeCapture.createAt,
            note = treeCapture.noteContent,
            attributes = attributesRequest
        )
    }
}
