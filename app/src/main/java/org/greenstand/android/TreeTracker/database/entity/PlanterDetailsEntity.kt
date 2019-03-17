package org.greenstand.android.TreeTracker.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = PlanterDetailsEntity.TABLE)
data class PlanterDetailsEntity(
    @ColumnInfo(name = INDENTIFIER)
    var indentifier: String?,
    @ColumnInfo(name = FIRST_NAME)
    var firstName: String?,
    @ColumnInfo(name = LAST_NAME)
    var lastName: String?,
    @ColumnInfo(name = ORGANIZATION)
    var organization: String?,
    @ColumnInfo(name = PHONE)
    var phone: String?,
    @ColumnInfo(name = EMAIL)
    var email: String?,
    @ColumnInfo(name = UPLOADED)
    var uploaded: Int = 0,
    @ColumnInfo(name = TIME_CREATED)
    var timeCreated: String
) {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = ID)
    var id: Int = 0

    companion object {
        const val TABLE = "planter_details"
        const val ID = "_id"
        const val INDENTIFIER = "identifier"
        const val FIRST_NAME = "first_name"
        const val LAST_NAME = "last_name"
        const val ORGANIZATION = "organization"
        const val PHONE = "phone"
        const val EMAIL = "email"
        const val UPLOADED = "uploaded"
        const val TIME_CREATED = "time_created"
    }
}