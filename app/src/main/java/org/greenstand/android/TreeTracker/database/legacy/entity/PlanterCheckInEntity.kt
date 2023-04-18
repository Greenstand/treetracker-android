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
package org.greenstand.android.TreeTracker.database.legacy.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = PlanterCheckInEntity.TABLE,
    foreignKeys = [
        ForeignKey(
            entity = PlanterInfoEntity::class,
            parentColumns = [PlanterInfoEntity.ID],
            childColumns = [PlanterCheckInEntity.PLANTER_INFO_ID],
            onUpdate = ForeignKey.CASCADE
        )
    ]
)
data class PlanterCheckInEntity(
    @ColumnInfo(name = PLANTER_INFO_ID, index = true)
    var planterInfoId: Long,
    @ColumnInfo(name = LOCAL_PHOTO_PATH, index = true)
    var localPhotoPath: String?,
    @ColumnInfo(name = PHOTO_URL)
    var photoUrl: String?,
    @ColumnInfo(name = LATITUDE)
    var latitude: Double,
    @ColumnInfo(name = LONGITUDE)
    var longitude: Double,
    @ColumnInfo(name = CREATED_AT)
    var createdAt: Long
) {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = ID)
    var id: Long = 0

    companion object {
        const val TABLE = "planter_check_in"

        const val ID = "_id"
        const val PLANTER_INFO_ID = "planter_info_id"
        const val LOCAL_PHOTO_PATH = "local_photo_path"
        const val PHOTO_URL = "photo_url"
        const val LATITUDE = "latitude"
        const val LONGITUDE = "longitude"
        const val CREATED_AT = "created_at"
    }
}
