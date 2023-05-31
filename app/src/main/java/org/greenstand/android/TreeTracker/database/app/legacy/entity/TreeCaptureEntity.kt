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
package org.greenstand.android.TreeTracker.database.app.legacy.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = TreeCaptureEntity.TABLE,
    foreignKeys = [
        ForeignKey(
            entity = PlanterCheckInEntity::class,
            parentColumns = [PlanterCheckInEntity.ID],
            childColumns = [TreeCaptureEntity.PLANTER_CHECK_IN_ID],
            onUpdate = ForeignKey.CASCADE
        )
    ]
)
data class TreeCaptureEntity(
    @ColumnInfo(name = UUID)
    var uuid: String,
    @ColumnInfo(name = PLANTER_CHECK_IN_ID, index = true)
    var planterCheckInId: Long,
    @ColumnInfo(name = LOCAL_PHOTO_PATH)
    var localPhotoPath: String?,
    @ColumnInfo(name = PHOTO_URL)
    var photoUrl: String?,
    @ColumnInfo(name = NOTE_CONTENT)
    var noteContent: String,
    @ColumnInfo(name = LATITUDE)
    var latitude: Double,
    @ColumnInfo(name = LONGITUDE)
    var longitude: Double,
    @ColumnInfo(name = ACCURACY)
    var accuracy: Double,
    @ColumnInfo(name = UPLOADED, index = true)
    var uploaded: Boolean = false,
    @ColumnInfo(name = CREATED_AT)
    var createAt: Long,
    @ColumnInfo(name = BUNDLE_ID)
    var bundleId: String? = null
) {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = ID)
    var id: Long = 0

    companion object {
        const val TABLE = "tree_capture"

        const val ID = "_id"
        const val UUID = "uuid"
        const val PLANTER_CHECK_IN_ID = "planter_checkin_id"
        const val LOCAL_PHOTO_PATH = "local_photo_path"
        const val PHOTO_URL = "photo_url"
        const val NOTE_CONTENT = "note_content"
        const val LATITUDE = "latitude"
        const val LONGITUDE = "longitude"
        const val ACCURACY = "accuracy"
        const val UPLOADED = "uploaded"
        const val CREATED_AT = "created_at"
        const val BUNDLE_ID = "bundle_id"
    }
}