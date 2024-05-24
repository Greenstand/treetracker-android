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

@Entity(
    tableName = "location",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["_id"],
            childColumns = ["session_id"],
            onUpdate = ForeignKey.CASCADE
        )
    ]
)
data class LocationEntity(
    @ColumnInfo(name = "json_value")
    var locationDataJson: String,
    @ColumnInfo(name = "session_id", index = true)
    var sessionId: Long,
) {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    var id: Long = 0
    @ColumnInfo(name = "uploaded", index = true)
    var uploaded: Boolean = false
    @ColumnInfo(name = "create_at")
    var createdAt: Long = System.currentTimeMillis()
}