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
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.database.entity.UserEntity
import org.greenstand.android.TreeTracker.models.location.LocationUpdateManager
import org.greenstand.android.TreeTracker.models.messages.database.MessagesDAO
import org.greenstand.android.TreeTracker.models.user.User
import org.greenstand.android.TreeTracker.preferences.PrefKey
import org.greenstand.android.TreeTracker.preferences.PrefKeys
import org.greenstand.android.TreeTracker.preferences.Preferences
import org.greenstand.android.TreeTracker.utilities.TimeProvider
import java.util.*

class UserRepo(
    private val locationUpdateManager: LocationUpdateManager,
    private val dao: TreeTrackerDAO,
    private val analytics: Analytics,
    private val timeProvider: TimeProvider,
    private val messagesDao: MessagesDAO,
    private val exceptionDataCollector: ExceptionDataCollector,
    private val preferences: Preferences,
) {
    /**
     * True after a logout until someone signs in again. Logout never changes the power user: the
     * device keeps its power user for good, and this flag alone sends the app to Signup.
     */
    fun isLoggedOut(): Boolean = preferences.getBoolean(LOGGED_OUT, false)

    fun setLoggedOut(loggedOut: Boolean) {
        preferences.edit().putBoolean(LOGGED_OUT, loggedOut).commit()
    }

    fun users(): Flow<List<User>> =
        dao
            .getAllUsers()
            .map { userEntities -> userEntities.mapNotNull { createUser(it) } }

    suspend fun getUserList(): List<User> = dao.getAllUsersList().mapNotNull { createUser(it) }

    suspend fun getUser(userId: Long): User? = createUser(dao.getUserById(userId))

    suspend fun getUserWithWallet(wallet: String): User? = createUser(dao.getUserByWallet(wallet))

    suspend fun deleteUser(wallet: String): Boolean {
        val deletedRows = dao.deleteUserByWallet(wallet)
        return deletedRows > 0
    }

    suspend fun checkForUnreadMessagesPerUser(wallet: String): Boolean = messagesDao.getUnreadMessageCountForWallet(wallet) >= 1

    suspend fun getPowerUser(): User? {
        val userEntity = dao.getPowerUser() ?: return null
        return createUser(userEntity)
    }

    suspend fun setPowerUserStatus(
        userId: Long,
        isPowerUser: Boolean,
    ) {
        if (!isPowerUser || getPowerUser() == null) {
            dao.updatePowerUserStatus(userId, isPowerUser)
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

            dao.insertUser(entity).also {
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
            dao
                .getSessionsByUserWallet(userEntity.wallet)
                .map {
                    dao.getTreeCountFromSessionId(it.id)
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
        val userEntity = dao.getUserById(user.id) ?: return
        val photoChanged = user.photoPath != userEntity.photoPath
        // Phone and email stay as registered: the upload service rejects a wallet registration
        // whose email has no "@", which writing the wallet into both fields caused.
        val updated =
            userEntity
                .copy(
                    firstName = user.firstName,
                    lastName = user.lastName ?: "",
                    photoPath = user.photoPath,
                    // A new photo needs uploading before the registration is sent again.
                    photoUrl = if (photoChanged) null else userEntity.photoUrl,
                    uploaded = false,
                ).also { it.id = user.id }
        dao.updateUser(updated)
    }

    companion object {
        val LOGGED_OUT = PrefKeys.SYSTEM_SETTINGS + PrefKey("logged-out")
    }
}
