package org.greenstand.android.TreeTracker.models.messages.network.responses

data class QueryResponse(
    val total: Int,
    val handle: String,
    val limit: Int,
    val offset: Int,
)