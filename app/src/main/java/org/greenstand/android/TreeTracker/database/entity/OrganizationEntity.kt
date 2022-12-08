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