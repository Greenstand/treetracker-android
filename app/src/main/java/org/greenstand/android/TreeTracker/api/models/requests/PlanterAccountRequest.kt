package org.greenstand.android.TreeTracker.api.models.requests

import com.google.gson.annotations.SerializedName

data class PlanterAccountRequest(
    @SerializedName("planter_identifiers")
    val planterIdentifiers: Set<String>
)