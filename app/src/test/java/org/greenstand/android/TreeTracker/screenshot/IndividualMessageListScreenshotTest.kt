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

import androidx.compose.foundation.ExperimentalFoundationApi
import org.greenstand.android.TreeTracker.messages.individualmeassagelist.IndividualMessageList
import org.greenstand.android.TreeTracker.messages.individualmeassagelist.IndividualMessageListState
import org.greenstand.android.TreeTracker.models.messages.AnnouncementMessage
import org.greenstand.android.TreeTracker.models.messages.DirectMessage
import org.greenstand.android.TreeTracker.models.messages.Question
import org.greenstand.android.TreeTracker.models.messages.SurveyMessage
import org.junit.Test

@OptIn(ExperimentalFoundationApi::class)
class IndividualMessageListScreenshotTest : ScreenshotTest() {
    @Test
    fun individual_message_list_empty() =
        snapshot {
            IndividualMessageList(state = IndividualMessageListState())
        }

    @Test
    fun individual_message_list_with_messages() =
        snapshot {
            val messages =
                listOf(
                    DirectMessage(
                        id = "dm1",
                        from = "planter_abc",
                        to = "my_wallet",
                        composedAt = "2024-01-15T10:00:00Z",
                        isRead = false,
                        parentMessageId = null,
                        body = "Hello!",
                    ),
                    SurveyMessage(
                        id = "survey1",
                        from = "admin",
                        to = "my_wallet",
                        composedAt = "2024-01-14T09:00:00Z",
                        isRead = true,
                        surveyId = "s1",
                        title = "Tree Health Survey",
                        questions =
                            listOf(
                                Question(prompt = "How are the trees?", choices = listOf("Good", "Bad")),
                            ),
                        isComplete = false,
                    ),
                    AnnouncementMessage(
                        id = "ann1",
                        from = "Greenstand",
                        to = "my_wallet",
                        composedAt = "2024-01-13T08:00:00Z",
                        isRead = true,
                        subject = "Monthly Update",
                        body = "Great progress this month!",
                        videoLink = null,
                    ),
                )
            IndividualMessageList(
                state =
                    IndividualMessageListState(
                        messages = messages,
                    ),
            )
        }

    @Test
    fun individual_message_list_with_selection() =
        snapshot {
            val selectedMessage =
                DirectMessage(
                    id = "dm1",
                    from = "planter_abc",
                    to = "my_wallet",
                    composedAt = "2024-01-15T10:00:00Z",
                    isRead = true,
                    parentMessageId = null,
                    body = "Hello!",
                )
            IndividualMessageList(
                state =
                    IndividualMessageListState(
                        messages = listOf(selectedMessage),
                        selectedMessage = selectedMessage,
                    ),
            )
        }
}