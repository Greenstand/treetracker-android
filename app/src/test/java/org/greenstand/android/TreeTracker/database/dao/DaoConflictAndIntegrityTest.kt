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
import kotlinx.datetime.Instant
import org.greenstand.android.TreeTracker.database.DbFixtures
import org.greenstand.android.TreeTracker.database.InMemoryDatabaseTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Conflict strategies, foreign key behaviour and value fidelity across the AppDatabase DAOs: what happens to
 * child rows when a parent is replaced or re-keyed, and what comes back for rows that don't exist.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class DaoConflictAndIntegrityTest : InMemoryDatabaseTest() {
    // region REPLACE on parents that have children

    @Test
    fun `insertSession with an existing id replaces the session and keeps its trees and locations`() =
        runTest {
            val sessionDao = database.sessionDao()
            val sessionId = sessionDao.insertSession(DbFixtures.session(uuid = "first"))
            database.treeDao().insertTree(DbFixtures.tree(sessionId = sessionId))
            database.locationDao().insertLocationData(DbFixtures.location(sessionId = sessionId))

            val replacedId =
                sessionDao.insertSession(
                    DbFixtures.session(uuid = "replaced", note = "edited").apply { id = sessionId },
                )

            assertEquals(sessionId, replacedId)
            assertEquals("replaced", sessionDao.getSessionById(sessionId).uuid)
            assertEquals("edited", sessionDao.getSessionById(sessionId).note)
            assertEquals(1, database.treeDao().getTreeCountFromSessionId(sessionId))
            assertEquals(1, database.locationDao().getLocationData().size)
        }

    @Test
    fun `insertPlanterInfo with an existing id replaces the planter and keeps its check ins`() =
        runTest {
            val planterDao = database.planterDao()
            val planterId = planterDao.insertPlanterInfo(DbFixtures.planterInfo(identifier = "old@example.com"))
            planterDao.insertPlanterCheckIn(DbFixtures.planterCheckIn(planterInfoId = planterId))

            planterDao.insertPlanterInfo(DbFixtures.planterInfo(identifier = "new@example.com").apply { id = planterId })

            assertEquals(listOf("new@example.com"), planterDao.getAllPlanterInfoList().map { it.identifier })
            assertEquals(1, planterDao.getAllPlanterCheckInsForPlanterInfoId(planterId).size)
        }

    @Test
    fun `insertPlanterCheckIn with an existing id replaces the check in`() =
        runTest {
            val planterDao = database.planterDao()
            val planterId = planterDao.insertPlanterInfo(DbFixtures.planterInfo())
            val checkInId = planterDao.insertPlanterCheckIn(DbFixtures.planterCheckIn(planterInfoId = planterId))

            planterDao.insertPlanterCheckIn(
                DbFixtures.planterCheckIn(planterInfoId = planterId, localPhotoPath = null, photoUrl = "https://photo").apply {
                    id = checkInId
                },
            )

            val stored = planterDao.getPlanterCheckInById(checkInId)!!
            assertNull(stored.localPhotoPath)
            assertEquals("https://photo", stored.photoUrl)
            assertEquals(1, planterDao.getAllPlanterCheckInsForPlanterInfoId(planterId).size)
        }

    @Test
    fun `insertTreeCapture with an existing id keeps the attributes that reference it`() =
        runTest {
            val treeDao = database.treeDao()
            val planterId = database.planterDao().insertPlanterInfo(DbFixtures.planterInfo())
            val checkInId = database.planterDao().insertPlanterCheckIn(DbFixtures.planterCheckIn(planterInfoId = planterId))
            val captureId =
                treeDao.insertTreeWithAttributes(
                    DbFixtures.treeCapture(planterCheckInId = checkInId),
                    listOf(DbFixtures.treeAttribute(treeCaptureId = 0)),
                )

            treeDao.insertTreeCapture(DbFixtures.treeCapture(planterCheckInId = checkInId, uploaded = true).apply { id = captureId })

            assertTrue(treeDao.getTreeCaptureById(captureId).uploaded)
            assertEquals(listOf("height"), treeDao.getTreeAttributeByTreeCaptureId(captureId).map { it.key })
        }

    @Test
    fun `insertLocationData with an existing id replaces the location json`() =
        runTest {
            val sessionId = database.sessionDao().insertSession(DbFixtures.session())
            val locationDao = database.locationDao()
            val locationId = locationDao.insertLocationData(DbFixtures.location(sessionId = sessionId, json = "{\"lat\":1.0}"))

            locationDao.insertLocationData(DbFixtures.location(sessionId = sessionId, json = "{\"lat\":2.0}").apply { id = locationId })

            assertEquals(listOf("{\"lat\":2.0}"), locationDao.getLocationData().map { it.locationDataJson })
        }

    @Test
    fun `insertTreeAttribute with an existing id replaces the attribute`() =
        runTest {
            val treeDao = database.treeDao()
            val planterId = database.planterDao().insertPlanterInfo(DbFixtures.planterInfo())
            val checkInId = database.planterDao().insertPlanterCheckIn(DbFixtures.planterCheckIn(planterInfoId = planterId))
            val captureId = treeDao.insertTreeCapture(DbFixtures.treeCapture(planterCheckInId = checkInId))
            val attributeId = treeDao.insertTreeAttribute(DbFixtures.treeAttribute(treeCaptureId = captureId, value = "3"))

            treeDao.insertTreeAttribute(DbFixtures.treeAttribute(treeCaptureId = captureId, value = "4").apply { id = attributeId })

            assertEquals(listOf("4"), treeDao.getTreeAttributeByTreeCaptureId(captureId).map { it.value })
        }

    // endregion

    // region ON UPDATE CASCADE

    @Test
    fun `changing a session id moves its trees and locations with it`() =
        runTest {
            val sessionId = database.sessionDao().insertSession(DbFixtures.session())
            database.treeDao().insertTree(DbFixtures.tree(sessionId = sessionId))
            database.locationDao().insertLocationData(DbFixtures.location(sessionId = sessionId))

            database.openHelper.writableDatabase.execSQL("UPDATE session SET _id = 42 WHERE _id = $sessionId")

            assertEquals(listOf(42L), database.treeDao().getAllTrees().map { it.sessionId })
            assertEquals(listOf(42L), database.locationDao().getLocationData().map { it.sessionId })
        }

    @Test
    fun `changing a device config id moves the sessions that reference it`() =
        runTest {
            val configId = database.deviceConfigDao().insertDeviceConfig(DbFixtures.deviceConfig())
            val sessionId = database.sessionDao().insertSession(DbFixtures.session(deviceConfigId = configId))

            database.openHelper.writableDatabase.execSQL("UPDATE device_config SET _id = 7 WHERE _id = $configId")

            assertEquals(7L, database.sessionDao().getSessionById(sessionId).deviceConfigId)
        }

    @Test
    fun `changing a planter id moves its check ins and their captures stay linked`() =
        runTest {
            val planterDao = database.planterDao()
            val planterId = planterDao.insertPlanterInfo(DbFixtures.planterInfo())
            val checkInId = planterDao.insertPlanterCheckIn(DbFixtures.planterCheckIn(planterInfoId = planterId))
            database.treeDao().insertTreeCapture(DbFixtures.treeCapture(planterCheckInId = checkInId))

            database.openHelper.writableDatabase.execSQL("UPDATE planter_info SET _id = 9 WHERE _id = $planterId")

            assertEquals(listOf(checkInId), planterDao.getAllPlanterCheckInsForPlanterInfoId(9L).map { it.id })
            val capture = database.treeDao().getAllTreeCaptures().single()
            assertEquals(checkInId, capture.planterCheckInId)
        }

    // endregion

    // region missing rows and empty id lists

    @Test
    fun `getSessionById throws for an unknown id because it returns a non-null session`() =
        runTest {
            val result = runCatching { database.sessionDao().getSessionById(404L) }

            assertTrue(result.isFailure)
        }

    @Test
    fun `getTreeCaptureById throws for an unknown id because it returns a non-null capture`() =
        runTest {
            val result = runCatching { database.treeDao().getTreeCaptureById(404L) }

            assertTrue(result.isFailure)
        }

    @Test
    fun `id list queries return nothing for an empty list`() =
        runTest {
            val sessionId = database.sessionDao().insertSession(DbFixtures.session())
            database.treeDao().insertTree(DbFixtures.tree(sessionId = sessionId))
            val planterId = database.planterDao().insertPlanterInfo(DbFixtures.planterInfo())
            database.planterDao().insertPlanterCheckIn(DbFixtures.planterCheckIn(planterInfoId = planterId))

            assertTrue(database.treeDao().getTreeCapturesByIds(emptyList()).isEmpty())
            assertTrue(database.planterDao().getPlanterCheckInsById(emptyList()).isEmpty())
            assertTrue(database.treeDao().getTreesByIds(emptyList()).isEmpty())
        }

    @Test
    fun `bulk updates with an empty id list change nothing`() =
        runTest {
            val sessionId = database.sessionDao().insertSession(DbFixtures.session())
            val treeId = database.treeDao().insertTree(DbFixtures.tree(sessionId = sessionId))

            database.sessionDao().updateSessionUploadStatus(emptyList(), true)
            database.sessionDao().updateSessionBundleIds(emptyList(), "bundle")
            database.treeDao().updateTreesUploadStatus(emptyList(), true)
            database.treeDao().removeTreesLocalImagePaths(emptyList())

            assertEquals(listOf(sessionId), database.sessionDao().getSessionsToUpload().map { it.id })
            assertNull(database.sessionDao().getSessionById(sessionId).bundleId)
            val tree = database.treeDao().getTreesByIds(listOf(treeId)).single()
            assertEquals(false, tree.uploaded)
            assertEquals("/tree.jpg", tree.photoPath)
        }

    @Test
    fun `getTreesByUserWallet emits an empty list for a wallet without sessions`() =
        runTest {
            val sessionId = database.sessionDao().insertSession(DbFixtures.session(originWallet = "wallet"))
            database.treeDao().insertTree(DbFixtures.tree(sessionId = sessionId))

            val trees = database.treeDao().getTreesByUserWallet("someone-else").first()
            assertTrue(trees.isEmpty())
        }

    // endregion

    // region value fidelity

    @Test
    fun `instants keep nanosecond precision through the database`() =
        runTest {
            val startTime = Instant.fromEpochSeconds(1_709_288_130L, 123_456_789)
            val endTime = Instant.fromEpochSeconds(1_709_291_730L, 1)
            val sessionId =
                database.sessionDao().insertSession(DbFixtures.session(endTime = endTime).copy(startTime = startTime))

            val stored = database.sessionDao().getSessionById(sessionId)

            assertEquals(startTime, stored.startTime)
            assertEquals(endTime, stored.endTime)
        }

    @Test
    fun `tree extra attributes with special characters and an empty map round trip`() =
        runTest {
            val sessionId = database.sessionDao().insertSession(DbFixtures.session())
            val attributes = mapOf("note" to "\"quoted\", with comma", "emoji" to "🌳", "empty" to "")
            val withAttributes =
                database.treeDao().insertTree(DbFixtures.tree(sessionId = sessionId, uuid = "a", extraAttributes = attributes))
            val withEmptyMap =
                database.treeDao().insertTree(DbFixtures.tree(sessionId = sessionId, uuid = "b", extraAttributes = emptyMap()))

            val trees = database.treeDao().getTreesByIds(listOf(withAttributes, withEmptyMap)).associateBy { it.uuid }

            assertEquals(attributes, trees.getValue("a").extraAttributes)
            assertEquals(emptyMap<String, String>(), trees.getValue("b").extraAttributes)
        }

    @Test
    fun `a tree whose stored extra attributes are corrupt still loads with null attributes`() =
        runTest {
            val sessionId = database.sessionDao().insertSession(DbFixtures.session())
            val treeId = database.treeDao().insertTree(DbFixtures.tree(sessionId = sessionId, extraAttributes = mapOf("a" to "b")))
            database.openHelper.writableDatabase.execSQL("UPDATE tree SET extra_attributes = 'not json' WHERE _id = $treeId")

            val tree = database.treeDao().getTreesByIds(listOf(treeId)).single()

            assertNull(tree.extraAttributes)
            assertEquals("tree-uuid", tree.uuid)
        }

    @Test
    fun `getLatestDeviceConfig picks the newest config across different days and fractional seconds`() =
        runTest {
            val dao = database.deviceConfigDao()
            dao.insertDeviceConfig(DbFixtures.deviceConfig(uuid = "older", loggedAt = Instant.parse("2024-03-01T23:59:59.999Z")))
            dao.insertDeviceConfig(DbFixtures.deviceConfig(uuid = "newest", loggedAt = Instant.parse("2024-03-02T00:00:00.001Z")))
            dao.insertDeviceConfig(DbFixtures.deviceConfig(uuid = "oldest", loggedAt = Instant.parse("2024-02-29T12:00:00Z")))

            assertEquals("newest", dao.getLatestDeviceConfig()?.uuid)
        }

    @Ignore(
        "Bug: logged_at is ORDER BY'd as text, and '…:30Z' sorts after '…:30.500Z', so a whole-second config wins over a later one in the same second",
    )
    @Test
    fun `getLatestDeviceConfig picks the later config within the same second`() =
        runTest {
            val dao = database.deviceConfigDao()
            dao.insertDeviceConfig(DbFixtures.deviceConfig(uuid = "earlier", loggedAt = Instant.parse("2024-03-01T10:15:30Z")))
            dao.insertDeviceConfig(DbFixtures.deviceConfig(uuid = "later", loggedAt = Instant.parse("2024-03-01T10:15:30.500Z")))

            assertEquals("later", dao.getLatestDeviceConfig()?.uuid)
        }

    // endregion
}