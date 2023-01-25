package org.greenstand.android.TreeTracker.messages.directmessages

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.messages.ChatViewModel
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.models.messages.MessagesRepo
import org.junit.Before
import org.junit.Rule

@ExperimentalCoroutinesApi
class ChatViewModelTest {
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private val userId =2L
    private val otherChatIdentifier = ""
    private val userRepo = mockk<UserRepo>(relaxed = true)
    private val messagesRepo = mockk<MessagesRepo>(relaxed = true)
    private lateinit var testSubject: ChatViewModel

    @Before
    fun setup() {
        testSubject = ChatViewModel(userId, otherChatIdentifier, userRepo, messagesRepo)
    }
}