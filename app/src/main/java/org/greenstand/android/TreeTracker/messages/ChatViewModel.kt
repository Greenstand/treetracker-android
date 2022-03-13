package org.greenstand.android.TreeTracker.messages


import androidx.compose.runtime.mutableStateListOf
import org.greenstand.android.TreeTracker.models.messages.DirectMessage


class ConversationUiState(
    val channelName: String,
    val channelMembers: Int,
    initialMessages: List<DirectMessage>
) {
    private val _messages: MutableList<DirectMessage> =
        mutableStateListOf(*initialMessages.toTypedArray())
    val messages: List<DirectMessage> = _messages

    fun addMessage(msg: DirectMessage) {
        _messages.add(0, msg) // Add to the beginning of the list
    }
}

private val initialMessages = listOf(
    DirectMessage(
        "ID",
        "Author",
        "Receiver",
        "8:07 PM",
        "parent ID",
        "Message Content",
        ),DirectMessage(
        "ID",
        "Teekay",
        "Receiver",
        "8:12 PM",
        "parent ID",
        "Message Content",
        ),DirectMessage(
        "ID",
        "Author 2",
        "Receiver",
        "8:27 PM",
        "parent ID",
        "Message Content",
        ),DirectMessage(
        "ID",
        "admin",
        "Receiver",
        "8:34 PM",
        "parent ID",
        "Message Content",
        ),DirectMessage(
        "ID",
        "admin",
        "Receiver",
        "8:34 PM",
        "parent ID",
        "Message Content",
        ),DirectMessage(
        "ID",
        "adn",
        "Receiver",
        "8:34 PM",
        "parent ID",
        "Message Content",
        ),

)

val exampleUiState = ConversationUiState(
    initialMessages = initialMessages,
    channelName = "#composers",
    channelMembers = 42
)


