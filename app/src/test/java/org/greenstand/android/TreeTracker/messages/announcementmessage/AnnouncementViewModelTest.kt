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
package org.greenstand.android.TreeTracker.messages.announcementmessage

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.models.messages.MessagesRepo
import org.greenstand.android.TreeTracker.navigation.AnnouncementRoute
import org.greenstand.android.TreeTracker.navigation.IndividualMessageListRoute
import org.greenstand.android.TreeTracker.navigation.MessagesUserSelectRoute
import org.greenstand.android.TreeTracker.utils.FakeFileGenerator.fakeAnnouncementMessage
import org.greenstand.android.TreeTracker.utils.fakeNavigator
import org.greenstand.android.TreeTracker.viewmodel.PopBackStackEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class AnnouncementViewModelTest {
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private val messageId = ""
    private val messagesRepo = mockk<MessagesRepo>(relaxed = true)
    private lateinit var announcementViewModel: AnnouncementViewModel

    @Before
    fun setup() {
        coEvery { messagesRepo.getAnnouncementMessages(any()) } returns fakeAnnouncementMessage
        announcementViewModel = AnnouncementViewModel(messageId, messagesRepo)
    }

    @Test
    fun `verify messages repo gets the correct announcement message `() =
        runTest {
            coVerify { messagesRepo.getAnnouncementMessages(messageId) }
        }

    @Test
    fun `WHEN get announcement is triggered THEN current URL changes`() =
        runTest {
            announcementViewModel.state.test {
                assertEquals(awaitItem().currentUrl, fakeAnnouncementMessage.videoLink)
            }
        }

    @Test
    fun `WHEN get announcement is triggered THEN current title changes`() =
        runTest {
            announcementViewModel.state.test {
                assertEquals(awaitItem().currentTitle, fakeAnnouncementMessage.subject)
            }
        }

    @Test
    fun `WHEN get announcement is triggered THEN from state changes`() =
        runTest {
            announcementViewModel.state.test {
                assertEquals(awaitItem().from, fakeAnnouncementMessage.from)
            }
        }

    @Test
    fun `WHEN get announcement is triggered THEN current body changes`() =
        runTest {
            announcementViewModel.state.test {
                assertEquals(awaitItem().currentBody, fakeAnnouncementMessage.body)
            }
        }

    @Test
    fun `verify message repo marks message as read`() =
        runTest {
            coVerify { messagesRepo.markMessageAsRead(messageId) }
        }

    @Test
    fun `WHEN NavigateBack action THEN emits PopBackStackEvent`() =
        runTest {
            announcementViewModel.handleAction(AnnouncementAction.NavigateBack)

            val event = announcementViewModel.events.first().getContentIfNotConsumed()
            assertEquals(PopBackStackEvent, event)
        }

    @Test
    fun `WHEN created THEN loads the given message, fills state and marks it read in order`() =
        runTest {
            val message =
                fakeAnnouncementMessage.copy(
                    id = "msg-1",
                    from = "Greenstand",
                    subject = "Planting day",
                    body = "Join us",
                    videoLink = "https://example.com/video",
                )
            coEvery { messagesRepo.getAnnouncementMessages("msg-1") } returns message

            val viewModel = AnnouncementViewModel("msg-1", messagesRepo)

            assertEquals(
                AnnouncementState(
                    from = "Greenstand",
                    currentBody = "Join us",
                    currentUrl = "https://example.com/video",
                    currentTitle = "Planting day",
                ),
                viewModel.state.value,
            )
            coVerifyOrder {
                messagesRepo.getAnnouncementMessages("msg-1")
                messagesRepo.markMessageAsRead("msg-1")
            }
        }

    @Test
    fun `GIVEN an announcement without body or link WHEN created THEN body and url are null`() =
        runTest {
            coEvery { messagesRepo.getAnnouncementMessages("msg-2") } returns
                fakeAnnouncementMessage.copy(body = null, videoLink = null)

            val viewModel = AnnouncementViewModel("msg-2", messagesRepo)

            assertNull(viewModel.state.value.currentBody)
            assertNull(viewModel.state.value.currentUrl)
            assertEquals(fakeAnnouncementMessage.subject, viewModel.state.value.currentTitle)
        }

    @Test
    fun `WHEN created THEN no events are emitted`() =
        runTest {
            assertTrue(announcementViewModel.events.replayCache.isEmpty())
        }

    @Test
    fun `WHEN OpenLink action THEN state is unchanged and no events are emitted`() =
        runTest {
            val before = announcementViewModel.state.value

            announcementViewModel.handleAction(AnnouncementAction.OpenLink("https://example.com"))

            assertEquals(before, announcementViewModel.state.value)
            assertTrue(announcementViewModel.events.replayCache.isEmpty())
        }

    @Test
    fun `WHEN NavigateBack pop is dispatched THEN navigator returns to the message list`() =
        runTest {
            val navigator =
                fakeNavigator(
                    MessagesUserSelectRoute,
                    IndividualMessageListRoute(planterInfoId = 1L),
                    AnnouncementRoute(messageId = messageId),
                )

            announcementViewModel.handleAction(AnnouncementAction.NavigateBack)
            assertEquals(PopBackStackEvent, announcementViewModel.events.first().getContentIfNotConsumed())
            navigator.throttledPopBackStack()

            assertEquals(
                listOf(MessagesUserSelectRoute, IndividualMessageListRoute(planterInfoId = 1L)),
                navigator.backStack,
            )
        }
}