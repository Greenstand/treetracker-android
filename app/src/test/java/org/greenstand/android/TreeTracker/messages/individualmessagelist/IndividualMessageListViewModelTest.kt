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
package org.greenstand.android.TreeTracker.messages.individualmessagelist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.messages.individualmeassagelist.IndividualMessageListAction
import org.greenstand.android.TreeTracker.messages.individualmeassagelist.IndividualMessageListViewModel
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.models.messages.AnnouncementMessage
import org.greenstand.android.TreeTracker.models.messages.DirectMessage
import org.greenstand.android.TreeTracker.models.messages.Message
import org.greenstand.android.TreeTracker.models.messages.MessagesRepo
import org.greenstand.android.TreeTracker.models.messages.SurveyMessage
import org.greenstand.android.TreeTracker.utils.FakeFileGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class IndividualMessageListViewModelTest {
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()
    private val userId = 2L
    private val userRepo = mockk<UserRepo>(relaxed = true)
    private val messagesRepo = mockk<MessagesRepo>(relaxed = true)
    private lateinit var testSubject: IndividualMessageListViewModel

    @Before
    fun setup() {
        coEvery { userRepo.getUser(any()) } returns FakeFileGenerator.fakeUsers.first()
        coEvery { messagesRepo.getMessageFlow(any()) } returns flowOf(FakeFileGenerator.messages)
        testSubject = IndividualMessageListViewModel(userId, userRepo, messagesRepo)
    }

    @Test
    fun `verify user repo gets the correct user`() =
        runTest {
            coVerify { userRepo.getUser(userId) }
        }

    @Test
    fun `WHEN message flow is triggered THEN state updates with correct message and user`() =
        runTest {
            val message =
                testSubject.state.value.messages
                    .first()
            val currentUser = testSubject.state.value.currentUser
            assertEquals(message, FakeFileGenerator.messages.first())
            assertEquals(currentUser, FakeFileGenerator.fakeUsers.first())
        }

    @Test
    fun `WHEN selected message is triggered THEN state updates with correct message`() =
        runTest {
            testSubject.handleAction(IndividualMessageListAction.SelectMessage(FakeFileGenerator.fakeSurveyMessage))
            val result = testSubject.state.value.selectedMessage
            assertEquals(result, FakeFileGenerator.messages[2])
        }

    @Test
    fun `WHEN selected message is null, Assert Null, THEN when we select message, returns correct data`() =
        runTest {
            val message = FakeFileGenerator.fakeDirectMessage
            assertNull(testSubject.state.value.selectedMessage)
            testSubject.handleAction(IndividualMessageListAction.SelectMessage(message))
            val result = testSubject.state.value.selectedMessage
            assertEquals(result, FakeFileGenerator.messages[1])
        }

    private val me = FakeFileGenerator.fakeUsers.first().copy(id = 9L, wallet = "me-wallet")

    private fun direct(
        id: String,
        from: String,
        composedAt: String,
    ) = DirectMessage(
        id = id,
        from = from,
        to = "me-wallet",
        composedAt = composedAt,
        isRead = false,
        parentMessageId = null,
        body = "body $id",
    )

    private fun survey(
        id: String,
        composedAt: String,
        isComplete: Boolean,
    ): SurveyMessage = FakeFileGenerator.fakeSurveyMessage.copy(id = id, composedAt = composedAt, isComplete = isComplete)

    private fun announcement(
        id: String,
        composedAt: String,
    ): AnnouncementMessage = FakeFileGenerator.fakeAnnouncementMessage.copy(id = id, composedAt = composedAt)

    private fun createViewModelFor(messages: List<Message>): IndividualMessageListViewModel {
        coEvery { userRepo.getUser(me.id) } returns me
        coEvery { messagesRepo.getMessageFlow("me-wallet") } returns flowOf(messages)
        return IndividualMessageListViewModel(me.id, userRepo, messagesRepo)
    }

    @Test
    fun `WHEN created THEN reads the message flow for the current user's wallet`() =
        runTest {
            createViewModelFor(emptyList())

            coVerify { messagesRepo.getMessageFlow("me-wallet") }
        }

    @Test
    fun `GIVEN no messages WHEN loaded THEN list is empty and current user is set`() =
        runTest {
            val viewModel = createViewModelFor(emptyList())

            assertTrue(
                viewModel.state.value.messages
                    .isEmpty(),
            )
            assertEquals(me, viewModel.state.value.currentUser)
            assertNull(viewModel.state.value.selectedMessage)
            assertTrue(viewModel.events.replayCache.isEmpty())
        }

    @Test
    fun `WHEN loaded THEN messages sent by the current user are hidden`() =
        runTest {
            val mine = direct("1", from = "me-wallet", composedAt = "2024-01-05")
            val theirs = direct("2", from = "Carla", composedAt = "2024-01-01")

            val viewModel = createViewModelFor(listOf(mine, theirs))

            assertEquals(listOf<Message>(theirs), viewModel.state.value.messages)
        }

    @Test
    fun `WHEN loaded THEN only the latest direct message per sender is shown`() =
        runTest {
            val older = direct("1", from = "Carla", composedAt = "2024-01-01")
            val newer = direct("2", from = "Carla", composedAt = "2024-01-03")
            val other = direct("3", from = "Mary", composedAt = "2024-01-02")

            val viewModel = createViewModelFor(listOf(older, other, newer))

            assertEquals(listOf<Message>(newer, other), viewModel.state.value.messages)
        }

    @Test
    fun `WHEN loaded THEN completed surveys are hidden and open surveys are shown`() =
        runTest {
            val done = survey("s1", composedAt = "2024-01-02", isComplete = true)
            val open = survey("s2", composedAt = "2024-01-01", isComplete = false)

            val viewModel = createViewModelFor(listOf(done, open))

            assertEquals(listOf<Message>(open), viewModel.state.value.messages)
        }

    @Test
    fun `WHEN loaded THEN every announcement is shown`() =
        runTest {
            val first = announcement("a1", composedAt = "2024-01-01")
            val second = announcement("a2", composedAt = "2024-01-02")

            val viewModel = createViewModelFor(listOf(first, second))

            assertEquals(listOf<Message>(second, first), viewModel.state.value.messages)
        }

    @Test
    fun `WHEN loaded THEN chats, surveys and announcements are merged newest first`() =
        runTest {
            val chat = direct("c", from = "Carla", composedAt = "2024-01-02")
            val openSurvey = survey("s", composedAt = "2024-01-03", isComplete = false)
            val news = announcement("a", composedAt = "2024-01-01")

            val viewModel = createViewModelFor(listOf(news, chat, openSurvey))

            assertEquals(listOf(openSurvey, chat, news), viewModel.state.value.messages)
        }

    @Test
    fun `WHEN the message flow emits again THEN the list is rebuilt`() =
        runTest {
            val first = announcement("a1", composedAt = "2024-01-01")
            val second = announcement("a2", composedAt = "2024-01-02")
            val flow = MutableStateFlow<List<Message>>(listOf(first))
            coEvery { userRepo.getUser(me.id) } returns me
            coEvery { messagesRepo.getMessageFlow("me-wallet") } returns flow
            val viewModel = IndividualMessageListViewModel(me.id, userRepo, messagesRepo)

            flow.value = listOf(first, second)

            assertEquals(listOf<Message>(second, first), viewModel.state.value.messages)
        }

    @Test
    fun `WHEN a message is selected THEN the message list is unchanged`() =
        runTest {
            val before = testSubject.state.value.messages

            testSubject.handleAction(IndividualMessageListAction.SelectMessage(FakeFileGenerator.fakeAnnouncementMessage))

            assertEquals(before, testSubject.state.value.messages)
            assertEquals(FakeFileGenerator.fakeAnnouncementMessage, testSubject.state.value.selectedMessage)
        }

    @Test
    fun `WHEN navigation actions are sent THEN state is unchanged and no events are emitted`() =
        runTest {
            testSubject.handleAction(IndividualMessageListAction.SelectMessage(FakeFileGenerator.fakeDirectMessage))
            val before = testSubject.state.value

            testSubject.handleAction(IndividualMessageListAction.NavigateBack)
            testSubject.handleAction(IndividualMessageListAction.NavigateToSelected)

            assertEquals(before, testSubject.state.value)
            assertTrue(testSubject.events.replayCache.isEmpty())
        }
}