package org.greenstand.android.TreeTracker.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = PlanterDetails.TABLE)
data class PlanterDetails(@PrimaryKey
                          @ColumnInfo(name = ID)
                          var id: Long,
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
                          var uploaded: Boolean?,
                          @ColumnInfo(name = TIME_CREATED)
                          var timeCreated: Long?) {

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