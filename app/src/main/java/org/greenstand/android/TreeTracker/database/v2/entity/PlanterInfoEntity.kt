package org.greenstand.android.TreeTracker.database.v2.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

inline class PlanterInfoId(val value: Long)

@Entity(tableName = PlanterInfoEntity.TABLE)
data class PlanterInfoEntity(
    @ColumnInfo(name = IDENTIFIER)
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
    @ColumnInfo(name = UPLOADED)
    var uploaded: Boolean = false,
    @ColumnInfo(name = CREATED_AT)
    var createdAt: Long
) {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = ID)
    var id: Long = 0

    companion object {
        const val TABLE = "planter_info"

        const val ID = "_id"
        const val IDENTIFIER = "identifier"
        const val FIRST_NAME = "first_name"
        const val LAST_NAME = "last_name"
        const val ORGANIZATION = "organization"
        const val PHONE = "phone"
        const val EMAIL = "email"
        const val LATITUDE = "latitude"
        const val LONGITUDE = "longitude"
        const val UPLOADED = "uploaded"
        const val CREATED_AT = "created_at"
    }
}