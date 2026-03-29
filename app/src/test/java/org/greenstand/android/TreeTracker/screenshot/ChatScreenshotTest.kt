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
package org.greenstand.android.TreeTracker.screenshot

import org.greenstand.android.TreeTracker.messages.Chat
import org.greenstand.android.TreeTracker.messages.ChatState
import org.greenstand.android.TreeTracker.models.messages.DirectMessage
import org.junit.Test

class ChatScreenshotTest : ScreenshotTest() {
    @Test
    fun chat_default() =
        snapshot {
            Chat(state = ChatState())
        }

    @Test
    fun chat_with_messages() =
        snapshot {
            Chat(
                state =
                    ChatState(
                        from = "planter_abc",
                        messages =
                            listOf(
                                DirectMessage(
                                    id = "1",
                                    from = "planter_abc",
                                    to = "my_wallet",
                                    composedAt = "2024-01-15T10:00:00Z",
                                    isRead = true,
                                    parentMessageId = null,
                                    body = "Hello, how are the trees doing?",
                                ),
                                DirectMessage(
                                    id = "2",
                                    from = "my_wallet",
                                    to = "planter_abc",
                                    composedAt = "2024-01-15T10:05:00Z",
                                    isRead = true,
                                    parentMessageId = "1",
                                    body = "They are growing well! Planted 10 more today.",
                                ),
                                DirectMessage(
                                    id = "3",
                                    from = "planter_abc",
                                    to = "my_wallet",
                                    composedAt = "2024-01-15T10:10:00Z",
                                    isRead = false,
                                    parentMessageId = "2",
                                    body = "Great work, keep it up!",
                                ),
                            ),
                        draftText = "",
                    ),
                checkIsOtherUser = { index ->
                    listOf(true, false, true)[index]
                },
                checkChatAuthor = { index, _ ->
                    listOf(true, true, true)[index]
                },
            )
        }

    @Test
    fun chat_with_draft() =
        snapshot {
            Chat(
                state =
                    ChatState(
                        from = "coordinator",
                        draftText = "I will plant more trees tomorrow",
                    ),
            )
        }
}