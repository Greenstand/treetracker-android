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
package org.greenstand.android.TreeTracker.database.dao

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.greenstand.android.TreeTracker.database.DbFixtures
import org.greenstand.android.TreeTracker.database.InMemoryDatabaseTest
import org.greenstand.android.TreeTracker.database.valuesBeforeAndAfter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Tests for the [TreeDAO] queries on the legacy `tree_capture` and `tree_attribute` tables. */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class TreeDaoLegacyCaptureTest : InMemoryDatabaseTest() {
    private val dao: TreeDAO get() = database.treeDao()

    private suspend fun insertCheckIn(): Long {
        val planterId = database.planterDao().insertPlanterInfo(DbFixtures.planterInfo())
        return database.planterDao().insertPlanterCheckIn(DbFixtures.planterCheckIn(planterId))
    }

    @Test
    fun `insertTreeCapture stores every field and returns the generated id`() =
        runTest {
            val capture = DbFixtures.treeCapture(insertCheckIn(), photoUrl = "https://photo", uploaded = true)

            val id = dao.insertTreeCapture(capture)

            val stored = dao.getTreeCaptureById(id)
            assertEquals(capture, stored)
            assertEquals(id, stored.id)
        }

    @Test
    fun `insertTreeCapture fails when the check in does not exist`() =
        runTest {
            val result = runCatching { dao.insertTreeCapture(DbFixtures.treeCapture(planterCheckInId = 77)) }

            assertTrue(result.isFailure)
            assertTrue(dao.getAllTreeCaptures().isEmpty())
        }

    @Test
    fun `insertTreeCapture with an existing id replaces the row`() =
        runTest {
            val checkInId = insertCheckIn()
            val id = dao.insertTreeCapture(DbFixtures.treeCapture(checkInId, uuid = "old"))

            dao.insertTreeCapture(DbFixtures.treeCapture(checkInId, uuid = "new").apply { this.id = id })

            assertEquals(listOf("new"), dao.getAllTreeCaptures().map { it.uuid })
        }

    @Test
    fun `getAllTreeCaptures returns every capture`() =
        runTest {
            val checkInId = insertCheckIn()
            dao.insertTreeCapture(DbFixtures.treeCapture(checkInId, uuid = "a"))
            dao.insertTreeCapture(DbFixtures.treeCapture(checkInId, uuid = "b"))

            assertEquals(setOf("a", "b"), dao.getAllTreeCaptures().map { it.uuid }.toSet())
        }

    @Test
    fun `getTreeCapturesByIds returns only the requested captures`() =
        runTest {
            val checkInId = insertCheckIn()
            val first = dao.insertTreeCapture(DbFixtures.treeCapture(checkInId, uuid = "a"))
            dao.insertTreeCapture(DbFixtures.treeCapture(checkInId, uuid = "b"))

            assertEquals(listOf("a"), dao.getTreeCapturesByIds(listOf(first, 999L)).map { it.uuid })
        }

    @Test
    fun `getTreeDataForMap returns the location and id of every capture`() =
        runTest {
            val checkInId = insertCheckIn()
            val first = dao.insertTreeCapture(DbFixtures.treeCapture(checkInId, latitude = 1.5, longitude = -2.5))
            val second = dao.insertTreeCapture(DbFixtures.treeCapture(checkInId, latitude = 3.25, longitude = 4.75))

            val markers = dao.getTreeDataForMap().sortedBy { it.treeCaptureId }

            assertEquals(listOf(first, second), markers.map { it.treeCaptureId })
            assertEquals(listOf(1.5, 3.25), markers.map { it.latitude })
            assertEquals(listOf(-2.5, 4.75), markers.map { it.longitude })
        }

    @Test
    fun `getTreeDataForMap is empty without captures`() =
        runTest {
            assertTrue(dao.getTreeDataForMap().isEmpty())
        }

    @Test
    fun `capture upload counts split captures by the uploaded flag`() =
        runTest {
            val checkInId = insertCheckIn()
            dao.insertTreeCapture(DbFixtures.treeCapture(checkInId, uploaded = true))
            dao.insertTreeCapture(DbFixtures.treeCapture(checkInId, uploaded = true))
            dao.insertTreeCapture(DbFixtures.treeCapture(checkInId, uploaded = false))

            assertEquals(2, dao.getUploadedTreeCaptureCount())
            assertEquals(1, dao.getNonUploadedTreeCaptureCount())
        }

    @Test
    fun `capture image counts split captures by whether they have a photo url`() =
        runTest {
            val checkInId = insertCheckIn()
            dao.insertTreeCapture(DbFixtures.treeCapture(checkInId, photoUrl = "https://a"))
            dao.insertTreeCapture(DbFixtures.treeCapture(checkInId, photoUrl = null))
            dao.insertTreeCapture(DbFixtures.treeCapture(checkInId, photoUrl = null))

            assertEquals(1, dao.getUploadedLegacyTreeImageCount())
            assertEquals(2, dao.getNonUploadedLegacyTreeCaptureImageCount())
            assertEquals(1, dao.getUploadedLegacyTreeImageCountFlow().first())
            assertEquals(2, dao.getNonUploadedLegacyTreeCaptureImageCountFlow().first())
        }

    @Test
    fun `capture counts are zero for an empty table`() =
        runTest {
            assertEquals(0, dao.getUploadedTreeCaptureCount())
            assertEquals(0, dao.getNonUploadedTreeCaptureCount())
            assertEquals(0, dao.getUploadedLegacyTreeImageCount())
            assertEquals(0, dao.getNonUploadedLegacyTreeCaptureImageCount())
        }

    @Test
    fun `getNonUploadedLegacyTreeCaptureImageCountFlow emits again when a capture is added`() =
        runTest {
            val checkInId = insertCheckIn()

            val (before, after) =
                dao.getNonUploadedLegacyTreeCaptureImageCountFlow().valuesBeforeAndAfter {
                    dao.insertTreeCapture(DbFixtures.treeCapture(checkInId, photoUrl = null))
                }

            assertEquals(0, before)
            assertEquals(1, after)
        }

    @Test
    fun `getUploadedLegacyTreeImageCountFlow emits again when a capture gets a photo url`() =
        runTest {
            val checkInId = insertCheckIn()
            val id = dao.insertTreeCapture(DbFixtures.treeCapture(checkInId, photoUrl = null))
            val uploaded = DbFixtures.treeCapture(checkInId, photoUrl = "https://photo").apply { this.id = id }

            val (before, after) = dao.getUploadedLegacyTreeImageCountFlow().valuesBeforeAndAfter { dao.updateTreeCapture(uploaded) }

            assertEquals(0, before)
            assertEquals(1, after)
        }

    @Test
    fun `getAllTreeCaptureIdsToUpload returns ids of captures that are not uploaded`() =
        runTest {
            val checkInId = insertCheckIn()
            val pending = dao.insertTreeCapture(DbFixtures.treeCapture(checkInId, uploaded = false))
            dao.insertTreeCapture(DbFixtures.treeCapture(checkInId, uploaded = true))

            assertEquals(listOf(pending), dao.getAllTreeCaptureIdsToUpload())
        }

    @Test
    fun `updateTreeCapturesBundleIds sets the bundle id only on the given captures`() =
        runTest {
            val checkInId = insertCheckIn()
            val first = dao.insertTreeCapture(DbFixtures.treeCapture(checkInId))
            val second = dao.insertTreeCapture(DbFixtures.treeCapture(checkInId))

            dao.updateTreeCapturesBundleIds(listOf(second), "bundle-2")

            assertNull(dao.getTreeCaptureById(first).bundleId)
            assertEquals("bundle-2", dao.getTreeCaptureById(second).bundleId)
        }

    @Test
    fun `updateTreeCapturesUploadStatus marks the given captures as uploaded`() =
        runTest {
            val checkInId = insertCheckIn()
            val first = dao.insertTreeCapture(DbFixtures.treeCapture(checkInId))
            val second = dao.insertTreeCapture(DbFixtures.treeCapture(checkInId))

            dao.updateTreeCapturesUploadStatus(listOf(first), true)

            assertTrue(dao.getTreeCaptureById(first).uploaded)
            assertFalse(dao.getTreeCaptureById(second).uploaded)
            assertEquals(listOf(second), dao.getAllTreeCaptureIdsToUpload())
        }

    @Test
    fun `removeTreeCapturesLocalImagePaths clears the local photo only for the given captures`() =
        runTest {
            val checkInId = insertCheckIn()
            val first = dao.insertTreeCapture(DbFixtures.treeCapture(checkInId, localPhotoPath = "/a.jpg"))
            val second = dao.insertTreeCapture(DbFixtures.treeCapture(checkInId, localPhotoPath = "/b.jpg"))

            dao.removeTreeCapturesLocalImagePaths(listOf(first))

            assertNull(dao.getTreeCaptureById(first).localPhotoPath)
            assertEquals("/b.jpg", dao.getTreeCaptureById(second).localPhotoPath)
        }

    @Test
    fun `updateTreeCapture updates the row with the same id`() =
        runTest {
            val checkInId = insertCheckIn()
            val id = dao.insertTreeCapture(DbFixtures.treeCapture(checkInId))
            val updated = DbFixtures.treeCapture(checkInId).copy(noteContent = "edited").apply { this.id = id }

            dao.updateTreeCapture(updated)

            assertEquals(updated, dao.getTreeCaptureById(id))
        }

    @Test
    fun `deleteTreeCapture removes the capture`() =
        runTest {
            val checkInId = insertCheckIn()
            val first = dao.insertTreeCapture(DbFixtures.treeCapture(checkInId, uuid = "a"))
            dao.insertTreeCapture(DbFixtures.treeCapture(checkInId, uuid = "b"))

            dao.deleteTreeCapture(dao.getTreeCaptureById(first))

            assertEquals(listOf("b"), dao.getAllTreeCaptures().map { it.uuid })
        }

    @Test
    fun `insertTreeAttribute stores an attribute for the capture`() =
        runTest {
            val captureId = dao.insertTreeCapture(DbFixtures.treeCapture(insertCheckIn()))
            val attribute = DbFixtures.treeAttribute(captureId, key = "species", value = "oak")

            val id = dao.insertTreeAttribute(attribute)

            val stored = dao.getTreeAttributeByTreeCaptureId(captureId).single()
            assertEquals(attribute, stored)
            assertEquals(id, stored.id)
        }

    @Test
    fun `insertTreeAttribute fails when the capture does not exist`() =
        runTest {
            val result = runCatching { dao.insertTreeAttribute(DbFixtures.treeAttribute(treeCaptureId = 55)) }

            assertTrue(result.isFailure)
            assertTrue(dao.getTreeAttributeByTreeCaptureId(55).isEmpty())
        }

    @Test
    fun `getTreeAttributeByTreeCaptureId returns only that capture's attributes`() =
        runTest {
            val checkInId = insertCheckIn()
            val first = dao.insertTreeCapture(DbFixtures.treeCapture(checkInId))
            val second = dao.insertTreeCapture(DbFixtures.treeCapture(checkInId))
            dao.insertTreeAttribute(DbFixtures.treeAttribute(first, key = "a"))
            dao.insertTreeAttribute(DbFixtures.treeAttribute(first, key = "b"))
            dao.insertTreeAttribute(DbFixtures.treeAttribute(second, key = "c"))

            assertEquals(setOf("a", "b"), dao.getTreeAttributeByTreeCaptureId(first).map { it.key }.toSet())
            assertEquals(listOf("c"), dao.getTreeAttributeByTreeCaptureId(second).map { it.key })
        }

    @Test
    fun `deleteTreeCapture fails while attributes still reference the capture`() =
        runTest {
            val captureId = dao.insertTreeCapture(DbFixtures.treeCapture(insertCheckIn()))
            dao.insertTreeAttribute(DbFixtures.treeAttribute(captureId))

            val result = runCatching { dao.deleteTreeCapture(dao.getTreeCaptureById(captureId)) }

            assertTrue(result.isFailure)
            assertEquals(1, dao.getAllTreeCaptures().size)
        }

    @Test
    fun `insertTreeWithAttributes links every attribute to the new capture`() =
        runTest {
            val capture = DbFixtures.treeCapture(insertCheckIn())
            val attributes =
                listOf(
                    DbFixtures.treeAttribute(treeCaptureId = 0, key = "height", value = "3"),
                    DbFixtures.treeAttribute(treeCaptureId = 0, key = "species", value = "oak"),
                )

            val captureId = dao.insertTreeWithAttributes(capture, attributes)

            assertEquals(capture, dao.getTreeCaptureById(captureId))
            val stored = dao.getTreeAttributeByTreeCaptureId(captureId)
            assertEquals(mapOf("height" to "3", "species" to "oak"), stored.associate { it.key to it.value })
            assertTrue(attributes.all { it.treeCaptureId == captureId })
        }

    @Test
    fun `insertTreeWithAttributes with null attributes stores only the capture`() =
        runTest {
            val captureId = dao.insertTreeWithAttributes(DbFixtures.treeCapture(insertCheckIn()), null)

            assertEquals(1, dao.getAllTreeCaptures().size)
            assertTrue(dao.getTreeAttributeByTreeCaptureId(captureId).isEmpty())
        }

    @Test
    fun `insertTreeWithAttributes with no attributes stores only the capture`() =
        runTest {
            val captureId = dao.insertTreeWithAttributes(DbFixtures.treeCapture(insertCheckIn()), emptyList())

            assertEquals(listOf(captureId), dao.getAllTreeCaptures().map { it.id })
            assertTrue(dao.getTreeAttributeByTreeCaptureId(captureId).isEmpty())
        }
}