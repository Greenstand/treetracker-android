/*
 * Copyright 2023 Treetracker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.greenstand.android.TreeTracker.models

import android.os.Build
import org.greenstand.android.TreeTracker.BuildConfig
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.database.entity.DeviceConfigEntity
import org.greenstand.android.TreeTracker.utilities.TimeProvider
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
            config.sdkVersion != Build.VERSION.SDK_INT
        ) {
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