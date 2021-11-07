package org.greenstand.android.TreeTracker.database.entity

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
    @ColumnInfo(name = POWER_USER, defaultValue = "0")
    var isPowerUser: Boolean,
    @ColumnInfo(name = LOCAL_PHOTO_PATH, index = true, defaultValue = "")
    var localPhotoPath: String,
    @ColumnInfo(name = PHOTO_URL, defaultValue = "NULL")
    var photoUrl: String? = null,
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
        const val POWER_USER = "power_user"
        const val LOCAL_PHOTO_PATH = "local_photo_path"
        const val PHOTO_URL = "photo_url"
    }
}