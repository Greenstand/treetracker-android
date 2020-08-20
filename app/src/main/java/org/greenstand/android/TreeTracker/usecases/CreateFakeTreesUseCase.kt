package org.greenstand.android.TreeTracker.usecases

import android.content.Context
import java.util.UUID
import org.greenstand.android.TreeTracker.managers.FeatureFlags
import org.greenstand.android.TreeTracker.managers.LocationUpdateManager
import org.greenstand.android.TreeTracker.managers.UserManager
import org.greenstand.android.TreeTracker.utilities.ImageUtils

data class CreateFakeTreesParams(val amount: Int)

class CreateFakeTreesUseCase(
    private val locationUpdateManager: LocationUpdateManager,
    private val userManager: UserManager,
    private val context: Context,
    private val createTreeUseCase: CreateTreeUseCase
) : UseCase<CreateFakeTreesParams, Unit>() {

    override suspend fun execute(params: CreateFakeTreesParams) {
        if (locationUpdateManager.currentLocation == null && FeatureFlags.HIGH_GPS_ACCURACY) {
            return
        }

        for (i in 0..params.amount) {

            val file = ImageUtils.createTestImageFile(context)

            val createTreeParams = CreateTreeParams(
                planterCheckInId = userManager.planterCheckinId!!,
                photoPath = file.absolutePath,
                content = "My Note",
                treeUuid = UUID.randomUUID()
            )

            createTreeUseCase.execute(createTreeParams)
        }
    }
}
