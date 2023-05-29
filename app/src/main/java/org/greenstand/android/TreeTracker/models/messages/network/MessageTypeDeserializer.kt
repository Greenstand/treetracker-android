/*
 * Copyright 2023 Treetracker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.greenstand.android.TreeTracker.models.messages.network

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import org.greenstand.android.TreeTracker.models.messages.network.responses.MessageType
import java.lang.reflect.Type

class MessageTypeDeserializer : JsonDeserializer<MessageType> {

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): MessageType {
        return when (json.asString) {
            "message" -> MessageType.MESSAGE
            "announce" -> MessageType.ANNOUNCE
            "survey" -> MessageType.SURVEY
            "survey_response" -> MessageType.SURVEY_RESPONSE
            else -> throw IllegalArgumentException("Message type unknown.")
        }
    }
}