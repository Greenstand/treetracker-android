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
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.models.messages.database.MessagesDAO
import org.greenstand.android.TreeTracker.models.messages.database.entities.MessageEntity
import org.greenstand.android.TreeTracker.models.messages.database.entities.QuestionEntity
import org.greenstand.android.TreeTracker.models.messages.database.entities.SurveyEntity
import org.greenstand.android.TreeTracker.models.messages.network.MessagesApiService
import org.greenstand.android.TreeTracker.models.messages.network.responses.LinksResponse
import org.greenstand.android.TreeTracker.models.messages.network.responses.MessageResponse
import org.greenstand.android.TreeTracker.models.messages.network.responses.MessageType
import org.greenstand.android.TreeTracker.models.messages.network.responses.MessagesResponse
import org.greenstand.android.TreeTracker.models.messages.network.responses.QueryResponse
import org.greenstand.android.TreeTracker.models.messages.network.responses.QuestionResponse
import org.greenstand.android.TreeTracker.models.messages.network.responses.SurveyResponse
import org.greenstand.android.TreeTracker.models.user.User
import org.greenstand.android.TreeTracker.utilities.Constants
import org.greenstand.android.TreeTracker.utilities.TimeProvider
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import timber.log.Timber
import java.util.Collections
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

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
        Timber.plant(errorRecorder)
    }

    @After
    fun tearDown() {
        Timber.uproot(errorRecorder)
    }

    private val errorRecorder = ErrorRecordingTree()

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

    @Test
    fun `markMessagesAsRead delegates all ids to DAO`() =
        runTest {
            coEvery { messagesDAO.markMessageAsRead(any()) } returns Unit

            messagesRepo.markMessagesAsRead(listOf("msg-1", "msg-2"))

            coVerify { messagesDAO.markMessageAsRead(listOf("msg-1", "msg-2")) }
        }

    @Test
    fun `saveSurveyAnswers stores a survey response to the survey author and completes the survey`() =
        runTest {
            val fakeTime = Instant.fromEpochMilliseconds(2000000L)
            every { timeProvider.currentTime() } returns fakeTime
            coEvery { messagesDAO.getMessage("survey-msg") } returns
                entity(id = "survey-msg", type = MessageType.SURVEY, from = "admin", to = "planter-wallet", surveyId = "survey-1")
            val responseSlot = slot<MessageEntity>()
            coEvery { messagesDAO.insertMessage(capture(responseSlot)) } returns 1L
            coEvery { messagesDAO.markSurveyMessageComplete(any()) } returns Unit

            messagesRepo.saveSurveyAnswers("survey-msg", listOf("Yes", "No"))

            val response = responseSlot.captured
            assertEquals(MessageType.SURVEY_RESPONSE, response.type)
            assertEquals("planter-wallet", response.wallet)
            assertEquals("planter-wallet", response.from)
            assertEquals("admin", response.to)
            assertEquals(listOf("Yes", "No"), response.surveyResponse)
            assertEquals("survey-1", response.surveyId)
            assertEquals(fakeTime.toString(), response.composedAt)
            assertEquals(true, response.shouldUpload)
            assertEquals(true, response.isRead)
            assertEquals(true, response.isSurveyComplete)
            coVerifyOrder {
                messagesDAO.insertMessage(any())
                messagesDAO.markSurveyMessageComplete("survey-msg")
            }
        }

    @Test
    fun `saveSurveyAnswers fails when the survey message does not exist`() =
        runTest {
            coEvery { messagesDAO.getMessage("missing") } returns null

            assertFailsWith<NullPointerException> {
                messagesRepo.saveSurveyAnswers("missing", listOf("Yes"))
            }
        }

    @Test
    fun `getMessageFlow converts every entity into its message type`() =
        runTest {
            every { messagesDAO.getMessagesForWalletFlow("wallet") } returns
                flowOf(
                    listOf(
                        entity(id = "direct", type = MessageType.MESSAGE),
                        entity(id = "announce", type = MessageType.ANNOUNCE, subject = "News", videoLink = "https://video"),
                        entity(id = "survey", type = MessageType.SURVEY, surveyId = "survey-1"),
                    ),
                )
            coEvery { messagesDAO.getSurvey(null) } returns null
            coEvery { messagesDAO.getSurvey("survey-1") } returns SurveyEntity(id = "survey-1", title = "Survey title")
            coEvery { messagesDAO.getQuestionsForSurvey("survey-1") } returns
                listOf(QuestionEntity(surveyId = "survey-1", prompt = "Healthy?", choices = listOf("Yes", "No")))

            val messages = messagesRepo.getMessageFlow("wallet").first()

            assertEquals(3, messages.size)
            assertTrue(messages[0] is DirectMessage)
            val announcement = messages[1] as AnnouncementMessage
            assertEquals("News", announcement.subject)
            assertEquals("https://video", announcement.videoLink)
            val survey = messages[2] as SurveyMessage
            assertEquals("Survey title", survey.title)
            assertEquals(listOf(Question(prompt = "Healthy?", choices = listOf("Yes", "No"))), survey.questions)
            assertEquals(false, survey.isComplete)
        }

    @Test
    fun `getDirectMessages keeps only the conversation with the other user newest first`() =
        runTest {
            every { messagesDAO.getDirectMessagesForWallet("me") } returns
                flowOf(
                    listOf(
                        entity(id = "sent", type = MessageType.MESSAGE, from = "me", to = "admin", composedAt = "2023-01-01T00:00:00Z"),
                        entity(id = "received", type = MessageType.MESSAGE, from = "admin", to = "me", composedAt = "2023-01-03T00:00:00Z"),
                        entity(id = "other-sender", type = MessageType.MESSAGE, from = "someone", to = "me", composedAt = "2023-01-02T00:00:00Z"),
                        entity(id = "other-recipient", type = MessageType.MESSAGE, from = "me", to = "someone", composedAt = "2023-01-04T00:00:00Z"),
                        entity(id = "announce", type = MessageType.ANNOUNCE, from = "admin", to = "me", composedAt = "2023-01-05T00:00:00Z"),
                    ),
                )
            coEvery { messagesDAO.getSurvey(null) } returns null

            val messages = messagesRepo.getDirectMessages(wallet = "me", otherChatIdentifier = "admin").first()

            assertEquals(listOf("received", "sent"), messages.map { it.id })
        }

    @Test
    fun `getAnnouncementMessages converts the stored announcement`() =
        runTest {
            coEvery { messagesDAO.getMessage("announce") } returns
                entity(id = "announce", type = MessageType.ANNOUNCE, subject = null, body = null)
            coEvery { messagesDAO.getSurvey(null) } returns null

            val announcement = messagesRepo.getAnnouncementMessages("announce")

            assertEquals("announce", announcement.id)
            assertEquals("", announcement.subject)
            assertEquals("", announcement.body)
        }

    @Test
    fun `getSurveyMessage converts the stored survey with its questions`() =
        runTest {
            coEvery { messagesDAO.getMessage("survey") } returns
                entity(id = "survey", type = MessageType.SURVEY, surveyId = "survey-1").copy(isSurveyComplete = true)
            coEvery { messagesDAO.getSurvey("survey-1") } returns SurveyEntity(id = "survey-1", title = "Survey title")
            coEvery { messagesDAO.getQuestionsForSurvey("survey-1") } returns
                listOf(
                    QuestionEntity(surveyId = "survey-1", prompt = "Q1", choices = listOf("A", "B")),
                    QuestionEntity(surveyId = "survey-1", prompt = "Q2", choices = listOf("C")),
                )

            val survey = messagesRepo.getSurveyMessage("survey")

            assertEquals("survey-1", survey.surveyId)
            assertEquals(listOf("Q1", "Q2"), survey.questions.map { it.prompt })
            assertEquals(true, survey.isComplete)
        }

    @Test
    fun `sync messages skips a wallet that has never had messages and continues with the next`() =
        runTest {
            stubSync(wallets = listOf("new-wallet", "old-wallet"))
            coEvery {
                apiService.getMessages(wallet = "new-wallet", lastSyncTime = any(), offset = any(), limit = any())
            } throws IllegalStateException(Constants.LOCAL_MSG_ERROR_HTTP404)
            coEvery {
                apiService.getMessages(wallet = "old-wallet", lastSyncTime = any(), offset = 0, limit = 100)
            } returns response(wallet = "old-wallet", total = 1, messages = listOf(messageResponse(id = "m1")))

            messagesRepo.syncMessages()

            coVerify { messagesDAO.insertMessage(match { it.id == "m1" && it.wallet == "old-wallet" }) }
            coVerify(exactly = 1) { messageUploader.uploadMessages() }
            assertTrue(errorRecorder.errors.isEmpty())
        }

    @Test
    fun `sync messages logs other errors and continues with the next wallet`() =
        runTest {
            stubSync(wallets = listOf("broken-wallet", "old-wallet"))
            coEvery {
                apiService.getMessages(wallet = "broken-wallet", lastSyncTime = any(), offset = any(), limit = any())
            } throws IllegalStateException("HTTP 500 Server Error")
            coEvery {
                apiService.getMessages(wallet = "old-wallet", lastSyncTime = any(), offset = 0, limit = 100)
            } returns response(wallet = "old-wallet", total = 1, messages = listOf(messageResponse(id = "m1")))

            messagesRepo.syncMessages()

            coVerify { messagesDAO.insertMessage(match { it.id == "m1" }) }
            coVerify(exactly = 1) { messageUploader.uploadMessages() }
            assertEquals<List<String?>>(listOf("HTTP 500 Server Error"), errorRecorder.errors.toList())
        }

    @Test
    fun `sync messages rethrows cancellation and does not upload`() =
        runTest {
            stubSync(wallets = listOf("wallet"))
            coEvery {
                apiService.getMessages(wallet = "wallet", lastSyncTime = any(), offset = any(), limit = any())
            } throws CancellationException("stopped")

            assertFailsWith<CancellationException> {
                messagesRepo.syncMessages()
            }

            coVerify(exactly = 0) { messageUploader.uploadMessages() }
        }

    @Test
    fun `sync messages with no users only uploads pending messages`() =
        runTest {
            stubSync(wallets = emptyList())

            messagesRepo.syncMessages()

            coVerify(exactly = 0) { apiService.getMessages(any(), any(), any(), any(), any()) }
            coVerify(exactly = 1) { messageUploader.uploadMessages() }
        }

    @Test
    fun `sync messages requests from the epoch when the wallet was never synced`() =
        runTest {
            stubSync(wallets = listOf("wallet"))
            coEvery { messagesDAO.getLatestSyncTimeForWallet("wallet") } returns null
            coEvery {
                apiService.getMessages(wallet = "wallet", lastSyncTime = "1970-01-01T00:00:00Z", offset = 0, limit = 100)
            } returns response(wallet = "wallet", total = 0, messages = emptyList())

            messagesRepo.syncMessages()

            coVerify(exactly = 1) {
                apiService.getMessages(wallet = "wallet", lastSyncTime = "1970-01-01T00:00:00Z", offset = 0, limit = 100)
            }
        }

    @Test
    fun `sync messages makes a single request and saves nothing when the server has no messages`() =
        runTest {
            stubSync(wallets = listOf("wallet"))
            coEvery {
                apiService.getMessages(wallet = "wallet", lastSyncTime = any(), offset = any(), limit = any())
            } returns response(wallet = "wallet", total = 0, messages = listOf(messageResponse(id = "ignored")))

            messagesRepo.syncMessages()

            coVerify(exactly = 1) { apiService.getMessages(wallet = "wallet", lastSyncTime = any(), offset = any(), limit = any()) }
            coVerify(exactly = 0) { messagesDAO.insertMessage(any()) }
        }

    @Test
    fun `sync messages fetches every remaining page`() =
        runTest {
            stubSync(wallets = listOf("wallet"))
            coEvery {
                apiService.getMessages(wallet = "wallet", lastSyncTime = any(), offset = any(), limit = any())
            } answers {
                response(wallet = "wallet", total = 250, messages = emptyList(), offset = arg(3))
            }

            messagesRepo.syncMessages()

            listOf(0, 100, 200).forEach { offset ->
                coVerify(exactly = 1) {
                    apiService.getMessages(wallet = "wallet", lastSyncTime = any(), offset = offset, limit = 100)
                }
            }
            coVerify(exactly = 3) { apiService.getMessages(wallet = "wallet", lastSyncTime = any(), offset = any(), limit = any()) }
        }

    @Test
    fun `sync messages saves server messages as unread and not to be uploaded`() =
        runTest {
            stubSync(wallets = listOf("wallet"))
            coEvery {
                apiService.getMessages(wallet = "wallet", lastSyncTime = any(), offset = any(), limit = any())
            } returns response(wallet = "wallet", total = 1, messages = listOf(messageResponse(id = "m1")))
            val savedSlot = slot<MessageEntity>()
            coEvery { messagesDAO.insertMessage(capture(savedSlot)) } returns 1L

            messagesRepo.syncMessages()

            val saved = savedSlot.captured
            assertEquals("wallet", saved.wallet)
            assertEquals(false, saved.isRead)
            assertEquals(false, saved.shouldUpload)
            assertEquals(null, saved.surveyId)
            assertEquals(null, saved.isSurveyComplete)
        }

    @Test
    fun `sync messages ignores survey responses coming from the server`() =
        runTest {
            stubSync(wallets = listOf("wallet"))
            coEvery {
                apiService.getMessages(wallet = "wallet", lastSyncTime = any(), offset = any(), limit = any())
            } returns
                response(
                    wallet = "wallet",
                    total = 1,
                    messages = listOf(messageResponse(id = "response", type = MessageType.SURVEY_RESPONSE)),
                )

            messagesRepo.syncMessages()

            coVerify(exactly = 0) { messagesDAO.insertMessage(any()) }
        }

    @Test
    fun `sync messages stores a new survey with its questions`() =
        runTest {
            stubSync(wallets = listOf("wallet"))
            val survey =
                SurveyResponse(
                    id = "survey-1",
                    title = "Tree health",
                    questions =
                        listOf(
                            QuestionResponse(prompt = "Q1", choices = listOf("A", "B")),
                            QuestionResponse(prompt = "Q2", choices = listOf("C")),
                        ),
                )
            coEvery {
                apiService.getMessages(wallet = "wallet", lastSyncTime = any(), offset = any(), limit = any())
            } returns
                response(
                    wallet = "wallet",
                    total = 1,
                    messages = listOf(messageResponse(id = "survey-msg", type = MessageType.SURVEY, survey = survey)),
                )
            coEvery { messagesDAO.getSurvey("survey-1") } returns null
            coEvery { messagesDAO.insertSurvey(any()) } returns 1L
            coEvery { messagesDAO.insertQuestion(any()) } returns 1L

            messagesRepo.syncMessages()

            coVerify { messagesDAO.insertMessage(match { it.surveyId == "survey-1" && it.isSurveyComplete == false }) }
            coVerify { messagesDAO.insertSurvey(match { it.id == "survey-1" && it.title == "Tree health" }) }
            coVerify { messagesDAO.insertQuestion(match { it.surveyId == "survey-1" && it.prompt == "Q1" && it.choices == listOf("A", "B") }) }
            coVerify { messagesDAO.insertQuestion(match { it.surveyId == "survey-1" && it.prompt == "Q2" && it.choices == listOf("C") }) }
        }

    @Test
    fun `sync messages reuses a survey that is already stored`() =
        runTest {
            stubSync(wallets = listOf("wallet"))
            val survey = SurveyResponse(id = "survey-1", title = "Tree health", questions = emptyList())
            coEvery {
                apiService.getMessages(wallet = "wallet", lastSyncTime = any(), offset = any(), limit = any())
            } returns
                response(
                    wallet = "wallet",
                    total = 1,
                    messages = listOf(messageResponse(id = "survey-msg", type = MessageType.SURVEY, survey = survey)),
                )
            coEvery { messagesDAO.getSurvey("survey-1") } returns SurveyEntity(id = "survey-1", title = "Tree health")

            messagesRepo.syncMessages()

            coVerify(exactly = 1) { messagesDAO.insertMessage(any()) }
            coVerify(exactly = 0) { messagesDAO.insertSurvey(any()) }
            coVerify(exactly = 0) { messagesDAO.insertQuestion(any()) }
        }

    @Test
    fun `sync messages propagates message upload failures`() =
        runTest {
            stubSync(wallets = emptyList())
            coEvery { messageUploader.uploadMessages() } throws IllegalStateException("upload failed")

            assertFailsWith<IllegalStateException> {
                messagesRepo.syncMessages()
            }
        }

    private fun stubSync(wallets: List<String>) {
        coEvery { userRepo.getUserList() } returns
            wallets.mapIndexed { index, wallet ->
                User(index.toLong(), wallet, 0, "First", "Last", "/photo.jpg", false, false)
            }
        coEvery { messagesDAO.getLatestSyncTimeForWallet(any()) } returns lastTimeMessageSynced
        coEvery { messagesDAO.insertMessage(any()) } returns 1L
        coEvery { messageUploader.uploadMessages() } returns Unit
    }

    private fun response(
        wallet: String,
        total: Int,
        messages: List<MessageResponse>,
        offset: Int = 0,
    ): MessagesResponse =
        MessagesResponse(
            messages = messages,
            links = LinksResponse(prev = null, next = null),
            query = QueryResponse(total = total, handle = wallet, limit = 100, offset = offset),
        )

    private fun messageResponse(
        id: String,
        type: MessageType = MessageType.MESSAGE,
        survey: SurveyResponse? = null,
    ): MessageResponse =
        MessageResponse(
            id = id,
            type = type,
            from = "admin",
            to = "wallet",
            subject = null,
            body = "body",
            composedAt = "2023-01-01T00:00:00Z",
            parentMessageId = null,
            videoLink = null,
            surveyResponse = null,
            survey = survey,
        )

    private fun entity(
        id: String,
        type: MessageType,
        from: String = "admin",
        to: String = "wallet",
        subject: String? = null,
        body: String? = "body",
        composedAt: String = "2023-01-01T00:00:00Z",
        videoLink: String? = null,
        surveyId: String? = null,
    ): MessageEntity =
        MessageEntity(
            id = id,
            wallet = to,
            type = type,
            from = from,
            to = to,
            subject = subject,
            body = body,
            composedAt = composedAt,
            parentMessageId = null,
            videoLink = videoLink,
            surveyResponse = null,
            shouldUpload = false,
            bundleId = null,
            isRead = false,
            surveyId = surveyId,
            isSurveyComplete = surveyId?.let { false },
        )

    private class ErrorRecordingTree : Timber.Tree() {
        val errors: MutableList<String?> = Collections.synchronizedList(mutableListOf())

        override fun log(
            priority: Int,
            tag: String?,
            message: String,
            t: Throwable?,
        ) {
            if (priority == android.util.Log.ERROR) {
                errors.add(t?.message)
            }
        }
    }
}