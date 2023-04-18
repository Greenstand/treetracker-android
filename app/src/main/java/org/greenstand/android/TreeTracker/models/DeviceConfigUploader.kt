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

import com.google.gson.Gson
import org.greenstand.android.TreeTracker.api.ObjectStorageClient
import org.greenstand.android.TreeTracker.api.models.requests.DeviceConfigRequest
import org.greenstand.android.TreeTracker.api.models.requests.UploadBundle
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.utilities.md5

class DeviceConfigUploader(
    private val dao: TreeTrackerDAO,
    private val objectStorageClient: ObjectStorageClient,
    private val gson: Gson,
) {

    suspend fun upload() {
        val deviceConfigsToUpload = dao.getDeviceConfigsToUpload()

        if(deviceConfigsToUpload.isEmpty()) {
            return
        }

        val deviceConfigRequests = deviceConfigsToUpload.map { config ->
            DeviceConfigRequest(
                id = config.uuid,
                appVersion = config.appVersion,
                appBuild = config.appBuild,
                osVersion = config.osVersion,
                sdkVersion = config.sdkVersion,
                loggedAt = config.loggedAt.toString(),
            )
        }

        val jsonBundle = gson.toJson(
            UploadBundle.createV2(
                deviceConfigs = deviceConfigRequests
            )
        )
        val bundleId = jsonBundle.md5() + "_deviceConfigs"
        val deviceConfigIds = deviceConfigsToUpload.map { it.id }

        dao.updateDeviceConfigBundleIds(deviceConfigIds, bundleId)
        objectStorageClient.uploadBundle(jsonBundle, bundleId)
        dao.updateDeviceConfigUploadStatus(deviceConfigIds, true)
    }
}