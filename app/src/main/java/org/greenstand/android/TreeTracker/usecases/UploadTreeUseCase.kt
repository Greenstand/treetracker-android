package org.greenstand.android.TreeTracker.usecases

import org.greenstand.android.TreeTracker.api.RetrofitApi
import org.greenstand.android.TreeTracker.api.models.requests.AttributeRequest
import org.greenstand.android.TreeTracker.api.models.requests.NewTreeRequest
import org.greenstand.android.TreeTracker.database.AppDatabase
import org.greenstand.android.TreeTracker.managers.TreeManager
import org.greenstand.android.TreeTracker.managers.UserManager
import org.greenstand.android.TreeTracker.utilities.Utils

data class UploadTreeParams(val treeId: Long,
                            val treeImageUrl: String)

class UploadTreeUseCase(private val treeManager: TreeManager,
                        private val userManager: UserManager,
                        private val api: RetrofitApi,
                        private val db: AppDatabase) : UseCase<UploadTreeParams, Unit>() {

    override suspend fun execute(params: UploadTreeParams) {

        val tree = treeManager.getTree(params.treeId)

        val attributesList = treeManager.getTreeAttributes(tree.tree_id)
        val attributesRequest =  mutableListOf<AttributeRequest>()
        for (attribute in attributesList){
            attributesRequest.add(AttributeRequest(key=attribute.key, value=attribute.value))
        }

        val newTreeRequest = NewTreeRequest(
            uuid = tree.uuid,
            imageUrl = params.treeImageUrl,
            userId = userManager.userId.toInt(),
            sequenceId = tree.tree_id,
            lat = tree.latitude,
            lon = tree.longitude,
            gpsAccuracy = tree.accuracy,
            planterIdentifier = tree.planter_identifier,
            planterPhotoUrl = tree.planter_photo_url,
            timestamp = Utils.convertDateToTimestamp(tree.tree_time_created!!),
            note = tree.note,
            attributes = attributesRequest
        )

        val treeIdResponse: Int = api.createTree(newTreeRequest)

        // Update tree db object with update status
        val treeEntity = db.treeDao().getTreeByID(tree.tree_id)
        treeEntity.isSynced = true
        treeEntity.mainDbId = treeIdResponse
        db.treeDao().updateTree(treeEntity)

    }
}