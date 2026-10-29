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
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.api.ObjectStorageClient
import org.greenstand.android.TreeTracker.models.messages.database.MessagesDAO
import org.greenstand.android.TreeTracker.models.messages.database.entities.MessageEntity
import org.greenstand.android.TreeTracker.models.messages.network.responses.MessageType
import org.greenstand.android.TreeTracker.utilities.DeviceUtils
import org.greenstand.android.TreeTracker.utilities.md5
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@ExperimentalCoroutinesApi
class MessageUploaderTest {
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @MockK(relaxed = true)
    private lateinit var objectStorageClient: ObjectStorageClient

    @MockK(relaxed = true)
    private lateinit var messagesDAO: MessagesDAO

    private val json =
        Json {
            explicitNulls = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    private lateinit var messageUploader: MessageUploader

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        mockkObject(DeviceUtils)
        every { DeviceUtils.deviceId } returns "test-device-id"
        messageUploader =
            MessageUploader(
                objectStorageClient = objectStorageClient,
                messagesDAO = messagesDAO,
                json = json,
            )
    }

    @After
    fun tearDown() {
        unmockkObject(DeviceUtils)
    }

    @Test
    fun `WHEN messages to upload exist THEN uploads message bundles and marks as uploaded`() =
        runTest {
            val messageEntity =
                MessageEntity(
                    id = "msg-1",
                    wallet = "wallet-1",
                    type = MessageType.MESSAGE,
                    from = "sender",
                    to = "recipient",
                    subject = null,
                    body = "Hello",
                    composedAt = "2023-01-01T00:00:00Z",
                    parentMessageId = null,
                    videoLink = null,
                    surveyResponse = null,
                    shouldUpload = true,
                    bundleId = null,
                    isRead = false,
                    surveyId = null,
                    isSurveyComplete = null,
                )

            coEvery { messagesDAO.getMessageIdsToUpload() } returns listOf("msg-1")
            coEvery { messagesDAO.getMessagesByIds(listOf("msg-1")) } returns listOf(messageEntity)

            messageUploader.uploadMessages()

            coVerify(exactly = 1) { objectStorageClient.uploadBundle(any(), any()) }
            coVerify(exactly = 1) { messagesDAO.markMessagesAsUploaded(listOf("msg-1")) }
            coVerify(exactly = 1) { messagesDAO.updateMessageBundleIds(listOf("msg-1"), any()) }
        }

    @Test
    fun `WHEN no messages to upload THEN no-ops`() =
        runTest {
            coEvery { messagesDAO.getMessageIdsToUpload() } returns emptyList()

            messageUploader.uploadMessages()

            coVerify(exactly = 0) { objectStorageClient.uploadBundle(any(), any()) }
            coVerify(exactly = 0) { messagesDAO.markMessagesAsUploaded(any()) }
        }

    @Test
    fun `WHEN direct message uploaded THEN bundle contains a message request`() =
        runTest {
            val bundleSlot = slot<String>()
            every { objectStorageClient.uploadBundle(capture(bundleSlot), any()) } returns Unit
            coEvery { messagesDAO.getMessageIdsToUpload() } returns listOf("msg-1")
            coEvery { messagesDAO.getMessagesByIds(listOf("msg-1")) } returns listOf(message(id = "msg-1", parentMessageId = "parent-1"))

            messageUploader.uploadMessages()

            val bundle = json.parseToJsonElement(bundleSlot.captured).jsonObject
            assertEquals("2", bundle["pack_format_version"]!!.jsonPrimitive.content)
            val request = bundle["messages"]!!.jsonArray.single().jsonObject
            assertEquals("msg-1", request["id"]!!.jsonPrimitive.content)
            assertEquals("message", request["type"]!!.jsonPrimitive.content)
            assertEquals("sender", request["author_handle"]!!.jsonPrimitive.content)
            assertEquals("recipient", request["recipient_handle"]!!.jsonPrimitive.content)
            assertEquals("Hello", request["body"]!!.jsonPrimitive.content)
            assertEquals("2023-01-01T00:00:00Z", request["composed_at"]!!.jsonPrimitive.content)
            assertEquals("parent-1", request["parentMessageId"]!!.jsonPrimitive.content)
            assertEquals(JsonNull, request["survey_response"])
            assertEquals(JsonNull, request["survey_id"])
        }

    @Test
    fun `WHEN survey response uploaded THEN request carries survey id and answers`() =
        runTest {
            val bundleSlot = slot<String>()
            every { objectStorageClient.uploadBundle(capture(bundleSlot), any()) } returns Unit
            coEvery { messagesDAO.getMessageIdsToUpload() } returns listOf("msg-2")
            coEvery { messagesDAO.getMessagesByIds(listOf("msg-2")) } returns
                listOf(
                    message(id = "msg-2").copy(
                        type = MessageType.SURVEY_RESPONSE,
                        body = null,
                        surveyId = "survey-1",
                        surveyResponse = listOf("Yes", "Maybe"),
                    ),
                )

            messageUploader.uploadMessages()

            val request =
                json
                    .parseToJsonElement(bundleSlot.captured)
                    .jsonObject["messages"]!!
                    .jsonArray
                    .single()
                    .jsonObject
            assertEquals("survey_response", request["type"]!!.jsonPrimitive.content)
            assertEquals("survey-1", request["survey_id"]!!.jsonPrimitive.content)
            assertEquals(listOf("Yes", "Maybe"), request["survey_response"]!!.jsonArray.map { it.jsonPrimitive.content })
        }

    @Test
    fun `WHEN messages uploaded THEN records bundle id uploads bundle then marks messages uploaded`() =
        runTest {
            val bundleSlot = slot<String>()
            val bundleIdSlot = slot<String>()
            every { objectStorageClient.uploadBundle(capture(bundleSlot), capture(bundleIdSlot)) } returns Unit
            coEvery { messagesDAO.getMessageIdsToUpload() } returns listOf("msg-1", "msg-2")
            coEvery { messagesDAO.getMessagesByIds(listOf("msg-1", "msg-2")) } returns
                listOf(message(id = "msg-1"), message(id = "msg-2"))

            messageUploader.uploadMessages()

            val expectedBundleId = bundleSlot.captured.md5() + "_messages"
            assertEquals(expectedBundleId, bundleIdSlot.captured)
            coVerifyOrder {
                messagesDAO.updateMessageBundleIds(listOf("msg-1", "msg-2"), expectedBundleId)
                objectStorageClient.uploadBundle(any(), expectedBundleId)
                messagesDAO.markMessagesAsUploaded(listOf("msg-1", "msg-2"))
            }
        }

    @Test
    fun `WHEN more than fifty messages THEN uploads them in bundles of fifty`() =
        runTest {
            val ids = (1..120).map { "msg-$it" }
            coEvery { messagesDAO.getMessageIdsToUpload() } returns ids
            coEvery { messagesDAO.getMessagesByIds(any()) } answers { firstArg<List<String>>().map { message(id = it) } }

            messageUploader.uploadMessages()

            coVerify { messagesDAO.getMessagesByIds(ids.subList(0, 50)) }
            coVerify { messagesDAO.getMessagesByIds(ids.subList(50, 100)) }
            coVerify { messagesDAO.getMessagesByIds(ids.subList(100, 120)) }
            coVerify(exactly = 3) { objectStorageClient.uploadBundle(any(), any()) }
            coVerify { messagesDAO.markMessagesAsUploaded(ids.subList(100, 120)) }
        }

    @Test
    fun `WHEN an announcement is queued for upload THEN fails without uploading`() =
        runTest {
            coEvery { messagesDAO.getMessageIdsToUpload() } returns listOf("msg-1")
            coEvery { messagesDAO.getMessagesByIds(listOf("msg-1")) } returns
                listOf(message(id = "msg-1").copy(type = MessageType.ANNOUNCE))

            assertFailsWith<IllegalStateException> {
                messageUploader.uploadMessages()
            }

            coVerify(exactly = 0) { objectStorageClient.uploadBundle(any(), any()) }
            coVerify(exactly = 0) { messagesDAO.markMessagesAsUploaded(any()) }
        }

    @Test
    fun `WHEN bundle upload fails THEN messages are not marked uploaded`() =
        runTest {
            coEvery { messagesDAO.getMessageIdsToUpload() } returns listOf("msg-1")
            coEvery { messagesDAO.getMessagesByIds(listOf("msg-1")) } returns listOf(message(id = "msg-1"))
            every { objectStorageClient.uploadBundle(any(), any()) } throws RuntimeException("offline")

            assertFailsWith<RuntimeException> {
                messageUploader.uploadMessages()
            }

            coVerify(exactly = 0) { messagesDAO.markMessagesAsUploaded(any()) }
        }

    private fun message(
        id: String,
        parentMessageId: String? = null,
    ): MessageEntity =
        MessageEntity(
            id = id,
            wallet = "wallet-1",
            type = MessageType.MESSAGE,
            from = "sender",
            to = "recipient",
            subject = null,
            body = "Hello",
            composedAt = "2023-01-01T00:00:00Z",
            parentMessageId = parentMessageId,
            videoLink = null,
            surveyResponse = null,
            shouldUpload = true,
            bundleId = null,
            isRead = true,
            surveyId = null,
            isSurveyComplete = null,
        )
}