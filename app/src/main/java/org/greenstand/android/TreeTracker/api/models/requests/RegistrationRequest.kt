package org.greenstand.android.TreeTracker.api.models.requests

import com.google.gson.annotations.SerializedName

class RegistrationRequest {

    @SerializedName("planter_identifier")
    var planterIdentifier: String? = null

    @SerializedName("first_name")
    var firstName: String? = null

    @SerializedName("last_name")
    var lastName: String? = null

    @SerializedName("organization")
    var organization: String? = null

}