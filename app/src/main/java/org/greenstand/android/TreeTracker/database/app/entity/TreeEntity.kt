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
package org.greenstand.android.TreeTracker.database.app.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant

@Entity(
    tableName = "tree",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["_id"],
            childColumns = ["session_id"],
            onUpdate = ForeignKey.CASCADE
        )
    ]
)
data class TreeEntity(
    @ColumnInfo(name = "uuid")
    var uuid: String,
    @ColumnInfo(name = "session_id", index = true)
    var sessionId: Long,
    @ColumnInfo(name = "photo_path")
    var photoPath: String?,
    @ColumnInfo(name = "photo_url")
    var photoUrl: String?,
    @ColumnInfo(name = "note")
    var note: String,
    @ColumnInfo(name = "latitude")
    var latitude: Double,
    @ColumnInfo(name = "longitude")
    var longitude: Double,
    @ColumnInfo(name = "uploaded", index = true)
    var uploaded: Boolean = false,
    @ColumnInfo(name = "created_at")
    var createdAt: Instant,
    @ColumnInfo(name = "bundle_id", defaultValue = "NULL")
    var bundleId: String? = null,
    @ColumnInfo(name = "extra_attributes", defaultValue = "NULL")
    var extraAttributes: Map<String, String>? = null,
) {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    var id: Long = 0
}