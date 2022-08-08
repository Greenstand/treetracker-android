package org.greenstand.android.TreeTracker.models.messages.network

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import org.greenstand.android.TreeTracker.models.messages.network.responses.MessageType
import java.lang.reflect.Type

class MessageTypeDeserializer: JsonDeserializer<MessageType> {

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): MessageType {
        return when(json.asString) {
            "message" -> MessageType.MESSAGE
            "announce" -> MessageType.ANNOUNCE
            "survey" -> MessageType.SURVEY
            "survey_response" -> MessageType.SURVEY_RESPONSE
            else -> throw IllegalArgumentException("Message type unknown.")
        }
    }
}