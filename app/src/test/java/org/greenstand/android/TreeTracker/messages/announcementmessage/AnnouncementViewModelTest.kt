package org.greenstand.android.TreeTracker.messages.announcementmessage

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import org.greenstand.android.TreeTracker.utils.getOrAwaitValueTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.models.messages.AnnouncementMessage
import org.greenstand.android.TreeTracker.models.messages.MessagesRepo
import org.greenstand.android.TreeTracker.utils.FakeFileGenerator
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class AnnouncementViewModelTest {
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()


    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private val messagesRepo = mockk<MessagesRepo>(relaxed = true)
    private val messageId = ""
    private lateinit var announcementViewModel: AnnouncementViewModel

    @Before
    fun setup(){
        announcementViewModel = AnnouncementViewModel(messageId,messagesRepo)
    }

    @Test
    fun `verify announcement message called from repository`() = runBlockingTest {
        coEvery { messagesRepo.getAnnouncementMessages(messageId) } returns FakeFileGenerator.fakeAnnouncementMessage
        coVerify { messagesRepo.getAnnouncementMessages(messageId) }
    }
}
