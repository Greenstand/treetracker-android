package org.greenstand.android.TreeTracker.api.models.requests

import com.google.gson.annotations.SerializedName
import org.greenstand.android.TreeTracker.models.messages.network.requests.MessageRequest
import org.greenstand.android.TreeTracker.utilities.DeviceUtils

class UploadBundle
    constructor(
        @SerializedName("pack_format_version")
        val version: String,
      
        @SerializedName("device_id")
        val deviceId: String = DeviceUtils.deviceId,

        // V1
        @SerializedName("trees")
        val trees: List<NewTreeRequest>? = null,
        @SerializedName("registrations")
        val registrations: List<RegistrationRequest>? = null,
        @SerializedName("devices")
        val devices: List<DeviceRequest>? = null,

        // V2
        @SerializedName("wallet_registrations")
        val walletRegistrations: List<WalletRegistrationRequest>? = null,
        @SerializedName("captures")
        val treeCaptures: List<TreeCaptureRequest>? = null,
        @SerializedName("device_configurations")
        val deviceConfig: List<DeviceConfigRequest>? = null,
        @SerializedName("sessions")
        val sessions: List<SessionRequest>? = null,
        @SerializedName("tracks")
        val tracks: List<TracksRequest>? = null,
        @SerializedName("messages")
        val messages: List<MessageRequest>? = null,
) {

    companion object {

        fun createV1(
            newTreeRequests: List<NewTreeRequest>? = null,
            registrations: List<RegistrationRequest>? = null): UploadBundle {
            return UploadBundle(
                version = "1",
                trees = newTreeRequests,
                registrations = registrations,
                devices = listOf(DeviceRequest()),
            )
        }

        fun createV2(
            walletRegistration: List<WalletRegistrationRequest>? = null,
            treeCaptures: List<TreeCaptureRequest>? = null,
            sessions: List<SessionRequest>? = null,
            tracks: List<TracksRequest>? = null,
            deviceConfigs: List<DeviceConfigRequest>? = null,
            messages: List<MessageRequest>? = null,
        ): UploadBundle {
            return UploadBundle(
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
}