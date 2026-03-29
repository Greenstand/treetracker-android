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
package org.greenstand.android.TreeTracker.api.models.requests

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.greenstand.android.TreeTracker.models.messages.network.requests.MessageRequest
import org.greenstand.android.TreeTracker.utilities.DeviceUtils

@Serializable
class UploadBundle
    constructor(
        @SerialName("pack_format_version")
        val version: String,
        @SerialName("device_id")
        val deviceId: String = DeviceUtils.deviceId,
        // V1
        @SerialName("trees")
        val trees: List<NewTreeRequest>? = null,
        @SerialName("registrations")
        val registrations: List<RegistrationRequest>? = null,
        @SerialName("devices")
        val devices: List<DeviceRequest>? = null,
        // V2
        @SerialName("wallet_registrations")
        val walletRegistrations: List<WalletRegistrationRequest>? = null,
        @SerialName("captures")
        val treeCaptures: List<TreeCaptureRequest>? = null,
        @SerialName("device_configurations")
        val deviceConfig: List<DeviceConfigRequest>? = null,
        @SerialName("sessions")
        val sessions: List<SessionRequest>? = null,
        @SerialName("tracks")
        val tracks: List<TracksRequest>? = null,
        @SerialName("messages")
        val messages: List<MessageRequest>? = null,
    ) {
        companion object {
            fun createV1(
                newTreeRequests: List<NewTreeRequest>? = null,
                registrations: List<RegistrationRequest>? = null,
                instanceId: String,
            ): UploadBundle =
                UploadBundle(
                    version = "1",
                    trees = newTreeRequests,
                    registrations = registrations,
                    devices = listOf(DeviceRequest(instanceId = instanceId)),
                )

            fun createV2(
                walletRegistration: List<WalletRegistrationRequest>? = null,
                treeCaptures: List<TreeCaptureRequest>? = null,
                sessions: List<SessionRequest>? = null,
                tracks: List<TracksRequest>? = null,
                deviceConfigs: List<DeviceConfigRequest>? = null,
                messages: List<MessageRequest>? = null,
            ): UploadBundle =
                UploadBundle(
                    version = "2",
                    walletRegistrations = walletRegistration,
                    treeCaptures = treeCaptures,
                    sessions = sessions,
                    tracks = tracks,
                    deviceConfig = deviceConfigs,
                    messages = messages,
                )
        }
    }