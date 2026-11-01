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
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.api.ObjectStorageClient
import org.greenstand.android.TreeTracker.api.models.requests.NewTreeRequest
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.database.entity.SessionEntity
import org.greenstand.android.TreeTracker.database.entity.TreeEntity
import org.greenstand.android.TreeTracker.database.legacy.entity.TreeCaptureEntity
import org.greenstand.android.TreeTracker.usecases.CreateTreeRequestParams
import org.greenstand.android.TreeTracker.usecases.CreateTreeRequestUseCase
import org.greenstand.android.TreeTracker.usecases.UploadImageParams
import org.greenstand.android.TreeTracker.usecases.UploadImageUseCase
import org.greenstand.android.TreeTracker.utilities.DeviceUtils
import org.greenstand.android.TreeTracker.utilities.md5
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
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

    @get:Rule
    val tempFolder = TemporaryFolder()

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

    @Test
    fun `WHEN trees uploaded THEN bundle contains capture requests built from trees and sessions`() =
        runTest {
            val bundleSlot = slot<String>()
            every { objectStorageClient.uploadBundle(capture(bundleSlot), any()) } returns Unit
            stubTrees(listOf(treeEntity(id = 1L, photoUrl = "https://existing.url/photo.jpg")))

            treeUploader.uploadTrees(listOf(1L))

            val bundle = parse(bundleSlot.captured)
            assertEquals("2", bundle["pack_format_version"]!!.jsonPrimitive.content)
            assertEquals("test-device-id", bundle["device_id"]!!.jsonPrimitive.content)
            val treeCapture = bundle["captures"]!!.jsonArray.single().jsonObject
            assertEquals("session-uuid-1", treeCapture["session_id"]!!.jsonPrimitive.content)
            assertEquals("tree-uuid-1", treeCapture["id"]!!.jsonPrimitive.content)
            assertEquals(37.0, treeCapture["lat"]!!.jsonPrimitive.double)
            assertEquals(-122.0, treeCapture["lon"]!!.jsonPrimitive.double)
            assertEquals("test note", treeCapture["note"]!!.jsonPrimitive.content)
            assertEquals("https://existing.url/photo.jpg", treeCapture["image_url"]!!.jsonPrimitive.content)
            assertEquals("2023-01-01T00:00:00Z", treeCapture["captured_at"]!!.jsonPrimitive.content)
            assertEquals(JsonNull, treeCapture["abs_step_count"])
            assertEquals(JsonNull, treeCapture["delta_step_count"])
            assertEquals(JsonNull, treeCapture["rotation_matrix"])
            assertEquals(JsonNull, treeCapture["extra_attributes"])
        }

    @Test
    fun `WHEN trees uploaded THEN records bundle id uploads bundle then marks uploaded and clears local paths`() =
        runTest {
            val bundleSlot = slot<String>()
            val bundleIdSlot = slot<String>()
            every { objectStorageClient.uploadBundle(capture(bundleSlot), capture(bundleIdSlot)) } returns Unit
            stubTrees(listOf(treeEntity(id = 1L, photoUrl = "https://existing.url/photo.jpg")))

            treeUploader.uploadTrees(listOf(1L))

            val expectedBundleId = "${bundleSlot.captured.md5()}_captures"
            assertEquals(expectedBundleId, bundleIdSlot.captured)
            coVerifyOrder {
                dao.updateTreesBundleIds(listOf(1L), expectedBundleId)
                objectStorageClient.uploadBundle(any(), expectedBundleId)
                dao.updateTreesUploadStatus(listOf(1L), true)
                dao.removeTreesLocalImagePaths(listOf(1L))
            }
        }

    @Test
    fun `WHEN tree image uploaded THEN stores the image url before bundling it`() =
        runTest {
            val bundleSlot = slot<String>()
            every { objectStorageClient.uploadBundle(capture(bundleSlot), any()) } returns Unit
            coEvery { uploadImageUseCase.execute(any()) } returns "https://uploaded.url/photo.jpg"
            stubTrees(listOf(treeEntity(id = 1L, photoUrl = null, photoPath = "/test/photo.jpg")))

            treeUploader.uploadTrees(listOf(1L))

            coVerify { uploadImageUseCase.execute(UploadImageParams(imagePath = "/test/photo.jpg", lat = 37.0, long = -122.0)) }
            coVerify { dao.updateTree(match { it.id == 1L && it.photoUrl == "https://uploaded.url/photo.jpg" }) }
            val treeCapture = parse(bundleSlot.captured)["captures"]!!.jsonArray.single().jsonObject
            assertEquals("https://uploaded.url/photo.jpg", treeCapture["image_url"]!!.jsonPrimitive.content)
        }

    @Test
    fun `WHEN tree image upload returns no url THEN skips the bundle and does not mark trees uploaded`() =
        runTest {
            coEvery { uploadImageUseCase.execute(any()) } returns null
            stubTrees(listOf(treeEntity(id = 1L, photoUrl = null)))

            treeUploader.uploadTrees(listOf(1L))

            coVerify(exactly = 0) { dao.updateTree(any()) }
            coVerify(exactly = 0) { objectStorageClient.uploadBundle(any(), any()) }
            coVerify(exactly = 0) { dao.updateTreesUploadStatus(any(), any()) }
        }

    @Test
    fun `WHEN trees uploaded THEN deletes their local image files`() =
        runTest {
            val photo = tempFolder.newFile("tree.jpg")
            stubTrees(listOf(treeEntity(id = 1L, photoUrl = "https://existing.url/photo.jpg", photoPath = photo.absolutePath)))

            treeUploader.uploadTrees(listOf(1L))

            assertFalse(photo.exists())
        }

    @Test
    fun `WHEN bundle upload fails THEN keeps local images and does not mark trees uploaded`() =
        runTest {
            val photo = tempFolder.newFile("tree.jpg")
            every { objectStorageClient.uploadBundle(any(), any()) } throws RuntimeException("offline")
            stubTrees(listOf(treeEntity(id = 1L, photoUrl = "https://existing.url/photo.jpg", photoPath = photo.absolutePath)))

            treeUploader.uploadTrees(listOf(1L))

            assertTrue(photo.exists())
            coVerify(exactly = 0) { dao.updateTreesUploadStatus(any(), any()) }
            coVerify(exactly = 0) { dao.removeTreesLocalImagePaths(any()) }
        }

    @Test
    fun `WHEN more than fifty trees THEN uploads them in windows of fifty`() =
        runTest {
            val treeIds = (1L..120L).toList()
            coEvery { dao.getTreesByIds(any()) } returns emptyList()

            treeUploader.uploadTrees(treeIds)

            coVerifyOrder {
                dao.getTreesByIds((1L..50L).toList())
                dao.getTreesByIds((51L..100L).toList())
                dao.getTreesByIds((101L..120L).toList())
            }
            coVerify(exactly = 3) { dao.getTreesByIds(any()) }
        }

    @Test
    fun `WHEN one window fails THEN the following windows are still uploaded`() =
        runTest {
            val treeIds = (1L..60L).toList()
            coEvery { dao.getTreesByIds((1L..50L).toList()) } throws RuntimeException("db error")
            stubTrees(listOf(treeEntity(id = 51L, photoUrl = "https://existing.url/photo.jpg")), ids = (51L..60L).toList())

            treeUploader.uploadTrees(treeIds)

            coVerify(exactly = 1) { objectStorageClient.uploadBundle(any(), any()) }
            coVerify(exactly = 1) { dao.updateTreesUploadStatus(listOf(51L), true) }
        }

    @Test
    fun `WHEN tree id list is empty THEN nothing is read or uploaded`() =
        runTest {
            treeUploader.uploadTrees(emptyList())
            treeUploader.uploadLegacyTrees(emptyList(), "instance-123")

            coVerify(exactly = 0) { dao.getTreesByIds(any()) }
            coVerify(exactly = 0) { dao.getTreeCapturesByIds(any()) }
            coVerify(exactly = 0) { objectStorageClient.uploadBundle(any(), any()) }
        }

    @Test
    fun `WHEN legacy trees uploaded THEN bundle is a v1 bundle with tree requests and the device`() =
        runTest {
            val bundleSlot = slot<String>()
            every { objectStorageClient.uploadBundle(capture(bundleSlot), any()) } returns Unit
            stubLegacyTrees(listOf(legacyTree(id = 1L, photoUrl = "https://existing.url/legacy.jpg")))
            coEvery { createTreeRequestUseCase.execute(any()) } returns
                NewTreeRequest(uuid = "legacy-uuid", imageUrl = "https://existing.url/legacy.jpg", sequenceId = 1L)

            treeUploader.uploadLegacyTrees(listOf(1L), "instance-123")

            val bundle = parse(bundleSlot.captured)
            assertEquals("1", bundle["pack_format_version"]!!.jsonPrimitive.content)
            val tree = bundle["trees"]!!.jsonArray.single().jsonObject
            assertEquals("legacy-uuid", tree["uuid"]!!.jsonPrimitive.content)
            assertEquals("https://existing.url/legacy.jpg", tree["image_url"]!!.jsonPrimitive.content)
            val device = bundle["devices"]!!.jsonArray.single().jsonObject
            assertEquals("instance-123", device["instanceId"]!!.jsonPrimitive.content)
            assertEquals("test-device-id", device["device_identifier"]!!.jsonPrimitive.content)
            assertEquals(JsonNull, bundle["captures"])
        }

    @Test
    fun `WHEN legacy trees uploaded THEN bundle id is the plain md5 and steps run in order`() =
        runTest {
            val bundleSlot = slot<String>()
            val bundleIdSlot = slot<String>()
            every { objectStorageClient.uploadBundle(capture(bundleSlot), capture(bundleIdSlot)) } returns Unit
            stubLegacyTrees(listOf(legacyTree(id = 1L, photoUrl = "https://existing.url/legacy.jpg")))
            coEvery { createTreeRequestUseCase.execute(any()) } returns NewTreeRequest(uuid = "legacy-uuid")

            treeUploader.uploadLegacyTrees(listOf(1L), "instance-123")

            val expectedBundleId = bundleSlot.captured.md5()
            assertEquals(expectedBundleId, bundleIdSlot.captured)
            coVerifyOrder {
                dao.updateTreeCapturesBundleIds(listOf(1L), expectedBundleId)
                objectStorageClient.uploadBundle(any(), expectedBundleId)
                dao.updateTreeCapturesUploadStatus(listOf(1L), true)
                dao.removeTreeCapturesLocalImagePaths(listOf(1L))
            }
        }

    @Test
    fun `WHEN legacy tree already has an image url THEN builds request with it without uploading the image`() =
        runTest {
            stubLegacyTrees(listOf(legacyTree(id = 1L, photoUrl = "https://existing.url/legacy.jpg")))
            coEvery { createTreeRequestUseCase.execute(any()) } returns NewTreeRequest(uuid = "legacy-uuid")

            treeUploader.uploadLegacyTrees(listOf(1L), "instance-123")

            coVerify(exactly = 0) { uploadImageUseCase.execute(any()) }
            coVerify { createTreeRequestUseCase.execute(CreateTreeRequestParams(1L, "https://existing.url/legacy.jpg")) }
        }

    @Test
    fun `WHEN legacy tree image uploaded THEN saves the url on the tree capture`() =
        runTest {
            stubLegacyTrees(listOf(legacyTree(id = 1L, photoUrl = null)))
            coEvery { uploadImageUseCase.execute(any()) } returns "https://uploaded.url/legacy.jpg"
            coEvery { createTreeRequestUseCase.execute(any()) } returns NewTreeRequest(uuid = "legacy-uuid")

            treeUploader.uploadLegacyTrees(listOf(1L), "instance-123")

            coVerify { uploadImageUseCase.execute(UploadImageParams(imagePath = "/test/legacy.jpg", lat = 37.0, long = -122.0)) }
            coVerify { dao.updateTreeCapture(match { it.id == 1L && it.photoUrl == "https://uploaded.url/legacy.jpg" }) }
            coVerify { createTreeRequestUseCase.execute(CreateTreeRequestParams(1L, "https://uploaded.url/legacy.jpg")) }
        }

    @Test
    fun `WHEN legacy tree image upload returns no url THEN skips the bundle`() =
        runTest {
            stubLegacyTrees(listOf(legacyTree(id = 1L, photoUrl = null)))
            coEvery { uploadImageUseCase.execute(any()) } returns null

            treeUploader.uploadLegacyTrees(listOf(1L), "instance-123")

            coVerify(exactly = 0) { createTreeRequestUseCase.execute(any()) }
            coVerify(exactly = 0) { objectStorageClient.uploadBundle(any(), any()) }
            coVerify(exactly = 0) { dao.updateTreeCapturesUploadStatus(any(), any()) }
        }

    @Test
    fun `WHEN legacy trees uploaded THEN deletes their local image files`() =
        runTest {
            val photo = tempFolder.newFile("legacy.jpg")
            stubLegacyTrees(
                listOf(legacyTree(id = 1L, photoUrl = "https://existing.url/legacy.jpg", localPhotoPath = photo.absolutePath)),
            )
            coEvery { createTreeRequestUseCase.execute(any()) } returns NewTreeRequest(uuid = "legacy-uuid")

            treeUploader.uploadLegacyTrees(listOf(1L), "instance-123")

            assertFalse(photo.exists())
        }

    private fun parse(bundle: String): JsonObject = json.parseToJsonElement(bundle).jsonObject

    private fun stubTrees(
        trees: List<TreeEntity>,
        ids: List<Long> = trees.map { it.id },
    ) {
        coEvery { dao.getTreesByIds(ids) } returns trees
        coEvery { dao.getSessionById(1L) } returns
            SessionEntity(
                uuid = "session-uuid-1",
                originUserId = "user-uuid",
                originWallet = "wallet",
                destinationWallet = "dest-wallet",
                startTime = Instant.parse("2023-01-01T00:00:00Z"),
                organization = "org",
                isUploaded = false,
            ).apply { id = 1L }
    }

    private fun stubLegacyTrees(trees: List<TreeCaptureEntity>) {
        coEvery { dao.getTreeCapturesByIds(trees.map { it.id }) } returns trees
    }

    private fun treeEntity(
        id: Long,
        photoUrl: String?,
        photoPath: String? = "/test/photo.jpg",
    ): TreeEntity =
        TreeEntity(
            uuid = "tree-uuid-$id",
            sessionId = 1L,
            photoPath = photoPath,
            photoUrl = photoUrl,
            note = "test note",
            latitude = 37.0,
            longitude = -122.0,
            createdAt = Instant.parse("2023-01-01T00:00:00Z"),
        ).apply { this.id = id }

    private fun legacyTree(
        id: Long,
        photoUrl: String?,
        localPhotoPath: String? = "/test/legacy.jpg",
    ): TreeCaptureEntity =
        TreeCaptureEntity(
            uuid = "legacy-uuid-$id",
            planterCheckInId = 1L,
            localPhotoPath = localPhotoPath,
            photoUrl = photoUrl,
            noteContent = "legacy note",
            latitude = 37.0,
            longitude = -122.0,
            accuracy = 5.0,
            createAt = 1_672_531_200L,
        ).apply { this.id = id }
}