package org.greenstand.android.TreeTracker.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant

@Entity(tableName = "device_config")
data class DeviceConfigEntity(
    @ColumnInfo(name = "uuid")
    val uuid: String,
    @ColumnInfo(name = "app_version")
    val appVersion: String,
    @ColumnInfo(name = "app_build")
    val appBuild: Int,
    @ColumnInfo(name = "os_version")
    val osVersion: String,
    @ColumnInfo(name = "sdk_version")
    val sdkVersion: Int,
    @ColumnInfo(name = "logged_at")
    val loggedAt: Instant,
    @ColumnInfo(name = "uploaded")
    val isUploaded: Boolean = false,
    @ColumnInfo(name = "bundle_id")
    val bundleId: String? = null,
) {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    var id: Long = 0
}