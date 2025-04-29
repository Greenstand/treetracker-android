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

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.util.Log
import com.hbb20.CCPCountry
import com.hbb20.CountryCodePicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.greenstand.android.TreeTracker.analytics.Analytics
import org.greenstand.android.TreeTracker.analytics.ExceptionDataCollector
import org.greenstand.android.TreeTracker.application.TreeTrackerApplication
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.database.entity.UserEntity
import org.greenstand.android.TreeTracker.models.countries.CountryPickerData
import org.greenstand.android.TreeTracker.models.location.LocationUpdateManager
import org.greenstand.android.TreeTracker.models.messages.database.MessagesDAO
import org.greenstand.android.TreeTracker.models.user.User
import org.greenstand.android.TreeTracker.utilities.TimeProvider
import org.greenstand.android.TreeTracker.view.getFlagMasterResID
import java.util.Locale
import java.util.UUID


class UserRepo(
    private val locationUpdateManager: LocationUpdateManager,
    private val dao: TreeTrackerDAO,
    private val analytics: Analytics,
    private val timeProvider: TimeProvider,
    private val messagesDao: MessagesDAO,
    private val exceptionDataCollector: ExceptionDataCollector,
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

    suspend fun checkForUnreadMessagesPerUser(wallet: String): Boolean {
        return messagesDao.getUnreadMessageCountForWallet(wallet) >= 1
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
            val time = timeProvider.currentTime()

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

            if (isPowerUser) {
                exceptionDataCollector.set(ExceptionDataCollector.POWER_USER_WALLET, wallet)
            }

            dao.insertUser(entity).also {
                analytics.userInfoCreated(
                    phone = phone.orEmpty(),
                    email = email.orEmpty()
                )
            }
        }
    }

    suspend fun doesUserExists(identifier: String): Boolean {
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
            numberOfTrees = treeCount,
            unreadMessagesAvailable = checkForUnreadMessagesPerUser(userEntity.wallet),
        )
    }

    suspend fun updateUser(user: User) {
        val userEntity = dao.getUserById(user.id) ?: return
        val updated = userEntity.copy(
            firstName = user.firstName,
            lastName = user.lastName ?: "",
            phone = user.wallet,
            email = user.wallet,
            photoPath = user.photoPath,
            uploaded = false
        ).also { it.id = user.id }
        dao.updateUser(updated)
    }


    suspend fun getCountryList(): List<CountryPickerData> = withContext(Dispatchers.Default) {
        CCPCountry.getLibraryMasterCountryList(TreeTrackerApplication.appContext, CountryCodePicker.Language.ENGLISH).map { Triple(it.phoneCode, it.nameCode, getFlagMasterResID(it)) }
            .map { (first, second, third) -> CountryPickerData(first, second, third) }
    }

    fun getUserCountryName(): String {
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val locales: LocaleList = TreeTrackerApplication.appContext.getResources().getConfiguration().getLocales()
            if (!locales.isEmpty()) {
                return locales.get(0).getCountry()
            }
            return ""
        } else {
            return TreeTrackerApplication.appContext.resources.configuration.locale.getCountry()
        }
        return ""*/

        return Locale.getDefault().country
    }

    fun getCountryFromLatLng(context: Context, latitude: Double, longitude: Double): String? {
        Log.v("gaurav", "Trying to access location from locationUpdateManager")
        val geocoder = Geocoder(context, Locale.getDefault())

        // Attempt to get the address list
        try {
            val addresses: List<Address>? = geocoder.getFromLocation(latitude, longitude, 1)

            if (addresses?.isNotEmpty() == true) {
                val address = addresses[0]
                return address.countryName // Get the country name from the Address object
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null // If geocoding fails, return null
    }

}