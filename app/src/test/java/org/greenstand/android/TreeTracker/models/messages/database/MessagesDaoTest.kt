/*
 * Copyright 2026 Treetracker
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
package org.greenstand.android.TreeTracker.models.messages.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.greenstand.android.TreeTracker.database.valuesBeforeAndAfter
import org.greenstand.android.TreeTracker.models.messages.database.entities.MessageEntity
import org.greenstand.android.TreeTracker.models.messages.database.entities.QuestionEntity
import org.greenstand.android.TreeTracker.models.messages.database.entities.SurveyEntity
import org.greenstand.android.TreeTracker.models.messages.network.responses.MessageType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class MessagesDaoTest {
    private lateinit var database: MessageDatabase
    private lateinit var dao: MessagesDAO

    @Before
    fun openDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, MessageDatabase::class.java).build()
        dao = database.messagesDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    private fun message(
        id: String,
        wallet: String = "wallet",
        type: MessageType = MessageType.MESSAGE,
        composedAt: String = "2024-03-01T10:00:00Z",
        isRead: Boolean = false,
        shouldUpload: Boolean = false,
        surveyResponse: List<String>? = null,
        surveyId: String? = null,
        isSurveyComplete: Boolean? = null,
        bundleId: String? = null,
    ) = MessageEntity(
        id = id,
        wallet = wallet,
        type = type,
        from = "admin",
        to = wallet,
        subject = "subject",
        body = "body",
        composedAt = composedAt,
        parentMessageId = null,
        videoLink = null,
        surveyResponse = surveyResponse,
        shouldUpload = shouldUpload,
        bundleId = bundleId,
        isRead = isRead,
        surveyId = surveyId,
        isSurveyComplete = isSurveyComplete,
    )

    // region messages

    @Test
    fun `insertMessage stores every field and getMessage finds it by id`() =
        runTest {
            val message =
                message(
                    id = "m1",
                    type = MessageType.SURVEY_RESPONSE,
                    surveyResponse = listOf("yes", "no"),
                    surveyId = "survey-1",
                    isSurveyComplete = true,
                    shouldUpload = true,
                    bundleId = "bundle",
                    isRead = true,
                ).copy(parentMessageId = "parent", videoLink = "https://video")

            dao.insertMessage(message)

            assertEquals(message, dao.getMessage("m1"))
        }

    @Test
    fun `insertMessage keeps null optional fields`() =
        runTest {
            val message = message(id = "m1").copy(subject = null, body = null)

            dao.insertMessage(message)

            val stored = dao.getMessage("m1")!!
            assertNull(stored.subject)
            assertNull(stored.body)
            assertNull(stored.surveyResponse)
            assertNull(stored.surveyId)
            assertNull(stored.isSurveyComplete)
        }

    @Test
    fun `insertMessage stores every message type`() =
        runTest {
            MessageType.entries.forEach { type -> dao.insertMessage(message(id = type.name, type = type)) }

            MessageType.entries.forEach { type -> assertEquals(type, dao.getMessage(type.name)?.type) }
        }

    @Test
    fun `insertMessage fails for a duplicate id and keeps the original`() =
        runTest {
            dao.insertMessage(message(id = "m1", wallet = "original"))

            val result = runCatching { dao.insertMessage(message(id = "m1", wallet = "duplicate")) }

            assertTrue(result.isFailure)
            assertEquals("original", dao.getMessage("m1")?.wallet)
        }

    @Test
    fun `getMessage returns null for an unknown id`() =
        runTest {
            assertNull(dao.getMessage("missing"))
        }

    @Test
    fun `deleteMessage removes only that message`() =
        runTest {
            dao.insertMessage(message(id = "m1"))
            dao.insertMessage(message(id = "m2"))

            dao.deleteMessage("m1")

            assertNull(dao.getMessage("m1"))
            assertNotNull(dao.getMessage("m2"))
        }

    @Test
    fun `getLatestSyncTimeForWallet returns the newest composed_at of that wallet`() =
        runTest {
            dao.insertMessage(message(id = "m1", wallet = "a", composedAt = "2024-01-01T00:00:00Z"))
            dao.insertMessage(message(id = "m2", wallet = "a", composedAt = "2024-03-01T00:00:00Z"))
            dao.insertMessage(message(id = "m3", wallet = "a", composedAt = "2024-02-01T00:00:00Z"))
            dao.insertMessage(message(id = "m4", wallet = "b", composedAt = "2025-01-01T00:00:00Z"))

            assertEquals("2024-03-01T00:00:00Z", dao.getLatestSyncTimeForWallet("a"))
        }

    @Test
    fun `getLatestSyncTimeForWallet returns null for a wallet without messages`() =
        runTest {
            dao.insertMessage(message(id = "m1", wallet = "a"))

            assertNull(dao.getLatestSyncTimeForWallet("b"))
        }

    @Test
    fun `getMessagesForWallet returns only that wallet's messages`() =
        runTest {
            dao.insertMessage(message(id = "m1", wallet = "a"))
            dao.insertMessage(message(id = "m2", wallet = "b"))
            dao.insertMessage(message(id = "m3", wallet = "a", type = MessageType.ANNOUNCE))

            assertEquals(setOf("m1", "m3"), dao.getMessagesForWallet("a").map { it.id }.toSet())
            assertTrue(dao.getMessagesForWallet("c").isEmpty())
        }

    @Test
    fun `getMessagesForWalletFlow returns only that wallet's messages`() =
        runTest {
            dao.insertMessage(message(id = "m1", wallet = "a"))
            dao.insertMessage(message(id = "m2", wallet = "b"))

            assertEquals(listOf("m1"), dao.getMessagesForWalletFlow("a").first().map { it.id })
        }

    @Test
    fun `getMessagesForWalletFlow emits again when a message is inserted`() =
        runTest {
            val message = message(id = "m1", wallet = "a")

            val (before, after) = dao.getMessagesForWalletFlow("a").valuesBeforeAndAfter { dao.insertMessage(message) }

            assertEquals(emptyList<MessageEntity>(), before)
            assertEquals(listOf(message), after)
        }

    @Test
    fun `getDirectMessagesForWallet returns only direct messages of that wallet`() =
        runTest {
            dao.insertMessage(message(id = "direct", wallet = "a", type = MessageType.MESSAGE))
            dao.insertMessage(message(id = "announce", wallet = "a", type = MessageType.ANNOUNCE))
            dao.insertMessage(message(id = "survey", wallet = "a", type = MessageType.SURVEY))
            dao.insertMessage(message(id = "response", wallet = "a", type = MessageType.SURVEY_RESPONSE))
            dao.insertMessage(message(id = "other-wallet", wallet = "b", type = MessageType.MESSAGE))

            assertEquals(listOf("direct"), dao.getDirectMessagesForWallet("a").first().map { it.id })
        }

    @Test
    fun `getAnnouncementMessagesForWallet returns only announcements of that wallet`() =
        runTest {
            dao.insertMessage(message(id = "direct", wallet = "a", type = MessageType.MESSAGE))
            dao.insertMessage(message(id = "announce", wallet = "a", type = MessageType.ANNOUNCE))
            dao.insertMessage(message(id = "other-wallet", wallet = "b", type = MessageType.ANNOUNCE))

            assertEquals(listOf("announce"), dao.getAnnouncementMessagesForWallet("a").first().map { it.id })
        }

    @Test
    fun `updateMessageBundleIds sets the bundle id only on the given messages`() =
        runTest {
            dao.insertMessage(message(id = "m1"))
            dao.insertMessage(message(id = "m2"))

            dao.updateMessageBundleIds(listOf("m1"), "bundle")

            assertEquals("bundle", dao.getMessage("m1")?.bundleId)
            assertNull(dao.getMessage("m2")?.bundleId)
        }

    @Test
    fun `getMessagesToUpload and getMessageIdsToUpload return only messages that should upload`() =
        runTest {
            dao.insertMessage(message(id = "pending", shouldUpload = true))
            dao.insertMessage(message(id = "received", shouldUpload = false))

            assertEquals(listOf("pending"), dao.getMessagesToUpload().map { it.id })
            assertEquals(listOf("pending"), dao.getMessageIdsToUpload())
        }

    @Test
    fun `getMessagesByIds returns only the requested messages`() =
        runTest {
            dao.insertMessage(message(id = "m1"))
            dao.insertMessage(message(id = "m2"))
            dao.insertMessage(message(id = "m3"))

            assertEquals(setOf("m1", "m3"), dao.getMessagesByIds(listOf("m1", "m3", "missing")).map { it.id }.toSet())
        }

    @Test
    fun `markMessagesAsUploaded clears should_upload only for the given messages`() =
        runTest {
            dao.insertMessage(message(id = "m1", shouldUpload = true))
            dao.insertMessage(message(id = "m2", shouldUpload = true))

            dao.markMessagesAsUploaded(listOf("m1"))

            assertFalse(dao.getMessage("m1")!!.shouldUpload)
            assertEquals(listOf("m2"), dao.getMessageIdsToUpload())
        }

    @Test
    fun `markMessageAsRead marks only the given messages as read`() =
        runTest {
            dao.insertMessage(message(id = "m1"))
            dao.insertMessage(message(id = "m2"))
            dao.insertMessage(message(id = "m3"))

            dao.markMessageAsRead(listOf("m1", "m3"))

            assertTrue(dao.getMessage("m1")!!.isRead)
            assertFalse(dao.getMessage("m2")!!.isRead)
            assertTrue(dao.getMessage("m3")!!.isRead)
        }

    @Test
    fun `markSurveyMessageComplete sets the survey complete flag for that message`() =
        runTest {
            dao.insertMessage(message(id = "s1", type = MessageType.SURVEY, isSurveyComplete = false))
            dao.insertMessage(message(id = "s2", type = MessageType.SURVEY, isSurveyComplete = null))

            dao.markSurveyMessageComplete("s1")

            assertEquals(true, dao.getMessage("s1")?.isSurveyComplete)
            assertNull(dao.getMessage("s2")?.isSurveyComplete)
        }

    @Test
    fun `markSurveyMessageComplete with a null id changes nothing`() =
        runTest {
            dao.insertMessage(message(id = "s1", type = MessageType.SURVEY, isSurveyComplete = false))

            dao.markSurveyMessageComplete(null)

            assertEquals(false, dao.getMessage("s1")?.isSurveyComplete)
        }

    @Test
    fun `getUnreadMessagesCount counts unread messages across wallets`() =
        runTest {
            dao.insertMessage(message(id = "m1", wallet = "a", isRead = false))
            dao.insertMessage(message(id = "m2", wallet = "b", isRead = false))
            dao.insertMessage(message(id = "m3", wallet = "a", isRead = true))

            assertEquals(2, dao.getUnreadMessagesCount())
        }

    @Test
    fun `getUnreadMessageCountForWallet counts unread messages of that wallet`() =
        runTest {
            dao.insertMessage(message(id = "m1", wallet = "a", isRead = false))
            dao.insertMessage(message(id = "m2", wallet = "a", isRead = true))
            dao.insertMessage(message(id = "m3", wallet = "b", isRead = false))

            assertEquals(1, dao.getUnreadMessageCountForWallet("a"))
            assertEquals(0, dao.getUnreadMessageCountForWallet("c"))
        }

    @Test
    fun `unread counts drop after messages are marked as read`() =
        runTest {
            dao.insertMessage(message(id = "m1", wallet = "a"))
            dao.insertMessage(message(id = "m2", wallet = "a"))

            dao.markMessageAsRead(listOf("m1"))

            assertEquals(1, dao.getUnreadMessagesCount())
            assertEquals(1, dao.getUnreadMessageCountForWallet("a"))
        }

    // endregion

    // region surveys and questions

    @Test
    fun `insertSurvey stores the survey and getSurvey finds it by id`() =
        runTest {
            dao.insertSurvey(SurveyEntity(id = "survey-1", title = "Tree health"))

            val survey = dao.getSurvey("survey-1")
            assertEquals("survey-1", survey?.id)
            assertEquals("Tree health", survey?.title)
        }

    @Test
    fun `getSurvey returns null for an unknown or null id`() =
        runTest {
            dao.insertSurvey(SurveyEntity(id = "survey-1", title = "Tree health"))

            assertNull(dao.getSurvey("missing"))
            assertNull(dao.getSurvey(null))
        }

    @Test
    fun `insertSurvey fails for a duplicate id and keeps the original`() =
        runTest {
            dao.insertSurvey(SurveyEntity(id = "survey-1", title = "Original"))

            val result = runCatching { dao.insertSurvey(SurveyEntity(id = "survey-1", title = "Duplicate")) }

            assertTrue(result.isFailure)
            assertEquals("Original", dao.getSurvey("survey-1")?.title)
        }

    @Test
    fun `deleteSurvey removes a survey without questions`() =
        runTest {
            dao.insertSurvey(SurveyEntity(id = "survey-1", title = "Tree health"))

            dao.deleteSurvey("survey-1")

            assertNull(dao.getSurvey("survey-1"))
        }

    @Test
    fun `deleteSurvey fails while questions still reference the survey`() =
        runTest {
            dao.insertSurvey(SurveyEntity(id = "survey-1", title = "Tree health"))
            dao.insertQuestion(QuestionEntity(surveyId = "survey-1", prompt = "Healthy?", choices = listOf("yes", "no")))

            val result = runCatching { dao.deleteSurvey("survey-1") }

            assertTrue(result.isFailure)
            assertNotNull(dao.getSurvey("survey-1"))
        }

    @Test
    fun `insertQuestion stores the question with its choices`() =
        runTest {
            dao.insertSurvey(SurveyEntity(id = "survey-1", title = "Tree health"))

            val id = dao.insertQuestion(QuestionEntity(surveyId = "survey-1", prompt = "Healthy?", choices = listOf("yes", "no")))

            val question = dao.getQuestionsForSurvey("survey-1").single()
            assertEquals(id, question.id)
            assertEquals("survey-1", question.surveyId)
            assertEquals("Healthy?", question.prompt)
            assertEquals(listOf("yes", "no"), question.choices)
        }

    @Test
    fun `insertQuestion fails when the survey does not exist`() =
        runTest {
            val result = runCatching { dao.insertQuestion(QuestionEntity(surveyId = "missing", prompt = "?", choices = emptyList())) }

            assertTrue(result.isFailure)
            assertTrue(dao.getQuestionsForSurvey("missing").isEmpty())
        }

    @Test
    fun `getQuestionsForSurvey returns only that survey's questions`() =
        runTest {
            dao.insertSurvey(SurveyEntity(id = "a", title = "A"))
            dao.insertSurvey(SurveyEntity(id = "b", title = "B"))
            dao.insertQuestion(QuestionEntity(surveyId = "a", prompt = "a1", choices = listOf("x")))
            dao.insertQuestion(QuestionEntity(surveyId = "a", prompt = "a2", choices = listOf("y")))
            dao.insertQuestion(QuestionEntity(surveyId = "b", prompt = "b1", choices = listOf("z")))

            assertEquals(setOf("a1", "a2"), dao.getQuestionsForSurvey("a").map { it.prompt }.toSet())
            assertTrue(dao.getQuestionsForSurvey("c").isEmpty())
        }

    @Test
    fun `deleteQuestions removes only the given questions`() =
        runTest {
            dao.insertSurvey(SurveyEntity(id = "a", title = "A"))
            val first = dao.insertQuestion(QuestionEntity(surveyId = "a", prompt = "a1", choices = listOf("x")))
            val second = dao.insertQuestion(QuestionEntity(surveyId = "a", prompt = "a2", choices = listOf("y")))
            val third = dao.insertQuestion(QuestionEntity(surveyId = "a", prompt = "a3", choices = listOf("z")))

            dao.deleteQuestions(listOf(first, third))

            assertEquals(listOf(second), dao.getQuestionsForSurvey("a").map { it.id })
        }

    @Test
    fun `a survey can be deleted after its questions are deleted`() =
        runTest {
            dao.insertSurvey(SurveyEntity(id = "a", title = "A"))
            val question = dao.insertQuestion(QuestionEntity(surveyId = "a", prompt = "a1", choices = listOf("x")))

            dao.deleteQuestions(listOf(question))
            dao.deleteSurvey("a")

            assertNull(dao.getSurvey("a"))
        }

    // endregion
}