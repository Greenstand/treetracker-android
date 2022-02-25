package org.greenstand.android.TreeTracker.models

import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.greenstand.android.TreeTracker.analytics.Analytics
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.database.entity.UserEntity
import org.greenstand.android.TreeTracker.models.location.LocationUpdateManager
import org.greenstand.android.TreeTracker.models.user.User

class Users(
    private val locationUpdateManager: LocationUpdateManager,
    private val dao: TreeTrackerDAO,
    private val analytics: Analytics
) {

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

            dao.insertUser(entity).also {
                analytics.userInfoCreated(
                    phone = phone.orEmpty(),
                    email = email.orEmpty()
                )
            }
        }
    }

    suspend fun doesUserExists(identifier: String): Boolean{
        return getUserWithWallet(identifier) != null
    }

    private suspend fun createUser(userEntity: UserEntity?): User? {
        userEntity ?: return null

        val treeCount = dao.getSessionsByUserWallet(userEntity.wallet).map {
            dao.getTreeCountFromSessionId(it.id)
        }.sum()

        return User(
            id = userEntity.id,
            wallet = userEntity.wallet,
            firstName = userEntity.firstName,
            lastName = userEntity.lastName,
            photoPath = userEntity.photoPath,
            isPowerUser = userEntity.powerUser,
            numberOfTrees = treeCount
        )
    }
}
