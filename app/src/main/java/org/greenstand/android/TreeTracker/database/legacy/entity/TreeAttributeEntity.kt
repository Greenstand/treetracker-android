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
    tableName = TreeAttributeEntity.TABLE,
    foreignKeys = [
        ForeignKey(
            entity = TreeCaptureEntity::class,
            parentColumns = [TreeCaptureEntity.ID],
            childColumns = [TreeAttributeEntity.TREE_CAPTURE_ID],
            onUpdate = ForeignKey.CASCADE
        )
    ]
)
data class TreeAttributeEntity(
    @ColumnInfo(name = KEY)
    var key: String,
    @ColumnInfo(name = VALUE)
    var value: String,
    @ColumnInfo(name = TREE_CAPTURE_ID, index = true)
    var treeCaptureId: Long
) {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = ID)
    var id: Long = 0

    companion object {
        const val TABLE = "tree_attribute"
        const val ID = "_id"
        const val KEY = "key"
        const val VALUE = "value"
        const val TREE_CAPTURE_ID = "tree_capture_id"
    }
}