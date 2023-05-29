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
package org.greenstand.android.TreeTracker.models.messages

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.*
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.models.messages.database.MessagesDAO
import org.greenstand.android.TreeTracker.models.messages.database.entities.MessageEntity
import org.greenstand.android.TreeTracker.models.messages.network.MessagesApiService
import org.greenstand.android.TreeTracker.models.messages.network.responses.*
import org.greenstand.android.TreeTracker.models.user.User
import org.greenstand.android.TreeTracker.utilities.TimeProvider
import org.junit.*
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
class MessagesRepoTest {

    @get:Rule
    val rule: TestRule = InstantTaskExecutorRule()

    // Set the main coroutines dispatcher for unit testing.
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private lateinit var testDispatcher: TestCoroutineDispatcher
    private lateinit var apiService: MessagesApiService
    private lateinit var userRepo: UserRepo
    private lateinit var timeProvider: TimeProvider
    private lateinit var messagesDAO: MessagesDAO
    private lateinit var messageUploader: MessageUploader

    // system under test
    private lateinit var messagesRepo: MessagesRepo

    @Before
    fun setUp() {
        testDispatcher = TestCoroutineDispatcher()
        Dispatchers.setMain(testDispatcher)

        apiService = mockk()
        userRepo = mockk()
        timeProvider = mockk()
        messagesDAO = mockk()
        messageUploader = mockk()

        messagesRepo = MessagesRepo(
            apiService,
            userRepo,
            timeProvider,
            messagesDAO,
            messageUploader
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val userList: List<User> = List(10) {
        User(
            it.toLong(),
            it.toString(),
            it,
            it.toString(),
            it.toString(),
            it.toString(),
            false,
            it % 2 == 0
        )
    }

    private val lastTimeMessageSynced = "123456789"

    @Test
    fun `sync messages method fetches messages from api and saves them in database for every page and wallet correctly`() =
        runBlocking {

            // mock
            coEvery {
                userRepo.getUserList()
            } returns userList

            coEvery {
                messagesDAO.insertMessage(any())
            } returns 0L

            coEvery {
                messageUploader.uploadMessages()
            } returns Unit

            coEvery {
                messagesDAO.getLatestSyncTimeForWallet(any())
            } returns lastTimeMessageSynced

            for (i in userList.indices) {
                val limit = 100
                val total = (i + 1) * 100 + 5

                iterateForEveryPage(0, limit, total) { offset ->
                    coEvery {
                        apiService.getMessages(
                            wallet = i.toString(),
                            lastSyncTime = lastTimeMessageSynced,
                            offset = offset,
                            limit = limit,
                        )
                    } returns MessagesResponse(
                        getMessageResponseList(limit, offset, i),
                        LinksResponse(null, null),
                        QueryResponse(total, i.toString(), limit, offset)
                    )
                }
            }

            // perform
            messagesRepo.syncMessages()

            // assert
            for (i in userList.indices) {
                val limit = 100
                val total = (i + 1) * 100 + 5

                iterateForEveryPage(0, limit, total) { offset ->
                    coVerify(exactly = 1) {
                        apiService.getMessages(
                            wallet = i.toString(),
                            lastSyncTime = lastTimeMessageSynced,
                            offset = offset,
                            limit = limit,
                        )

                        getMessageResponseList(limit, offset, i).forEach {
                            messagesDAO.insertMessage(
                                getMessageEntityFromResponse(it, i.toString())
                            )
                        }
                    }
                }
            }
        }

    private fun iterateForEveryPage(
        initialOffset: Int = 0,
        limit: Int = 100,
        total: Int,
        action: (offset: Int) -> Unit
    ) {
        var offset = initialOffset

        do {
            action.invoke(offset)
            // prepare for next iteration
            offset += limit
        } while (offset <= total)
    }

    private fun getMessageResponseList(
        limit: Int,
        offset: Int,
        userIndex: Int
    ): List<MessageResponse> {
        return List<MessageResponse>(10) {
            MessageResponse(
                id = ((limit + offset) + it + userIndex).toString(),
                type = MessageType.MESSAGE,
                from = "from",
                to = "to",
                composedAt = "some time",
                subject = null,
                body = null,
                parentMessageId = null,
                videoLink = null,
                surveyResponse = null,
                survey = null
            )
        }
    }

    private fun getMessageEntityFromResponse(
        messageResponse: MessageResponse,
        wallet: String
    ): MessageEntity {
        return MessageEntity(
            id = messageResponse.id,
            wallet = wallet,
            type = messageResponse.type,
            from = messageResponse.from,
            to = messageResponse.to,
            subject = messageResponse.subject,
            body = messageResponse.body,
            composedAt = messageResponse.composedAt,
            parentMessageId = messageResponse.parentMessageId,
            videoLink = messageResponse.videoLink,
            surveyResponse = messageResponse.surveyResponse,
            shouldUpload = false,
            bundleId = null,
            isRead = false,
            surveyId = messageResponse.survey?.id,
            isSurveyComplete = messageResponse.survey?.let { false }
        )
    }
}