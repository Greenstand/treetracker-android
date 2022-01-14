package org.greenstand.android.TreeTracker.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

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
    var createdAt: Long,
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