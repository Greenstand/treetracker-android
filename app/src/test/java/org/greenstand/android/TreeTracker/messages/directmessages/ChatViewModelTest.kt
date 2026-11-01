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
package org.greenstand.android.TreeTracker.messages.directmessages

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.messages.ChatAction
import org.greenstand.android.TreeTracker.messages.ChatState
import org.greenstand.android.TreeTracker.messages.ChatViewModel
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.models.messages.DirectMessage
import org.greenstand.android.TreeTracker.models.messages.MessagesRepo
import org.greenstand.android.TreeTracker.utils.FakeFileGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class ChatViewModelTest {
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private val userId = 2L
    private val otherChatIdentifier = "Mary"
    private val userRepo = mockk<UserRepo>(relaxed = true)
    private val messagesRepo = mockk<MessagesRepo>(relaxed = true)
    private lateinit var testSubject: ChatViewModel

    @Before
    fun setup() {
        coEvery { userRepo.getUser(any()) } returns FakeFileGenerator.fakeUsers.first()
        coEvery { messagesRepo.getDirectMessages(any(), any()) } returns flowOf(FakeFileGenerator.fakeDirectMessageList)
        testSubject = ChatViewModel(userId, otherChatIdentifier, userRepo, messagesRepo)
    }

    @Test
    fun `WHEN draft text is empty,returns empty string, THEN when draft text updates, returns correct data`() =
        runTest {
            assertEquals(testSubject.state.value.draftText, "")
            testSubject.handleAction(ChatAction.UpdateDraftText("random"))
            val result = testSubject.state.value.draftText
            assertEquals(result, "random")
        }

    @Test
    fun `Verify message repo saves message`() =
        runTest {
            testSubject.handleAction(ChatAction.UpdateDraftText("Draft"))
            testSubject.handleAction(ChatAction.SendMessage)
            coVerify { messagesRepo.saveMessage("some random text", "Mary", "Draft") }
        }

    @Test
    fun `WHEN current user sends message THEN message is sent AND draft message is cleared`() =
        runTest {
            testSubject.handleAction(ChatAction.UpdateDraftText("Hello, World"))
            testSubject.handleAction(ChatAction.SendMessage)
            val result = testSubject.state.value.draftText
            assertEquals(result, "")
        }

    @Test
    fun `Check chat author, Assert True if its First Message by Author`() =
        runTest {
            assertTrue(testSubject.checkChatAuthor(0, true))
        }

    @Test
    fun `Check chat author, Assert False if its not First Message by Author`() =
        runTest {
            assertFalse(testSubject.checkChatAuthor(1, true))
        }

    @Test
    fun `Check Is Other User, Assert True for messages from other user`() =
        runTest {
            val result = testSubject.checkIsOtherUser(2)
            assertTrue(result)
        }

    @Test
    fun `Check Is Other User, Assert False for messages from current user`() =
        runTest {
            val result = testSubject.checkIsOtherUser(1)
            assertFalse(result)
        }

    private val me = FakeFileGenerator.fakeUsers.first().copy(wallet = "me-wallet")

    private fun directMessage(
        id: String,
        from: String,
        isRead: Boolean = true,
    ) = DirectMessage(
        id = id,
        from = from,
        to = if (from == "me-wallet") otherChatIdentifier else "me-wallet",
        composedAt = "2024-01-0$id",
        isRead = isRead,
        parentMessageId = null,
        body = "body $id",
    )

    @Test
    fun `WHEN created THEN loads the user and their conversation with the other party`() =
        runTest {
            coVerify { userRepo.getUser(userId) }
            coVerify { messagesRepo.getDirectMessages("some random text", otherChatIdentifier) }
        }

    @Test
    fun `WHEN messages are loaded THEN state holds the other party, current user and messages`() =
        runTest {
            val state = testSubject.state.value

            assertEquals(otherChatIdentifier, state.from)
            assertEquals(FakeFileGenerator.fakeUsers.first(), state.currentUser)
            assertEquals(FakeFileGenerator.fakeDirectMessageList, state.messages)
            assertEquals("", state.draftText)
            assertTrue(testSubject.events.replayCache.isEmpty())
        }

    @Test
    fun `WHEN messages are loaded THEN only unread messages are marked as read`() =
        runTest {
            coVerify { messagesRepo.markMessagesAsRead(listOf("helloId", "helloId")) }
        }

    @Test
    fun `GIVEN all messages read WHEN loaded THEN marks an empty list as read`() =
        runTest {
            coEvery { userRepo.getUser(5L) } returns me
            coEvery { messagesRepo.getDirectMessages("me-wallet", otherChatIdentifier) } returns
                flowOf(listOf(directMessage("1", otherChatIdentifier)))

            ChatViewModel(5L, otherChatIdentifier, userRepo, messagesRepo)

            coVerify { messagesRepo.markMessagesAsRead(emptyList()) }
        }

    @Test
    fun `WHEN a new message arrives THEN state updates and the new unread message is marked read`() =
        runTest {
            val first = directMessage("1", "me-wallet")
            val incoming = directMessage("2", otherChatIdentifier, isRead = false)
            val conversation = MutableStateFlow(listOf(first))
            coEvery { userRepo.getUser(5L) } returns me
            coEvery { messagesRepo.getDirectMessages("me-wallet", otherChatIdentifier) } returns conversation
            val viewModel = ChatViewModel(5L, otherChatIdentifier, userRepo, messagesRepo)

            conversation.value = listOf(incoming, first)

            assertEquals(listOf(incoming, first), viewModel.state.value.messages)
            coVerify { messagesRepo.markMessagesAsRead(listOf("2")) }
        }

    @Test
    fun `WHEN draft text is updated THEN messages are untouched`() =
        runTest {
            testSubject.handleAction(ChatAction.UpdateDraftText("Hi"))

            assertEquals(FakeFileGenerator.fakeDirectMessageList, testSubject.state.value.messages)
            assertEquals("Hi", testSubject.state.value.draftText)
        }

    @Test
    fun `GIVEN an empty draft WHEN sending THEN an empty message is saved`() =
        runTest {
            testSubject.handleAction(ChatAction.SendMessage)

            coVerify { messagesRepo.saveMessage("some random text", otherChatIdentifier, "") }
            assertEquals("", testSubject.state.value.draftText)
        }

    @Test
    fun `WHEN sending THEN no events are emitted`() =
        runTest {
            testSubject.handleAction(ChatAction.UpdateDraftText("Hi"))

            testSubject.handleAction(ChatAction.SendMessage)

            assertTrue(testSubject.events.replayCache.isEmpty())
        }

    @Test
    fun `WHEN NavigateBack action THEN state is unchanged and no events are emitted`() =
        runTest {
            testSubject.handleAction(ChatAction.UpdateDraftText("Hi"))
            val before: ChatState = testSubject.state.value

            testSubject.handleAction(ChatAction.NavigateBack)

            assertEquals(before, testSubject.state.value)
            assertTrue(testSubject.events.replayCache.isEmpty())
        }

    @Test
    fun `Check chat author, last message of a run by the same author`() =
        runTest {
            // Authors are Carla, Carla, Mary.
            assertFalse(testSubject.checkChatAuthor(0, false))
            assertTrue(testSubject.checkChatAuthor(1, false))
            assertTrue(testSubject.checkChatAuthor(2, false))
        }

    @Test
    fun `Check chat author, a single message is both first and last of its run`() =
        runTest {
            assertTrue(testSubject.checkChatAuthor(2, true))
            assertTrue(testSubject.checkChatAuthor(2, false))
        }

    @Test
    fun `Check Is Other User, Assert False for the first message from current side`() =
        runTest {
            assertFalse(testSubject.checkIsOtherUser(0))
        }
}