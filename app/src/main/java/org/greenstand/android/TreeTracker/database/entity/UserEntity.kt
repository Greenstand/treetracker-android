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
package org.greenstand.android.TreeTracker.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant

@Entity(tableName = "user")
data class UserEntity(
    @ColumnInfo(name = "uuid")
    var uuid: String,
    @ColumnInfo(name = "wallet", index = true)
    var wallet: String,
    @ColumnInfo(name = "first_name")
    var firstName: String,
    @ColumnInfo(name = "last_name")
    var lastName: String,
    @ColumnInfo(name = "phone")
    var phone: String?,
    @ColumnInfo(name = "email")
    var email: String?,
    @ColumnInfo(name = "latitude")
    var latitude: Double,
    @ColumnInfo(name = "longitude")
    var longitude: Double,
    @ColumnInfo(name = "uploaded", index = true)
    var uploaded: Boolean = false,
    @ColumnInfo(name = "created_at")
    var createdAt: Instant,
    @ColumnInfo(name = "bundle_id")
    var bundleId: String? = null,
    @ColumnInfo(name = "photo_path")
    var photoPath: String,
    @ColumnInfo(name = "photo_url", defaultValue = "NULL")
    var photoUrl: String?,
    @ColumnInfo(name =
    "power_user", defaultValue = "0")
    var powerUser: Boolean,
) {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    var id: Long = 0
}