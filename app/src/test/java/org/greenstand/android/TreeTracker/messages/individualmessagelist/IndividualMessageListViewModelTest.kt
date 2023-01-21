package org.greenstand.android.TreeTracker.messages.individualmessagelist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.messages.individualmeassagelist.IndividualMessageListViewModel
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.models.messages.MessagesRepo
import org.greenstand.android.TreeTracker.utils.FakeFileGenerator
import org.greenstand.android.TreeTracker.utils.getOrAwaitValueTest
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class IndividualMessageListViewModelTest {
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()
    private val userId = -33234254353456
    private val userRepo = mockk<UserRepo>(relaxed = true)
    private val messagesRepo = mockk<MessagesRepo>(relaxed = true)
    private lateinit var individualMessageListViewModel: IndividualMessageListViewModel

    @Before
    fun setup(){
        coEvery { userRepo.getUser(any()) } returns FakeFileGenerator.emptyUser
        coEvery { messagesRepo.getMessageFlow(any()) } returns flow {
            listOf(FakeFileGenerator.fakeAnnouncementMessage)
        }
        individualMessageListViewModel = IndividualMessageListViewModel(userId, userRepo, messagesRepo)
    }

    @Test
    fun `verify user repo gets the correct user`() = runBlocking {
        coVerify { userRepo.getUser(userId) }
    }

    @Test
    fun `verify message repo gets the correct message flow`() = runBlocking {
        val currentUser = userRepo.getUser(userId)
        val id = currentUser!!.wallet
        coVerify { messagesRepo.getMessageFlow(id) }
    }

    @Test
    fun `WHEN selected message, state gets updated`() = runBlocking {
        val message = FakeFileGenerator.fakeAnnouncementMessage
    }

}