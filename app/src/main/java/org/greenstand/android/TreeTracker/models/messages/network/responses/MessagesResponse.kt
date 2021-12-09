package org.greenstand.android.TreeTracker.models.messages.network.responses

data class MessagesResponse(
    val messages: List<MessageResponse>,
    val links: LinksResponse,
)