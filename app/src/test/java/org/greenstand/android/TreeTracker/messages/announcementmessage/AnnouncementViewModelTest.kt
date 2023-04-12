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
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.models.messages.MessagesRepo
import org.greenstand.android.TreeTracker.utils.FakeFileGenerator.fakeAnnouncementMessage
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class AnnouncementViewModelTest{
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private val messageId = ""
    private val messagesRepo = mockk<MessagesRepo>(relaxed = true)
    private lateinit var announcementViewModel: AnnouncementViewModel

    @Before
    fun setup(){
        coEvery { messagesRepo.getAnnouncementMessages(any()) } returns fakeAnnouncementMessage
        announcementViewModel = AnnouncementViewModel(messageId, messagesRepo)
    }

    @Test
    fun `verify messages repo gets the correct announcement message `()= runBlocking {
        coVerify { messagesRepo.getAnnouncementMessages(messageId) }
    }

    @Test
    fun `WHEN get announcement is triggered THEN current URL changes`()= runBlocking {
        announcementViewModel.state.test {
            assertEquals(awaitItem().currentUrl, fakeAnnouncementMessage.videoLink)
        }
    }
    @Test
    fun `WHEN get announcement is triggered THEN current title changes`()= runBlocking {
        announcementViewModel.state.test {
            assertEquals(awaitItem().currentTitle, fakeAnnouncementMessage.subject)
        }
    }
    @Test
    fun `WHEN get announcement is triggered THEN from state changes`()= runBlocking {
        announcementViewModel.state.test {
            assertEquals(awaitItem().from, fakeAnnouncementMessage.from)
        }
    }

    @Test
    fun `WHEN get announcement is triggered THEN current body changes`()= runBlocking {
        announcementViewModel.state.test {
            assertEquals(awaitItem().currentBody, fakeAnnouncementMessage.body)
        }
    }

    @Test
    fun `verify message repo marks message as read`()= runBlocking {
        coVerify { messagesRepo.markMessageAsRead(messageId) }
    }
}
