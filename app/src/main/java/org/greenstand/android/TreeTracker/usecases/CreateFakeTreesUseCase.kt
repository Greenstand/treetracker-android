package org.greenstand.android.TreeTracker.usecases

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.database.legacy.entity.PlanterCheckInEntity
import org.greenstand.android.TreeTracker.database.legacy.entity.PlanterInfoEntity
import org.greenstand.android.TreeTracker.models.SessionTracker
import org.greenstand.android.TreeTracker.models.Tree
import org.greenstand.android.TreeTracker.models.location.LocationUpdateManager
import org.greenstand.android.TreeTracker.utilities.ImageUtils
import java.util.*

data class CreateFakeTreesParams(val amount: Int)

class CreateFakeTreesUseCase(
    private val sessionTracker: SessionTracker,
    private val context: Context,
    private val createTreeUseCase: CreateTreeUseCase,
    private val dao: TreeTrackerDAO,
    private val locationUpdateManager: LocationUpdateManager,
    private val createLegacyTreeUseCase: CreateLegacyTreeUseCase,
) : UseCase<CreateFakeTreesParams, Unit>() {

    override suspend fun execute(params: CreateFakeTreesParams) {
        for (i in 1..params.amount / 2) {

            val file = ImageUtils.createTestImageFile(context)

            val tree = Tree(
                sessionId = sessionTracker.currentSessionId,
                photoPath = file.absolutePath,
                content = "My Note",
                treeUuid = UUID.randomUUID(),
                meanLongitude = 0.0,
                meanLatitude = 0.0
            )

            createTreeUseCase.execute(tree)
        }

        val legacyTreeAmount = params.amount / 2

        // Create user 1
        // User 1 checks in 2 times and plants trees
        // Create user 2
        // User 2 checks in 1 time and plants trees
        // User 1 checks in 1 time and plants trees

        val planterOne = createLegacyUser(
            firstName = "TestUser1",
            lastName = "LastName1",
            organization = "Umbrella Corp",
            phone = "123-421-1203",
            email = "testEmail1@gmail.com",
            identifier = "@TestUser1"
        )
        createLegacyCheckIn(planterOne.id)
        for (i in 1..legacyTreeAmount / 4) {
            createLegacyTree(planterOne.id)
        }
        createLegacyCheckIn(planterOne.id)
        for (i in 1..legacyTreeAmount / 4) {
            createLegacyTree(planterOne.id)
        }

        val planterTwo = createLegacyUser(
            firstName = "TestUser2",
            lastName = "LastName2",
            organization = "Umbrella Corp",
            phone = "123-421-9499",
            email = "testEmail2@gmail.com",
            identifier = "@TestUser2"
        )
        createLegacyCheckIn(planterTwo.id)
        for (i in 1..legacyTreeAmount / 4) {
            createLegacyTree(planterTwo.id)
        }

        createLegacyCheckIn(planterOne.id)
        for (i in 1..legacyTreeAmount / 4) {
            createLegacyTree(planterOne.id)
        }
    }

    suspend fun createLegacyUser(
        firstName: String,
        lastName: String,
        organization: String?,
        phone: String?,
        email: String?,
        identifier: String,
    ): PlanterInfoEntity {
        return withContext(Dispatchers.IO) {
            val location = locationUpdateManager.currentLocation
            val time = location?.time ?: System.currentTimeMillis()

            val entity = PlanterInfoEntity(
                identifier = identifier + time,
                firstName = firstName,
                lastName = lastName,
                organization = organization,
                phone = phone,
                email = email,
                longitude = location?.longitude ?: 0.0,
                latitude = location?.latitude ?: 0.0,
                createdAt = time,
                uploaded = false,
                recordUuid = UUID.randomUUID().toString(),
            )

            val id = dao.insertPlanterInfo(entity)
            dao.getPlanterInfoById(id)!!
        }
    }

    suspend fun createLegacyCheckIn(
        planterInfoId: Long,
    ) {
        val location = locationUpdateManager.currentLocation
        val time = location?.time ?: System.currentTimeMillis()

        val planterCheckInEntity = PlanterCheckInEntity(
            planterInfoId = planterInfoId,
            localPhotoPath = ImageUtils.createTestImageFile(context).absolutePath,
            longitude = location?.longitude ?: 0.0,
            latitude = location?.latitude ?: 0.0,
            createdAt = time,
            photoUrl = null
        )

        dao.insertPlanterCheckIn(planterCheckInEntity)
    }

    suspend fun createLegacyTree(planterInfoId: Long) {
        createLegacyTreeUseCase.execute(
            CreateLegacyTreeParams(
                planterInfoId,
                Tree(
                    sessionId = sessionTracker.currentSessionId,
                    photoPath = ImageUtils.createTestImageFile(context).absolutePath,
                    content = "My Legacy Note",
                    treeUuid = UUID.randomUUID(),
                    meanLongitude = 0.0,
                    meanLatitude = 0.0
                )
            )
        )
    }
}
