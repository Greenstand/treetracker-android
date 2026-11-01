/*
 * Copyright 2026 Treetracker
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
package org.greenstand.android.TreeTracker.uitest.messages

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.messages.individualmeassagelist.IndividualMessageList
import org.greenstand.android.TreeTracker.messages.individualmeassagelist.IndividualMessageListAction
import org.greenstand.android.TreeTracker.messages.individualmeassagelist.IndividualMessageListScreen
import org.greenstand.android.TreeTracker.messages.individualmeassagelist.IndividualMessageListState
import org.greenstand.android.TreeTracker.messages.individualmeassagelist.IndividualMessageListViewModel
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.models.messages.AnnouncementMessage
import org.greenstand.android.TreeTracker.models.messages.DirectMessage
import org.greenstand.android.TreeTracker.models.messages.Message
import org.greenstand.android.TreeTracker.models.messages.MessagesRepo
import org.greenstand.android.TreeTracker.models.messages.Question
import org.greenstand.android.TreeTracker.models.messages.SurveyMessage
import org.greenstand.android.TreeTracker.models.user.User
import org.greenstand.android.TreeTracker.navigation.AnnouncementRoute
import org.greenstand.android.TreeTracker.navigation.ChatRoute
import org.greenstand.android.TreeTracker.navigation.DashboardRoute
import org.greenstand.android.TreeTracker.navigation.IndividualMessageListRoute
import org.greenstand.android.TreeTracker.navigation.MessagesUserSelectRoute
import org.greenstand.android.TreeTracker.navigation.SurveyRoute
import org.greenstand.android.TreeTracker.uitest.ComposeUiTest
import org.greenstand.android.TreeTracker.view.ARROW_BACK_BUTTON_TAG
import org.greenstand.android.TreeTracker.view.ARROW_FORWARD_BUTTON_TAG
import org.greenstand.android.TreeTracker.view.USER_IMAGE_BUTTON_TAG
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalFoundationApi::class)
class IndividualMessageListUiTest : ComposeUiTest() {
    private val actions = mutableListOf<IndividualMessageListAction>()

    private val filledState =
        IndividualMessageListState(
            messages = listOf(DIRECT, SURVEY, ANNOUNCEMENT),
            currentUser = USER,
        )

    @Test
    fun `empty list shows the no messages placeholder`() {
        setContent { IndividualMessageList(state = IndividualMessageListState(currentUser = USER)) }

        composeTestRule.onNodeWithText(string(R.string.no_messages_yet)).assertIsDisplayed()
    }

    @Test
    fun `filled list shows a card for every message type`() {
        setContent { IndividualMessageList(state = filledState) }

        composeTestRule.onNodeWithText(DIRECT.from).assertIsDisplayed()
        composeTestRule.onNodeWithText(SURVEY.title).assertIsDisplayed()
        composeTestRule.onNodeWithText(ANNOUNCEMENT.subject).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.no_messages_yet)).assertDoesNotExist()
    }

    @Test
    fun `cards are labelled with their message type`() {
        setContent { IndividualMessageList(state = filledState) }

        composeTestRule.onNodeWithText(string(R.string.message).uppercase()).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.survey).uppercase()).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.announcement).uppercase()).assertIsDisplayed()
    }

    @Test
    fun `the user image is shown when a user is loaded`() {
        setContent { IndividualMessageList(state = filledState) }

        composeTestRule.onNodeWithTag(USER_IMAGE_BUTTON_TAG).assertIsDisplayed()
    }

    @Test
    fun `the user image is hidden while no user is loaded`() {
        setContent { IndividualMessageList(state = IndividualMessageListState()) }

        composeTestRule.onNodeWithTag(USER_IMAGE_BUTTON_TAG).assertDoesNotExist()
    }

    @Test
    fun `clicking a message selects it`() {
        setContent { IndividualMessageList(state = filledState, onHandleAction = { actions += it }) }

        composeTestRule.onNodeWithText(SURVEY.title).performClick()

        composeTestRule.runOnIdle {
            assertEquals(listOf(IndividualMessageListAction.SelectMessage(SURVEY)), actions)
        }
    }

    @Test
    fun `forward is disabled while no message is selected`() {
        setContent { IndividualMessageList(state = filledState, onHandleAction = { actions += it }) }

        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()

        composeTestRule.runOnIdle { assertTrue(actions.isEmpty()) }
    }

    @Test
    fun `forward opens the selected message`() {
        setContent {
            IndividualMessageList(
                state = filledState.copy(selectedMessage = DIRECT),
                onHandleAction = { actions += it },
            )
        }

        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()

        composeTestRule.runOnIdle {
            assertEquals(listOf(IndividualMessageListAction.NavigateToSelected), actions)
        }
    }

    @Test
    fun `back arrow sends NavigateBack`() {
        setContent { IndividualMessageList(state = filledState, onHandleAction = { actions += it }) }

        composeTestRule.onNodeWithTag(ARROW_BACK_BUTTON_TAG).performClick()

        composeTestRule.runOnIdle { assertEquals(listOf(IndividualMessageListAction.NavigateBack), actions) }
    }

    @Test
    fun `user image sends NavigateBack`() {
        setContent { IndividualMessageList(state = filledState, onHandleAction = { actions += it }) }

        composeTestRule.onNodeWithTag(USER_IMAGE_BUTTON_TAG).performClick()

        composeTestRule.runOnIdle { assertEquals(listOf(IndividualMessageListAction.NavigateBack), actions) }
    }

    @Test
    fun `screen shows the messages loaded for the user`() {
        setScreenContent()

        composeTestRule.onNodeWithText(DIRECT.from).assertIsDisplayed()
        composeTestRule.onNodeWithText(SURVEY.title).assertIsDisplayed()
        composeTestRule.onNodeWithText(ANNOUNCEMENT.subject).assertIsDisplayed()
    }

    @Test
    fun `selecting a direct message and going forward opens the chat`() {
        setScreenContent()

        composeTestRule.onNodeWithText(DIRECT.from).performClick()
        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()

        composeTestRule.runOnIdle {
            assertEquals(ChatRoute(planterInfoId = USER.id, otherChatIdentifier = DIRECT.from), backStack.last())
        }
    }

    @Test
    fun `selecting a survey and going forward opens the survey`() {
        setScreenContent()

        composeTestRule.onNodeWithText(SURVEY.title).performClick()
        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()

        composeTestRule.runOnIdle { assertEquals(SurveyRoute(messageId = SURVEY.id), backStack.last()) }
    }

    @Test
    fun `selecting an announcement and going forward opens the announcement`() {
        setScreenContent()

        composeTestRule.onNodeWithText(ANNOUNCEMENT.subject).performClick()
        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()

        composeTestRule.runOnIdle {
            assertEquals(AnnouncementRoute(messageId = ANNOUNCEMENT.id), backStack.last())
        }
    }

    @Test
    fun `going forward without a selection does not navigate`() {
        setScreenContent()

        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()

        composeTestRule.runOnIdle { assertEquals(IndividualMessageListRoute(USER.id), backStack.last()) }
    }

    @Test
    fun `back on the screen returns to the user select`() {
        setScreenContent()

        composeTestRule.onNodeWithTag(ARROW_BACK_BUTTON_TAG).performClick()

        composeTestRule.runOnIdle {
            assertEquals(listOf(DashboardRoute, MessagesUserSelectRoute), backStack.toList())
        }
    }

    @Test
    fun `screen hides completed surveys`() {
        setScreenContent(flowOf(listOf(DIRECT, SURVEY.copy(isComplete = true), ANNOUNCEMENT)))

        composeTestRule.onNodeWithText(DIRECT.from).assertExists()
        composeTestRule.onNodeWithText(ANNOUNCEMENT.subject).assertExists()
        composeTestRule.onNodeWithText(SURVEY.title).assertDoesNotExist()
    }

    @Test
    fun `screen shows one card per correspondent and none for the planter's own messages`() {
        val olderFromFriend = DIRECT.copy(id = "direct-0", composedAt = "2025-12-01T00:00:00Z", body = "Older")
        val ownReply = DIRECT.copy(id = "direct-2", from = USER.wallet, to = DIRECT.from, body = "Reply")
        setScreenContent(flowOf(listOf(olderFromFriend, DIRECT, ownReply)))

        composeTestRule.onAllNodesWithText(DIRECT.from).assertCountEquals(1)
        composeTestRule.onNodeWithText(USER.wallet).assertDoesNotExist()
    }

    @Test
    fun `screen with no messages shows the empty inbox`() {
        setScreenContent(flowOf(emptyList()))

        composeTestRule.onNodeWithText(string(R.string.no_messages_yet)).assertIsDisplayed()
    }

    @Test
    fun `screen with only completed surveys shows the empty inbox`() {
        setScreenContent(flowOf(listOf(SURVEY.copy(isComplete = true))))

        composeTestRule.onNodeWithText(string(R.string.no_messages_yet)).assertIsDisplayed()
    }

    @Test
    fun `a message arriving while the list is open replaces the empty inbox`() {
        val messages = MutableStateFlow<List<Message>>(emptyList())
        setScreenContent(messages)
        composeTestRule.onNodeWithText(string(R.string.no_messages_yet)).assertExists()

        messages.value = listOf(ANNOUNCEMENT)

        composeTestRule.onNodeWithText(ANNOUNCEMENT.subject).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.no_messages_yet)).assertDoesNotExist()
    }

    @Test
    fun `the chat opened is the one with the selected correspondent`() {
        val otherFriend = DIRECT.copy(id = "direct-9", from = "colleague@example.com", composedAt = "2026-01-04T00:00:00Z")
        setScreenContent(flowOf(listOf(DIRECT, otherFriend)))

        composeTestRule.onNodeWithText(otherFriend.from).performClick()
        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()

        composeTestRule.runOnIdle {
            assertEquals(ChatRoute(planterInfoId = USER.id, otherChatIdentifier = otherFriend.from), backStack.last())
        }
    }

    private fun setScreenContent(messages: Flow<List<Message>> = flowOf(listOf(DIRECT, SURVEY, ANNOUNCEMENT))) {
        backStack.add(MessagesUserSelectRoute)
        backStack.add(IndividualMessageListRoute(USER.id))
        val userRepo = mockk<UserRepo>()
        coEvery { userRepo.getUser(USER.id) } returns USER
        val messagesRepo = mockk<MessagesRepo>()
        every { messagesRepo.getMessageFlow(USER.wallet) } returns messages
        val viewModel = IndividualMessageListViewModel(USER.id, userRepo, messagesRepo)
        setContent(inspectionMode = false) { IndividualMessageListScreen(USER.id, viewModel) }
    }

    private companion object {
        val USER =
            User(
                id = 7,
                wallet = "planter@example.com",
                numberOfTrees = 3,
                firstName = "Ada",
                lastName = "Lovelace",
                photoPath = "/does/not/exist.jpg",
                isPowerUser = false,
                unreadMessagesAvailable = true,
            )

        val DIRECT =
            DirectMessage(
                id = "direct-1",
                from = "friend@example.com",
                to = "planter@example.com",
                composedAt = "2026-01-03T00:00:00Z",
                isRead = false,
                parentMessageId = null,
                body = "Hello there",
            )

        val SURVEY =
            SurveyMessage(
                id = "survey-1",
                from = "greenstand",
                to = "planter@example.com",
                composedAt = "2026-01-02T00:00:00Z",
                isRead = true,
                surveyId = "survey",
                title = "Planting survey",
                questions = listOf(Question(prompt = "How many?", choices = listOf("1", "2"))),
                isComplete = false,
            )

        val ANNOUNCEMENT =
            AnnouncementMessage(
                id = "announcement-1",
                from = "greenstand",
                to = "planter@example.com",
                composedAt = "2026-01-01T00:00:00Z",
                isRead = false,
                subject = "Planting season",
                body = "Starts next week",
                videoLink = null,
            )
    }
}