package org.greenstand.android.TreeTracker.api.models.requests

import com.google.gson.annotations.SerializedName

/**
 * Created by zaven on 4/8/18.
 */

class RegisterRequest {

    @SerializedName("first_name")
    var firstName: String? = null

    @SerializedName("last_name")
    var lastName: String? = null

    @SerializedName("phone")
    var phone: String? = null

    @SerializedName("organization")
    var organization: String? = null

    @SerializedName("client_id")
    var clientId: String? = null

    @SerializedName("client_secret")
    var clientSecret: String? = null
}
