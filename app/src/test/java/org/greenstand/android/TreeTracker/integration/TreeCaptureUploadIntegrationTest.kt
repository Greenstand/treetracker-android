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
package org.greenstand.android.TreeTracker.integration

import com.amazonaws.AmazonClientException
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.greenstand.android.TreeTracker.analytics.Analytics
import org.greenstand.android.TreeTracker.api.ObjectStorageClient
import org.greenstand.android.TreeTracker.database.DbFixtures
import org.greenstand.android.TreeTracker.database.InMemoryDatabaseTest
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.models.Tree
import org.greenstand.android.TreeTracker.models.TreeUploader
import org.greenstand.android.TreeTracker.usecases.CreateTreeRequestUseCase
import org.greenstand.android.TreeTracker.usecases.CreateTreeUseCase
import org.greenstand.android.TreeTracker.usecases.UploadImageUseCase
import org.greenstand.android.TreeTracker.utilities.DeviceUtils
import org.greenstand.android.TreeTracker.utilities.TimeProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.UUID

/**
 * Captures trees with [CreateTreeUseCase] into a real in-memory Room database and uploads them
 * with [TreeUploader], faking only S3. Covers the path a planted tree takes from the capture
 * screen to the upload bundle, for v2 trees and for legacy tree captures.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class TreeCaptureUploadIntegrationTest : InMemoryDatabaseTest() {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private val json =
        Json {
            explicitNulls = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    private val capturedAt = Instant.parse("2024-05-06T07:08:09Z")

    private val uploadedBundles = mutableListOf<Pair<String, String>>()
    private val uploadedImages = mutableListOf<String>()
    private val failingImages = mutableSetOf<String>()
    private val storageClient: ObjectStorageClient = mockk()

    private val dao: TreeTrackerDAO get() = database.treeTrackerDao()

    private val timeProvider: TimeProvider = mockk { every { currentTime() } answers { capturedAt } }

    private fun createTree() = CreateTreeUseCase(dao, mockk<Analytics>(relaxed = true), timeProvider)

    private fun uploader() =
        TreeUploader(
            uploadImageUseCase = UploadImageUseCase(storageClient),
            objectStorageClient = storageClient,
            createTreeRequestUseCase = CreateTreeRequestUseCase(dao),
            dao = dao,
            json = json,
        )

    @Before
    fun setUpFakes() {
        every { storageClient.put(any(), any(), any()) } answers {
            val path = firstArg<String>()
            if (path in failingImages) throw AmazonClientException("offline")
            uploadedImages += path
            "https://images.example/${File(path).name}"
        }
        every { storageClient.uploadBundle(any(), any()) } answers {
            uploadedBundles += secondArg<String>() to firstArg<String>()
        }
        mockkObject(DeviceUtils)
        every { DeviceUtils.deviceId } returns "device-id"
    }

    @After
    fun tearDownFakes() {
        unmockkAll()
    }

    private fun bundleJson(index: Int): JsonObject = json.parseToJsonElement(uploadedBundles[index].second).jsonObject

    private fun photo(name: String): File = tempFolder.newFile(name).apply { writeText("jpeg") }

    private suspend fun captureTree(
        sessionId: Long,
        photo: File,
        note: String = "by the river",
    ): Long =
        createTree().execute(
            Tree(
                treeUuid = UUID.nameUUIDFromBytes(photo.name.toByteArray()),
                sessionId = sessionId,
                content = note,
                photoPath = photo.absolutePath,
                meanLongitude = 36.8,
                meanLatitude = -1.3,
            ).apply { addTreeAttribute(Tree.TREE_COLOR_ATTR_KEY, "green") },
        )

    @Test
    fun `captured tree is stored pending with its attributes and capture time`() =
        runTest {
            val sessionId = dao.insertSession(DbFixtures.session())

            val treeId = captureTree(sessionId, photo("a.jpg"), note = "first")

            val stored = dao.getTreesByIds(listOf(treeId)).single()
            assertFalse(stored.uploaded)
            assertNull(stored.photoUrl)
            assertEquals("first", stored.note)
            assertEquals(capturedAt, stored.createdAt)
            assertEquals(mapOf(Tree.TREE_COLOR_ATTR_KEY to "green"), stored.extraAttributes)
            assertEquals(listOf(treeId), dao.getAllTreeIdsToUpload())
        }

    @Test
    fun `uploading a captured tree sends its image and a captures bundle, then clears the local photo`() =
        runTest {
            val sessionId = dao.insertSession(DbFixtures.session(uuid = "session-uuid-1"))
            val photo = photo("tree.jpg")
            val treeId = captureTree(sessionId, photo)

            uploader().uploadTrees(dao.getAllTreeIdsToUpload())

            assertEquals(listOf(photo.absolutePath), uploadedImages)
            val capture = bundleJson(0)["captures"]!!.jsonArray.single().jsonObject
            assertEquals("session-uuid-1", capture["session_id"]!!.jsonPrimitive.content)
            assertEquals("https://images.example/tree.jpg", capture["image_url"]!!.jsonPrimitive.content)
            assertEquals(capturedAt.toString(), capture["captured_at"]!!.jsonPrimitive.content)
            assertTrue(uploadedBundles[0].first.endsWith("_captures"))

            val stored = dao.getTreesByIds(listOf(treeId)).single()
            assertTrue(stored.uploaded)
            assertEquals("https://images.example/tree.jpg", stored.photoUrl)
            assertEquals(uploadedBundles[0].first, stored.bundleId)
            assertNull(stored.photoPath)
            assertFalse(photo.exists())
            assertTrue(dao.getAllTreeIdsToUpload().isEmpty())
        }

    @Test
    fun `a failed image upload leaves that bundle pending and keeps the photo for a retry`() =
        runTest {
            val sessionId = dao.insertSession(DbFixtures.session())
            val photo = photo("offline.jpg")
            val treeId = captureTree(sessionId, photo)
            failingImages += photo.absolutePath

            uploader().uploadTrees(dao.getAllTreeIdsToUpload())

            assertTrue(uploadedBundles.isEmpty())
            assertTrue(photo.exists())
            assertEquals(listOf(treeId), dao.getAllTreeIdsToUpload())

            failingImages.clear()
            uploader().uploadTrees(dao.getAllTreeIdsToUpload())

            assertTrue(dao.getAllTreeIdsToUpload().isEmpty())
            assertFalse(photo.exists())
        }

    @Test
    fun `a tree whose image was already uploaded is not uploaded again`() =
        runTest {
            val sessionId = dao.insertSession(DbFixtures.session())
            val treeId =
                dao.insertTree(DbFixtures.tree(sessionId = sessionId, photoPath = null, photoUrl = "https://images.example/old.jpg"))

            uploader().uploadTrees(listOf(treeId))

            assertTrue(uploadedImages.isEmpty())
            val capture = bundleJson(0)["captures"]!!.jsonArray.single().jsonObject
            assertEquals("https://images.example/old.jpg", capture["image_url"]!!.jsonPrimitive.content)
            assertTrue(dao.getTreesByIds(listOf(treeId)).single().uploaded)
        }

    @Test
    fun `more than fifty trees go up in bundles of fifty`() =
        runTest {
            val sessionId = dao.insertSession(DbFixtures.session())
            repeat(51) { captureTree(sessionId, photo("tree-$it.jpg")) }

            uploader().uploadTrees(dao.getAllTreeIdsToUpload())

            assertEquals(listOf(50, 1), uploadedBundles.indices.map { bundleJson(it)["captures"]!!.jsonArray.size })
            assertEquals(2, uploadedBundles.map { it.first }.toSet().size)
            assertTrue(dao.getAllTreeIdsToUpload().isEmpty())
        }

    @Test
    fun `legacy tree captures upload with their planter and attributes in a v1 bundle`() =
        runTest {
            val planterInfoId = dao.insertPlanterInfo(DbFixtures.planterInfo(identifier = "planter@example.com"))
            val checkInId = dao.insertPlanterCheckIn(DbFixtures.planterCheckIn(planterInfoId, photoUrl = "https://images.example/selfie.jpg"))
            val photo = photo("legacy.jpg")
            val captureId = dao.insertTreeCapture(DbFixtures.treeCapture(checkInId, localPhotoPath = photo.absolutePath))
            dao.insertTreeAttribute(DbFixtures.treeAttribute(captureId, key = "height", value = "3"))

            uploader().uploadLegacyTrees(dao.getAllTreeCaptureIdsToUpload(), instanceId = "instance-1")

            val bundle = bundleJson(0)
            assertEquals("1", bundle["pack_format_version"]!!.jsonPrimitive.content)
            val tree = bundle["trees"]!!.jsonArray.single().jsonObject
            assertEquals("capture-uuid", tree["uuid"]!!.jsonPrimitive.content)
            assertEquals("planter@example.com", tree["planter_identifier"]!!.jsonPrimitive.content)
            assertEquals("https://images.example/legacy.jpg", tree["image_url"]!!.jsonPrimitive.content)
            val attribute = tree["attributes"]!!.jsonArray.single().jsonObject
            assertEquals("height", attribute["key"]!!.jsonPrimitive.content)
            assertEquals("3", attribute["value"]!!.jsonPrimitive.content)

            val stored = dao.getTreeCaptureById(captureId)
            assertTrue(stored.uploaded)
            assertNull(stored.localPhotoPath)
            assertEquals(uploadedBundles[0].first, stored.bundleId)
            assertFalse(photo.exists())
            assertTrue(dao.getAllTreeCaptureIdsToUpload().isEmpty())
        }
}