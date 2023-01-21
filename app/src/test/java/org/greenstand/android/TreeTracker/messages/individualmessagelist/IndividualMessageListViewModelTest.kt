package org.greenstand.android.TreeTracker.messages.individualmessagelist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.messages.individualmeassagelist.IndividualMessageListViewModel
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.models.messages.MessagesRepo
import org.greenstand.android.TreeTracker.utils.FakeFileGenerator
import org.greenstand.android.TreeTracker.utils.getOrAwaitValueTest
import org.junit.Assert.assertEquals
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
    private lateinit var individualMessageListViewModel: IndividualMessageListViewModel

    @Before
    fun setup(){
        coEvery { userRepo.getUser(any()) } returns FakeFileGenerator.fakeUsers.first()
        coEvery { messagesRepo.getMessageFlow(any()) } returns flowOf(FakeFileGenerator.messages)
        individualMessageListViewModel = IndividualMessageListViewModel(userId, userRepo, messagesRepo)
    }

    @Test
    @Throws(Exception::class)
    fun `verify user repo gets the correct user`() = runBlocking {
       coVerify { userRepo.getUser(userId) }
    }

    @Test
    @Throws(Exception::class)
    fun `verify message repo gets the correct message flow`() = runBlocking {
        val currentUser = userRepo.getUser(userId)
        val wallet = currentUser!!.wallet
        coVerify { messagesRepo.getMessageFlow(wallet) }
    }

    @Test
    @Throws(Exception::class)
    fun `WHEN message flow is triggered THEN message state updates with correct message`()= runBlocking {
        val result = individualMessageListViewModel.state.getOrAwaitValueTest().messages.first()
        assertEquals(result, FakeFileGenerator.messages.first())
    }
    @Test
    @Throws(Exception::class)
    fun `WHEN message flow is triggered THEN current user state updates with correct User`()= runBlocking {
        val result = individualMessageListViewModel.state.getOrAwaitValueTest().currentUser
        assertEquals(result, FakeFileGenerator.fakeUsers.first())
    }

    @Test
    @Throws(Exception::class)
    fun `WHEN selected message is triggered THEN state updates with correct message`()= runBlocking {
        individualMessageListViewModel.selectMessage(FakeFileGenerator.fakeSurveyMessage)
        val result = individualMessageListViewModel.state.getOrAwaitValueTest().selectedMessage
        assertEquals(result, FakeFileGenerator.messages[2])
    }
}