package org.greenstand.android.TreeTracker.api.models.requests

import com.google.gson.annotations.SerializedName

/**
 * Created by zaven on 4/8/18.
 */

class AuthenticationRequest {

    @SerializedName("client_id")
    var clientId: String? = null

    @SerializedName("client_secret")
    var clientSecret: String? = null

    @SerializedName("device_android_id")
    var deviceAndroidId: String? = null
}
