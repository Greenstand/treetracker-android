package org.greenstand.android.TreeTracker.api.models.requests

data class UploadBundle(val trees: List<NewTreeRequest>? = null,
                        val registrations: List<RegistrationRequest>? = null,
                        val devices: List<DeviceRequest> = listOf(DeviceRequest()))