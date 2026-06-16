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
package org.greenstand.android.TreeTracker.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.greenstand.android.TreeTracker.database.entity.UserEntity

@Dao
interface UserDAO {
    @Query("SELECT * FROM user")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM user")
    suspend fun getAllUsersList(): List<UserEntity>

    @Query("SELECT * FROM user where uploaded = 0")
    suspend fun getAllUsersToUpload(): List<UserEntity>

    @Query("SELECT * FROM user WHERE _id = :id")
    suspend fun getUserById(id: Long): UserEntity?

    @Query("SELECT * FROM user WHERE wallet = :wallet")
    suspend fun getUserByWallet(wallet: String): UserEntity?

    @Query("SELECT * FROM user WHERE power_user = 1")
    suspend fun getPowerUser(): UserEntity?

    @Query("DELETE FROM user WHERE wallet = :wallet")
    suspend fun deleteUserByWallet(wallet: String): Int

    @Query("UPDATE user SET power_user = :isPowerUser WHERE _id = :userId")
    suspend fun updatePowerUserStatus(
        userId: Long,
        isPowerUser: Boolean,
    )

    @Update
    suspend fun updateUser(userEntity: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(userEntity: UserEntity): Long

    @Query("UPDATE user SET bundle_id = :bundleId WHERE _id IN (:ids)")
    suspend fun updateUserBundleIds(
        ids: List<Long>,
        bundleId: String,
    )

    @Query("UPDATE user SET uploaded = :isUploaded WHERE _id IN (:ids)")
    suspend fun updateUserUploadStatus(
        ids: List<Long>,
        isUploaded: Boolean,
    )
}
