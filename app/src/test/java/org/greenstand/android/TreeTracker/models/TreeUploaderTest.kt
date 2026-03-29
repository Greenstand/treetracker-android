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
package org.greenstand.android.TreeTracker.models

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.serialization.json.Json
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.api.ObjectStorageClient
import org.greenstand.android.TreeTracker.api.models.requests.NewTreeRequest
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.database.entity.SessionEntity
import org.greenstand.android.TreeTracker.database.entity.TreeEntity
import org.greenstand.android.TreeTracker.database.legacy.entity.TreeCaptureEntity
import org.greenstand.android.TreeTracker.usecases.CreateTreeRequestUseCase
import org.greenstand.android.TreeTracker.usecases.UploadImageUseCase
import org.greenstand.android.TreeTracker.utilities.DeviceUtils
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
@ExperimentalCoroutinesApi
class TreeUploaderTest {

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @MockK(relaxed = true)
    private lateinit var uploadImageUseCase: UploadImageUseCase

    @MockK(relaxed = true)
    private lateinit var objectStorageClient: ObjectStorageClient

    @MockK(relaxed = true)
    private lateinit var createTreeRequestUseCase: CreateTreeRequestUseCase

    @MockK(relaxed = true)
    private lateinit var dao: TreeTrackerDAO

    private val json = Json { explicitNulls = true; ignoreUnknownKeys = true; encodeDefaults = true }

    private lateinit var treeUploader: TreeUploader

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        mockkObject(DeviceUtils)
        every { DeviceUtils.deviceId } returns "test-device-id"
        treeUploader = TreeUploader(
            uploadImageUseCase = uploadImageUseCase,
            objectStorageClient = objectStorageClient,
            createTreeRequestUseCase = createTreeRequestUseCase,
            dao = dao,
            json = json,
        )
    }

    @After
    fun tearDown() {
        unmockkObject(DeviceUtils)
    }

    @Test
    fun `WHEN trees have null photoUrl THEN uploads images and bundles`() = runTest {
        val treeEntity = TreeEntity(
            uuid = "tree-uuid-1",
            sessionId = 1L,
            photoPath = "/test/photo.jpg",
            photoUrl = null,
            note = "test note",
            latitude = 37.0,
            longitude = -122.0,
            createdAt = Instant.parse("2023-01-01T00:00:00Z"),
        ).apply { id = 1L }

        val sessionEntity = SessionEntity(
            uuid = "session-uuid-1",
            originUserId = "user-uuid",
            originWallet = "wallet",
            destinationWallet = "dest-wallet",
            startTime = Instant.parse("2023-01-01T00:00:00Z"),
            organization = "org",
            isUploaded = false,
        ).apply { id = 1L }

        coEvery { dao.getTreesByIds(listOf(1L)) } returns listOf(treeEntity)
        coEvery { uploadImageUseCase.execute(any()) } returns "https://uploaded.url/photo.jpg"
        coEvery { dao.getSessionById(1L) } returns sessionEntity

        treeUploader.uploadTrees(listOf(1L))

        coVerify(exactly = 1) { uploadImageUseCase.execute(any()) }
        coVerify(exactly = 1) { objectStorageClient.uploadBundle(any(), any()) }
        coVerify(exactly = 1) { dao.updateTreesUploadStatus(listOf(1L), true) }
    }

    @Test
    fun `WHEN trees have existing photoUrl THEN skips image upload`() = runTest {
        val treeEntity = TreeEntity(
            uuid = "tree-uuid-1",
            sessionId = 1L,
            photoPath = "/test/photo.jpg",
            photoUrl = "https://existing.url/photo.jpg",
            note = "test note",
            latitude = 37.0,
            longitude = -122.0,
            createdAt = Instant.parse("2023-01-01T00:00:00Z"),
        ).apply { id = 1L }

        val sessionEntity = SessionEntity(
            uuid = "session-uuid-1",
            originUserId = "user-uuid",
            originWallet = "wallet",
            destinationWallet = "dest-wallet",
            startTime = Instant.parse("2023-01-01T00:00:00Z"),
            organization = "org",
            isUploaded = false,
        ).apply { id = 1L }

        coEvery { dao.getTreesByIds(listOf(1L)) } returns listOf(treeEntity)
        coEvery { dao.getSessionById(1L) } returns sessionEntity

        treeUploader.uploadTrees(listOf(1L))

        coVerify(exactly = 0) { uploadImageUseCase.execute(any()) }
        coVerify(exactly = 1) { objectStorageClient.uploadBundle(any(), any()) }
    }

    @Test
    fun `WHEN uploadLegacyTrees called THEN processes legacy tree captures`() = runTest {
        val legacyTree = TreeCaptureEntity(
            uuid = "legacy-uuid",
            planterCheckInId = 1L,
            localPhotoPath = "/test/legacy.jpg",
            photoUrl = null,
            noteContent = "legacy note",
            latitude = 37.0,
            longitude = -122.0,
            accuracy = 5.0,
            createAt = System.currentTimeMillis(),
        ).apply { id = 1L }

        val newTreeRequest = mockk<NewTreeRequest>(relaxed = true)

        coEvery { dao.getTreeCapturesByIds(listOf(1L)) } returns listOf(legacyTree)
        coEvery { uploadImageUseCase.execute(any()) } returns "https://uploaded.url/legacy.jpg"
        coEvery { createTreeRequestUseCase.execute(any()) } returns newTreeRequest

        treeUploader.uploadLegacyTrees(listOf(1L), "instance-123")

        coVerify(exactly = 1) { uploadImageUseCase.execute(any()) }
        coVerify(exactly = 1) { objectStorageClient.uploadBundle(any(), any()) }
        coVerify(exactly = 1) { dao.updateTreeCapturesUploadStatus(listOf(1L), true) }
    }
}
