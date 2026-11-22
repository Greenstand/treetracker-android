/*
 * Copyright 2023 Treetracker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.greenstand.android.TreeTracker.models

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.greenstand.android.TreeTracker.analytics.Analytics
import org.greenstand.android.TreeTracker.analytics.ExceptionDataCollector
import org.greenstand.android.TreeTracker.database.dao.SessionDAO
import org.greenstand.android.TreeTracker.database.dao.TreeDAO
import org.greenstand.android.TreeTracker.database.dao.UserDAO
import org.greenstand.android.TreeTracker.database.entity.UserEntity
import org.greenstand.android.TreeTracker.models.location.LocationUpdateManager
import org.greenstand.android.TreeTracker.models.messages.database.MessagesDAO
import org.greenstand.android.TreeTracker.models.user.User
import org.greenstand.android.TreeTracker.utilities.TimeProvider
import java.util.UUID

class UserRepo(
    private val locationUpdateManager: LocationUpdateManager,
    private val userDao: UserDAO,
    private val sessionDao: SessionDAO,
    private val treeDao: TreeDAO,
    private val analytics: Analytics,
    private val timeProvider: TimeProvider,
    private val messagesDao: MessagesDAO,
    private val exceptionDataCollector: ExceptionDataCollector,
) {
    fun users(): Flow<List<User>> = userDao.getAllUsers().map { userEntities -> userEntities.mapNotNull { createUser(it) } }

    suspend fun getUserList(): List<User> = userDao.getAllUsersList().mapNotNull { createUser(it) }

    suspend fun getUser(userId: Long): User? = createUser(userDao.getUserById(userId))

    suspend fun getUserWithWallet(wallet: String): User? = createUser(userDao.getUserByWallet(wallet))

    suspend fun deleteUser(wallet: String): Boolean {
        val deletedRows = userDao.deleteUserByWallet(wallet)
        return deletedRows > 0
    }

    suspend fun checkForUnreadMessagesPerUser(wallet: String): Boolean = messagesDao.getUnreadMessageCountForWallet(wallet) >= 1

    suspend fun getPowerUser(): User? {
        val userEntity = userDao.getPowerUser() ?: return null
        return createUser(userEntity)
    }

    suspend fun setPowerUserStatus(
        userId: Long,
        isPowerUser: Boolean,
    ) {
        if (!isPowerUser || getPowerUser() == null) {
            userDao.updatePowerUserStatus(userId, isPowerUser)
        }
    }

    suspend fun createUser(
        firstName: String,
        lastName: String,
        phone: String?,
        email: String?,
        wallet: String,
        photoPath: String,
        isPowerUser: Boolean = false,
    ): Long =
        withContext(Dispatchers.IO) {
            val location = locationUpdateManager.currentLocation
            val time = timeProvider.currentTime()

            val entity =
                UserEntity(
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

            if (isPowerUser) {
                exceptionDataCollector.set(ExceptionDataCollector.POWER_USER_WALLET, wallet)
            }

            userDao.insertUser(entity).also {
                analytics.userInfoCreated(
                    phone = phone.orEmpty(),
                    email = email.orEmpty(),
                )
            }
        }

    suspend fun doesUserExists(identifier: String): Boolean = getUserWithWallet(identifier) != null

    private suspend fun createUser(userEntity: UserEntity?): User? {
        userEntity ?: return null

        val treeCount =
            sessionDao
                .getSessionsByUserWallet(userEntity.wallet)
                .map {
                    treeDao.getTreeCountFromSessionId(it.id)
                }.sum()

        return User(
            id = userEntity.id,
            wallet = userEntity.wallet,
            firstName = userEntity.firstName,
            lastName = userEntity.lastName,
            photoPath = userEntity.photoPath,
            isPowerUser = userEntity.powerUser,
            numberOfTrees = treeCount,
            unreadMessagesAvailable = checkForUnreadMessagesPerUser(userEntity.wallet),
        )
    }

    suspend fun updateUser(user: User) {
        val userEntity = userDao.getUserById(user.id) ?: return
        val updated =
            userEntity
                .copy(
                    firstName = user.firstName,
                    lastName = user.lastName ?: "",
                    phone = user.wallet,
                    email = user.wallet,
                    photoPath = user.photoPath,
                    uploaded = false,
                ).also { it.id = user.id }
        userDao.updateUser(updated)
    }
}