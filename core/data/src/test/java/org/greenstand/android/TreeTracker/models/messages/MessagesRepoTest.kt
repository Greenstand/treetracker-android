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
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.models.messages.database.MessagesDAO
import org.greenstand.android.TreeTracker.models.messages.database.entities.MessageEntity
import org.greenstand.android.TreeTracker.models.messages.network.MessagesApiService
import org.greenstand.android.TreeTracker.models.messages.network.responses.LinksResponse
import org.greenstand.android.TreeTracker.models.messages.network.responses.MessageResponse
import org.greenstand.android.TreeTracker.models.messages.network.responses.MessageType
import org.greenstand.android.TreeTracker.models.messages.network.responses.MessagesResponse
import org.greenstand.android.TreeTracker.models.messages.network.responses.QueryResponse
import org.greenstand.android.TreeTracker.models.user.User
import org.greenstand.android.TreeTracker.utilities.TimeProvider
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule

@OptIn(ExperimentalCoroutinesApi::class)
class MessagesRepoTest {
    @get:Rule
    val rule: TestRule = InstantTaskExecutorRule()

    // Set the main coroutines dispatcher for unit testing.
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private lateinit var apiService: MessagesApiService
    private lateinit var userRepo: UserRepo
    private lateinit var timeProvider: TimeProvider
    private lateinit var messagesDAO: MessagesDAO
    private lateinit var messageUploader: MessageUploader

    // system under test
    private lateinit var messagesRepo: MessagesRepo

    @Before
    fun setUp() {
        apiService = mockk()
        userRepo = mockk()
        timeProvider = mockk()
        messagesDAO = mockk()
        messageUploader = mockk()

        messagesRepo =
            MessagesRepo(
                apiService,
                userRepo,
                timeProvider,
                messagesDAO,
                messageUploader,
            )
    }

    private val userList: List<User> =
        List(10) {
            User(
                it.toLong(),
                it.toString(),
                it,
                it.toString(),
                it.toString(),
                it.toString(),
                false,
                it % 2 == 0,
            )
        }

    private val lastTimeMessageSynced = "123456789"

    @Test
    fun `sync messages method fetches messages from api and saves them in database for every page and wallet correctly`() =
        runTest {
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
                    } returns
                        MessagesResponse(
                            getMessageResponseList(limit, offset, i),
                            LinksResponse(null, null),
                            QueryResponse(total, i.toString(), limit, offset),
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
                                getMessageEntityFromResponse(it, i.toString()),
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
        action: (offset: Int) -> Unit,
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
        userIndex: Int,
    ): List<MessageResponse> =
        List<MessageResponse>(10) {
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
                survey = null,
            )
        }

    private fun getMessageEntityFromResponse(
        messageResponse: MessageResponse,
        wallet: String,
    ): MessageEntity =
        MessageEntity(
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
            isSurveyComplete = messageResponse.survey?.let { false },
        )

    @Test
    fun `markMessageAsRead delegates to DAO`() =
        runTest {
            coEvery { messagesDAO.markMessageAsRead(any()) } returns Unit

            messagesRepo.markMessageAsRead("msg-123")

            coVerify { messagesDAO.markMessageAsRead(listOf("msg-123")) }
        }

    @Test
    fun `saveMessage creates entity with correct fields`() =
        runTest {
            val fakeTime = Instant.fromEpochMilliseconds(1000000L)
            every { timeProvider.currentTime() } returns fakeTime
            coEvery { messagesDAO.insertMessage(any()) } returns 0L

            messagesRepo.saveMessage(
                wallet = "test-wallet",
                to = "admin",
                body = "Hello admin",
            )

            coVerify {
                messagesDAO.insertMessage(
                    match { entity ->
                        entity.wallet == "test-wallet" &&
                            entity.to == "admin" &&
                            entity.body == "Hello admin" &&
                            entity.from == "test-wallet" &&
                            entity.type == MessageType.MESSAGE &&
                            entity.shouldUpload &&
                            entity.isRead &&
                            entity.subject == null &&
                            entity.parentMessageId == null
                    },
                )
            }
        }

    @Test
    fun `checkForUnreadMessages returns true when count is at least 1`() =
        runTest {
            coEvery { messagesDAO.getUnreadMessagesCount() } returns 5

            val result = messagesRepo.checkForUnreadMessages()

            kotlin.test.assertTrue(result)
        }

    @Test
    fun `checkForUnreadMessages returns false when count is 0`() =
        runTest {
            coEvery { messagesDAO.getUnreadMessagesCount() } returns 0

            val result = messagesRepo.checkForUnreadMessages()

            kotlin.test.assertFalse(result)
        }
}