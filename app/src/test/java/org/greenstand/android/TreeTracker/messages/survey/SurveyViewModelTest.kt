package org.greenstand.android.TreeTracker.messages.survey

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.messages.individualmeassagelist.IndividualMessageListViewModel
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.models.messages.MessagesRepo
import org.greenstand.android.TreeTracker.utils.FakeFileGenerator
import org.junit.Before
import org.junit.Rule

@ExperimentalCoroutinesApi
class SurveyViewModelTest {
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()
    private val messageId = "message"
    private val messagesRepo = mockk<MessagesRepo>(relaxed = true)
    private val userRepo = mockk<UserRepo>(relaxed = true)
    private lateinit var testSubject: SurveyViewModel

    @Before
    fun setup(){
        coEvery { messagesRepo.getSurveyMessage(any())} returns FakeFileGenerator.fakeSurveyMessage
        coEvery { userRepo.getUserWithWallet(any()) } returns FakeFileGenerator.fakeUsers.first()
        testSubject = SurveyViewModel(messageId, messagesRepo, userRepo)
    }

}