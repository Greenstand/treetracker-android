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
import org.greenstand.android.TreeTracker.database.entity.SessionEntity

@Dao
interface SessionDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(sessionEntity: SessionEntity): Long

    @Update
    suspend fun updateSession(sessionEntity: SessionEntity)

    @Query("SELECT * FROM session WHERE _id = :id")
    suspend fun getSessionById(id: Long): SessionEntity

    @Query("SELECT * FROM session WHERE origin_wallet = :wallet")
    suspend fun getSessionsByUserWallet(wallet: String): List<SessionEntity>

    @Query("SELECT * FROM session WHERE uploaded = 0")
    suspend fun getSessionsToUpload(): List<SessionEntity>

    @Query("UPDATE session SET bundle_id = :bundleId WHERE _id IN (:ids)")
    suspend fun updateSessionBundleIds(
        ids: List<Long>,
        bundleId: String,
    )

    @Query("UPDATE session SET uploaded = :isUploaded WHERE _id IN (:ids)")
    suspend fun updateSessionUploadStatus(
        ids: List<Long>,
        isUploaded: Boolean,
    )
}
