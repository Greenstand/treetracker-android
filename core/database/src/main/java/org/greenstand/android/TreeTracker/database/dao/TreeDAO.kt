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
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.greenstand.android.TreeTracker.database.entity.TreeEntity
import org.greenstand.android.TreeTracker.database.legacy.entity.TreeAttributeEntity
import org.greenstand.android.TreeTracker.database.legacy.entity.TreeCaptureEntity
import org.greenstand.android.TreeTracker.database.legacy.views.TreeMapMarkerDbView

@Dao
interface TreeDAO {
    @Query("SELECT latitude, longitude, _id as treeCaptureId FROM tree_capture")
    suspend fun getTreeDataForMap(): List<TreeMapMarkerDbView>

    @Transaction
    @Query("SELECT COUNT(*) FROM tree_capture WHERE uploaded = 1")
    suspend fun getUploadedTreeCaptureCount(): Int

    @Query("SELECT COUNT(*) FROM tree_capture WHERE photo_url not null")
    suspend fun getUploadedLegacyTreeImageCount(): Int

    @Query("SELECT COUNT(*) FROM tree_capture WHERE photo_url not null")
    fun getUploadedLegacyTreeImageCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM tree WHERE photo_url not null")
    suspend fun getUploadedTreeImageCount(): Int

    @Query("SELECT COUNT(*) FROM tree WHERE photo_url not null")
    fun getUploadedTreeImageCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM tree WHERE uploaded = 1")
    suspend fun getUploadedTreeCount(): Int

    @Transaction
    @Query("SELECT COUNT(*) FROM tree_capture WHERE photo_url is null")
    suspend fun getNonUploadedLegacyTreeCaptureImageCount(): Int

    @Transaction
    @Query("SELECT COUNT(*) FROM tree_capture WHERE photo_url is null")
    fun getNonUploadedLegacyTreeCaptureImageCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM tree WHERE photo_url is null")
    suspend fun getNonUploadedTreeImageCount(): Int

    @Query("SELECT COUNT(*) FROM tree WHERE photo_url is null")
    fun getNonUploadedTreeImageCountFlow(): Flow<Int>

    @Transaction
    @Query("SELECT COUNT(*) FROM tree_capture WHERE uploaded = 0")
    suspend fun getNonUploadedTreeCaptureCount(): Int

    @Transaction
    @Query("SELECT COUNT(*) FROM tree WHERE uploaded = 0")
    suspend fun getNonUploadedTreeCount(): Int

    @Query("SELECT _id FROM tree_capture WHERE uploaded = 0")
    suspend fun getAllTreeCaptureIdsToUpload(): List<Long>

    @Query("SELECT _id FROM tree WHERE uploaded = 0")
    suspend fun getAllTreeIdsToUpload(): List<Long>

    @Query("SELECT COUNT(*) FROM tree WHERE session_id = :sessionId")
    suspend fun getTreeCountFromSessionId(sessionId: Long): Int

    @Query("SELECT * FROM tree_capture")
    suspend fun getAllTreeCaptures(): List<TreeCaptureEntity>

    @Query("SELECT * FROM tree_capture WHERE _id = :id")
    suspend fun getTreeCaptureById(id: Long): TreeCaptureEntity

    @Query("SELECT * FROM tree_capture WHERE _id IN (:ids)")
    suspend fun getTreeCapturesByIds(ids: List<Long>): List<TreeCaptureEntity>

    @Query("SELECT * FROM tree WHERE _id IN (:ids)")
    suspend fun getTreesByIds(ids: List<Long>): List<TreeEntity>

    @Query("SELECT * FROM tree")
    suspend fun getAllTrees(): List<TreeEntity>

    @Query("SELECT t.* FROM tree t INNER JOIN session s ON t.session_id = s._id WHERE s.origin_wallet = :wallet")
    fun getTreesByUserWallet(wallet: String): Flow<List<TreeEntity>>

    @Query("DELETE FROM tree WHERE _id = :treeId")
    suspend fun deleteTreeById(treeId: Long)

    @Query("UPDATE tree_capture SET bundle_id = :bundleId WHERE _id IN (:ids)")
    suspend fun updateTreeCapturesBundleIds(
        ids: List<Long>,
        bundleId: String,
    )

    @Query("UPDATE tree SET bundle_id = :bundleId WHERE _id IN (:ids)")
    suspend fun updateTreesBundleIds(
        ids: List<Long>,
        bundleId: String,
    )

    @Query("UPDATE tree SET uploaded = :uploaded WHERE _id IN (:ids)")
    suspend fun updateTreesUploadStatus(
        ids: List<Long>,
        uploaded: Boolean,
    )

    @Query("UPDATE tree_capture SET uploaded = :isUploaded WHERE _id IN (:ids)")
    suspend fun updateTreeCapturesUploadStatus(
        ids: List<Long>,
        isUploaded: Boolean,
    )

    @Query("UPDATE tree_capture SET local_photo_path = null WHERE _id IN (:ids)")
    suspend fun removeTreeCapturesLocalImagePaths(ids: List<Long>)

    @Query("UPDATE tree SET photo_path = null WHERE _id IN (:ids)")
    suspend fun removeTreesLocalImagePaths(ids: List<Long>)

    @Update
    suspend fun updateTreeCapture(treeCaptureEntity: TreeCaptureEntity)

    @Update
    suspend fun updateTree(treeEntity: TreeEntity)

    @Delete
    suspend fun deleteTreeCapture(treeCaptureEntity: TreeCaptureEntity)

    @Query("SELECT * FROM tree_attribute WHERE tree_capture_id = :treeCaptureId")
    suspend fun getTreeAttributeByTreeCaptureId(treeCaptureId: Long): List<TreeAttributeEntity>

    @Insert
    suspend fun insertTree(treeEntity: TreeEntity): Long

    @Transaction
    suspend fun insertTreeWithAttributes(
        tree: TreeCaptureEntity,
        attributes: List<TreeAttributeEntity>?,
    ): Long {
        val treeId = insertTreeCapture(tree)
        attributes?.forEach {
            it.treeCaptureId = treeId
            insertTreeAttribute(it)
        }
        return treeId
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTreeCapture(treeCaptureEntity: TreeCaptureEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTreeAttribute(treeAttributeEntity: TreeAttributeEntity): Long
}