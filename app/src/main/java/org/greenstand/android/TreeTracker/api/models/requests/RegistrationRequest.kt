package org.greenstand.android.TreeTracker.api.models.requests

import com.google.gson.annotations.SerializedName

data class RegistrationRequest(@SerializedName("planter_identifier")
                               val planterIdentifier: String?,
                               @SerializedName("first_name")
                               val firstName: String?,
                               @SerializedName("last_name")
                               val lastName: String?,
                               @SerializedName("organization")
                               val organization: String?)