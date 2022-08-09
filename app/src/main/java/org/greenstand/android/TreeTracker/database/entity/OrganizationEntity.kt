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
    @ColumnInfo(name = "is_token_transfer_choice_enabled")
    val isTokenTransferChoiceEnabled: Boolean,
    @ColumnInfo(name = "is_session_note_enabled")
    val isSessionNoteEnabled: Boolean,
)