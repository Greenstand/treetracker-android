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

@Entity(tableName = "organization")
class OrganizationEntity(
    @PrimaryKey
    @ColumnInfo(name = "_id")
    var id: String,
    @ColumnInfo(name = "version")
    var version: Int,
    @ColumnInfo(name = "name")
    var name: String,
    @ColumnInfo(name = "wallet_id")
    var walletId: String,
    @ColumnInfo(name = "capture_setup_flow_json")
    val captureSetupFlowJson: String,
    @ColumnInfo(name = "capture_flow_json")
    val captureFlowJson: String,
)