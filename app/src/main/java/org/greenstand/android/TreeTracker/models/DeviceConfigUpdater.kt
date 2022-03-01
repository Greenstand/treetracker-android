package org.greenstand.android.TreeTracker.models

import android.os.Build
import org.greenstand.android.TreeTracker.BuildConfig
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.database.entity.DeviceConfigEntity
import org.greenstand.android.TreeTracker.models.location.LocationUpdateManager
import org.greenstand.android.TreeTracker.utilities.TimeProvider
import timber.log.Timber
import java.util.*

class DeviceConfigUpdater(
    private val dao: TreeTrackerDAO,
    private val timeProvider: TimeProvider,
) {

    suspend fun saveLatestConfig() {
        val config = dao.getLatestDeviceConfig() ?: saveNewDeviceConfig()
        if (config.appVersion != BuildConfig.VERSION_NAME ||
            config.appBuild != BuildConfig.VERSION_CODE ||
            config.osVersion != Build.VERSION.RELEASE ||
            config.sdkVersion != Build.VERSION.SDK_INT) {
            saveNewDeviceConfig()
        }
    }

    private suspend fun saveNewDeviceConfig(): DeviceConfigEntity {
        val deviceConfigEntity = DeviceConfigEntity(
            uuid = UUID.randomUUID().toString(),
            appVersion = BuildConfig.VERSION_NAME,
            appBuild = BuildConfig.VERSION_CODE,
            osVersion = Build.VERSION.RELEASE,
            sdkVersion = Build.VERSION.SDK_INT,
            loggedAt = timeProvider.currentTime(),
        )

        dao.insertDeviceConfig(deviceConfigEntity)

        return deviceConfigEntity
    }
}