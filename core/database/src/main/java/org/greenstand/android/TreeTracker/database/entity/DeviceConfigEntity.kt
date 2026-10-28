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

@Entity(tableName = "device_config")
data class DeviceConfigEntity(
    @ColumnInfo(name = "uuid")
    val uuid: String,
    @ColumnInfo(name = "app_version")
    val appVersion: String,
    @ColumnInfo(name = "app_build")
    val appBuild: Int,
    @ColumnInfo(name = "os_version")
    val osVersion: String,
    @ColumnInfo(name = "sdk_version")
    val sdkVersion: Int,
    @ColumnInfo(name = "logged_at")
    val loggedAt: Instant,
    @ColumnInfo(name = "uploaded")
    val isUploaded: Boolean = false,
    @ColumnInfo(name = "bundle_id")
    val bundleId: String? = null,
) {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    var id: Long = 0
}