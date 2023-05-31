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
import androidx.room.PrimaryKey

@Entity(
    tableName = LocationDataEntity.TABLE
)
data class LocationDataEntity(
    @ColumnInfo(name = JSON_VALUE)
    var locationDataJson: String
) {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = ID)
    var id: Long = 0
    @ColumnInfo(name = UPLOADED, index = true)
    var uploaded: Boolean = false
    @ColumnInfo(name = CREATED_AT)
    var createdAt: Long = System.currentTimeMillis()

    companion object {
        const val TABLE = "location_data"

        const val ID = "_id"
        // Json string of LocationData defined in LocationUpdateManager.kt
        const val JSON_VALUE = "json_value"
        const val UPLOADED = "uploaded"
        const val CREATED_AT = "created_at"
    }
}