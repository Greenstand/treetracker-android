package org.greenstand.android.TreeTracker.models

import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.greenstand.android.TreeTracker.analytics.Analytics
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.database.entity.UserEntity
import org.greenstand.android.TreeTracker.database.legacy.entity.PlanterCheckInEntity
import org.greenstand.android.TreeTracker.models.user.User
import timber.log.Timber

class Users(
    private val locationUpdateManager: LocationUpdateManager,
    private val dao: TreeTrackerDAO,
    private val analytics: Analytics
) {

    var currentSessionUser: User? = null
        private set

    fun users(): Flow<List<User>> {
        return dao.getAllUsers()
            .map { userEntities -> userEntities.mapNotNull { createUser(it) } }
    }

    suspend fun getUserList(): List<User> {
        return dao.getAllUsersList().mapNotNull { createUser(it) }
    }

    suspend fun getUser(userId: Long): User? {
        return createUser(dao.getUserById(userId))
    }

    suspend fun getUserWithWallet(wallet: String): User? {
        return createUser(dao.getUserByWallet(wallet))
    }

    suspend fun getPowerUser(): User? {
        val userEntity = dao.getPowerUser() ?: return null
        return createUser(userEntity)
    }

    suspend fun createUser(
        firstName: String,
        lastName: String,
        organization: String?,
        phone: String?,
        email: String?,
        wallet: String,
        photoPath: String,
        isPowerUser: Boolean = false
    ): Long {
        return withContext(Dispatchers.IO) {

            val location = locationUpdateManager.currentLocation
            val time = location?.time ?: System.currentTimeMillis()

            val entity = UserEntity(
                wallet = wallet,
                firstName = firstName,
                lastName = lastName,
                organization = organization,
                phone = phone,
                email = email,
                longitude = location?.longitude ?: 0.0,
                latitude = location?.latitude ?: 0.0,
                createdAt = time,
                uploaded = false,
                uuid = UUID.randomUUID().toString(),
                powerUser = isPowerUser,
                photoPath = photoPath,
                photoUrl = null,
            )

            val userId = dao.insertUser(entity).also {
                analytics.userInfoCreated(
                    phone = phone.orEmpty(),
                    email = email.orEmpty()
                )
            }

            startUserSession(
                localPhotoPath = photoPath,
                planterInfoId = userId,
            )
            userId
        }
    }

    suspend fun doesUserExists(identifier: String): Boolean{
        return getUserWithWallet(identifier) != null
    }

    suspend fun startUserSession(
        localPhotoPath: String,
        planterInfoId: Long
    ) {
        endUserSession()
        withContext(Dispatchers.IO) {

            val location = locationUpdateManager.currentLocation
            val time = location?.time ?: System.currentTimeMillis()

            val planterCheckInEntity = PlanterCheckInEntity(
                planterInfoId = planterInfoId,
                localPhotoPath = localPhotoPath,
                longitude = location?.longitude ?: 0.0,
                latitude = location?.latitude ?: 0.0,
                createdAt = time,
                photoUrl = null
            )

            dao.insertPlanterCheckIn(planterCheckInEntity)

            dao.getPlanterInfoById(planterInfoId)?.let { planterInfo ->
//                currentSessionUser = createUser(planterInfo)
            } ?: Timber.e("Could not find planter info of id $planterInfoId")

            analytics.userCheckedIn()
        }
    }

    fun endUserSession() {
        currentSessionUser = null
    }

    private suspend fun createUser(userEntity: UserEntity?): User? {
        userEntity ?: return null
        return User(
            id = userEntity.id,
            wallet = userEntity.wallet,
            firstName = userEntity.firstName,
            lastName = userEntity.lastName,
            photoPath = userEntity.photoPath,
            isPowerUser = userEntity.powerUser,
            numberOfTrees = "a"//dao.getTreesByEachPlanter(planterInfoEntity.identifier).toString()
        )
    }
}
