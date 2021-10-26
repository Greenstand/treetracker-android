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
    @ColumnInfo(name = PHOTO_URL, defaultValue = "")
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

// TableInfo{name='planter_info', columns={local_photo_path=Column{name='local_photo_path', type='TEXT', affinity='2', notNull=false, primaryKeyPosition=0, defaultValue=''''}, latitude=Column{name='latitude', type='REAL', affinity='4', notNull=true, primaryKeyPosition=0, defaultValue='null'}, last_name=Column{name='last_name', type='TEXT', affinity='2', notNull=true, primaryKeyPosition=0, defaultValue='null'}, created_at=Column{name='created_at', type='INTEGER', affinity='3', notNull=true, primaryKeyPosition=0, defaultValue='null'}, planter_identifier=Column{name='planter_identifier', type='TEXT', affinity='2', notNull=true, primaryKeyPosition=0, defaultValue='null'}, power_user=Column{name='power_user', type='INTEGER', affinity='3', notNull=true, primaryKeyPosition=0, defaultValue='0'}, record_uuid=Column{name='record_uuid', type='TEXT', affinity='2', notNull=true, primaryKeyPosition=0, defaultValue=''''}, phone=Column{name='phone', type='TEXT', affinity='2', notNull=false, primaryKeyPosition=0, defaultValue='null'}, organization=Column{name='organization', type='TEXT', affinity='2', notNull=false, primaryKeyPosition=0, defaultValue='null'}, bundle_id=Column{name='bundle_id', type='TEXT', affinity='2', notNull=false, primaryKeyPosition=0, defaultValue='null'}, uploaded=Column{name='uploaded', type='INTEGER', affinity='3', notNull=true, primaryKeyPosition=0, defaultValue='null'}, photo_url=Column{name='photo_url', type='TEXT', affinity='2', notNull=true, primaryKeyPosition=0, defaultValue=''''}, _id=Column{name='_id', type='INTEGER', affinity='3', notNull=true, primaryKeyPosition=1, defaultValue='null'}, first_name=Column{name='first_name', type='TEXT', affinity='2', notNull=true, primaryKeyPosition=0, defaultValue='null'}, email=Column{name='email', type='TEXT', affinity='2', notNull=false, primaryKeyPosition=0, defaultValue='null'}, longitude=Column{name='longitude', type='REAL', affinity='4', notNull=true, primaryKeyPosition=0, defaultValue='null'}}, foreignKeys=[], indices=[Index{name='index_planter_info_planter_identifier', unique=false, columns=[planter_identifier]}, Index{name='index_planter_info_uploaded', unique=false, columns=[uploaded]}, Index{name='index_planter_info_local_photo_path', unique=false, columns=[local_photo_path]}]}
// TableInfo{name='planter_info', columns={local_photo_path=Column{name='local_photo_path', type='TEXT', affinity='2', notNull=true, primaryKeyPosition=0, defaultValue='""'}, latitude=Column{name='latitude', type='REAL', affinity='4', notNull=true, primaryKeyPosition=0, defaultValue='null'}, last_name=Column{name='last_name', type='TEXT', affinity='2', notNull=true, primaryKeyPosition=0, defaultValue='null'}, created_at=Column{name='created_at', type='INTEGER', affinity='3', notNull=true, primaryKeyPosition=0, defaultValue='null'}, planter_identifier=Column{name='planter_identifier', type='TEXT', affinity='2', notNull=true, primaryKeyPosition=0, defaultValue='null'}, power_user=Column{name='power_user', type='INTEGER', affinity='3', notNull=true, primaryKeyPosition=0, defaultValue='0'}, record_uuid=Column{name='record_uuid', type='TEXT', affinity='2', notNull=true, primaryKeyPosition=0, defaultValue=''''}, phone=Column{name='phone', type='TEXT', affinity='2', notNull=false, primaryKeyPosition=0, defaultValue='null'}, organization=Column{name='organization', type='TEXT', affinity='2', notNull=false, primaryKeyPosition=0, defaultValue='null'}, bundle_id=Column{name='bundle_id', type='TEXT', affinity='2', notNull=false, primaryKeyPosition=0, defaultValue='null'}, uploaded=Column{name='uploaded', type='INTEGER', affinity='3', notNull=true, primaryKeyPosition=0, defaultValue='null'}, _id=Column{name='_id', type='INTEGER', affinity='3', notNull=true, primaryKeyPosition=1, defaultValue='null'}, photo_url=Column{name='photo_url', type='TEXT', affinity='2', notNull=true, primaryKeyPosition=0, defaultValue='""'}, first_name=Column{name='first_name', type='TEXT', affinity='2', notNull=true, primaryKeyPosition=0, defaultValue='null'}, email=Column{name='email', type='TEXT', affinity='2', notNull=false, primaryKeyPosition=0, defaultValue='null'}, longitude=Column{name='longitude', type='REAL', affinity='4', notNull=true, primaryKeyPosition=0, defaultValue='null'}}, foreignKeys=[], indices=[Index{name='index_planter_info_uploaded', unique=false, columns=[uploaded]}, Index{name='index_planter_info_planter_identifier', unique=false, columns=[planter_identifier]}]}
