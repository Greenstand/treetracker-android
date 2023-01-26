package org.greenstand.android.TreeTracker.messages.directmessages

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.messages.ChatState
import org.greenstand.android.TreeTracker.messages.ChatViewModel
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.models.messages.MessagesRepo
import org.greenstand.android.TreeTracker.utils.FakeFileGenerator
import org.greenstand.android.TreeTracker.utils.getOrAwaitValueTest
import org.junit.Assert
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.androidx.viewmodel.scope.emptyState
import org.mockito.Mockito
import org.mockito.Mockito.*

@ExperimentalCoroutinesApi
class ChatViewModelTest {
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private val userId =2L
    private val otherChatIdentifier = "Mary"
    private val userRepo = mockk<UserRepo>(relaxed = true)
    private val messagesRepo = mockk<MessagesRepo>(relaxed = true)
    private lateinit var testSubject: ChatViewModel

    @Before
    fun setup() {
        coEvery { userRepo.getUser(any()) } returns FakeFileGenerator.fakeUsers.first()
        coEvery { messagesRepo.getDirectMessages(any(),any()) } returns flowOf(FakeFileGenerator.fakeDirectMessageList)
        testSubject = ChatViewModel(userId, otherChatIdentifier, userRepo, messagesRepo)
    }
    @Test
    fun `WHEN draft text is empty,returns empty string, THEN when draft text updates, returns correct data`()= runBlocking {
        assertEquals(testSubject.state.getOrAwaitValueTest().draftText, "")
        testSubject.updateDraftText("random")
        val result = testSubject.state.getOrAwaitValueTest().draftText
        assertEquals(result, "random")
    }

    @Test
    fun `Verify message repo saves message`() = runBlocking {
        testSubject.updateDraftText("Draft")
        testSubject.sendMessage()
        coVerify { messagesRepo.saveMessage("some random text", "Mary","Draft") }
    }
    @Test
    fun `WHEN current user sends message THEN message is sent AND draft message is cleared`()= runBlocking {
        testSubject.updateDraftText("Hello, World")
        testSubject.sendMessage()
        val result = testSubject.state.getOrAwaitValueTest().draftText
        assertEquals(result, "")
    }

    @Test
    fun `Check chat author, Assert True if its First Message by Author`()= runBlocking {
        assertTrue(testSubject.checkChatAuthor(0, true))
    }
    @Test
    fun `Check chat author, Assert False if its not First Message by Author`()= runBlocking {
        assertFalse(testSubject.checkChatAuthor(1, true))
    }

    @Test
    fun `Check Is Other User, Assert True for messages from other user`()= runBlocking {
        val result = testSubject.checkIsOtherUser(2)
        assertTrue(result)
    }

    @Test
    fun `Check Is Other User, Assert False for messages from current user`()= runBlocking {
        val result = testSubject.checkIsOtherUser(1)
        assertFalse(result)
    }
}