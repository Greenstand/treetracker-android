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

@Entity(tableName = PlanterInfoEntity.TABLE)
data class PlanterInfoEntity(
    @ColumnInfo(name = IDENTIFIER, index = true)
    var identifier: String,
    @ColumnInfo(name = FIRST_NAME)
    var firstName: String,
    @ColumnInfo(name = LAST_NAME)
    var lastName: String,
    @ColumnInfo(name = ORGANIZATION)
    var organization: String?,
    @ColumnInfo(name = PHONE)
    var phone: String?,
    @ColumnInfo(name = EMAIL)
    var email: String?,
    @ColumnInfo(name = LATITUDE)
    var latitude: Double,
    @ColumnInfo(name = LONGITUDE)
    var longitude: Double,
    @ColumnInfo(name = UPLOADED, index = true)
    var uploaded: Boolean = false,
    @ColumnInfo(name = CREATED_AT)
    var createdAt: Long,
    @ColumnInfo(name = BUNDLE_ID)
    var bundleId: String? = null,
    @ColumnInfo(name = RECORD_UUID, defaultValue = "")
    var recordUuid: String,
) {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = ID)
    var id: Long = 0

    companion object {
        const val TABLE = "planter_info"

        const val ID = "_id"
        const val IDENTIFIER = "planter_identifier"
        const val FIRST_NAME = "first_name"
        const val LAST_NAME = "last_name"
        const val ORGANIZATION = "organization"
        const val PHONE = "phone"
        const val EMAIL = "email"
        const val LATITUDE = "latitude"
        const val LONGITUDE = "longitude"
        const val UPLOADED = "uploaded"
        const val CREATED_AT = "created_at"
        const val BUNDLE_ID = "bundle_id"
        const val RECORD_UUID = "record_uuid"
    }
}