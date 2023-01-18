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
        coEvery { messagesRepo.getAnnouncementMessages(messageId) } returns fakeAnnouncementMessage
        announcementViewModel = AnnouncementViewModel(messageId, messagesRepo)
    }

    @Test
    fun `verify messages repo gets the correct announcement message `()= runBlocking {
        coEvery { messagesRepo.getAnnouncementMessages(messageId) } returns fakeAnnouncementMessage
        coVerify { messagesRepo.getAnnouncementMessages(messageId) }
    }

    @Test
    fun `WHEN get announcement is triggered THEN current URL changes`()= runBlocking {
        coEvery { messagesRepo.getAnnouncementMessages(messageId) } returns fakeAnnouncementMessage
        announcementViewModel.state.test {
            assertEquals(awaitItem().currentUrl, fakeAnnouncementMessage.videoLink)
        }
    }
    @Test
    fun `WHEN get announcement is triggered THEN current title changes`()= runBlocking {
        coEvery { messagesRepo.getAnnouncementMessages(messageId) } returns fakeAnnouncementMessage
        announcementViewModel.state.test {
            assertEquals(awaitItem().currentTitle, fakeAnnouncementMessage.subject)
        }
    }
    @Test
    fun `WHEN get announcement is triggered THEN from state changes`()= runBlocking {
        coEvery { messagesRepo.getAnnouncementMessages(messageId) } returns fakeAnnouncementMessage
        announcementViewModel.state.test {
            assertEquals(awaitItem().from, fakeAnnouncementMessage.from)
        }
    }

    @Test
    fun `WHEN get announcement is triggered THEN current body changes`()= runBlocking {
        coEvery { messagesRepo.getAnnouncementMessages(messageId) } returns fakeAnnouncementMessage
        announcementViewModel.state.test {
            assertEquals(awaitItem().currentBody, fakeAnnouncementMessage.body)
        }
    }

    @Test
    fun `verify message repo marks message as read`()= runBlocking {
        coEvery { messagesRepo.markMessageAsRead(messageId) }
        coVerify { messagesRepo.markMessageAsRead(messageId) }
    }
}
