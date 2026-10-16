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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.analytics.ExceptionDataCollector
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
import java.io.IOException
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

    @MockK(relaxed = true)
    private lateinit var exceptionDataCollector: ExceptionDataCollector

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
                exceptionDataCollector = exceptionDataCollector,
            )
    }

    @After
    fun tearDown() {
        unmockkObject(DeviceUtils)
    }

    private fun tree(
        id: Long,
        photoUrl: String? = null,
    ) = TreeEntity(
        uuid = "tree-uuid-$id",
        sessionId = 1L,
        photoPath = "/test/photo-$id.jpg",
        photoUrl = photoUrl,
        note = "test note",
        latitude = 37.0,
        longitude = -122.0,
        createdAt = Instant.parse("2023-01-01T00:00:00Z"),
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
    fun `WHEN a bundle fails with a network error THEN it is recorded and does not abort the sync`() =
        runTest {
            coEvery { dao.getTreesByIds(any()) } throws IOException("No internet")

            treeUploader.uploadTrees(listOf(1L))

            val recorded = slot<Throwable>()
            coVerify(exactly = 1) { exceptionDataCollector.recordFailure(capture(recorded), any(), any()) }
            assertTrue(recorded.captured is IOException)
        }

    @Test
    fun `WHEN a bundle fails with a parsing error THEN it is recorded and does not abort the sync`() =
        runTest {
            coEvery { dao.getTreesByIds(any()) } throws SerializationException("JSON error")

            treeUploader.uploadTrees(listOf(1L))

            val recorded = slot<Throwable>()
            coVerify(exactly = 1) { exceptionDataCollector.recordFailure(capture(recorded), any(), any()) }
            assertTrue(recorded.captured is SerializationException)
        }

    @Test
    fun `WHEN one bundle fails THEN the remaining bundles are still uploaded`() =
        runTest {
            // Bundles hold 50 trees, so 51 trees are split into two bundles.
            val failingBundle = (1L..50L).toList()
            coEvery { dao.getTreesByIds(failingBundle) } throws IOException("No internet")
            coEvery { dao.getTreesByIds(listOf(51L)) } returns listOf(tree(51L, photoUrl = "https://existing.url/photo.jpg"))
            coEvery { dao.getSessionById(1L) } returns session()

            treeUploader.uploadTrees(failingBundle + 51L)

            coVerify(exactly = 1) { exceptionDataCollector.recordFailure(any(), any(), any()) }
            coVerify(exactly = 1) { objectStorageClient.uploadBundle(any(), any()) }
            coVerify(exactly = 1) { dao.updateTreesUploadStatus(listOf(51L), true) }
        }

    @Test
    fun `WHEN a tree photo fails to upload THEN the tree is held back for the next sync`() =
        runTest {
            coEvery { dao.getTreesByIds(listOf(1L)) } returns listOf(tree(1L))
            // UploadImageUseCase returns null after recording the failure itself.
            coEvery { uploadImageUseCase.execute(any()) } returns null

            treeUploader.uploadTrees(listOf(1L))

            // The tree must not be sent without its photo, marked uploaded, or lose its local photo.
            coVerify(exactly = 0) { objectStorageClient.uploadBundle(any(), any()) }
            coVerify(exactly = 0) { dao.updateTreesUploadStatus(any(), any()) }
            coVerify(exactly = 0) { dao.removeTreesLocalImagePaths(any()) }
            // The held-back bundle is reported once (the photo failure itself is reported by UploadImageUseCase).
            coVerify(exactly = 1) { exceptionDataCollector.recordFailure(match { it is IllegalStateException }, any(), any()) }
        }

    @Test
    fun `WHEN a legacy tree photo fails to upload THEN the tree is held back for the next sync`() =
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
            coEvery { dao.getTreeCapturesByIds(listOf(1L)) } returns listOf(legacyTree)
            coEvery { uploadImageUseCase.execute(any()) } returns null

            treeUploader.uploadLegacyTrees(listOf(1L), "instance-123")

            coVerify(exactly = 0) { objectStorageClient.uploadBundle(any(), any()) }
            coVerify(exactly = 0) { dao.updateTreeCapturesUploadStatus(any(), any()) }
            coVerify(exactly = 0) { dao.removeTreeCapturesLocalImagePaths(any()) }
            coVerify(exactly = 1) { exceptionDataCollector.recordFailure(match { it is IllegalStateException }, any(), any()) }
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