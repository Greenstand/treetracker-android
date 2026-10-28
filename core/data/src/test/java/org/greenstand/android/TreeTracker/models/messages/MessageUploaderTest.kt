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
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.api.ObjectStorageClient
import org.greenstand.android.TreeTracker.models.messages.database.MessagesDAO
import org.greenstand.android.TreeTracker.models.messages.database.entities.MessageEntity
import org.greenstand.android.TreeTracker.models.messages.network.responses.MessageType
import org.greenstand.android.TreeTracker.utilities.DeviceUtils
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

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
}