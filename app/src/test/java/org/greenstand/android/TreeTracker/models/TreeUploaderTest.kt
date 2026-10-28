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
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.api.ObjectStorageClient
import org.greenstand.android.TreeTracker.api.models.requests.NewTreeRequest
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.database.entity.SessionEntity
import org.greenstand.android.TreeTracker.database.entity.TreeEntity
import org.greenstand.android.TreeTracker.database.legacy.entity.TreeCaptureEntity
import org.greenstand.android.TreeTracker.usecases.CreateTreeRequestUseCase
import org.greenstand.android.TreeTracker.usecases.UploadImageParams
import org.greenstand.android.TreeTracker.usecases.UploadImageUseCase
import org.greenstand.android.TreeTracker.utilities.DeviceUtils
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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

    private val json =
        Json {
            explicitNulls = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    private lateinit var treeUploader: TreeUploader

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        mockkObject(DeviceUtils)
        every { DeviceUtils.deviceId } returns "test-device-id"
        treeUploader =
            TreeUploader(
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
    fun `WHEN trees have null photoUrl THEN uploads images and bundles`() =
        runTest {
            val treeEntity =
                TreeEntity(
                    uuid = "tree-uuid-1",
                    sessionId = 1L,
                    photoPath = "/test/photo.jpg",
                    photoUrl = null,
                    note = "test note",
                    latitude = 37.0,
                    longitude = -122.0,
                    createdAt = Instant.parse("2023-01-01T00:00:00Z"),
                ).apply { id = 1L }

            val sessionEntity =
                SessionEntity(
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
    fun `WHEN trees have existing photoUrl THEN skips image upload`() =
        runTest {
            val treeEntity =
                TreeEntity(
                    uuid = "tree-uuid-1",
                    sessionId = 1L,
                    photoPath = "/test/photo.jpg",
                    photoUrl = "https://existing.url/photo.jpg",
                    note = "test note",
                    latitude = 37.0,
                    longitude = -122.0,
                    createdAt = Instant.parse("2023-01-01T00:00:00Z"),
                ).apply { id = 1L }

            val sessionEntity =
                SessionEntity(
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
    fun `WHEN one tree image fails to upload THEN the other trees are still bundled`() =
        runTest {
            coEvery { dao.getTreesByIds(listOf(1L, 2L, 3L)) } returns listOf(tree(1L), tree(2L), tree(3L))
            coEvery { dao.getSessionById(1L) } returns session()
            coEvery { uploadImageUseCase.execute(any()) } answers { "https://uploaded.url/${firstArg<UploadImageParams>().imagePath}" }
            coEvery { uploadImageUseCase.execute(match { it.imagePath == "/test/photo2.jpg" }) } returns null
            val bundle = slot<String>()
            coEvery { objectStorageClient.uploadBundle(capture(bundle), any()) } returns Unit

            treeUploader.uploadTrees(listOf(1L, 2L, 3L))

            coVerify(exactly = 1) { objectStorageClient.uploadBundle(any(), any()) }
            assertTrue(bundle.captured.contains("tree-uuid-1"))
            assertFalse(bundle.captured.contains("tree-uuid-2"))
            assertTrue(bundle.captured.contains("tree-uuid-3"))
            coVerify(exactly = 1) { dao.updateTreesUploadStatus(listOf(1L, 3L), true) }
            coVerify(exactly = 1) { dao.removeTreesLocalImagePaths(listOf(1L, 3L)) }
        }

    @Test
    fun `WHEN a tree image upload throws THEN the other trees are still bundled`() =
        runTest {
            coEvery { dao.getTreesByIds(listOf(1L, 2L)) } returns listOf(tree(1L), tree(2L))
            coEvery { dao.getSessionById(1L) } returns session()
            coEvery { uploadImageUseCase.execute(any()) } returns "https://uploaded.url/photo.jpg"
            coEvery { uploadImageUseCase.execute(match { it.imagePath == "/test/photo1.jpg" }) } throws RuntimeException("boom")

            treeUploader.uploadTrees(listOf(1L, 2L))

            coVerify(exactly = 1) { dao.updateTreesUploadStatus(listOf(2L), true) }
        }

    @Test
    fun `WHEN a tree has no local image THEN it is skipped and the other trees are still bundled`() =
        runTest {
            coEvery { dao.getTreesByIds(listOf(1L, 2L)) } returns listOf(tree(1L, photoPath = null), tree(2L))
            coEvery { dao.getSessionById(1L) } returns session()
            coEvery { uploadImageUseCase.execute(any()) } returns "https://uploaded.url/photo.jpg"

            treeUploader.uploadTrees(listOf(1L, 2L))

            coVerify(exactly = 1) { uploadImageUseCase.execute(any()) }
            coVerify(exactly = 1) { dao.updateTreesUploadStatus(listOf(2L), true) }
        }

    @Test
    fun `WHEN every tree image fails THEN no bundle is uploaded`() =
        runTest {
            coEvery { dao.getTreesByIds(listOf(1L, 2L)) } returns listOf(tree(1L), tree(2L))
            coEvery { uploadImageUseCase.execute(any()) } returns null

            treeUploader.uploadTrees(listOf(1L, 2L))

            coVerify(exactly = 0) { objectStorageClient.uploadBundle(any(), any()) }
            coVerify(exactly = 0) { dao.updateTreesUploadStatus(any(), any()) }
            coVerify(exactly = 0) { dao.removeTreesLocalImagePaths(any()) }
        }

    @Test
    fun `WHEN an image upload is cancelled THEN cancellation propagates`() =
        runTest {
            coEvery { dao.getTreesByIds(listOf(1L)) } returns listOf(tree(1L))
            coEvery { uploadImageUseCase.execute(any()) } throws CancellationException("stopped")

            assertFailsWith<CancellationException> {
                treeUploader.uploadTrees(listOf(1L))
            }
            coVerify(exactly = 0) { objectStorageClient.uploadBundle(any(), any()) }
        }

    @Test
    fun `WHEN one legacy tree image fails to upload THEN the other legacy trees are still bundled`() =
        runTest {
            val legacyTrees = listOf(legacyTree(1L), legacyTree(2L))
            coEvery { dao.getTreeCapturesByIds(listOf(1L, 2L)) } returns legacyTrees
            coEvery { createTreeRequestUseCase.execute(any()) } returns mockk<NewTreeRequest>(relaxed = true)
            coEvery { uploadImageUseCase.execute(any()) } returns "https://uploaded.url/legacy.jpg"
            coEvery { uploadImageUseCase.execute(match { it.imagePath == "/test/legacy1.jpg" }) } returns null

            treeUploader.uploadLegacyTrees(listOf(1L, 2L), "instance-123")

            coVerify(exactly = 1) { objectStorageClient.uploadBundle(any(), any()) }
            coVerify(exactly = 1) { dao.updateTreeCapturesUploadStatus(listOf(2L), true) }
            coVerify(exactly = 1) { dao.removeTreeCapturesLocalImagePaths(listOf(2L)) }
        }

    @Test
    fun `WHEN one legacy tree's upload request can't be built THEN the other legacy trees are still bundled`() =
        runTest {
            val uploadedImage = "https://uploaded.url/legacy.jpg"
            coEvery { dao.getTreeCapturesByIds(listOf(1L, 2L, 3L)) } returns
                listOf(legacyTree(1L, uploadedImage), legacyTree(2L, uploadedImage), legacyTree(3L, uploadedImage))
            coEvery { createTreeRequestUseCase.execute(any()) } returns mockk<NewTreeRequest>(relaxed = true)
            coEvery { createTreeRequestUseCase.execute(match { it.treeId == 2L }) } throws IllegalStateException("No Planter CheckIn")

            treeUploader.uploadLegacyTrees(listOf(1L, 2L, 3L), "instance-123")

            coVerify(exactly = 1) { objectStorageClient.uploadBundle(any(), any()) }
            coVerify(exactly = 1) { dao.updateTreeCapturesUploadStatus(listOf(1L, 3L), true) }
            coVerify(exactly = 1) { dao.removeTreeCapturesLocalImagePaths(listOf(1L, 3L)) }
        }

    @Test
    fun `WHEN one tree's session can't be loaded THEN the other trees are still bundled`() =
        runTest {
            val uploadedImage = "https://uploaded.url/photo.jpg"
            coEvery { dao.getTreesByIds(listOf(1L, 2L)) } returns
                listOf(tree(1L, photoUrl = uploadedImage), tree(2L, photoUrl = uploadedImage, sessionId = 2L))
            coEvery { dao.getSessionById(1L) } returns session()
            coEvery { dao.getSessionById(2L) } throws IllegalStateException("No session")

            treeUploader.uploadTrees(listOf(1L, 2L))

            coVerify(exactly = 1) { objectStorageClient.uploadBundle(any(), any()) }
            coVerify(exactly = 1) { dao.updateTreesUploadStatus(listOf(1L), true) }
            coVerify(exactly = 1) { dao.removeTreesLocalImagePaths(listOf(1L)) }
        }

    private fun tree(
        id: Long,
        photoPath: String? = "/test/photo$id.jpg",
        photoUrl: String? = null,
        sessionId: Long = 1L,
    ) = TreeEntity(
        uuid = "tree-uuid-$id",
        sessionId = sessionId,
        photoPath = photoPath,
        photoUrl = photoUrl,
        note = "",
        latitude = 37.0,
        longitude = -122.0,
        createdAt = Instant.parse("2023-01-01T00:00:00Z"),
    ).apply { this.id = id }

    private fun legacyTree(
        id: Long,
        photoUrl: String? = null,
    ) = TreeCaptureEntity(
        uuid = "legacy-uuid-$id",
        planterCheckInId = 1L,
        localPhotoPath = "/test/legacy$id.jpg",
        photoUrl = photoUrl,
        noteContent = "",
        latitude = 37.0,
        longitude = -122.0,
        accuracy = 5.0,
        createAt = 0L,
    ).apply { this.id = id }

    private fun session() =
        SessionEntity(
            uuid = "session-uuid-1",
            originUserId = "user-uuid",
            originWallet = "wallet",
            destinationWallet = "dest-wallet",
            startTime = Instant.parse("2023-01-01T00:00:00Z"),
            organization = "org",
            isUploaded = false,
        ).apply { id = 1L }

    @Test
    fun `WHEN uploadLegacyTrees called THEN processes legacy tree captures`() =
        runTest {
            val legacyTree =
                TreeCaptureEntity(
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