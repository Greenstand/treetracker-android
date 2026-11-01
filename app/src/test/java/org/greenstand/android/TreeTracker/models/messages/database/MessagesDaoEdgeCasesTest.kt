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
import org.greenstand.android.TreeTracker.models.messages.Question
import org.greenstand.android.TreeTracker.models.messages.SurveyMessage
import org.greenstand.android.TreeTracker.models.messages.database.entities.MessageEntity
import org.greenstand.android.TreeTracker.models.messages.database.entities.QuestionEntity
import org.greenstand.android.TreeTracker.models.messages.database.entities.SurveyEntity
import org.greenstand.android.TreeTracker.models.messages.network.responses.MessageType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Storage details and edge cases of [MessagesDAO] that MessagesDaoTest doesn't cover. */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class MessagesDaoEdgeCasesTest {
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
        surveyResponse: List<String>? = null,
        surveyId: String? = null,
        isSurveyComplete: Boolean? = null,
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
        shouldUpload = false,
        bundleId = null,
        isRead = isRead,
        surveyId = surveyId,
        isSurveyComplete = isSurveyComplete,
    )

    private fun rawString(sql: String): String? =
        database.openHelper.readableDatabase.query(sql).use { cursor ->
            cursor.moveToFirst()
            cursor.getString(0)
        }

    // region storage format

    @Test
    fun `message type is stored as the enum name the type filtered queries rely on`() =
        runTest {
            MessageType.entries.forEach { dao.insertMessage(message(id = it.name, type = it)) }

            MessageType.entries.forEach {
                assertEquals(it.name, rawString("SELECT type FROM messages WHERE id = '${it.name}'"))
            }
        }

    @Test
    fun `survey responses are stored as a JSON array`() =
        runTest {
            dao.insertMessage(message(id = "m1", type = MessageType.SURVEY_RESPONSE, surveyResponse = listOf("Yes", "1-5")))

            assertEquals("""["Yes","1-5"]""", rawString("SELECT survey_response FROM messages WHERE id = 'm1'"))
        }

    @Test
    fun `an empty survey response stays empty and is not confused with null`() =
        runTest {
            dao.insertMessage(message(id = "empty", surveyResponse = emptyList()))
            dao.insertMessage(message(id = "null", surveyResponse = null))

            assertEquals(emptyList<String>(), dao.getMessage("empty")!!.surveyResponse)
            assertNull(dao.getMessage("null")!!.surveyResponse)
        }

    @Test
    fun `survey response answers with commas and quotes round trip`() =
        runTest {
            val answers = listOf("Yes, mostly", "about \"5\"", "")
            dao.insertMessage(message(id = "m1", surveyResponse = answers))

            assertEquals(answers, dao.getMessage("m1")!!.surveyResponse)
        }

    @Test
    fun `isSurveyComplete keeps null, false and true distinct`() =
        runTest {
            dao.insertMessage(message(id = "unknown", isSurveyComplete = null))
            dao.insertMessage(message(id = "no", isSurveyComplete = false))
            dao.insertMessage(message(id = "yes", isSurveyComplete = true))

            assertNull(dao.getMessage("unknown")!!.isSurveyComplete)
            assertEquals(false, dao.getMessage("no")!!.isSurveyComplete)
            assertEquals(true, dao.getMessage("yes")!!.isSurveyComplete)
        }

    @Test
    fun `question choices with an empty list and special characters round trip`() =
        runTest {
            dao.insertSurvey(SurveyEntity(id = "survey", title = "Survey"))
            dao.insertQuestion(QuestionEntity(surveyId = "survey", prompt = "Free text?", choices = emptyList()))
            dao.insertQuestion(QuestionEntity(surveyId = "survey", prompt = "Pick", choices = listOf("A, B", "\"C\"", "été")))

            val questions = dao.getQuestionsForSurvey("survey")

            assertEquals(listOf(emptyList(), listOf("A, B", "\"C\"", "été")), questions.map { it.choices })
        }

    @Test
    fun `message ids are case sensitive`() =
        runTest {
            dao.insertMessage(message(id = "abc"))
            dao.insertMessage(message(id = "ABC"))

            assertEquals(2, dao.getMessagesForWallet("wallet").size)
            assertNull(dao.getMessage("Abc"))
        }

    // endregion

    // region queries and flows

    @Test
    fun `getLatestSyncTimeForWallet compares server timestamps across days and months`() =
        runTest {
            dao.insertMessage(message(id = "a", composedAt = "2023-09-30T23:59:59.999Z"))
            dao.insertMessage(message(id = "b", composedAt = "2023-10-01T00:00:00.000Z"))
            dao.insertMessage(message(id = "c", composedAt = "2023-01-15T12:00:00.000Z"))
            dao.insertMessage(message(id = "other", wallet = "other", composedAt = "2030-01-01T00:00:00.000Z"))

            assertEquals("2023-10-01T00:00:00.000Z", dao.getLatestSyncTimeForWallet("wallet"))
        }

    @Test
    fun `getDirectMessagesForWallet emits again when a message is marked as read`() =
        runTest {
            dao.insertMessage(message(id = "m1"))

            val (before, after) =
                dao.getDirectMessagesForWallet("wallet").valuesBeforeAndAfter {
                    dao.markMessageAsRead(listOf("m1"))
                }

            assertFalse(before.single().isRead)
            assertTrue(after.single().isRead)
        }

    @Test
    fun `getAnnouncementMessagesForWallet emits again when an announcement arrives`() =
        runTest {
            val (before, after) =
                dao.getAnnouncementMessagesForWallet("wallet").valuesBeforeAndAfter {
                    dao.insertMessage(message(id = "a1", type = MessageType.ANNOUNCE))
                }

            assertTrue(before.isEmpty())
            assertEquals(listOf("a1"), after.map { it.id })
        }

    @Test
    fun `getMessagesForWalletFlow drops a deleted message`() =
        runTest {
            dao.insertMessage(message(id = "m1"))
            dao.insertMessage(message(id = "m2"))

            dao.deleteMessage("m1")

            assertEquals(listOf("m2"), dao.getMessagesForWalletFlow("wallet").first().map { it.id })
        }

    @Test
    fun `deleteMessage with an unknown id changes nothing`() =
        runTest {
            dao.insertMessage(message(id = "m1"))

            dao.deleteMessage("missing")

            assertEquals(listOf("m1"), dao.getMessagesForWallet("wallet").map { it.id })
        }

    @Test
    fun `getMessagesByIds skips unknown ids and returns nothing for an empty list`() =
        runTest {
            dao.insertMessage(message(id = "m1"))

            assertEquals(listOf("m1"), dao.getMessagesByIds(listOf("m1", "missing")).map { it.id })
            assertTrue(dao.getMessagesByIds(emptyList()).isEmpty())
        }

    @Test
    fun `mark operations with an empty id list change nothing`() =
        runTest {
            dao.insertMessage(message(id = "m1").copy(shouldUpload = true))

            dao.markMessageAsRead(emptyList())
            dao.markMessagesAsUploaded(emptyList())
            dao.updateMessageBundleIds(emptyList(), "bundle")

            val stored = dao.getMessage("m1")!!
            assertFalse(stored.isRead)
            assertTrue(stored.shouldUpload)
            assertNull(stored.bundleId)
        }

    @Test
    fun `markSurveyMessageComplete on an unknown id changes nothing`() =
        runTest {
            dao.insertMessage(message(id = "m1", type = MessageType.SURVEY, isSurveyComplete = false))

            dao.markSurveyMessageComplete("missing")

            assertEquals(false, dao.getMessage("m1")!!.isSurveyComplete)
        }

    @Test
    fun `getQuestionsForSurvey returns questions in insertion order`() =
        runTest {
            dao.insertSurvey(SurveyEntity(id = "survey", title = "Survey"))
            dao.insertSurvey(SurveyEntity(id = "other", title = "Other"))
            listOf("first", "second", "third").forEachIndexed { index, prompt ->
                dao.insertQuestion(QuestionEntity(surveyId = "survey", prompt = prompt, choices = listOf("$index")))
                dao.insertQuestion(QuestionEntity(surveyId = "other", prompt = "other $prompt", choices = emptyList()))
            }

            assertEquals(listOf("first", "second", "third"), dao.getQuestionsForSurvey("survey").map { it.prompt })
        }

    @Test
    fun `insertQuestion returns increasing ids`() =
        runTest {
            dao.insertSurvey(SurveyEntity(id = "survey", title = "Survey"))

            val first = dao.insertQuestion(QuestionEntity(surveyId = "survey", prompt = "a", choices = emptyList()))
            val second = dao.insertQuestion(QuestionEntity(surveyId = "survey", prompt = "b", choices = emptyList()))

            assertTrue(second > first)
            assertEquals(listOf(first, second), dao.getQuestionsForSurvey("survey").map { it.id })
        }

    // endregion

    @Test
    fun `a stored survey message converts back into a SurveyMessage with its questions`() =
        runTest {
            dao.insertSurvey(SurveyEntity(id = "survey", title = "Tree health"))
            dao.insertQuestion(QuestionEntity(surveyId = "survey", prompt = "Healthy?", choices = listOf("Yes", "No")))
            dao.insertQuestion(QuestionEntity(surveyId = "survey", prompt = "Height?", choices = listOf("<1m", ">1m")))
            dao.insertMessage(message(id = "m1", type = MessageType.SURVEY, surveyId = "survey", isSurveyComplete = true))

            val entity = dao.getMessage("m1")!!
            val message =
                DatabaseConverters.createMessageFromEntities(
                    messageEntity = entity,
                    surveyEntity = dao.getSurvey(entity.surveyId),
                    questionEntities = dao.getQuestionsForSurvey(entity.surveyId!!),
                ) as SurveyMessage

            assertEquals("Tree health", message.title)
            assertEquals("survey", message.surveyId)
            assertTrue(message.isComplete)
            assertEquals(
                listOf(Question("Healthy?", listOf("Yes", "No")), Question("Height?", listOf("<1m", ">1m"))),
                message.questions,
            )
        }
}