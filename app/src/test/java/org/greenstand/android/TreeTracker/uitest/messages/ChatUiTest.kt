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

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.messages.Chat
import org.greenstand.android.TreeTracker.messages.ChatScreen
import org.greenstand.android.TreeTracker.messages.ChatState
import org.greenstand.android.TreeTracker.messages.ChatViewModel
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.models.messages.DirectMessage
import org.greenstand.android.TreeTracker.models.messages.MessagesRepo
import org.greenstand.android.TreeTracker.models.user.User
import org.greenstand.android.TreeTracker.navigation.ChatRoute
import org.greenstand.android.TreeTracker.navigation.DashboardRoute
import org.greenstand.android.TreeTracker.navigation.IndividualMessageListRoute
import org.greenstand.android.TreeTracker.uitest.ComposeUiTest
import org.greenstand.android.TreeTracker.view.ARROW_BACK_BUTTON_TAG
import org.greenstand.android.TreeTracker.view.ARROW_FORWARD_BUTTON_TAG
import org.junit.Assert.assertEquals
import org.junit.Test

class ChatUiTest : ComposeUiTest() {
    private var backClicks = 0
    private var sendClicks = 0
    private val drafts = mutableListOf<String>()

    private val filledState =
        ChatState(
            from = OTHER,
            messages = listOf(INCOMING, OUTGOING),
            currentUser = USER,
        )

    @Test
    fun `chat shows the other participant in upper case`() {
        setContent { Chat(state = filledState) }

        composeTestRule.onNodeWithText(OTHER.uppercase()).assertIsDisplayed()
    }

    @Test
    fun `chat shows every message body`() {
        setContent { Chat(state = filledState) }

        composeTestRule.onNodeWithText(INCOMING.body).assertIsDisplayed()
        composeTestRule.onNodeWithText(OUTGOING.body).assertIsDisplayed()
    }

    @Test
    fun `empty chat shows no message bubbles`() {
        setContent { Chat(state = ChatState(from = OTHER)) }

        composeTestRule.onNodeWithText(INCOMING.body).assertDoesNotExist()
        composeTestRule.onNodeWithText(OUTGOING.body).assertDoesNotExist()
    }

    @Test
    fun `empty draft shows the write message placeholder`() {
        setContent { Chat(state = filledState) }

        composeTestRule.onNodeWithText(string(R.string.click_to_write_message)).assertExists()
    }

    @Test
    fun `draft text is shown in the input field`() {
        setContent { Chat(state = filledState.copy(draftText = DRAFT)) }

        composeTestRule.onNode(hasSetTextAction() and hasText(DRAFT)).assertIsDisplayed()
    }

    @Test
    fun `typing reports the new draft`() {
        setContent { Chat(state = filledState, onDraftTextChanged = { drafts += it }) }

        composeTestRule.onNode(hasSetTextAction()).performTextInput(DRAFT)

        composeTestRule.runOnIdle { assertEquals(DRAFT, drafts.last()) }
    }

    @Test
    fun `the keyboard send action sends the message`() {
        setContent { Chat(state = filledState.copy(draftText = DRAFT), onSendClicked = { sendClicks++ }) }

        composeTestRule.onNode(hasSetTextAction()).performImeAction()

        composeTestRule.runOnIdle { assertEquals(1, sendClicks) }
    }

    @Test
    fun `chat only offers a back button`() {
        setContent { Chat(state = filledState) }

        composeTestRule.onNodeWithTag(ARROW_BACK_BUTTON_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).assertDoesNotExist()
    }

    @Test
    fun `clicking back invokes onBackClicked`() {
        setContent { Chat(state = filledState, onBackClicked = { backClicks++ }) }

        composeTestRule.onNodeWithTag(ARROW_BACK_BUTTON_TAG).performClick()

        composeTestRule.runOnIdle { assertEquals(1, backClicks) }
    }

    @Test
    fun `screen shows the conversation loaded by the view model`() {
        setScreenContent(fakeMessagesRepo())

        composeTestRule.onNodeWithText(OTHER.uppercase()).assertIsDisplayed()
        composeTestRule.onNodeWithText(INCOMING.body).assertIsDisplayed()
        composeTestRule.onNodeWithText(OUTGOING.body).assertIsDisplayed()
    }

    @Test
    fun `typing on the screen updates the draft`() {
        setScreenContent(fakeMessagesRepo())

        composeTestRule.onNode(hasSetTextAction()).performTextInput(DRAFT)

        composeTestRule.onNode(hasSetTextAction() and hasText(DRAFT)).assertIsDisplayed()
    }

    @Test
    fun `sending on the screen saves the draft and clears the input`() {
        val messagesRepo = fakeMessagesRepo()
        setScreenContent(messagesRepo)

        composeTestRule.onNode(hasSetTextAction()).performTextInput(DRAFT)
        composeTestRule.onNode(hasSetTextAction()).performImeAction()

        composeTestRule.onNode(hasSetTextAction() and hasText(DRAFT)).assertDoesNotExist()
        coVerify { messagesRepo.saveMessage(USER.wallet, OTHER, DRAFT) }
    }

    @Test
    fun `back on the screen returns to the message list`() {
        setScreenContent(fakeMessagesRepo())

        composeTestRule.onNodeWithTag(ARROW_BACK_BUTTON_TAG).performClick()

        composeTestRule.runOnIdle {
            assertEquals(listOf(DashboardRoute, IndividualMessageListRoute(USER.id)), backStack.toList())
        }
    }

    private fun fakeMessagesRepo(): MessagesRepo {
        val messagesRepo = mockk<MessagesRepo>(relaxed = true)
        every { messagesRepo.getDirectMessages(USER.wallet, OTHER) } returns flowOf(listOf(INCOMING, OUTGOING))
        return messagesRepo
    }

    private fun setScreenContent(messagesRepo: MessagesRepo) {
        backStack.add(IndividualMessageListRoute(USER.id))
        backStack.add(ChatRoute(planterInfoId = USER.id, otherChatIdentifier = OTHER))
        val userRepo = mockk<UserRepo>()
        coEvery { userRepo.getUser(USER.id) } returns USER
        val viewModel = ChatViewModel(USER.id, OTHER, userRepo, messagesRepo)
        setContent(inspectionMode = false) { ChatScreen(USER.id, OTHER, viewModel) }
    }

    private companion object {
        const val OTHER = "friend@example.com"
        const val DRAFT = "See you at the nursery"

        val USER =
            User(
                id = 3,
                wallet = "planter@example.com",
                numberOfTrees = 3,
                firstName = "Ada",
                lastName = "Lovelace",
                photoPath = "/does/not/exist.jpg",
                isPowerUser = false,
                unreadMessagesAvailable = false,
            )

        val INCOMING =
            DirectMessage(
                id = "message-1",
                from = OTHER,
                to = "planter@example.com",
                composedAt = "2026-01-02T00:00:00Z",
                isRead = false,
                parentMessageId = null,
                body = "How are the seedlings?",
            )

        val OUTGOING =
            DirectMessage(
                id = "message-2",
                from = "planter@example.com",
                to = OTHER,
                composedAt = "2026-01-01T00:00:00Z",
                isRead = true,
                parentMessageId = null,
                body = "Growing well!",
            )
    }
}