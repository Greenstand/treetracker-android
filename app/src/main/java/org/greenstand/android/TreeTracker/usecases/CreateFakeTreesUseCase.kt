package org.greenstand.android.TreeTracker.usecases

import android.content.Context
import java.util.UUID
import org.greenstand.android.TreeTracker.models.LocationUpdateManager
import org.greenstand.android.TreeTracker.models.Tree
import org.greenstand.android.TreeTracker.models.User
import org.greenstand.android.TreeTracker.utilities.ImageUtils

data class CreateFakeTreesParams(val amount: Int)

class CreateFakeTreesUseCase(
    private val locationUpdateManager: LocationUpdateManager,
    private val user: User,
    private val context: Context,
    private val createTreeUseCase: CreateTreeUseCase
) : UseCase<CreateFakeTreesParams, Unit>() {

    override suspend fun execute(params: CreateFakeTreesParams) {
        if (locationUpdateManager.currentLocation == null) {
            return
        }

        for (i in 0..params.amount) {

            val file = ImageUtils.createTestImageFile(context)

            val tree = Tree(
                planterCheckInId = user.planterCheckinId!!,
                photoPath = file.absolutePath,
                content = "My Note",
                treeUuid = UUID.randomUUID()
            )

            createTreeUseCase.execute(tree)
        }
    }
}
