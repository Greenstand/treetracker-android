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
    tableName = "session",
    foreignKeys = [
        ForeignKey(
            entity = DeviceConfigEntity::class,
            parentColumns = ["_id"],
            childColumns = ["device_config_id"],
            onUpdate = ForeignKey.CASCADE
        )
    ]
)
data class SessionEntity(
    @ColumnInfo(name = "uuid")
    var uuid: String,
    @ColumnInfo(name = "origin_user_id")
    var originUserId: String,
    @ColumnInfo(name = "origin_wallet", index = true)
    var originWallet: String,
    @ColumnInfo(name = "destination_wallet")
    var destinationWallet: String,
    @ColumnInfo(name = "start_time")
    var startTime: Instant,
    @ColumnInfo(name = "end_time")
    var endTime: Instant? = null,
    @ColumnInfo(name = "organization")
    var organization: String?,
    @ColumnInfo(name = "uploaded")
    var isUploaded: Boolean,
    @ColumnInfo(name = "bundle_id")
    var bundleId: String? = null,
    @ColumnInfo(name = "device_config_id")
    var deviceConfigId: Long? = null,
    @ColumnInfo(name = "note")
    var note: String? = null,
) {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    var id: Long = 0
}