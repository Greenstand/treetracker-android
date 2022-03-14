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
        false,
        "parent ID",
        "Hi, I am Taofeek from Greenstand",
        ),DirectMessage(
        "ID",
        "Teekay",
        "Receiver",
        "8:12 PM",
        false,
        "parent ID",
        "Nice to meet you",
        ),DirectMessage(
        "ID",
        "Author 2",
        "Receiver",
        "8:27 PM",
        true,
        "parent ID",
        "\n" +
                "tree captures. We provide tree-planting initiatives with the ability to be\n" +
                "transparent with their donors and clearly communicate their impacts. In\n" +
                "addition to its environmental benefits, the Treetracker platform can help\n" +
                "planting initiatives compensate their planters on the ground. The\n" +
                "Treetracker does more than incentivize environmental action; it allows tree\n" +
                "growers to earn a living while protecting their local ecosystems",
        ),DirectMessage(
        "ID",
        "admin",
        "Receiver",
        "8:34 PM",
        false,
        "parent ID",
        "Message Content",
        ),DirectMessage(
        "ID",
        "admin",
        "Receiver",
        "8:34 PM",
        true,
        "parent ID",
        "Hi",
        ),DirectMessage(
        "ID",
        "adn",
        "Receiver",
        "8:34 PM",
        false,
        "parent ID",
        "Hello",
        ),
DirectMessage(
        "ID",
        "Author",
        "Receiver",
        "8:07 PM",
        false,
        "parent ID",
        "Hi, I am Taofeek from Greenstand",
        ),DirectMessage(
        "ID",
        "Teekay",
        "Receiver",
        "8:12 PM",
        false,
        "parent ID",
        "Nice to meet you",
        ),DirectMessage(
        "ID",
        "Author 2",
        "Receiver",
        "8:27 PM",
        true,
        "parent ID",
        "\n" +
                "tree captures. We provide tree-planting initiatives with the ability to be\n" +
                "transparent with their donors and clearly communicate their impacts. In\n" +
                "addition to its environmental benefits, the Treetracker platform can help\n" +
                "planting initiatives compensate their planters on the ground. The\n" +
                "Treetracker does more than incentivize environmental action; it allows tree\n" +
                "growers to earn a living while protecting their local ecosystems",
        ),DirectMessage(
        "ID",
        "admin",
        "Receiver",
        "8:34 PM",
        false,
        "parent ID",
        "Message Content",
        ),DirectMessage(
        "ID",
        "admin",
        "Receiver",
        "8:34 PM",
        true,
        "parent ID",
        "Hi",
        ),DirectMessage(
        "ID",
        "adn",
        "Receiver",
        "8:34 PM",
        false,
        "parent ID",
        "Hello",
        ),

)

val exampleUiState = ConversationUiState(
    initialMessages = initialMessages,
    channelName = "#composers",
    channelMembers = 42
)


