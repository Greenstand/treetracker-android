package org.greenstand.android.TreeTracker.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "organization")
class OrganizationEntity(
    @ColumnInfo(name = "name")
    var name: String,
    @ColumnInfo(name = "photo_path", defaultValue = "NULL")
    var photoPath: String?,
    @ColumnInfo(name = "photo_url")
    var photoUrl: String,
) {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    var id: Long = 0
}